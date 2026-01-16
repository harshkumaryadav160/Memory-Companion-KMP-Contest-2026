package com.harsh.myapplication.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.harsh.myapplication.data.model.Memory
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Entity(
    tableName = "memories",
    foreignKeys = [
        ForeignKey(
            entity = PersonEntity::class,
            parentColumns = ["id"],
            childColumns = ["personId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("personId")]
)
data class MemoryEntity(
    @PrimaryKey
    val id: String,
    val personId: String,
    val rawInput: String,
    val aiSummary: String,
    val topic: String,
    val emotion: String?,
    val timeReference: String?,
    val actionItems: String,
    val keyDetails: String,
    val createdAt: Long,
    val isProcessed: Boolean
) {
    fun toDomain(): Memory {
        return Memory(
            id = id,
            personId = personId,
            rawInput = rawInput,
            aiSummary = aiSummary,
            topic = topic,
            emotion = emotion,
            timeReference = timeReference,
            actionItems = parseStringList(actionItems),
            keyDetails = parseStringList(keyDetails),
            createdAt = createdAt, // Direct pass-through
            isProcessed = isProcessed
        )
    }

    companion object {
        fun fromDomain(memory: Memory): MemoryEntity {
            return MemoryEntity(
                id = memory.id,
                personId = memory.personId,
                rawInput = memory.rawInput,
                aiSummary = memory.aiSummary,
                topic = memory.topic,
                emotion = memory.emotion,
                timeReference = memory.timeReference,
                actionItems = stringifyList(memory.actionItems),
                keyDetails = stringifyList(memory.keyDetails),
                createdAt = memory.createdAt, // Direct pass-through
                isProcessed = memory.isProcessed
            )
        }

        private val json = Json { ignoreUnknownKeys = true }

        private fun stringifyList(list: List<String>): String {
            return try {
                json.encodeToString(list)
            } catch (e: Exception) {
                "[]"
            }
        }

        private fun parseStringList(str: String): List<String> {
            if (str.isBlank()) return emptyList()
            return try {
                json.decodeFromString(str)
            } catch (e: Exception) {
                emptyList()
            }
        }
    }
}