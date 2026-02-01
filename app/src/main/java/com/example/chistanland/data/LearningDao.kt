package com.example.chistanland.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface LearningDao {
    @Query("SELECT * FROM learning_items")
    fun getAllItems(): Flow<List<LearningItem>>

    @Query("SELECT * FROM learning_items WHERE id = :id")
    suspend fun getItemById(id: String): LearningItem?

    // فقط مواردی که حداقل یک بار با موفقیت گذرانده شده‌اند (level > 1) و زمان مرورشان رسیده یا هنوز به تسلط کامل (5) نرسیده‌اند
    @Query("SELECT * FROM learning_items WHERE category = :category AND lastReviewTime > 0 AND level > 1 AND (nextReviewTime <= :currentTime OR level < 5)")
    fun getItemsToReviewByCategory(category: String, currentTime: Long): Flow<List<LearningItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItems(items: List<LearningItem>)

    @Update
    suspend fun updateItem(item: LearningItem)
}
