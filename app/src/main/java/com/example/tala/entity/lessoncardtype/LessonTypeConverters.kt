package com.example.tala.entity.lessoncardtype

import androidx.room.TypeConverter
import com.example.tala.model.enums.CardTypeEnum

object LessonTypeConverters {

    @TypeConverter
    @JvmStatic
    fun fromCardType(cardType: CardTypeEnum?): String? = cardType?.name

    @TypeConverter
    @JvmStatic
    fun toCardType(value: String?): CardTypeEnum? = value?.let { CardTypeEnum.valueOf(it) }
}


