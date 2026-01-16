package com.harsh.myapplication.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.harsh.myapplication.data.local.dao.MemoryDao
import com.harsh.myapplication.data.local.dao.PersonDao
import com.harsh.myapplication.data.local.entity.MemoryEntity
import com.harsh.myapplication.data.local.entity.PersonEntity

/**
 * Main database class
 * Version 1: Initial schema
 */
@Database(
    entities = [
        PersonEntity::class,
        MemoryEntity::class
    ],
    version = 1,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {

    /**
     * Access to Person operations
     */
    abstract fun personDao(): PersonDao

    /**
     * Access to Memory operations
     */
    abstract fun memoryDao(): MemoryDao

    companion object {
        const val DATABASE_NAME = "memory_companion.db"
    }
}
