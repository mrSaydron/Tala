package com.example.tala

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.tala.entity.cardhistory.CardHistory
import com.example.tala.entity.cardhistory.CardHistoryDao
import com.example.tala.entity.word.Word
import com.example.tala.entity.word.WordDao
import com.example.tala.entity.word.WordTypeConverters
import com.example.tala.entity.wordCollection.WordCollection
import com.example.tala.entity.wordCollection.WordCollectionDao
import com.example.tala.entity.wordCollection.WordCollectionEntry
import com.example.tala.entity.wordCollection.WordCollectionEntryDao
import com.example.tala.entity.lessoncardtype.LessonCardType
import com.example.tala.entity.lessoncardtype.LessonCardTypeDao
import com.example.tala.entity.lesson.LessonDao
import com.example.tala.entity.lessoncardtype.LessonTypeConverters
import com.example.tala.entity.lesson.Lesson
import com.example.tala.entity.lessonprogress.LessonProgress
import com.example.tala.entity.lessonprogress.LessonProgressDao
import com.example.tala.entity.lessonprogress.LessonProgressTypeConverters

@Database(
    entities = [
        Word::class,
        WordCollection::class,
        WordCollectionEntry::class,
        Lesson::class,
        LessonCardType::class,
        LessonProgress::class,
        CardHistory::class
    ],
    version = 31,
    exportSchema = false
)
@TypeConverters(
    value = [
        WordTypeConverters::class,
        LessonTypeConverters::class,
        LessonProgressTypeConverters::class
    ]
)
abstract class TalaDatabase : RoomDatabase() {
    abstract fun wordDao(): WordDao
    abstract fun wordCollectionDao(): WordCollectionDao
    abstract fun wordCollectionEntryDao(): WordCollectionEntryDao
    abstract fun lessonDao(): LessonDao
    abstract fun lessonCardTypeDao(): LessonCardTypeDao
    abstract fun lessonProgressDao(): LessonProgressDao
    abstract fun cardHistoryDao(): CardHistoryDao

    companion object {
        @Volatile
        private var INSTANCE: TalaDatabase? = null

        fun getDatabase(context: Context): TalaDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    TalaDatabase::class.java,
                    "tala_database"
                )
//                    .fallbackToDestructiveMigration()
                    .addMigrations(
                    )
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }

}