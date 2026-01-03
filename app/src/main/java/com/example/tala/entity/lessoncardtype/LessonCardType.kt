package com.example.tala.entity.lessoncardtype

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import com.example.tala.entity.wordCollection.WordCollection
import com.example.tala.model.enums.CardTypeEnum

@Entity(
    tableName = "lesson_card_types",
    primaryKeys = ["collection_id", "card_type"],
    foreignKeys = [
        ForeignKey(
            entity = WordCollection::class,
            parentColumns = ["id"],
            childColumns = ["collection_id"],
            onDelete = ForeignKey.CASCADE,
            onUpdate = ForeignKey.NO_ACTION
        )
    ],
    indices = [
        Index(value = ["collection_id"]),
        Index(value = ["card_type"])
    ]
)
data class LessonCardType(
    @ColumnInfo(name = "collection_id")
    val collectionId: Int,
    @ColumnInfo(name = "card_type")
    val cardType: CardTypeEnum,
    @ColumnInfo(name = "condition_on_card_type")
    val conditionOnCardType: CardTypeEnum? = null,
    @ColumnInfo(name = "condition_on_value")
    val conditionOnValue: Int? = null,
    @ColumnInfo(name = "condition_off_card_type")
    val conditionOffCardType: CardTypeEnum? = null,
    @ColumnInfo(name = "condition_off_value")
    val conditionOffValue: Int? = null
)


