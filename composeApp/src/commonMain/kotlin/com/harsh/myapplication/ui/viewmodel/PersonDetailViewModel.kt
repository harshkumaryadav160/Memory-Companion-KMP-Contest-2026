package com.harsh.myapplication.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.harsh.myapplication.data.model.Memory
import com.harsh.myapplication.data.model.Person
import com.harsh.myapplication.data.repository.MemoryRepository
import com.harsh.myapplication.data.repository.PersonRepository
import com.harsh.myapplication.data.repository.Result
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel for Person Detail Screen
 */
class PersonDetailViewModel(
    private val personRepository: PersonRepository,
    private val memoryRepository: MemoryRepository,
    private val personId: String
) : ViewModel() {

    // Current person
    private val _person = MutableStateFlow<Person?>(null)
    val person: StateFlow<Person?> = _person.asStateFlow()

    // Memories for this person
    private val _memories = MutableStateFlow<List<Memory>>(emptyList())
    val memories: StateFlow<List<Memory>> = _memories.asStateFlow()

    // UI state
    private val _uiState = MutableStateFlow<PersonDetailUiState>(PersonDetailUiState.Loading)
    val uiState: StateFlow<PersonDetailUiState> = _uiState.asStateFlow()

    init {
        loadPersonAndMemories()
    }

    /**
     * Load person details and their memories
     */
    private fun loadPersonAndMemories() {
        viewModelScope.launch {
            // Load person
            when (val personResult = personRepository.getPersonById(personId)) {
                is Result.Success -> {
                    _person.value = personResult.data
                }

                is Result.Error -> {
                    _uiState.value = PersonDetailUiState.Error("Failed to load person")
                    return@launch
                }

                is Result.Loading -> { /* No action */
                }
            }

            // Load memories (reactive - updates automatically)
            memoryRepository.getMemoriesForPerson(personId).collect { result ->
                when (result) {
                    is Result.Success -> {
                        _memories.value = result.data
                        _uiState.value = if (result.data.isEmpty()) {
                            PersonDetailUiState.Empty
                        } else {
                            PersonDetailUiState.Success
                        }
                    }

                    is Result.Error -> {
                        _uiState.value = PersonDetailUiState.Error(result.message)
                    }

                    is Result.Loading -> { /* No action */
                    }
                }
            }
        }
    }

    /**
     * Delete a memory
     */
    fun deleteMemory(memory: Memory) {
        viewModelScope.launch {
            memoryRepository.deleteMemory(memory)
            // List will update automatically via Flow
        }
    }
}

/**
 * UI State for Person Detail Screen
 */
sealed class PersonDetailUiState {
    object Loading : PersonDetailUiState()
    object Empty : PersonDetailUiState()
    object Success : PersonDetailUiState()
    data class Error(val message: String) : PersonDetailUiState()
}
