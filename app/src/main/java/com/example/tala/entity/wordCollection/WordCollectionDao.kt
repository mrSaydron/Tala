package com.example.tala.entity.wordCollection

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction

@Dao
interface WordCollectionDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(collection: WordCollection): Long

    @Delete
    suspend fun delete(collection: WordCollection)

    @Query("SELECT * FROM word_collection ORDER BY name COLLATE NOCASE")
    suspend fun getAll(): List<WordCollection>

    @Query("SELECT * FROM word_collection WHERE id = :id")
    suspend fun getById(id: Int): WordCollection?

    @Query("SELECT * FROM word_collection WHERE name = :name COLLATE NOCASE LIMIT 1")
    suspend fun getByName(name: String): WordCollection?

    @Transaction
    @Query("SELECT * FROM word_collection ORDER BY name COLLATE NOCASE")
    suspend fun getAllWithEntries(): List<WordCollectionWithEntries>

    @Transaction
    @Query("SELECT * FROM word_collection WHERE id = :id")
    suspend fun getByIdWithEntries(id: Int): WordCollectionWithEntries?
}

