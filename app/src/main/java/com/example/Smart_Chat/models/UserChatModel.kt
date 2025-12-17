package com.example.Smart_Chat.models

import com.google.firebase.Timestamp

class UserChatModel {
    @JvmField
    var chatRoomID: String? = null

    @JvmField
    var userID: MutableList<String?>? = null

    @JvmField
    var lastMsg: String? = null

    @JvmField
    var lastMsgSenderID: String? = null

    @JvmField
    var lastMsgTimestamp: Timestamp? = null

    // Track which users have deleted this chat
    @JvmField
    var deletedBy: MutableList<String> = mutableListOf()

    constructor()

    constructor(
        chatRoomID: String?,
        userID: MutableList<String?>?,
        lastMsg: String?,
        lastMsgSenderID: String?,
        lastMsgTimestamp: Timestamp?
    ) {
        this.chatRoomID = chatRoomID
        this.userID = userID
        this.lastMsg = lastMsg
        this.lastMsgSenderID = lastMsgSenderID
        this.lastMsgTimestamp = lastMsgTimestamp
        this.deletedBy = mutableListOf()
    }
}