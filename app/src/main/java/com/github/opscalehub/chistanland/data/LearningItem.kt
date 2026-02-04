package com.github.opscalehub.chistanland.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "learning_items")
data class LearningItem(
    @PrimaryKey val id: String,
    val character: String,
    val word: String,
    val phonetic: String,
    val imageUrl: String,
    val category: String, // "ALPHABET" or "NUMBER"
    val level: Int = 1,
    val lastReviewTime: Long = 0,
    val nextReviewTime: Long = 0,
    val isMastered: Boolean = false
)
