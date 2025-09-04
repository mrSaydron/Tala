package com.example.tala.entity.card

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.map
import androidx.lifecycle.viewModelScope
import com.example.tala.TalaDatabase
import com.example.tala.ReviewSettings
import com.example.tala.model.dto.CardListDto
import com.example.tala.model.dto.info.WordCardInfo
import com.example.tala.model.enums.CardTypeEnum
import com.example.tala.model.dto.CardDto
import com.example.tala.model.dto.toCardDto
import com.example.tala.model.enums.StatusEnum
import com.example.tala.service.card.CardTypeFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import java.util.UUID

class CardViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: CardRepository
    private val reviewSettings: ReviewSettings

    init {
        val wordDao = TalaDatabase.getDatabase(application).cardDao()
        repository = CardRepository(wordDao)
        reviewSettings = ReviewSettings(application.applicationContext)
    }

    fun insert(cardDto: CardListDto) = viewModelScope.launch {
        if (cardDto.cards.isEmpty()) {
            Log.e(TAG, "insert: cards is empty, nothing to insert")
            return@launch
        }
        val commonId = UUID.randomUUID().toString()

        val entities: List<Card> = cardDto.cards.mapNotNull { (type, info) ->
            val wordInfo = (info as? WordCardInfo) ?: WordCardInfo(
                english = cardDto.english,
                russian = cardDto.russian,
                imagePath = cardDto.imagePath,
                hint = try { cardDto.info?.let { org.json.JSONObject(it).optString("hint", null) } } catch (_: Exception) { null }
            )

            val ef = reviewSettings.getEf(type)
            Card(
                commonId = commonId,
                english = wordInfo.english ?: cardDto.english,
                russian = wordInfo.russian ?: cardDto.russian,
                categoryId = cardDto.categoryId,
                imagePath = wordInfo.imagePath ?: cardDto.imagePath,
                info = wordInfo.toJsonOrNull(),
                cardType = type,
                ef = ef,
            )
        }

        if (entities.isEmpty()) {
            Log.e(TAG, "insert: built entities is empty, nothing to insert")
            return@launch
        }
        repository.insertAll(entities)
    }

    private fun update(card: Card) = viewModelScope.launch {
        repository.update(card)
    }

    // Synchronous (suspend) update helper for deterministic flows (e.g., after-answer actions)
    private suspend fun updateSync(card: Card) {
        repository.update(card)
    }

    fun update(cardDto: CardListDto) = viewModelScope.launch(Dispatchers.IO) {
        try {
            if (cardDto.cards.isEmpty()) {
                Log.e(TAG, "update: cards is empty, nothing to update")
                return@launch
            }

            // 1) Получаем все карточки по commonId за один запрос
            val existingList = repository.byCommonId(cardDto.commonId!!)
            val existingMap: Map<CardTypeEnum, Card> = existingList.associateBy { it.cardType }

            val existingTypes = existingMap.keys
            val desiredTypes = cardDto.cards.keys

            // 2) Типы к удалению: были раньше, но теперь не нужны
            val typesToDelete = existingTypes - desiredTypes
            typesToDelete.forEach { type ->
                existingMap[type]?.let { repository.delete(it) }
            }

            // 3) Типы к добавлению: нужны теперь, но их не было
            val typesToAdd = desiredTypes - existingTypes
            if (typesToAdd.isNotEmpty()) {
                val cardsToInsert = typesToAdd.mapNotNull { type ->
                    val infoAny = cardDto.cards[type]
                    val wordInfo = (infoAny as? com.example.tala.model.dto.info.WordCardInfo) ?: com.example.tala.model.dto.info.WordCardInfo(
                        english = cardDto.english,
                        russian = cardDto.russian,
                        imagePath = cardDto.imagePath,
                        hint = try { cardDto.info?.let { org.json.JSONObject(it).optString("hint", null) } } catch (_: Exception) { null }
                    )
                    val ef = reviewSettings.getEf(type)
                    Card(
                        commonId = cardDto.commonId,
                        english = wordInfo.english ?: cardDto.english,
                        russian = wordInfo.russian ?: cardDto.russian,
                        categoryId = cardDto.categoryId,
                        imagePath = wordInfo.imagePath ?: cardDto.imagePath,
                        info = wordInfo.toJsonOrNull(),
                        cardType = type,
                        ef = ef,
                    )
                }
                if (cardsToInsert.isNotEmpty()) {
                    repository.insertAll(cardsToInsert)
                }
            }

            // 4) Типы к обновлению: были и остаются — обновляем поля
            val typesToUpdate = desiredTypes intersect existingTypes
            if (typesToUpdate.isNotEmpty()) {
                val cardsToUpdate = typesToUpdate.mapNotNull { type ->
                    val existing = existingMap[type] ?: return@mapNotNull null
                    val infoAny = cardDto.cards[type]
                    val wordInfo = (infoAny as? com.example.tala.model.dto.info.WordCardInfo) ?: com.example.tala.model.dto.info.WordCardInfo(
                        english = cardDto.english,
                        russian = cardDto.russian,
                        imagePath = cardDto.imagePath,
                        hint = try { cardDto.info?.let { org.json.JSONObject(it).optString("hint", null) } } catch (_: Exception) { null }
                    )
                    existing.copy(
                        english = wordInfo.english ?: existing.english,
                        russian = wordInfo.russian ?: existing.russian,
                        categoryId = cardDto.categoryId,
                        imagePath = wordInfo.imagePath ?: existing.imagePath,
                        info = wordInfo.toJsonOrNull()
                    )
                }
                if (cardsToUpdate.isNotEmpty()) {
                    repository.updateAll(cardsToUpdate)
                }
            }
        } catch (e: Exception) {
            Log.e("CardViewModel", "update:", e)
        }
    }

    fun delete(card: CardListDto) = viewModelScope.launch(Dispatchers.IO) {
        repository.delete(card.commonId!!)
    }

    suspend fun deleteSync(commonId: String) {
        withContext(Dispatchers.IO) {
            repository.delete(commonId)
        }
    }

    fun allCardList(): LiveData<List<CardListDto>> {
        return repository.getAll().map { cards ->
            cards
                .groupBy { it.commonId ?: "__single_${'$'}{it.id}" }
                .map { (_, group) ->
                    val dtos = group.map { it.toCardDto() }
                    val primaryDto = dtos.firstOrNull { it.cardType == CardTypeEnum.TRANSLATE } ?: dtos.first()
                    val primaryInfo = primaryDto.info as? WordCardInfo

                    val cardsMap: Map<CardTypeEnum, com.example.tala.model.dto.info.CardInfo> = dtos.associate { dto ->
                        dto.cardType to (dto.info ?: WordCardInfo())
                    }

                    CardListDto(
                        commonId = primaryDto.commonId,
                        english = primaryInfo?.english ?: "",
                        russian = primaryInfo?.russian ?: "",
                        categoryId = primaryDto.categoryId,
                        imagePath = primaryInfo?.imagePath,
                        info = primaryInfo?.toJsonOrNull(),
                        types = dtos.map { it.cardType }.toSet(),
                        cards = cardsMap,
                    )
                }
        }
    }

    fun allCards(): LiveData<List<Card>> {
        return repository.getAll()
    }

    fun allCardDtos(): LiveData<List<CardDto>> {
        return repository.getAll().map { cards ->
            cards.map { it.toCardDto() }
        }
    }

    suspend fun getNextWordToReview(currentDate: Long): Card? {
        return repository.getNextToReview(currentDate)
    }

    suspend fun getNextWordToReview(categoryId: Int, currentDate: Long): Card? {
        return repository.getNextToReview(categoryId, currentDate)
    }

    suspend fun getNextCardDtoToReview(currentDate: Long): CardDto? {
        return repository.getNextToReview(currentDate)?.toCardDto()
    }

    suspend fun getNextCardDtoToReview(categoryId: Int, currentDate: Long): CardDto? {
        return repository.getNextToReview(categoryId, currentDate)?.toCardDto()
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

    fun getNewCardsCountByCategory(categoryId: Int): LiveData<Int> {
        val endDate = LocalDate.now().plusDays(1).atStartOfDay().atZone(ZoneId.systemDefault()).toEpochSecond()
        return repository.getCountToReview(endDate, StatusEnum.NEW, categoryId)
    }

    fun getResetCardsCountByCategory(categoryId: Int): LiveData<Int> {
        val endDate = LocalDate.now().plusDays(1).atStartOfDay().atZone(ZoneId.systemDefault()).toEpochSecond()
        return repository.getCountToReview(endDate, StatusEnum.PROGRESS_RESET, categoryId)
    }

    fun getInProgressCardCountByCategory(categoryId: Int): LiveData<Int> {
        val endDate = LocalDate.now().plusDays(1).atStartOfDay().atZone(ZoneId.systemDefault()).toEpochSecond()
        return repository.getCountToReview(endDate, StatusEnum.IN_PROGRESS, categoryId)
    }

    fun updateImagePath(id: Int, imagePath: String) = viewModelScope.launch {
        repository.updateImagePath(id, imagePath)
    }

    fun getCardsByCategory(categoryId: Int): LiveData<List<Card>> {
        return repository.getByCategory(categoryId)
    }

    fun getCardDtosByCategory(categoryId: Int): LiveData<List<CardDto>> {
        return repository.getByCategory(categoryId).map { cards ->
            cards.map { it.toCardDto() }
        }
    }

    fun getCardListByCategory(categoryId: Int): LiveData<List<CardListDto>> {
        return repository.getByCategory(categoryId).map { cards ->
            cards
                .groupBy { it.commonId ?: "__single_${'$'}{it.id}" }
                .map { (_, group) ->
                    val dtos = group.map { it.toCardDto() }
                    val primaryDto = dtos.firstOrNull { it.cardType == CardTypeEnum.TRANSLATE } ?: dtos.first()
                    val primaryInfo = primaryDto.info as? WordCardInfo

                    val cardsMap: Map<CardTypeEnum, com.example.tala.model.dto.info.CardInfo> = dtos.associate { dto ->
                        dto.cardType to (dto.info ?: WordCardInfo())
                    }

                    CardListDto(
                        commonId = primaryDto.commonId,
                        english = primaryInfo?.english ?: "",
                        russian = primaryInfo?.russian ?: "",
                        categoryId = primaryDto.categoryId,
                        imagePath = primaryInfo?.imagePath,
                        info = primaryInfo?.toJsonOrNull(),
                        types = dtos.map { it.cardType }.toSet(),
                        cards = cardsMap,
                    )
                }
        }
    }

    suspend fun deleteAllWords() {
        repository.deleteAll()
    }

    suspend fun getCardByTypeAndCommonId(cardType: CardTypeEnum, commonId: String?): Card? {
        return repository.byTypeAndCommonId(cardType, commonId)
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

    suspend fun resultHardSuspend(card: Card) {
        Log.i(TAG, "resultHardSuspend")
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
        updateSync(savingWord)
    }

    suspend fun resultMediumSuspend(card: Card) {
        Log.i(TAG, "resultMediumSuspend")
        val interval = if (card.status == StatusEnum.NEW || card.status == StatusEnum.PROGRESS_RESET) {
            1
        } else {
            Math.round(card.interval * card.ef).toInt()
        }
        val savingWord = card.copy(
            status = StatusEnum.IN_PROGRESS,
            nextReviewDate = calculateNextReviewDate(interval, ChronoUnit.DAYS)
        )
        updateSync(savingWord)
    }

    suspend fun resultEasySuspend(card: Card) {
        Log.i(TAG, "resultEasySuspend")
        val interval: Int
        val ef: Double
        if (card.status == StatusEnum.NEW || card.status == StatusEnum.PROGRESS_RESET) {
            interval = 2
            ef = card.ef
        } else {
            interval = Math.round(card.interval * card.ef * 1.5).toInt()
            ef = card.ef + 0.1
        }
        val savingWord = card.copy(
            status = StatusEnum.IN_PROGRESS,
            nextReviewDate = calculateNextReviewDate(interval, ChronoUnit.DAYS),
            ef = ef
        )
        updateSync(savingWord)
    }

    private fun calculateNextReviewDate(add: Int, unit: ChronoUnit): Long {
        return LocalDateTime.now().plus(add.toLong(), unit).atZone(ZoneId.systemDefault()).toEpochSecond()
    }

    companion object {
        private const val TAG = "CardViewModel"

    }
}