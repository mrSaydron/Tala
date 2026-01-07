package com.example.tala.entity.word

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface WordDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: Word): Long

    @Delete
    suspend fun delete(entry: Word)

    @Query("SELECT * FROM words")
    suspend fun getAll(): List<Word>

    @Query("SELECT * FROM words WHERE base_word_id IS NULL OR base_word_id = id")
    suspend fun getBaseEntries(): List<Word>

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
        FROM words d
        LEFT JOIN words child
            ON child.base_word_id = d.id
            AND child.id != d.id
        WHERE d.base_word_id IS NULL
           OR d.base_word_id = d.id
        GROUP BY d.id
        """
    )
    suspend fun getBaseEntriesWithDependentCount(): List<WordWithDependentCount>

    @Query("SELECT * FROM words WHERE id = :id")
    suspend fun getById(id: Int): Word?

    @Query("SELECT * FROM words WHERE word = :word COLLATE NOCASE")
    suspend fun getByWord(word: String): List<Word>

    @Query("SELECT * FROM words WHERE base_word_id = :baseWordId")
    suspend fun getByBaseWordId(baseWordId: Int): List<Word>

    @Query("SELECT * FROM words WHERE base_word_id = :baseWordId OR id = :baseWordId")
    suspend fun getGroupByBaseId(baseWordId: Int): List<Word>

    @Query(
        """
        SELECT *
        FROM words
        WHERE id = (
            SELECT COALESCE(base_word_id, :entryId)
            FROM words
            WHERE id = :entryId
        )
        UNION
        SELECT *
        FROM words
        WHERE base_word_id = (
            SELECT COALESCE(base_word_id, :entryId)
            FROM words
            WHERE id = :entryId
        )
        ORDER BY id
        """
    )
    suspend fun getGroupByEntryId(entryId: Int): List<Word>

    @Query("SELECT * FROM words WHERE id IN (:ids)")
    suspend fun getByIds(ids: List<Int>): List<Word>
}

