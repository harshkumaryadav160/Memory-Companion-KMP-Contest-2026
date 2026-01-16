package com.harsh.myapplication

import android.app.Application
import com.harsh.myapplication.data.local.initializeDatabase
import com.harsh.myapplication.di.appModule
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin

/**
 * Custom Application class for Android
 * Initializes Koin and Database
 */
class MyApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        // Initialize database context (needed for Android)
        initializeDatabase(this)

        // Initialize Koin
        startKoin {
            androidLogger() // Logs Koin operations (useful for debugging)
            androidContext(this@MyApplication)
            modules(appModule)
        }
    }
}
