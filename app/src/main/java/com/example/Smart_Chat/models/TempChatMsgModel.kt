package com.example.Smart_Chat.models

import com.google.firebase.Timestamp

class TempChatMsgModel {
    @JvmField
    var senderID: String? = null

    @JvmField
    var encryptedMsg: String? = null // Changed from 'msg' to 'encryptedMsg'

    var timestamp: Timestamp? = null

    @JvmField
    var encryptedImageUrl: String? = null // Changed from 'imageUrl' to 'encryptedImageUrl'

    @JvmField
    var messageType: String? = "text"

    constructor()

    constructor(senderID: String?, encryptedMsg: String?, timestamp: Timestamp?) {
        this.senderID = senderID
        this.encryptedMsg = encryptedMsg
        this.timestamp = timestamp
        this.messageType = "text"
    }

    constructor(
        senderID: String?,
        encryptedMsg: String?,
        timestamp: Timestamp?,
        encryptedImageUrl: String?,
        messageType: String?
    ) {
        this.senderID = senderID
        this.encryptedMsg = encryptedMsg
        this.timestamp = timestamp
        this.encryptedImageUrl = encryptedImageUrl
        this.messageType = messageType
    }
}

/**
 * Decrypted message for in-memory storage
 * This is NEVER stored in Firestore - only in RAM
 */
data class DecryptedTempMessage(
    val senderID: String,
    val message: String,
    val timestamp: Timestamp,
    val messageType: String = "text",
    val imageUrl: String? = null
)