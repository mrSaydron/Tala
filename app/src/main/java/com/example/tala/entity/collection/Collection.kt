package com.example.tala.entity.collection

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "collections")
data class CardCollection(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String
)


