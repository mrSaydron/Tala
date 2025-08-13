package com.example.tala

import android.content.Context
import androidx.core.content.edit
import com.example.tala.model.enums.CardTypeEnum

class ReviewSettings(context: Context) {

    private val sharedPreferences = context.getSharedPreferences("review_settings", Context.MODE_PRIVATE)

    fun getEf(cardType: CardTypeEnum): Double {
        val key = efKey(cardType)
        val defaultValue = cardType.defaultEf.toFloat()
        return sharedPreferences.getFloat(key, defaultValue).toDouble()
    }

    fun setEf(cardType: CardTypeEnum, value: Double) {
        val key = efKey(cardType)
        sharedPreferences.edit { putFloat(key, value.toFloat()) }
    }

    private fun efKey(cardType: CardTypeEnum): String = when (cardType) {
        CardTypeEnum.TRANSLATE -> "ef_translate"
        CardTypeEnum.REVERSE_TRANSLATE -> "ef_reverse_translate"
        CardTypeEnum.ENTER_WORD -> "ef_enter_word"
        CardTypeEnum.SENTENCE_TO_STUDIED_LANGUAGE -> "ef_sentence_to_studied"
        CardTypeEnum.SENTENCE_TO_STUDENT_LANGUAGE -> "ef_sentence_to_student"
    }

    var englishLevel: String
        get() = sharedPreferences.getString("english_level", "A0") ?: "A0"
        set(value) = sharedPreferences.edit { putString("english_level", value) }

}