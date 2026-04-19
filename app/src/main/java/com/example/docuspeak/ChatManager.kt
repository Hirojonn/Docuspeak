package com.example.docuspeak

import android.util.Log
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

class ChatManager {

    companion object {
        private val G4F_BASE_URL = BuildConfig.G4F_BASE_URL
        private val USE_GPT4FREE = BuildConfig.USE_GPT4FREE
        
        private const val POLLINATIONS_URL = "https://text.pollinations.ai/"
        
        private const val USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
        private const val MAX_CONTEXT_CHARS = 8000
    }

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    private val gson = Gson()
    private val messageHistory = mutableListOf<Map<String, String>>()
    private var pdfContext: String = ""

    fun loadPdfContext(text: String) {
        val truncated = if (text.length > MAX_CONTEXT_CHARS) {
            text.substring(0, MAX_CONTEXT_CHARS).takeWhile { it != '\u0000' } + "..."
        } else {
            text
        }
        pdfContext = "You are an AI assistant for DocuSpeak. Use this PDF context to answer:\n\n$truncated\n\nBe concise."
        messageHistory.clear()
        messageHistory.add(mapOf("role" to "system", "content" to pdfContext))
    }

    suspend fun chat(userMessage: String): String = withContext(Dispatchers.IO) {
        messageHistory.add(mapOf("role" to "user", "content" to userMessage))

        val errors = mutableListOf<String>()

        // Strategy 1: User-hosted G4F Server
        if (G4F_BASE_URL.isNotEmpty()) {
            val res = attemptOpenAiStyle(G4F_BASE_URL, "gpt-4", "Bearer random")
            if (!res.startsWith("ERROR:")) return@withContext res
            errors.add("Local G4F: $res")
        }

        // Strategy 2: Built-in Free Mode (Pollinations AI) - The most stable option
        if (USE_GPT4FREE) {
            val res = attemptPollinations()
            if (!res.startsWith("ERROR:")) return@withContext res
            errors.add("Free Mode (Pollinations): $res")
        }

        return@withContext "AI currently unavailable. Check your internet connection.\nDetails:\n${errors.joinToString("\n")}"
    }

    /**
     * Pollinations AI is our new primary "Free AI" strategy. 
     * It is stable, doesn't require keys, and doesn't use complex handshakes.
     */
    private fun attemptPollinations(): String {
        val requestBody = mapOf(
            "messages" to messageHistory,
            // "openai" model on Pollinations maps to gpt-4o-mini
            "model" to "openai",
            "stream" to false
        )

        val request = Request.Builder()
            .url(POLLINATIONS_URL)
            .addHeader("Content-Type", "application/json")
            .addHeader("User-Agent", USER_AGENT)
            .post(gson.toJson(requestBody).toRequestBody("application/json".toMediaType()))
            .build()

        return try {
            client.newCall(request).execute().use { response ->
                val text = response.body?.string() ?: ""
                
                if (!response.isSuccessful) {
                    return "ERROR: HTTP ${response.code}"
                }

                if (text.isNotEmpty()) {
                    messageHistory.add(mapOf("role" to "assistant", "content" to text))
                    return text
                }
                return "ERROR: Empty response"
            }
        } catch (e: Exception) {
            "ERROR: ${e.message}"
        }
    }

    private fun attemptOpenAiStyle(url: String, model: String, auth: String): String {
        val requestBody = mapOf("model" to model, "messages" to messageHistory, "stream" to false)
        val request = Request.Builder()
            .url(url)
            .addHeader("Authorization", auth)
            .addHeader("Content-Type", "application/json")
            .addHeader("User-Agent", USER_AGENT)
            .post(gson.toJson(requestBody).toRequestBody("application/json".toMediaType()))
            .build()
        return try {
            client.newCall(request).execute().use { response ->
                val text = response.body?.string() ?: ""
                if (!response.isSuccessful) return "ERROR: ${response.code}"
                
                // For OpenAI style, we need to parse the JSON choices
                val apiRes = gson.fromJson(text, OpenAiResponse::class.java)
                val content = apiRes.choices?.firstOrNull()?.message?.get("content") ?: "No response"
                messageHistory.add(mapOf("role" to "assistant", "content" to content))
                content
            }
        } catch (e: Exception) { "ERROR: ${e.message}" }
    }

    fun clearHistory() {
        messageHistory.clear()
        if (pdfContext.isNotEmpty()) {
            messageHistory.add(mapOf("role" to "system", "content" to pdfContext))
        }
    }

    private data class OpenAiResponse(val choices: List<Choice>?)
    private data class Choice(val message: Map<String, String>?)
}
