package com.example.chistanland.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "learning_items")
data class LearningItem(
    @PrimaryKey val id: String,
    val character: String,
    val word: String,
    val phonetic: String, // Path or resource name for phonetic audio
    val imageUrl: String, // Local asset path
    val level: Int = 1, // 1 to 5 (Leitner levels)
    val lastReviewTime: Long = 0,
    val nextReviewTime: Long = 0,
    val isMastered: Boolean = false
)
