package com.example.tala.entity.dictionary

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "dictionary_collections")
data class DictionaryCollection(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val name: String,
    val description: String? = null,
    @ColumnInfo(name = "dictionary_ids")
    val dictionaryIds: List<Int> = emptyList()
)

