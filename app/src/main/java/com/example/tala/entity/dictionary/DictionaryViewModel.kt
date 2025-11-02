package com.example.tala.entity.dictionary

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.tala.TalaDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class DictionaryViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: DictionaryRepository = DictionaryRepository(
        TalaDatabase.getDatabase(application).dictionaryDao()
    )

    fun insert(entry: Dictionary) = viewModelScope.launch(Dispatchers.IO) {
        repository.insert(entry)
    }

    fun delete(entry: Dictionary) = viewModelScope.launch(Dispatchers.IO) {
        repository.delete(entry)
    }

    suspend fun getAll(): List<Dictionary> = withContext(Dispatchers.IO) {
        repository.getAll()
    }

    suspend fun getByWord(word: String): List<Dictionary> = withContext(Dispatchers.IO) {
        repository.getByWord(word)
    }

    suspend fun getByBaseWordId(baseWordId: Int): List<Dictionary> = withContext(Dispatchers.IO) {
        repository.getByBaseWordId(baseWordId)
    }
}

