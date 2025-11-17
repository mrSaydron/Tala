package com.example.tala.entity.dictionary

import androidx.room.ColumnInfo
import androidx.room.Embedded

data class DictionaryWithDependentCount(
    @Embedded
    val dictionary: Dictionary,
    @ColumnInfo(name = "dependent_count")
    val dependentCount: Int,
)

