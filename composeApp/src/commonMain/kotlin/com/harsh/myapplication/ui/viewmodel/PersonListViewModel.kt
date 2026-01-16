package com.harsh.myapplication.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.harsh.myapplication.data.model.Person
import com.harsh.myapplication.data.repository.MemoryRepository
import com.harsh.myapplication.data.repository.PersonRepository
import com.harsh.myapplication.data.repository.Result
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

enum class SortOption {
    LATEST,
    ALPHABETICAL,
    MOST_MEMORIES
}

class PersonListViewModel(
    private val personRepository: PersonRepository,
    private val memoryRepository: MemoryRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<PersonListUiState>(PersonListUiState.Loading)
    val uiState: StateFlow<PersonListUiState> = _uiState.asStateFlow()

    private val _memoryCounts = MutableStateFlow<Map<String, Int>>(emptyMap())
    val memoryCounts: StateFlow<Map<String, Int>> = _memoryCounts.asStateFlow()

    private val _sortOption = MutableStateFlow(SortOption.LATEST)
    val sortOption: StateFlow<SortOption> = _sortOption.asStateFlow()

    private val _showAddDialog = MutableStateFlow(false)
    val showAddDialog: StateFlow<Boolean> = _showAddDialog.asStateFlow()

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            combine(
                personRepository.getAllPersons(),
                memoryRepository.getAllMemories(),
                _sortOption
            ) { personResult, memoryResult, sortOption ->
                Triple(personResult, memoryResult, sortOption)
            }.collect { (personResult, memoryResult, sortOption) ->

                if (personResult is Result.Success) {
                    val persons = personResult.data

                    val counts = if (memoryResult is Result.Success) {
                        memoryResult.data.groupingBy { it.personId }.eachCount()
                    } else {
                        emptyMap()
                    }
                    _memoryCounts.value = counts

                    if (persons.isEmpty()) {
                        _uiState.value = PersonListUiState.Empty
                    } else {
                        val sortedList = when (sortOption) {
                            SortOption.LATEST -> persons
                            SortOption.ALPHABETICAL -> persons.sortedBy { it.name.lowercase() }
                            SortOption.MOST_MEMORIES -> persons.sortedByDescending { counts[it.id] ?: 0 }
                        }
                        _uiState.value = PersonListUiState.Success(sortedList)
                    }
                } else if (personResult is Result.Error) {
                    _uiState.value = PersonListUiState.Error(personResult.message)
                }
            }
        }
    }

    fun updateSortOption(option: SortOption) { _sortOption.value = option }
    suspend fun getMemoryCountForPerson(personId: String): Int = memoryCounts.value[personId] ?: 0
    fun showAddPersonDialog() { _showAddDialog.value = true }
    fun hideAddPersonDialog() { _showAddDialog.value = false }
    fun createPerson(name: String) {
        viewModelScope.launch {
            if (personRepository.createPerson(name) is Result.Success) hideAddPersonDialog()
        }
    }
    fun deletePerson(person: Person) {
        viewModelScope.launch { personRepository.deletePerson(person) }
    }
}

sealed class PersonListUiState {
    object Loading : PersonListUiState()
    object Empty : PersonListUiState()
    data class Success(val persons: List<Person>) : PersonListUiState()
    data class Error(val message: String) : PersonListUiState()
}