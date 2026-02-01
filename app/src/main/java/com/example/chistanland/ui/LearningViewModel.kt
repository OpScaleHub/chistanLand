package com.github.opscalehub.chistanland.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.github.opscalehub.chistanland.data.AppDatabase
import com.github.opscalehub.chistanland.data.LearningItem
import com.github.opscalehub.chistanland.data.LearningRepository
import com.github.opscalehub.chistanland.util.AudioManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class LearningViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: LearningRepository = LearningRepository(
        AppDatabase.getDatabase(application).learningDao()
    )
    private val audioManager = AudioManager(application)
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
        generateAdaptiveKeyboard(item)

        narrativeJob = viewModelScope.launch {
            _avatarState.value = "SPEAKING"
            // Wait for narrative to finish before playing phonetics
            audioManager.playSound("inst_type_word")
            delay(400) // Natural pause between sentences
            audioManager.playSound(item.phonetic)
            _avatarState.value = "IDLE"
        }
    }

    fun startReviewSession(allowedItems: List<LearningItem>? = null, onReady: () -> Unit = {}) {
        val currentCat = _selectedCategory.value ?: "ALPHABET"
        viewModelScope.launch {
            val itemsToReview = repository.getItemsToReviewByCategory(currentCat).first()

            val basePool = if (allowedItems != null) {
                itemsToReview.filter { item -> allowedItems.any { it.id == item.id } }
            } else {
                itemsToReview
            }

            val pool = if (basePool.isNotEmpty()) {
                basePool
            } else {
                val fallbackPool = allowedItems ?: allItems.value.filter { it.category == currentCat }
                fallbackPool.filter { it.level > 1 || it.isMastered }
            }

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
            audioManager.playSound("inst_find_char")
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

            val totalPool = (wordChars + accessibleLetters).toList().shuffled()
            _keyboardKeys.value = totalPool.take(12).shuffled()
        }
    }

    fun onCharTyped(char: String) {
        val current = _currentItem.value ?: return
        val targetFullString = if (current.category == "NUMBER") current.character else current.word
        if (_typedText.value.length >= targetFullString.length) return
        val targetChar = targetFullString[_typedText.value.length].toString()

        if (char == targetChar) {
            _typedText.value += char
            _charStatus.value = _charStatus.value + true
            _avatarState.value = "HAPPY"
            // SFX can be async as they don't block narrative flow
            audioManager.playSoundAsync("pop_sound")
            if (_typedText.value.length == targetFullString.length) {
                completeLevel(!hasErrorInCurrentWord)
            }
        } else {
            hasErrorInCurrentWord = true
            _avatarState.value = "THINKING"
            viewModelScope.launch {
                _uiEvent.emit(UiEvent.Error)
                delay(1200)
                if (_avatarState.value == "THINKING") _avatarState.value = "IDLE"
            }
            audioManager.playSoundAsync("error_sound")
        }
    }

    private fun completeLevel(isCorrect: Boolean) {
        narrativeJob?.cancel()
        narrativeJob = viewModelScope.launch {
            val item = _currentItem.value ?: return@launch
            repository.updateProgress(item, isCorrect)
            if (isCorrect) {
                _streak.value += 1
                // Play success music and show celebration simultaneously
                launch { _uiEvent.emit(UiEvent.Success) }
                audioManager.playSound("success_fest")
            }
            delay(1000) // Celebration time
            _currentItem.value = null
            _avatarState.value = "IDLE"
        }
    }

    fun seedData() {
        viewModelScope.launch {
            val cumulativeAlphabet = listOf(
                LearningItem("a1", "ب", "آب", "audio_a1", "img_a1", "ALPHABET"),
                LearningItem("a2", "ا", "بابا", "audio_a2", "img_a2", "ALPHABET"),
                LearningItem("a3", "د", "باد", "audio_a3", "img_a3", "ALPHABET"),
                LearningItem("a4", "م", "بام", "audio_a4", "img_a4", "ALPHABET"),
                LearningItem("a5", "س", "سبد", "audio_a5", "img_a5", "ALPHABET"),
                LearningItem("a6", "ن", "نان", "audio_a6", "img_a6", "ALPHABET"),
                LearningItem("a7", "ر", "ابر", "audio_a7", "img_a7", "ALPHABET"),
                LearningItem("a8", "ت", "دست", "audio_a8", "img_a8", "ALPHABET"),
                LearningItem("a9", "و", "بوم", "audio_a9", "img_a9", "ALPHABET"),
                LearningItem("a10", "ی", "سیب", "audio_a10", "img_a10", "ALPHABET"),
                LearningItem("a11", "ز", "باز", "audio_a11", "img_a11", "ALPHABET"),
                LearningItem("a12", "ش", "آش", "audio_a12", "img_a12", "ALPHABET"),
                LearningItem("a13", "ک", "کتاب", "audio_a13", "img_a13", "ALPHABET"),
                LearningItem("a14", "گ", "سگ", "audio_a14", "img_a14", "ALPHABET"),
                LearningItem("a15", "ف", "برف", "audio_a15", "img_a15", "ALPHABET"),
                LearningItem("a16", "خ", "شاخ", "audio_a16", "img_a16", "ALPHABET"),
                LearningItem("a17", "ق", "قایق", "audio_a17", "img_a17", "ALPHABET"),
                LearningItem("a18", "ل", "لباس", "audio_a18", "img_a18", "ALPHABET"),
                LearningItem("a19", "ج", "تاج", "audio_a19", "img_a19", "ALPHABET"),
                LearningItem("a20", "چ", "چای", "audio_a20", "img_a20", "ALPHABET"),
                LearningItem("a21", "ه", "کوه", "audio_a21", "img_a21", "ALPHABET"),
                LearningItem("a22", "ژ", "ژله", "audio_a22", "img_a22", "ALPHABET"),
                LearningItem("a23", "ص", "صورت", "audio_a23", "img_a23", "ALPHABET"),
                LearningItem("a24", "ذ", "ذرت", "audio_a24", "img_a24", "ALPHABET"),
                LearningItem("a25", "ع", "عینک", "audio_a25", "img_a25", "ALPHABET"),
                LearningItem("a26", "ث", "ثروت", "audio_a26", "img_a26", "ALPHABET"),
                LearningItem("a27", "ح", "حلزون", "audio_a27", "img_a27", "ALPHABET"),
                LearningItem("a28", "ض", "ضامن", "audio_a28", "img_a28", "ALPHABET"),
                LearningItem("a29", "ط", "طوطی", "audio_a29", "img_a29", "ALPHABET"),
                LearningItem("a30", "غ", "غذا", "audio_a30", "img_a30", "ALPHABET"),
                LearningItem("a31", "ظ", "ظرف", "audio_a31", "img_a31", "ALPHABET")
            )

            val orderedNumbers = listOf(1, 2, 3, 4, 5, 6, 7, 8, 9, 0)
            val numberItems = orderedNumbers.mapIndexed { index, num ->
                LearningItem("n$num", num.toString().toPersianDigit(), num.toPersianWord(), "audio_n$num", "img_n$num", "NUMBER")
            }

            repository.insertInitialData(cumulativeAlphabet + numberItems)
        }
    }

    private fun String.toPersianDigit() = this.replace('0','۰').replace('1','۱').replace('2','۲').replace('3','۳').replace('4','۴').replace('5','۵').replace('6','۶').replace('7','۷').replace('8','۸').replace('9','۹')
    private fun Int.toPersianWord() = listOf("صفر","یک","دو","سه","چهار","پنج","شش","هفت","هشت","نه")[this]

    override fun onCleared() {
        super.onCleared()
        audioManager.release()
    }
}
