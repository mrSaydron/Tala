package com.example.tala.service.lessonCard

import com.example.tala.entity.dictionary.Dictionary
import com.example.tala.entity.dictionary.DictionaryRepository
import com.example.tala.entity.lessonprogress.LessonProgress
import com.example.tala.entity.lessonprogress.LessonProgressRepository
import com.example.tala.model.dto.lessonCard.LessonCardDto
import com.example.tala.model.dto.lessonCard.TranslateLessonCardDto
import com.example.tala.model.enums.CardTypeEnum
import com.example.tala.model.enums.StatusEnum
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class TranslateLessonCardTypeService(
    private val lessonProgressRepository: LessonProgressRepository,
    private val dictionaryRepository: DictionaryRepository
) : LessonCardTypeService {

    override suspend fun createProgress(lessonId: Int, words: List<Dictionary>) {
        withContext(Dispatchers.IO) {
            val progressEntries = words
                .map { dictionary ->
                    LessonProgress(
                        lessonId = lessonId,
                        cardType = CardTypeEnum.TRANSLATE,
                        dictionaryId = dictionary.id,
                        nextReviewDate = System.currentTimeMillis(),
                        intervalMinutes = 0,
                        ef = 2.5,
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
}

