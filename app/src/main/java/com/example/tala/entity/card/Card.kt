package com.example.tala.entity.card

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.tala.model.enums.CardTypeEnum
import com.example.tala.model.enums.StatusEnum
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.UUID

@Entity(tableName = "card")
data class Card(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val commonId: String? = null,

    val english: String,
    val russian: String,
    val nextReviewDate: Long = LocalDateTime.now().atZone(ZoneId.systemDefault()).toEpochSecond(),
    val categoryId: Int = 0,
    val imagePath: String? = null,
    val cardType: CardTypeEnum,

    val interval: Int = 1,
    val status: StatusEnum = StatusEnum.NEW,
    val ef: Double = 2.5,
)
