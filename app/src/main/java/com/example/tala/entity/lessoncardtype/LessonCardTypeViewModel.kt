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

    suspend fun insertAllSync(entities: List<LessonCardType>) = withContext(Dispatchers.IO) {
        repository.insertAll(entities)
    }

    fun delete(entity: LessonCardType) = viewModelScope.launch(Dispatchers.IO) {
        repository.delete(entity)
    }

    fun deleteByCollectionId(collectionId: Int) = viewModelScope.launch(Dispatchers.IO) {
        repository.deleteByCollectionId(collectionId)
    }

    suspend fun getAll(): List<LessonCardType> = withContext(Dispatchers.IO) {
        repository.getAll()
    }

    suspend fun getByCollectionId(collectionId: Int): List<LessonCardType> = withContext(Dispatchers.IO) {
        repository.getByCollectionId(collectionId)
    }

    suspend fun replaceForCollection(collectionId: Int, entities: List<LessonCardType>) =
        withContext(Dispatchers.IO) {
            repository.replaceForCollection(collectionId, entities)
        }
}


