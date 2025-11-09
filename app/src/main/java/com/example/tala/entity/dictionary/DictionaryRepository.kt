package com.example.tala.entity.dictionary

class DictionaryRepository(private val dao: DictionaryDao) {

    suspend fun insert(entry: Dictionary): Long = dao.insert(entry)

    suspend fun update(entry: Dictionary): Long = dao.insert(entry)

    suspend fun delete(entry: Dictionary) = dao.delete(entry)

    suspend fun getAll(): List<Dictionary> = dao.getAll()

    suspend fun getById(id: Int): Dictionary? = dao.getById(id)

    suspend fun getByWord(word: String): List<Dictionary> = dao.getByWord(word)

    suspend fun getByBaseWordId(baseWordId: Int): List<Dictionary> = dao.getByBaseWordId(baseWordId)

    suspend fun getGroupByBaseId(baseWordId: Int): List<Dictionary> = dao.getGroupByBaseId(baseWordId)

    suspend fun getByIds(ids: List<Int>): List<Dictionary> =
        if (ids.isEmpty()) emptyList() else dao.getByIds(ids)
}

