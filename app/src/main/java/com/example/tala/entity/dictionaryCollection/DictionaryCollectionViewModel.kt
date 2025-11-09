package com.example.tala.entity.dictionaryCollection

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.tala.TalaDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class DictionaryCollectionViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: DictionaryCollectionRepository

    init {
        val database = TalaDatabase.getDatabase(application)
        repository = DictionaryCollectionRepository(
            database.dictionaryCollectionDao(),
            database.dictionaryCollectionEntryDao()
        )
    }

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

    suspend fun getAllWithEntries(): List<DictionaryCollectionWithEntries> = withContext(Dispatchers.IO) {
        repository.getAllWithEntries()
    }

    suspend fun getByIdWithEntries(id: Int): DictionaryCollectionWithEntries? = withContext(Dispatchers.IO) {
        repository.getByIdWithEntries(id)
    }

    fun addDictionaryToCollection(collectionId: Int, dictionaryId: Int) = viewModelScope.launch(Dispatchers.IO) {
        repository.addDictionaryToCollection(collectionId, dictionaryId)
    }

    fun addDictionariesToCollection(collectionId: Int, dictionaryIds: List<Int>) =
        viewModelScope.launch(Dispatchers.IO) {
            repository.addDictionariesToCollection(collectionId, dictionaryIds)
        }

    fun removeDictionaryFromCollection(collectionId: Int, dictionaryId: Int) =
        viewModelScope.launch(Dispatchers.IO) {
            repository.removeDictionaryFromCollection(collectionId, dictionaryId)
        }

    fun clearCollection(collectionId: Int) = viewModelScope.launch(Dispatchers.IO) {
        repository.clearCollection(collectionId)
    }

    suspend fun getDictionaryIds(collectionId: Int): List<Int> = withContext(Dispatchers.IO) {
        repository.getDictionaryIds(collectionId)
    }

    fun replaceCollectionDictionaries(collectionId: Int, dictionaryIds: List<Int>) =
        viewModelScope.launch(Dispatchers.IO) {
            repository.replaceCollectionDictionaries(collectionId, dictionaryIds)
        }

    suspend fun replaceCollectionDictionariesSync(collectionId: Int, dictionaryIds: List<Int>) =
        withContext(Dispatchers.IO) {
            repository.replaceCollectionDictionaries(collectionId, dictionaryIds)
        }
}

