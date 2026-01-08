package com.example.smart_chat.models.request

import com.google.firebase.Timestamp

class FriendRequestModel {
    @JvmField
    var requestID: String? = null
    @JvmField
    var senderID: String? = null
    @JvmField
    var senderName: String? = null
    @JvmField
    var receiverID: String? = null
    @JvmField
    var receiverName: String? = null
    @JvmField
    var status: String? = "pending"  // "pending", "accepted", "rejected"
    var timestamp: Timestamp? = null

    constructor()

    constructor(
        requestID: String?,
        senderID: String?,
        senderName: String?,
        receiverID: String?,
        receiverName: String?,
        status: String?,
        timestamp: Timestamp?
    ) {
        this.requestID = requestID
        this.senderID = senderID
        this.senderName = senderName
        this.receiverID = receiverID
        this.receiverName = receiverName
        this.status = status
        this.timestamp = timestamp
    }
}