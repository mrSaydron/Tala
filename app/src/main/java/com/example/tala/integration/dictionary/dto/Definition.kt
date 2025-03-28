package com.example.tala.integration.dictionary.dto

data class Definition(
    val text: String,
    val pos: String,
    val tr: List<Translation>
)
