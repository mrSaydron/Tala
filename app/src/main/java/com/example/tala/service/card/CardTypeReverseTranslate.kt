package com.example.tala.service.card

import com.example.tala.entity.card.Card
import com.example.tala.model.dto.CardListDto
import com.example.tala.model.enums.CardTypeEnum

class CardTypeReverseTranslate : CardType {

    override fun create(cardDto: CardListDto): Card {
        return Card(
            commonId = cardDto.commonId,
            english = cardDto.english,
            russian = cardDto.russian,
            categoryId = cardDto.categoryId,
            imagePath = cardDto.imagePath,
            cardType = CardTypeEnum.REVERSE_TRANSLATE,
            ef = CardTypeEnum.REVERSE_TRANSLATE.defaultEf
        )
    }

    override fun update(cardDto: CardListDto, card: Card): Card {
        return card.copy(
            english = cardDto.english,
            russian = cardDto.russian,
            categoryId = cardDto.categoryId,
            imagePath = cardDto.imagePath
        )
    }

    override fun toListDto(card: Card): CardListDto {
        return CardListDto(
            commonId = card.commonId,
            english = card.english,
            russian = card.russian,
            categoryId = card.categoryId,
            imagePath = card.imagePath
        )
    }
}