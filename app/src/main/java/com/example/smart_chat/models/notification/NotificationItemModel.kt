package com.example.smart_chat.models.notification

import com.example.smart_chat.models.request.FriendRequestModel
import com.example.smart_chat.models.request.GroupJoinRequestModel
import com.example.smart_chat.models.userModel

enum class NotificationType {
    FRIEND_REQUEST,
    FRIEND_REQUEST_ACCEPTED,
    GROUP_JOIN_REQUEST,
    GROUP_JOIN_REQUEST_ACCEPTED,
    REMOVED_FROM_GROUP,
    BLOCKED_FROM_GROUP,
    UNBLOCKED_FROM_GROUP,
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
    val notification: NotificationModel? = null  // For other notification types
)