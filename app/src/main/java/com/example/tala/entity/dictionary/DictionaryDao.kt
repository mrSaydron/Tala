package com.example.tala.entity.dictionary

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface DictionaryDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: Dictionary): Long

    @Delete
    suspend fun delete(entry: Dictionary)

    @Query("SELECT * FROM dictionary")
    suspend fun getAll(): List<Dictionary>

    @Query("SELECT * FROM dictionary WHERE word = :word COLLATE NOCASE")
    suspend fun getByWord(word: String): List<Dictionary>

    @Query("SELECT * FROM dictionary WHERE base_word_id = :baseWordId")
    suspend fun getByBaseWordId(baseWordId: Int): List<Dictionary>
}

