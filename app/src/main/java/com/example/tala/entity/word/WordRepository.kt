package com.example.tala.entity.word

class WordRepository(private val dao: WordDao) {

    suspend fun insert(entry: Word): Long = dao.insert(entry)

    suspend fun update(entry: Word): Long = dao.insert(entry)

    suspend fun delete(entry: Word) = dao.delete(entry)

    suspend fun getAll(): List<Word> = dao.getAll()

    suspend fun getBaseEntries(): List<Word> = dao.getBaseEntries()

    suspend fun getBaseEntriesWithDependentCount(): List<WordWithDependentCount> =
        dao.getBaseEntriesWithDependentCount()

    suspend fun getById(id: Int): Word? = dao.getById(id)

    suspend fun getByWord(word: String): List<Word> = dao.getByWord(word)

    suspend fun getByBaseWordId(baseWordId: Int): List<Word> = dao.getByBaseWordId(baseWordId)

    suspend fun getGroupByBaseId(baseWordId: Int): List<Word> = dao.getGroupByBaseId(baseWordId)

    suspend fun getGroupByEntryId(entryId: Int): List<Word> = dao.getGroupByEntryId(entryId)

    suspend fun getByIds(ids: List<Int>): List<Word> =
        if (ids.isEmpty()) emptyList() else dao.getByIds(ids)
}

