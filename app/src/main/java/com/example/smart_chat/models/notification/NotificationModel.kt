package com.example.smart_chat.models.notification

import com.google.firebase.Timestamp

class NotificationModel {
    @JvmField
    var notificationID: String? = null

    @JvmField
    var type: String? = null  // Maps to NotificationType

    @JvmField
    var recipientID: String? = null  // User who receives notification

    @JvmField
    var senderID: String? = null  // User who triggered the notification

    @JvmField
    var senderName: String? = null

    @JvmField
    var groupID: String? = null  // For group-related notifications

    @JvmField
    var groupName: String? = null

    @JvmField
    var communityID: String? = null  // For community-related notifications

    @JvmField
    var communityName: String? = null

    @JvmField
    var message: String? = null  // Notification message

    @JvmField
    var isRead: Boolean = false

    var timestamp: Timestamp? = null

    constructor()

    constructor(
        notificationID: String?,
        type: String?,
        recipientID: String?,
        senderID: String?,
        senderName: String?,
        groupID: String?,
        groupName: String?,
        communityID: String?,
        communityName: String?,
        message: String?,
        isRead: Boolean,
        timestamp: Timestamp?
    ) {
        this.notificationID = notificationID
        this.type = type
        this.recipientID = recipientID
        this.senderID = senderID
        this.senderName = senderName
        this.groupID = groupID
        this.groupName = groupName
        this.communityID = communityID
        this.communityName = communityName
        this.message = message
        this.isRead = isRead
        this.timestamp = timestamp
    }
}