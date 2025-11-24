package com.example.Smart_Chat.models

import com.google.firebase.Timestamp

class groupModel {
    @JvmField
    var groupID: String? = null

    @JvmField
    var groupName: String? = null

    @JvmField
    var groupImage: String? = null  // Base64 encoded image

    @JvmField
    var memberIDs: MutableList<String?>? = null  // List of user IDs in the group

    @JvmField
    var adminIDs: MutableList<String?>? = null  // List of admin user IDs

    @JvmField
    var lastMsg: String? = null

    @JvmField
    var lastMsgSenderID: String? = null

    @JvmField
    var lastMsgTimestamp: Timestamp? = null

    var createdTimestamp: Timestamp? = null

    @JvmField
    var createdBy: String? = null  // User ID who created the group

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
    }
}