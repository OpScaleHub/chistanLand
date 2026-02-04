package com.github.opscalehub.chistanland.data

import kotlinx.coroutines.flow.Flow
import java.util.concurrent.TimeUnit

class LearningRepository(private val learningDao: LearningDao) {

    val allItems: Flow<List<LearningItem>> = learningDao.getAllItems()

    fun getItemsToReviewByCategory(category: String): Flow<List<LearningItem>> {
        return learningDao.getItemsToReviewByCategory(category, System.currentTimeMillis())
    }

    suspend fun updateProgress(item: LearningItem, isCorrect: Boolean) {
        var newLevel = item.level
        var newExp = item.experience
        val nextReviewDelay: Long

        if (isCorrect) {
            newExp += 1
            if (newExp >= 3) { // برای هر سطح 3 بار تمرین لازم است
                newLevel = (item.level + 1).coerceAtMost(5)
                newExp = 0
            }
            
            nextReviewDelay = when (newLevel) {
                1 -> TimeUnit.MINUTES.toMillis(5)
                2 -> TimeUnit.HOURS.toMillis(12)
                3 -> TimeUnit.DAYS.toMillis(2)
                4 -> TimeUnit.DAYS.toMillis(5)
                5 -> Long.MAX_VALUE
                else -> 0
            }
        } else {
            // کاهش تجربه در صورت اشتباه (اما سطح پایین نمی‌آید تا کودک ناامید نشود)
            newExp = (newExp - 1).coerceAtLeast(0)
            nextReviewDelay = 0
        }

        val updatedItem = item.copy(
            level = newLevel,
            experience = newExp,
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
