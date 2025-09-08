package com.example.tala.model.dto

import com.example.tala.model.dto.info.CardInfo
import com.example.tala.model.enums.CardTypeEnum

data class CardListDto(
    val commonId: String? = null,
    val categoryId: Int = 0,
    val cards: Map<CardTypeEnum, CardInfo> = emptyMap(),
)
