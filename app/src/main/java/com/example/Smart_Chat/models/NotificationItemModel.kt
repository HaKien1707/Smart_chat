package com.example.Smart_Chat.models

enum class NotificationType {
    FRIEND_REQUEST,
    FRIEND_REQUEST_ACCEPTED,
    GROUP_JOIN_REQUEST,
    GROUP_JOIN_REQUEST_ACCEPTED,
    REMOVED_FROM_GROUP,
    BLOCKED_FROM_GROUP,      // NEW
    UNBLOCKED_FROM_GROUP,    // NEW
    BANNED_FROM_COMMUNITY,
    UNBANNED_FROM_COMMUNITY,
    BLOCKED_BY_USER,
    ADDED_TO_GROUP
}

data class NotificationItemModel(
    val type: NotificationType,
    val user: userModel?,  // Make optional
    val friendRequest: FriendRequestModel? = null,
    val groupJoinRequest: GroupJoinRequestModel? = null,
    val notification: NotificationModel? = null  // NEW: For other notification types
)