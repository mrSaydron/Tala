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
import com.example.tala.model.dto.toEntityCard
import com.example.tala.model.dto.EnterWordCardDto
import com.example.tala.model.dto.ReverseTranslateCardDto
import com.example.tala.model.dto.TranslateCardDto
import com.example.tala.model.enums.StatusEnum
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
            val ef = reviewSettings.getEf(type)
            Card(
                commonId = commonId,
                collectionId = cardDto.collectionId,
                info = (info as? WordCardInfo)?.toJsonOrNull(),
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

    private suspend fun updateSync(cardDto: CardDto) {
        repository.update(cardDto.toEntityCard())
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
                val cardsToInsert = typesToAdd.map { type ->
                    val infoAny = cardDto.cards[type]
                    val ef = reviewSettings.getEf(type)
                    Card(
                        commonId = cardDto.commonId,
                        collectionId = cardDto.collectionId,
                        info = (infoAny as? WordCardInfo)?.toJsonOrNull(),
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
                    val wordInfo = (infoAny as? WordCardInfo)
                    existing.copy(
                        collectionId = cardDto.collectionId,
                        info = wordInfo?.toJsonOrNull()
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

    suspend fun deleteSync(commonId: String) {
        withContext(Dispatchers.IO) {
            repository.delete(commonId)
        }
    }

    fun allCardList(): LiveData<List<CardListDto>> {
        return repository.getAll().map { cards ->
            cards
                .groupBy { it.commonId }
                .map { (_, group) ->
                    val dtos = group.map { it.toCardDto() }
                    val primaryDto = dtos.firstOrNull { it.cardType == CardTypeEnum.TRANSLATE } ?: dtos.first()

                    val cardsMap: Map<CardTypeEnum, com.example.tala.model.dto.info.CardInfo> = dtos.associate { dto ->
                        dto.cardType to (dto.info ?: WordCardInfo())
                    }

                    CardListDto(
                        commonId = primaryDto.commonId,
                        collectionId = primaryDto.collectionId,
                        cards = cardsMap,
                    )
                }
        }
    }

    suspend fun getNextCardDtoToReview(categoryId: Int, currentDate: Long): CardDto? {
        return repository.getNextToReview(categoryId, currentDate)?.toCardDto()
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

    fun getCardListByCategory(categoryId: Int): LiveData<List<CardListDto>> {
        return repository.getByCollection(categoryId).map { cards ->
            cards
                .groupBy { it.commonId }
                .map { (_, group) ->
                    val dtos = group.map { it.toCardDto() }
                    val primaryDto = dtos.firstOrNull { it.cardType == CardTypeEnum.TRANSLATE } ?: dtos.first()

                    val cardsMap: Map<CardTypeEnum, com.example.tala.model.dto.info.CardInfo> = dtos.associate { dto ->
                        dto.cardType to (dto.info ?: WordCardInfo())
                    }

                    CardListDto(
                        commonId = primaryDto.commonId,
                        collectionId = primaryDto.collectionId,
                        cards = cardsMap,
                    )
                }
        }
    }

    suspend fun deleteAllWords() {
        repository.deleteAll()
    }

    suspend fun getCardByTypeAndCommonId(cardType: CardTypeEnum, commonId: String?): CardDto? {
        return repository.byTypeAndCommonId(cardType, commonId)?.toCardDto()
    }

    suspend fun getCardListByCommonId(commonId: String): CardListDto? {
        return withContext(Dispatchers.IO) {
            val group = repository.byCommonId(commonId)
            if (group.isEmpty()) return@withContext null
            val dtos = group.map { it.toCardDto() }
            val primaryDto = dtos.firstOrNull { it.cardType == CardTypeEnum.TRANSLATE } ?: dtos.first()
            val cardsMap: Map<CardTypeEnum, com.example.tala.model.dto.info.CardInfo> = dtos.associate { dto ->
                dto.cardType to (dto.info ?: WordCardInfo())
            }
            CardListDto(
                commonId = primaryDto.commonId,
                collectionId = primaryDto.collectionId,
                cards = cardsMap,
            )
        }
    }

    fun getHardInterval(card: CardDto): String {
        return "<10 мин."
    }

    fun getMediumInterval(card: CardDto): String {
        if (card.status == StatusEnum.NEW || card.status == StatusEnum.PROGRESS_RESET) {
            return "1 дн."
        } else {
            val interval: Int = Math.round(card.intervalMinutes / MINUTES_IN_DAY * card.ef).toInt()
            return "$interval дн."
        }
    }

    fun getEasyInterval(card: CardDto): String {
        if (card.status == StatusEnum.NEW || card.status == StatusEnum.PROGRESS_RESET) {
            return "2 дн."
        } else {
            val interval: Int = Math.round(card.intervalMinutes / MINUTES_IN_DAY * card.ef * 1.5).toInt()
            return "$interval дн."
        }
    }

    suspend fun resultHardSuspend(cardDto: CardDto) {
        Log.i(TAG, "resultHardSuspend")
        val ef = if (cardDto.status == StatusEnum.IN_PROGRESS) {
            maxOf(cardDto.ef - 0.5, 1.3)
        } else {
            cardDto.ef
        }
        val savingDto = copyWith(
            cardDto = cardDto,
            status = StatusEnum.PROGRESS_RESET,
            nextReviewDate = calculateNextReviewDate(10, ChronoUnit.MINUTES),
            ef = ef
        )
        updateSync(savingDto)
    }

    suspend fun resultMediumSuspend(cardDto: CardDto) {
        Log.i(TAG, "resultMediumSuspend")
        val intervalMinutes = if (cardDto.status == StatusEnum.NEW || cardDto.status == StatusEnum.PROGRESS_RESET) {
            MINUTES_IN_DAY
        } else {
            Math.round(cardDto.intervalMinutes * cardDto.ef).toInt()
        }
        val savingDto = copyWith(
            cardDto = cardDto,
            status = StatusEnum.IN_PROGRESS,
            nextReviewDate = calculateNextReviewDate(intervalMinutes, ChronoUnit.MINUTES),
            intervalMinutes = intervalMinutes
        )
        updateSync(savingDto)
    }

    suspend fun resultEasySuspend(cardDto: CardDto) {
        Log.i(TAG, "resultEasySuspend")
        val intervalMinutes: Int
        val ef: Double
        if (cardDto.status == StatusEnum.NEW || cardDto.status == StatusEnum.PROGRESS_RESET) {
            intervalMinutes = MINUTES_IN_TWO_DAYS
            ef = cardDto.ef
        } else {
            intervalMinutes = Math.round(cardDto.intervalMinutes * cardDto.ef * 1.5).toInt()
            ef = cardDto.ef + 0.1
        }

        val savingDto = copyWith(
            cardDto = cardDto,
            status = StatusEnum.IN_PROGRESS,
            nextReviewDate = calculateNextReviewDate(intervalMinutes, ChronoUnit.MINUTES),
            ef = ef,
            intervalMinutes = intervalMinutes
        )
        updateSync(savingDto)
    }

    private fun copyWith(
        cardDto: CardDto,
        nextReviewDate: Long? = null,
        intervalMinutes: Int? = null,
        status: StatusEnum? = null,
        ef: Double? = null,
    ): CardDto {
        return when (cardDto) {
            is TranslateCardDto -> cardDto.copy(
                nextReviewDate = nextReviewDate ?: cardDto.nextReviewDate,
                intervalMinutes = intervalMinutes ?: cardDto.intervalMinutes,
                status = status ?: cardDto.status,
                ef = ef ?: cardDto.ef,
            )
            is ReverseTranslateCardDto -> cardDto.copy(
                nextReviewDate = nextReviewDate ?: cardDto.nextReviewDate,
                intervalMinutes = intervalMinutes ?: cardDto.intervalMinutes,
                status = status ?: cardDto.status,
                ef = ef ?: cardDto.ef,
            )
            is EnterWordCardDto -> cardDto.copy(
                nextReviewDate = nextReviewDate ?: cardDto.nextReviewDate,
                intervalMinutes = intervalMinutes ?: cardDto.intervalMinutes,
                status = status ?: cardDto.status,
                ef = ef ?: cardDto.ef,
            )
            else -> cardDto
        }
    }

    private fun calculateNextReviewDate(add: Int, unit: ChronoUnit): Long {
        return LocalDateTime.now().plus(add.toLong(), unit).atZone(ZoneId.systemDefault()).toEpochSecond()
    }

    companion object {
        private const val TAG = "CardViewModel"
        private const val MINUTES_IN_DAY = 1440
        private const val MINUTES_IN_TWO_DAYS = 2880

    }
}