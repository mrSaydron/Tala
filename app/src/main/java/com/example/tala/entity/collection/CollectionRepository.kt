package com.example.tala.entity.collection

import androidx.lifecycle.LiveData

class CollectionRepository(private val collectionDao: CollectionDao) {

    suspend fun insert(collection: CardCollection) {
        collectionDao.insert(collection)
    }

    suspend fun delete(collection: CardCollection) {
        collectionDao.delete(collection)
    }

    fun getAllCollections(): LiveData<List<CardCollection>> {
        return collectionDao.getAllCollections()
    }

    suspend fun deleteAllCollections() {
        collectionDao.deleteAllCollections()
    }

    suspend fun existsByName(name: String): Boolean {
        return collectionDao.countByName(name) > 0
    }
}


