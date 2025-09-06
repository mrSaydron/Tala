package com.example.tala.model.dto.info

import org.json.JSONObject
import java.io.Serializable

data class WordCardInfo(
    val english: String? = null,
    val russian: String? = null,
    val imagePath: String? = null,
    val hint: String? = null,
) : CardInfo, Serializable {
    fun toJsonOrNull(): String? {
        return try {
            val obj = JSONObject()
            if (!english.isNullOrEmpty()) obj.put("english", english)
            if (!russian.isNullOrEmpty()) obj.put("russian", russian)
            if (!imagePath.isNullOrEmpty()) obj.put("imagePath", imagePath)
            if (!hint.isNullOrEmpty()) obj.put("hint", hint)
            if (obj.length() == 0) null else obj.toString()
        } catch (_: Exception) { null }
    }

    companion object {
        fun fromJson(raw: String?): WordCardInfo {
            if (raw.isNullOrEmpty()) return WordCardInfo()
            return try {
                val obj = JSONObject(raw)
                WordCardInfo(
                    english = obj.optString("english", null).takeIf { !it.isNullOrEmpty() },
                    russian = obj.optString("russian", null).takeIf { !it.isNullOrEmpty() },
                    imagePath = obj.optString("imagePath", null).takeIf { !it.isNullOrEmpty() },
                    hint = obj.optString("hint", null).takeIf { !it.isNullOrEmpty() },
                )
            } catch (_: Exception) { WordCardInfo() }
        }
    }
}


