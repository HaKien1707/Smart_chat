package com.example.Smart_Chat.models

import com.google.firebase.Timestamp

class MsgModel {
    @JvmField
    var senderID: String? = null
    @JvmField
    var msg: String? = null
    var timestamp: Timestamp? = null

    constructor()

    constructor(senderID: String?, msg: String?, timestamp: Timestamp?) {
        this.senderID = senderID
        this.msg = msg
        this.timestamp = timestamp
    }
}
