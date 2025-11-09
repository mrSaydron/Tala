package com.example.tala.entity.dictionary

import androidx.room.TypeConverter

object DictionaryTypeConverters {

    @TypeConverter
    @JvmStatic
    fun fromLevel(level: DictionaryLevel?): String? = level?.name

    @TypeConverter
    @JvmStatic
    fun toLevel(value: String?): DictionaryLevel? = value?.let(DictionaryLevel::valueOf)

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

