package com.example.tala.integration.dictionary.dto

data class Translation(
    val text: String,
    val pos: String,
    val gen: String?,
    val asp: String?,
    val fr: Int?,
    val syn: List<Synonym> = emptyList(),
    val mean: List<Mean> = emptyList(),
)