package com.example.tala.model.dto

import com.example.tala.model.dto.info.WordCardInfo
import com.example.tala.model.enums.CardTypeEnum
import com.example.tala.model.enums.StatusEnum

data class EnterWordCardDto(
    override val id: Int,
    override val commonId: String?,
    override val nextReviewDate: Long,
    override val categoryId: Int,
    override val cardType: CardTypeEnum = CardTypeEnum.ENTER_WORD,
    override val interval: Int,
    override val status: StatusEnum,
    override val ef: Double,
    override val info: WordCardInfo?
) : CardDto


