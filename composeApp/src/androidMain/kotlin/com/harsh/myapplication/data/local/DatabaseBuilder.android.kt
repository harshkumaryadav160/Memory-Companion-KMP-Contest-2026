package com.harsh.myapplication.data.local

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase

/**
 * Android-specific database builder
 * Uses application context to create database
 */
actual fun getDatabaseBuilder(): RoomDatabase.Builder<AppDatabase> {
    val appContext = getApplicationContext()
    val dbFile = appContext.getDatabasePath(AppDatabase.DATABASE_NAME)
    return Room.databaseBuilder<AppDatabase>(
        context = appContext,
        name = dbFile.absolutePath
    )
}

// Helper to get application context
private lateinit var applicationContext: Context

fun initializeDatabase(context: Context) {
    applicationContext = context.applicationContext
}

internal fun getApplicationContext(): Context {
    return applicationContext
}
