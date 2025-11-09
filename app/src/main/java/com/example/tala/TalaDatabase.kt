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
import com.example.tala.entity.dictionaryCollection.DictionaryCollection
import com.example.tala.entity.dictionaryCollection.DictionaryCollectionDao
import com.example.tala.entity.dictionaryCollection.DictionaryCollectionEntry
import com.example.tala.entity.dictionaryCollection.DictionaryCollectionEntryDao

@Database(
    entities = [
        Card::class,
        CardCollection::class,
        Dictionary::class,
        DictionaryCollection::class,
        DictionaryCollectionEntry::class
    ],
    version = 20,
    exportSchema = false
)
@TypeConverters(DictionaryTypeConverters::class)
abstract class TalaDatabase : RoomDatabase() {
    abstract fun cardDao(): CardDao
    abstract fun collectionsDao(): CollectionDao
    abstract fun dictionaryDao(): DictionaryDao
    abstract fun dictionaryCollectionDao(): DictionaryCollectionDao
    abstract fun dictionaryCollectionEntryDao(): DictionaryCollectionEntryDao

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
                        MIGRATION_19_20
                    )
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
    }

}