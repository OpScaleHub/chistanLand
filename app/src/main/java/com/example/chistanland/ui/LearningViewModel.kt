package com.example.chistanland.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.chistanland.data.AppDatabase
import com.example.chistanland.data.LearningItem
import com.example.chistanland.data.LearningRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class LearningViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: LearningRepository
    val allItems: StateFlow<List<LearningItem>>
    val reviewItems: StateFlow<List<LearningItem>>

    private val _currentItem = MutableStateFlow<LearningItem?>(null)
    val currentItem: StateFlow<LearningItem?> = _currentItem.asStateFlow()

    private val _typedText = MutableStateFlow("")
    val typedText: StateFlow<String> = _typedText.asStateFlow()

    init {
        val dao = AppDatabase.getDatabase(application).learningDao()
        repository = LearningRepository(dao)
        
        // Initializing flows
        allItems = repository.allItems.let { flow ->
            val state = MutableStateFlow<List<LearningItem>>(emptyList())
            viewModelScope.launch { flow.collect { state.value = it } }
            state.asStateFlow()
        }
        
        reviewItems = repository.getItemsToReview().let { flow ->
            val state = MutableStateFlow<List<LearningItem>>(emptyList())
            viewModelScope.launch { flow.collect { state.value = it } }
            state.asStateFlow()
        }
    }

    fun startLearning(item: LearningItem) {
        _currentItem.value = item
        _typedText.value = ""
    }

    fun onCharTyped(char: String) {
        val current = _currentItem.value ?: return
        val targetChar = current.word.getOrNull(_typedText.value.length)?.toString()

        if (char == targetChar) {
            _typedText.value += char
            if (_typedText.value == current.word) {
                completeLevel(true)
            }
        } else {
            // Logic for incorrect type - possibly haptic feedback or shake animation trigger
            // In a real app, we'd emit an event for the UI to show shaking
        }
    }

    private fun completeLevel(isCorrect: Boolean) {
        viewModelScope.launch {
            _currentItem.value?.let {
                repository.updateProgress(it, isCorrect)
                _currentItem.value = null
                _typedText.value = ""
            }
        }
    }

    fun seedData() {
        viewModelScope.launch {
            val initialItems = listOf(
                LearningItem("1", "آ", "آب", "ab_audio", "ab_img"),
                LearningItem("2", "ب", "بابا", "baba_audio", "baba_img"),
                LearningItem("3", "پ", "پا", "pa_audio", "pa_img"),
                LearningItem("4", "ت", "توت", "toot_audio", "toot_img"),
                LearningItem("5", "ث", "ثروت", "servat_audio", "servat_img")
            )
            repository.insertInitialData(initialItems)
        }
    }
}
