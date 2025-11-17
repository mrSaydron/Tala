package com.example.tala.entity.lessoncardtype

class LessonCardTypeRepository(private val dao: LessonCardTypeDao) {

    suspend fun insert(entity: LessonCardType) = dao.insert(entity)

    suspend fun insertAll(entities: List<LessonCardType>) = dao.insertAll(entities)

    suspend fun delete(entity: LessonCardType) = dao.delete(entity)

    suspend fun deleteByCollectionId(collectionId: Int) = dao.deleteByCollectionId(collectionId)

    suspend fun getAll(): List<LessonCardType> = dao.getAll()

    suspend fun getByCollectionId(collectionId: Int): List<LessonCardType> = dao.getByCollectionId(collectionId)

    suspend fun replaceForCollection(collectionId: Int, entities: List<LessonCardType>) =
        dao.replaceForCollection(collectionId, entities)
}


