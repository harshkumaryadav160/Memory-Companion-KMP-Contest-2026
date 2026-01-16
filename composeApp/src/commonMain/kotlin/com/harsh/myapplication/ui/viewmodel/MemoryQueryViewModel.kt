package com.harsh.myapplication.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.harsh.myapplication.data.model.Memory
import com.harsh.myapplication.data.remote.GeminiService
import com.harsh.myapplication.data.repository.MemoryRepository
import com.harsh.myapplication.data.repository.Result
import com.harsh.myapplication.ui.components.ChatMessageData
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

class MemoryQueryViewModel(
    private val memoryRepository: MemoryRepository,
    private val geminiService: GeminiService
) : ViewModel() {

    private val _messages = MutableStateFlow<List<ChatMessageData>>(emptyList())
    val messages: StateFlow<List<ChatMessageData>> = _messages.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private var allMemories: List<Memory> = emptyList()

    init {
        loadAllMemories()
    }

    private fun loadAllMemories() {
        viewModelScope.launch {
            memoryRepository.getAllMemories().collect { result ->
                when (result) {
                    is Result.Success -> {
                        // Only use processed memories for context
                        allMemories = result.data.filter { it.isProcessed }
                    }

                    is Result.Error -> {
                        _error.value = "Failed to load memories: ${result.exception?.message}"
                    }

                    is Result.Loading -> {}
                }
            }
        }
    }

    fun sendQuery(query: String) {
        if (query.isBlank()) return

        // Add user message
        val userMessage = ChatMessageData(
            id = UUID.randomUUID().toString(),
            text = query,
            isUser = true
        )
        _messages.value = _messages.value + userMessage
        _isLoading.value = true
        _error.value = null

        viewModelScope.launch {
            try {
                // Call Gemini AI
                val result = geminiService.queryMemories(
                    question = query,
                    memories = allMemories
                )

                when (result) {
                    is Result.Success -> {
                        val response = result.data
                        val aiMessage = ChatMessageData(
                            id = UUID.randomUUID().toString(),
                            text = response,
                            isUser = false,
                            sourceMemories = findRelevantMemories(query, response)
                        )
                        _messages.value = _messages.value + aiMessage
                    }

                    is Result.Error -> {
                        _error.value = "AI Error: ${result.message}"
                        addErrorMessage()
                    }

                    else -> {}
                }
            } catch (e: Exception) {
                _error.value = "Failed: ${e.message}"
                addErrorMessage()
            } finally {
                _isLoading.value = false
            }
        }
    }

    private fun addErrorMessage() {
        val errorMessage = ChatMessageData(
            id = UUID.randomUUID().toString(),
            text = "Sorry, I couldn't process your question.",
            isUser = false
        )
        _messages.value = _messages.value + errorMessage
    }

    private fun findRelevantMemories(query: String, response: String): List<Memory> {
        val queryWords = query.lowercase().split(" ").filter { it.length > 3 }
        return allMemories.filter { memory ->
            val memoryText = "${memory.rawInput} ${memory.aiSummary} ${memory.topic}".lowercase()
            queryWords.any { word -> memoryText.contains(word) }
        }.take(3)
    }

    fun clearError() {
        _error.value = null
    }

    fun clearChat() {
        _messages.value = emptyList()
    }
}