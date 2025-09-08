package com.example.tala.model.dto

import com.example.tala.model.dto.info.CardInfo
import com.example.tala.model.enums.CardTypeEnum
import com.example.tala.model.enums.StatusEnum

interface CardDto {
    val id: Int
    val commonId: String?
    val nextReviewDate: Long
    val categoryId: Int
    val cardType: CardTypeEnum
    val intervalMinutes: Int
    val status: StatusEnum
    val ef: Double
    val info: CardInfo?
}


