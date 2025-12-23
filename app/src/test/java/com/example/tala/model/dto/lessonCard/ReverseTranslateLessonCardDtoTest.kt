package com.example.tala.model.dto.lessonCard

import com.example.tala.entity.dictionary.Dictionary
import com.example.tala.entity.lessonprogress.LessonProgress
import com.example.tala.model.enums.CardTypeEnum
import com.example.tala.model.enums.StatusEnum
import org.junit.Assert.assertEquals
import org.junit.Test

class ReverseTranslateLessonCardDtoTest {

    @Test
    fun fromProgress_mapsAllFields() {
        val progress = LessonProgress(
            id = 21,
            lessonId = 52,
            cardType = CardTypeEnum.REVERSE_TRANSLATE,
            dictionaryId = 9,
            nextReviewDate = 987654321L,
            intervalMinutes = 35,
            ef = 2.45,
            status = StatusEnum.PROGRESS_RESET,
            info = """{"hint":"подсказка"}"""
        )
        val dictionary = Dictionary(
            id = 9,
            word = "example",
            translation = "пример",
            partOfSpeech = com.example.tala.entity.dictionary.PartOfSpeech.NOUN,
            hint = "слово-подсказка",
            imagePath = "https://example.com/image.jpg",
            baseWordId = null
        )

        val dto = ReverseTranslateLessonCardDto.fromProgress(progress, dictionary)

        assertEquals(21, dto.progressId)
        assertEquals(52, dto.lessonId)
        assertEquals(9, dto.dictionaryId)
        assertEquals("example", dto.word)
        assertEquals("пример", dto.translation)
        assertEquals("слово-подсказка", dto.hint)
        assertEquals("https://example.com/image.jpg", dto.imagePath)
        assertEquals(StatusEnum.PROGRESS_RESET, dto.status)
        assertEquals(35, dto.intervalMinutes)
        assertEquals(2.45, dto.ef, 0.0001)
        assertEquals(987654321L, dto.nextReviewDate)
        assertEquals("""{"hint":"подсказка"}""", dto.info)
        assertEquals(CardTypeEnum.REVERSE_TRANSLATE, dto.type)
        assertEquals("example", dto.cardInfo.english)
        assertEquals("пример", dto.cardInfo.russian)
        assertEquals("слово-подсказка", dto.cardInfo.hint)
        assertEquals("https://example.com/image.jpg", dto.cardInfo.imagePath)
    }
}

