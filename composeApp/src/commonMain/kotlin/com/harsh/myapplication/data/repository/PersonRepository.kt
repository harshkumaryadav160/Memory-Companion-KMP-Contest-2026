package com.harsh.myapplication.data.repository

import com.harsh.myapplication.data.local.AppDatabase
import com.harsh.myapplication.data.local.entity.PersonEntity
import com.harsh.myapplication.data.model.Person
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

/**
 * Repository for Person operations
 * Handles all business logic related to persons
 */
class PersonRepository(
    private val database: AppDatabase
) {
    private val personDao = database.personDao()

    /**
     * Get all persons as Flow
     * Automatically converts entities to domain models
     * UI will update automatically when data changes
     */
    fun getAllPersons(): Flow<Result<List<Person>>> {
        return personDao.getAllPersons()
            .map { entities ->
                Result.Success(entities.map { it.toDomain() }) as Result<List<Person>>
            }
            .catch { exception ->
                emit(Result.Error("Failed to load persons", exception as? Exception))
            }
    }

    /**
     * Get person by ID
     * Returns null if not found
     */
    suspend fun getPersonById(personId: String): Result<Person?> {
        return try {
            val entity = personDao.getPersonById(personId)
            Result.Success(entity?.toDomain())
        } catch (e: Exception) {
            Result.Error("Failed to load person", e)
        }
    }

    /**
     * Create a new person
     */
    suspend fun createPerson(name: String, photoUri: String? = null): Result<Person> {
        return try {
            // Validate input
            if (name.isBlank()) {
                return Result.Error("Name cannot be empty")
            }

            // Create domain model
            val person = Person.create(name = name, photoUri = photoUri)

            // Validate
            if (!person.isValid()) {
                return Result.Error("Invalid person data")
            }

            // Convert to entity and save
            val entity = PersonEntity.fromDomain(person)
            personDao.insert(entity)

            Result.Success(person)
        } catch (e: Exception) {
            Result.Error("Failed to create person", e)
        }
    }

    /**
     * Update existing person
     */
    suspend fun updatePerson(person: Person): Result<Unit> {
        return try {
            // Validate
            if (!person.isValid()) {
                return Result.Error("Invalid person data")
            }

            // Convert and update
            val entity = PersonEntity.fromDomain(person)
            personDao.update(entity)

            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error("Failed to update person", e)
        }
    }

    /**
     * Delete a person
     * This will also delete all their memories (CASCADE)
     */
    suspend fun deletePerson(person: Person): Result<Unit> {
        return try {
            val entity = PersonEntity.fromDomain(person)
            personDao.delete(entity)
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error("Failed to delete person", e)
        }
    }

    /**
     * Delete person by ID
     */
    suspend fun deletePersonById(personId: String): Result<Unit> {
        return try {
            val entity = personDao.getPersonById(personId)
            if (entity != null) {
                personDao.delete(entity)
                Result.Success(Unit)
            } else {
                Result.Error("Person not found")
            }
        } catch (e: Exception) {
            Result.Error("Failed to delete person", e)
        }
    }

    /**
     * Search persons by name
     */
    fun searchPersons(query: String): Flow<Result<List<Person>>> {
        return personDao.searchPersons(query)
            .map { entities ->
                Result.Success(entities.map { it.toDomain() }) as Result<List<Person>>
            }
            .catch { exception ->
                emit(Result.Error("Failed to search persons", exception as? Exception))
            }
    }

    /**
     * Get total count of persons
     */
    suspend fun getPersonCount(): Result<Int> {
        return try {
            val count = personDao.getPersonCount()
            Result.Success(count)
        } catch (e: Exception) {
            Result.Error("Failed to get person count", e)
        }
    }

    /**
     * Check if a person exists
     */
    suspend fun personExists(personId: String): Boolean {
        return try {
            personDao.getPersonById(personId) != null
        } catch (e: Exception) {
            false
        }
    }
}
