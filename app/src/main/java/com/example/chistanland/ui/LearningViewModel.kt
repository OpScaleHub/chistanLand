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
            val currentItems = filteredItems.value
            val currentIndex = currentItems.indexOfFirst { it.id == item.id }
            
            // در روش تجمعی، کیبورد فقط حروفی را دارد که تا این مرحله تدریس شده‌اند
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
            delay(2500)
            _currentItem.value = null
            _avatarState.value = "IDLE"
        }
    }

    fun seedData() {
        viewModelScope.launch {
            // درخت یادگیری تجمعی (Cumulative) - هر مرحله فقط یک حرف جدید اضافه می‌شود
            // استثنای اول: "آب" که آ و ب را همزمان معرفی می‌کند.
            val cumulativeAlphabet = listOf(
                LearningItem("a1", "ب", "آب", "audio_a1", "https://cdn-icons-png.flaticon.com/512/3105/3105807.png", "ALPHABET"),
                LearningItem("a2", "ا", "بابا", "audio_a2", "https://cdn-icons-png.flaticon.com/512/4139/4139162.png", "ALPHABET"), // ب (قبلی) + ا (جدید)
                LearningItem("a3", "د", "باد", "audio_a3", "https://cdn-icons-png.flaticon.com/512/95/95451.png", "ALPHABET"), // ب، ا (قبلی) + د (جدید)
                LearningItem("a4", "م", "بام", "audio_a4", "https://cdn-icons-png.flaticon.com/512/619/619032.png", "ALPHABET"), // ب، ا (قبلی) + م (جدید)
                LearningItem("a5", "س", "سبد", "audio_a5", "https://cdn-icons-png.flaticon.com/512/415/415733.png", "ALPHABET"), // ب، د (قبلی) + س (جدید)
                LearningItem("a6", "ن", "آبان", "audio_a6", "https://cdn-icons-png.flaticon.com/512/2693/2693507.png", "ALPHABET"), // آ، ب، ا (قبلی) + ن (جدید)
                LearningItem("a7", "ر", "ابر", "audio_a7", "https://cdn-icons-png.flaticon.com/512/1163/1163624.png", "ALPHABET"), // ا، ب (قبلی) + ر (جدید)
                LearningItem("a8", "ت", "دست", "audio_a8", "https://cdn-icons-png.flaticon.com/512/3063/3063822.png", "ALPHABET"), // د، س (قبلی) + ت (جدید)
                LearningItem("a9", "و", "بوم", "audio_a9", "https://cdn-icons-png.flaticon.com/512/3094/3094137.png", "ALPHABET"), // ب، م (قبلی) + و (جدید)
                LearningItem("a10", "ی", "سیب", "audio_a10", "https://cdn-icons-png.flaticon.com/512/415/415682.png", "ALPHABET"), // س، ب (قبلی) + ی (جدید)
                LearningItem("a11", "ز", "باز", "audio_a11", "https://cdn-icons-png.flaticon.com/512/1998/1998631.png", "ALPHABET"), // ب، ا (قبلی) + ز (جدید)
                LearningItem("a12", "ش", "آش", "audio_a12", "https://cdn-icons-png.flaticon.com/512/2082/2082045.png", "ALPHABET"), // آ (قبلی) + ش (جدید)
                LearningItem("a13", "ک", "کتاب", "audio_a13", "https://cdn-icons-png.flaticon.com/512/3389/3389081.png", "ALPHABET"), // ت، ا، ب (قبلی) + ک (جدید)
                LearningItem("a14", "گ", "سگ", "audio_a14", "https://cdn-icons-png.flaticon.com/512/1998/1998610.png", "ALPHABET"), // س (قبلی) + گ (جدید)
                LearningItem("a15", "ف", "برف", "audio_a15", "https://cdn-icons-png.flaticon.com/512/2315/2315309.png", "ALPHABET"), // ب، ر (قبلی) + ف (جدید)
                LearningItem("a16", "خ", "شاخ", "audio_a16", "https://cdn-icons-png.flaticon.com/512/1998/1998762.png", "ALPHABET"), // ش، ا (قبلی) + خ (جدید)
                LearningItem("a17", "ق", "قایق", "audio_a17", "https://cdn-icons-png.flaticon.com/512/2964/2964551.png", "ALPHABET"), // ا، ی (قبلی) + ق (جدید)
                LearningItem("a18", "ل", "لباس", "audio_a18", "https://cdn-icons-png.flaticon.com/512/3534/3534312.png", "ALPHABET"), // ب، ا، س (قبلی) + ل (جدید)
                LearningItem("a19", "ج", "تاج", "audio_a19", "https://cdn-icons-png.flaticon.com/512/2953/2953361.png", "ALPHABET"), // ت، ا (قبلی) + ج (جدید)
                LearningItem("a20", "چ", "چای", "audio_a20", "https://cdn-icons-png.flaticon.com/512/3054/3054813.png", "ALPHABET"), // ا، ی (قبلی) + چ (جدید)
                LearningItem("a21", "ژ", "ژله", "audio_a21", "https://cdn-icons-png.flaticon.com/512/184/184545.png", "ALPHABET"), // ل (قبلی) + ژ (جدید)
                LearningItem("a22", "ه", "کوه", "audio_a22", "https://cdn-icons-png.flaticon.com/512/2909/2909825.png", "ALPHABET")  // ک، و (قبلی) + ه (جدید)
            )
            
            val numberImages = listOf(
                "https://cdn-icons-png.flaticon.com/512/3570/3570095.png",
                "https://cdn-icons-png.flaticon.com/512/3570/3570096.png",
                "https://cdn-icons-png.flaticon.com/512/3570/3570097.png",
                "https://cdn-icons-png.flaticon.com/512/3570/3570098.png",
                "https://cdn-icons-png.flaticon.com/512/3570/3570099.png",
                "https://cdn-icons-png.flaticon.com/512/3570/3570100.png",
                "https://cdn-icons-png.flaticon.com/512/3570/3570101.png",
                "https://cdn-icons-png.flaticon.com/512/3570/3570102.png",
                "https://cdn-icons-png.flaticon.com/512/3570/3570103.png",
                "https://cdn-icons-png.flaticon.com/512/3570/3570104.png"
            )

            val numberItems = (0..9).map { 
                LearningItem("n$it", it.toString().toPersianDigit(), it.toPersianWord(), "audio_n$it", numberImages[it], "NUMBER")
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
