package com.example.tala.model.dto.lessonCard

import com.example.tala.entity.dictionary.Dictionary
import com.example.tala.entity.lessonprogress.LessonProgress
import com.example.tala.model.enums.CardTypeEnum
import com.example.tala.model.enums.StatusEnum
import org.junit.Assert.assertEquals
import org.junit.Test

class TranslationComparisonLessonCardDtoTest {

    @Test
    fun fromProgress_mapsItems() {
        val progressOne = LessonProgress(
            id = 1,
            lessonId = 42,
            cardType = CardTypeEnum.TRANSLATION_COMPARISON,
            dictionaryId = 10,
            nextReviewDate = 111L,
            intervalMinutes = 20,
            ef = 2.4,
            status = StatusEnum.NEW,
            info = "info1"
        )
        val progressTwo = progressOne.copy(
            id = 2,
            dictionaryId = 11,
            nextReviewDate = 222L,
            status = StatusEnum.IN_PROGRESS,
            info = "info2"
        )

        val dictionaries = mapOf(
            10 to Dictionary(
                id = 10,
                word = "apple",
                translation = "яблоко",
                partOfSpeech = com.example.tala.entity.dictionary.PartOfSpeech.NOUN,
                hint = "фрукт",
                imagePath = "apple.jpg"
            ),
            11 to Dictionary(
                id = 11,
                word = "train",
                translation = "поезд",
                partOfSpeech = com.example.tala.entity.dictionary.PartOfSpeech.NOUN,
                hint = "транспорт",
                imagePath = "train.jpg"
            )
        )

        val dto = TranslationComparisonLessonCardDto.fromProgress(
            lessonId = 42,
            progresses = listOf(progressOne, progressTwo),
            dictionaries = dictionaries
        )

        assertEquals(42, dto.lessonId)
        assertEquals(CardTypeEnum.TRANSLATION_COMPARISON, dto.type)
        assertEquals(2, dto.items.size)

        val firstItem = dto.items[0]
        assertEquals(1, firstItem.progressId)
        assertEquals("apple", firstItem.word)
        assertEquals("яблоко", firstItem.translation)
        assertEquals("фрукт", firstItem.hint)
        assertEquals("apple.jpg", firstItem.imagePath)
        assertEquals(StatusEnum.NEW, firstItem.status)
        assertEquals(20, firstItem.intervalMinutes)
        assertEquals(2.4, firstItem.ef, 0.0001)
        assertEquals(111L, firstItem.nextReviewDate)
        assertEquals("info1", firstItem.info)
    }
}

