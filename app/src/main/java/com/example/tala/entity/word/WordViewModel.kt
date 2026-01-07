package com.example.tala.entity.word

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.tala.TalaDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class WordViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: WordRepository = WordRepository(
        TalaDatabase.getDatabase(application).wordDao()
    )

    fun insert(entry: Word) = viewModelScope.launch(Dispatchers.IO) {
        repository.insert(entry)
    }

    suspend fun insertSync(entry: Word): Long = withContext(Dispatchers.IO) {
        repository.insert(entry)
    }

    fun update(entry: Word) = viewModelScope.launch(Dispatchers.IO) {
        repository.update(entry)
    }

    suspend fun updateSync(entry: Word) = withContext(Dispatchers.IO) {
        repository.update(entry)
    }

    fun delete(entry: Word) = viewModelScope.launch(Dispatchers.IO) {
        repository.delete(entry)
    }

    suspend fun getAll(): List<Word> = withContext(Dispatchers.IO) {
        repository.getAll()
    }

    suspend fun getBaseEntries(): List<Word> = withContext(Dispatchers.IO) {
        repository.getBaseEntries()
    }

    suspend fun getBaseEntriesWithDependentCount(): List<WordWithDependentCount> = withContext(Dispatchers.IO) {
        repository.getBaseEntriesWithDependentCount()
    }

    suspend fun getById(id: Int): Word? = withContext(Dispatchers.IO) {
        repository.getById(id)
    }

    suspend fun getGroupByBaseId(baseWordId: Int): List<Word> = withContext(Dispatchers.IO) {
        repository.getGroupByBaseId(baseWordId)
    }

    suspend fun getGroupByEntryId(entryId: Int): List<Word> = withContext(Dispatchers.IO) {
        repository.getGroupByEntryId(entryId)
    }

    suspend fun getByWord(word: String): List<Word> = withContext(Dispatchers.IO) {
        repository.getByWord(word)
    }

    suspend fun getByBaseWordId(baseWordId: Int): List<Word> = withContext(Dispatchers.IO) {
        repository.getByBaseWordId(baseWordId)
    }

    suspend fun getByIds(ids: List<Int>): List<Word> = withContext(Dispatchers.IO) {
        repository.getByIds(ids)
    }
}

