package com.example.tala

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.tala.entity.category.CategoryDao
import com.example.tala.entity.category.Category
import com.example.tala.entity.learningMode.LearningMode
import com.example.tala.entity.learningMode.LearningModeDao
import com.example.tala.entity.card.Card
import com.example.tala.entity.card.CardDao

@Database(entities = [Card::class, Category::class, LearningMode::class], version = 9, exportSchema = false)
abstract class TalaDatabase : RoomDatabase() {
    abstract fun cardDao(): CardDao
    abstract fun categoryDao(): CategoryDao
    abstract fun learningModeDao(): LearningModeDao

    companion object {
        @Volatile
        private var INSTANCE: TalaDatabase? = null

        fun getDatabase(context: Context): TalaDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    TalaDatabase::class.java,
                    "tala_database"
                ).fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}