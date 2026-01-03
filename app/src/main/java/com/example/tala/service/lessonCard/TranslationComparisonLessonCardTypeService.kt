package com.example.tala.service.lessonCard

import com.example.tala.entity.cardhistory.CardHistory
import com.example.tala.entity.cardhistory.CardHistoryRepository
import com.example.tala.entity.word.Word
import com.example.tala.entity.word.WordRepository
import com.example.tala.entity.lessonprogress.LessonProgress
import com.example.tala.entity.lessonprogress.LessonProgressRepository
import com.example.tala.model.dto.lessonCard.EnterWordLessonCardDto
import com.example.tala.model.dto.lessonCard.LessonCardDto
import com.example.tala.model.dto.lessonCard.ReverseTranslateLessonCardDto
import com.example.tala.model.dto.lessonCard.TranslateLessonCardDto
import com.example.tala.model.dto.lessonCard.TranslationComparisonLessonCardDto
import com.example.tala.model.enums.CardTypeEnum
import com.example.tala.model.enums.StatusEnum
import com.example.tala.service.lessonCard.model.CardAnswer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.roundToLong

class TranslationComparisonLessonCardTypeService(
    private val lessonProgressRepository: LessonProgressRepository,
    private val wordRepository: WordRepository,
    private val cardHistoryRepository: CardHistoryRepository,
    private val timeProvider: () -> Long = System::currentTimeMillis
) : LessonCardTypeService {

    override suspend fun createProgress(lessonId: Int, words: List<Word>) {
        withContext(Dispatchers.IO) {
            val existingDictionaryIds = lessonProgressRepository
                .getByLessonCardType(lessonId, CardTypeEnum.TRANSLATION_COMPARISON)
                .mapNotNull { it.wordId }
                .toSet()

            val candidates = words
                .asSequence()
                .filter { it.baseWordId == null || it.baseWordId == it.id }
                .filterNot { existingDictionaryIds.contains(it.id) }
                .toList()

            if (candidates.isEmpty()) {
                return@withContext
            }

            val createdAt = timeProvider()
            val progressEntries = candidates.map { dictionary ->
                LessonProgress(
                    lessonId = lessonId,
                    cardType = CardTypeEnum.TRANSLATION_COMPARISON,
                    wordId = dictionary.id,
                    nextReviewDate = createdAt,
                    intervalMinutes = MINUTES_IN_DAY,
                    ef = DEFAULT_EF,
                    status = StatusEnum.NEW,
                    info = null
                )
            }
            if (progressEntries.isNotEmpty()) {
                lessonProgressRepository.insertAll(progressEntries)
            }
        }
    }

    override suspend fun getCards(cardProgress: List<LessonProgress>): List<LessonCardDto> {
        if (cardProgress.isEmpty()) return emptyList()

        val now = timeProvider()
        val readyProgress = cardProgress.filter { isProgressReady(it, now) }
        if (readyProgress.size < MIN_ITEMS_PER_CARD) return emptyList()

        val shuffled = readyProgress.shuffled()
        val wordIds = shuffled.mapNotNull { it.wordId }.distinct()
        val words = wordRepository.getByIds(wordIds).associateBy { it.id }

        val groups = shuffled.chunked(MAX_ITEMS_PER_CARD).filter { it.size >= MIN_ITEMS_PER_CARD }
        return groups.map { group ->
            TranslationComparisonLessonCardDto.fromProgress(
                lessonId = group.first().lessonId,
                progresses = group,
                dictionaries = words
            )
        }
    }

    override suspend fun answerResult(
        card: LessonCardDto,
        answer: CardAnswer?,
        @Suppress("UNUSED_PARAMETER") quality: Int,
        currentTimeMillis: Long
    ): LessonCardDto? {
        val dto = card as? TranslationComparisonLessonCardDto ?: return null
        logHistory(card, answer, currentTimeMillis)

        val progressList = dto.items.mapNotNull { lessonProgressRepository.getById(it.progressId) }
        if (progressList.isEmpty()) return null

        val answerMap = (answer as? CardAnswer.Comparison)?.matches?.associateBy { it.progressId } ?: emptyMap()
        val words = wordRepository
            .getByIds(progressList.mapNotNull { it.wordId })
            .associateBy { it.id }

        val updatedProgresses = mutableListOf<LessonProgress>()
        var shouldRepeat = false
        progressList.forEach { progress ->
            val match = answerMap[progress.id]
            val isCorrect = match != null && match.selectedWordId == progress.wordId
            if (!isCorrect) {
                shouldRepeat = true
            }
            val itemQuality = if (isCorrect) MAX_QUALITY else MIN_QUALITY
            val updated = updateProgressForQuality(progress, itemQuality, currentTimeMillis)
            lessonProgressRepository.update(updated)
            updatedProgresses.add(updated)
        }

        return if (shouldRepeat) {
            TranslationComparisonLessonCardDto.fromProgress(
                lessonId = dto.lessonId,
                progresses = updatedProgresses,
                dictionaries = words
            )
        } else {
            null
        }
    }

    private suspend fun logHistory(
        card: TranslationComparisonLessonCardDto,
        answer: CardAnswer?,
        timestamp: Long
    ) {
        val matches = (answer as? CardAnswer.Comparison)
            ?.matches
            ?.associateBy { it.progressId }
            ?: emptyMap()
        val entries: List<CardHistory> = card.items.map { item ->
            val match = matches[item.progressId]
            val itemQuality = if (match != null && match.selectedWordId == item.wordId) {
                MAX_QUALITY
            } else {
                MIN_QUALITY
            }
            buildEntry(
                lessonId = card.lessonId,
                cardType = card.type,
                wordId = item.wordId,
                quality = itemQuality,
                timestamp = timestamp
            )
        }

        if (entries.isNotEmpty()) {
            cardHistoryRepository.insertAll(entries)
        }
    }

    private fun updateProgressForQuality(
        progress: LessonProgress,
        quality: Int,
        currentTimeMillis: Long
    ): LessonProgress {
        return when {
            quality == 0 -> handleIncorrect(progress, currentTimeMillis)
            progress.status == StatusEnum.NEW || progress.status == StatusEnum.PROGRESS_RESET ->
                handleFirstSuccess(progress, currentTimeMillis)
            else -> handleRepeatedSuccess(progress, quality, currentTimeMillis)
        }
    }

    private fun handleIncorrect(
        progress: LessonProgress,
        currentTimeMillis: Long
    ): LessonProgress {
        val isRepeatError = progress.status == StatusEnum.PROGRESS_RESET
        val updatedEf = if (isRepeatError) {
            progress.ef
        } else {
            recalculateEf(progress.ef, 0)
        }
        val nextReviewDate = currentTimeMillis + toMillis(RESET_DELAY_MINUTES)
        return progress.copy(
            status = StatusEnum.PROGRESS_RESET,
            ef = updatedEf,
            intervalMinutes = MINUTES_IN_DAY,
            nextReviewDate = nextReviewDate
        )
    }

    private fun handleFirstSuccess(
        progress: LessonProgress,
        currentTimeMillis: Long
    ): LessonProgress {
        val nextReviewDate = currentTimeMillis + toMillis(MINUTES_IN_DAY)
        return progress.copy(
            status = StatusEnum.IN_PROGRESS,
            nextReviewDate = nextReviewDate,
            intervalMinutes = MINUTES_IN_DAY
        )
    }

    private fun handleRepeatedSuccess(
        progress: LessonProgress,
        quality: Int,
        currentTimeMillis: Long
    ): LessonProgress {
        val updatedEf = recalculateEf(progress.ef, quality)
        val intervalMinutes = maxOf(
            MIN_INTERVAL_MINUTES,
            (progress.intervalMinutes * updatedEf).roundToLong()
        )
        val nextReviewDate = currentTimeMillis + toMillis(intervalMinutes)
        return progress.copy(
            status = StatusEnum.IN_PROGRESS,
            ef = updatedEf,
            intervalMinutes = intervalMinutes,
            nextReviewDate = nextReviewDate
        )
    }

    private fun recalculateEf(currentEf: Double, quality: Int): Double {
        val diff = 5 - quality
        val updated = currentEf + (0.1 - diff * (0.08 + diff * 0.02))
        return maxOf(updated, MIN_EF)
    }

    private fun toMillis(minutes: Long): Long =
        java.util.concurrent.TimeUnit.MINUTES.toMillis(minutes)

    companion object {
        private const val MIN_ITEMS_PER_CARD = 2
        private const val MAX_ITEMS_PER_CARD = 5

        private const val MIN_EF = 1.3
        private const val MIN_QUALITY = 0
        private const val MAX_QUALITY = 5
        private const val MINUTES_IN_DAY = 1440L
        private const val DEFAULT_EF = 2.5
        private const val MIN_INTERVAL_MINUTES = 1L
        private const val RESET_DELAY_MINUTES = 10L
    }
}

