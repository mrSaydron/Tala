package com.example.tala.entity.card

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.map
import androidx.lifecycle.viewModelScope
import com.example.tala.TalaDatabase
import com.example.tala.model.dto.CardListDto
import com.example.tala.model.enums.CardTypeEnum
import com.example.tala.model.enums.StatusEnum
import com.example.tala.service.card.CardTypeFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import java.util.UUID

class CardViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: CardRepository

    init {
        val wordDao = TalaDatabase.getDatabase(application).cardDao()
        repository = CardRepository(wordDao)
    }

    fun insert(cardDto: CardListDto) = viewModelScope.launch {
        val commonId = UUID.randomUUID().toString()
        val cardDtoWithCommonId = cardDto.copy(commonId = commonId)
        CardTypeEnum.entries
            .filter { it.use }
            .map { CardTypeFactory.getCardType(it).create(cardDtoWithCommonId) }
            .let { repository.insertAll(it) }
    }

    private fun update(card: Card) = viewModelScope.launch {
        repository.update(card)
    }

    fun update(cardDto: CardListDto) = viewModelScope.launch(Dispatchers.IO) {
        try {
            CardTypeEnum.entries
                .filter { it.use }
                .mapNotNull { cardType ->
                    val card = repository.byTypeAndCommonId(cardType, cardDto.commonId)
                    Log.i("CardViewModel", "update: $card")
                    card?.let { CardTypeFactory.getCardType(cardType).update(cardDto, card) }
                }
                .let { cards -> repository.updateAll(cards) }
        } catch (e: Exception) {
            Log.e("CardViewModel", "update:", e)
        }
    }

    fun delete(card: CardListDto) = viewModelScope.launch {
        repository.delete(card.commonId!!)
    }

    fun allCardList(): LiveData<List<CardListDto>> {
        return repository.getAllByType(CardTypeEnum.TRANSLATE).map {
            it.map { card -> CardTypeFactory.getCardType(CardTypeEnum.TRANSLATE).toListDto(card) }
        }
    }

    fun allCards(): LiveData<List<Card>> {
        return repository.getAll()
    }

    suspend fun getNextWordToReview(currentDate: Long): Card? {
        return repository.getNextToReview(currentDate)
    }

    suspend fun getNextWordToReview(categoryId: Int, currentDate: Long): Card? {
        return repository.getNextToReview(categoryId, currentDate)
    }

    fun updateWord(id: Int, nextReviewDate: Long, interval: Int) = viewModelScope.launch {
        repository.update(id, nextReviewDate, interval)
    }

    fun getNewCardsCount(): LiveData<Int> {
        val endDate = LocalDate.now().plusDays(1).atStartOfDay().atZone(ZoneId.systemDefault()).toEpochSecond()
        return repository.getCountToReview(endDate, StatusEnum.NEW)
    }

    fun getResetCardsCount(): LiveData<Int> {
        val endDate = LocalDate.now().plusDays(1).atStartOfDay().atZone(ZoneId.systemDefault()).toEpochSecond()
        return repository.getCountToReview(endDate, StatusEnum.PROGRESS_RESET)
    }

    fun getInProgressCardCount(): LiveData<Int> {
        val endDate = LocalDate.now().plusDays(1).atStartOfDay().atZone(ZoneId.systemDefault()).toEpochSecond()
        return repository.getCountToReview(endDate, StatusEnum.IN_PROGRESS)
    }

    fun updateImagePath(id: Int, imagePath: String) = viewModelScope.launch {
        repository.updateImagePath(id, imagePath)
    }

    fun getCardsByCategory(categoryId: Int): LiveData<List<Card>> {
        return repository.getByCategory(categoryId)
    }

    fun getCardListByCategory(categoryId: Int): LiveData<List<CardListDto>> {
        return repository.getAllByTypeAndCategory(CardTypeEnum.TRANSLATE, categoryId).map {
            it.map { card -> CardTypeFactory.getCardType(CardTypeEnum.TRANSLATE).toListDto(card) }
        }
    }

    suspend fun deleteAllWords() {
        repository.deleteAll()
    }

    fun getHardInterval(card: Card): String {
        return "<10 мин."
    }

    fun getMediumInterval(card: Card): String {
        if (card.status == StatusEnum.NEW || card.status == StatusEnum.PROGRESS_RESET) {
            return "1 дн."
        } else {
            val interval: Int = Math.round(card.interval * card.ef).toInt()
            return "$interval дн."
        }
    }

    fun getEasyInterval(card: Card): String {
        if (card.status == StatusEnum.NEW || card.status == StatusEnum.PROGRESS_RESET) {
            return "2 дн."
        } else {
            val interval: Int = Math.round(card.interval * card.ef * 1.5).toInt()
            return "$interval дн."
        }
    }

    fun resultHard(card: Card) {
        Log.i(TAG, "resultHard")
        val ef = if (card.status == StatusEnum.IN_PROGRESS) {
            maxOf(card.ef - 0.5, 1.3)
        } else {
            card.ef
        }
        val savingWord = card.copy(
            status = StatusEnum.PROGRESS_RESET,
            nextReviewDate = calculateNextReviewDate(10, ChronoUnit.MINUTES),
            ef = ef
        )
        update(savingWord)
    }

    fun resultMedium(card: Card) {
        Log.i(TAG, "resultMedium")
        val interval = Math.round(card.interval * card.ef).toInt()
        val savingWord = card.copy(
            status = StatusEnum.IN_PROGRESS,
            nextReviewDate = calculateNextReviewDate(interval, ChronoUnit.DAYS)
        )
        update(savingWord)
    }

    fun resultEasy(card: Card) {
        Log.i(TAG, "resultEasy")
        val interval = Math.round(card.interval * card.ef * 1.5).toInt()
        val ef = card.ef + 0.1
        val savingWord = card.copy(
            status = StatusEnum.IN_PROGRESS,
            nextReviewDate = calculateNextReviewDate(interval, ChronoUnit.DAYS),
            ef = ef
        )
        update(savingWord)
    }

    private fun calculateNextReviewDate(add: Int, unit: ChronoUnit): Long {
        return LocalDateTime.now().plus(add.toLong(), unit).atZone(ZoneId.systemDefault()).toEpochSecond()
    }

    companion object {
        private const val TAG = "CardViewModel"

    }
}