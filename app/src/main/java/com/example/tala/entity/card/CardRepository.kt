package com.example.tala.entity.card

import android.util.Log
import androidx.lifecycle.LiveData
import com.example.tala.model.enums.CardTypeEnum
import com.example.tala.model.enums.StatusEnum

class CardRepository(private val cardDao: CardDao) {

    suspend fun byTypeAndCommonId(cardType: CardTypeEnum, commonId: String?): Card? {
        return cardDao.byTypeAndCommonId(cardType, commonId)
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

    suspend fun getNextToReview(categoryId: Int, currentDate: Long): Card? {
        return cardDao.getNextToReview(categoryId, currentDate)
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
        categoryId: Int,
    ): LiveData<Int> {
        return cardDao.getCountToReview(currentDate, status, categoryId)
    }

    suspend fun updateImagePath(id: Int, imagePath: String) {
        cardDao.updateImagePath(id, imagePath)
    }

    fun getByCategory(categoryId: Int): LiveData<List<Card>> {
        return cardDao.getByCategory(categoryId)
    }

    suspend fun deleteAll() {
        cardDao.deleteAll()
    }

    fun getAllByType(cardType: CardTypeEnum): LiveData<List<Card>> {
        return cardDao.getAllByType(cardType)
    }

    fun getAllByTypeAndCategory(cardType: CardTypeEnum, categoryId: Int): LiveData<List<Card>> {
        return cardDao.getAllByTypeAndCategory(cardType, categoryId)
    }

}