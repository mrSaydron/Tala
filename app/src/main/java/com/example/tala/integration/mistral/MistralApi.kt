package com.example.tala.integration.mistral

import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

interface MistralApi {
    @POST("completions")
    suspend fun generateText(
        @Header("Authorization") apiKey: String = MISTRAL_API_KEY,
        @Body request: MistralRequest
    ): MistralResponse

    companion object {
        const val MISTRAL_API_KEY = "Bearer fGcz4WaWp1g1s5oBc40RwSsUMoSq5wbC"
    }
}

data class MistralRequest(
    val model: String,
    val messages: List<MistralRequestMessage>,
)

data class MistralRequestMessage(
    val role: String,
    val content: String,
)

data class MistralResponse(
    val choices: List<MistralChoice>
)

data class MistralChoice(
    val index: Int,
    val message: MistralResponseMessage,
    val finishReason: String,
)

data class MistralResponseMessage(
    val content: String,
    val role: String,
)

data class SentenceResponse(
    val eng: String,
    val rus: String,
)