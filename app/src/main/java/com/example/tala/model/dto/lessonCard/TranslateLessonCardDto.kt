package com.example.tala.model.dto.lessonCard

import android.os.Parcelable
import com.example.tala.entity.dictionary.Dictionary
import com.example.tala.entity.lessonprogress.LessonProgress
import com.example.tala.model.enums.CardTypeEnum
import com.example.tala.model.enums.StatusEnum
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize

@Parcelize
data class TranslateLessonCardDto(
    val lessonId: Int,
    val dictionaryId: Int?,
    val word: String,
    val translation: String,
    val status: StatusEnum,
    val intervalMinutes: Long,
    val ef: Double,
    val info: String?
) : LessonCardDto, Parcelable {
    @IgnoredOnParcel
    val type: CardTypeEnum = CardTypeEnum.TRANSLATE

    companion object {
        fun fromProgress(
            progress: LessonProgress,
            dictionary: Dictionary?
        ): TranslateLessonCardDto = TranslateLessonCardDto(
            lessonId = progress.lessonId,
            dictionaryId = progress.dictionaryId,
            word = dictionary?.word ?: "",
            translation = dictionary?.translation ?: "",
            status = progress.status,
            intervalMinutes = progress.intervalMinutes,
            ef = progress.ef,
            info = progress.info
        )
    }
}

