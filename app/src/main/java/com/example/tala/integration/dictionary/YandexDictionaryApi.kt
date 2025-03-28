package com.example.tala.integration.dictionary

import com.example.tala.integration.dictionary.dto.YandexTranslationResponse
import retrofit2.http.GET
import retrofit2.http.Query

interface YandexDictionaryApi {
    @GET("lookup")
    suspend fun getTranslation(
        @Query("text") text: String,
        @Query("lang") lang: String,
        @Query("key") apiKey: String
    ): YandexTranslationResponse

    companion object {
        const val YANDEX_API_KEY = "dict.1.1.20250221T202214Z.b398e6dada85c843.a61f04a25b5d24842a2d005185eca8e229cd1515"
    }
}