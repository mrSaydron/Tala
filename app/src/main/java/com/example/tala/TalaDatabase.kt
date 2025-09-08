package com.example.tala

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.tala.entity.collection.CollectionDao
import com.example.tala.entity.collection.CardCollection
import com.example.tala.entity.card.Card
import com.example.tala.entity.card.CardDao

@Database(entities = [Card::class, CardCollection::class], version = 15, exportSchema = false)
abstract class TalaDatabase : RoomDatabase() {
    abstract fun cardDao(): CardDao
    abstract fun collectionsDao(): CollectionDao

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
    }
}