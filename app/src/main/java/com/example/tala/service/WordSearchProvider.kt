package com.example.tala.service

import com.example.tala.entity.word.Word

/**
 * Контракт для источников словарных данных. Реализации могут использовать разные API или локальные источники.
 */
interface WordSearchProvider {

    /**
     * Простейшее определение языка запроса. Реализации могут использовать эвристику или сторонние сервисы.
     */
    fun detectLanguage(term: String): WordSearchLanguage

    /**
     * Возвращает варианты словаря для запроса на русском языке.
     */
    suspend fun searchByRussian(term: String): List<List<Word>>

    /**
     * Возвращает варианты словаря для запроса на английском языке.
     */
    suspend fun searchByEnglish(term: String): List<List<Word>>

    /**
     * Универсальный поиск: определяет язык и вызывает соответствующий метод.
     */
    suspend fun search(term: String): List<List<Word>> {
        return when (detectLanguage(term)) {
            WordSearchLanguage.RUSSIAN -> searchByRussian(term)
            WordSearchLanguage.ENGLISH -> searchByEnglish(term)
            WordSearchLanguage.UNKNOWN -> searchByEnglish(term)
        }
    }
}

enum class WordSearchLanguage {
    RUSSIAN,
    ENGLISH,
    UNKNOWN
}

