package com.harsh.myapplication

/**
 * Expect/Actual pattern for platform-specific BuildConfig access
 */
expect object BuildConfigProvider {
    fun getGeminiApiKey(): String
}
