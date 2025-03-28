package com.example.tala

import android.content.Context
import androidx.core.content.edit

class ReviewSettings(context: Context) {

    private val sharedPreferences = context.getSharedPreferences("review_settings", Context.MODE_PRIVATE)

    var easyInterval: Int
        get() = sharedPreferences.getInt("easy_interval", 4) // По умолчанию 4 дня
        set(value) = sharedPreferences.edit { putInt("easy_interval", value) }

    var mediumInterval: Int
        get() = sharedPreferences.getInt("medium_interval", 2) // По умолчанию 2 дня
        set(value) = sharedPreferences.edit { putInt("medium_interval", value) }

    var hardInterval: Int
        get() = sharedPreferences.getInt("hard_interval", 1) // По умолчанию 1 день
        set(value) = sharedPreferences.edit { putInt("hard_interval", value) }

    var englishLevel: String
        get() = sharedPreferences.getString("english_level", "A0") ?: "A0"
        set(value) = sharedPreferences.edit { putString("english_level", value) }

}