package com.example.tala.entity.learningMode

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface LearningModeDao {

    @Insert
    suspend fun insert(word: LearningMode)

    @Query("SELECT * FROM learning_modes WHERE name = :name LIMIT 1")
    suspend fun getLearningModeByName(name: String): LearningMode?

    @Query("SELECT * FROM learning_modes")
    suspend fun getAll(): List<LearningMode>

}