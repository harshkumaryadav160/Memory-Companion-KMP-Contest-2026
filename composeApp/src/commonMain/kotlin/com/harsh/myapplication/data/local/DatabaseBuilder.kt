package com.harsh.myapplication.data.local

import androidx.room.RoomDatabase

/**
 * Expect function for platform-specific database creation
 * Each platform (Android, Desktop) will provide its own implementation
 */
expect fun getDatabaseBuilder(): RoomDatabase.Builder<AppDatabase>
