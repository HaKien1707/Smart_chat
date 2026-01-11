package com.example.smart_chat.models.group

import com.google.firebase.Timestamp

class groupModel {
    @JvmField
    var groupID: String? = null

    @JvmField
    var groupName: String? = null

    @JvmField
    var groupImage: String? = null

    @JvmField
    var memberIDs: MutableList<String?>? = null

    @JvmField
    var adminIDs: MutableList<String?>? = null

    // New schema (backwards compatible): exactly one owner, and 0..n admins.
    @JvmField
    var ownerID: String? = null

    @JvmField
    var blockedUserIDs: MutableList<String> = mutableListOf() // Blocked users list

    @JvmField
    var lastMsg: String? = null

    @JvmField
    var lastMsgSenderID: String? = null

    @JvmField
    var lastMsgTimestamp: Timestamp? = null

    var createdTimestamp: Timestamp? = null

    @JvmField
    var createdBy: String? = null

    constructor()

    constructor(
        groupID: String?,
        groupName: String?,
        groupImage: String?,
        memberIDs: MutableList<String?>?,
        adminIDs: MutableList<String?>?,
        createdTimestamp: Timestamp?,
        createdBy: String?
    ) {
        this.groupID = groupID
        this.groupName = groupName
        this.groupImage = groupImage
        this.memberIDs = memberIDs
        this.adminIDs = adminIDs
        this.createdTimestamp = createdTimestamp
        this.createdBy = createdBy
        this.ownerID = createdBy
        this.blockedUserIDs = mutableListOf()
    }
}