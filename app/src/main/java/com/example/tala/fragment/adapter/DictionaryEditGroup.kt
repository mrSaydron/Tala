package com.example.tala.fragment.adapter

import com.example.tala.entity.dictionary.DictionaryLevel
import java.util.UUID

data class DictionaryEditGroup(
    val key: String = UUID.randomUUID().toString(),
    var baseWordId: Int? = null,
    val items: MutableList<DictionaryEditItem> = mutableListOf(),
    var level: DictionaryLevel? = null,
    var isExpanded: Boolean = true,
)


