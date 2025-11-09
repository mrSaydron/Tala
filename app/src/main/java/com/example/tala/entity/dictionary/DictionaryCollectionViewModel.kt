package com.example.tala.entity.dictionary

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.tala.TalaDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class DictionaryCollectionViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = DictionaryCollectionRepository(
        TalaDatabase.getDatabase(application).dictionaryCollectionDao()
    )

    fun insert(collection: DictionaryCollection) = viewModelScope.launch(Dispatchers.IO) {
        repository.insert(collection)
    }

    suspend fun insertSync(collection: DictionaryCollection): Long = withContext(Dispatchers.IO) {
        repository.insert(collection)
    }

    fun update(collection: DictionaryCollection) = viewModelScope.launch(Dispatchers.IO) {
        repository.update(collection)
    }

    suspend fun updateSync(collection: DictionaryCollection): Long = withContext(Dispatchers.IO) {
        repository.update(collection)
    }

    fun delete(collection: DictionaryCollection) = viewModelScope.launch(Dispatchers.IO) {
        repository.delete(collection)
    }

    suspend fun getAll(): List<DictionaryCollection> = withContext(Dispatchers.IO) {
        repository.getAll()
    }

    suspend fun getById(id: Int): DictionaryCollection? = withContext(Dispatchers.IO) {
        repository.getById(id)
    }

    suspend fun getByName(name: String): DictionaryCollection? = withContext(Dispatchers.IO) {
        repository.getByName(name)
    }
}

