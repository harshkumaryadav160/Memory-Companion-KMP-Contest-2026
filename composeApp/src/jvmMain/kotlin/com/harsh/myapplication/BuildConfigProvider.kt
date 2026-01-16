/**
 * Desktop version - you need to manually provide the key
 * (BuildConfig doesn't work on Desktop)
 */
package com.harsh.myapplication

actual object BuildConfigProvider {
    actual fun getGeminiApiKey(): String {
        val key = System.getProperty("GEMINI_API_KEY")

        if (key.isNullOrBlank()) {
            println("⚠️ ERROR: GEMINI_API_KEY not found in System Properties.")
            return ""
        }

        return key
    }
}
