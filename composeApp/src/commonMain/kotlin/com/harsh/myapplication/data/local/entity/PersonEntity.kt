package com.harsh.myapplication.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.harsh.myapplication.data.model.Person

@Entity(tableName = "persons")
data class PersonEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val photoUri: String?,
    val createdAt: Long
) {
    fun toDomain(): Person {
        return Person(
            id = id,
            name = name,
            photoUri = photoUri,
            createdAt = createdAt // Direct pass-through
        )
    }

    companion object {
        fun fromDomain(person: Person): PersonEntity {
            return PersonEntity(
                id = person.id,
                name = person.name,
                photoUri = person.photoUri,
                createdAt = person.createdAt
            )
        }
    }
}