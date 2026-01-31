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
import kotlin.random.Random

class LearningViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: LearningRepository
    private val audioManager = AudioManager(application)

    val allItems: StateFlow<List<LearningItem>>
    val reviewItems: StateFlow<List<LearningItem>>

    private val _currentItem = MutableStateFlow<LearningItem?>(null)
    val currentItem: StateFlow<LearningItem?> = _currentItem.asStateFlow()

    private val _typedText = MutableStateFlow("")
    val typedText: StateFlow<String> = _typedText.asStateFlow()

    private val _charStatus = MutableStateFlow<List<Boolean>>(emptyList())
    val charStatus: StateFlow<List<Boolean>> = _charStatus.asStateFlow()

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

    init {
        val dao = AppDatabase.getDatabase(application).learningDao()
        repository = LearningRepository(dao)
        
        allItems = repository.allItems.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
        reviewItems = repository.getItemsToReview().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    }

    fun startLearning(item: LearningItem) {
        _currentItem.value = item
        _typedText.value = ""
        _charStatus.value = emptyList()
        generateAdaptiveKeyboard(item.word)
        playSound(item.phonetic)
    }

    private fun generateAdaptiveKeyboard(word: String) {
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
        val isCorrect = char == targetChar

        _typedText.value += char
        _charStatus.value = _charStatus.value + isCorrect

        if (isCorrect) {
            playSound("pop_sound")
            // Play phonics for the typed character if it's correct
            // In a real app, we'd have audio files for each letter: playSound("letter_${char}")
        } else {
            viewModelScope.launch { _uiEvent.emit(UiEvent.Error) }
            playSound("error_sound")
        }

        if (_typedText.value.length == current.word.length) {
            val allCorrect = _charStatus.value.all { it }
            if (allCorrect) {
                _streak.value += 1
                completeLevel(true)
            } else {
                _streak.value = 0
                completeLevel(false)
            }
        }
    }

    private fun playSound(sound: String) {
        audioManager.playSound(sound)
    }

    private fun completeLevel(isCorrect: Boolean) {
        viewModelScope.launch {
            val item = _currentItem.value ?: return@launch
            
            if (isCorrect) {
                _uiEvent.emit(UiEvent.Success)
            } else {
                if (item.level > 1) {
                    _uiEvent.emit(UiEvent.LevelDown)
                }
            }

            repository.updateProgress(item, isCorrect)
            
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
        val inProgress = items.count { it.level in 2..4 }
        
        return when {
            mastered == items.size -> "تبریک! فرزند شما تمام جزیره‌ها را فتح کرده و اکنون یک استاد بزرگ است."
            mastered > 0 -> "فرزند شما با پشتکار $mastered حرف را به قصر طلایی رسانده است. او به خوبی ریتم تایپ کلمات را درک کرده است."
            else -> "ماجراجویی آغاز شده! فرزند شما در حال اکتشاف در اولین جزیره‌هاست."
        }
    }

    fun seedData() {
        viewModelScope.launch {
            val initialItems = listOf(
                LearningItem("1", "آ", "آب", "ab_audio", "ab_img"),
                LearningItem("2", "ب", "بابا", "baba_audio", "baba_img"),
                LearningItem("3", "پ", "پا", "pa_audio", "pa_img"),
                LearningItem("4", "ت", "توت", "toot_audio", "toot_img"),
                LearningItem("5", "ث", "ثروت", "servat_audio", "servat_img"),
                LearningItem("6", "ج", "جوجه", "jooje_audio", "jooje_img"),
                LearningItem("7", "چ", "چای", "chai_audio", "chai_img"),
                LearningItem("8", "ح", "حلزون", "halazoon_audio", "halazoon_img"),
                LearningItem("9", "خ", "خرگوش", "khargoosh_audio", "khargoosh_img"),
                LearningItem("10", "د", "درخت", "derakht_audio", "derakht_img")
            )
            repository.insertInitialData(initialItems)
        }
    }

    override fun onCleared() {
        super.onCleared()
        audioManager.release()
    }
}
