package com.example.tala.entity.word

import androidx.room.TypeConverter

object WordTypeConverters {

    @TypeConverter
    @JvmStatic
    fun fromLevel(level: WordLevel?): String? = level?.name

    @TypeConverter
    @JvmStatic
    fun toLevel(value: String?): WordLevel? = value?.let(WordLevel::valueOf)

    @TypeConverter
    @JvmStatic
    fun fromPartOfSpeech(partOfSpeech: PartOfSpeech?): String? = partOfSpeech?.value

    @TypeConverter
    @JvmStatic
    fun toPartOfSpeech(value: String?): PartOfSpeech? = value?.let { raw ->
        PartOfSpeech.values().firstOrNull { it.value.equals(raw, ignoreCase = true) }
    }

    @TypeConverter
    @JvmStatic
    fun fromTags(tags: Set<TagType>?): String = tags
        ?.takeIf { it.isNotEmpty() }
        ?.map { it.value }
        ?.sorted()
        ?.joinToString(",")
        ?: ""

    @TypeConverter
    @JvmStatic
    fun toTags(value: String?): Set<TagType> = value
        ?.takeIf { it.isNotBlank() }
        ?.split(',')
        ?.mapNotNull { tag ->
            val normalized = tag.trim().lowercase()
            TagType.values().firstOrNull { it.value.equals(normalized, ignoreCase = true) }
        }
        ?.toSet()
        ?: emptySet()

}

