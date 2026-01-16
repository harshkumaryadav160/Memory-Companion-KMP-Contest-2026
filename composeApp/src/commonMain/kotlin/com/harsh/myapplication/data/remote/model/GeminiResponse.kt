package com.harsh.myapplication.data.remote.model

import kotlinx.serialization.Serializable

/**
 * Response model from Gemini API
 */
@Serializable
data class GeminiResponse(
    val candidates: List<Candidate>? = null
)

@Serializable
data class Candidate(
    val content: Content? = null
)

/**
 * Extract text from response
 */
fun GeminiResponse.extractText(): String? {
    return candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
}
