package com.example.tala.entity.dictionaryCollection

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction

@Dao
interface DictionaryCollectionDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(collection: DictionaryCollection): Long

    @Delete
    suspend fun delete(collection: DictionaryCollection)

    @Query("SELECT * FROM dictionary_collections ORDER BY name COLLATE NOCASE")
    suspend fun getAll(): List<DictionaryCollection>

    @Query("SELECT * FROM dictionary_collections WHERE id = :id")
    suspend fun getById(id: Int): DictionaryCollection?

    @Query("SELECT * FROM dictionary_collections WHERE name = :name COLLATE NOCASE LIMIT 1")
    suspend fun getByName(name: String): DictionaryCollection?

    @Transaction
    @Query("SELECT * FROM dictionary_collections ORDER BY name COLLATE NOCASE")
    suspend fun getAllWithEntries(): List<DictionaryCollectionWithEntries>

    @Transaction
    @Query("SELECT * FROM dictionary_collections WHERE id = :id")
    suspend fun getByIdWithEntries(id: Int): DictionaryCollectionWithEntries?
}

