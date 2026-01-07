package com.example.tala.entity.word

import androidx.room.ColumnInfo
import androidx.room.Embedded

data class WordWithDependentCount(
    @Embedded
    val word: Word,
    @ColumnInfo(name = "dependent_count")
    val dependentCount: Int,
)

