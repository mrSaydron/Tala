package com.example.tala.service.lessonCard

import com.example.tala.entity.dictionary.Dictionary
import com.example.tala.entity.dictionary.DictionaryRepository
import com.example.tala.entity.lessonprogress.LessonProgress
import com.example.tala.entity.lessonprogress.LessonProgressRepository
import com.example.tala.model.dto.lessonCard.LessonCardDto
import com.example.tala.model.dto.lessonCard.TranslateLessonCardDto
import com.example.tala.model.enums.CardTypeEnum
import com.example.tala.model.enums.StatusEnum
import com.example.tala.service.lessonCard.model.CardAnswer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.roundToLong

class TranslateLessonCardTypeService(
    private val lessonProgressRepository: LessonProgressRepository,
    private val dictionaryRepository: DictionaryRepository,
    private val timeProvider: () -> Long = System::currentTimeMillis
) : LessonCardTypeService {

    override suspend fun createProgress(lessonId: Int, words: List<Dictionary>) {
        withContext(Dispatchers.IO) {
            val existingDictionaryIds = lessonProgressRepository
                .getByLessonCardType(lessonId, CardTypeEnum.TRANSLATE)
                .mapNotNull { it.dictionaryId }
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
                    cardType = CardTypeEnum.TRANSLATE,
                    dictionaryId = dictionary.id,
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

        val dictionaryIds = cardProgress.mapNotNull { it.dictionaryId }.distinct()
        val dictionaries = dictionaryRepository.getByIds(dictionaryIds)
            .associateBy { it.id }

        return cardProgress.map { progress ->
            val dictionary = progress.dictionaryId?.let { dictionaries[it] }
            TranslateLessonCardDto.fromProgress(progress, dictionary)
        }
    }

    override suspend fun answerResult(
        card: LessonCardDto,
        answer: CardAnswer?,
        quality: Int,
        currentTimeMillis: Long
    ): LessonCardDto? {
        val dto = card as? TranslateLessonCardDto ?: return null
        val progress = lessonProgressRepository.getById(dto.progressId) ?: return null
        val clampedQuality = quality.coerceIn(MIN_QUALITY, MAX_QUALITY)
        val updatedProgress = when {
            clampedQuality == 0 -> handleIncorrect(progress, currentTimeMillis)
            progress.status == StatusEnum.NEW || progress.status == StatusEnum.PROGRESS_RESET ->
                handleFirstSuccess(progress, currentTimeMillis)
            else -> handleRepeatedSuccess(progress, clampedQuality, currentTimeMillis)
        }
        lessonProgressRepository.update(updatedProgress)
        return if (clampedQuality == 0) {
            buildCardFromProgress(updatedProgress)
        } else {
            null
        }
    }

    private suspend fun buildCardFromProgress(progress: LessonProgress): TranslateLessonCardDto {
        val dictionary = progress.dictionaryId?.let { dictionaryRepository.getById(it) }
        return TranslateLessonCardDto.fromProgress(progress, dictionary)
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
        private const val MIN_EF = 1.3
        private const val MIN_QUALITY = 0
        private const val MAX_QUALITY = 5
        private const val MINUTES_IN_DAY = 1440L
        private const val DEFAULT_EF = 2.5
        private const val MIN_INTERVAL_MINUTES = 1L
        private const val RESET_DELAY_MINUTES = 10L
    }
}

