package com.example.tala.entity.dictionaryCollection

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface DictionaryCollectionEntryDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(entry: DictionaryCollectionEntry): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(entries: List<DictionaryCollectionEntry>): List<Long>

    @Delete
    suspend fun delete(entry: DictionaryCollectionEntry)

    @Query("DELETE FROM dictionary_collection_entries WHERE collection_id = :collectionId")
    suspend fun deleteByCollectionId(collectionId: Int)

    @Query(
        "DELETE FROM dictionary_collection_entries " +
            "WHERE collection_id = :collectionId AND dictionary_id = :dictionaryId"
    )
    suspend fun deleteByCollectionAndDictionary(collectionId: Int, dictionaryId: Int)

    @Query("SELECT * FROM dictionary_collection_entries WHERE collection_id = :collectionId")
    suspend fun getByCollectionId(collectionId: Int): List<DictionaryCollectionEntry>
}

