package com.example.Smart_Chat.models.community

import com.google.firebase.Timestamp

class CommunityMsgModel {
    @JvmField
    var senderID: String? = null

    @JvmField
    var senderName: String? = null

    @JvmField
    var msg: String? = null

    @JvmField
    var timestamp: Timestamp? = null

    @JvmField
    var isAnnouncement: Boolean = false

    @JvmField
    var imageUrl: String? = null

    @JvmField
    var fileUrl: String? = null

    @JvmField
    var fileName: String? = null

    @JvmField
    var fileSize: Long? = null

    @JvmField
    var messageType: String? = "text"

    @JvmField
    var isDeleted: Boolean = false

    // Reply fields
    @JvmField
    var replyToMessageId: String? = null

    @JvmField
    var replyToText: String? = null

    @JvmField
    var replyToType: String? = null

    @JvmField
    var replyToImageUrl: String? = null

    @JvmField
    var replyToFileName: String? = null

    @JvmField
    var replyToFileSize: Long? = null

    @JvmField
    var replyToSenderName: String? = null

    constructor()

    constructor(
        senderID: String?,
        senderName: String?,
        msg: String?,
        timestamp: Timestamp?
    ) {
        this.senderID = senderID
        this.senderName = senderName
        this.msg = msg
        this.timestamp = timestamp
        this.messageType = "text"
        this.isDeleted = false
    }

    constructor(
        senderID: String?,
        senderName: String?,
        msg: String?,
        timestamp: Timestamp?,
        imageUrl: String?,
        messageType: String?
    ) {
        this.senderID = senderID
        this.senderName = senderName
        this.msg = msg
        this.timestamp = timestamp
        this.imageUrl = imageUrl
        this.messageType = messageType
        this.isDeleted = false
    }

    // File constructor
    constructor(
        senderID: String?,
        senderName: String?,
        msg: String?,
        timestamp: Timestamp?,
        fileUrl: String?,
        fileName: String?,
        fileSize: Long?,
        messageType: String?
    ) {
        this.senderID = senderID
        this.senderName = senderName
        this.msg = msg
        this.timestamp = timestamp
        this.fileUrl = fileUrl
        this.fileName = fileName
        this.fileSize = fileSize
        this.messageType = messageType
        this.isDeleted = false
    }

    constructor(
        senderID: String?,
        senderName: String?,
        msg: String?,
        timestamp: Timestamp?,
        imageUrl: String? = null,
        messageType: String? = "text",
        isAnnouncement: Boolean = false,
        fileUrl: String? = null,
        fileName: String? = null,
        fileSize: Long? = null,
        replyToMessageId: String? = null,
        replyToText: String? = null,
        replyToType: String? = null,
        replyToImageUrl: String? = null,
        replyToFileName: String? = null,
        replyToFileSize: Long? = null,
        replyToSenderName: String? = null
    ) {
        this.senderID = senderID
        this.senderName = senderName
        this.msg = msg
        this.timestamp = timestamp
        this.imageUrl = imageUrl
        this.messageType = messageType
        this.isAnnouncement = isAnnouncement
        this.fileUrl = fileUrl
        this.fileName = fileName
        this.fileSize = fileSize
        this.replyToMessageId = replyToMessageId
        this.replyToText = replyToText
        this.replyToType = replyToType
        this.replyToImageUrl = replyToImageUrl
        this.replyToFileName = replyToFileName
        this.replyToFileSize = replyToFileSize
        this.replyToSenderName = replyToSenderName
    }
}