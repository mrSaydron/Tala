package com.example.tala.fragment.adapter

import com.example.tala.entity.word.WordLevel
import java.util.UUID

data class WordEditGroup(
    val key: String = UUID.randomUUID().toString(),
    var baseWordId: Int? = null,
    val items: MutableList<WordEditItem> = mutableListOf(),
    var level: WordLevel? = null,
    var isExpanded: Boolean = true,
)


