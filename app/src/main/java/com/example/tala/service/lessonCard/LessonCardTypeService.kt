package com.example.tala.service.lessonCard

import com.example.tala.entity.dictionary.Dictionary
import com.example.tala.entity.lessonprogress.LessonProgress
import com.example.tala.model.dto.lessonCard.LessonCardDto

interface LessonCardTypeService {
    suspend fun createProgress(lessonId: Int, words: List<Dictionary>)
    suspend fun getCards(cardProgress: List<LessonProgress>): List<LessonCardDto>
    suspend fun answerResult(
        progress: LessonProgress,
        quality: Int,
        currentTimeMillis: Long
    ): LessonProgress
}