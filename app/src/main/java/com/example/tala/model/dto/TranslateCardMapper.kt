package com.example.tala.model.dto

import com.example.tala.entity.card.Card
import com.example.tala.model.enums.CardTypeEnum
import com.example.tala.model.dto.info.WordCardInfo

fun Card.toTranslateDto(): TranslateCardDto {
    require(this.cardType == CardTypeEnum.TRANSLATE) { "CardType must be TRANSLATE to map to TranslateCardDto" }

    val info = WordCardInfo.fromJson(this.info)
    val englishFromInfo = info.english
    val russianFromInfo = info.russian
    val imagePathFromInfo = info.imagePath
    val extractedHint = info.hint

    return TranslateCardDto(
        id = this.id,
        commonId = this.commonId,
        nextReviewDate = this.nextReviewDate,
        categoryId = this.categoryId,
        cardType = CardTypeEnum.TRANSLATE,
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

fun TranslateCardDto.toEntity(): Card {
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
        cardType = CardTypeEnum.TRANSLATE,
        interval = this.interval,
        status = this.status,
        ef = this.ef,
    )
}



