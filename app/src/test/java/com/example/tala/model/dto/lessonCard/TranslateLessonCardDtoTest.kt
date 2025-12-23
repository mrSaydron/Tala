package com.example.tala.model.dto.lessonCard

import com.example.tala.entity.dictionary.Dictionary
import com.example.tala.entity.lessonprogress.LessonProgress
import com.example.tala.model.enums.CardTypeEnum
import com.example.tala.model.enums.StatusEnum
import org.junit.Assert.assertEquals
import org.junit.Test

class TranslateLessonCardDtoTest {

    @Test
    fun fromProgress_mapsAllFields() {
        val progress = LessonProgress(
            id = 10,
            lessonId = 42,
            cardType = CardTypeEnum.TRANSLATE,
            dictionaryId = 7,
            nextReviewDate = 123456789L,
            intervalMinutes = 25,
            ef = 2.85,
            status = StatusEnum.IN_PROGRESS,
            info = "custom info"
        )
        val dictionary = Dictionary(
            id = 7,
            word = "example",
            translation = "пример",
            partOfSpeech = com.example.tala.entity.dictionary.PartOfSpeech.NOUN,
            hint = "подсказка",
            imagePath = "https://example.com/image.jpg",
            baseWordId = null
        )

        val dto = TranslateLessonCardDto.fromProgress(progress, dictionary)

        assertEquals(10, dto.progressId)
        assertEquals(42, dto.lessonId)
        assertEquals(7, dto.dictionaryId)
        assertEquals("example", dto.word)
        assertEquals("пример", dto.translation)
        assertEquals("подсказка", dto.hint)
        assertEquals("https://example.com/image.jpg", dto.imagePath)
        assertEquals(StatusEnum.IN_PROGRESS, dto.status)
        assertEquals(25, dto.intervalMinutes)
        assertEquals(2.85, dto.ef, 0.0001)
        assertEquals(123456789L, dto.nextReviewDate)
        assertEquals("custom info", dto.info)
        assertEquals(CardTypeEnum.TRANSLATE, dto.type)
        assertEquals("example", dto.cardInfo.english)
        assertEquals("пример", dto.cardInfo.russian)
        assertEquals("подсказка", dto.cardInfo.hint)
        assertEquals("https://example.com/image.jpg", dto.cardInfo.imagePath)
    }
}

