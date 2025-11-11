package com.example.tala.entity.lessonprogress

import androidx.room.TypeConverter
import com.example.tala.model.enums.StatusEnum

object LessonProgressTypeConverters {

    @TypeConverter
    @JvmStatic
    fun fromStatus(status: StatusEnum?): String? = status?.name

    @TypeConverter
    @JvmStatic
    fun toStatus(value: String?): StatusEnum? = value?.let { StatusEnum.valueOf(it) }
}

