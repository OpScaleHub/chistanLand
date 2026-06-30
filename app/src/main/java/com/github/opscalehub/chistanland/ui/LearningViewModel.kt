package com.github.opscalehub.chistanland.ui

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.github.opscalehub.chistanland.data.AppDatabase
import com.github.opscalehub.chistanland.data.LearningItem
import com.github.opscalehub.chistanland.data.LearningRepository
import com.github.opscalehub.chistanland.util.AudioManager
import com.github.opscalehub.chistanland.util.TtsManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import kotlin.random.Random

class LearningViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: LearningRepository = LearningRepository(
        AppDatabase.getDatabase(application).learningDao()
    )
    private val audioManager = AudioManager(application)
    val ttsManager = TtsManager(application)
    private var narrativeJob: Job? = null

    // Session advancement (reward → load next) runs in its OWN job so narration/onPause/
    // stopAudio can never cancel it mid-flight (which used to leave the UI stuck). `advancing`
    // is a re-entrancy guard so a fast double-tap can't complete the same item twice.
    private var advanceJob: Job? = null
    private var advancing = false

    // Daily streak (consecutive days the child has played), persisted across launches.
    private val prefs = application.getSharedPreferences("chistan_prefs", Context.MODE_PRIVATE)
    private val _dailyStreak = MutableStateFlow(initialDailyStreak())
    val dailyStreak: StateFlow<Int> = _dailyStreak.asStateFlow()

    val allItems: StateFlow<List<LearningItem>> = repository.allItems
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _selectedCategory = MutableStateFlow<String?>("ALPHABET")
    val selectedCategory: StateFlow<String?> = _selectedCategory.asStateFlow()

    val filteredItems = allItems.map { items ->
        items.filter { it.category == "ALPHABET" }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _currentItem = MutableStateFlow<LearningItem?>(null)
    val currentItem: StateFlow<LearningItem?> = _currentItem.asStateFlow()

    private val _typedText = MutableStateFlow("")
    val typedText: StateFlow<String> = _typedText.asStateFlow()

    private val _charStatus = MutableStateFlow<List<Boolean>>(emptyList())
    val charStatus: StateFlow<List<Boolean>> = _charStatus.asStateFlow()

    private val _activityType = MutableStateFlow(ActivityType.PHONICS_INTRO)
    val activityType: StateFlow<ActivityType> = _activityType.asStateFlow()

    private val _missingCharIndex = MutableStateFlow(-1)
    val missingCharIndex: StateFlow<Int> = _missingCharIndex.asStateFlow()

    private var hasErrorInCurrentWord: Boolean = false

    private val _streak = MutableStateFlow(0)
    val streak: StateFlow<Int> = _streak.asStateFlow()

    private val _keyboardKeys = MutableStateFlow<List<String>>(emptyList())
    val keyboardKeys: StateFlow<List<String>> = _keyboardKeys.asStateFlow()

    private val _uiEvent = MutableSharedFlow<UiEvent>()
    val uiEvent = _uiEvent.asSharedFlow()

    private val _avatarState = MutableStateFlow("IDLE")
    val avatarState: StateFlow<String> = _avatarState.asStateFlow()

    private val _isReviewMode = MutableStateFlow(false)
    val isReviewMode: StateFlow<Boolean> = _isReviewMode.asStateFlow()


    // Image-pick (WORD_RECOGNITION): the shuffled set of picture choices, one of which matches.
    private val _recognitionOptions = MutableStateFlow<List<LearningItem>>(emptyList())
    val recognitionOptions: StateFlow<List<LearningItem>> = _recognitionOptions.asStateFlow()

    // A queued round. `scores = false` means an extra practice word that gives full reward
    // feedback (sparkles, streak, sticker) but does NOT advance the letter's level — so more
    // examples deepen exposure without speeding up mastery.
    private data class QueueEntry(val item: LearningItem, val scores: Boolean)
    private val sessionQueue = mutableListOf<QueueEntry>()
    private var currentScores = true

    /**
     * Extra concrete example words per letter, each built ONLY from letters introduced at or
     * before that step (vocabulary control / the +1 principle). They are practiced as deeper,
     * non-scoring exposure alongside the main word. Extend this map to slow the pace further.
     */
    private val letterExamples: Map<String, List<String>> = mapOf(
        // Early letters (steps 2–11): a second concrete word slows the youngest learner's pace.
        // Every word is vocabulary-controlled — it uses ONLY letters introduced at or before its step.
        "ب" to listOf("آب"),
        "م" to listOf("بادام", "آدم"),
        "ر" to listOf("مار", "مادر"),
        "ز" to listOf("بز"),
        "و" to listOf("رود", "بازو"),
        "س" to listOf("اسب", "سوسمار"),
        "ن" to listOf("آسمان", "نردبان"),
        "ت" to listOf("توت", "دست"),
        "ی" to listOf("دریا", "میز"),
        // Later letters (steps 12–32): by here almost the whole alphabet is available,
        // so each gets one clear, concrete example built only from already-known letters.
        "ش" to listOf("موش", "شتر"),
        "ه" to listOf("ماه", "ماهی"),
        "پ" to listOf("پروانه", "پسته"),
        "خ" to listOf("خرس", "درخت"),
        "ف" to listOf("آفتاب", "دفتر"),
        "ق" to listOf("قاشق", "قایق"),
        "ل" to listOf("فیل", "لباس"),
        "ک" to listOf("کفش", "کتاب"),
        "گ" to listOf("گاو", "انگور"),
        "چ" to listOf("چتر", "قارچ"),
        "ج" to listOf("جنگل", "هویج"),
        "ح" to listOf("حلزون", "تمساح"),
        "ع" to listOf("عینک", "عسل"),
        "غ" to listOf("مرغ", "کلاغ"),
        "ط" to listOf("طوطی", "بطری"),
        "ص" to listOf("صدف", "صندلی"),
        "ض" to listOf("حوض"),
        "ذ" to listOf("کاغذ", "ذرت"),
        "ژ" to listOf("ژاکت")
        // ظ (step 29) and ث (step 31) intentionally omitted — no concrete, child-friendly
        // Persian word exists using only the letters known by that step; their main word stands alone.
    )

    enum class ActivityType {
        PHONICS_INTRO,      
        MISSING_LETTER,     
        SPELLING,           
        WORD_RECOGNITION,   
        QUICK_RECALL,
        STORY_TELLING,
        TRACE_LETTER 
    }

    sealed class UiEvent {
        object Error : UiEvent()
        object Success : UiEvent()
        object LevelDown : UiEvent()
        object StartReviewSession : UiEvent()
        object SessionComplete : UiEvent()
    }

    fun selectCategory(category: String?) {
        _selectedCategory.value = category
    }

    fun stopAudio() {
        narrativeJob?.cancel()
        ttsManager.stop()
    }

    /**
     * Abandon the current session cleanly when the child leaves the learning screen.
     * Cancels everything in flight and resets state so re-entering always starts fresh —
     * prevents stray coroutines from firing SessionComplete/onBack after we've left.
     */
    fun cancelSession() {
        advanceJob?.cancel()
        narrativeJob?.cancel()
        ttsManager.stop()
        advancing = false
        _isReviewMode.value = false
        sessionQueue.clear()
        _currentItem.value = null
    }

    /** Speak a single word (used by the sticker album when a child taps a collected sticker). */
    fun speakWord(text: String) {
        narrativeJob?.cancel()
        narrativeJob = viewModelScope.launch { ttsManager.speak(text) }
    }

    /** Local-time day number (days since epoch), so streaks roll over at midnight. */
    private fun todayIndex(): Long {
        val now = System.currentTimeMillis()
        val tzOffset = java.util.TimeZone.getDefault().getOffset(now)
        return (now + tzOffset) / 86_400_000L
    }

    /** Streak to show on launch: still alive if played today or yesterday, otherwise broken. */
    private fun initialDailyStreak(): Int {
        val last = prefs.getLong("last_play_day", -1L)
        val stored = prefs.getInt("daily_streak", 0)
        if (last < 0) return 0
        val diff = todayIndex() - last
        return if (diff == 0L || diff == 1L) stored else 0
    }

    /** Mark that the child played today and advance/reset the daily streak accordingly. */
    fun recordPlayToday() {
        val last = prefs.getLong("last_play_day", -1L)
        val stored = prefs.getInt("daily_streak", 0)
        val today = todayIndex()
        val newStreak = when {
            last < 0L -> 1
            today == last -> stored.coerceAtLeast(1) // already counted today
            today - last == 1L -> stored + 1          // consecutive day
            else -> 1                                  // a day was missed → restart
        }
        prefs.edit()
            .putInt("daily_streak", newStreak)
            .putLong("last_play_day", today)
            .putInt("best_streak", maxOf(newStreak, prefs.getInt("best_streak", 0)))
            .apply()
        _dailyStreak.value = newStreak
    }

    fun startLearningSession(mainItem: LearningItem) {
        recordPlayToday()
        _isReviewMode.value = false
        sessionQueue.clear()
        // 1) The main word — this is the one that advances the letter's level.
        sessionQueue.add(QueueEntry(mainItem, scores = true))
        // 2) Extra example words for the same letter — deeper, non-scoring practice.
        // Each gets a DISTINCT id (still derived from the main id, so the "p0" keyboard check
        // holds): screens key their per-exercise state on item.id, so reusing the main id left
        // a traced/typed exercise looking already-finished and ignoring touch on the next word.
        letterExamples[mainItem.character].orEmpty().forEachIndexed { i, example ->
            sessionQueue.add(QueueEntry(mainItem.copy(id = "${mainItem.id}_ex$i", word = example), scores = false))
        }
        // 3) One previously-mastered letter interleaved as light review (non-scoring).
        allItems.value
            .filter { it.id != mainItem.id && it.isMastered }
            .shuffled()
            .take(1)
            .forEach { sessionQueue.add(QueueEntry(it, scores = false)) }
        startNextInQueue()
    }

    fun startReviewSession(items: List<LearningItem>, onStarted: () -> Unit) {
        recordPlayToday()
        sessionQueue.clear()
        sessionQueue.addAll(items.shuffled().take(5).map { QueueEntry(it, scores = true) })
        _isReviewMode.value = true
        startNextInQueue()
        onStarted()
    }

    private fun startNextInQueue() {
        if (sessionQueue.isEmpty()) {
            advancing = false
            _currentItem.value = null
            viewModelScope.launch { _uiEvent.emit(UiEvent.SessionComplete) }
            return
        }
        val next = sessionQueue.removeAt(0)
        currentScores = next.scores
        startLearning(next.item, isReview = _isReviewMode.value)
    }

    fun playHintInstruction() {
        if (advancing) return // don't talk over the reward / next-item transition
        val item = _currentItem.value ?: return
        narrativeJob?.cancel()
        narrativeJob = viewModelScope.launch {
            delay(300) 
            _avatarState.value = "SPEAKING"
            
            if (_activityType.value == ActivityType.STORY_TELLING) {
                generateAndPlayStory(item)
            } else {
                val instruction = when (_activityType.value) {
                    // آواشناسی: اول صدای نشانه را معرفی کن، بعد آن را در کلمه نشان بده (پیوند صدا↔نشانه↔کلمه)
                    ActivityType.PHONICS_INTRO -> "این نِشانه «${item.character}» است. «${item.character}»... مثلِ «${item.word}». بیا با هم «${item.word}» را بِنِویسیم!"
                    ActivityType.MISSING_LETTER -> "توی کلمه ${item.word}، کدوم نِشانه گُم شده؟"
                    ActivityType.SPELLING -> "حالا خودت بِنِویس: «${item.word}»"
                    ActivityType.WORD_RECOGNITION -> "تَصویرِ ${item.word} کُجاست؟"
                    ActivityType.QUICK_RECALL -> "زود بِنِویس: «${item.word}»"
                    ActivityType.TRACE_LETTER -> "بیا نِشانه «${item.character}» رو نَقاشی کنیم!"
                    else -> ""
                }
                ttsManager.speak(instruction)
            }
            _avatarState.value = "IDLE"
        }
    }

    private suspend fun generateAndPlayStory(item: LearningItem) {
        val stories = listOf(
            "یه روز ${item.word} کوچولو رفت بازی، اونقدر شاد بود که همه خندیدن!",
            "${item.word} توی باغ بود که یه پروانه رنگارنگ اومد پیشش و باهاش دوست شد!",
            "روزی بود که ${item.word} یه راز جادویی پیدا کرد و سفر شگفت‌انگیزی شروع شد!",
            "یه ${item.word} مهربون بود که هر روز با دوستاش بازی می‌کرد و همه رو خوشحال می‌کرد!"
        )
        ttsManager.speak(stories.random())
        delay(5000)
    }

    fun startLearning(item: LearningItem, isReview: Boolean = false) {
        advancing = false // the new item is interactive again
        _isReviewMode.value = isReview
        _currentItem.value = item
        _typedText.value = ""
        _charStatus.value = emptyList()
        hasErrorInCurrentWord = false
        _activityType.value = decideActivityType(item, isReview)
        
        if (_activityType.value == ActivityType.MISSING_LETTER) {
            _missingCharIndex.value = Random.nextInt(item.word.length)
        } else {
            _missingCharIndex.value = -1
        }

        if (_activityType.value == ActivityType.WORD_RECOGNITION) {
            // Build the picture choices: the target word + two other words (distinct words), shuffled.
            val distractors = allItems.value
                .filter { it.id != item.id && it.word != item.word }
                .distinctBy { it.word }
                .shuffled()
                .take(2)
            _recognitionOptions.value = (distractors + item).shuffled()
        } else {
            _recognitionOptions.value = emptyList()
        }

        generateAdaptiveKeyboard(item)
        playHintInstruction()
    }

    /** Image-pick answer: the child tapped one of the picture choices. */
    fun onImagePicked(picked: LearningItem) {
        if (advancing) return
        val current = _currentItem.value ?: return
        if (_activityType.value != ActivityType.WORD_RECOGNITION) return
        if (picked.id == current.id) {
            _avatarState.value = "HAPPY"
            completeLevel(!hasErrorInCurrentWord)
        } else {
            handleError()
        }
    }

    private fun decideActivityType(item: LearningItem, isReview: Boolean): ActivityType {
        if (isReview) {
            return listOf(ActivityType.QUICK_RECALL, ActivityType.WORD_RECOGNITION).random()
        }

        return when (item.level) {
            1 -> ActivityType.PHONICS_INTRO
            2 -> listOf(ActivityType.TRACE_LETTER, ActivityType.MISSING_LETTER).random()
            3 -> listOf(ActivityType.SPELLING, ActivityType.MISSING_LETTER).random()
            4 -> listOf(ActivityType.WORD_RECOGNITION, ActivityType.STORY_TELLING).random()
            else -> ActivityType.STORY_TELLING
        }
    }

    fun onCharTyped(char: String) {
        if (advancing) return // ignore input while a completion is animating to the next item
        val current = _currentItem.value ?: return
        val targetFullString = current.word

        if (_activityType.value == ActivityType.STORY_TELLING || _activityType.value == ActivityType.TRACE_LETTER) {
            completeLevel(true)
            return
        }

        when (_activityType.value) {
            ActivityType.MISSING_LETTER -> {
                val targetChar = targetFullString[_missingCharIndex.value].toString()
                if (char == targetChar) {
                    _typedText.value = targetFullString
                    _charStatus.value = List(targetFullString.length) { true }
                    completeLevel(true)
                } else {
                    handleError()
                }
            }
            else -> {
                if (_typedText.value.length >= targetFullString.length) return
                val targetChar = targetFullString[_typedText.value.length].toString()
                if (char == targetChar) {
                    processCorrectChar(char, targetFullString)
                } else {
                    handleError()
                }
            }
        }
    }

    private fun processCorrectChar(char: String, targetFullString: String) {
        _typedText.value += char
        _charStatus.value = _charStatus.value + true
        _avatarState.value = "HAPPY"
        audioManager.playSoundAsync("pop_sound")
        if (_typedText.value.length == targetFullString.length) {
            completeLevel(!hasErrorInCurrentWord)
        } else {
            viewModelScope.launch {
                delay(800)
                if (_avatarState.value == "HAPPY") _avatarState.value = "IDLE"
            }
        }
    }

    private fun handleError() {
        hasErrorInCurrentWord = true
        _streak.value = 0 // زنجیره موفقیت با اولین اشتباه صفر می‌شود (وگرنه هرگز ریست نمی‌شد)
        _avatarState.value = "THINKING"
        viewModelScope.launch {
            _uiEvent.emit(UiEvent.Error)
            delay(1500) 
            if (_avatarState.value == "THINKING") _avatarState.value = "IDLE"
        }
        audioManager.playSoundAsync("error_sound")
    }

    private fun completeLevel(isCorrect: Boolean) {
        if (advancing) return // already completing this item — ignore extra taps
        advancing = true
        val item = _currentItem.value
        narrativeJob?.cancel() // stop any in-progress hint/story narration
        advanceJob?.cancel()
        // NOTE: advancement runs in advanceJob (NOT narrativeJob), so stopAudio()/onPause/
        // narration can't cancel it — startNextInQueue() is guaranteed to run.
        advanceJob = viewModelScope.launch {
            // The score write must survive even if the child bolts mid-celebration.
            if (item != null && currentScores) {
                withContext(NonCancellable) { repository.updateProgress(item, isCorrect) }
            }
            if (isCorrect) {
                _streak.value += 1
                _uiEvent.emit(UiEvent.Success)
                audioManager.playSound("success_fest")
                delay(600)
                val rewards = listOf("آفرین قَهرمان!", "عالی بود عَزیزم", "خیلی باهوشی!", "صد آفرین به تو", "ماشاالله، ادامه بِدِه!")
                ttsManager.speak(rewards.random())
                delay(1400)
            } else {
                delay(1000)
            }
            _avatarState.value = "IDLE"
            startNextInQueue()
        }
    }

    private fun generateAdaptiveKeyboard(item: LearningItem) {
        val wordChars = item.word.map { it.toString() }.toSet()
        val mandatory = wordChars.toMutableSet()
        
        val distractorCount = when(item.level) {
            1 -> 0 
            2 -> if (item.id.contains("p0")) 1 else 2 
            3 -> 3
            else -> 4
        }
        
        if (distractorCount == 0) {
            _keyboardKeys.value = mandatory.toList().shuffled()
            return
        }
        
        val currentItems = allItems.value
        val learnedLetters = currentItems
            .filter { it.isMastered }
            .flatMap { it.word.map { c -> c.toString() } }
            .toSet()
            
        val potentialDistractors = (learnedLetters - mandatory).toList()
        val distractors = if (potentialDistractors.size >= distractorCount) {
            potentialDistractors.shuffled().take(distractorCount)
        } else {
            listOf("آ", "ب", "د", "م", "ر", "ز", "س", "ن", "ت", "و").filter { it !in mandatory }.shuffled().take(distractorCount)
        }
        _keyboardKeys.value = (mandatory.toList() + distractors).shuffled()
    }

    fun seedData() {
        viewModelScope.launch {
            // زنجیره یادگیری ۱۰۰٪ عینی و تصویری (Concrete V2.1)
            val pedagogicalData = listOf(
                LearningItem("p01", "آ", "آ", "آ", "img_a1", "ALPHABET"),
                LearningItem("p02", "ب", "بابا", "بابا", "img_a2", "ALPHABET"),
                LearningItem("p03", "د", "باد", "باد", "img_a3", "ALPHABET"),
                LearningItem("p04", "م", "بام", "بام", "img_a4", "ALPHABET"),
                LearningItem("p05", "ر", "آرد", "آرد", "img_a5", "ALPHABET"),
                LearningItem("p06", "ز", "بازار", "بازار", "img_a6", "ALPHABET"),
                LearningItem("p07", "و", "دود", "دود", "img_a7", "ALPHABET"),
                LearningItem("p08", "س", "سبد", "سبد", "img_a8", "ALPHABET"),
                LearningItem("p09", "ن", "نان", "نان", "img_a9", "ALPHABET"),
                LearningItem("p10", "ت", "تاب", "تاب", "img_a10", "ALPHABET"),
                LearningItem("p11", "ی", "سینی", "سینی", "img_a11", "ALPHABET"),
                LearningItem("p12", "ش", "آش", "آش", "img_a12", "ALPHABET"),
                LearningItem("p13", "ه", "کوه", "کوه", "img_a13", "ALPHABET"),
                LearningItem("p14", "پ", "توپ", "توپ", "img_a14", "ALPHABET"),
                LearningItem("p15", "خ", "شاخ", "شاخ", "img_a15", "ALPHABET"),
                LearningItem("p16", "ف", "برف", "برف", "img_a16", "ALPHABET"),
                LearningItem("p17", "ق", "قند", "قند", "img_a17", "ALPHABET"),
                LearningItem("p18", "ل", "گل", "گل", "img_a18", "ALPHABET"),
                LearningItem("p19", "ک", "کارد", "کارد", "img_a19", "ALPHABET"),
                LearningItem("p20", "گ", "سگ", "سگ", "img_a20", "ALPHABET"),
                LearningItem("p21", "چ", "چادر", "چادر", "img_a21", "ALPHABET"),
                LearningItem("p22", "ج", "جوجه", "جوجه", "img_a22", "ALPHABET"),
                LearningItem("p23", "ح", "حوله", "حوله", "img_a23", "ALPHABET"),
                LearningItem("p24", "ع", "عروسک", "عروسک", "img_a24", "ALPHABET"),
                LearningItem("p25", "غ", "غار", "غار", "img_a25", "ALPHABET"),
                LearningItem("p26", "ط", "طناب", "طناب", "img_a26", "ALPHABET"),
                LearningItem("p27", "ص", "صابون", "صابون", "img_a27", "ALPHABET"),
                LearningItem("p28", "ض", "وضو", "وضو", "img_a28", "ALPHABET"),
                LearningItem("p29", "ظ", "ظرف", "ظرف", "img_a29", "ALPHABET"),
                LearningItem("p30", "ذ", "ذره‌بین", "ذره‌بین", "img_a30", "ALPHABET"),
                LearningItem("p31", "ث", "مثلث", "مثلث", "img_a31", "ALPHABET"),
                LearningItem("p32", "ژ", "ژله", "ژله", "img_a32", "ALPHABET")
            )
            repository.insertInitialData(pedagogicalData)
        }
    }

    fun getParentNarrative(): String {
        val items = allItems.value
        if (items.isEmpty()) return "هنوز داده‌ای برای تحلیل وجود ندارد."
        
        val masteredCount = items.count { it.isMastered }
        val totalCount = items.size
        val progressPercent = if (totalCount > 0) (masteredCount.toFloat() / totalCount * 100).toInt() else 0
        
        return when {
            progressPercent == 0 -> "کودک شما در حال آشنایی با اولین نشانه‌هاست."
            progressPercent < 30 -> "$masteredCount نشانه به خوبی یاد گرفته شده."
            progressPercent < 70 -> "کودک شما بر بیش از نیمی از نشانه‌ها مسلط شده است."
            else -> "تقریباً تمام نشانه‌ها تثبیت شده‌اند."
        }
    }

    override fun onCleared() {
        super.onCleared()
        audioManager.release()
        ttsManager.release()
    }
}
