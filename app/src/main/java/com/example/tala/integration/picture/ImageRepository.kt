package com.example.tala.integration.picture

import android.content.Context
import android.graphics.drawable.Drawable
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.example.tala.service.ApiClient
import java.io.File

class ImageRepository(
    private val unsplashApi: com.example.tala.integration.picture.UnsplashApi = ApiClient.unsplashApi,
    private val apiKey: String = UnsplashApi.USPLASH_API_KEY,
) {
    suspend fun searchImages(query: String): List<String> = try {
        unsplashApi.searchImages(query = query, apiKey = apiKey).results.map { it.urls.regular }
    } catch (_: Exception) {
        emptyList()
    }

    fun downloadToFile(context: Context, imageUrl: String, callback: (File?) -> Unit) {
        Glide.with(context)
            .asFile()
            .load(imageUrl)
            .into(object : CustomTarget<File>() {
                override fun onResourceReady(resource: File, transition: Transition<in File>?) {
                    callback(resource)
                }

                override fun onLoadCleared(placeholder: Drawable?) {
                    callback(null)
                }
            })
    }
}


