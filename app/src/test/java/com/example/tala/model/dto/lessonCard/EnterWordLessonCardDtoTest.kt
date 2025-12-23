package com.example.tala.model.dto.lessonCard

import com.example.tala.entity.dictionary.Dictionary
import com.example.tala.entity.lessonprogress.LessonProgress
import com.example.tala.model.enums.CardTypeEnum
import com.example.tala.model.enums.StatusEnum
import org.junit.Assert.assertEquals
import org.junit.Test

class EnterWordLessonCardDtoTest {

    @Test
    fun fromProgress_mapsAllFields() {
        val progress = LessonProgress(
            id = 33,
            lessonId = 12,
            cardType = CardTypeEnum.ENTER_WORD,
            dictionaryId = 5,
            nextReviewDate = 222222L,
            intervalMinutes = 55,
            ef = 2.1,
            status = StatusEnum.NEW,
            info = """{"hint":"подсказка"}"""
        )
        val dictionary = Dictionary(
            id = 5,
            word = "example",
            translation = "пример",
            partOfSpeech = com.example.tala.entity.dictionary.PartOfSpeech.NOUN,
            hint = "слово-подсказка",
            imagePath = "https://example.com/image.jpg",
            baseWordId = null
        )

        val dto = EnterWordLessonCardDto.fromProgress(progress, dictionary)

        assertEquals(33, dto.progressId)
        assertEquals(12, dto.lessonId)
        assertEquals(5, dto.dictionaryId)
        assertEquals("example", dto.word)
        assertEquals("пример", dto.translation)
        assertEquals("слово-подсказка", dto.hint)
        assertEquals("https://example.com/image.jpg", dto.imagePath)
        assertEquals(StatusEnum.NEW, dto.status)
        assertEquals(55, dto.intervalMinutes)
        assertEquals(2.1, dto.ef, 0.0001)
        assertEquals(222222L, dto.nextReviewDate)
        assertEquals("""{"hint":"подсказка"}""", dto.info)
        assertEquals(CardTypeEnum.ENTER_WORD, dto.type)
        assertEquals("example", dto.cardInfo.english)
        assertEquals("пример", dto.cardInfo.russian)
        assertEquals("слово-подсказка", dto.cardInfo.hint)
        assertEquals("https://example.com/image.jpg", dto.cardInfo.imagePath)
    }
}


