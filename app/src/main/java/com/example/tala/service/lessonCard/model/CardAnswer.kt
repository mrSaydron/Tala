package com.example.tala.service.lessonCard.model

sealed interface CardAnswer {
    data class Text(val value: String) : CardAnswer
    data class Comparison(val matches: List<Match>) : CardAnswer {
        data class Match(
            val progressId: Int,
            val selectedWordId: Int?
        )
    }
}

