package com.example.tala.model.dto

import com.example.tala.model.enums.CardTypeEnum

data class CardListDto(
    val commonId: String? = null,

    val english: String,
    val russian: String,
    val categoryId: Int = 0,
    val imagePath: String? = null,
    val info: String? = null,
    val types: Set<CardTypeEnum> = emptySet(),
)
