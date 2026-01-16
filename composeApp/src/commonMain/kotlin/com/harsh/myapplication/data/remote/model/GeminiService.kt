package com.harsh.myapplication.data.remote

import com.harsh.myapplication.data.model.AiAnalysisResult
import com.harsh.myapplication.data.model.Memory
import com.harsh.myapplication.data.remote.model.Content
import com.harsh.myapplication.data.remote.model.GeminiRequest
import com.harsh.myapplication.data.remote.model.GeminiResponse
import com.harsh.myapplication.data.remote.model.Part
import com.harsh.myapplication.data.remote.model.extractText
import com.harsh.myapplication.data.repository.Result
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.serialization.json.Json

/**
 * Service for Gemini AI operations
 */
class GeminiService(
    private val httpClient: HttpClient,
    private val apiKey: String
) {
    private val baseUrl = "https://generativelanguage.googleapis.com/v1beta"

    // OLD: private val model = "gemini-1.5-flash"
    // NEW: Use the specific version that is guaranteed to exist
    private val model = "gemini-2.5-flash"

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    /**
     * Analyze a memory and extract structured information
     */
    suspend fun analyzeMemory(memoryText: String): Result<AiAnalysisResult> {
        return try {
            val prompt = buildAnalysisPrompt(memoryText)
            val response = callGemini(prompt)
            val analysisText = response ?: return Result.Error("Empty response from AI")

            // Parse the JSON response from AI
            val analysis = parseAnalysisResponse(analysisText)
            Result.Success(analysis)
        } catch (e: Exception) {
            Result.Error("AI analysis failed: ${e.message}", e as? Exception)
        }
    }

    /**
     * Query memories using natural language
     */
    suspend fun queryMemories(
        question: String,
        memories: List<Memory>
    ): Result<String> {
        return try {
            val prompt = buildQueryPrompt(question, memories)
            val response = callGemini(prompt)

            if (response.isNullOrBlank()) {
                Result.Error("No response from AI")
            } else {
                Result.Success(response)
            }
        } catch (e: Exception) {
            Result.Error("Query failed: ${e.message}", e as? Exception)
        }
    }

//    /**
//     * Call Gemini API
//     */
//    private suspend fun callGemini(prompt: String): String? {
//        val request = GeminiRequest(
//            contents = listOf(
//                Content(
//                    parts = listOf(Part(text = prompt))
//                )
//            )
//        )
//
//        val response: HttpResponse = httpClient.post(
//            "$baseUrl/models/$model:generateContent?key=$apiKey"
//        ) {
//            contentType(ContentType.Application.Json)
//            setBody(request)
//        }
//
//        val geminiResponse: GeminiResponse = response.body()
//        return geminiResponse.extractText()
//    }

    /**
     * Call Gemini API
     */
    private suspend fun callGemini(prompt: String): String? {
        val request = GeminiRequest(
            contents = listOf(
                Content(
                    parts = listOf(Part(text = prompt))
                )
            )
        )

        val response: HttpResponse = httpClient.post(
            "$baseUrl/models/$model:generateContent?key=$apiKey"
        ) {
            contentType(ContentType.Application.Json)
            setBody(request)
        }

        // Read the raw text first to debug
        val responseBody = response.bodyAsText()

        //  println("🤖 AI RAW RESPONSE: $responseBody") // <--- This will show the real error in Logcat

        if (response.status.value != 200) {
            //  println("🤖 AI ERROR STATUS: ${response.status}")
            return null
        }

        return try {
            val geminiResponse = json.decodeFromString<GeminiResponse>(responseBody)
            geminiResponse.extractText()
        } catch (e: Exception) {
            //  println("🤖 JSON PARSING ERROR: ${e.message}")
            null
        }
    }

    /**
     * Build prompt for memory analysis
     */
    private fun buildAnalysisPrompt(memoryText: String): String {
        return """
Analyze this memory and extract information in JSON format.

Memory: "$memoryText"

Respond ONLY with valid JSON (no markdown, no explanation):
{
  "topic": "main topic in 2-3 words",
  "emotion": "emotional tone (happy/sad/neutral/stressed/excited/etc) or null",
  "timeReference": "any date/time mentioned or null",
  "actionItems": ["list of any promises or tasks mentioned"],
  "keyDetails": ["list of 2-3 most important facts"],
  "summary": "one sentence summary"
}

Rules:
- Keep topic very brief (max 3 words)
- Only include emotion if clearly expressed
- actionItems and keyDetails should be arrays (can be empty)
- Make summary concise and clear
        """.trimIndent()
    }

    /**
     * Build prompt for memory query
     */
    private fun buildQueryPrompt(question: String, memories: List<Memory>): String {
        val memoryContext = memories.joinToString("\n\n") { memory ->
            """
Memory ${memories.indexOf(memory) + 1}:
Text: ${memory.rawInput}
Topic: ${memory.topic.ifBlank { "unknown" }}
Emotion: ${memory.emotion ?: "none"}
Time: ${memory.timeReference ?: "unspecified"}
            """.trimIndent()
        }

        return """
You are a memory assistant. Answer the user's question based ONLY on these memories.

Memories:
$memoryContext

User Question: "$question"

Instructions:
- Answer naturally and conversationally
- Only use information from the memories provided
- If you're uncertain, say "I might be mistaken, but..."
- If the answer isn't in the memories, say "I don't have information about that in your memories"
- Keep response concise (2-3 sentences max)
        """.trimIndent()
    }

    /**
     * Parse AI analysis response
     */
    private fun parseAnalysisResponse(responseText: String): AiAnalysisResult {
        return try {
            // Clean response (remove markdown code blocks if present)
            val cleanJson = responseText
                .replace("```json", "")
                .replace("```", "")
                .trim()

            json.decodeFromString<AiAnalysisResult>(cleanJson)
        } catch (e: Exception) {
            // If parsing fails, return empty result
            println("Failed to parse AI response: $responseText")
            AiAnalysisResult()
        }
    }

    /**
     * DEBUG: List all available models
     */
    suspend fun listModels() {
        try {
            val response: HttpResponse = httpClient.get(
                "$baseUrl/models?key=$apiKey"
            )
            val body = response.bodyAsText()
            println("🤖 AVAILABLE MODELS: $body")
        } catch (e: Exception) {
            println("🤖 ERROR LISTING MODELS: ${e.message}")
        }
    }

}
