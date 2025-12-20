package com.example.Smart_Chat.utils.AI

import android.content.Context
import android.util.Log
import com.example.Smart_Chat.models.*
import com.example.Smart_Chat.models.community.CommunityMsgModel
import com.example.Smart_Chat.models.group.GroupMsgModel
import com.google.firebase.Timestamp
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await

object BotMessageHelper {

    /**
     * Check if message is a bot command
     */
    fun isBotCommand(message: String): Boolean {
        return message.trimStart().startsWith("@Bot", ignoreCase = true) ||
                message.trimStart().startsWith("@bot") ||
                message.trimStart().startsWith("@BOT")
    }

    /**
     * Extract user prompt from bot command
     */
    fun extractPrompt(command: String): String {
        return command.substring(4).trim()
    }

    /**
     * Check if bot can process request (rate limit check)
     */
    fun canProcessBotRequest(context: Context): Pair<Boolean, String> {
        if (!GeminiHelper.canMakeBotRequest(context)) {
            val message = "Daily bot limit reached (10 requests/day). Try again tomorrow."
            return Pair(false, message)
        }
        return Pair(true, "")
    }

    /**
     * Fetch and format recent messages for context
     * Works for all chat types
     */
    suspend fun fetchAndFormatMessages(
        messagesRef: CollectionReference,
        currentUserId: String,
        chatType: ChatType,
        otherUserName: String? = null,
        limit: Int = 30
    ): List<String> {
        return try {
            val documents = messagesRef
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(limit.toLong())
                .get()
                .await()

            val messages = mutableListOf<Pair<String, String>>()

            documents.reversed().forEach { doc ->
                when (chatType) {
                    ChatType.USER_CHAT -> {
                        val msg = doc.toObject(MsgModel::class.java)
                        if (msg.isBot || msg.isDeleted) return@forEach

                        val senderName = if (msg.senderID == currentUserId) "You" else (otherUserName ?: "User")
                        val text = getMessageText(msg.messageType, msg.msg, msg.fileName)
                        if (text.isNotEmpty()) messages.add(Pair(senderName, text))
                    }
                    ChatType.GROUP_CHAT -> {
                        val msg = doc.toObject(GroupMsgModel::class.java)
                        if (msg.isDeleted) return@forEach

                        val senderName = if (msg.senderID == currentUserId) "You" else (msg.senderName ?: "Unknown")
                        val text = getMessageText(msg.messageType, msg.msg, msg.fileName)
                        if (text.isNotEmpty()) messages.add(Pair(senderName, text))
                    }
                    ChatType.COMMUNITY_CHAT -> {
                        val msg = doc.toObject(CommunityMsgModel::class.java)
                        if (msg.isDeleted) return@forEach

                        val senderName = if (msg.senderID == currentUserId) "You" else (msg.senderName ?: "Unknown")
                        val text = getMessageText(msg.messageType, msg.msg, msg.fileName)
                        if (text.isNotEmpty()) messages.add(Pair(senderName, text))
                    }
                    ChatType.TEMP_CHAT -> {
                        // Temp chat needs special handling due to encryption
                        // We'll handle this separately in the activity
                        return@forEach
                    }
                }
            }

            GeminiHelper.formatMessagesForContext(messages, limit)
        } catch (e: Exception) {
            Log.e("BotMessageHelper", "Failed to fetch messages", e)
            emptyList()
        }
    }

    /**
     * Get message text based on type
     */
    private fun getMessageText(messageType: String?, text: String?, fileName: String?): String {
        return when (messageType) {
            "image" -> "[Image]"
            "file" -> "[File: $fileName]"
            else -> text ?: ""
        }
    }

    /**
     * Send bot response message
     * Returns the message ID for follow-up usage message
     */
    suspend fun sendBotResponse(
        messagesRef: CollectionReference,
        chatRef: DocumentReference,
        response: String,
        chatType: ChatType,
        currentUserId: String,
        currentUserName: String? = null
    ): String? {
        return try {
            val msgModel = when (chatType) {
                ChatType.USER_CHAT -> {
                    MsgModel(
                        senderID = "BOT",
                        msg = response,
                        timestamp = Timestamp.now(),
                        messageType = "text",
                        isBot = true
                    )
                }
                ChatType.GROUP_CHAT -> {
                    GroupMsgModel(
                        senderID = "BOT",
                        senderName = "🤖 Bot",
                        msg = response,
                        timestamp = Timestamp.now(),
                        messageType = "text"
                    )
                }
                ChatType.COMMUNITY_CHAT -> {
                    CommunityMsgModel(
                        senderID = "BOT",
                        senderName = "🤖 Bot",
                        msg = response,
                        timestamp = Timestamp.now(),
                        messageType = "text"
                    )
                }
                ChatType.TEMP_CHAT -> {
                    // Temp chat needs encryption - handle separately
                    return null
                }
            }

            // Add message
            val docRef = messagesRef.add(msgModel).await()

            // Update last message in chat
            val lastMsgPreview = "🤖 Bot: ${response.take(30)}..."
            chatRef.update(
                mapOf(
                    "lastMsg" to lastMsgPreview,
                    "lastMsgSenderID" to "BOT",
                    "lastMsgTimestamp" to Timestamp.now()
                )
            ).await()

            docRef.id
        } catch (e: Exception) {
            Log.e("BotMessageHelper", "Failed to send bot message", e)
            null
        }
    }

    /**
     * Send usage info message after bot response
     */
    suspend fun sendUsageMessage(
        context: Context,
        messagesRef: CollectionReference,
        chatType: ChatType
    ) {
        try {
            val remaining = GeminiHelper.getRemainingRequests(context)
            val usageText = "ℹ️ Bot requests remaining today: $remaining/10"

            val msgModel = when (chatType) {
                ChatType.USER_CHAT -> {
                    MsgModel(
                        senderID = "SYSTEM",
                        msg = usageText,
                        timestamp = Timestamp.now(),
                        messageType = "text",
                        isBot = true
                    )
                }
                ChatType.GROUP_CHAT -> {
                    GroupMsgModel(
                        senderID = "SYSTEM",
                        senderName = "System",
                        msg = usageText,
                        timestamp = Timestamp.now(),
                        messageType = "text"
                    )
                }
                ChatType.COMMUNITY_CHAT -> {
                    CommunityMsgModel(
                        senderID = "SYSTEM",
                        senderName = "System",
                        msg = usageText,
                        timestamp = Timestamp.now(),
                        messageType = "text"
                    )
                }
                ChatType.TEMP_CHAT -> {
                    // Handle separately
                    return
                }
            }

            messagesRef.add(msgModel).await()
        } catch (e: Exception) {
            Log.e("BotMessageHelper", "Failed to send usage message", e)
        }
    }

    /**
     * Chat type enum
     */
    enum class ChatType {
        USER_CHAT,
        GROUP_CHAT,
        COMMUNITY_CHAT,
        TEMP_CHAT
    }
}