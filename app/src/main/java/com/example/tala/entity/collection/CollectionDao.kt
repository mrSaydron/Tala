package com.example.tala.entity.collection

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query

@Dao
interface CollectionDao {
    @Insert
    suspend fun insert(collection: CardCollection)

    @Delete
    suspend fun delete(collection: CardCollection)

    @Query("SELECT * FROM collections")
    fun getAllCollections(): LiveData<List<CardCollection>>

    @Query("DELETE FROM collections")
    suspend fun deleteAllCollections()

    @Query("SELECT COUNT(*) FROM collections WHERE name = :name")
    suspend fun countByName(name: String): Int

    @Query("SELECT COUNT(*) FROM collections")
    suspend fun countAll(): Int

    @Query("UPDATE collections SET name = :newName WHERE id = :id")
    suspend fun renameById(id: Int, newName: String)
}


