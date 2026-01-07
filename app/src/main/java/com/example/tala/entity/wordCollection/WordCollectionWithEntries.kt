package com.example.tala.entity.wordCollection

import androidx.room.Embedded
import androidx.room.Relation

data class WordCollectionWithEntries(
    @Embedded
    val collection: WordCollection,
    @Relation(
        parentColumn = "id",
        entityColumn = "collection_id"
    )
    val entries: List<WordCollectionEntry>
)

