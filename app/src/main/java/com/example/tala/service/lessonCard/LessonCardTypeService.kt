package com.example.tala.service.lessonCard

import com.example.tala.entity.dictionary.Dictionary
import com.example.tala.entity.lessonprogress.LessonProgress
import com.example.tala.model.dto.lessonCard.LessonCardDto
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
}