package com.example.tala.service

import com.example.tala.integration.dictionary.YandexDictionaryApi
import com.example.tala.integration.mistral.MistralApi
import com.example.tala.integration.picture.UnsplashApi
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object ApiClient {
    private const val BASE_URL = "https://dictionary.yandex.net/api/v1/dicservice.json/"
    private const val UNSPLASH_BASE_URL = "https://api.unsplash.com/"
    private const val MISTRAL_BASE_URL = "https://api.mistral.ai/v1/chat/"
    private const val GEMINI_BASE_URL = "https://generativelanguage.googleapis.com/v1beta/"

    val yandexDictionaryApi: YandexDictionaryApi by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .client(OkHttpClient.Builder().addInterceptor(HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            }).build())
            .build()
            .create(YandexDictionaryApi::class.java)
    }

    val unsplashApi: UnsplashApi by lazy {
        Retrofit.Builder()
            .baseUrl(UNSPLASH_BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .client(OkHttpClient.Builder().addInterceptor(HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            }).build())
            .build()
            .create(UnsplashApi::class.java)
    }

    val mistralApi: MistralApi by lazy {
        Retrofit.Builder()
            .baseUrl(MISTRAL_BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .client(OkHttpClient.Builder().addInterceptor(HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            }).build())
            .build()
            .create(MistralApi::class.java)
    }

}