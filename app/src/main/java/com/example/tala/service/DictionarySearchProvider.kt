package com.example.tala.service

import com.example.tala.entity.dictionary.Dictionary

/**
 * Контракт для источников словарных данных. Реализации могут использовать разные API или локальные источники.
 */
interface DictionarySearchProvider {

    /**
     * Простейшее определение языка запроса. Реализации могут использовать эвристику или сторонние сервисы.
     */
    fun detectLanguage(term: String): DictionarySearchLanguage

    /**
     * Возвращает варианты словаря для запроса на русском языке.
     */
    suspend fun searchByRussian(term: String): List<List<Dictionary>>

    /**
     * Возвращает варианты словаря для запроса на английском языке.
     */
    suspend fun searchByEnglish(term: String): List<List<Dictionary>>

    /**
     * Универсальный поиск: определяет язык и вызывает соответствующий метод.
     */
    suspend fun search(term: String): List<List<Dictionary>> {
        return when (detectLanguage(term)) {
            DictionarySearchLanguage.RUSSIAN -> searchByRussian(term)
            DictionarySearchLanguage.ENGLISH -> searchByEnglish(term)
            DictionarySearchLanguage.UNKNOWN -> searchByEnglish(term)
        }
    }
}

enum class DictionarySearchLanguage {
    RUSSIAN,
    ENGLISH,
    UNKNOWN
}

