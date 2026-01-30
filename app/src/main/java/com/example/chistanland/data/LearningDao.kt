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

    @Query("SELECT * FROM learning_items WHERE nextReviewTime <= :currentTime OR level = 1")
    fun getItemsToReview(currentTime: Long): Flow<List<LearningItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItems(items: List<LearningItem>)

    @Update
    suspend fun updateItem(item: LearningItem)
}
