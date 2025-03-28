package com.example.tala.integration.picture

import com.example.tala.integration.picture.dto.UnsplashResponse
import retrofit2.http.GET
import retrofit2.http.Query

interface UnsplashApi {
    @GET("search/photos")
    suspend fun searchImages(
        @Query("query") query: String,
        @Query("client_id") apiKey: String
    ): UnsplashResponse

    companion object {
        const val USPLASH_API_KEY = "9DLJZ0IDL17f86_UFtZDUGl9sIv9zNrY7QJX_2GtO_0"
    }
}