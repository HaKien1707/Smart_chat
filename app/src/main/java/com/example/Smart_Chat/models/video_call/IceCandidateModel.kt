package com.example.Smart_Chat.models.video_call

class IceCandidateModel {
    @JvmField
    var sdpMid: String? = null

    @JvmField
    var sdpMLineIndex: Int? = null

    @JvmField
    var sdp: String? = null

    @JvmField
    var userId: String? = null

    constructor()

    constructor(sdpMid: String?, sdpMLineIndex: Int?, sdp: String?, userId: String?) {
        this.sdpMid = sdpMid
        this.sdpMLineIndex = sdpMLineIndex
        this.sdp = sdp
        this.userId = userId
    }
}