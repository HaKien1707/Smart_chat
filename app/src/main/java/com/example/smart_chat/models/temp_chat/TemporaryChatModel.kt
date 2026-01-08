package com.example.smart_chat.models.temp_chat

import com.google.firebase.Timestamp
import java.util.Date

class TemporaryChatModel {
    @JvmField
    var chatID: String? = null

    @JvmField
    var userIDs: MutableList<String?>? = null

    @JvmField
    var createdAt: Timestamp? = null

    @JvmField
    var expiresAt: Timestamp? = null

    @JvmField
    var lastMsg: String? = null

    @JvmField
    var lastMsgSenderID: String? = null

    @JvmField
    var lastMsgTimestamp: Timestamp? = null

    @JvmField
    var encryptionKey: String? = null // AES encryption key

    @JvmField
    var activeUsers: MutableList<String?>? = null // Track active users

    constructor()

    constructor(
        chatID: String?,
        userIDs: MutableList<String?>?,
        createdAt: Timestamp?,
        encryptionKey: String?
    ) {
        this.chatID = chatID
        this.userIDs = userIDs
        this.createdAt = createdAt
        this.encryptionKey = encryptionKey

        // Set expiration to 5 minutes from creation
        val expiryMillis = createdAt?.toDate()?.time?.plus(5 * 60 * 1000) // 5 minutes
        this.expiresAt = if (expiryMillis != null) Timestamp(Date(expiryMillis)) else null

        this.activeUsers = mutableListOf() // Initially empty
    }
}