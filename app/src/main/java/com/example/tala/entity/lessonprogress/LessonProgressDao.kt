package com.example.tala.entity.lessonprogress

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.tala.model.enums.CardTypeEnum

@Dao
interface LessonProgressDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(progress: LessonProgress): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(progressList: List<LessonProgress>): List<Long>

    @Update
    suspend fun update(progress: LessonProgress)

    @Delete
    suspend fun delete(progress: LessonProgress)

    @Query("DELETE FROM lesson_progress WHERE lesson_id = :lessonId AND card_type = :cardType")
    suspend fun deleteByLessonCardType(lessonId: Int, cardType: CardTypeEnum)

    @Query("SELECT * FROM lesson_progress")
    suspend fun getAll(): List<LessonProgress>

    @Query("SELECT * FROM lesson_progress WHERE lesson_id = :lessonId AND card_type = :cardType")
    suspend fun getByLessonCardType(lessonId: Int, cardType: CardTypeEnum): List<LessonProgress>

    @Query("SELECT * FROM lesson_progress WHERE dictionary_id = :dictionaryId")
    suspend fun getByDictionaryId(dictionaryId: Int): List<LessonProgress>

    @Query("SELECT * FROM lesson_progress WHERE id = :id LIMIT 1")
    suspend fun getById(id: Int): LessonProgress?
}

