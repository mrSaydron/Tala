package com.example.tala.model.dto

import com.example.tala.entity.card.Card
import com.example.tala.model.enums.CardTypeEnum
import com.example.tala.model.dto.info.WordCardInfo

fun Card.toReverseTranslateDto(): ReverseTranslateCardDto {
    require(this.cardType == CardTypeEnum.REVERSE_TRANSLATE) { "CardType must be REVERSE_TRANSLATE to map to ReverseTranslateCardDto" }

    val info = WordCardInfo.fromJson(this.info)
    val englishFromInfo = info.english
    val russianFromInfo = info.russian
    val imagePathFromInfo = info.imagePath
    val extractedHint = info.hint

    return ReverseTranslateCardDto(
        id = this.id,
        commonId = this.commonId,
        nextReviewDate = this.nextReviewDate,
        collectionId = this.collectionId,
        cardType = CardTypeEnum.REVERSE_TRANSLATE,
        intervalMinutes = this.intervalMinutes,
        status = this.status,
        ef = this.ef,
        info = WordCardInfo(
            english = englishFromInfo,
            russian = russianFromInfo,
            imagePath = imagePathFromInfo,
            hint = extractedHint,
        )
    )
}

fun ReverseTranslateCardDto.toEntity(): Card {
    val infoJson: String? = (this.info as? WordCardInfo)?.toJsonOrNull()

    return Card(
        id = this.id,
        commonId = this.commonId,
        nextReviewDate = this.nextReviewDate,
        collectionId = this.collectionId,
        info = infoJson,
        cardType = CardTypeEnum.REVERSE_TRANSLATE,
        intervalMinutes = this.intervalMinutes,
        status = this.status,
        ef = this.ef,
    )
}


