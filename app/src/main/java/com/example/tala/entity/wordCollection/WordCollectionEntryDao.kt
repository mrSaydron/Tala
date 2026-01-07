package com.example.tala.entity.wordCollection

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface WordCollectionEntryDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(entry: WordCollectionEntry): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(entries: List<WordCollectionEntry>): List<Long>

    @Delete
    suspend fun delete(entry: WordCollectionEntry)

    @Query("DELETE FROM word_collection_entries WHERE collection_id = :collectionId")
    suspend fun deleteByCollectionId(collectionId: Int)

    @Query(
        "DELETE FROM word_collection_entries " +
            "WHERE collection_id = :collectionId AND word_id = :wordId"
    )
    suspend fun deleteByCollectionAndWord(collectionId: Int, wordId: Int)

    @Query("SELECT * FROM word_collection_entries WHERE collection_id = :collectionId")
    suspend fun getByCollectionId(collectionId: Int): List<WordCollectionEntry>
}

