package com.harsh.myapplication.di

import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.harsh.myapplication.BuildConfigProvider
import com.harsh.myapplication.data.local.AppDatabase
import com.harsh.myapplication.data.local.getDatabaseBuilder
import com.harsh.myapplication.data.remote.GeminiService
import com.harsh.myapplication.data.repository.MemoryRepository
import com.harsh.myapplication.data.repository.PersonRepository
import com.harsh.myapplication.data.speech.SpeechRecognizer
import com.harsh.myapplication.ui.viewmodel.AddMemoryViewModel
import com.harsh.myapplication.ui.viewmodel.MemoryQueryViewModel
import com.harsh.myapplication.ui.viewmodel.PersonDetailViewModel
import com.harsh.myapplication.ui.viewmodel.PersonListViewModel
import io.ktor.client.HttpClient
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import org.koin.dsl.module

val appModule = module {

    // 1. Database
    single<AppDatabase> {
        getDatabaseBuilder()
            .setDriver(BundledSQLiteDriver()) // Critical for Desktop
            .fallbackToDestructiveMigration(true)
            .build()
    }

    // HTTP Client for API calls
    single<HttpClient> {
        HttpClient {
            install(ContentNegotiation) {
                json(Json {
                    ignoreUnknownKeys = true
                    isLenient = true
                })
            }
        }
    }

    // Gemini AI Service
    single<GeminiService> {
        GeminiService(
            httpClient = get(),
            apiKey = BuildConfigProvider.getGeminiApiKey()
        )
    }

    // 2. Repositories
    single<PersonRepository> {
        PersonRepository(database = get())
    }

    single<MemoryRepository> {
        MemoryRepository(database = get())
    }

    single { SpeechRecognizer() } // No arguments needed now!

    // 3. ViewModels (Factory = create new instance every time)

    single {
        PersonListViewModel(
            personRepository = get(),
            memoryRepository = get()
        )
    }

    factory {
        AddMemoryViewModel(
            personRepository = get(),
            memoryRepository = get(),
            geminiService = get(),
            speechRecognizer = get()
        )
    }

    // ADD PersonDetailViewModel factory (with parameter):
    factory { (personId: String) ->
        PersonDetailViewModel(
            personRepository = get(),
            memoryRepository = get(),
            personId = personId
        )
    }

    single {
        MemoryQueryViewModel(
            memoryRepository = get(),
            geminiService = get()
        )
    }
}