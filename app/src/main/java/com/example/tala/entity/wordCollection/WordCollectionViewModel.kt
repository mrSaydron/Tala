package com.example.tala.entity.wordCollection

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.tala.TalaDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class WordCollectionViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: WordCollectionRepository

    init {
        val database = TalaDatabase.getDatabase(application)
        repository = WordCollectionRepository(
            database.wordCollectionDao(),
            database.wordCollectionEntryDao()
        )
    }

    fun insert(collection: WordCollection) = viewModelScope.launch(Dispatchers.IO) {
        repository.insert(collection)
    }

    suspend fun insertSync(collection: WordCollection): Long = withContext(Dispatchers.IO) {
        repository.insert(collection)
    }

    fun update(collection: WordCollection) = viewModelScope.launch(Dispatchers.IO) {
        repository.update(collection)
    }

    suspend fun updateSync(collection: WordCollection): Long = withContext(Dispatchers.IO) {
        repository.update(collection)
    }

    fun delete(collection: WordCollection) = viewModelScope.launch(Dispatchers.IO) {
        repository.delete(collection)
    }

    suspend fun getAll(): List<WordCollection> = withContext(Dispatchers.IO) {
        repository.getAll()
    }

    suspend fun getById(id: Int): WordCollection? = withContext(Dispatchers.IO) {
        repository.getById(id)
    }

    suspend fun getByName(name: String): WordCollection? = withContext(Dispatchers.IO) {
        repository.getByName(name)
    }

    suspend fun getAllWithEntries(): List<WordCollectionWithEntries> = withContext(Dispatchers.IO) {
        repository.getAllWithEntries()
    }

    suspend fun getByIdWithEntries(id: Int): WordCollectionWithEntries? = withContext(Dispatchers.IO) {
        repository.getByIdWithEntries(id)
    }

    fun addWordToCollection(collectionId: Int, wordId: Int) = viewModelScope.launch(Dispatchers.IO) {
        repository.addWordToCollection(collectionId, wordId)
    }

    fun addWordsToCollection(collectionId: Int, wordIds: List<Int>) =
        viewModelScope.launch(Dispatchers.IO) {
            repository.addWordsToCollection(collectionId, wordIds)
        }

    fun removeWordFromCollection(collectionId: Int, wordId: Int) =
        viewModelScope.launch(Dispatchers.IO) {
            repository.removeWordFromCollection(collectionId, wordId)
        }

    fun clearCollection(collectionId: Int) = viewModelScope.launch(Dispatchers.IO) {
        repository.clearCollection(collectionId)
    }

    suspend fun getWordIds(collectionId: Int): List<Int> = withContext(Dispatchers.IO) {
        repository.getWordIds(collectionId)
    }

    fun replaceCollectionWords(collectionId: Int, wordIds: List<Int>) =
        viewModelScope.launch(Dispatchers.IO) {
            repository.replaceCollectionWords(collectionId, wordIds)
        }

    suspend fun replaceCollectionWordsSync(collectionId: Int, wordIds: List<Int>) =
        withContext(Dispatchers.IO) {
            repository.replaceCollectionWords(collectionId, wordIds)
        }
}

