package com.example.tala.model.enums

enum class CardTypeEnum(val use: Boolean, val defaultEf: Double) {
    TRANSLATE(true, 3.5),
    REVERSE_TRANSLATE(true, 3.0),
    ENTER_WORD(true, 2.5),
    SENTENCE_TO_STUDIED_LANGUAGE(false, 2.5),
    SENTENCE_TO_STUDENT_LANGUAGE(false, 2.5),
}