package com.example.chistanland.data

import kotlinx.coroutines.flow.Flow
import java.util.concurrent.TimeUnit

class LearningRepository(private val learningDao: LearningDao) {

    val allItems: Flow<List<LearningItem>> = learningDao.getAllItems()

    fun getItemsToReviewByCategory(category: String): Flow<List<LearningItem>> {
        return learningDao.getItemsToReviewByCategory(category, System.currentTimeMillis())
    }

    suspend fun updateProgress(item: LearningItem, isCorrect: Boolean) {
        val newLevel: Int
        val nextReviewDelay: Long

        if (isCorrect) {
            newLevel = (item.level + 1).coerceAtMost(5)
            nextReviewDelay = when (newLevel) {
                1 -> TimeUnit.MINUTES.toMillis(10) // Immediate review for Level 1
                2 -> TimeUnit.HOURS.toMillis(24)    // 1 Day
                3 -> TimeUnit.DAYS.toMillis(4)     // 4 Days
                4 -> TimeUnit.DAYS.toMillis(7)     // 1 Week
                5 -> Long.MAX_VALUE                // Mastered (Long Term)
                else -> 0
            }
        } else {
            // Loss Aversion: Return to level 1 on error
            newLevel = 1
            nextReviewDelay = 0
        }

        val updatedItem = item.copy(
            level = newLevel,
            lastReviewTime = System.currentTimeMillis(),
            nextReviewTime = if (newLevel == 5) Long.MAX_VALUE else System.currentTimeMillis() + nextReviewDelay,
            isMastered = newLevel == 5
        )
        learningDao.updateItem(updatedItem)
    }

    suspend fun insertInitialData(items: List<LearningItem>) {
        learningDao.insertItems(items)
    }
}
