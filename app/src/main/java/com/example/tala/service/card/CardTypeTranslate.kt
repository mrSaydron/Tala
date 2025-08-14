package com.example.tala.service.card

import com.example.tala.entity.card.Card
import com.example.tala.model.dto.CardListDto
import com.example.tala.model.enums.CardTypeEnum

class CardTypeTranslate : CardType {

    override fun create(cardDto: CardListDto): Card {
        return Card(
            commonId = cardDto.commonId,
            english = cardDto.english,
            russian = cardDto.russian,
            categoryId = cardDto.categoryId,
            imagePath = cardDto.imagePath,
            info = cardDto.info,
            cardType = CardTypeEnum.TRANSLATE,
            ef = CardTypeEnum.TRANSLATE.defaultEf
        )
    }

    override fun update(cardDto: CardListDto, card: Card): Card {
        return card.copy(
            english = cardDto.english,
            russian = cardDto.russian,
            categoryId = cardDto.categoryId,
            imagePath = cardDto.imagePath,
            info = cardDto.info
        )
    }

    override fun toListDto(card: Card): CardListDto {
        return CardListDto(
            commonId = card.commonId,
            english = card.english,
            russian = card.russian,
            categoryId = card.categoryId,
            imagePath = card.imagePath,
            info = card.info
        )
    }
}