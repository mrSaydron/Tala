package com.example.tala.entity.cardhistory

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.tala.TalaDatabase
import com.example.tala.model.enums.CardTypeEnum
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class CardHistoryViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: CardHistoryRepository

    init {
        val dao = TalaDatabase.getDatabase(application).cardHistoryDao()
        repository = CardHistoryRepository(dao)
    }

    fun insert(entry: CardHistory) = viewModelScope.launch(Dispatchers.IO) {
        repository.insert(entry)
    }

    fun insertAll(entries: List<CardHistory>) = viewModelScope.launch(Dispatchers.IO) {
        repository.insertAll(entries)
    }

    suspend fun getByLesson(lessonId: Int): List<CardHistory> = withContext(Dispatchers.IO) {
        repository.getByLesson(lessonId)
    }

    suspend fun getByLessonAndType(lessonId: Int, cardType: CardTypeEnum): List<CardHistory> =
        withContext(Dispatchers.IO) {
            repository.getByLessonAndType(lessonId, cardType)
        }

    suspend fun getByWord(wordId: Int): List<CardHistory> = withContext(Dispatchers.IO) {
        repository.getByWord(wordId)
    }

    fun clearAll() = viewModelScope.launch(Dispatchers.IO) {
        repository.clearAll()
    }

    fun clearByLesson(lessonId: Int) = viewModelScope.launch(Dispatchers.IO) {
        repository.clearByLesson(lessonId)
    }
}

