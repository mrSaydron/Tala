package com.example.tala.model.enums

enum class CardTypeEnum(val use: Boolean, val defaultEf: Double, val titleRu: String) {
    TRANSLATE(true, 3.5, "Перевод"),
    REVERSE_TRANSLATE(true, 3.0, "Обратный перевод"),
    ENTER_WORD(true, 2.5, "Ввод слова"),
    TRANSLATION_COMPARISON(true, 2.8, "Сопоставление перевода"),
    SENTENCE_TO_STUDIED_LANGUAGE(false, 2.5, "Предложение → изучаемый язык"),
    SENTENCE_TO_STUDENT_LANGUAGE(false, 2.5, "Предложение → родной язык"),
}