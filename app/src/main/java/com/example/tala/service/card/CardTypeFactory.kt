package com.example.tala.service.card

import com.example.tala.model.enums.CardTypeEnum

object CardTypeFactory {

    fun getCardType(cardType: CardTypeEnum): CardType {
        return when(cardType) {
            CardTypeEnum.TRANSLATE -> CardTypeTranslate()
            CardTypeEnum.REVERSE_TRANSLATE -> CardTypeReverseTranslate()
            CardTypeEnum.ENTER_WORD -> CardTypeEnterWord()
            CardTypeEnum.SENTENCE_TO_STUDIED_LANGUAGE -> TODO()
            CardTypeEnum.SENTENCE_TO_STUDENT_LANGUAGE -> TODO()
        }
    }

}