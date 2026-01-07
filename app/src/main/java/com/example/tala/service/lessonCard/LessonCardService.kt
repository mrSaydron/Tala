package com.example.tala.service.lessonCard

import com.example.tala.entity.cardhistory.CardHistoryRepository
import com.example.tala.entity.word.WordRepository
import com.example.tala.entity.lesson.LessonRepository
import com.example.tala.entity.lessoncardtype.LessonCardTypeRepository
import com.example.tala.entity.lessoncardtype.LessonCardType
import com.example.tala.entity.lessonprogress.LessonProgressRepository
import com.example.tala.entity.wordCollection.WordCollectionRepository
import com.example.tala.model.dto.lessonCard.LessonCardDto
import com.example.tala.model.dto.lessonCard.EnterWordLessonCardDto
import com.example.tala.model.dto.lessonCard.ReverseTranslateLessonCardDto
import com.example.tala.model.dto.lessonCard.TranslateLessonCardDto
import com.example.tala.model.dto.lessonCard.TranslationComparisonLessonCardDto
import com.example.tala.model.enums.CardTypeEnum
import com.example.tala.model.enums.StatusEnum
import com.example.tala.service.lessonCard.model.CardAnswer

class LessonCardService(
    private val lessonRepository: LessonRepository,
    private val lessonCardTypeRepository: LessonCardTypeRepository,
    private val wordCollectionRepository: WordCollectionRepository,
    private val wordRepository: WordRepository,
    private val cardHistoryRepository: CardHistoryRepository,
    private val lessonProgressRepository: LessonProgressRepository,
    private val typeServices: Map<CardTypeEnum, LessonCardTypeService>,
    private val timeProvider: () -> Long = System::currentTimeMillis
) {

    suspend fun createProgress(lessonId: Int) {
        val lesson = lessonRepository.getById(lessonId) ?: return
        val lessonCardTypes = lessonCardTypeRepository.getByCollectionId(lesson.collectionId)
        if (lessonCardTypes.isEmpty()) return

        val wordIds = wordCollectionRepository.getWordIds(lesson.collectionId)
        if (wordIds.isEmpty()) return

        val words = wordRepository.getByIds(wordIds)
        if (words.isEmpty()) return

        lessonCardTypes.forEach { lessonCardType ->
            val service = typeServices[lessonCardType.cardType] ?: return@forEach
            service.createProgress(lessonId, words)
        }
    }

    suspend fun getCards(lessonId: Int): List<LessonCardDto> {
        val lesson = lessonRepository.getById(lessonId) ?: return emptyList()
        val lessonCardTypes = lessonCardTypeRepository.getByCollectionId(lesson.collectionId)
        if (lessonCardTypes.isEmpty()) return emptyList()

        val filteredTypes = filterCardTypesByConditions(lessonId, lessonCardTypes)
        if (filteredTypes.isEmpty()) return emptyList()

        val now = timeProvider()

        val cards = filteredTypes.flatMap { lessonCardType ->
            val service = typeServices[lessonCardType.cardType] ?: return@flatMap emptyList()
            val progress = lessonProgressRepository.getByLessonCardType(lessonId, lessonCardType.cardType)
            service.getCards(progress)
        }
        if (cards.isEmpty()) return emptyList()

        val readyCards = cards.filter { isCardReady(it, now) }
        if (readyCards.isEmpty()) return emptyList()

        return readyCards.sortedBy { resolveNextReviewTimestamp(it) }
    }

    suspend fun answerResult(card: LessonCardDto, answer: CardAnswer?, quality: Int): LessonCardDto? {
        val service = typeServices[card.type] ?: return null
        val now = timeProvider()
        val result = service.answerResult(card, answer, quality, now)
        return result
    }

    /**
     * Возвращает количество карточек в каждом статусе для переданного списка.
     * Карточки с неизвестным статусом пропускаются.
     */
    fun countCardsByStatus(cards: List<LessonCardDto>): Map<StatusEnum, Int> {
        if (cards.isEmpty()) return emptyMap()
        return cards.mapNotNull { it.statusAndNextReview()?.first }
            .groupingBy { it }
            .eachCount()
    }

    /**
     * Возвращает количество карточек в каждом статусе для указанного урока.
     * Использует {@link #getCards(Int)} для получения актуального набора карточек.
     */
    suspend fun countCardsByStatus(lessonId: Int): Map<StatusEnum, Int> {
        val cards = getCards(lessonId)
        if (cards.isEmpty()) return emptyMap()
        return countCardsByStatus(cards)
    }

    private suspend fun filterCardTypesByConditions(
        lessonId: Int,
        lessonCardTypes: List<LessonCardType>
    ): List<LessonCardType> {
        if (lessonCardTypes.isEmpty()) return emptyList()

        val typesWithConditions = lessonCardTypes.filter {
            (it.conditionOnCardType != null && it.conditionOnValue != null) ||
                (it.conditionOffCardType != null && it.conditionOffValue != null)
        }
        if (typesWithConditions.isEmpty()) {
            return lessonCardTypes
        }

        val correctAnswersCount = cardHistoryRepository.getByLesson(lessonId)
            .asSequence()
            .filter { it.quality > 0 }
            .groupingBy { it.cardType }
            .eachCount()

        return lessonCardTypes.filter { type ->
            val meetsOnCondition = if (type.conditionOnCardType != null && type.conditionOnValue != null) {
                val count = correctAnswersCount[type.conditionOnCardType] ?: 0
                count >= type.conditionOnValue
            } else {
                true
            }

            val meetsOffCondition = if (type.conditionOffCardType != null && type.conditionOffValue != null) {
                val count = correctAnswersCount[type.conditionOffCardType] ?: 0
                count < type.conditionOffValue
            } else {
                true
            }

            meetsOnCondition && meetsOffCondition
        }
    }

    private fun isCardReady(card: LessonCardDto, now: Long): Boolean {
        val (status, nextReviewDate) = card.statusAndNextReview() ?: return true
        if (status == StatusEnum.NEW || status == StatusEnum.PROGRESS_RESET) return true
        return nextReviewDate?.let { it <= now } ?: true
    }

    private fun resolveNextReviewTimestamp(card: LessonCardDto): Long {
        val nextReviewDate = card.statusAndNextReview()?.second ?: return Long.MIN_VALUE
        return nextReviewDate ?: Long.MIN_VALUE
    }

    private fun LessonCardDto.statusAndNextReview(): Pair<StatusEnum, Long?>? {
        return when (this) {
            is TranslateLessonCardDto -> status to nextReviewDate
            is ReverseTranslateLessonCardDto -> status to nextReviewDate
            is EnterWordLessonCardDto -> status to nextReviewDate
            is TranslationComparisonLessonCardDto -> {
                val reference = items.minByOrNull { it.nextReviewDate ?: Long.MIN_VALUE }
                reference?.let { it.status to it.nextReviewDate }
            }
            else -> null
        }
    }
}