package com.harsh.myapplication.data.model

import com.harsh.myapplication.util.getCurrentTimeMillis
import kotlinx.serialization.Serializable
import kotlin.random.Random

/**
 * Represents a person in the memory system.
 * UPDATED: Uses Real Time for createdAt
 */
@Serializable
data class Person(
    val id: String,
    val name: String,
    val photoUri: String? = null,
    val createdAt: Long = 0L
) {
    companion object {
        fun create(name: String, photoUri: String? = null): Person {
            return Person(
                id = generateId(),
                name = name.trim(),
                photoUri = photoUri,
                createdAt = getCurrentTimeMillis()
            )
        }

        private fun generateId(): String {
            // Simple random ID generation
            val random = Random.nextInt(10000, 99999)
            return "person_$random"
        }
    }

    fun isValid(): Boolean {
        return name.isNotBlank()
    }
}