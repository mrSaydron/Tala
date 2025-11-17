package com.example.tala.entity.dictionary

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.tala.entity.dictionary.PartOfSpeech as DictionaryPartOfSpeech
import com.example.tala.entity.dictionary.TagType as DictionaryTagType


@Entity(
    tableName = "dictionary",
    indices = [
        Index(value = ["word"], name = "index_dictionary_word"),
        Index(value = ["base_word_id"], name = "index_dictionary_base_word_id")
    ],
    foreignKeys = [
        ForeignKey(
            entity = Dictionary::class,
            parentColumns = ["id"],
            childColumns = ["base_word_id"],
            onDelete = ForeignKey.SET_NULL,
            onUpdate = ForeignKey.NO_ACTION
        )
    ]
)
data class Dictionary(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val word: String,
    val translation: String,
    @ColumnInfo(name = "part_of_speech")
    val partOfSpeech: DictionaryPartOfSpeech,
    val ipa: String? = null,
    val hint: String? = null,
    @ColumnInfo(name = "image_path")
    val imagePath: String? = null,
    @ColumnInfo(name = "base_word_id")
    val baseWordId: Int? = null,
    val frequency: Double? = null,
    val level: DictionaryLevel? = null,
    val tags: Set<DictionaryTagType> = emptySet(),
)
