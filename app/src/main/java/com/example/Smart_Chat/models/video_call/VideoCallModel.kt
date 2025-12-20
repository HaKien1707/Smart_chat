package com.example.Smart_Chat.models.video_call

import com.google.firebase.Timestamp

class VideoCallModel {
    @JvmField
    var callId: String? = null

    @JvmField
    var callerId: String? = null

    @JvmField
    var callerName: String? = null

    @JvmField
    var receiverId: String? = null

    @JvmField
    var receiverName: String? = null

    @JvmField
    var status: String? = "ringing" // "ringing", "accepted", "rejected", "ended", "missed"

    @JvmField
    var type: String? = "video" // "video" or "audio"

    var timestamp: Timestamp? = null

    @JvmField
    var offer: String? = null // SDP offer

    @JvmField
    var answer: String? = null // SDP answer

    constructor()

    constructor(
        callId: String?,
        callerId: String?,
        callerName: String?,
        receiverId: String?,
        receiverName: String?,
        status: String?,
        type: String?,
        timestamp: Timestamp?
    ) {
        this.callId = callId
        this.callerId = callerId
        this.callerName = callerName
        this.receiverId = receiverId
        this.receiverName = receiverName
        this.status = status
        this.type = type
        this.timestamp = timestamp
    }
}