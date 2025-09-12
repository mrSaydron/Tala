package com.example.tala.entity.card

import android.util.Log
import androidx.lifecycle.LiveData
import com.example.tala.model.enums.CardTypeEnum
import com.example.tala.model.enums.StatusEnum

class CardRepository(private val cardDao: CardDao) {

    suspend fun byTypeAndCommonId(cardType: CardTypeEnum, commonId: String?): Card? {
        return cardDao.byTypeAndCommonId(cardType, commonId)
    }

    suspend fun byCommonId(commonId: String): List<Card> {
        return cardDao.getByCommonId(commonId)
    }

    suspend fun insert(card: Card) {
        return cardDao.insert(card)
    }

    suspend fun insertAll(cards: List<Card>) {
        return cardDao.insertAll(cards)
    }

    suspend fun update(card: Card) {
        return cardDao.update(card)
    }

    suspend fun updateAll(cards: List<Card>) {
        Log.i("CardRepository", "updateAll: $cards")
        return cardDao.updateAll(cards)
    }

    suspend fun delete(card: Card) {
        cardDao.delete(card)
    }

    suspend fun delete(commonId: String) {
        cardDao.delete(commonId)
    }

    fun getAll(): LiveData<List<Card>> {
        return cardDao.getAll()
    }

    suspend fun getNextToReview(currentDate: Long): Card? {
        return cardDao.getNextToReview(currentDate)
    }

    suspend fun getNextToReview(collectionId: Int, currentDate: Long): Card? {
        return cardDao.getNextToReview(collectionId, currentDate)
    }

    suspend fun update(id: Int, nextReviewDate: Long, interval: Int) {
        cardDao.update(id, nextReviewDate, interval)
    }

    fun getCountToReview(currentDate: Long, status: StatusEnum): LiveData<Int> {
        return cardDao.getCountToReview(currentDate, status)
    }

    fun getCountToReview(
        currentDate: Long,
        status: StatusEnum,
        collectionId: Int,
    ): LiveData<Int> {
        return cardDao.getCountToReview(currentDate, status, collectionId)
    }

    // imagePath теперь хранится в info; отдельный апдейт не требуется

    fun getByCollection(collectionId: Int): LiveData<List<Card>> {
        return cardDao.getByCollection(collectionId)
    }

    fun getCountByCollection(collectionId: Int): LiveData<Int> {
        return cardDao.getCountByCollection(collectionId)
    }

    suspend fun deleteAll() {
        cardDao.deleteAll()
    }

    suspend fun deleteByCollection(collectionId: Int) {
        cardDao.deleteByCollection(collectionId)
    }

    fun getAllByType(cardType: CardTypeEnum): LiveData<List<Card>> {
        return cardDao.getAllByType(cardType)
    }

    fun getAllByTypeAndCollection(cardType: CardTypeEnum, collectionId: Int): LiveData<List<Card>> {
        return cardDao.getAllByTypeAndCollection(cardType, collectionId)
    }

    // Новые обёртки для выборок свободного/планового обучения
    suspend fun getAllDue(collectionId: Int, endFindDate: Long): List<Card> {
        return cardDao.getAllDue(collectionId, endFindDate)
    }

    suspend fun getRandom(collectionId: Int, limit: Int): List<Card> {
        return cardDao.getRandom(collectionId, limit)
    }

    suspend fun getHard(collectionId: Int, limit: Int): List<Card> {
        return cardDao.getHard(collectionId, limit)
    }

    suspend fun getSoon(collectionId: Int, limit: Int): List<Card> {
        return cardDao.getSoon(collectionId, limit)
    }

}