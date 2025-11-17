package com.example.tala.entity.lessonprogress

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.tala.entity.dictionary.Dictionary
import com.example.tala.entity.lesson.Lesson
import com.example.tala.model.enums.CardTypeEnum
import com.example.tala.model.enums.StatusEnum

@Entity(
    tableName = "lesson_progress",
    foreignKeys = [
        ForeignKey(
            entity = Lesson::class,
            parentColumns = ["id"],
            childColumns = ["lesson_id"],
            onDelete = ForeignKey.CASCADE,
            onUpdate = ForeignKey.NO_ACTION
        ),
        ForeignKey(
            entity = Dictionary::class,
            parentColumns = ["id"],
            childColumns = ["dictionary_id"],
            onDelete = ForeignKey.SET_NULL,
            onUpdate = ForeignKey.NO_ACTION
        )
    ],
    indices = [
        Index(value = ["lesson_id", "card_type"]),
        Index(value = ["dictionary_id"]),
        Index(value = ["next_review_date"])
    ]
)
data class LessonProgress(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    @ColumnInfo(name = "lesson_id")
    val lessonId: Int,
    @ColumnInfo(name = "card_type")
    val cardType: CardTypeEnum,
    @ColumnInfo(name = "dictionary_id")
    val dictionaryId: Int? = null,
    @ColumnInfo(name = "next_review_date")
    val nextReviewDate: Long?,
    @ColumnInfo(name = "interval_minutes")
    val intervalMinutes: Long,
    @ColumnInfo(name = "ef")
    val ef: Double,
    @ColumnInfo(name = "status")
    val status: StatusEnum,
    @ColumnInfo(name = "info")
    val info: String? = null
)

