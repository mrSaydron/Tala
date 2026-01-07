package com.example.tala.model.dto.lessonCard

import android.os.Parcelable
import com.example.tala.entity.word.Word
import com.example.tala.entity.lessonprogress.LessonProgress
import com.example.tala.model.enums.CardTypeEnum
import com.example.tala.model.enums.StatusEnum
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize

@Parcelize
data class TranslationComparisonLessonCardDto(
    val lessonId: Int,
    val items: List<Item>
) : LessonCardDto, Parcelable {

    @IgnoredOnParcel
    override val type: CardTypeEnum = CardTypeEnum.TRANSLATION_COMPARISON

    @Parcelize
    data class Item(
        val progressId: Int,
        val wordId: Int?,
        val word: String,
        val translation: String,
        val hint: String?,
        val imagePath: String?,
        val status: StatusEnum,
        val intervalMinutes: Long,
        val ef: Double,
        val nextReviewDate: Long?,
        val info: String?
    ) : Parcelable

    companion object {
        fun fromProgress(
            lessonId: Int,
            progresses: List<LessonProgress>,
            words: Map<Int, Word?>
        ): TranslationComparisonLessonCardDto {
            val items = progresses.map { progress ->
                val word = progress.wordId?.let { words[it] }
                Item(
                    progressId = progress.id,
                    wordId = progress.wordId,
                    word = word?.word.orEmpty(),
                    translation = word?.translation.orEmpty(),
                    hint = word?.hint,
                    imagePath = word?.imagePath,
                    status = progress.status,
                    intervalMinutes = progress.intervalMinutes,
                    ef = progress.ef,
                    nextReviewDate = progress.nextReviewDate,
                    info = progress.info
                )
            }
            return TranslationComparisonLessonCardDto(
                lessonId = lessonId,
                items = items
            )
        }
    }
}

