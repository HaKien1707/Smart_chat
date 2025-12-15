package com.example.Smart_Chat.utils.firebase

import com.example.Smart_Chat.models.groupModel
import com.example.Smart_Chat.models.userModel
import com.google.firebase.firestore.FieldValue

object FirebaseBlocking {
    // ========== USER BLOCKING ==========
    @JvmStatic
    fun blockUser(
        userID: String,
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        val currentUserID = FirebaseAuth.currentUserID() ?: return

        // Remove friend if they are friends
        val requestID = FirebaseFriends.generateFriendRequestID(currentUserID, userID)
        FirebaseFriends.getFriendRequestReference(requestID).delete()

        // Add to blocked list
        FirebaseAuth.currentUserDetails().update(
            "blockedUsers",
            FieldValue.arrayUnion(userID)
        )
            .addOnSuccessListener {
                // Send notification
                FirebaseAuth.currentUserDetails().get().addOnSuccessListener { doc ->
                    val currentUser = doc.toObject(userModel::class.java)
                    FirebaseNotifications.createNotification(
                        type = "BLOCKED_BY_USER",
                        recipientID = userID,
                        senderID = currentUserID,
                        senderName = currentUser?.username ?: "Someone",
                        message = "${currentUser?.username} has blocked you"
                    )
                }
                onSuccess()
            }
            .addOnFailureListener { onFailure(it) }
    }

    @JvmStatic
    fun unblockUser(
        userID: String,
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        FirebaseAuth.currentUserDetails().update(
            "blockedUsers",
            FieldValue.arrayRemove(userID)
        )
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onFailure(it) }
    }

    @JvmStatic
    fun isUserBlocked(
        userID: String,
        onResult: (Boolean) -> Unit
    ) {
        FirebaseAuth.currentUserDetails().get()
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
        FirebaseAuth.allUsersCollection().document(userID).get()
            .addOnSuccessListener { document ->
                val user = document.toObject(userModel::class.java)
                val isBlockedBy = user?.blockedUsers?.contains(FirebaseAuth.currentUserID()) == true
                onResult(isBlockedBy)
            }
            .addOnFailureListener {
                onResult(false)
            }
    }

    // ========== GROUP BLOCKING ==========

    @JvmStatic
    fun blockUserFromGroup(
        groupID: String,
        userID: String,
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        val groupRef = FirebaseGroups.getGroupReference(groupID)

        // Get group info for notification
        groupRef.get().addOnSuccessListener { groupDoc ->
            val group = groupDoc.toObject(groupModel::class.java)

            // Add to blocked list and remove from members
            groupRef.update(
                mapOf(
                    "blockedUserIDs" to FieldValue.arrayUnion(userID),
                    "memberIDs" to FieldValue.arrayRemove(userID)
                )
            ).addOnSuccessListener {
                // Send notification
                FirebaseNotifications.createNotification(
                    type = "BLOCKED_FROM_GROUP",
                    recipientID = userID,
                    senderID = FirebaseAuth.currentUserID() ?: "",
                    senderName = "Admin",
                    groupID = groupID,
                    groupName = group?.groupName,
                    message = "You have been blocked from ${group?.groupName}"
                )
                onSuccess()
            }.addOnFailureListener { onFailure(it) }
        }.addOnFailureListener { onFailure(it) }
    }

    @JvmStatic
    fun unblockUserFromGroup(
        groupID: String,
        userID: String,
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        val groupRef = FirebaseGroups.getGroupReference(groupID)

        // Get group info for notification
        groupRef.get().addOnSuccessListener { groupDoc ->
            val group = groupDoc.toObject(groupModel::class.java)

            // Remove from blocked list
            groupRef.update("blockedUserIDs", FieldValue.arrayRemove(userID))
                .addOnSuccessListener {
                    // Send notification
                    FirebaseNotifications.createNotification(
                        type = "UNBLOCKED_FROM_GROUP",
                        recipientID = userID,
                        senderID = FirebaseAuth.currentUserID() ?: "",
                        senderName = "Admin",
                        groupID = groupID,
                        groupName = group?.groupName,
                        message = "You have been unblocked from ${group?.groupName}"
                    )
                    onSuccess()
                }
                .addOnFailureListener { onFailure(it) }
        }.addOnFailureListener { onFailure(it) }
    }

    @JvmStatic
    fun getBlockedUsersFromGroup(
        groupID: String,
        onSuccess: (List<String>) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        FirebaseGroups.getGroupReference(groupID).get()
            .addOnSuccessListener { document ->
                val group = document.toObject(groupModel::class.java)
                val blockedIDs = group?.blockedUserIDs?.mapNotNull { it } ?: emptyList()
                onSuccess(blockedIDs)
            }
            .addOnFailureListener { onFailure(it) }
    }

    @JvmStatic
    fun isBlockedFromGroup(
        groupID: String,
        userID: String,
        onResult: (Boolean) -> Unit
    ) {
        FirebaseGroups.getGroupReference(groupID).get()
            .addOnSuccessListener { document ->
                val group = document.toObject(groupModel::class.java)
                val isBlocked = group?.blockedUserIDs?.contains(userID) == true
                onResult(isBlocked)
            }
            .addOnFailureListener {
                onResult(false)
            }
    }
}