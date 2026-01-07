package com.example.tala.fragment.adapter

import com.example.tala.entity.word.Word
import com.example.tala.entity.word.WordLevel
import com.example.tala.entity.word.PartOfSpeech
import com.example.tala.entity.word.TagType
import java.util.UUID

data class WordEditItem(
    val key: String = UUID.randomUUID().toString(),
    var id: Int = 0,
    var baseWordId: Int? = null,
    var word: String = "",
    var translation: String = "",
    var partOfSpeech: PartOfSpeech = PartOfSpeech.NOUN,
    var ipa: String = "",
    var hint: String = "",
    var imagePath: String = "",
    var frequencyText: String = "",
    var level: WordLevel? = null,
    val tags: MutableSet<TagType> = mutableSetOf(),
) {
    fun toWordOrNull(): Word? {
        val cleanedWord = word.trim()
        val cleanedTranslation = translation.trim()
        if (cleanedWord.isEmpty() || cleanedTranslation.isEmpty()) {
            return null
        }

        return Word(
            id = id,
            word = cleanedWord,
            translation = cleanedTranslation,
            partOfSpeech = partOfSpeech,
            ipa = ipa.trim().takeIf { it.isNotEmpty() },
            hint = hint.trim().takeIf { it.isNotEmpty() },
            imagePath = imagePath.trim().takeIf { it.isNotEmpty() },
            baseWordId = baseWordId,
            frequency = frequencyText.trim()
                .takeIf { it.isNotEmpty() }
                ?.replace(',', '.')
                ?.toDoubleOrNull(),
            level = level,
            tags = tags.toSet(),
        )
    }

    companion object {
        fun fromWord(word: Word): WordEditItem {
            val key = if (word.id != 0) {
                "existing_${word.id}"
            } else {
                UUID.randomUUID().toString()
            }
            return WordEditItem(
                key = key,
                id = word.id,
                baseWordId = word.baseWordId,
                word = word.word,
                translation = word.translation,
                partOfSpeech = word.partOfSpeech,
                ipa = word.ipa.orEmpty(),
                hint = word.hint.orEmpty(),
                imagePath = word.imagePath.orEmpty(),
                frequencyText = word.frequency?.toString().orEmpty(),
                level = word.level,
                tags = word.tags.toMutableSet(),
            )
        }
    }
}

