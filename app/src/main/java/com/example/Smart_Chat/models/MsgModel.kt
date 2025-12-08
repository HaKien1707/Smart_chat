package com.example.Smart_Chat.models

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
    var messageType: String? = "text"

    @JvmField
    var isRead: Boolean = false

    @JvmField
    var readTimestamp: Timestamp? = null

    @JvmField
    var isDeleted: Boolean = false  // Track if message is deleted

    constructor()

    constructor(senderID: String?, msg: String?, timestamp: Timestamp?) {
        this.senderID = senderID
        this.msg = msg
        this.timestamp = timestamp
        this.messageType = "text"
        this.isRead = false
        this.isDeleted = false
    }

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
}