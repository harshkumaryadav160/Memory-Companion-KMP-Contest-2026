package com.harsh.myapplication.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.harsh.myapplication.data.local.entity.PersonEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for Person operations
 */
@Dao
interface PersonDao {

    /**
     * Insert a new person
     * OnConflictStrategy.REPLACE: If person exists, replace it
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(person: PersonEntity)

    /**
     * Insert multiple persons
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(persons: List<PersonEntity>)

    /**
     * Update existing person
     */
    @Update
    suspend fun update(person: PersonEntity)

    /**
     * Delete a person
     */
    @Delete
    suspend fun delete(person: PersonEntity)

    /**
     * Get all persons as Flow (reactive - UI updates automatically)
     * Ordered by creation date (newest first)
     */
    @Query("SELECT * FROM persons ORDER BY createdAt DESC")
    fun getAllPersons(): Flow<List<PersonEntity>>

    /**
     * Get person by ID
     */
    @Query("SELECT * FROM persons WHERE id = :personId")
    suspend fun getPersonById(personId: String): PersonEntity?

    /**
     * Search persons by name
     */
    @Query("SELECT * FROM persons WHERE name LIKE '%' || :query || '%' ORDER BY createdAt DESC")
    fun searchPersons(query: String): Flow<List<PersonEntity>>

    /**
     * Get count of all persons
     */
    @Query("SELECT COUNT(*) FROM persons")
    suspend fun getPersonCount(): Int
}
