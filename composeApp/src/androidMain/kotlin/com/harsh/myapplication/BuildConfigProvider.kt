package com.harsh.myapplication

/**
 * Platform-specific way to access BuildConfig
 */
actual object BuildConfigProvider {
    actual fun getGeminiApiKey(): String {
        return BuildConfig.GEMINI_API_KEY
    }
}
