package com.example.tala.entity.dictionary

class DictionaryCollectionRepository(private val dao: DictionaryCollectionDao) {

    suspend fun insert(collection: DictionaryCollection): Long = dao.insert(collection)

    suspend fun update(collection: DictionaryCollection): Long = dao.insert(collection)

    suspend fun delete(collection: DictionaryCollection) = dao.delete(collection)

    suspend fun getAll(): List<DictionaryCollection> = dao.getAll()

    suspend fun getById(id: Int): DictionaryCollection? = dao.getById(id)

    suspend fun getByName(name: String): DictionaryCollection? = dao.getByName(name)
}

