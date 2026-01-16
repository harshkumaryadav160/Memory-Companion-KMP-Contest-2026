package com.harsh.myapplication.data.model

import com.harsh.myapplication.util.getCurrentTimeMillis
import kotlinx.serialization.Serializable
import kotlin.random.Random

/**
 * Represents a memory about a person.
 * UPDATED: Uses Real Time for createdAt
 */
@Serializable
data class Memory(
    val id: String,
    val personId: String,
    val rawInput: String,
    val aiSummary: String = "",
    val topic: String = "",
    val emotion: String? = null,
    val timeReference: String? = null,
    val actionItems: List<String> = emptyList(),
    val keyDetails: List<String> = emptyList(),
    val createdAt: Long = 0L,
    val isProcessed: Boolean = false
) {
    companion object {
        fun createUnprocessed(
            personId: String,
            rawInput: String
        ): Memory {
            return Memory(
                id = generateId(),
                personId = personId,
                rawInput = rawInput.trim(),
                isProcessed = false,
                createdAt = getCurrentTimeMillis()
            )
        }

        private fun generateId(): String {
            val random = Random.nextInt(10000, 99999)
            return "memory_$random"
        }
    }

    fun withAiProcessing(
        summary: String,
        topic: String,
        emotion: String?,
        timeReference: String?,
        actionItems: List<String>,
        keyDetails: List<String>
    ): Memory {
        return copy(
            aiSummary = summary,
            topic = topic,
            emotion = emotion,
            timeReference = timeReference,
            actionItems = actionItems,
            keyDetails = keyDetails,
            isProcessed = true
        )
    }

    fun isValid(): Boolean {
        return rawInput.isNotBlank() && personId.isNotBlank()
    }
}