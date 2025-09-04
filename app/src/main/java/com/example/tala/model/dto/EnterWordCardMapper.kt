package com.example.tala.model.dto

import com.example.tala.entity.card.Card
import com.example.tala.model.enums.CardTypeEnum
import com.example.tala.model.dto.info.WordCardInfo

fun Card.toEnterWordDto(): EnterWordCardDto {
    require(this.cardType == CardTypeEnum.ENTER_WORD) { "CardType must be ENTER_WORD to map to EnterWordCardDto" }

    val info = WordCardInfo.fromJson(this.info)
    val englishFromInfo = info.english
    val russianFromInfo = info.russian
    val imagePathFromInfo = info.imagePath
    val extractedHint = info.hint

    return EnterWordCardDto(
        id = this.id,
        commonId = this.commonId,
        nextReviewDate = this.nextReviewDate,
        categoryId = this.categoryId,
        cardType = CardTypeEnum.ENTER_WORD,
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

fun EnterWordCardDto.toEntity(): Card {
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
        cardType = CardTypeEnum.ENTER_WORD,
        interval = this.interval,
        status = this.status,
        ef = this.ef,
    )
}



