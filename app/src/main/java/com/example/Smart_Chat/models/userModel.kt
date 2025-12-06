package com.example.Smart_Chat.models

import com.google.firebase.Timestamp

class userModel {
    @JvmField
    var phoneNumber: String? = null
    @JvmField
    var username: String? = null
    var createdTimestamp: Timestamp? = null
    @JvmField
    var userID: String? = null
    @JvmField
    var fcmToken: String? = null
    @JvmField
    var profileImage: String? = null

    // NEW: Blocked users list
    @JvmField
    var blockedUsers: MutableList<String> = mutableListOf()

    constructor()

    constructor(
        phoneNumber: String?,
        username: String?,
        createdTimestamp: Timestamp?,
        userID: String?,
        profileImage: String?,
        fcm: String?
    ) {
        this.phoneNumber = phoneNumber
        this.username = username
        this.createdTimestamp = createdTimestamp
        this.userID = userID
        this.profileImage = profileImage
        this.fcmToken = fcm
        this.blockedUsers = mutableListOf()
    }
}