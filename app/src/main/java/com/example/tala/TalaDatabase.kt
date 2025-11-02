package com.example.tala

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.tala.entity.collection.CollectionDao
import com.example.tala.entity.collection.CardCollection
import com.example.tala.entity.card.Card
import com.example.tala.entity.card.CardDao
import com.example.tala.entity.dictionary.Dictionary
import com.example.tala.entity.dictionary.DictionaryDao
import com.example.tala.entity.dictionary.DictionaryTypeConverters

@Database(entities = [Card::class, CardCollection::class, Dictionary::class], version = 16, exportSchema = false)
@TypeConverters(DictionaryTypeConverters::class)
abstract class TalaDatabase : RoomDatabase() {
    abstract fun cardDao(): CardDao
    abstract fun collectionsDao(): CollectionDao
    abstract fun dictionaryDao(): DictionaryDao

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
                    .addMigrations(MIGRATION_15_16)
                    .addCallback(object : RoomDatabase.Callback() {
                        override fun onCreate(db: SupportSQLiteDatabase) {
                            super.onCreate(db)
                            // Предзаполнение коллекцией по умолчанию при первом создании БД
                            db.execSQL("INSERT INTO collections(name) VALUES('Default')")
                        }
                        override fun onOpen(db: SupportSQLiteDatabase) {
                            super.onOpen(db)
                            try {
                                val cursor = db.query("SELECT COUNT(*) FROM collections WHERE name = 'Default'")
                                cursor.use {
                                    if (it.moveToFirst()) {
                                        val cnt = it.getLong(0)
                                        if (cnt == 0L) {
                                            db.execSQL("INSERT INTO collections(name) VALUES('Default')")
                                        }
                                    }
                                }
                            } catch (_: Exception) { }
                        }
                    })
                    .build()
                INSTANCE = instance
                instance
            }
        }

        private val MIGRATION_15_16 = object : Migration(15, 16) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `dictionary` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `word` TEXT NOT NULL,
                        `translation` TEXT NOT NULL,
                        `part_of_speech` TEXT NOT NULL,
                        `ipa` TEXT,
                        `hint` TEXT,
                        `base_word_id` INTEGER,
                        `frequency` REAL,
                        `level` TEXT,
                        `tags` TEXT
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_dictionary_word` ON `dictionary` (`word`)")
            }
        }
    }

}