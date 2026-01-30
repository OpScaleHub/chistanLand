package com.example.chistanland.data

import kotlinx.coroutines.flow.Flow
import java.util.concurrent.TimeUnit

class LearningRepository(private val learningDao: LearningDao) {

    val allItems: Flow<List<LearningItem>> = learningDao.getAllItems()

    fun getItemsToReview(): Flow<List<LearningItem>> {
        return learningDao.getItemsToReview(System.currentTimeMillis())
    }

    suspend fun updateProgress(item: LearningItem, isCorrect: Boolean) {
        val newLevel: Int
        val nextReviewDelay: Long

        if (isCorrect) {
            newLevel = (item.level + 1).coerceAtMost(5)
            nextReviewDelay = when (newLevel) {
                1 -> 0 // Should not happen if correct, but for safety
                2 -> TimeUnit.HOURS.toMillis(24)
                3 -> TimeUnit.DAYS.toMillis(4)
                4 -> TimeUnit.DAYS.toMillis(7)
                5 -> Long.MAX_VALUE // Mastered
                else -> 0
            }
        } else {
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
