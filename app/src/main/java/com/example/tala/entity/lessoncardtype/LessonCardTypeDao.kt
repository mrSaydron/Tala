package com.example.tala.entity.lessoncardtype

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction

@Dao
interface LessonCardTypeDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: LessonCardType)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entities: List<LessonCardType>)

    @Transaction
    suspend fun replaceForCollection(collectionId: Int, entities: List<LessonCardType>) {
        deleteByCollectionId(collectionId)
        insertAll(entities)
    }

    @Delete
    suspend fun delete(entity: LessonCardType)

    @Query("DELETE FROM lesson_card_types WHERE collection_id = :collectionId")
    suspend fun deleteByCollectionId(collectionId: Int)

    @Query("SELECT * FROM lesson_card_types")
    suspend fun getAll(): List<LessonCardType>

    @Query("SELECT * FROM lesson_card_types WHERE collection_id = :collectionId")
    suspend fun getByCollectionId(collectionId: Int): List<LessonCardType>
}


