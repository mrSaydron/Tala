package com.example.tala.entity.wordCollection

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "word_collection",
    indices = [
        Index(value = ["name"], unique = true, name = "index_word_collection_name")
    ]
)
data class WordCollection(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val name: String,
    val description: String? = null
)

