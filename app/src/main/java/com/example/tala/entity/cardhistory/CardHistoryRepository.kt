package com.example.tala.entity.cardhistory

import com.example.tala.model.enums.CardTypeEnum

class CardHistoryRepository(
    private val cardHistoryDao: CardHistoryDao
) {

    suspend fun insert(entry: CardHistory) {
        cardHistoryDao.insert(entry)
    }

    suspend fun insertAll(entries: List<CardHistory>) {
        if (entries.isEmpty()) return
        cardHistoryDao.insertAll(entries)
    }

    suspend fun getByLesson(lessonId: Int): List<CardHistory> {
        return cardHistoryDao.getByLesson(lessonId)
    }

    suspend fun getByLessonAndType(lessonId: Int, cardType: CardTypeEnum): List<CardHistory> {
        return cardHistoryDao.getByLessonAndType(lessonId, cardType)
    }

    suspend fun getByDictionary(dictionaryId: Int): List<CardHistory> {
        return cardHistoryDao.getByDictionary(dictionaryId)
    }

    suspend fun clearAll() {
        cardHistoryDao.clearAll()
    }

    suspend fun clearByLesson(lessonId: Int) {
        cardHistoryDao.clearByLesson(lessonId)
    }
}

