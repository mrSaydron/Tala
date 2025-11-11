package com.example.tala.entity.lessoncardtype

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface LessonCardTypeDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: LessonCardType)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entities: List<LessonCardType>)

    @Delete
    suspend fun delete(entity: LessonCardType)

    @Query("DELETE FROM lesson_card_types WHERE lesson_id = :lessonId")
    suspend fun deleteByLessonId(lessonId: Int)

    @Query("SELECT * FROM lesson_card_types")
    suspend fun getAll(): List<LessonCardType>

    @Query("SELECT * FROM lesson_card_types WHERE lesson_id = :lessonId")
    suspend fun getByLessonId(lessonId: Int): List<LessonCardType>
}


