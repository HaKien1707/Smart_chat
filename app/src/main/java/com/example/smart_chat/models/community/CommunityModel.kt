package com.example.smart_chat.models.community

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

    // New schema (backwards compatible): one owner, many admins
    @JvmField
    var ownerID: String? = null

    @JvmField
    var adminIDs: MutableList<String>? = null

    // "public" (searchable) or "private" (not searchable)
    @JvmField
    var communityType: String? = "public"

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
        this.ownerID = adminID
        this.adminIDs = mutableListOf()
        this.communityType = "public"
        this.createdTimestamp = createdTimestamp
        this.bannedUserIDs = mutableListOf()
        this.announcement = null
    }
}