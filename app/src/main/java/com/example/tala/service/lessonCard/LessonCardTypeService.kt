package com.example.tala.service.lessonCard

import com.example.tala.entity.cardhistory.CardHistory
import com.example.tala.entity.dictionary.Dictionary
import com.example.tala.entity.lessonprogress.LessonProgress
import com.example.tala.model.dto.lessonCard.LessonCardDto
import com.example.tala.model.enums.CardTypeEnum
import com.example.tala.model.enums.StatusEnum
import com.example.tala.service.lessonCard.model.CardAnswer

interface LessonCardTypeService {
    suspend fun createProgress(lessonId: Int, words: List<Dictionary>)
    suspend fun getCards(cardProgress: List<LessonProgress>): List<LessonCardDto>
    suspend fun answerResult(
        card: LessonCardDto,
        answer: CardAnswer?,
        quality: Int,
        currentTimeMillis: Long
    ): LessonCardDto?

    fun buildEntry(
        lessonId: Int,
        cardType: CardTypeEnum,
        dictionaryId: Int?,
        quality: Int,
        timestamp: Long
    ): CardHistory {
        val clampedQuality = quality.coerceIn(MIN_QUALITY, MAX_QUALITY)
        return CardHistory(
            lessonId = lessonId,
            cardType = cardType,
            dictionaryId = dictionaryId,
            quality = clampedQuality,
            date = timestamp
        )
    }

    fun isProgressReady(progress: LessonProgress, now: Long): Boolean {
        if (progress.status == StatusEnum.NEW || progress.status == StatusEnum.PROGRESS_RESET) {
            return true
        }
        val nextReviewDate = progress.nextReviewDate ?: return true
        return nextReviewDate <= now
    }

    companion object {
        private const val MIN_QUALITY = 0
        private const val MAX_QUALITY = 5
    }
}