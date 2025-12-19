package com.example.Smart_Chat.utils.AI

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.example.Smart_Chat.BuildConfig
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.generationConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Calendar

object GeminiHelper {

    private const val MODEL_NAME = "gemini-2.5-flash"
    private const val USAGE_LIMIT_KEY = "bot_usage_count"
    private const val DAILY_LIMIT = 10
    private const val LAST_RESET_KEY = "bot_last_reset"

    private val generativeModel by lazy {
        GenerativeModel(
            modelName = MODEL_NAME,
            apiKey = BuildConfig.GEMINI_API_KEY,
            generationConfig = generationConfig {
                temperature = 0.7f
                topK = 40
                topP = 0.95f
                maxOutputTokens = 1024
            }
        )
    }

    /**
     * Check if user has remaining bot requests today
     */
    fun canMakeBotRequest(context: Context): Boolean {
        val prefs = context.getSharedPreferences("bot_prefs", Context.MODE_PRIVATE)
        resetIfNewDay(context, prefs)

        val usageCount = prefs.getInt(USAGE_LIMIT_KEY, 0)
        return usageCount < DAILY_LIMIT
    }

    /**
     * Get remaining requests for today
     */
    fun getRemainingRequests(context: Context): Int {
        val prefs = context.getSharedPreferences("bot_prefs", Context.MODE_PRIVATE)
        resetIfNewDay(context, prefs)

        val usageCount = prefs.getInt(USAGE_LIMIT_KEY, 0)
        return DAILY_LIMIT - usageCount
    }

    /**
     * Increment bot usage counter
     */
    private fun incrementUsage(context: Context) {
        val prefs = context.getSharedPreferences("bot_prefs", Context.MODE_PRIVATE)
        val currentCount = prefs.getInt(USAGE_LIMIT_KEY, 0)
        prefs.edit().putInt(USAGE_LIMIT_KEY, currentCount + 1).apply()
    }

    /**
     * Reset counter if it's a new day
     */
    private fun resetIfNewDay(context: Context, prefs: SharedPreferences) {
        val today = Calendar.getInstance().get(Calendar.DAY_OF_YEAR)
        val lastReset = prefs.getInt(LAST_RESET_KEY, -1)

        if (today != lastReset) {
            prefs.edit()
                .putInt(USAGE_LIMIT_KEY, 0)
                .putInt(LAST_RESET_KEY, today)
                .apply()
        }
    }

    /**
     * Send prompt to Gemini and get response
     */
    suspend fun getBotResponse(
        context: Context,
        chatHistory: List<String>,
        userPrompt: String
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            if (!canMakeBotRequest(context)) {
                return@withContext Result.failure(
                    Exception("Daily limit reached (${DAILY_LIMIT} requests/day). Try again tomorrow.")
                )
            }

            // Build the full prompt
            val fullPrompt = buildPrompt(chatHistory, userPrompt)

            Log.d("GeminiHelper", "Sending prompt: $fullPrompt")

            // Call Gemini API
            val response = generativeModel.generateContent(fullPrompt)
            val responseText = response.text ?: "No response received"

            // Increment usage counter
            incrementUsage(context)

            Log.d("GeminiHelper", "Response: $responseText")

            Result.success(responseText)

        } catch (e: Exception) {
            Log.e("GeminiHelper", "Error calling Gemini API", e)
            Result.failure(e)
        }
    }

    /**
     * Build the prompt with system instruction, context, and user request
     */
    private fun buildPrompt(chatHistory: List<String>, userPrompt: String): String {
        return buildString {
            appendLine("You are a helpful AI assistant in a chat application.")
            appendLine("You can summarize conversations, answer questions about the chat, and provide context.")
            appendLine("Be concise and helpful. Keep responses under 200 words unless specifically asked for more detail.")
            appendLine()
            appendLine("Here is the recent conversation:")
            appendLine("---")
            chatHistory.forEach { message ->
                appendLine(message)
            }
            appendLine("---")
            appendLine()
            appendLine("User request: $userPrompt")
        }
    }

    /**
     * Format messages for AI context
     */
    fun formatMessagesForContext(
        messages: List<Pair<String, String>>, // List of (senderName, messageText)
        limit: Int = 30
    ): List<String> {
        return messages
            .takeLast(limit)
            .map { (sender, message) -> "$sender: $message" }
    }
}