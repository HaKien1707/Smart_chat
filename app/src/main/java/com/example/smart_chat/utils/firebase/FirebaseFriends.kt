package com.example.smart_chat.utils.firebase

import com.example.smart_chat.models.request.FriendRequestModel
import com.example.smart_chat.models.userModel
import com.google.firebase.Timestamp
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore

object FirebaseFriends {
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
        val currentUserID = FirebaseAuthentication.currentUserID() ?: return

        FirebaseAuthentication.currentUserDetails().get().addOnSuccessListener { document ->
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

            getFriendRequestReference(requestID).set(request)
                .addOnSuccessListener { onSuccess() }
                .addOnFailureListener { onFailure(it) }
        }
    }

    @JvmStatic
    fun acceptFriendRequest(
        senderID: String,
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        val currentUserID = FirebaseAuthentication.currentUserID() ?: return
        val requestID = generateFriendRequestID(currentUserID, senderID)

        getFriendRequestReference(requestID).update("status", "accepted")
            .addOnSuccessListener {
                // Send notification
                FirebaseAuthentication.currentUserDetails().get().addOnSuccessListener { doc ->
                    val currentUser = doc.toObject(userModel::class.java)
                    FirebaseNotifications.createNotification(
                        type = "FRIEND_REQUEST_ACCEPTED",
                        recipientID = senderID,
                        senderID = currentUserID,
                        senderName = currentUser?.username ?: "Someone",
                        message = "${currentUser?.username} accepted your friend request"
                    )
                }
                onSuccess()
            }
            .addOnFailureListener { onFailure(it) }
    }

    @JvmStatic
    fun rejectFriendRequest(
        senderID: String,
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        val currentUserID = FirebaseAuthentication.currentUserID() ?: return
        val requestID = generateFriendRequestID(currentUserID, senderID)

        getFriendRequestReference(requestID).update("status", "rejected")
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onFailure(it) }
    }

    @JvmStatic
    fun cancelFriendRequest(
        receiverID: String,
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        val currentUserID = FirebaseAuthentication.currentUserID() ?: return
        val requestID = generateFriendRequestID(currentUserID, receiverID)

        getFriendRequestReference(requestID).delete()
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onFailure(it) }
    }

    @JvmStatic
    fun checkFriendshipStatus(
        otherUserID: String,
        onResult: (FriendshipStatus) -> Unit
    ) {
        val currentUserID = FirebaseAuthentication.currentUserID() ?: return
        val requestID = generateFriendRequestID(currentUserID, otherUserID)

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

    @JvmStatic
    fun getPendingFriendRequests(
        onSuccess: (List<FriendRequestModel>) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        val currentUserID = FirebaseAuthentication.currentUserID() ?: return

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
            .addOnFailureListener { onFailure(it) }
    }

    @JvmStatic
    fun getAllFriends(
        onSuccess: (List<String>) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        val currentUserID = FirebaseAuthentication.currentUserID() ?: return
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
            .addOnFailureListener { onFailure(it) }

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
            .addOnFailureListener { onFailure(it) }
    }

    @JvmStatic
    fun removeFriend(
        friendID: String,
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        val currentUserID = FirebaseAuthentication.currentUserID() ?: return
        val requestID = generateFriendRequestID(currentUserID, friendID)

        getFriendRequestReference(requestID).delete()
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onFailure(it) }
    }

    enum class FriendshipStatus {
        FRIENDS,
        REQUEST_SENT,
        REQUEST_RECEIVED,
        NOT_FRIENDS
    }
}