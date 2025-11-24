package com.example.Smart_Chat.models

import com.google.firebase.Timestamp

class chatRoomModel {
    var chatRoomID: String? = null
    @JvmField
    var userID: MutableList<String?>? = null
    @JvmField
    var lastMsgTimestamp: Timestamp? = null
    @JvmField
    var lastMsgSenderID: String? = null
    @JvmField
    var lastMsg: String? = null

    constructor()

    constructor(
        chatRoomID: String?,
        userID: MutableList<String?>?,
        lastMsgTimestamp: Timestamp?,
        lastMsgSenderID: String?
    ) {
        this.chatRoomID = chatRoomID
        this.userID = userID
        this.lastMsgTimestamp = lastMsgTimestamp
        this.lastMsgSenderID = lastMsgSenderID
    }
}
