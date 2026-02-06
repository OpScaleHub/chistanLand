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
            
            // شتاب‌دهی در گام‌های نخست: جهش سریع از سطح ۱ به ۲
            val requiredExp = if (item.level == 1) 1 else 3

            if (newExp >= requiredExp) {
                newLevel = (item.level + 1).coerceAtMost(5)
                newExp = 0
            }
            
            nextReviewDelay = when (newLevel) {
                1 -> TimeUnit.MINUTES.toMillis(5)
                2 -> TimeUnit.HOURS.toMillis(1)
                3 -> TimeUnit.DAYS.toMillis(1)
                4 -> TimeUnit.DAYS.toMillis(3)
                5 -> Long.MAX_VALUE // تسلط کامل، دیگر نیاز به مرور اتوماتیک نیست
                else -> 0
            }
        } else {
            // کاهش تجربه در صورت اشتباه (اما سطح پایین نمی‌آید تا کودک ناامید نشود)
            newExp = (newExp - 1).coerceAtLeast(0)
            nextReviewDelay = TimeUnit.MINUTES.toMillis(10) // مرور مجدد آیتم اشتباه در آینده نزدیک
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
