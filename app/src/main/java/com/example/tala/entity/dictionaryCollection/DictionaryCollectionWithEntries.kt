package com.example.tala.entity.dictionaryCollection

import androidx.room.Embedded
import androidx.room.Relation

data class DictionaryCollectionWithEntries(
    @Embedded
    val collection: DictionaryCollection,
    @Relation(
        parentColumn = "id",
        entityColumn = "collection_id"
    )
    val entries: List<DictionaryCollectionEntry>
)

