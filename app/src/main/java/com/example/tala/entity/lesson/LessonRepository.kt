package com.example.tala.entity.lesson

class LessonRepository(private val dao: LessonDao) {

    suspend fun insert(lesson: Lesson): Long = dao.insert(lesson)

    suspend fun update(lesson: Lesson) = dao.update(lesson)

    suspend fun delete(lesson: Lesson) = dao.delete(lesson)

    suspend fun getAll(): List<Lesson> = dao.getAll()

    suspend fun getById(id: Int): Lesson? = dao.getById(id)

    suspend fun getByCollectionId(collectionId: Int): List<Lesson> = dao.getByCollectionId(collectionId)
}

