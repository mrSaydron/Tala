package com.example.tala.service.card

import com.example.tala.entity.card.Card
import com.example.tala.model.dto.CardListDto

interface CardType {

    fun create(cardDto: CardListDto): Card
    fun update(cardDto: CardListDto, card: Card): Card
    fun toListDto(card: Card): CardListDto

}