package com.example.chistanland.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.chistanland.data.AppDatabase
import com.example.chistanland.data.LearningItem
import com.example.chistanland.data.LearningRepository
import com.example.chistanland.util.AudioManager
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class LearningViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: LearningRepository = LearningRepository(
        AppDatabase.getDatabase(application).learningDao()
    )
    private val audioManager = AudioManager(application)

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

    // Now tracking only correct steps to show green dots
    private val _charStatus = MutableStateFlow<List<Boolean>>(emptyList())
    val charStatus: StateFlow<List<Boolean>> = _charStatus.asStateFlow()

    // To keep track if the child made any mistake for the current word
    private var hasErrorInCurrentWord: Boolean = false

    private val _streak = MutableStateFlow(0)
    val streak: StateFlow<Int> = _streak.asStateFlow()

    private val _keyboardKeys = MutableStateFlow<List<String>>(emptyList())
    val keyboardKeys: StateFlow<List<String>> = _keyboardKeys.asStateFlow()

    private val _uiEvent = MutableSharedFlow<UiEvent>()
    val uiEvent = _uiEvent.asSharedFlow()

    sealed class UiEvent {
        object Error : UiEvent()
        object Success : UiEvent()
        object LevelDown : UiEvent()
    }

    private val fullAlphabet = listOf(
        "آ", "ا", "ب", "پ", "ت", "ث", "ج", "چ", "ح", "خ", "د", "ذ", "ر", "ز", "ژ", "س", "ش", "ص", "ض", "ط", "ظ", "ع", "غ", "ف", "ق", "ک", "گ", "ل", "م", "ن", "و", "ه", "ی"
    )

    fun selectCategory(category: String?) {
        _selectedCategory.value = category
    }

    fun startLearning(item: LearningItem) {
        _currentItem.value = item
        _typedText.value = ""
        _charStatus.value = emptyList()
        hasErrorInCurrentWord = false
        generateAdaptiveKeyboard(item.word, item.category)
        playSound(item.phonetic)
    }

    private fun generateAdaptiveKeyboard(word: String, category: String) {
        // Uniform logic for both categories: Keyboard only contains alphabet letters to spell names
        val wordChars = word.map { it.toString() }.toSet()
        val distractorsCount = (10 - wordChars.size).coerceAtLeast(4)
        val distractors = fullAlphabet.filterNot { wordChars.contains(it) }
            .shuffled()
            .take(distractorsCount)
        
        _keyboardKeys.value = (wordChars + distractors).shuffled()
    }

    fun onCharTyped(char: String) {
        val current = _currentItem.value ?: return
        if (_typedText.value.length >= current.word.length) return

        val targetChar = current.word[_typedText.value.length].toString()
        
        if (char == targetChar) {
            // Correct hit: Advance
            _typedText.value += char
            _charStatus.value = _charStatus.value + true
            playSound("pop_sound")

            // Check if word is finished
            if (_typedText.value.length == current.word.length) {
                if (!hasErrorInCurrentWord) {
                    _streak.value += 1
                    completeLevel(true)
                } else {
                    _streak.value = 0
                    completeLevel(false)
                }
            }
        } else {
            // Wrong hit: Shake and block
            hasErrorInCurrentWord = true
            viewModelScope.launch { _uiEvent.emit(UiEvent.Error) }
            playSound("error_sound")
        }
    }

    private fun playSound(sound: String) {
        audioManager.playSound(sound)
    }

    private fun completeLevel(isCorrect: Boolean) {
        viewModelScope.launch {
            val item = _currentItem.value ?: return@launch
            repository.updateProgress(item, isCorrect)
            
            if (isCorrect) {
                _uiEvent.emit(UiEvent.Success)
            } else {
                if (item.level > 1) {
                    _uiEvent.emit(UiEvent.LevelDown)
                }
            }
            
            kotlinx.coroutines.delay(2000)
            
            _currentItem.value = null
            _typedText.value = ""
            _charStatus.value = emptyList()
            _keyboardKeys.value = emptyList()
        }
    }

    fun getParentNarrative(): String {
        val items = allItems.value
        if (items.isEmpty()) return "هنوز ماجراجویی شروع نشده است!"
        
        val mastered = items.count { it.isMastered }
        return "فرزند شما $mastered مورد را به حافظه بلندمدت سپرده است."
    }

    fun seedData() {
        viewModelScope.launch {
            val alphabetItems = listOf(
                LearningItem("a1", "آ", "آب", "audio_a1", "img_a1", "ALPHABET"),
                LearningItem("a2", "ا", "اسب", "audio_a2", "img_a2", "ALPHABET"),
                LearningItem("a3", "ب", "بابا", "audio_a3", "img_a3", "ALPHABET"),
                LearningItem("a4", "پ", "پا", "audio_a4", "img_a4", "ALPHABET"),
                LearningItem("a5", "ت", "توت", "audio_a5", "img_a5", "ALPHABET"),
                LearningItem("a6", "ث", "ثروت", "audio_a6", "img_a6", "ALPHABET"),
                LearningItem("a7", "ج", "جوجه", "audio_a7", "img_a7", "ALPHABET"),
                LearningItem("a8", "چ", "چای", "audio_a8", "img_a8", "ALPHABET"),
                LearningItem("a9", "ح", "حلزون", "audio_a9", "img_a9", "ALPHABET"),
                LearningItem("a10", "خ", "خرگوش", "audio_a10", "img_a10", "ALPHABET"),
                LearningItem("a11", "د", "درخت", "audio_a11", "img_a11", "ALPHABET"),
                LearningItem("a12", "ذ", "ذرت", "audio_a12", "img_a12", "ALPHABET"),
                LearningItem("a13", "ر", "روباه", "audio_a13", "img_a13", "ALPHABET"),
                LearningItem("a14", "ز", "زنبور", "audio_a14", "img_a14", "ALPHABET"),
                LearningItem("a15", "ژ", "ژله", "audio_a15", "img_a15", "ALPHABET"),
                LearningItem("a16", "س", "سیب", "audio_a16", "img_a16", "ALPHABET"),
                LearningItem("a17", "ش", "شتر", "audio_a17", "img_a17", "ALPHABET"),
                LearningItem("a18", "ص", "صورت", "audio_a18", "img_a18", "ALPHABET"),
                LearningItem("a19", "ض", "ضامن", "audio_a19", "img_a19", "ALPHABET"),
                LearningItem("a20", "ط", "طوطی", "audio_a20", "img_a20", "ALPHABET"),
                LearningItem("a21", "ظ", "ظرف", "audio_a21", "img_a21", "ALPHABET"),
                LearningItem("a22", "ع", "عینک", "audio_a22", "img_a22", "ALPHABET"),
                LearningItem("a23", "غ", "غذا", "audio_a23", "img_a23", "ALPHABET"),
                LearningItem("a24", "ف", "فیل", "audio_a24", "img_a24", "ALPHABET"),
                LearningItem("a25", "ق", "قایق", "audio_a25", "img_a25", "ALPHABET"),
                LearningItem("a26", "ک", "کتاب", "audio_a26", "img_a26", "ALPHABET"),
                LearningItem("a27", "گ", "گاو", "audio_a27", "img_a27", "ALPHABET"),
                LearningItem("a28", "ل", "لباس", "audio_a28", "img_a28", "ALPHABET"),
                LearningItem("a29", "م", "موز", "audio_a29", "img_a29", "ALPHABET"),
                LearningItem("a30", "ن", "نان", "audio_a30", "img_a30", "ALPHABET"),
                LearningItem("a31", "و", "ورزش", "audio_a31", "img_a31", "ALPHABET"),
                LearningItem("a32", "ه", "هلو", "audio_a32", "img_a32", "ALPHABET"),
                LearningItem("a33", "ی", "یخ", "audio_a33", "img_a33", "ALPHABET")
            )
            
            val numberItems = listOf(
                LearningItem("n0", "۰", "صفر", "audio_n0", "img_n0", "NUMBER"),
                LearningItem("n1", "۱", "یک", "audio_n1", "img_n1", "NUMBER"),
                LearningItem("n2", "۲", "دو", "audio_n2", "img_n2", "NUMBER"),
                LearningItem("n3", "۳", "سه", "audio_n3", "img_n3", "NUMBER"),
                LearningItem("n4", "۴", "چهار", "audio_n4", "img_n4", "NUMBER"),
                LearningItem("n5", "۵", "پنج", "audio_n5", "img_n5", "NUMBER"),
                LearningItem("n6", "۶", "شش", "audio_n6", "img_n6", "NUMBER"),
                LearningItem("n7", "۷", "هفت", "audio_n7", "img_n7", "NUMBER"),
                LearningItem("n8", "۸", "هشت", "audio_n8", "img_n8", "NUMBER"),
                LearningItem("n9", "۹", "نه", "audio_n9", "img_n9", "NUMBER")
            )
            
            repository.insertInitialData(alphabetItems + numberItems)
        }
    }

    override fun onCleared() {
        super.onCleared()
        audioManager.release()
    }
}
