package com.example.tala.entity.dictionaryCollection

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "dictionary_collection_entries",
    primaryKeys = ["collection_id", "dictionary_id"],
    foreignKeys = [
        ForeignKey(
            entity = DictionaryCollection::class,
            parentColumns = ["id"],
            childColumns = ["collection_id"],
            onDelete = ForeignKey.CASCADE,
            onUpdate = ForeignKey.NO_ACTION
        ),
        ForeignKey(
            entity = com.example.tala.entity.dictionary.Dictionary::class,
            parentColumns = ["id"],
            childColumns = ["dictionary_id"],
            onDelete = ForeignKey.CASCADE,
            onUpdate = ForeignKey.NO_ACTION
        )
    ],
    indices = [
        Index(value = ["collection_id"]),
        Index(value = ["dictionary_id"])
    ]
)
data class DictionaryCollectionEntry(
    @ColumnInfo(name = "collection_id")
    val collectionId: Int,
    @ColumnInfo(name = "dictionary_id")
    val dictionaryId: Int
)

