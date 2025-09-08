package com.example.tala.model.dto

import com.example.tala.model.dto.info.WordCardInfo
import com.example.tala.model.enums.CardTypeEnum
import com.example.tala.model.enums.StatusEnum

data class ReverseTranslateCardDto(
    override val id: Int,
    override val commonId: String?,
    override val nextReviewDate: Long,
    override val collectionId: Int,
    override val cardType: CardTypeEnum = CardTypeEnum.REVERSE_TRANSLATE,
    override val intervalMinutes: Int,
    override val status: StatusEnum,
    override val ef: Double,
    override val info: WordCardInfo?
) : CardDto


