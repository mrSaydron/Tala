package com.example.tala.service.lessonCard

import com.example.tala.entity.cardhistory.CardHistory
import com.example.tala.entity.cardhistory.CardHistoryRepository
import com.example.tala.entity.dictionary.DictionaryRepository
import com.example.tala.entity.lesson.LessonRepository
import com.example.tala.entity.lessoncardtype.LessonCardTypeRepository
import com.example.tala.entity.lessonprogress.LessonProgressRepository
import com.example.tala.entity.dictionaryCollection.DictionaryCollectionRepository
import com.example.tala.model.dto.lessonCard.LessonCardDto
import com.example.tala.model.dto.lessonCard.EnterWordLessonCardDto
import com.example.tala.model.dto.lessonCard.ReverseTranslateLessonCardDto
import com.example.tala.model.dto.lessonCard.TranslateLessonCardDto
import com.example.tala.model.dto.lessonCard.TranslationComparisonLessonCardDto
import com.example.tala.model.enums.CardTypeEnum
import com.example.tala.service.lessonCard.model.CardAnswer

class LessonCardService(
    private val lessonRepository: LessonRepository,
    private val lessonCardTypeRepository: LessonCardTypeRepository,
    private val dictionaryCollectionRepository: DictionaryCollectionRepository,
    private val dictionaryRepository: DictionaryRepository,
    private val lessonProgressRepository: LessonProgressRepository,
    private val typeServices: Map<CardTypeEnum, LessonCardTypeService>,
    private val timeProvider: () -> Long = System::currentTimeMillis
) {

    suspend fun createProgress(lessonId: Int) {
        val lesson = lessonRepository.getById(lessonId) ?: return
        val lessonCardTypes = lessonCardTypeRepository.getByCollectionId(lesson.collectionId)
        if (lessonCardTypes.isEmpty()) return

        val dictionaryIds = dictionaryCollectionRepository.getDictionaryIds(lesson.collectionId)
        if (dictionaryIds.isEmpty()) return

        val dictionaries = dictionaryRepository.getByIds(dictionaryIds)
        if (dictionaries.isEmpty()) return

        lessonCardTypes.forEach { lessonCardType ->
            val service = typeServices[lessonCardType.cardType] ?: return@forEach
            service.createProgress(lessonId, dictionaries)
        }
    }

    suspend fun getCards(lessonId: Int): List<LessonCardDto> {
        val lesson = lessonRepository.getById(lessonId) ?: return emptyList()
        val lessonCardTypes = lessonCardTypeRepository.getByCollectionId(lesson.collectionId)
        if (lessonCardTypes.isEmpty()) return emptyList()

        return lessonCardTypes.flatMap { lessonCardType ->
            val service = typeServices[lessonCardType.cardType] ?: return@flatMap emptyList()
            val progress = lessonProgressRepository.getByLessonCardType(lessonId, lessonCardType.cardType)
            service.getCards(progress)
        }
    }

    suspend fun answerResult(card: LessonCardDto, answer: CardAnswer?, quality: Int): LessonCardDto? {
        val service = typeServices[card.type] ?: return null
        val now = timeProvider()
        val result = service.answerResult(card, answer, quality, now)
        return result
    }
}