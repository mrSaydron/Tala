package com.example.tala.entity.learningMode

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "learning_modes")
data class LearningMode(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val name: String
)