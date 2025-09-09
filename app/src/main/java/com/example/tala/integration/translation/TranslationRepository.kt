package com.example.tala.integration.translation

import com.example.tala.integration.dictionary.YandexDictionaryApi
import com.example.tala.service.ApiClient

class TranslationRepository(
    private val api: YandexDictionaryApi = ApiClient.yandexDictionaryApi,
    private val apiKey: String = YandexDictionaryApi.YANDEX_API_KEY,
) {
    suspend fun getTranslations(word: String, fromLang: String, toLang: String): List<String> = try {
        val lang = "$fromLang-$toLang"
        api.getTranslation(text = word, lang = lang, apiKey = apiKey)
            .def.flatMap { def -> def.tr.map { it.text } }
    } catch (_: Exception) {
        emptyList()
    }
}


