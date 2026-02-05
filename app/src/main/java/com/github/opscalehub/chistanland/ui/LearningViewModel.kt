package com.github.opscalehub.chistanland.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.github.opscalehub.chistanland.data.AppDatabase
import com.github.opscalehub.chistanland.data.LearningItem
import com.github.opscalehub.chistanland.data.LearningRepository
import com.github.opscalehub.chistanland.util.AudioManager
import com.github.opscalehub.chistanland.util.TtsManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlin.random.Random

class LearningViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: LearningRepository = LearningRepository(
        AppDatabase.getDatabase(application).learningDao()
    )
    private val audioManager = AudioManager(application)
    private val ttsManager = TtsManager(application)
    private var narrativeJob: Job? = null

    val allItems: StateFlow<List<LearningItem>> = repository.allItems
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _selectedCategory = MutableStateFlow<String?>(null)
    val selectedCategory: StateFlow<String?> = _selectedCategory.asStateFlow()

    val filteredItems = combine(allItems, _selectedCategory) { items, category ->
        if (category == null) emptyList()
        else items.filter { it.category == category }
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

    private val sessionQueue = mutableListOf<LearningItem>()

    enum class ActivityType { 
        PHONICS_INTRO,      // مرحله تطبیق: تایپ کل کلمه با کیبورد محدود (بدون مزاحم)
        MISSING_LETTER,     // مرحله تعمیق: پیدا کردن حرف گمشده
        SPELLING,           // مرحله تثبیت: نوشتن کامل کلمه با مزاحم‌های بیشتر
        WORD_RECOGNITION,   
        QUICK_RECALL        
    }

    sealed class UiEvent {
        object Error : UiEvent()
        object Success : UiEvent()
        object LevelDown : UiEvent()
        object StartReviewSession : UiEvent()
        object SessionComplete : UiEvent()
    }

    private val persianDigits = listOf("۱", "۲", "۳", "۴", "۵", "۶", "۷", "۸", "۹", "۰")

    fun selectCategory(category: String?) {
        _selectedCategory.value = category
    }

    fun startLearningSession(mainItem: LearningItem) {
        sessionQueue.clear()
        sessionQueue.add(mainItem)
        
        val extraItems = filteredItems.value
            .filter { it.id < mainItem.id && it.lastReviewTime > 0 }
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
            _avatarState.value = "SPEAKING"
            val instruction = when (_activityType.value) {
                ActivityType.PHONICS_INTRO -> "بیا با هم بنویسیم: «${item.word}»"
                ActivityType.MISSING_LETTER -> "توی کلمه «${item.word}» کدوم نشانه گم شده؟"
                ActivityType.SPELLING -> "حالا خودت بنویس: «${item.word}»"
                ActivityType.WORD_RECOGNITION -> "تصویر «${item.word}» کجاست؟"
                ActivityType.QUICK_RECALL -> "زود بنویس: «${item.word}»"
            }
            ttsManager.speak(instruction)
            _avatarState.value = "IDLE"
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
        if (isReview) return ActivityType.QUICK_RECALL
        return when (item.level) {
            1 -> ActivityType.PHONICS_INTRO
            2 -> ActivityType.MISSING_LETTER
            3 -> ActivityType.SPELLING
            4 -> if (Random.nextBoolean()) ActivityType.WORD_RECOGNITION else ActivityType.SPELLING
            else -> ActivityType.QUICK_RECALL
        }
    }

    fun onCharTyped(char: String) {
        val current = _currentItem.value ?: return
        
        // در تمامی فعالیت‌های الفبایی، هدف نهایی تکمیل کل کلمه است
        val targetFullString = if (current.category == "NUMBER") current.character else current.word
        
        when (_activityType.value) {
            ActivityType.MISSING_LETTER -> {
                val targetChar = targetFullString[_missingCharIndex.value].toString()
                if (char == targetChar) {
                    // بعد از حدس درست حرف گمشده، کل کلمه نمایش داده می‌شود
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
        }
    }

    private fun handleError() {
        hasErrorInCurrentWord = true
        _avatarState.value = "THINKING"
        viewModelScope.launch {
            _uiEvent.emit(UiEvent.Error)
            delay(1200)
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
                delay(500)
                ttsManager.speak("آفرین")
            }
            delay(1500)
            _avatarState.value = "IDLE"
            startNextInQueue()
        }
    }

    private fun generateAdaptiveKeyboard(item: LearningItem) {
        if (item.category == "NUMBER") {
            _keyboardKeys.value = persianDigits
        } else {
            val wordChars = item.word.map { it.toString() }.toSet()
            val mandatory = wordChars.toMutableSet()
            
            // در سطح ۱ (PHONICS_INTRO)، هیچ حرف مزاحمی نمایش نمی‌دهیم تا کودک فقط حروف کلمه را تطبیق دهد
            val distractorCount = when(item.level) {
                1 -> 0 
                2 -> 2
                3 -> 4
                else -> 6
            }

            if (distractorCount == 0) {
                _keyboardKeys.value = mandatory.toList().shuffled()
                return
            }

            val currentItems = filteredItems.value
            // انتخاب مزاحم‌ها فقط از حروفی که قبلاً تدریس شده‌اند و در همان دسته‌بندی هستند
            val learnedLetters = currentItems
                .filter { it.id < item.id && it.category == item.category }
                .flatMap { it.word.map { c -> c.toString() } }
                .toSet()
            
            val potentialDistractors = (learnedLetters - mandatory).toList()
            
            val distractors = potentialDistractors.shuffled().take(distractorCount)
            _keyboardKeys.value = (mandatory.toList() + distractors).shuffled()
        }
    }

    fun getParentNarrative(): String {
        val items = allItems.value
        if (items.isEmpty()) return "هنوز ماجراجویی را شروع نکرده‌اید! بیایید اولین کلمه را با هم یاد بگیریم."
        
        val masteredCount = items.count { it.isMastered }
        val totalCount = items.size
        val progressPercent = (masteredCount.toFloat() / totalCount * 100).toInt()
        
        return when {
            progressPercent > 80 -> "فرزند شما در یادگیری عالی عمل کرده است! او تقریباً بر تمام کلمات مسلط شده و آماده چالش‌های بزرگتر است."
            progressPercent > 50 -> "پیشرفت بسیار خوبی حاصل شده. تمرکز بر کلمات جدید و مرور مستمر به تثبیت یادگیری کمک می‌کند."
            progressPercent > 20 -> "سفر یادگیری به خوبی در حال انجام است. تکرار و تمرین کلید موفقیت در این مرحله است."
            else -> "در ابتدای راه هستیم. با تشویق و همراهی، فرزندتان به زودی کلمات بیشتری را یاد خواهد گرفت."
        }
    }

    fun seedData() {
        viewModelScope.launch {
            val alphabetData = listOf(
                LearningItem("a01", "آ", "آب", "آب", "img_a1", "ALPHABET"),
                LearningItem("a02", "ب", "بابا", "بابا", "img_a2", "ALPHABET"),
                LearningItem("a03", "د", "دست", "دست", "img_a3", "ALPHABET"),
                LearningItem("a04", "م", "مادر", "مادر", "img_a4", "ALPHABET"),
                LearningItem("a05", "س", "سبد", "سبد", "img_a5", "ALPHABET"),
                LearningItem("a06", "ا", "انار", "انار", "img_a6", "ALPHABET"),
                LearningItem("a07", "ر", "رنگ", "رنگ", "img_a7", "ALPHABET"),
                LearningItem("a08", "ن", "نان", "نان", "img_a8", "ALPHABET"),
                LearningItem("a09", "ز", "زنبور", "زنبور", "img_a9", "ALPHABET"),
                LearningItem("a10", "ت", "تاب", "تاب", "img_a10", "ALPHABET"),
            )

            val orderedNumbers = listOf(1, 2, 3, 4, 5, 6, 7, 8, 9, 0)
            val numberItems = orderedNumbers.mapIndexed { index, num ->
                val word = num.toPersianWord()
                val idSuffix = if (num == 0) "10" else String.format("%02d", num)
                LearningItem("n$idSuffix", num.toString().toPersianDigit(), word, word, "img_n$num", "NUMBER")
            }

            repository.insertInitialData(alphabetData + numberItems)
        }
    }

    private fun String.toPersianDigit() = this.replace('0','۰').replace('1','۱').replace('2','۲').replace('3','۳').replace('4','۴').replace('5','۵').replace('6','۶').replace('7','۷').replace('8','۸').replace('9','۹')
    private fun Int.toPersianWord() = listOf("صفر","یک","دو","سه","چهار","پنج","شش","هفت","هشت","نه")[this]

    override fun onCleared() {
        super.onCleared()
        audioManager.release()
        ttsManager.release()
    }
}
