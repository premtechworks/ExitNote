package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        ChecklistTemplate::class,
        ChecklistItem::class,
        CheckSession::class,
        SessionItem::class
    ],
    version = 1,
    exportSchema = true
)
abstract class ExitNoteDatabase : RoomDatabase() {

    abstract fun exitNoteDao(): ExitNoteDao

    companion object {
        private const val DATABASE_NAME = "exitnote_local.db"

        @Volatile
        private var INSTANCE: ExitNoteDatabase? = null

        fun getInstance(context: Context): ExitNoteDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    ExitNoteDatabase::class.java,
                    DATABASE_NAME
                )
                    .fallbackToDestructiveMigration(false)
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
