package com.example.tala.entity.lessoncardtype

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import com.example.tala.entity.lesson.Lesson
import com.example.tala.model.enums.CardTypeEnum

@Entity(
    tableName = "lesson_card_types",
    primaryKeys = ["lesson_id", "card_type"],
    foreignKeys = [
        ForeignKey(
            entity = Lesson::class,
            parentColumns = ["id"],
            childColumns = ["lesson_id"],
            onDelete = ForeignKey.CASCADE,
            onUpdate = ForeignKey.NO_ACTION
        )
    ],
    indices = [
        Index(value = ["lesson_id"]),
        Index(value = ["card_type"])
    ]
)
data class LessonCardType(
    @ColumnInfo(name = "lesson_id")
    val lessonId: Int,
    @ColumnInfo(name = "card_type")
    val cardType: CardTypeEnum
)


