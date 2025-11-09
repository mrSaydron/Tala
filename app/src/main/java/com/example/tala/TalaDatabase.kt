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
import com.example.tala.entity.dictionary.DictionaryCollection
import com.example.tala.entity.dictionary.DictionaryCollectionDao
import com.example.tala.entity.dictionary.DictionaryDao
import com.example.tala.entity.dictionary.DictionaryTypeConverters

@Database(
    entities = [Card::class, CardCollection::class, Dictionary::class, DictionaryCollection::class],
    version = 19,
    exportSchema = false
)
@TypeConverters(DictionaryTypeConverters::class)
abstract class TalaDatabase : RoomDatabase() {
    abstract fun cardDao(): CardDao
    abstract fun collectionsDao(): CollectionDao
    abstract fun dictionaryDao(): DictionaryDao
    abstract fun dictionaryCollectionDao(): DictionaryCollectionDao

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
                    .addMigrations(MIGRATION_15_16, MIGRATION_16_17, MIGRATION_17_18, MIGRATION_18_19)
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
                recreateDictionaryTable(db)
            }
        }

        private val MIGRATION_16_17 = object : Migration(16, 17) {
            override fun migrate(db: SupportSQLiteDatabase) {
                recreateDictionaryTable(db)
            }
        }

        private val MIGRATION_17_18 = object : Migration(17, 18) {
            override fun migrate(db: SupportSQLiteDatabase) {
                recreateDictionaryTable(db)
            }
        }

        private val MIGRATION_18_19 = object : Migration(18, 19) {
            override fun migrate(db: SupportSQLiteDatabase) {
                createDictionaryCollectionsTable(db)
            }
        }

        private fun recreateDictionaryTable(db: SupportSQLiteDatabase) {
            db.execSQL("DROP TABLE IF EXISTS `dictionary`")
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
                    `tags` TEXT NOT NULL DEFAULT '',
                    FOREIGN KEY(`base_word_id`) REFERENCES `dictionary`(`id`) ON UPDATE NO ACTION ON DELETE SET NULL
                )
                """.trimIndent()
            )
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_dictionary_word` ON `dictionary` (`word`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_dictionary_base_word_id` ON `dictionary` (`base_word_id`)")
        }

        private fun createDictionaryCollectionsTable(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `dictionary_collections` (
                    `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    `name` TEXT NOT NULL,
                    `description` TEXT,
                    `dictionary_ids` TEXT NOT NULL DEFAULT ''
                )
                """.trimIndent()
            )
            db.execSQL(
                "CREATE UNIQUE INDEX IF NOT EXISTS `index_dictionary_collections_name` ON `dictionary_collections` (`name`)"
            )
        }
    }

}