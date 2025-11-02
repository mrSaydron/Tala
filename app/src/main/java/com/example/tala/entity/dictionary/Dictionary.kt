package com.example.tala.entity.dictionary

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "dictionary")
data class Dictionary(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val word: String,
    val translation: String,
    @ColumnInfo(name = "part_of_speech")
    val partOfSpeech: PartOfSpeech,
    val ipa: String? = null,
    val hint: String? = null,
    @ColumnInfo(name = "base_word_id")
    val baseWordId: Int? = null,
    val frequency: Double? = null,
    val level: DictionaryLevel? = null,
    val tags: Set<TagType> = emptySet(),
)

enum class DictionaryLevel {
    A1,
    A2,
    B1,
    B2,
    C1,
    C2,
}

enum class PartOfSpeech(val value: String) {
    NOUN("noun"),
    VERB("verb"),
    ADJECTIVE("adjective"),
    ADVERB("adverb"),
    PRONOUN("pronoun"),
    PREPOSITION("preposition"),
    CONJUNCTION("conjunction"),
    INTERJECTION("interjection"),
    DETERMINER("determiner"),
    ARTICLE("article"),
    NUMERAL("numeral"),
    PARTICLE("particle"),
    AUXILIARY_VERB("auxiliary_verb"),
    MODAL_VERB("modal_verb"),
    PHRASAL_VERB("phrasal_verb"),
    GERUND("gerund"),
    PROPER_NOUN("proper_noun"),
    IDIOM("idiom"),
}

enum class TagType(val value: String) {
    IS_IDIOM("is_idiom"),
    IS_PHRASAL("is_phrasal"),
    IS_COLLOCATION("is_collocation"),
    CASE_SENSITIVE("case_sensitive"),
    IS_FIXED_EXPRESSION("is_fixed_expression"),
    PLURAL("plural"),
    PAST_SIMPLE("past_simple"),
    PAST_PARTICIPLE("past_participle"),
    COMPARATIVE("comparative"),
    SUPERLATIVE("superlative"),
}

