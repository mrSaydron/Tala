package com.example.tala.entity.lessonprogress

import com.example.tala.model.enums.CardTypeEnum

class LessonProgressRepository(private val dao: LessonProgressDao) {

    suspend fun insert(progress: LessonProgress): Long = dao.insert(progress)

    suspend fun insertAll(progressList: List<LessonProgress>): List<Long> = dao.insertAll(progressList)

    suspend fun update(progress: LessonProgress) = dao.update(progress)

    suspend fun delete(progress: LessonProgress) = dao.delete(progress)

    suspend fun deleteByLessonCardType(lessonId: Int, cardType: CardTypeEnum) =
        dao.deleteByLessonCardType(lessonId, cardType)

    suspend fun getAll(): List<LessonProgress> = dao.getAll()

    suspend fun getById(id: Int): LessonProgress? = dao.getById(id)

    suspend fun getByLessonCardType(lessonId: Int, cardType: CardTypeEnum): List<LessonProgress> =
        dao.getByLessonCardType(lessonId, cardType)

    suspend fun getByWordId(wordId: Int): List<LessonProgress> =
        dao.getByWordId(wordId)
}

