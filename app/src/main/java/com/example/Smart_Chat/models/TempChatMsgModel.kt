package com.example.Smart_Chat.models

import com.google.firebase.Timestamp

class TempChatMsgModel {
    @JvmField
    var senderID: String? = null

    @JvmField
    var encryptedMsg: String? = null

    var timestamp: Timestamp? = null

    @JvmField
    var encryptedImageUrl: String? = null

    @JvmField
    var encryptedFileUrl: String? = null // NEW

    @JvmField
    var encryptedFileName: String? = null // NEW

    @JvmField
    var fileSize: Long? = null // NEW (not encrypted, just size)

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

    // NEW: File constructor
    constructor(
        senderID: String?,
        encryptedMsg: String?,
        timestamp: Timestamp?,
        encryptedFileUrl: String?,
        encryptedFileName: String?,
        fileSize: Long?,
        messageType: String?
    ) {
        this.senderID = senderID
        this.encryptedMsg = encryptedMsg
        this.timestamp = timestamp
        this.encryptedFileUrl = encryptedFileUrl
        this.encryptedFileName = encryptedFileName
        this.fileSize = fileSize
        this.messageType = messageType
    }
}

data class DecryptedTempMessage(
    val senderID: String,
    val message: String,
    val timestamp: Timestamp,
    val messageType: String = "text",
    val imageUrl: String? = null,
    val fileUrl: String? = null, // NEW
    val fileName: String? = null, // NEW
    val fileSize: Long? = null // NEW
)