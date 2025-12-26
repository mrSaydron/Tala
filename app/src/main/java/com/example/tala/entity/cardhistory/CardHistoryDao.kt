package com.example.tala.entity.cardhistory

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.tala.model.enums.CardTypeEnum

@Dao
interface CardHistoryDao {

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(entry: CardHistory)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertAll(entries: List<CardHistory>)

    @Query("SELECT * FROM card_history WHERE lesson_id = :lessonId ORDER BY date DESC")
    suspend fun getByLesson(lessonId: Int): List<CardHistory>

    @Query(
        """
        SELECT *
        FROM card_history
        WHERE lesson_id = :lessonId AND card_type = :cardType
        ORDER BY date DESC
        """
    )
    suspend fun getByLessonAndType(lessonId: Int, cardType: CardTypeEnum): List<CardHistory>

    @Query("SELECT * FROM card_history WHERE dictionary_id = :dictionaryId ORDER BY date DESC")
    suspend fun getByDictionary(dictionaryId: Int): List<CardHistory>

    @Query("DELETE FROM card_history")
    suspend fun clearAll()

    @Query("DELETE FROM card_history WHERE lesson_id = :lessonId")
    suspend fun clearByLesson(lessonId: Int)
}

