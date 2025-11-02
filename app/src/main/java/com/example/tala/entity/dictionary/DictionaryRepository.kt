package com.example.tala.entity.dictionary

class DictionaryRepository(private val dao: DictionaryDao) {

    suspend fun insert(entry: Dictionary): Long = dao.insert(entry)

    suspend fun delete(entry: Dictionary) = dao.delete(entry)

    suspend fun getAll(): List<Dictionary> = dao.getAll()

    suspend fun getByWord(word: String): List<Dictionary> = dao.getByWord(word)

    suspend fun getByBaseWordId(baseWordId: Int): List<Dictionary> = dao.getByBaseWordId(baseWordId)
}

