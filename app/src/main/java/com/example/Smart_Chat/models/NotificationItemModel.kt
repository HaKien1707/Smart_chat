package com.example.Smart_Chat.models

enum class NotificationType {
    FRIEND_REQUEST,
    GROUP_JOIN_REQUEST
}

data class NotificationItemModel(
    val type: NotificationType,
    val user: userModel,
    val friendRequest: FriendRequestModel? = null,
    val groupJoinRequest: GroupJoinRequestModel? = null
)