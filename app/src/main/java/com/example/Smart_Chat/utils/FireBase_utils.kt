package com.example.Smart_Chat.utils

import android.util.Log
import com.example.Smart_Chat.models.CommunityModel
import com.example.Smart_Chat.models.FriendRequestModel
import com.example.Smart_Chat.models.GroupJoinRequestModel
import com.example.Smart_Chat.models.groupModel
import com.example.Smart_Chat.models.userModel
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

object FireBase_utils {
    @JvmStatic
    fun currentUserID(): String? {
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser != null) {
            return currentUser.uid
        }
        return null // Return null instead of crashing
    }

    @JvmStatic
    fun currentUserDetails(): DocumentReference {
        return FirebaseFirestore.getInstance().collection("users").document(currentUserID()!!)
    }

    @JvmStatic
    fun allUsersCollection(): CollectionReference {
        return FirebaseFirestore.getInstance().collection("users")
    }

    @JvmStatic
    fun getChatRoomReferences(chatRoomID: String): DocumentReference {
        return FirebaseFirestore.getInstance().collection("chatRooms").document(chatRoomID)
    }

    @JvmStatic
    fun getChatRoomID(userID1: String?, userID2: String?): String {
        return if (userID1.hashCode() < userID2.hashCode()) {
            userID1 + "_" + userID2
        } else {
            userID2 + "_" + userID1
        }
    }

    @JvmStatic
    fun getChatRoomMessagesReferences(chatRoomID: String): CollectionReference {
        return getChatRoomReferences(chatRoomID).collection("messages")
    }

    @JvmStatic
    fun allChatRoomsCollectionReference(): CollectionReference {
        return FirebaseFirestore.getInstance().collection("chatRooms")
    }

    fun get2ndUserInChatRoom(userID: MutableList<String?>?): DocumentReference? {
        // First, check if the list is not null and contains at least two user IDs.
        if (userID != null && userID.size >= 2) {
            // Now that we've confirmed the list is not null, we can safely access its elements.
            val otherUserId = if (userID[0] == currentUserID()) {
                userID[1]
            } else {
                userID[0]
            }

            // It's also a good practice to ensure the other user's ID is not null before creating the document reference.
            return otherUserId?.let { allUsersCollection().document(it) }
        }
        // If the list is null or doesn't have two users, we return null.
        return null
    }

    // ========== GROUP CHAT FUNCTIONS ==========

    @JvmStatic
    fun allGroupsCollection(): CollectionReference {
        return FirebaseFirestore.getInstance().collection("chatgroups")
    }

    @JvmStatic
    fun getGroupReference(groupID: String): DocumentReference {
        return allGroupsCollection().document(groupID)
    }

    @JvmStatic
    fun getGroupMessagesReference(groupID: String): CollectionReference {
        return getGroupReference(groupID).collection("messages")
    }

    @JvmStatic
    fun getUserGroupsQuery(): Query {
        return allGroupsCollection()
            .whereArrayContains("memberIDs", currentUserID()!!)
            .orderBy("lastMsgTimestamp", Query.Direction.DESCENDING)
    }


    // ========== log - sign ==========
    @JvmStatic
    fun logout() {
        FirebaseAuth.getInstance().signOut()
    }

    @JvmStatic
    val isLoggedIn: Boolean
        get() = currentUserID() != null

    // ========== FRIEND SYSTEM FUNCTIONS ==========

    @JvmStatic
    fun friendRequestsCollection(): CollectionReference {
        return FirebaseFirestore.getInstance().collection("friendRequests")
    }

    @JvmStatic
    fun getFriendRequestReference(requestID: String): DocumentReference {
        return friendRequestsCollection().document(requestID)
    }

    @JvmStatic
    fun generateFriendRequestID(userID1: String?, userID2: String?): String {
        // Generate consistent ID for friend request
        return if (userID1.hashCode() < userID2.hashCode()) {
            "${userID1}_${userID2}"
        } else {
            "${userID2}_${userID1}"
        }
    }

    @JvmStatic
    fun sendFriendRequest(
        receiverID: String,
        receiverName: String,
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        val currentUserID = currentUserID() ?: return

        currentUserDetails().get().addOnSuccessListener { document ->
            val currentUser = document.toObject(userModel::class.java)
            val requestID = generateFriendRequestID(currentUserID, receiverID)

            val request = FriendRequestModel(
                requestID,
                currentUserID,
                currentUser?.username,
                receiverID,
                receiverName,
                "pending",
                Timestamp.now()
            )

            // ONLY write to friendRequests collection
            getFriendRequestReference(requestID).set(request)
                .addOnSuccessListener {
                    onSuccess()
                }
                .addOnFailureListener { e ->
                    onFailure(e)
                }
        }
    }

    @JvmStatic
    fun acceptFriendRequest(
        senderID: String,
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        val currentUserID = currentUserID() ?: return
        val requestID = generateFriendRequestID(currentUserID, senderID)

        // Simply update request status to "accepted"
        getFriendRequestReference(requestID).update("status", "accepted")
            .addOnSuccessListener {
                onSuccess()
            }
            .addOnFailureListener { e ->
                onFailure(e)
            }
    }

    @JvmStatic
    fun rejectFriendRequest(
        senderID: String,
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        val currentUserID = currentUserID() ?: return
        val requestID = generateFriendRequestID(currentUserID, senderID)

        // Update request status to "rejected"
        getFriendRequestReference(requestID).update("status", "rejected")
            .addOnSuccessListener {
                onSuccess()
            }
            .addOnFailureListener { e ->
                onFailure(e)
            }
    }

    @JvmStatic
    fun cancelFriendRequest(
        receiverID: String,
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        val currentUserID = currentUserID() ?: return
        val requestID = generateFriendRequestID(currentUserID, receiverID)

        // Delete the request
        getFriendRequestReference(requestID).delete()
            .addOnSuccessListener {
                onSuccess()
            }
            .addOnFailureListener { e ->
                onFailure(e)
            }
    }

    @JvmStatic
    fun checkFriendshipStatus(
        otherUserID: String,
        onResult: (FriendshipStatus) -> Unit
    ) {
        val currentUserID = currentUserID() ?: return
        val requestID = generateFriendRequestID(currentUserID, otherUserID)

        // Query the friend request document
        getFriendRequestReference(requestID).get()
            .addOnSuccessListener { document ->
                if (!document.exists()) {
                    onResult(FriendshipStatus.NOT_FRIENDS)
                    return@addOnSuccessListener
                }

                val request = document.toObject(FriendRequestModel::class.java)

                when (request?.status) {
                    "accepted" -> onResult(FriendshipStatus.FRIENDS)
                    "pending" -> {
                        // Check if current user is sender or receiver
                        if (request.senderID == currentUserID) {
                            onResult(FriendshipStatus.REQUEST_SENT)
                        } else {
                            onResult(FriendshipStatus.REQUEST_RECEIVED)
                        }
                    }
                    else -> onResult(FriendshipStatus.NOT_FRIENDS)
                }
            }
            .addOnFailureListener {
                onResult(FriendshipStatus.NOT_FRIENDS)
            }
    }

    // Get all pending friend requests where current user is the receiver
    @JvmStatic
    fun getPendingFriendRequests(
        onSuccess: (List<FriendRequestModel>) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        val currentUserID = currentUserID() ?: return

        friendRequestsCollection()
            .whereEqualTo("receiverID", currentUserID)
            .whereEqualTo("status", "pending")
            .get()
            .addOnSuccessListener { documents ->
                val requests = documents.mapNotNull {
                    it.toObject(FriendRequestModel::class.java)
                }
                onSuccess(requests)
            }
            .addOnFailureListener { e ->
                onFailure(e)
            }
    }

    // Get all friends (accepted requests where user is sender or receiver)
    @JvmStatic
    fun getAllFriends(
        onSuccess: (List<String>) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        val currentUserID = currentUserID() ?: return
        val friendIDs = mutableSetOf<String>()
        var completedQueries = 0

        // Query 1: Get accepted requests where I'm the SENDER
        friendRequestsCollection()
            .whereEqualTo("senderID", currentUserID)
            .whereEqualTo("status", "accepted")
            .get()
            .addOnSuccessListener { documents ->
                documents.forEach { doc ->
                    val request = doc.toObject(FriendRequestModel::class.java)
                    request.receiverID?.let { friendIDs.add(it) }
                }

                completedQueries++
                if (completedQueries == 2) {
                    onSuccess(friendIDs.toList())
                }
            }
            .addOnFailureListener { e ->
                onFailure(e)
            }

        // Query 2: Get accepted requests where I'm the RECEIVER
        friendRequestsCollection()
            .whereEqualTo("receiverID", currentUserID)
            .whereEqualTo("status", "accepted")
            .get()
            .addOnSuccessListener { documents ->
                documents.forEach { doc ->
                    val request = doc.toObject(FriendRequestModel::class.java)
                    request.senderID?.let { friendIDs.add(it) }
                }

                completedQueries++
                if (completedQueries == 2) {
                    onSuccess(friendIDs.toList())
                }
            }
            .addOnFailureListener { e ->
                onFailure(e)
            }
    }

    @JvmStatic
    fun removeFriend(
        friendID: String,
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        val currentUserID = currentUserID() ?: return
        val requestID = generateFriendRequestID(currentUserID, friendID)

        // Delete the friend request document
        getFriendRequestReference(requestID).delete()
            .addOnSuccessListener {
                onSuccess()
            }
            .addOnFailureListener { e ->
                onFailure(e)
            }
    }
    //===================== Handle Delete Chat Room ====================
    @JvmStatic
    fun softDeleteChatRoom(
        chatRoomID: String,
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        val currentUserID = currentUserID() ?: return

        getChatRoomReferences(chatRoomID)
            .update(
                "deletedBy",
                com.google.firebase.firestore.FieldValue.arrayUnion(currentUserID)
            )
            .addOnSuccessListener {
                onSuccess()
            }
            .addOnFailureListener { e ->
                onFailure(e)
            }
    }

    @JvmStatic
    fun recoverChatRoom(
        chatRoomID: String,
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        val currentUserID = currentUserID() ?: return

        getChatRoomReferences(chatRoomID)
            .update(
                "deletedBy",
                com.google.firebase.firestore.FieldValue.arrayRemove(currentUserID)
            )
            .addOnSuccessListener {
                onSuccess()
            }
            .addOnFailureListener { e ->
                onFailure(e)
            }
    }

    @JvmStatic
    fun permanentlyDeleteChatRoom(
        chatRoomID: String,
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        val chatRoomRef = getChatRoomReferences(chatRoomID)

        // First delete all messages in the chatroom
        getChatRoomMessagesReferences(chatRoomID)
            .get()
            .addOnSuccessListener { messages ->
                val batch = FirebaseFirestore.getInstance().batch()

                // Add all message deletions to batch
                messages.documents.forEach { doc ->
                    batch.delete(doc.reference)
                }

                // Add chatroom deletion to batch
                batch.delete(chatRoomRef)

                // Commit batch delete
                batch.commit()
                    .addOnSuccessListener {
                        onSuccess()
                    }
                    .addOnFailureListener { e ->
                        onFailure(e)
                    }
            }
            .addOnFailureListener { e ->
                onFailure(e)
            }
    }

    // Query for active (non-deleted) chats
    @JvmStatic
    fun getActiveChatRoomsQuery(): Query {
        val currentUserID = currentUserID() ?: return allChatRoomsCollectionReference()
            .whereArrayContains("userID", "")

        return allChatRoomsCollectionReference()
            .whereArrayContains("userID", currentUserID)
            .orderBy("lastMsgTimestamp", Query.Direction.DESCENDING)
    }

    // Query for deleted chats
    @JvmStatic
    fun getDeletedChatRoomsQuery(): Query {
        val currentUserID = currentUserID() ?: return allChatRoomsCollectionReference()
            .whereArrayContains("userID", "")

        return allChatRoomsCollectionReference()
            .whereArrayContains("deletedBy", currentUserID)
            .orderBy("lastMsgTimestamp", Query.Direction.DESCENDING)
    }

    // ========== USER BLOCK FUNCTIONS ==========
    @JvmStatic
    fun blockUser(
        userID: String,
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        val currentUserID = currentUserID() ?: return

        // Remove friend if they are friends
        val requestID = generateFriendRequestID(currentUserID, userID)
        getFriendRequestReference(requestID).delete()

        // Add to blocked list
        currentUserDetails().update(
            "blockedUsers",
            com.google.firebase.firestore.FieldValue.arrayUnion(userID)
        )
            .addOnSuccessListener {
                onSuccess()
            }
            .addOnFailureListener { e ->
                onFailure(e)
            }
    }

    @JvmStatic
    fun unblockUser(
        userID: String,
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        currentUserDetails().update(
            "blockedUsers",
            com.google.firebase.firestore.FieldValue.arrayRemove(userID)
        )
            .addOnSuccessListener {
                onSuccess()
            }
            .addOnFailureListener { e ->
                onFailure(e)
            }
    }

    @JvmStatic
    fun isUserBlocked(
        userID: String,
        onResult: (Boolean) -> Unit
    ) {
        currentUserDetails().get()
            .addOnSuccessListener { document ->
                val user = document.toObject(userModel::class.java)
                val isBlocked = user?.blockedUsers?.contains(userID) == true
                onResult(isBlocked)
            }
            .addOnFailureListener {
                onResult(false)
            }
    }

    @JvmStatic
    fun isBlockedByUser(
        userID: String,
        onResult: (Boolean) -> Unit
    ) {
        allUsersCollection().document(userID).get()
            .addOnSuccessListener { document ->
                val user = document.toObject(userModel::class.java)
                val isBlockedBy = user?.blockedUsers?.contains(currentUserID()) == true
                onResult(isBlockedBy)
            }
            .addOnFailureListener {
                onResult(false)
            }
    }

    enum class FriendshipStatus {
        FRIENDS,
        REQUEST_SENT,
        REQUEST_RECEIVED,
        NOT_FRIENDS
    }

    // ========== GROUP BLOCK FUNCTIONS ==========

    @JvmStatic
    fun blockUserFromGroup(
        groupID: String,
        userID: String,
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        val batch = FirebaseFirestore.getInstance().batch()
        val groupRef = getGroupReference(groupID)

        // Add to blocked list
        batch.update(groupRef, "blockedUserIDs",
            com.google.firebase.firestore.FieldValue.arrayUnion(userID))

        // Remove from members
        batch.update(groupRef, "memberIDs",
            com.google.firebase.firestore.FieldValue.arrayRemove(userID))

        // Remove from admins if they are admin
        batch.update(groupRef, "adminIDs",
            com.google.firebase.firestore.FieldValue.arrayRemove(userID))

        batch.commit()
            .addOnSuccessListener {
                onSuccess()
            }
            .addOnFailureListener { e ->
                onFailure(e)
            }
    }

    @JvmStatic
    fun unblockUserFromGroup(
        groupID: String,
        userID: String,
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        getGroupReference(groupID)
            .update("blockedUserIDs",
                com.google.firebase.firestore.FieldValue.arrayRemove(userID))
            .addOnSuccessListener {
                onSuccess()
            }
            .addOnFailureListener { e ->
                onFailure(e)
            }
    }

    @JvmStatic
    fun isBlockedFromGroup(
        groupID: String,
        userID: String,
        onResult: (Boolean) -> Unit
    ) {
        getGroupReference(groupID).get()
            .addOnSuccessListener { document ->
                val group = document.toObject(groupModel::class.java)
                val isBlocked = group?.blockedUserIDs?.contains(userID) == true
                onResult(isBlocked)
            }
            .addOnFailureListener {
                onResult(false)
            }
    }

    // ========== GROUP JOIN REQUEST FUNCTIONS ==========
    @JvmStatic
    fun groupJoinRequestsCollection(): CollectionReference {
        return FirebaseFirestore.getInstance().collection("groupJoinRequests")
    }

    @JvmStatic
    fun getGroupJoinRequestReference(requestID: String): DocumentReference {
        return groupJoinRequestsCollection().document(requestID)
    }

    @JvmStatic
    fun sendGroupJoinRequest(
        groupID: String,
        groupName: String,
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        val currentUserID = currentUserID() ?: return

        Log.d("GROUP_JOIN_UTIL", "=== sendGroupJoinRequest called ===")
        Log.d("GROUP_JOIN_UTIL", "Current User ID: $currentUserID")
        Log.d("GROUP_JOIN_UTIL", "Group ID: $groupID")

        // Check if blocked first
        isBlockedFromGroup(groupID, currentUserID) { isBlocked ->
            Log.d("GROUP_JOIN_UTIL", "Is blocked: $isBlocked")

            if (isBlocked) {
                onFailure(Exception("You are blocked from this group"))
                return@isBlockedFromGroup
            }

            // Check if already a member of the group
            getGroupReference(groupID).get()
                .addOnSuccessListener { groupDoc ->
                    val group = groupDoc.toObject(groupModel::class.java)

                    if (group?.memberIDs?.contains(currentUserID) == true) {
                        onFailure(Exception("You are already a member of this group"))
                        return@addOnSuccessListener
                    }

                    // Check if already sent request
                    val requestID = "${groupID}_${currentUserID}"
                    Log.d("GROUP_JOIN_UTIL", "Request ID: $requestID")

                    getGroupJoinRequestReference(requestID).get()
                        .addOnSuccessListener { document ->
                            Log.d("GROUP_JOIN_UTIL", "Document exists: ${document.exists()}")

                            if (document.exists()) {
                                val request = document.toObject(GroupJoinRequestModel::class.java)
                                Log.d("GROUP_JOIN_UTIL", "Request status: ${request?.status}")

                                when (request?.status) {
                                    "pending" -> {
                                        onFailure(Exception("Request already sent"))
                                        return@addOnSuccessListener
                                    }
                                    "accepted" -> {
                                        // Previous request was accepted, but user might have been removed
                                        // Delete old request and create new one
                                        Log.d("GROUP_JOIN_UTIL", "Deleting old accepted request")
                                        getGroupJoinRequestReference(requestID).delete()
                                            .addOnSuccessListener {
                                                // Now create new request
                                                createNewJoinRequest(
                                                    requestID,
                                                    groupID,
                                                    groupName,
                                                    currentUserID,
                                                    onSuccess,
                                                    onFailure
                                                )
                                            }
                                            .addOnFailureListener { e ->
                                                Log.e("GROUP_JOIN_UTIL", "Failed to delete old request", e)
                                                onFailure(e)
                                            }
                                        return@addOnSuccessListener
                                    }
                                    "rejected" -> {
                                        // Previous request was rejected, delete and create new one
                                        Log.d("GROUP_JOIN_UTIL", "Deleting old rejected request")
                                        getGroupJoinRequestReference(requestID).delete()
                                            .addOnSuccessListener {
                                                createNewJoinRequest(
                                                    requestID,
                                                    groupID,
                                                    groupName,
                                                    currentUserID,
                                                    onSuccess,
                                                    onFailure
                                                )
                                            }
                                            .addOnFailureListener { e ->
                                                Log.e("GROUP_JOIN_UTIL", "Failed to delete old request", e)
                                                onFailure(e)
                                            }
                                        return@addOnSuccessListener
                                    }
                                }
                            }

                            // No existing request, create new one
                            createNewJoinRequest(
                                requestID,
                                groupID,
                                groupName,
                                currentUserID,
                                onSuccess,
                                onFailure
                            )
                        }
                        .addOnFailureListener { e ->
                            Log.e("GROUP_JOIN_UTIL", "❌ Failed to check existing request", e)
                            onFailure(e)
                        }
                }
                .addOnFailureListener { e ->
                    Log.e("GROUP_JOIN_UTIL", "Failed to check group membership", e)
                    onFailure(e)
                }
        }
    }

    // Helper function to create a new join request
    private fun createNewJoinRequest(
        requestID: String,
        groupID: String,
        groupName: String,
        currentUserID: String,
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        currentUserDetails().get().addOnSuccessListener { userDoc ->
            val currentUser = userDoc.toObject(userModel::class.java)
            Log.d("GROUP_JOIN_UTIL", "Current user name: ${currentUser?.username}")

            val request = GroupJoinRequestModel(
                requestID,
                groupID,
                groupName,
                currentUserID,
                currentUser?.username,
                "pending",
                Timestamp.now()
            )

            Log.d("GROUP_JOIN_UTIL", "About to create request")

            getGroupJoinRequestReference(requestID).set(request)
                .addOnSuccessListener {
                    Log.d("GROUP_JOIN_UTIL", "✅ Request created successfully!")
                    onSuccess()
                }
                .addOnFailureListener { e ->
                    Log.e("GROUP_JOIN_UTIL", "❌ Failed to create request", e)
                    onFailure(e)
                }
        }.addOnFailureListener { e ->
            Log.e("GROUP_JOIN_UTIL", "❌ Failed to get user details", e)
            onFailure(e)
        }
    }

    @JvmStatic
    fun acceptGroupJoinRequest(
        requestID: String,
        groupID: String,
        userID: String,
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        // Update request status
        getGroupJoinRequestReference(requestID).update("status", "accepted")
            .addOnSuccessListener {
                // Add user to group
                getGroupReference(groupID).update(
                    "memberIDs",
                    com.google.firebase.firestore.FieldValue.arrayUnion(userID)
                )
                    .addOnSuccessListener {
                        onSuccess()
                    }
                    .addOnFailureListener { e ->
                        onFailure(e)
                    }
            }
            .addOnFailureListener { e ->
                onFailure(e)
            }
    }

    @JvmStatic
    fun rejectGroupJoinRequest(
        requestID: String,
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        getGroupJoinRequestReference(requestID).update("status", "rejected")
            .addOnSuccessListener {
                onSuccess()
            }
            .addOnFailureListener { e ->
                onFailure(e)
            }
    }

    @JvmStatic
    fun getPendingGroupJoinRequests(
        groupID: String,
        onSuccess: (List<GroupJoinRequestModel>) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        groupJoinRequestsCollection()
            .whereEqualTo("groupID", groupID)
            .whereEqualTo("status", "pending")
            .get()
            .addOnSuccessListener { documents ->
                val requests = documents.mapNotNull {
                    it.toObject(GroupJoinRequestModel::class.java)
                }
                onSuccess(requests)
            }
            .addOnFailureListener { e ->
                onFailure(e)
            }
    }

    @JvmStatic
    fun getAllPendingGroupJoinRequestsForAdmin(
        onSuccess: (List<GroupJoinRequestModel>) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        val currentUserID = currentUserID() ?: return

        // Get all groups where user is admin
        allGroupsCollection()
            .whereArrayContains("adminIDs", currentUserID)
            .get()
            .addOnSuccessListener { groupDocs ->
                val groupIDs = groupDocs.map { it.id }

                if (groupIDs.isEmpty()) {
                    onSuccess(emptyList())
                    return@addOnSuccessListener
                }

                // Get all pending requests for these groups
                groupJoinRequestsCollection()
                    .whereIn("groupID", groupIDs)
                    .whereEqualTo("status", "pending")
                    .get()
                    .addOnSuccessListener { requestDocs ->
                        val requests = requestDocs.mapNotNull {
                            it.toObject(GroupJoinRequestModel::class.java)
                        }
                        onSuccess(requests)
                    }
                    .addOnFailureListener { e ->
                        onFailure(e)
                    }
            }
            .addOnFailureListener { e ->
                onFailure(e)
            }
    }

    // ========== COMMUNITY FUNCTIONS ==========

    @JvmStatic
    fun allCommunitiesCollection(): CollectionReference {
        return FirebaseFirestore.getInstance().collection("communities")
    }

    @JvmStatic
    fun getCommunityReference(communityID: String): DocumentReference {
        return allCommunitiesCollection().document(communityID)
    }

    @JvmStatic
    fun getCommunityMessagesReference(communityID: String): CollectionReference {
        return getCommunityReference(communityID).collection("messages")
    }

    @JvmStatic
    fun createCommunity(
        communityName: String,
        communityDescription: String,
        communityImage: String?,
        onSuccess: (String) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        val currentUserID = currentUserID() ?: return
        val communityID = allCommunitiesCollection().document().id

        val community = CommunityModel(
            communityID,
            communityName,
            communityDescription,
            communityImage,
            currentUserID,
            Timestamp.now()
        )

        getCommunityReference(communityID).set(community)
            .addOnSuccessListener {
                onSuccess(communityID)
            }
            .addOnFailureListener { e ->
                onFailure(e)
            }
    }

    @JvmStatic
    fun banUserFromCommunity(
        communityID: String,
        userID: String,
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        getCommunityReference(communityID)
            .update("bannedUserIDs", FieldValue.arrayUnion(userID))
            .addOnSuccessListener {
                onSuccess()
            }
            .addOnFailureListener { e ->
                onFailure(e)
            }
    }

    @JvmStatic
    fun unbanUserFromCommunity(
        communityID: String,
        userID: String,
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        getCommunityReference(communityID)
            .update("bannedUserIDs", FieldValue.arrayRemove(userID))
            .addOnSuccessListener {
                onSuccess()
            }
            .addOnFailureListener { e ->
                onFailure(e)
            }
    }

    @JvmStatic
    fun isBannedFromCommunity(
        communityID: String,
        userID: String,
        onResult: (Boolean) -> Unit
    ) {
        getCommunityReference(communityID).get()
            .addOnSuccessListener { document ->
                val community = document.toObject(CommunityModel::class.java)
                val isBanned = community?.bannedUserIDs?.contains(userID) == true
                onResult(isBanned)
            }
            .addOnFailureListener {
                onResult(false)
            }
    }
}