package com.example.tala.entity.lesson

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.tala.entity.collection.CardCollection

@Entity(
    tableName = "lessons",
    foreignKeys = [
        ForeignKey(
            entity = CardCollection::class,
            parentColumns = ["id"],
            childColumns = ["collection_id"],
            onDelete = ForeignKey.CASCADE,
            onUpdate = ForeignKey.NO_ACTION
        )
    ],
    indices = [
        Index(value = ["collection_id"]),
        Index(value = ["name"], unique = true)
    ]
)
data class Lesson(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val name: String,
    @ColumnInfo(name = "full_name")
    val fullName: String,
    @ColumnInfo(name = "collection_id")
    val collectionId: Int
)

