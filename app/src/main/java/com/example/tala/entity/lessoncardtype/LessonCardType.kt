package com.example.tala.entity.lessoncardtype

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import com.example.tala.entity.dictionaryCollection.DictionaryCollection
import com.example.tala.model.enums.CardTypeEnum

@Entity(
    tableName = "lesson_card_types",
    primaryKeys = ["collection_id", "card_type"],
    foreignKeys = [
        ForeignKey(
            entity = DictionaryCollection::class,
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
    val cardType: CardTypeEnum
)


