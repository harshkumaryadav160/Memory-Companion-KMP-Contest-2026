package com.harsh.myapplication.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.harsh.myapplication.data.model.AiAnalysisResult
import com.harsh.myapplication.data.model.Person
import com.harsh.myapplication.data.remote.GeminiService
import com.harsh.myapplication.data.repository.MemoryRepository
import com.harsh.myapplication.data.repository.PersonRepository
import com.harsh.myapplication.data.repository.Result
import com.harsh.myapplication.data.speech.SpeechRecognizer
import com.harsh.myapplication.data.speech.SpeechRecognizerState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel for Add Memory Screen with AI Review
 */
class AddMemoryViewModel(
    private val personRepository: PersonRepository,
    private val memoryRepository: MemoryRepository,
    private val geminiService: GeminiService,
    private val speechRecognizer: SpeechRecognizer
) : ViewModel() {

    private val _persons = MutableStateFlow<List<Person>>(emptyList())
    val persons: StateFlow<List<Person>> = _persons.asStateFlow()

    private val _selectedPerson = MutableStateFlow<Person?>(null)
    val selectedPerson: StateFlow<Person?> = _selectedPerson.asStateFlow()

    private val _memoryText = MutableStateFlow("")
    val memoryText: StateFlow<String> = _memoryText.asStateFlow()

    private val _aiAnalysis = MutableStateFlow<AiAnalysisResult?>(null)
    val aiAnalysis: StateFlow<AiAnalysisResult?> = _aiAnalysis.asStateFlow()

    private val _uiState = MutableStateFlow<AddMemoryUiState>(AddMemoryUiState.Idle)
    val uiState: StateFlow<AddMemoryUiState> = _uiState.asStateFlow()

    private val _isListening = MutableStateFlow(false)
    val isListening: StateFlow<Boolean> = _isListening.asStateFlow()

    init {
        loadPersons()
    }

    private fun loadPersons() {
        viewModelScope.launch {
            personRepository.getAllPersons().collect { result ->
                when (result) {
                    is Result.Success -> {
                        _persons.value = result.data
                        if (result.data.isNotEmpty() && _selectedPerson.value == null) {
                            _selectedPerson.value = result.data.first()
                        } else if (_selectedPerson.value != null) {
                            _selectedPerson.value = result.data.find { it.id == _selectedPerson.value?.id }
                                ?: result.data.firstOrNull()
                        }
                    }
                    is Result.Error -> {
                        _uiState.value = AddMemoryUiState.Error("Failed to load persons")
                    }
                    is Result.Loading -> {}
                }
            }
        }
    }

    // HELPER: Catches Windows/Desktop errors & fixes double prefixes
    private fun getUserFriendlyError(rawMessage: String): String {
        // 1. Recursive Clean: Remove existing "AI analysis failed:" prefix if present
        if (rawMessage.contains("AI analysis failed:", ignoreCase = true)) {
            val inner = rawMessage.replace("AI analysis failed:", "", ignoreCase = true).trim()
            return getUserFriendlyError(inner) // Check the inner message
        }

        return when {
            // Android Network Errors
            rawMessage.contains("Unable to resolve host", ignoreCase = true) -> "No Internet Connection"
            rawMessage.contains("address associated with hostname", ignoreCase = true) -> "No Internet Connection"

            // Desktop (Windows/JVM) Network Errors
            rawMessage.contains("UnknownHost", ignoreCase = true) -> "No Internet Connection"
            rawMessage.contains("No such host is known", ignoreCase = true) -> "No Internet Connection"
            rawMessage.contains("generativelanguage.googleapis.com", ignoreCase = true) -> "No Internet Connection"
            rawMessage.contains("ConnectException", ignoreCase = true) -> "No Internet Connection"
            rawMessage.contains("Failed to connect", ignoreCase = true) -> "No Internet Connection"

            // Common Timeouts & SSL
            rawMessage.contains("timeout", ignoreCase = true) -> "Connection timed out. Please try again."
            rawMessage.contains("SSL", ignoreCase = true) -> "Secure connection failed."

            // Fallback
            else -> rawMessage
        }
    }

    fun selectPersonById(personId: String) {
        val person = _persons.value.find { it.id == personId }
        if (person != null) {
            _selectedPerson.value = person
        }
    }

    fun startVoiceInput() {
        speechRecognizer.startListening { state ->
            when (state) {
                is SpeechRecognizerState.Listening -> _isListening.value = true
                is SpeechRecognizerState.Result -> {
                    val current = _memoryText.value
                    _memoryText.value =
                        if (current.isBlank()) state.text else "$current ${state.text}"
                    _isListening.value = false
                }
                is SpeechRecognizerState.Error -> {
                    _uiState.value = AddMemoryUiState.Error(state.message)
                    _isListening.value = false
                }
                else -> _isListening.value = false
            }
        }
    }

    fun stopVoiceInput() {
        speechRecognizer.stopListening()
        _isListening.value = false
    }

    fun selectPerson(person: Person) {
        _selectedPerson.value = person
    }

    fun updateMemoryText(text: String) {
        _memoryText.value = text
    }

    fun analyzeMemory() {
        val person = _selectedPerson.value
        val text = _memoryText.value.trim()

        if (person == null) {
            _uiState.value = AddMemoryUiState.Error("Please select a person")
            return
        }

        if (text.isBlank()) {
            _uiState.value = AddMemoryUiState.Error("Memory cannot be empty")
            return
        }

        _uiState.value = AddMemoryUiState.AnalyzingWithAI

        viewModelScope.launch {
            when (val result = geminiService.analyzeMemory(text)) {
                is Result.Success -> {
                    _aiAnalysis.value = result.data
                    _uiState.value = AddMemoryUiState.ReviewingAI
                }
                is Result.Error -> {
                    // Fix: Use helper and avoid manual prefix
                    val friendlyMsg = getUserFriendlyError(result.message)
                    _uiState.value = AddMemoryUiState.Error(friendlyMsg)
                }
                is Result.Loading -> {}
            }
        }
    }

    fun saveMemoryWithAI(approvedAnalysis: AiAnalysisResult) {
        val person = _selectedPerson.value ?: return
        val text = _memoryText.value.trim()

        _uiState.value = AddMemoryUiState.Saving

        viewModelScope.launch {
            when (val createResult = memoryRepository.createMemory(person.id, text)) {
                is Result.Success -> {
                    val memory = createResult.data

                    when (val updateResult = memoryRepository.updateMemoryWithAiProcessing(
                        memoryId = memory.id,
                        summary = approvedAnalysis.summary,
                        topic = approvedAnalysis.topic,
                        emotion = approvedAnalysis.emotion,
                        timeReference = approvedAnalysis.timeReference,
                        actionItems = approvedAnalysis.actionItems,
                        keyDetails = approvedAnalysis.keyDetails
                    )) {
                        is Result.Success -> {
                            _uiState.value = AddMemoryUiState.Success
                            _memoryText.value = ""
                            _aiAnalysis.value = null
                        }
                        is Result.Error -> {
                            val msg = getUserFriendlyError(updateResult.message)
                            _uiState.value = AddMemoryUiState.Error("Failed to save: $msg")
                        }
                        is Result.Loading -> {}
                    }
                }
                is Result.Error -> {
                    val msg = getUserFriendlyError(createResult.message)
                    _uiState.value = AddMemoryUiState.Error(msg)
                }
                is Result.Loading -> {}
            }
        }
    }

    fun updateAiAnalysis(updated: AiAnalysisResult) {
        _aiAnalysis.value = updated
    }

    fun discardAiAnalysis() {
        _aiAnalysis.value = null
        _uiState.value = AddMemoryUiState.Idle
    }

    fun resetState() {
        _uiState.value = AddMemoryUiState.Idle
    }
}

sealed class AddMemoryUiState {
    object Idle : AddMemoryUiState()
    object AnalyzingWithAI : AddMemoryUiState()
    object ReviewingAI : AddMemoryUiState()
    object Saving : AddMemoryUiState()
    object Success : AddMemoryUiState()
    data class Error(val message: String) : AddMemoryUiState()
}