package com.example.tala.model.enums

enum class CardTypeEnum(val use: Boolean) {
    TRANSLATE(true),
    REVERSE_TRANSLATE(true),
    ENTER_WORD(true),
    SENTENCE_TO_STUDIED_LANGUAGE(false),
    SENTENCE_TO_STUDENT_LANGUAGE(false),
}