package com.github.opscalehub.chistanland.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.github.opscalehub.chistanland.BuildConfig
import com.github.opscalehub.chistanland.data.AppDatabase
import com.github.opscalehub.chistanland.data.LearningItem
import com.github.opscalehub.chistanland.data.LearningRepository
import com.github.opscalehub.chistanland.util.AudioManager
import com.github.opscalehub.chistanland.util.TtsManager
import com.google.ai.client.generativeai.GenerativeModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.json.JSONArray
import kotlin.random.Random

class LearningViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: LearningRepository = LearningRepository(
        AppDatabase.getDatabase(application).learningDao()
    )
    private val audioManager = AudioManager(application)
    val ttsManager = TtsManager(application)
    private var narrativeJob: Job? = null

    private val generativeModel = GenerativeModel(
        modelName = "gemini-1.5-flash",
        apiKey = BuildConfig.GEMINI_API_KEY
    )

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

    private val _isGenerating = MutableStateFlow(false)
    val isGenerating: StateFlow<Boolean> = _isGenerating.asStateFlow()

    private val sessionQueue = mutableListOf<LearningItem>()

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

    fun startLearningSession(mainItem: LearningItem) {
        sessionQueue.clear()
        sessionQueue.add(mainItem)
        val extraItems = allItems.value
            .filter { it.id != mainItem.id && it.isMastered }
            .shuffled()
            .take(2)
        sessionQueue.addAll(extraItems)
        startNextInQueue()
    }

    fun startReviewSession(items: List<LearningItem>, onStarted: () -> Unit) {
        sessionQueue.clear()
        sessionQueue.addAll(items.shuffled().take(5))
        _isReviewMode.value = true
        startNextInQueue()
        onStarted()
    }

    private fun startNextInQueue() {
        if (sessionQueue.isEmpty()) {
            _currentItem.value = null
            viewModelScope.launch { _uiEvent.emit(UiEvent.SessionComplete) }
            return
        }
        val nextItem = sessionQueue.removeAt(0)
        startLearning(nextItem, isReview = _isReviewMode.value)
    }

    fun playHintInstruction() {
        val item = _currentItem.value ?: return
        narrativeJob?.cancel()
        narrativeJob = viewModelScope.launch {
            delay(300) 
            _avatarState.value = "SPEAKING"
            
            if (_activityType.value == ActivityType.STORY_TELLING) {
                generateAndPlayStory(item)
            } else {
                val instruction = when (_activityType.value) {
                    ActivityType.PHONICS_INTRO -> "بیا با هم بِنِویسیم: «${item.word}»..."
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
        _isGenerating.value = true
        try {
            val prompt = """
                یک داستان بسیار کوتاه، شاد و کودکانه (حداکثر ۲ جمله) برای یک کودک ۴ ساله درباره کلمه «${item.word}» بگو.
                داستان باید با این کلمه شروع شود و حس کنجکاوی کودک را برانگیزد.
                فقط متن داستان را برگردان.
            """.trimIndent()
            
            val response = generativeModel.generateContent(prompt)
            val story = response.text ?: "یه روز یه ${item.word} مهربون داشتیم که خیلی خوشحال بود!"
            ttsManager.speak(story)
            delay(5000) 
        } catch (e: Exception) {
            ttsManager.speak("بیا با هم درباره ${item.word} یاد بگیریم!")
        } finally {
            _isGenerating.value = false
        }
    }

    fun startLearning(item: LearningItem, isReview: Boolean = false) {
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
        
        generateAdaptiveKeyboard(item)
        playHintInstruction()
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
        _avatarState.value = "THINKING"
        viewModelScope.launch {
            _uiEvent.emit(UiEvent.Error)
            delay(1500) 
            if (_avatarState.value == "THINKING") _avatarState.value = "IDLE"
        }
        audioManager.playSoundAsync("error_sound")
    }

    private fun completeLevel(isCorrect: Boolean) {
        narrativeJob?.cancel()
        narrativeJob = viewModelScope.launch {
            val item = _currentItem.value ?: return@launch
            repository.updateProgress(item, isCorrect)
            if (isCorrect) {
                _streak.value += 1
                launch { _uiEvent.emit(UiEvent.Success) }
                audioManager.playSound("success_fest")
                delay(600) 
                val rewards = listOf("آفرین قَهرمان!", "عالی بود عَزیزم", "خیلی باهوشی!", "صد آفرین به تو", "ماشاالله، ادامه بِدِه!")
                ttsManager.speak(rewards.random())
                delay(2000) 
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
            2 -> 2
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
            listOf("ا", "ب", "د", "م", "س", "ر", "ن", "ز", "ت").filter { it !in mandatory }.shuffled().take(distractorCount)
        }
        _keyboardKeys.value = (mandatory.toList() + distractors).shuffled()
    }

    fun seedData() {
        viewModelScope.launch {
            // ترتیب آموزشی پله‌پله (Scaffolding): هر کلمه فقط شامل حروف همان پله یا پله‌های قبل است.
            val pedagogicalData = listOf(
                // پله ۱: شروع با ساده‌ترین‌ها (آ، ب)
                LearningItem("p01", "آ", "آ", "آ", "img_a1", "ALPHABET"),
                LearningItem("p02", "ب", "بابا", "بابا", "img_a2", "ALPHABET"),
                LearningItem("p03", "د", "آباد", "آباد", "img_a3", "ALPHABET"),
                
                // پله ۲: ترکیب با نشانه‌های جدید (م، ن)
                LearningItem("p04", "م", "بام", "بام", "img_a4", "ALPHABET"),
                LearningItem("p05", "ن", "نان", "نان", "img_a5", "ALPHABET"),
                
                // پله ۳: حرکت و صداهای کشیده (ر، ز)
                LearningItem("p06", "ر", "بار", "بار", "img_a6", "ALPHABET"),
                LearningItem("p07", "ز", "باز", "باز", "img_a7", "ALPHABET"),
                
                // پله ۴: حروف پایه دیگر (س، ت)
                LearningItem("p08", "س", "سام", "سام", "img_a8", "ALPHABET"),
                LearningItem("p09", "ت", "تار", "تار", "img_a9", "ALPHABET"),
                
                // پله ۵: حروف دارای شباهت ظاهری (و، ه)
                LearningItem("p10", "و", "بوم", "بوم", "img_a10", "ALPHABET"),
                LearningItem("p11", "ه", "ماه", "ماه", "img_a11", "ALPHABET"),
                
                // پله ۶: تکمیل تدریجی با کلمات ساده
                LearningItem("p12", "ی", "سینی", "سینی", "img_a12", "ALPHABET"),
                LearningItem("p13", "ش", "شام", "شام", "img_a13", "ALPHABET"),
                LearningItem("p14", "خ", "خار", "خار", "img_a14", "ALPHABET"),
                LearningItem("p15", "ف", "فارس", "فارس", "img_a15", "ALPHABET"),
                LearningItem("p16", "ق", "قند", "قند", "img_a16", "ALPHABET"),
                LearningItem("p17", "ل", "لادن", "لادن", "img_a17", "ALPHABET"),
                LearningItem("p18", "ک", "کارد", "کارد", "img_a18", "ALPHABET"),
                LearningItem("p19", "گ", "گام", "گام", "img_a19", "ALPHABET"),
                LearningItem("p20", "پ", "پایان", "پایان", "img_a20", "ALPHABET"),
                LearningItem("p21", "چ", "چادر", "چادر", "img_a21", "ALPHABET"),
                LearningItem("p22", "ج", "جان", "جان", "img_a22", "ALPHABET"),
                LearningItem("p23", "ح", "حرم", "حرم", "img_a23", "ALPHABET"),
                LearningItem("p24", "ع", "عادل", "عادل", "img_a24", "ALPHABET"),
                LearningItem("p25", "غ", "غار", "غار", "img_a25", "ALPHABET"),
                LearningItem("p26", "ط", "طناب", "طناب", "img_a26", "ALPHABET"),
                LearningItem("p27", "ظ", "ناظم", "ناظم", "img_a27", "ALPHABET"),
                LearningItem("p28", "ص", "صابون", "صابون", "img_a28", "ALPHABET"),
                LearningItem("p29", "ض", "رضا", "رضا", "img_a29", "ALPHABET"),
                LearningItem("p30", "ذ", "آذر", "آذر", "img_a30", "ALPHABET"),
                LearningItem("p31", "ث", "ثبت", "ثبت", "img_a31", "ALPHABET"),
                LearningItem("p32", "ژ", "دژ", "دژ", "img_a32", "ALPHABET")
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
