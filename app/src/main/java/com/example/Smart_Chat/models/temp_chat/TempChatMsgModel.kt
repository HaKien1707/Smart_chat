package com.example.Smart_Chat.models.temp_chat

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
    var encryptedFileUrl: String? = null

    @JvmField
    var encryptedFileName: String? = null

    @JvmField
    var fileSize: Long? = null

    @JvmField
    var messageType: String? = "text"

    @JvmField
    var replyToFileSize: Long? = null

    // Reply fields (encrypted)
    @JvmField
    var replyToMessageId: String? = null

    @JvmField
    var encryptedReplyToText: String? = null

    @JvmField
    var replyToType: String? = null

    @JvmField
    var encryptedReplyToImageUrl: String? = null

    @JvmField
    var encryptedReplyToFileName: String? = null

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

    // File constructor
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

    constructor(
        senderID: String?,
        encryptedMsg: String?,
        timestamp: Timestamp?,
        encryptedImageUrl: String? = null,
        messageType: String? = "text",
        encryptedFileUrl: String? = null,
        encryptedFileName: String? = null,
        fileSize: Long? = null,
        replyToMessageId: String? = null,
        encryptedReplyToText: String? = null,
        replyToType: String? = null,
        encryptedReplyToImageUrl: String? = null,
        encryptedReplyToFileName: String? = null,
        replyToFileSize: Long? = null
    ) {
        this.senderID = senderID
        this.encryptedMsg = encryptedMsg
        this.timestamp = timestamp
        this.encryptedImageUrl = encryptedImageUrl
        this.messageType = messageType
        this.encryptedFileUrl = encryptedFileUrl
        this.encryptedFileName = encryptedFileName
        this.fileSize = fileSize
        this.replyToMessageId = replyToMessageId
        this.encryptedReplyToText = encryptedReplyToText
        this.replyToType = replyToType
        this.encryptedReplyToImageUrl = encryptedReplyToImageUrl
        this.encryptedReplyToFileName = encryptedReplyToFileName
        this.replyToFileSize = replyToFileSize
    }
}

data class DecryptedTempMessage(
    val senderID: String?,
    val msg: String?,
    val timestamp: Timestamp?,
    val imageUrl: String?,
    val messageType: String?,
    val isDeleted: Boolean,
    val fileUrl: String?,
    val fileName: String?,
    val fileSize: Long?,
    val replyToMessageId: String?,
    val replyToText: String?,
    val replyToType: String?,
    val replyToImageUrl: String?,
    val replyToFileName: String?,
    val replyToFileSize: Long?
)