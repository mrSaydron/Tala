package com.example.tala.entity.word

import com.example.tala.R

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
    SUPERLATIVE("superlative");

    fun localizedNameRes(): Int = when (this) {
        IS_IDIOM -> R.string.word_tag_is_idiom
        IS_PHRASAL -> R.string.word_tag_is_phrasal
        IS_COLLOCATION -> R.string.word_tag_is_collocation
        CASE_SENSITIVE -> R.string.word_tag_case_sensitive
        IS_FIXED_EXPRESSION -> R.string.word_tag_is_fixed_expression
        PLURAL -> R.string.word_tag_plural
        PAST_SIMPLE -> R.string.word_tag_past_simple
        PAST_PARTICIPLE -> R.string.word_tag_past_participle
        COMPARATIVE -> R.string.word_tag_comparative
        SUPERLATIVE -> R.string.word_tag_superlative
    }
}

