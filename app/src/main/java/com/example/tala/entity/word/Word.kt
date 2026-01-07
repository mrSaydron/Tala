package com.example.tala.entity.word

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "words",
    indices = [
        Index(value = ["word"], name = "index_words_word"),
        Index(value = ["base_word_id"], name = "index_words_base_word_id")
    ],
    foreignKeys = [
        ForeignKey(
            entity = Word::class,
            parentColumns = ["id"],
            childColumns = ["base_word_id"],
            onDelete = ForeignKey.SET_NULL,
            onUpdate = ForeignKey.NO_ACTION
        )
    ]
)
data class Word(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val word: String,
    val translation: String,
    @ColumnInfo(name = "part_of_speech")
    val partOfSpeech: PartOfSpeech,
    val ipa: String? = null,
    val hint: String? = null,
    @ColumnInfo(name = "image_path")
    val imagePath: String? = null,
    @ColumnInfo(name = "base_word_id")
    val baseWordId: Int? = null,
    val frequency: Double? = null,
    val level: WordLevel? = null,
    val tags: Set<TagType> = emptySet(),
)
