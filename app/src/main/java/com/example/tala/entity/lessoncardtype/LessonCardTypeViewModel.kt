package com.example.tala.entity.lessoncardtype

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.tala.TalaDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class LessonCardTypeViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: LessonCardTypeRepository = LessonCardTypeRepository(
        TalaDatabase.getDatabase(application).lessonCardTypeDao()
    )

    fun insert(entity: LessonCardType) = viewModelScope.launch(Dispatchers.IO) {
        repository.insert(entity)
    }

    fun insertAll(entities: List<LessonCardType>) = viewModelScope.launch(Dispatchers.IO) {
        repository.insertAll(entities)
    }

    fun delete(entity: LessonCardType) = viewModelScope.launch(Dispatchers.IO) {
        repository.delete(entity)
    }

    fun deleteByLessonId(lessonId: Int) = viewModelScope.launch(Dispatchers.IO) {
        repository.deleteByLessonId(lessonId)
    }

    suspend fun getAll(): List<LessonCardType> = withContext(Dispatchers.IO) {
        repository.getAll()
    }

    suspend fun getByLessonId(lessonId: Int): List<LessonCardType> = withContext(Dispatchers.IO) {
        repository.getByLessonId(lessonId)
    }
}


