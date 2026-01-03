package com.example.tala.entity.cardhistory

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.tala.entity.word.Word
import com.example.tala.entity.lesson.Lesson
import com.example.tala.model.enums.CardTypeEnum

@Entity(
    tableName = "card_history",
    foreignKeys = [
        ForeignKey(
            entity = Lesson::class,
            parentColumns = ["id"],
            childColumns = ["lesson_id"],
            onDelete = ForeignKey.CASCADE,
            onUpdate = ForeignKey.NO_ACTION
        ),
        ForeignKey(
            entity = Word::class,
            parentColumns = ["id"],
            childColumns = ["word_id"],
            onDelete = ForeignKey.SET_NULL,
            onUpdate = ForeignKey.NO_ACTION
        )
    ],
    indices = [
        Index(value = ["lesson_id"]),
        Index(value = ["word_id"]),
        Index(value = ["date"])
    ]
)
data class CardHistory(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    @ColumnInfo(name = "lesson_id")
    val lessonId: Int,
    @ColumnInfo(name = "card_type")
    val cardType: CardTypeEnum,
    @ColumnInfo(name = "word_id")
    val wordId: Int?,
    @ColumnInfo(name = "quality")
    val quality: Int,
    @ColumnInfo(name = "date")
    val date: Long
)

