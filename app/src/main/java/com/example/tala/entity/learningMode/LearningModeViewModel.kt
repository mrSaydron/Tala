package com.example.tala.entity.learningMode

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.example.tala.TalaDatabase

class LearningModeViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: LearningModeRepository

    init {
        val dao = TalaDatabase.getDatabase(application).learningModeDao()
        repository = LearningModeRepository(dao)

    }

    suspend fun getLearningModeByName(name: String): LearningMode? {
        return repository.getLearningModeByName(name)
    }

    suspend fun insert(learningMode: LearningMode) {
        repository.insert(learningMode)
    }

    suspend fun getAll(): List<LearningMode> {
        return repository.getAll()
    }
}