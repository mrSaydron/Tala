package com.example.tala.model.dto

data class CardListDto(
    val commonId: String? = null,

    val english: String,
    val russian: String,
    val categoryId: Int = 0,
    val imagePath: String? = null,
)
