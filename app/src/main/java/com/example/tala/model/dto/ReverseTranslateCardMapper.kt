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
        categoryId = this.categoryId,
        cardType = CardTypeEnum.REVERSE_TRANSLATE,
        interval = this.interval,
        status = this.status,
        ef = this.ef,
        info = WordCardInfo(
            english = englishFromInfo ?: this.english,
            russian = russianFromInfo ?: this.russian,
            imagePath = imagePathFromInfo ?: this.imagePath,
            hint = extractedHint,
        )
    )
}

fun ReverseTranslateCardDto.toEntity(): Card {
    val infoJson: String? = (this.info as? WordCardInfo)?.toJsonOrNull()

    return Card(
        id = this.id,
        commonId = this.commonId,
        english = (this.info as? WordCardInfo)?.english ?: "",
        russian = (this.info as? WordCardInfo)?.russian ?: "",
        nextReviewDate = this.nextReviewDate,
        categoryId = this.categoryId,
        imagePath = (this.info as? WordCardInfo)?.imagePath,
        info = infoJson,
        cardType = CardTypeEnum.REVERSE_TRANSLATE,
        interval = this.interval,
        status = this.status,
        ef = this.ef,
    )
}


