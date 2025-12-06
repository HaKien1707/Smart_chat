package com.example.Smart_Chat.utils

import com.example.Smart_Chat.models.FriendRequestModel
import com.example.Smart_Chat.models.userModel
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentReference
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

    @JvmStatic
    fun deleteChatRoom(
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
}