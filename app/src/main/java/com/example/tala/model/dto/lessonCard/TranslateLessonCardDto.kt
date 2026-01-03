package com.example.tala.model.dto.lessonCard

import android.os.Parcelable
import com.example.tala.entity.word.Word
import com.example.tala.entity.lessonprogress.LessonProgress
import com.example.tala.model.dto.info.WordCardInfo
import com.example.tala.model.enums.CardTypeEnum
import com.example.tala.model.enums.StatusEnum
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize

@Parcelize
data class TranslateLessonCardDto(
    val progressId: Int,
    val lessonId: Int,
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
) : LessonCardDto, Parcelable {
    @IgnoredOnParcel
    override val type: CardTypeEnum = CardTypeEnum.TRANSLATE

    @IgnoredOnParcel
    val cardInfo: WordCardInfo =
        WordCardInfo.fromJson(info).let { raw ->
            WordCardInfo(
                english = raw.english ?: word,
                russian = raw.russian ?: translation,
                imagePath = raw.imagePath ?: imagePath,
                hint = raw.hint ?: hint
            )
        }

    companion object {
        fun fromProgress(
            progress: LessonProgress,
            dictionary: Word?
        ): TranslateLessonCardDto = TranslateLessonCardDto(
            progressId = progress.id,
            lessonId = progress.lessonId,
            wordId = progress.wordId,
            word = dictionary?.word ?: "",
            translation = dictionary?.translation ?: "",
            hint = dictionary?.hint,
            imagePath = dictionary?.imagePath,
            status = progress.status,
            intervalMinutes = progress.intervalMinutes,
            ef = progress.ef,
            nextReviewDate = progress.nextReviewDate,
            info = progress.info
        )
    }
}

