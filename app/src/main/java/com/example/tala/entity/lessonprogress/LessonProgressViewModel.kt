package com.example.tala.entity.lessonprogress

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.tala.TalaDatabase
import com.example.tala.model.enums.StatusEnum
import com.example.tala.model.enums.CardTypeEnum
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class LessonProgressViewModel(application: Application) : AndroidViewModel(application) {

    private val database = TalaDatabase.getDatabase(application)
    private val repository: LessonProgressRepository =
        LessonProgressRepository(database.lessonProgressDao())

    fun insert(progress: LessonProgress) = viewModelScope.launch(Dispatchers.IO) {
        repository.insert(progress)
    }

    suspend fun insertSync(progress: LessonProgress): Long = withContext(Dispatchers.IO) {
        repository.insert(progress)
    }

    fun insertAll(progressList: List<LessonProgress>) = viewModelScope.launch(Dispatchers.IO) {
        repository.insertAll(progressList)
    }

    fun update(progress: LessonProgress) = viewModelScope.launch(Dispatchers.IO) {
        repository.update(progress)
    }

    suspend fun updateSync(progress: LessonProgress) = withContext(Dispatchers.IO) {
        repository.update(progress)
    }

    fun delete(progress: LessonProgress) = viewModelScope.launch(Dispatchers.IO) {
        repository.delete(progress)
    }

    fun deleteByLessonCardType(lessonId: Int, cardType: CardTypeEnum) = viewModelScope.launch(Dispatchers.IO) {
        repository.deleteByLessonCardType(lessonId, cardType)
    }

    suspend fun getAll(): List<LessonProgress> = withContext(Dispatchers.IO) {
        repository.getAll()
    }

    suspend fun getByLessonCardType(lessonId: Int, cardType: CardTypeEnum): List<LessonProgress> =
        withContext(Dispatchers.IO) {
            repository.getByLessonCardType(lessonId, cardType)
        }

    suspend fun getByDictionaryId(dictionaryId: Int): List<LessonProgress> = withContext(Dispatchers.IO) {
        repository.getByDictionaryId(dictionaryId)
    }
}

