package com.example.Smart_Chat.models

import com.google.firebase.Timestamp

class GroupJoinRequestModel {
    @JvmField
    var requestID: String? = null
    @JvmField
    var groupID: String? = null
    @JvmField
    var groupName: String? = null
    @JvmField
    var senderID: String? = null
    @JvmField
    var senderName: String? = null
    @JvmField
    var status: String? = "pending" // "pending", "accepted", "rejected"
    var timestamp: Timestamp? = null

    constructor()

    constructor(
        requestID: String?,
        groupID: String?,
        groupName: String?,
        senderID: String?,
        senderName: String?,
        status: String?,
        timestamp: Timestamp?
    ) {
        this.requestID = requestID
        this.groupID = groupID
        this.groupName = groupName
        this.senderID = senderID
        this.senderName = senderName
        this.status = status
        this.timestamp = timestamp
    }
}