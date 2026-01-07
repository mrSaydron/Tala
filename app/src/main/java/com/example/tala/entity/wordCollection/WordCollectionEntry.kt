package com.example.tala.entity.wordCollection

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import com.example.tala.entity.word.Word

@Entity(
    tableName = "word_collection_entries",
    primaryKeys = ["collection_id", "word_id"],
    foreignKeys = [
        ForeignKey(
            entity = WordCollection::class,
            parentColumns = ["id"],
            childColumns = ["collection_id"],
            onDelete = ForeignKey.CASCADE,
            onUpdate = ForeignKey.NO_ACTION
        ),
        ForeignKey(
            entity = Word::class,
            parentColumns = ["id"],
            childColumns = ["word_id"],
            onDelete = ForeignKey.CASCADE,
            onUpdate = ForeignKey.NO_ACTION
        )
    ],
    indices = [
        Index(value = ["collection_id"]),
        Index(value = ["word_id"])
    ]
)
data class WordCollectionEntry(
    @ColumnInfo(name = "collection_id")
    val collectionId: Int,
    @ColumnInfo(name = "word_id")
    val wordId: Int
)

