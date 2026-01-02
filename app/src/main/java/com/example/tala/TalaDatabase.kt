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
import com.example.tala.entity.dictionary.Dictionary
import com.example.tala.entity.dictionary.DictionaryDao
import com.example.tala.entity.dictionary.DictionaryTypeConverters
import com.example.tala.entity.dictionaryCollection.DictionaryCollection
import com.example.tala.entity.dictionaryCollection.DictionaryCollectionDao
import com.example.tala.entity.dictionaryCollection.DictionaryCollectionEntry
import com.example.tala.entity.dictionaryCollection.DictionaryCollectionEntryDao
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
        Dictionary::class,
        DictionaryCollection::class,
        DictionaryCollectionEntry::class,
        Lesson::class,
        LessonCardType::class,
        LessonProgress::class,
        CardHistory::class
    ],
    version = 29,
    exportSchema = false
)
@TypeConverters(
    value = [
        DictionaryTypeConverters::class,
        LessonTypeConverters::class,
        LessonProgressTypeConverters::class
    ]
)
abstract class TalaDatabase : RoomDatabase() {
    abstract fun dictionaryDao(): DictionaryDao
    abstract fun dictionaryCollectionDao(): DictionaryCollectionDao
    abstract fun dictionaryCollectionEntryDao(): DictionaryCollectionEntryDao
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
                    .addMigrations(
                        MIGRATION_15_16,
                        MIGRATION_16_17,
                        MIGRATION_17_18,
                        MIGRATION_18_19,
                        MIGRATION_19_20,
                        MIGRATION_20_21,
                        MIGRATION_21_22,
                        MIGRATION_22_23,
                        MIGRATION_23_24,
                        MIGRATION_24_25,
                        MIGRATION_25_26,
                        MIGRATION_26_27,
                        MIGRATION_27_28,
                        MIGRATION_28_29
                    )
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
                createDictionaryCollectionEntriesTable(db)
            }
        }

        private val MIGRATION_19_20 = object : Migration(19, 20) {
            override fun migrate(db: SupportSQLiteDatabase) {
                data class LegacyCollection(
                    val id: Int,
                    val name: String,
                    val description: String?,
                    val dictionaryIds: String?
                )

                val hasDictionaryIdsColumn = db.query("PRAGMA table_info(`dictionary_collections`)").use { cursor ->
                    val nameIndex = cursor.getColumnIndex("name")
                    var hasColumn = false
                    while (cursor.moveToNext()) {
                        if (nameIndex >= 0 && cursor.getString(nameIndex).equals("dictionary_ids", ignoreCase = true)) {
                            hasColumn = true
                            break
                        }
                    }
                    hasColumn
                }

                val selectSql = if (hasDictionaryIdsColumn) {
                    "SELECT id, name, description, dictionary_ids FROM dictionary_collections"
                } else {
                    "SELECT id, name, description, NULL as dictionary_ids FROM dictionary_collections"
                }

                val legacyCollections = mutableListOf<LegacyCollection>()
                val cursor = db.query(selectSql)
                cursor.use {
                    val idIndex = it.getColumnIndex("id")
                    val nameIndex = it.getColumnIndex("name")
                    val descriptionIndex = it.getColumnIndex("description")
                    val dictionaryIdsIndex = it.getColumnIndex("dictionary_ids")
                    while (it.moveToNext()) {
                        if (idIndex < 0 || nameIndex < 0) continue
                        val id = it.getInt(idIndex)
                        val name = it.getString(nameIndex)
                        val description = if (descriptionIndex >= 0) it.getString(descriptionIndex) else null
                        val dictionaryIds = if (dictionaryIdsIndex >= 0) it.getString(dictionaryIdsIndex) else null
                        legacyCollections.add(LegacyCollection(id, name, description, dictionaryIds))
                    }
                }

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `dictionary_collections_new` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `name` TEXT NOT NULL,
                        `description` TEXT
                    )
                    """.trimIndent()
                )

                legacyCollections.forEach { collection ->
                    db.execSQL(
                        "INSERT INTO `dictionary_collections_new` (`id`, `name`, `description`) VALUES (?, ?, ?)",
                        arrayOf<Any?>(collection.id, collection.name, collection.description)
                    )
                }

                db.execSQL("DROP TABLE IF EXISTS `dictionary_collections`")
                db.execSQL("ALTER TABLE `dictionary_collections_new` RENAME TO `dictionary_collections`")
                db.execSQL(
                    "CREATE UNIQUE INDEX IF NOT EXISTS `index_dictionary_collections_name` ON `dictionary_collections` (`name`)"
                )

                createDictionaryCollectionEntriesTable(db)

                legacyCollections.forEach { collection ->
                    val dictionaryIds = collection.dictionaryIds
                        ?.split(',')
                        ?.mapNotNull { it.trim().toIntOrNull() }
                        ?.distinct()
                        ?: emptyList()

                    dictionaryIds.forEach { dictionaryId ->
                        db.execSQL(
                            """
                            INSERT OR IGNORE INTO `dictionary_collection_entries` (`collection_id`, `dictionary_id`)
                            VALUES (?, ?)
                            """.trimIndent(),
                            arrayOf<Any?>(collection.id, dictionaryId)
                        )
                    }
                }
            }
        }

        private val MIGRATION_20_21 = object : Migration(20, 21) {
            override fun migrate(db: SupportSQLiteDatabase) {
                createLessonsTable(db)
            }
        }

        private val MIGRATION_21_22 = object : Migration(21, 22) {
            override fun migrate(db: SupportSQLiteDatabase) {
                createLessonCardTypesTable(db)
            }
        }

        private val MIGRATION_22_23 = object : Migration(22, 23) {
            override fun migrate(db: SupportSQLiteDatabase) {
                createLessonProgressTable(db)
            }
        }

        private val MIGRATION_23_24 = object : Migration(23, 24) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("DROP TABLE IF EXISTS `lesson_progress`")
                db.execSQL("DROP TABLE IF EXISTS `lesson_card_types`")
                createLessonCardTypesTable(db)
                createLessonProgressTable(db)
            }
        }

        private val MIGRATION_24_25 = object : Migration(24, 25) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("DROP TABLE IF EXISTS `lesson_progress`")
                db.execSQL("DROP TABLE IF EXISTS `lessons`")
                createLessonsTable(db)
                createLessonCardTypesTable(db)
                createLessonProgressTable(db)
            }
        }

        private val MIGRATION_25_26 = object : Migration(25, 26) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `dictionary` ADD COLUMN `image_path` TEXT")
            }
        }

        private val MIGRATION_26_27 = object : Migration(26, 27) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `card_history` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `lesson_id` INTEGER NOT NULL,
                        `card_type` TEXT NOT NULL,
                        `dictionary_id` INTEGER,
                        `quality` INTEGER NOT NULL,
                        `date` INTEGER NOT NULL,
                        FOREIGN KEY(`lesson_id`) REFERENCES `lessons`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE,
                        FOREIGN KEY(`dictionary_id`) REFERENCES `dictionary`(`id`) ON UPDATE NO ACTION ON DELETE SET NULL
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_card_history_lesson_id` ON `card_history` (`lesson_id`)"
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_card_history_dictionary_id` ON `card_history` (`dictionary_id`)"
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_card_history_date` ON `card_history` (`date`)"
                )
            }
        }

        private val MIGRATION_27_28 = object : Migration(27, 28) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `lesson_card_types` ADD COLUMN `condition_on_card_type` TEXT")
                db.execSQL("ALTER TABLE `lesson_card_types` ADD COLUMN `condition_on_value` INTEGER")
                db.execSQL("ALTER TABLE `lesson_card_types` ADD COLUMN `condition_off_card_type` TEXT")
                db.execSQL("ALTER TABLE `lesson_card_types` ADD COLUMN `condition_off_value` INTEGER")
            }
        }

        private val MIGRATION_28_29 = object : Migration(28, 29) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("DROP TABLE IF EXISTS `card`")
                db.execSQL("DROP TABLE IF EXISTS `collections`")
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
                    `description` TEXT
                )
                """.trimIndent()
            )
            db.execSQL(
                "CREATE UNIQUE INDEX IF NOT EXISTS `index_dictionary_collections_name` ON `dictionary_collections` (`name`)"
            )
        }

        private fun createDictionaryCollectionEntriesTable(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `dictionary_collection_entries` (
                    `collection_id` INTEGER NOT NULL,
                    `dictionary_id` INTEGER NOT NULL,
                    PRIMARY KEY(`collection_id`, `dictionary_id`),
                    FOREIGN KEY(`collection_id`) REFERENCES `dictionary_collections`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE,
                    FOREIGN KEY(`dictionary_id`) REFERENCES `dictionary`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                )
                """.trimIndent()
            )
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS `index_dictionary_collection_entries_collection` ON `dictionary_collection_entries` (`collection_id`)"
            )
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS `index_dictionary_collection_entries_dictionary` ON `dictionary_collection_entries` (`dictionary_id`)"
            )
        }

        private fun createLessonsTable(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `lessons` (
                    `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    `name` TEXT NOT NULL,
                    `full_name` TEXT NOT NULL,
                    `collection_id` INTEGER NOT NULL,
                    FOREIGN KEY(`collection_id`) REFERENCES `dictionary_collections`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                )
                """.trimIndent()
            )
            db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_lessons_name` ON `lessons` (`name`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_lessons_collection_id` ON `lessons` (`collection_id`)")
        }

        private fun createLessonCardTypesTable(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `lesson_card_types` (
                    `collection_id` INTEGER NOT NULL,
                    `card_type` TEXT NOT NULL,
                    `condition_on_card_type` TEXT,
                    `condition_on_value` INTEGER,
                    `condition_off_card_type` TEXT,
                    `condition_off_value` INTEGER,
                    PRIMARY KEY(`collection_id`, `card_type`),
                    FOREIGN KEY(`collection_id`) REFERENCES `dictionary_collections`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                )
                """.trimIndent()
            )
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS `index_lesson_card_types_collection_id` ON `lesson_card_types` (`collection_id`)"
            )
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS `index_lesson_card_types_card_type` ON `lesson_card_types` (`card_type`)"
            )
        }

        private fun createLessonProgressTable(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `lesson_progress` (
                    `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    `lesson_id` INTEGER NOT NULL,
                    `card_type` TEXT NOT NULL,
                    `dictionary_id` INTEGER,
                    `next_review_date` INTEGER,
                    `interval_minutes` INTEGER NOT NULL,
                    `ef` REAL NOT NULL,
                    `status` TEXT NOT NULL,
                    `info` TEXT,
                    FOREIGN KEY(`lesson_id`) REFERENCES `lessons`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE,
                    FOREIGN KEY(`dictionary_id`) REFERENCES `dictionary`(`id`) ON UPDATE NO ACTION ON DELETE SET NULL
                )
                """.trimIndent()
            )
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS `index_lesson_progress_lesson_card` ON `lesson_progress` (`lesson_id`, `card_type`)"
            )
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS `index_lesson_progress_dictionary` ON `lesson_progress` (`dictionary_id`)"
            )
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS `index_lesson_progress_next_review_date` ON `lesson_progress` (`next_review_date`)"
            )
        }
    }

}