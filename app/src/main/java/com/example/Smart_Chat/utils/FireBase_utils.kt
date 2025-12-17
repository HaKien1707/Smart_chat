package com.example.Smart_Chat.utils

import com.example.Smart_Chat.models.*
import com.example.Smart_Chat.utils.firebase.*

/**
 * Legacy wrapper for FireBase utilities
 * Delegates to new modular Firebase classes
 *
 * @deprecated Use specific Firebase modules instead (FirebaseAuth, FirebaseChat, etc.)
 */
@Deprecated("Use specific Firebase modules instead")
object FireBase_utils {

    // ========== AUTH & USER ==========
    @JvmStatic
    fun currentUserID() = FirebaseAuth.currentUserID()

    @JvmStatic
    fun currentUserDetails() = FirebaseAuth.currentUserDetails()

    @JvmStatic
    fun allUsersCollection() = FirebaseAuth.allUsersCollection()

    @JvmStatic
    fun logout() = FirebaseAuth.logout()

    @JvmStatic
    val isLoggedIn get() = FirebaseAuth.isLoggedIn

    // ========== CHAT ROOMS ==========
    @JvmStatic
    fun getChatRoomReferences(chatRoomID: String) = FirebaseChat.getChatRoomReference(chatRoomID)

    @JvmStatic
    fun getChatRoomID(userID1: String?, userID2: String?) = FirebaseChat.getChatRoomID(userID1, userID2)

    @JvmStatic
    fun getChatRoomMessagesReferences(chatRoomID: String) = FirebaseChat.getChatRoomMessagesReference(chatRoomID)

    @JvmStatic
    fun allChatRoomsCollectionReference() = FirebaseChat.allChatRoomsCollection()

    @JvmStatic
    fun get2ndUserInChatRoom(userID: MutableList<String?>?) = FirebaseChat.get2ndUserInChatRoom(userID)

    @JvmStatic
    fun softDeleteChatRoom(chatRoomID: String, onSuccess: () -> Unit, onFailure: (Exception) -> Unit) =
        FirebaseChat.softDeleteChatRoom(chatRoomID, onSuccess, onFailure)

    @JvmStatic
    fun recoverChatRoom(chatRoomID: String, onSuccess: () -> Unit, onFailure: (Exception) -> Unit) =
        FirebaseChat.recoverChatRoom(chatRoomID, onSuccess, onFailure)

    @JvmStatic
    fun permanentlyDeleteChatRoom(chatRoomID: String, onSuccess: () -> Unit, onFailure: (Exception) -> Unit) =
        FirebaseChat.permanentlyDeleteChatRoom(chatRoomID, onSuccess, onFailure)

    @JvmStatic
    fun getActiveChatRoomsQuery() = FirebaseChat.getActiveChatRoomsQuery()

    @JvmStatic
    fun getDeletedChatRoomsQuery() = FirebaseChat.getDeletedChatRoomsQuery()

    // ========== FRIENDS ==========
    @JvmStatic
    fun friendRequestsCollection() = FirebaseFriends.friendRequestsCollection()

    @JvmStatic
    fun getFriendRequestReference(requestID: String) = FirebaseFriends.getFriendRequestReference(requestID)

    @JvmStatic
    fun generateFriendRequestID(userID1: String?, userID2: String?) =
        FirebaseFriends.generateFriendRequestID(userID1, userID2)

    @JvmStatic
    fun sendFriendRequest(receiverID: String, receiverName: String, onSuccess: () -> Unit, onFailure: (Exception) -> Unit) =
        FirebaseFriends.sendFriendRequest(receiverID, receiverName, onSuccess, onFailure)

    @JvmStatic
    fun acceptFriendRequest(senderID: String, onSuccess: () -> Unit, onFailure: (Exception) -> Unit) =
        FirebaseFriends.acceptFriendRequest(senderID, onSuccess, onFailure)

    @JvmStatic
    fun rejectFriendRequest(senderID: String, onSuccess: () -> Unit, onFailure: (Exception) -> Unit) =
        FirebaseFriends.rejectFriendRequest(senderID, onSuccess, onFailure)

    @JvmStatic
    fun cancelFriendRequest(receiverID: String, onSuccess: () -> Unit, onFailure: (Exception) -> Unit) =
        FirebaseFriends.cancelFriendRequest(receiverID, onSuccess, onFailure)

    @JvmStatic
    fun checkFriendshipStatus(otherUserID: String, onResult: (FirebaseFriends.FriendshipStatus) -> Unit) =
        FirebaseFriends.checkFriendshipStatus(otherUserID, onResult)

    @JvmStatic
    fun getPendingFriendRequests(onSuccess: (List<FriendRequestModel>) -> Unit, onFailure: (Exception) -> Unit) =
        FirebaseFriends.getPendingFriendRequests(onSuccess, onFailure)

    @JvmStatic
    fun getAllFriends(onSuccess: (List<String>) -> Unit, onFailure: (Exception) -> Unit) =
        FirebaseFriends.getAllFriends(onSuccess, onFailure)

    @JvmStatic
    fun removeFriend(friendID: String, onSuccess: () -> Unit, onFailure: (Exception) -> Unit) =
        FirebaseFriends.removeFriend(friendID, onSuccess, onFailure)

    // Use FirebaseFriends.FriendshipStatus directly
    typealias FriendshipStatus = FirebaseFriends.FriendshipStatus

    // ========== GROUPS ==========
    @JvmStatic
    fun allGroupsCollection() = FirebaseGroups.allGroupsCollection()

    @JvmStatic
    fun getGroupReference(groupID: String) = FirebaseGroups.getGroupReference(groupID)

    @JvmStatic
    fun getGroupMessagesReference(groupID: String) = FirebaseGroups.getGroupMessagesReference(groupID)

    @JvmStatic
    fun getUserGroupsQuery() = FirebaseGroups.getUserGroupsQuery()

    @JvmStatic
    fun groupJoinRequestsCollection() = FirebaseGroups.groupJoinRequestsCollection()

    @JvmStatic
    fun getGroupJoinRequestReference(requestID: String) = FirebaseGroups.getGroupJoinRequestReference(requestID)

    @JvmStatic
    fun sendGroupJoinRequest(groupID: String, groupName: String, onSuccess: () -> Unit, onFailure: (Exception) -> Unit) =
        FirebaseGroups.sendGroupJoinRequest(groupID, groupName, onSuccess, onFailure)

    @JvmStatic
    fun acceptGroupJoinRequest(requestID: String, groupID: String, userID: String, onSuccess: () -> Unit, onFailure: (Exception) -> Unit) =
        FirebaseGroups.acceptGroupJoinRequest(requestID, groupID, userID, onSuccess, onFailure)

    @JvmStatic
    fun rejectGroupJoinRequest(requestID: String, onSuccess: () -> Unit, onFailure: (Exception) -> Unit) =
        FirebaseGroups.rejectGroupJoinRequest(requestID, onSuccess, onFailure)

    @JvmStatic
    fun getPendingGroupJoinRequests(groupID: String, onSuccess: (List<GroupJoinRequestModel>) -> Unit, onFailure: (Exception) -> Unit) =
        FirebaseGroups.getPendingGroupJoinRequests(groupID, onSuccess, onFailure)

    @JvmStatic
    fun getAllPendingGroupJoinRequestsForAdmin(onSuccess: (List<GroupJoinRequestModel>) -> Unit, onFailure: (Exception) -> Unit) =
        FirebaseGroups.getAllPendingGroupJoinRequestsForAdmin(onSuccess, onFailure)

    // ========== BLOCKING ==========
    @JvmStatic
    fun blockUser(userID: String, onSuccess: () -> Unit, onFailure: (Exception) -> Unit) =
        FirebaseBlocking.blockUser(userID, onSuccess, onFailure)

    @JvmStatic
    fun unblockUser(userID: String, onSuccess: () -> Unit, onFailure: (Exception) -> Unit) =
        FirebaseBlocking.unblockUser(userID, onSuccess, onFailure)

    @JvmStatic
    fun isUserBlocked(userID: String, onResult: (Boolean) -> Unit) =
        FirebaseBlocking.isUserBlocked(userID, onResult)

    @JvmStatic
    fun isBlockedByUser(userID: String, onResult: (Boolean) -> Unit) =
        FirebaseBlocking.isBlockedByUser(userID, onResult)

    @JvmStatic
    fun blockUserFromGroup(groupID: String, userID: String, onSuccess: () -> Unit, onFailure: (Exception) -> Unit) =
        FirebaseBlocking.blockUserFromGroup(groupID, userID, onSuccess, onFailure)

    @JvmStatic
    fun unblockUserFromGroup(groupID: String, userID: String, onSuccess: () -> Unit, onFailure: (Exception) -> Unit) =
        FirebaseBlocking.unblockUserFromGroup(groupID, userID, onSuccess, onFailure)

    @JvmStatic
    fun getBlockedUsersFromGroup(groupID: String, onSuccess: (List<String>) -> Unit, onFailure: (Exception) -> Unit) =
        FirebaseBlocking.getBlockedUsersFromGroup(groupID, onSuccess, onFailure)

    @JvmStatic
    fun isBlockedFromGroup(groupID: String, userID: String, onResult: (Boolean) -> Unit) =
        FirebaseBlocking.isBlockedFromGroup(groupID, userID, onResult)

    // ========== COMMUNITY ==========
    @JvmStatic
    fun allCommunitiesCollection() = FirebaseCommunity.allCommunitiesCollection()

    @JvmStatic
    fun getCommunityReference(communityID: String) = FirebaseCommunity.getCommunityReference(communityID)

    @JvmStatic
    fun getCommunityMessagesReference(communityID: String) = FirebaseCommunity.getCommunityMessagesReference(communityID)

    @JvmStatic
    fun createCommunity(communityName: String, communityDescription: String, communityImage: String?, onSuccess: (String) -> Unit, onFailure: (Exception) -> Unit) =
        FirebaseCommunity.createCommunity(communityName, communityDescription, communityImage, onSuccess, onFailure)

    @JvmStatic
    fun banUserFromCommunity(communityID: String, userID: String, onSuccess: () -> Unit, onFailure: (Exception) -> Unit) =
        FirebaseCommunity.banUserFromCommunity(communityID, userID, onSuccess, onFailure)

    @JvmStatic
    fun unbanUserFromCommunity(communityID: String, userID: String, onSuccess: () -> Unit, onFailure: (Exception) -> Unit) =
        FirebaseCommunity.unbanUserFromCommunity(communityID, userID, onSuccess, onFailure)

    @JvmStatic
    fun isBannedFromCommunity(communityID: String, userID: String, onResult: (Boolean) -> Unit) =
        FirebaseCommunity.isBannedFromCommunity(communityID, userID, onResult)

    // ========== TEMPORARY CHAT ==========
    @JvmStatic
    fun allTemporaryChatsCollection() = FirebaseTemporaryChat.allTemporaryChatsCollection()

    @JvmStatic
    fun getTemporaryChatReference(chatID: String) = FirebaseTemporaryChat.getTemporaryChatReference(chatID)

    @JvmStatic
    fun getTemporaryChatMessagesReference(chatID: String) = FirebaseTemporaryChat.getTemporaryChatMessagesReference(chatID)

    @JvmStatic
    fun createTemporaryChat(friendID: String, onSuccess: (String, String) -> Unit, onFailure: (Exception) -> Unit) =
        FirebaseTemporaryChat.createTemporaryChat(friendID, onSuccess, onFailure)

    @JvmStatic
    fun markUserAsActiveInTempChat(chatID: String) =
        FirebaseTemporaryChat.markUserAsActiveInTempChat(chatID)

    @JvmStatic
    fun markUserAsInactiveInTempChat(chatID: String, onBothLeft: () -> Unit = {}) =
        FirebaseTemporaryChat.markUserAsInactiveInTempChat(chatID, onBothLeft)

    @JvmStatic
    fun deleteTemporaryChat(chatID: String) =
        FirebaseTemporaryChat.deleteTemporaryChat(chatID)

    @JvmStatic
    fun deleteExpiredTemporaryChats() =
        FirebaseTemporaryChat.deleteExpiredTemporaryChats()

    @JvmStatic
    fun getUserTemporaryChatsQuery() = FirebaseTemporaryChat.getUserTemporaryChatsQuery()

    // ========== NOTIFICATIONS ==========
    @JvmStatic
    fun notificationsCollection() = FirebaseNotifications.notificationsCollection()

    @JvmStatic
    fun createNotification(
        type: String,
        recipientID: String,
        senderID: String,
        senderName: String,
        groupID: String? = null,
        groupName: String? = null,
        communityID: String? = null,
        communityName: String? = null,
        message: String,
        onSuccess: () -> Unit = {},
        onFailure: (Exception) -> Unit = {}
    ) = FirebaseNotifications.createNotification(
        type, recipientID, senderID, senderName, groupID, groupName, communityID, communityName, message, onSuccess, onFailure
    )

    @JvmStatic
    fun getUserNotifications(userID: String, onSuccess: (List<NotificationModel>) -> Unit, onFailure: (Exception) -> Unit) =
        FirebaseNotifications.getUserNotifications(userID, onSuccess, onFailure)

    @JvmStatic
    fun markNotificationAsRead(notificationID: String, onSuccess: () -> Unit = {}, onFailure: (Exception) -> Unit = {}) =
        FirebaseNotifications.markNotificationAsRead(notificationID, onSuccess, onFailure)

    @JvmStatic
    fun deleteNotification(notificationID: String, onSuccess: () -> Unit = {}, onFailure: (Exception) -> Unit = {}) =
        FirebaseNotifications.deleteNotification(notificationID, onSuccess, onFailure)

    @JvmStatic
    fun getFriendRequestsCollection() = FirebaseFriends.friendRequestsCollection()

    @JvmStatic
    fun getGroupJoinRequestsCollection() = FirebaseGroups.groupJoinRequestsCollection()

    @JvmStatic
    fun getGroupsCollection() = FirebaseGroups.allGroupsCollection()
}