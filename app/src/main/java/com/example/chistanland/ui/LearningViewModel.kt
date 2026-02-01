package com.example.chistanland.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.chistanland.data.AppDatabase
import com.example.chistanland.data.LearningItem
import com.example.chistanland.data.LearningRepository
import com.example.chistanland.util.AudioManager
import kotlinx.coroutines.delay
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

    private val fullAlphabet = listOf(
        "آ", "ا", "ب", "پ", "ت", "ث", "ج", "چ", "ح", "خ", "د", "ذ", "ر", "ز", "ژ", "س", "ش", "ص", "ض", "ط", "ظ", "ع", "غ", "ف", "ق", "ک", "گ", "ل", "م", "ن", "و", "ه", "ی"
    )
    
    private val persianDigits = listOf("۰", "۱", "۲", "۳", "۴", "۵", "۶", "۷", "۸", "۹")

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
        _isReviewMode.value = isReview
        _currentItem.value = item
        _typedText.value = ""
        _charStatus.value = emptyList()
        hasErrorInCurrentWord = false
        generateAdaptiveKeyboard(item)
        
        viewModelScope.launch {
            _avatarState.value = "SPEAKING"
            playSound("inst_type_word") 
            delay(1500)
            playSound(item.phonetic)
            delay(1000)
            _avatarState.value = "IDLE"
        }
    }

    fun startReviewSession(allowedItems: List<LearningItem>? = null) {
        val currentCat = _selectedCategory.value ?: "ALPHABET"
        viewModelScope.launch {
            val itemsToReview = repository.getItemsToReviewByCategory(currentCat).first()
            
            // اگر لیست آیتم‌های مجاز ارسال شده باشد، فقط از آن‌ها استفاده می‌کنیم
            val basePool = if (allowedItems != null) {
                itemsToReview.filter { it.id in allowedItems.map { it.id } }
            } else {
                itemsToReview
            }

            // اگر آیتمی برای مرور زمان‌بندی شده نبود، از کل آیتم‌های مجاز که حداقل یک بار یاد گرفته شده‌اند انتخاب کن
            val pool = if (basePool.isNotEmpty()) {
                basePool
            } else {
                val fallbackPool = allowedItems ?: allItems.value.filter { it.category == currentCat }
                fallbackPool.filter { it.level > 1 }
            }
            
            pool.randomOrNull()?.let {
                startLearning(it, isReview = true)
                _uiEvent.emit(UiEvent.StartReviewSession)
            }
        }
    }

    fun playHintInstruction() {
        viewModelScope.launch {
            _avatarState.value = "SPEAKING"
            playSound("inst_find_char") 
            delay(1000)
            _avatarState.value = "IDLE"
        }
    }

    private fun generateAdaptiveKeyboard(item: LearningItem) {
        if (item.category == "NUMBER") {
            _keyboardKeys.value = persianDigits
        } else {
            val wordChars = item.word.map { it.toString() }.toSet()
            
            val currentIndex = allItems.value.indexOfFirst { it.id == item.id }
            val knownOrPreviousItems = if (currentIndex >= 0) {
                allItems.value.take(currentIndex + 1)
            } else {
                allItems.value.filter { it.level > 1 }
            }
            
            val basePool = knownOrPreviousItems.map { it.character }.toSet()
            
            val futureItems = if (currentIndex >= 0) allItems.value.drop(currentIndex + 1) else emptyList()
            val futureChars = futureItems.map { it.character }.toSet()
            
            val distractors = fullAlphabet
                .filter { it in basePool || it !in futureChars }
                .filterNot { wordChars.contains(it) }
                .shuffled()
                .take(6)
                
            _keyboardKeys.value = (wordChars + distractors).shuffled()
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
            playSound("pop_sound")
            if (_typedText.value.length == targetFullString.length) {
                completeLevel(!hasErrorInCurrentWord)
            }
        } else {
            hasErrorInCurrentWord = true
            _avatarState.value = "THINKING"
            viewModelScope.launch { 
                _uiEvent.emit(UiEvent.Error)
                delay(1000)
                _avatarState.value = "IDLE"
            }
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
                _streak.value += 1
                playSound("success_fest")
                _uiEvent.emit(UiEvent.Success)
            }
            delay(2000)
            _currentItem.value = null
            _avatarState.value = "IDLE"
        }
    }

    fun seedData() {
        viewModelScope.launch {
            val pedagogicalAlphabet = listOf(
                LearningItem("a1", "آ", "آب", "audio_a1", "img_a1", "ALPHABET"),
                LearningItem("a2", "ا", "اسب", "audio_a2", "img_a2", "ALPHABET"),
                LearningItem("a3", "ب", "بابا", "audio_a3", "img_a3", "ALPHABET"),
                LearningItem("a11", "د", "درخت", "audio_a11", "img_a11", "ALPHABET"),
                LearningItem("a29", "م", "موز", "audio_a29", "img_a29", "ALPHABET"),
                LearningItem("a16", "س", "سیب", "audio_a16", "img_a16", "ALPHABET"),
                LearningItem("a5", "ت", "توت", "audio_a5", "img_a5", "ALPHABET"),
                LearningItem("a13", "ر", "روباه", "audio_a13", "img_a13", "ALPHABET"),
                LearningItem("a30", "ن", "نان", "audio_a30", "img_a30", "ALPHABET"),
                LearningItem("a14", "ز", "زنبور", "audio_a14", "img_a14", "ALPHABET"),
                LearningItem("a17", "ش", "شتر", "audio_a17", "img_a17", "ALPHABET"),
                LearningItem("a26", "ک", "کتاب", "audio_a26", "img_a26", "ALPHABET"),
                LearningItem("a31", "و", "ورزش", "audio_a31", "img_a31", "ALPHABET"),
                LearningItem("a4", "پ", "پا", "audio_a4", "img_a4", "ALPHABET"),
                LearningItem("a27", "گ", "گاو", "audio_a27", "img_a27", "ALPHABET"),
                LearningItem("a24", "ف", "فیل", "audio_a24", "img_a24", "ALPHABET"),
                LearningItem("a10", "خ", "خرگوش", "audio_a10", "img_a10", "ALPHABET"),
                LearningItem("a25", "ق", "قایق", "audio_a25", "img_a25", "ALPHABET"),
                LearningItem("a28", "ل", "لباس", "audio_a28", "img_a28", "ALPHABET"),
                LearningItem("a7", "ج", "جوجه", "audio_a7", "img_a7", "ALPHABET"),
                LearningItem("a32", "ه", "هلو", "audio_a32", "img_a32", "ALPHABET"),
                LearningItem("a8", "چ", "چای", "audio_a8", "img_a8", "ALPHABET"),
                LearningItem("a15", "ژ", "ژله", "audio_a15", "img_a15", "ALPHABET"),
                
                LearningItem("a18", "ص", "صورت", "audio_a18", "img_a18", "ALPHABET"),
                LearningItem("a12", "ذ", "ذرت", "audio_a12", "img_a12", "ALPHABET"),
                LearningItem("a22", "ع", "عینک", "audio_a22", "img_a22", "ALPHABET"),
                LearningItem("a6", "ث", "ثروت", "audio_a6", "img_a6", "ALPHABET"),
                LearningItem("a9", "ح", "حلزون", "audio_a9", "img_a9", "ALPHABET"),
                LearningItem("a19", "ض", "ضامن", "audio_a19", "img_a19", "ALPHABET"),
                LearningItem("a20", "ط", "طوطی", "audio_a20", "img_a20", "ALPHABET"),
                LearningItem("a23", "غ", "غذا", "audio_a23", "img_a23", "ALPHABET"),
                LearningItem("a21", "ظ", "ظرف", "audio_a21", "img_a21", "ALPHABET"),
                LearningItem("a33", "ی", "یخ", "audio_a33", "img_a33", "ALPHABET")
            )
            
            val numberItems = (0..9).map { 
                LearningItem("n$it", it.toString().toPersianDigit(), it.toPersianWord(), "audio_n$it", "img_n$it", "NUMBER") 
            }
            
            repository.insertInitialData(pedagogicalAlphabet + numberItems)
        }
    }

    private fun String.toPersianDigit() = this.replace('0','۰').replace('1','۱').replace('2','۲').replace('3','۳').replace('4','۴').replace('5','۵').replace('6','۶').replace('7','۷').replace('8','۸').replace('9','۹')
    private fun Int.toPersianWord() = listOf("صفر","یک","دو","سه","چهار","پنج","شش","هفت","هشت","نه")[this]

    override fun onCleared() {
        super.onCleared()
        audioManager.release()
    }
}
