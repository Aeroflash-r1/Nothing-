package com.nothing.assistant.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.concurrent.TimeUnit

/**
 * Lightweight HTTP client for the Gemini API.
 *
 * Supports both streaming and non-streaming requests.
 * All network calls are designed to be cancelled via structured concurrency.
 */
class GeminiClient(private val apiKey: String) {

    private val client = OkHttpClient.Builder()
        .connectTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .readTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .writeTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .build()

    private val jsonMediaType = "application/json".toMediaType()

    /**
     * Send a non-streaming request to Gemini and return the full response text.
     *
     * @param model Model ID (e.g. "gemini-2.5-flash-lite")
     * @param messages List of (role, text) pairs representing the conversation history
     * @param temperature Model temperature (0.0–1.0)
     * @param maxOutputTokens Maximum tokens in the response
     * @return The generated text response
     * @throws GeminiException on API errors, network failures, or timeouts
     */
    suspend fun generateContent(
        model: String,
        messages: List<Pair<String, String>>,
        temperature: Float = 0.7f,
        maxOutputTokens: Int = 512,
    ): String = withContext(Dispatchers.IO) {
        val requestBody = buildRequestBody(messages, temperature, maxOutputTokens)
        val request = Request.Builder()
            .url("$BASE_URL/models/$model:generateContent")
            .header("x-goog-api-key", apiKey)
            .header("Content-Type", "application/json")
            .post(requestBody.toRequestBody(jsonMediaType))
            .build()

        val response = client.newCall(request).execute()
        val bodyString = response.body?.string() ?: ""

        if (!response.isSuccessful) {
            throw parseError(bodyString, response.code)
        }

        parseResponseBody(bodyString)
    }

    /**
     * Send a streaming request and call [onToken] for each text chunk as it arrives.
     *
     * Reads the SSE stream line-by-line to avoid splitting events across chunks.
     *
     * @param model Model ID
     * @param messages Conversation history as (role, text) pairs
     * @param onToken Called on each partial text token
     * @param temperature Model temperature
     * @param maxOutputTokens Maximum output tokens
     * @throws GeminiException on API errors
     */
    suspend fun generateContentStreaming(
        model: String,
        messages: List<Pair<String, String>>,
        onToken: (String) -> Unit,
        temperature: Float = 0.7f,
        maxOutputTokens: Int = 512,
    ): Unit = withContext(Dispatchers.IO) {
        val requestBody = buildRequestBody(messages, temperature, maxOutputTokens)
        val request = Request.Builder()
            .url("$BASE_URL/models/$model:streamGenerateContent?alt=sse")
            .header("x-goog-api-key", apiKey)
            .header("Content-Type", "application/json")
            .post(requestBody.toRequestBody(jsonMediaType))
            .build()

        val response = client.newCall(request).execute()
        val body = response.body ?: throw GeminiException("Empty response body", response.code)

        if (!response.isSuccessful) {
            val errorBody = body.string()
            throw parseError(errorBody, response.code)
        }

        // Parse SSE stream line-by-line to handle any chunk boundaries correctly
        BufferedReader(InputStreamReader(body.byteStream())).use { reader ->
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                val currentLine = line ?: continue
                if (currentLine.startsWith("data: ")) {
                    val jsonStr = currentLine.removePrefix("data: ")
                    if (jsonStr == "[DONE]") break
                    try {
                        val text = parseStreamChunkText(jsonStr)
                        if (text.isNotEmpty()) {
                            onToken(text)
                        }
                    } catch (_: Exception) {
                        // Skip malformed chunks
                    }
                }
            }
        }
    }

    /** Validate an API key by making a lightweight test call. */
    suspend fun validateApiKey(): Boolean = withContext(Dispatchers.IO) {
        try {
            val testBody = JSONObject().apply {
                put("contents", JSONArray().put(JSONObject().apply {
                    put("role", "user")
                    put("parts", JSONArray().put(JSONObject().apply {
                        put("text", "Hello")
                    }))
                }))
                put("generationConfig", JSONObject().apply {
                    put("maxOutputTokens", 10)
                })
            }

            val request = Request.Builder()
                .url("$BASE_URL/models/gemini-2.5-flash-lite:generateContent")
                .header("x-goog-api-key", apiKey)
                .header("Content-Type", "application/json")
                .post(testBody.toString().toRequestBody(jsonMediaType))
                .build()

            val response = client.newCall(request).execute()
            response.isSuccessful
        } catch (_: Exception) {
            false
        }
    }

    // ─── Private helpers ───

    private fun buildRequestBody(
        messages: List<Pair<String, String>>,
        temperature: Float,
        maxOutputTokens: Int,
    ): String {
        val contents = JSONArray()
        for ((role, text) in messages) {
            contents.put(JSONObject().apply {
                put("role", role)
                put("parts", JSONArray().put(JSONObject().apply {
                    put("text", text)
                }))
            })
        }

        return JSONObject().apply {
            put("contents", contents)
            put("generationConfig", JSONObject().apply {
                put("temperature", temperature)
                put("maxOutputTokens", maxOutputTokens)
            })
        }.toString()
    }

    private fun parseResponseBody(bodyString: String): String {
        val json = JSONObject(bodyString)
        val candidates = json.optJSONArray("candidates")
        if (candidates != null && candidates.length() > 0) {
            val content = candidates.getJSONObject(0)
                .optJSONObject("content")
            if (content != null) {
                val parts = content.optJSONArray("parts")
                if (parts != null && parts.length() > 0) {
                    return parts.getJSONObject(0).optString("text", "")
                }
            }
            // Check for blocked finish reason
            val finishReason = candidates.getJSONObject(0)
                .optString("finishReason", "")
            if (finishReason == "SAFETY" || finishReason == "BLOCKLIST") {
                throw GeminiException("Response blocked: $finishReason", 0)
            }
        }
        // Try promptFeedback for blocked requests
        val promptFeedback = json.optJSONObject("promptFeedback")
        if (promptFeedback != null) {
            val blockReason = promptFeedback.optString("blockReason", "")
            if (blockReason.isNotEmpty()) {
                throw GeminiException("Request blocked: $blockReason", 0)
            }
        }
        return ""
    }

    private fun parseStreamChunkText(jsonStr: String): String {
        val json = JSONObject(jsonStr)
        val candidates = json.optJSONArray("candidates")
        if (candidates != null && candidates.length() > 0) {
            val content = candidates.getJSONObject(0)
                .optJSONObject("content")
            if (content != null) {
                val parts = content.optJSONArray("parts")
                if (parts != null && parts.length() > 0) {
                    return parts.getJSONObject(0).optString("text", "")
                }
            }
        }
        return ""
    }

    private fun parseError(bodyString: String, httpCode: Int): GeminiException {
        return try {
            val json = JSONObject(bodyString)
            val error = json.optJSONObject("error")
            val message = error?.optString("message", "") ?: ""
            GeminiException(message.ifEmpty { "HTTP $httpCode" }, httpCode)
        } catch (_: Exception) {
            GeminiException("HTTP $httpCode", httpCode)
        }
    }

    class GeminiException(
        message: String,
        val httpCode: Int,
    ) : Exception(message)

    companion object {
        private const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta"
        private const val TIMEOUT_SECONDS = 15L
    }
}
