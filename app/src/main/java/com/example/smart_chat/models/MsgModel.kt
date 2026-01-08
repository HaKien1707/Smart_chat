package com.example.smart_chat.models

import com.google.firebase.Timestamp

class MsgModel {
    @JvmField
    var senderID: String? = null

    @JvmField
    var msg: String? = null

    var timestamp: Timestamp? = null

    @JvmField
    var imageUrl: String? = null

    @JvmField
    var fileUrl: String? = null // For file attachments

    @JvmField
    var fileName: String? = null // Original file name

    @JvmField
    var fileSize: Long? = null // File size in bytes

    @JvmField
    var messageType: String? = "text" // "text", "image", "file"

    @JvmField
    var isBot: Boolean = false

    @JvmField
    var isRead: Boolean = false

    @JvmField
    var isDeleted: Boolean = false  // Track if message is deleted

    // Reply fields
    @JvmField
    var replyToMessageId: String? = null

    @JvmField
    var replyToText: String? = null

    @JvmField
    var replyToType: String? = null // "text", "image", "file"

    @JvmField
    var replyToImageUrl: String? = null

    @JvmField
    var replyToFileName: String? = null

    @JvmField
    var replyToFileSize: Long? = null

    constructor()

    constructor(
        senderID: String?,
        msg: String?,
        timestamp: Timestamp?,
        imageUrl: String?,
        messageType: String?
    ) {
        this.senderID = senderID
        this.msg = msg
        this.timestamp = timestamp
        this.imageUrl = imageUrl
        this.messageType = messageType
        this.isRead = false
        this.isDeleted = false
    }

    // Constructor for file messages
    constructor(
        senderID: String?,
        msg: String?,
        timestamp: Timestamp?,
        fileUrl: String?,
        fileName: String?,
        fileSize: Long?,
        messageType: String?
    ) {
        this.senderID = senderID
        this.msg = msg
        this.timestamp = timestamp
        this.fileUrl = fileUrl
        this.fileName = fileName
        this.fileSize = fileSize
        this.messageType = messageType
        this.isRead = false
        this.isDeleted = false
    }

    constructor(
        senderID: String?,
        msg: String?,
        timestamp: Timestamp?,
        imageUrl: String? = null,
        messageType: String? = "text",
        fileUrl: String? = null,
        fileName: String? = null,
        fileSize: Long? = null,
        replyToMessageId: String? = null,
        replyToText: String? = null,
        replyToType: String? = null,
        replyToImageUrl: String? = null,
        replyToFileName: String? = null,
        replyToFileSize: Long? = null
    ) {
        this.senderID = senderID
        this.msg = msg
        this.timestamp = timestamp
        this.imageUrl = imageUrl
        this.messageType = messageType
        this.fileUrl = fileUrl
        this.fileName = fileName
        this.fileSize = fileSize
        this.replyToMessageId = replyToMessageId
        this.replyToText = replyToText
        this.replyToType = replyToType
        this.replyToImageUrl = replyToImageUrl
        this.replyToFileName = replyToFileName
        this.replyToFileSize = replyToFileSize
    }

    constructor(
        senderID: String?,
        msg: String?,
        timestamp: Timestamp?,
        imageUrl: String? = null,
        messageType: String? = "text",
        fileUrl: String? = null,
        fileName: String? = null,
        fileSize: Long? = null,
        replyToMessageId: String? = null,
        replyToText: String? = null,
        replyToType: String? = null,
        replyToImageUrl: String? = null,
        replyToFileName: String? = null,
        replyToFileSize: Long? = null,
        isBot: Boolean = false
    ) {
        this.senderID = senderID
        this.msg = msg
        this.timestamp = timestamp
        this.imageUrl = imageUrl
        this.messageType = messageType
        this.fileUrl = fileUrl
        this.fileName = fileName
        this.fileSize = fileSize
        this.replyToMessageId = replyToMessageId
        this.replyToText = replyToText
        this.replyToType = replyToType
        this.replyToImageUrl = replyToImageUrl
        this.replyToFileName = replyToFileName
        this.replyToFileSize = replyToFileSize
        this.isBot = isBot
    }
}