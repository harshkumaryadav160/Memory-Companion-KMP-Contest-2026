package com.harsh.myapplication.data.local

import androidx.room.Room
import androidx.room.RoomDatabase
import java.io.File

/**
 * Desktop-specific database builder
 * Stores database in user's home directory
 */
actual fun getDatabaseBuilder(): RoomDatabase.Builder<AppDatabase> {
    val dbFile = File(System.getProperty("user.home"), AppDatabase.DATABASE_NAME)
    return Room.databaseBuilder<AppDatabase>(
        name = dbFile.absolutePath
    )
}
