package com.example.tala.fragment.adapter

import com.example.tala.entity.word.Word
import com.example.tala.entity.word.DictionaryLevel
import com.example.tala.entity.word.PartOfSpeech
import com.example.tala.entity.word.TagType
import java.util.UUID

data class DictionaryEditItem(
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
    var level: DictionaryLevel? = null,
    val tags: MutableSet<TagType> = mutableSetOf(),
) {
    fun toDictionaryOrNull(): Word? {
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
        fun fromWord(dictionary: Word): DictionaryEditItem {
            val key = if (dictionary.id != 0) {
                "existing_${dictionary.id}"
            } else {
                UUID.randomUUID().toString()
            }
            return DictionaryEditItem(
                key = key,
                id = dictionary.id,
                baseWordId = dictionary.baseWordId,
                word = dictionary.word,
                translation = dictionary.translation,
                partOfSpeech = dictionary.partOfSpeech,
                ipa = dictionary.ipa.orEmpty(),
                hint = dictionary.hint.orEmpty(),
                imagePath = dictionary.imagePath.orEmpty(),
                frequencyText = dictionary.frequency?.toString().orEmpty(),
                level = dictionary.level,
                tags = dictionary.tags.toMutableSet(),
            )
        }
    }
}

