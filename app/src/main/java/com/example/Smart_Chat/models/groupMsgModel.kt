package com.example.Smart_Chat.models

import com.google.firebase.Timestamp

class GroupMsgModel {
    @JvmField
    var senderID: String? = null
    @JvmField
    var senderName: String? = null
    @JvmField
    var msg: String? = null
    var timestamp: Timestamp? = null
    @JvmField
    var imageUrl: String? = null  // NEW
    @JvmField
    var messageType: String? = "text"  // NEW

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
    }
}