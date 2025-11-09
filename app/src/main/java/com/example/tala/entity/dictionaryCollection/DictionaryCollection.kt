package com.example.tala.entity.dictionaryCollection

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "dictionary_collections",
    indices = [
        Index(value = ["name"], unique = true, name = "index_dictionary_collections_name")
    ]
)
data class DictionaryCollection(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val name: String,
    val description: String? = null
)

