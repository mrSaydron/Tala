package com.example.tala.fragment.model

import android.os.Parcelable
import com.example.tala.model.enums.CardTypeEnum
import kotlinx.parcelize.Parcelize

@Parcelize
data class CardTypeConditionArgs(
    val cardType: CardTypeEnum,
    val enabled: Boolean = true,
    val conditionOnType: CardTypeEnum? = null,
    val conditionOnValue: Int? = null,
    val conditionOffType: CardTypeEnum? = null,
    val conditionOffValue: Int? = null
) : Parcelable

