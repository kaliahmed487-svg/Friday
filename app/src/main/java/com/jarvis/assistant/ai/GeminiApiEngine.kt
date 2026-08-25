package com.jarvis.assistant.ai

import com.jarvis.assistant.core.ApiConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.StandardCharsets

/**
 * Online brain: calls Google's Gemini API directly over REST. No SDK
 * dependency needed — just HttpURLConnection (built into Android) and
 * org.json (also built in), so this adds zero build risk.
 *
 * Free tier: generous per-minute/per-day request quota, no billing setup
 * required to get started. Get a key at https://aistudio.google.com/apikey
 * and paste it into ApiConfig.kt.
 */
class GeminiApiEngine {

    companion object {
        private const val MODEL = "gemini-2.0-flash"
        private const val ENDPOINT =
            "https://generativelanguage.googleapis.com/v1beta/models/$MODEL:generateContent"

        const val SYSTEM_PROMPT =
            "You are Friday, a highly intelligent, polite, and witty British butler. " +
            "Keep responses concise, direct, and mildly humorous. Speak naturally " +
            "without using emotional tags or stage directions."
    }

    suspend fun generate(userPrompt: String): String = withContext(Dispatchers.IO) {
        val url = URL("$ENDPOINT?key=${ApiConfig.GEMINI_API_KEY}")
        val connection = url.openConnection() as HttpURLConnection
        try {
            connection.requestMethod = "POST"
            connection.setRequestProperty("Content-Type", "application/json")
            connection.doOutput = true
            connection.connectTimeout = 8_000
            connection.readTimeout = 15_000

            val body = JSONObject().apply {
                put("system_instruction", JSONObject().apply {
                    put("parts", JSONArray().put(JSONObject().put("text", SYSTEM_PROMPT)))
                })
                put("contents", JSONArray().put(JSONObject().apply {
                    put("role", "user")
                    put("parts", JSONArray().put(JSONObject().put("text", userPrompt)))
                }))
                put("generationConfig", JSONObject().apply {
                    put("maxOutputTokens", 200)
                    put("temperature", 0.8)
                })
            }

            connection.outputStream.use { it.write(body.toString().toByteArray(StandardCharsets.UTF_8)) }

            val responseCode = connection.responseCode
            val stream = if (responseCode in 200..299) connection.inputStream else connection.errorStream
            val responseText = stream.bufferedReader(StandardCharsets.UTF_8).use { it.readText() }

            if (responseCode !in 200..299) {
                error("Gemini API error $responseCode: $responseText")
            }

            val json = JSONObject(responseText)
            json.getJSONArray("candidates")
                .getJSONObject(0)
                .getJSONObject("content")
                .getJSONArray("parts")
                .getJSONObject(0)
                .getString("text")
                .trim()
        } finally {
            connection.disconnect()
        }
    }
}
