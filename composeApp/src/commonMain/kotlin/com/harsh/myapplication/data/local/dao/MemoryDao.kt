package com.harsh.myapplication.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.harsh.myapplication.data.local.entity.MemoryEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for Memory operations
 */
@Dao
interface MemoryDao {

    /**
     * Insert a new memory
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(memory: MemoryEntity)

    /**
     * Insert multiple memories
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(memories: List<MemoryEntity>)

    /**
     * Update existing memory
     */
    @Update
    suspend fun update(memory: MemoryEntity)

    /**
     * Delete a memory
     */
    @Delete
    suspend fun delete(memory: MemoryEntity)

    /**
     * Get all memories as Flow
     * Ordered by creation date (newest first)
     */
    @Query("SELECT * FROM memories ORDER BY createdAt DESC")
    fun getAllMemories(): Flow<List<MemoryEntity>>

    /**
     * Get all memories for a specific person
     */
    @Query("SELECT * FROM memories WHERE personId = :personId ORDER BY createdAt DESC")
    fun getMemoriesForPerson(personId: String): Flow<List<MemoryEntity>>

    /**
     * Get memory by ID
     */
    @Query("SELECT * FROM memories WHERE id = :memoryId")
    suspend fun getMemoryById(memoryId: String): MemoryEntity?

    /**
     * Search memories by content (raw input or summary)
     */
    @Query(
        """
        SELECT * FROM memories 
        WHERE rawInput LIKE '%' || :query || '%' 
           OR aiSummary LIKE '%' || :query || '%'
           OR topic LIKE '%' || :query || '%'
        ORDER BY createdAt DESC
    """
    )
    fun searchMemories(query: String): Flow<List<MemoryEntity>>

    /**
     * Get count of memories for a person
     */
    @Query("SELECT COUNT(*) FROM memories WHERE personId = :personId")
    suspend fun getMemoryCountForPerson(personId: String): Int

    /**
     * Delete all memories for a person
     * (Usually handled by CASCADE, but useful for manual cleanup)
     */
    @Query("DELETE FROM memories WHERE personId = :personId")
    suspend fun deleteMemoriesForPerson(personId: String)
}
