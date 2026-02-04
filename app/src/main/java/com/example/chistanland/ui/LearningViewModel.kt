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

    private val _activityType = MutableStateFlow(ActivityType.SPELLING)
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

    enum class ActivityType { SPELLING, MISSING_LETTER }

    sealed class UiEvent {
        object Error : UiEvent()
        object Success : UiEvent()
        object LevelDown : UiEvent()
        object StartReviewSession : UiEvent()
    }

    private val persianDigits = listOf("۱", "۲", "۳", "۴", "۵", "۶", "۷", "۸", "۹", "۰")

    fun selectCategory(category: String?) {
        _selectedCategory.value = category
    }

    fun getParentNarrative(): String {
        val items = allItems.value
        if (items.isEmpty()) return "در حال بارگذاری اطلاعات..."
        val masteredTotal = items.count { it.isMastered }
        return "فرزند شما تاکنون ${masteredTotal.toString().toPersianDigit()} نشانه را به خوبی یاد گرفته است."
    }

    fun startLearning(item: LearningItem, isReview: Boolean = false) {
        narrativeJob?.cancel()
        _isReviewMode.value = isReview
        _currentItem.value = item
        _typedText.value = ""
        _charStatus.value = emptyList()
        hasErrorInCurrentWord = false
        
        // Decide Activity Type: 40% chance for "Missing Letter" if level > 2
        _activityType.value = if (!isReview && item.level > 2 && Random.nextFloat() < 0.4f) {
            ActivityType.MISSING_LETTER
        } else {
            ActivityType.SPELLING
        }

        if (_activityType.value == ActivityType.MISSING_LETTER) {
            _missingCharIndex.value = Random.nextInt(item.word.length)
        } else {
            _missingCharIndex.value = -1
        }

        generateAdaptiveKeyboard(item)

        narrativeJob = viewModelScope.launch {
            _avatarState.value = "SPEAKING"
            val prefix = if (item.category == "NUMBER") "بنویس عدد " else "بنویس کلمه "
            val instruction = when (_activityType.value) {
                ActivityType.SPELLING -> "$prefix ${item.word}"
                ActivityType.MISSING_LETTER -> "کدوم نشانه توی این کلمه گم شده؟"
            }
            ttsManager.speak(instruction)
            _avatarState.value = "IDLE"
        }
    }

    fun onCharTyped(char: String) {
        val current = _currentItem.value ?: return
        val targetFullString = if (current.category == "NUMBER") current.character else current.word
        
        if (_activityType.value == ActivityType.MISSING_LETTER) {
            val targetChar = targetFullString[_missingCharIndex.value].toString()
            if (char == targetChar) {
                _typedText.value = targetFullString
                _charStatus.value = List(targetFullString.length) { true }
                completeLevel(true)
            } else {
                handleError()
            }
        } else {
            if (_typedText.value.length >= targetFullString.length) return
            val targetChar = targetFullString[_typedText.value.length].toString()

            if (char == targetChar) {
                _typedText.value += char
                _charStatus.value = _charStatus.value + true
                _avatarState.value = "HAPPY"
                audioManager.playSoundAsync("pop_sound")
                if (_typedText.value.length == targetFullString.length) {
                    completeLevel(!hasErrorInCurrentWord)
                }
            } else {
                handleError()
            }
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
                ttsManager.speak("آفرین! خیلی عالی بود")
            }
            delay(1000)
            _currentItem.value = null
            _avatarState.value = "IDLE"
        }
    }

    private fun generateAdaptiveKeyboard(item: LearningItem) {
        if (item.category == "NUMBER") {
            _keyboardKeys.value = persianDigits
        } else {
            val wordChars = item.word.map { it.toString() }.toSet()
            val currentItems = filteredItems.value
            val currentIndex = currentItems.indexOfFirst { it.id == item.id }

            val accessibleLetters = if (currentIndex >= 0) {
                currentItems.take(currentIndex + 1).flatMap { it.word.map { c -> c.toString() } }.toSet()
            } else {
                currentItems.filter { it.level > 1 || it.isMastered }.flatMap { it.word.map { c -> c.toString() } }.toSet()
            }

            // Ensure the target char for missing letter is always in the pool
            val targetPool = (wordChars + accessibleLetters).toList().shuffled()
            _keyboardKeys.value = targetPool.take(12).shuffled()
        }
    }

    fun startReviewSession(allowedItems: List<LearningItem>? = null, onReady: () -> Unit = {}) {
        viewModelScope.launch {
            val currentCat = _selectedCategory.value ?: "ALPHABET"
            val itemsToReview = repository.getItemsToReviewByCategory(currentCat).first()
            val pool = itemsToReview.ifEmpty { allItems.value.filter { it.category == currentCat } }
            val selected = pool.randomOrNull()
            if (selected != null) {
                startLearning(selected, isReview = true)
                _uiEvent.emit(UiEvent.StartReviewSession)
                onReady()
            }
        }
    }

    fun playHintInstruction() {
        narrativeJob?.cancel()
        narrativeJob = viewModelScope.launch {
            _avatarState.value = "SPEAKING"
            ttsManager.speak("نشانه مورد نظر رو پیدا کن")
            _avatarState.value = "IDLE"
        }
    }

    fun seedData() {
        viewModelScope.launch {
            val cumulativeAlphabet = listOf(
                LearningItem("a1", "ب", "آب", "آب", "img_a1", "ALPHABET"),
                LearningItem("a2", "ا", "بابا", "بابا", "img_a2", "ALPHABET"),
                LearningItem("a3", "د", "باد", "باد", "img_a3", "ALPHABET"),
                LearningItem("a4", "م", "بام", "بام", "img_a4", "ALPHABET"),
                LearningItem("a5", "س", "سبد", "سبد", "img_a5", "ALPHABET"),
                LearningItem("a6", "ن", "نان", "نان", "img_a6", "ALPHABET"),
                LearningItem("a7", "ر", "ابر", "ابر", "img_a7", "ALPHABET"),
                LearningItem("a8", "ت", "دست", "دست", "img_a8", "ALPHABET"),
                LearningItem("a9", "و", "بوم", "بوم", "img_a9", "ALPHABET"),
                LearningItem("a10", "ی", "سیب", "سیب", "img_a10", "ALPHABET"),
                LearningItem("a11", "ز", "باز", "باز", "img_a11", "ALPHABET"),
                LearningItem("a12", "ش", "آش", "آش", "img_a12", "ALPHABET"),
                LearningItem("a13", "ک", "کتاب", "کتاب", "img_a13", "ALPHABET"),
                LearningItem("a14", "گ", "سگ", "سگ", "img_a14", "ALPHABET"),
                LearningItem("a15", "ف", "برف", "برف", "img_a15", "ALPHABET"),
                LearningItem("a16", "خ", "شاخ", "شاخ", "img_a16", "ALPHABET"),
                LearningItem("a17", "ق", "قایق", "قایق", "img_a17", "ALPHABET"),
                LearningItem("a18", "ل", "لباس", "لباس", "img_a18", "ALPHABET"),
                LearningItem("a19", "ج", "تاج", "تاج", "img_a19", "ALPHABET"),
                LearningItem("a20", "چ", "چای", "چای", "img_a20", "ALPHABET"),
                LearningItem("a21", "ه", "کوه", "کوه", "img_a21", "ALPHABET"),
                LearningItem("a22", "ژ", "ژله", "ژله", "img_a22", "ALPHABET"),
                LearningItem("a23", "ص", "صورت", "صورت", "img_a23", "ALPHABET"),
                LearningItem("a24", "ذ", "ذرت", "ذرت", "img_a24", "ALPHABET"),
                LearningItem("a25", "ع", "عینک", "عینک", "img_a25", "ALPHABET"),
                LearningItem("a26", "ث", "ثروت", "ثروت", "img_a26", "ALPHABET"),
                LearningItem("a27", "ح", "حلزون", "حلزون", "img_a27", "ALPHABET"),
                LearningItem("a28", "ض", "ضامن", "ضامن", "img_a28", "ALPHABET"),
                LearningItem("a29", "ط", "طوطی", "طوطی", "img_a29", "ALPHABET"),
                LearningItem("a30", "غ", "غذا", "غذا", "img_a30", "ALPHABET"),
                LearningItem("a31", "ظ", "ظرف", "ظرف", "img_a31", "ALPHABET")
            )

            val orderedNumbers = listOf(1, 2, 3, 4, 5, 6, 7, 8, 9, 0)
            val numberItems = orderedNumbers.mapIndexed { index, num ->
                val word = num.toPersianWord()
                LearningItem("n$num", num.toString().toPersianDigit(), word, word, "img_n$num", "NUMBER")
            }

            repository.insertInitialData(cumulativeAlphabet + numberItems)
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
