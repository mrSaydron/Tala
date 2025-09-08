package com.example.tala.entity.collection

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.viewModelScope
import com.example.tala.TalaDatabase
import kotlinx.coroutines.launch

class CollectionViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: CollectionRepository

    init {
        val dao = TalaDatabase.getDatabase(application).collectionsDao()
        repository = CollectionRepository(dao)
    }

    fun insertCollection(collection: CardCollection) = viewModelScope.launch {
        repository.insert(collection)
    }

    fun deleteCollection(collection: CardCollection) = viewModelScope.launch {
        repository.delete(collection)
    }

    fun getAllCollections(): LiveData<List<CardCollection>> {
        return repository.getAllCollections()
    }

    suspend fun deleteAllCollections() {
        repository.deleteAllCollections()
    }
}


