package com.harsh.myapplication.data.repository

import com.harsh.myapplication.data.local.AppDatabase
import com.harsh.myapplication.data.local.entity.MemoryEntity
import com.harsh.myapplication.data.model.Memory
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

/**
 * Repository for Memory operations
 * Handles all business logic related to memories
 */
class MemoryRepository(
    private val database: AppDatabase
) {
    private val memoryDao = database.memoryDao()
    private val personDao = database.personDao()

    /**
     * Get all memories as Flow
     * Automatically converts entities to domain models
     */
    fun getAllMemories(): Flow<Result<List<Memory>>> {
        return memoryDao.getAllMemories()
            .map { entities ->
                Result.Success(entities.map { it.toDomain() }) as Result<List<Memory>>
            }
            .catch { exception ->
                emit(Result.Error("Failed to load memories", exception as? Exception))
            }
    }

    /**
     * Get all memories for a specific person
     */
    fun getMemoriesForPerson(personId: String): Flow<Result<List<Memory>>> {
        return memoryDao.getMemoriesForPerson(personId)
            .map { entities ->
                Result.Success(entities.map { it.toDomain() }) as Result<List<Memory>>
            }
            .catch { exception ->
                emit(Result.Error("Failed to load memories", exception as? Exception))
            }
    }

    /**
     * Get memory by ID
     */
    suspend fun getMemoryById(memoryId: String): Result<Memory?> {
        return try {
            val entity = memoryDao.getMemoryById(memoryId)
            Result.Success(entity?.toDomain())
        } catch (e: Exception) {
            Result.Error("Failed to load memory", e)
        }
    }

    /**
     * Create a new unprocessed memory
     * This is called when user first types a memory
     */
    suspend fun createMemory(personId: String, rawInput: String): Result<Memory> {
        return try {
            // Validate input
            if (rawInput.isBlank()) {
                return Result.Error("Memory content cannot be empty")
            }

            // Check if person exists
            val personExists = personDao.getPersonById(personId) != null
            if (!personExists) {
                return Result.Error("Person not found")
            }

            // Create unprocessed memory
            val memory = Memory.createUnprocessed(
                personId = personId,
                rawInput = rawInput
            )

            // Validate
            if (!memory.isValid()) {
                return Result.Error("Invalid memory data")
            }

            // Convert and save
            val entity = MemoryEntity.fromDomain(memory)
            memoryDao.insert(entity)

            Result.Success(memory)
        } catch (e: Exception) {
            Result.Error("Failed to create memory", e)
        }
    }

    /**
     * Update memory with AI processing results
     * This is called after AI analyzes the memory
     */
    suspend fun updateMemoryWithAiProcessing(
        memoryId: String,
        summary: String,
        topic: String,
        emotion: String?,
        timeReference: String?,
        actionItems: List<String>,
        keyDetails: List<String>
    ): Result<Memory> {
        return try {
            // Get existing memory
            val existingEntity = memoryDao.getMemoryById(memoryId)
            if (existingEntity == null) {
                return Result.Error("Memory not found")
            }

            // Convert to domain model
            val existingMemory = existingEntity.toDomain()

            // Apply AI processing
            val updatedMemory = existingMemory.withAiProcessing(
                summary = summary,
                topic = topic,
                emotion = emotion,
                timeReference = timeReference,
                actionItems = actionItems,
                keyDetails = keyDetails
            )

            // Save updated memory
            val updatedEntity = MemoryEntity.fromDomain(updatedMemory)
            memoryDao.update(updatedEntity)

            Result.Success(updatedMemory)
        } catch (e: Exception) {
            Result.Error("Failed to update memory", e)
        }
    }

    /**
     * Update existing memory (general update)
     */
    suspend fun updateMemory(memory: Memory): Result<Unit> {
        return try {
            // Validate
            if (!memory.isValid()) {
                return Result.Error("Invalid memory data")
            }

            // Convert and update
            val entity = MemoryEntity.fromDomain(memory)
            memoryDao.update(entity)

            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error("Failed to update memory", e)
        }
    }

    /**
     * Delete a memory
     */
    suspend fun deleteMemory(memory: Memory): Result<Unit> {
        return try {
            val entity = MemoryEntity.fromDomain(memory)
            memoryDao.delete(entity)
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error("Failed to delete memory", e)
        }
    }

    /**
     * Delete memory by ID
     */
    suspend fun deleteMemoryById(memoryId: String): Result<Unit> {
        return try {
            val entity = memoryDao.getMemoryById(memoryId)
            if (entity != null) {
                memoryDao.delete(entity)
                Result.Success(Unit)
            } else {
                Result.Error("Memory not found")
            }
        } catch (e: Exception) {
            Result.Error("Failed to delete memory", e)
        }
    }

    /**
     * Search memories by content
     * Searches in rawInput, aiSummary, and topic
     */
    fun searchMemories(query: String): Flow<Result<List<Memory>>> {
        return memoryDao.searchMemories(query)
            .map { entities ->
                Result.Success(entities.map { it.toDomain() }) as Result<List<Memory>>
            }
            .catch { exception ->
                emit(Result.Error("Failed to search memories", exception as? Exception))
            }
    }

    /**
     * Get memory count for a person
     */
    suspend fun getMemoryCountForPerson(personId: String): Result<Int> {
        return try {
            val count = memoryDao.getMemoryCountForPerson(personId)
            Result.Success(count)
        } catch (e: Exception) {
            Result.Error("Failed to get memory count", e)
        }
    }

    /**
     * Delete all memories for a person
     * Usually handled by CASCADE, but useful for manual cleanup
     */
    suspend fun deleteAllMemoriesForPerson(personId: String): Result<Unit> {
        return try {
            memoryDao.deleteMemoriesForPerson(personId)
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error("Failed to delete memories", e)
        }
    }

    /**
     * Get all processed memories (ones that have AI analysis)
     */
    fun getProcessedMemories(): Flow<Result<List<Memory>>> {
        return memoryDao.getAllMemories()
            .map { entities ->
                val processedMemories = entities
                    .map { it.toDomain() }
                    .filter { it.isProcessed }
                Result.Success(processedMemories) as Result<List<Memory>>
            }
            .catch { exception ->
                emit(Result.Error("Failed to load memories", exception as? Exception))
            }
    }

    /**
     * Get all unprocessed memories (waiting for AI)
     */
    fun getUnprocessedMemories(): Flow<Result<List<Memory>>> {
        return memoryDao.getAllMemories()
            .map { entities ->
                val unprocessedMemories = entities
                    .map { it.toDomain() }
                    .filter { !it.isProcessed }
                Result.Success(unprocessedMemories) as Result<List<Memory>>
            }
            .catch { exception ->
                emit(Result.Error("Failed to load memories", exception as? Exception))
            }
    }
}
