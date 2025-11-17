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

    @Query("SELECT * FROM dictionary WHERE base_word_id IS NULL OR base_word_id = id")
    suspend fun getBaseEntries(): List<Dictionary>

    @Query(
        """
        SELECT
            d.id AS id,
            d.word AS word,
            d.translation AS translation,
            d.part_of_speech AS part_of_speech,
            d.ipa AS ipa,
            d.hint AS hint,
            d.image_path AS image_path,
            d.base_word_id AS base_word_id,
            d.frequency AS frequency,
            d.level AS level,
            d.tags AS tags,
            COUNT(child.id) AS dependent_count
        FROM dictionary d
        LEFT JOIN dictionary child
            ON child.base_word_id = d.id
            AND child.id != d.id
        WHERE d.base_word_id IS NULL
           OR d.base_word_id = d.id
        GROUP BY d.id
        """
    )
    suspend fun getBaseEntriesWithDependentCount(): List<DictionaryWithDependentCount>

    @Query("SELECT * FROM dictionary WHERE id = :id")
    suspend fun getById(id: Int): Dictionary?

    @Query("SELECT * FROM dictionary WHERE word = :word COLLATE NOCASE")
    suspend fun getByWord(word: String): List<Dictionary>

    @Query("SELECT * FROM dictionary WHERE base_word_id = :baseWordId")
    suspend fun getByBaseWordId(baseWordId: Int): List<Dictionary>

    @Query("SELECT * FROM dictionary WHERE base_word_id = :baseWordId OR id = :baseWordId")
    suspend fun getGroupByBaseId(baseWordId: Int): List<Dictionary>

    @Query(
        """
        SELECT *
        FROM dictionary
        WHERE id = (
            SELECT COALESCE(base_word_id, :entryId)
            FROM dictionary
            WHERE id = :entryId
        )
        UNION
        SELECT *
        FROM dictionary
        WHERE base_word_id = (
            SELECT COALESCE(base_word_id, :entryId)
            FROM dictionary
            WHERE id = :entryId
        )
        ORDER BY id
        """
    )
    suspend fun getGroupByEntryId(entryId: Int): List<Dictionary>

    @Query("SELECT * FROM dictionary WHERE id IN (:ids)")
    suspend fun getByIds(ids: List<Int>): List<Dictionary>
}

