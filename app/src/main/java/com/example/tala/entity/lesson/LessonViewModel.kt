package com.example.tala.entity.lesson

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.tala.TalaDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class LessonViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: LessonRepository = LessonRepository(
        TalaDatabase.getDatabase(application).lessonDao()
    )

    fun insert(lesson: Lesson) = viewModelScope.launch(Dispatchers.IO) {
        repository.insert(lesson)
    }

    suspend fun insertSync(lesson: Lesson): Long = withContext(Dispatchers.IO) {
        repository.insert(lesson)
    }

    fun update(lesson: Lesson) = viewModelScope.launch(Dispatchers.IO) {
        repository.update(lesson)
    }

    suspend fun updateSync(lesson: Lesson) = withContext(Dispatchers.IO) {
        repository.update(lesson)
    }

    fun delete(lesson: Lesson) = viewModelScope.launch(Dispatchers.IO) {
        repository.delete(lesson)
    }

    suspend fun getAll(): List<Lesson> = withContext(Dispatchers.IO) {
        repository.getAll()
    }

    suspend fun getById(id: Int): Lesson? = withContext(Dispatchers.IO) {
        repository.getById(id)
    }

    suspend fun getByCollectionId(collectionId: Int): List<Lesson> = withContext(Dispatchers.IO) {
        repository.getByCollectionId(collectionId)
    }
}

