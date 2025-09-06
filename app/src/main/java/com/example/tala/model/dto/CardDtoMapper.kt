package com.example.tala.model.dto

import com.example.tala.entity.card.Card
import com.example.tala.model.dto.info.WordCardInfo
import com.example.tala.model.enums.CardTypeEnum

fun Card.toCardDto(): CardDto {
    val parsed = WordCardInfo.fromJson(this.info)
    val mergedInfo = WordCardInfo(
        english = parsed.english,
        russian = parsed.russian,
        imagePath = parsed.imagePath,
        hint = parsed.hint,
    )

    return when (this.cardType) {
        CardTypeEnum.TRANSLATE -> TranslateCardDto(
            id = this.id,
            commonId = this.commonId,
            nextReviewDate = this.nextReviewDate,
            categoryId = this.categoryId,
            interval = this.interval,
            status = this.status,
            ef = this.ef,
            info = mergedInfo,
        )
        CardTypeEnum.REVERSE_TRANSLATE -> ReverseTranslateCardDto(
            id = this.id,
            commonId = this.commonId,
            nextReviewDate = this.nextReviewDate,
            categoryId = this.categoryId,
            interval = this.interval,
            status = this.status,
            ef = this.ef,
            info = mergedInfo,
        )
        CardTypeEnum.ENTER_WORD -> EnterWordCardDto(
            id = this.id,
            commonId = this.commonId,
            nextReviewDate = this.nextReviewDate,
            categoryId = this.categoryId,
            interval = this.interval,
            status = this.status,
            ef = this.ef,
            info = mergedInfo,
        )
        else -> throw UnsupportedOperationException("Card type not supported for DTO mapping: ${'$'}{this.cardType}")
    }
}

fun CardDto.toEntityCard(): Card {
    val info = this.info as? WordCardInfo
    val infoJson = info?.toJsonOrNull()
    return Card(
        id = this.id,
        commonId = this.commonId,
        nextReviewDate = this.nextReviewDate,
        categoryId = this.categoryId,
        info = infoJson,
        cardType = this.cardType,
        interval = this.interval,
        status = this.status,
        ef = this.ef,
    )
}


