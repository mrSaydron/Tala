package com.example.tala.entity.lessoncardtype

class LessonCardTypeRepository(private val dao: LessonCardTypeDao) {

    suspend fun insert(entity: LessonCardType) = dao.insert(entity)

    suspend fun insertAll(entities: List<LessonCardType>) = dao.insertAll(entities)

    suspend fun delete(entity: LessonCardType) = dao.delete(entity)

    suspend fun deleteByLessonId(lessonId: Int) = dao.deleteByLessonId(lessonId)

    suspend fun getAll(): List<LessonCardType> = dao.getAll()

    suspend fun getByLessonId(lessonId: Int): List<LessonCardType> = dao.getByLessonId(lessonId)
}


