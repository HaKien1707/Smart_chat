package com.example.Smart_Chat.models

import com.google.firebase.Timestamp

class GroupMsgModel {
    @JvmField
    var senderID: String? = null

    @JvmField
    var senderName: String? = null  // To show who sent the message

    @JvmField
    var msg: String? = null

    var timestamp: Timestamp? = null

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
    }
}