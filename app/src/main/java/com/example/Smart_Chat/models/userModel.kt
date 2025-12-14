package com.example.Smart_Chat.models

import com.google.firebase.Timestamp

class userModel {
    @JvmField
    var userID: String? = null

    @JvmField
    var username: String? = null

    @JvmField
    var phoneNumber: String? = null

    @JvmField
    var password: String? = null

    @JvmField
    var email: String? = null

    @JvmField
    var nationality: String? = null

    @JvmField
    var profileImage: String? = null

    @JvmField
    var fcmToken: String? = null

    @JvmField
    var blockedUsers: MutableList<String?>? = null

    @JvmField
    var createdAt: Timestamp? = null

    constructor()

    constructor(
        userID: String?,
        username: String?,
        phoneNumber: String?,
        password: String?,
        email: String?,
        nationality: String?,
        createdAt: Timestamp?
    ) {
        this.userID = userID
        this.username = username
        this.phoneNumber = phoneNumber
        this.password = password
        this.email = email
        this.nationality = nationality
        this.createdAt = createdAt
        this.blockedUsers = mutableListOf()
    }
}