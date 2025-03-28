package com.example.tala.entity.learningMode

import androidx.lifecycle.LiveData

class LearningModeRepository(private val learningModeDao: LearningModeDao) {

    suspend fun insert(learningMode: LearningMode) {
        learningModeDao.insert(learningMode)
    }

    suspend fun getLearningModeByName(name: String): LearningMode? {
        return learningModeDao.getLearningModeByName(name)
    }

    suspend fun getAll(): List<LearningMode> {
        return learningModeDao.getAll()
    }
}