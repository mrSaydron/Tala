package com.example.tala.model.dto

import com.example.tala.model.enums.StatusEnum

fun CardDto.copy(
    nextReviewDate: Long? = null,
    intervalMinutes: Int? = null,
    status: StatusEnum? = null,
    ef: Double? = null,
): CardDto {
    return when (this) {
        is TranslateCardDto -> this.copy(
            nextReviewDate = nextReviewDate ?: this.nextReviewDate,
            intervalMinutes = intervalMinutes ?: this.intervalMinutes,
            status = status ?: this.status,
            ef = ef ?: this.ef,
        )
        is ReverseTranslateCardDto -> this.copy(
            nextReviewDate = nextReviewDate ?: this.nextReviewDate,
            intervalMinutes = intervalMinutes ?: this.intervalMinutes,
            status = status ?: this.status,
            ef = ef ?: this.ef,
        )
        is EnterWordCardDto -> this.copy(
            nextReviewDate = nextReviewDate ?: this.nextReviewDate,
            intervalMinutes = intervalMinutes ?: this.intervalMinutes,
            status = status ?: this.status,
            ef = ef ?: this.ef,
        )
        else -> this
    }
}