// ============================================
// FILE 3: AiAnalysisResult.kt
// ============================================
// Location: composeApp/src/commonMain/kotlin/com/harsh/myapplication/data/model/AiAnalysisResult.kt

package com.harsh.myapplication.data.model

import kotlinx.serialization.Serializable

/**
 * Represents the result of AI analysis on a memory.
 * This is what the Gemini API will return.
 */
@Serializable
data class AiAnalysisResult(
    val topic: String = "",
    val emotion: String? = null,
    val timeReference: String? = null,
    val actionItems: List<String> = emptyList(),
    val keyDetails: List<String> = emptyList(),
    val summary: String = ""
) {
    /**
     * Check if analysis contains meaningful data
     */
    fun isEmpty(): Boolean {
        return topic.isBlank() &&
                emotion.isNullOrBlank() &&
                actionItems.isEmpty() &&
                keyDetails.isEmpty()
    }
}
