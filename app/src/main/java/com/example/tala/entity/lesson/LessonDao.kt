package com.example.tala.entity.lesson

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update

@Dao
interface LessonDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(lesson: Lesson): Long

    @Update
    suspend fun update(lesson: Lesson)

    @Delete
    suspend fun delete(lesson: Lesson)

    @Query("SELECT * FROM lessons ORDER BY name COLLATE NOCASE")
    suspend fun getAll(): List<Lesson>

    @Query("SELECT * FROM lessons WHERE id = :id")
    suspend fun getById(id: Int): Lesson?

    @Query("SELECT * FROM lessons WHERE collection_id = :collectionId ORDER BY name COLLATE NOCASE")
    suspend fun getByCollectionId(collectionId: Int): List<Lesson>
}

