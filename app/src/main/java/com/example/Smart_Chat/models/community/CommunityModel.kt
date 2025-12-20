package com.example.Smart_Chat.models.community

import com.google.firebase.Timestamp

class CommunityModel {
    @JvmField
    var communityID: String? = null

    @JvmField
    var communityName: String? = null

    @JvmField
    var communityDescription: String? = null

    @JvmField
    var communityImage: String? = null

    @JvmField
    var adminID: String? = null

    @JvmField
    var bannedUserIDs: MutableList<String> = mutableListOf()

    @JvmField
    var announcement: String? = null

    @JvmField
    var lastMsg: String? = null

    @JvmField
    var lastMsgSenderID: String? = null

    @JvmField
    var lastMsgTimestamp: Timestamp? = null

    @JvmField
    var createdTimestamp: Timestamp? = null

    constructor()

    constructor(
        communityID: String?,
        communityName: String?,
        communityDescription: String?,
        communityImage: String?,
        adminID: String?,
        createdTimestamp: Timestamp?
    ) {
        this.communityID = communityID
        this.communityName = communityName
        this.communityDescription = communityDescription
        this.communityImage = communityImage
        this.adminID = adminID
        this.createdTimestamp = createdTimestamp
        this.bannedUserIDs = mutableListOf()
        this.announcement = null
    }
}