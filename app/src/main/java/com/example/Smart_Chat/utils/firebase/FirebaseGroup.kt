package com.example.Smart_Chat.utils.firebase

import android.util.Log
import com.example.Smart_Chat.models.GroupJoinRequestModel
import com.example.Smart_Chat.models.groupModel
import com.example.Smart_Chat.models.userModel
import com.google.firebase.Timestamp
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

object FirebaseGroups {
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
            .whereArrayContains("memberIDs", FirebaseAuth.currentUserID()!!)
            .orderBy("lastMsgTimestamp", Query.Direction.DESCENDING)
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
        val currentUserID = FirebaseAuth.currentUserID() ?: return

        Log.d("GROUP_JOIN", "=== sendGroupJoinRequest called ===")
        Log.d("GROUP_JOIN", "Current User ID: $currentUserID")
        Log.d("GROUP_JOIN", "Group ID: $groupID")

        // Check if blocked first
        FirebaseBlocking.isBlockedFromGroup(groupID, currentUserID) { isBlocked ->
            Log.d("GROUP_JOIN", "Is blocked: $isBlocked")

            if (isBlocked) {
                onFailure(Exception("You are blocked from this group"))
                return@isBlockedFromGroup
            }

            // Check if already a member
            getGroupReference(groupID).get()
                .addOnSuccessListener { groupDoc ->
                    val group = groupDoc.toObject(groupModel::class.java)

                    if (group?.memberIDs?.contains(currentUserID) == true) {
                        onFailure(Exception("You are already a member of this group"))
                        return@addOnSuccessListener
                    }

                    // Check if already sent request
                    val requestID = "${groupID}_${currentUserID}"
                    Log.d("GROUP_JOIN", "Request ID: $requestID")

                    getGroupJoinRequestReference(requestID).get()
                        .addOnSuccessListener { document ->
                            Log.d("GROUP_JOIN", "Document exists: ${document.exists()}")

                            if (document.exists()) {
                                val request = document.toObject(GroupJoinRequestModel::class.java)
                                Log.d("GROUP_JOIN", "Request status: ${request?.status}")

                                when (request?.status) {
                                    "pending" -> {
                                        onFailure(Exception("Request already sent"))
                                        return@addOnSuccessListener
                                    }
                                    "accepted", "rejected" -> {
                                        // Delete old request and create new one
                                        Log.d("GROUP_JOIN", "Deleting old request")
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
                                                Log.e("GROUP_JOIN", "Failed to delete old request", e)
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
                            Log.e("GROUP_JOIN", "Failed to check existing request", e)
                            onFailure(e)
                        }
                }
                .addOnFailureListener { e ->
                    Log.e("GROUP_JOIN", "Failed to check group membership", e)
                    onFailure(e)
                }
        }
    }

    private fun createNewJoinRequest(
        requestID: String,
        groupID: String,
        groupName: String,
        currentUserID: String,
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        FirebaseAuth.currentUserDetails().get().addOnSuccessListener { userDoc ->
            val currentUser = userDoc.toObject(userModel::class.java)
            Log.d("GROUP_JOIN", "Current user name: ${currentUser?.username}")

            val request = GroupJoinRequestModel(
                requestID,
                groupID,
                groupName,
                currentUserID,
                currentUser?.username,
                "pending",
                Timestamp.now()
            )

            Log.d("GROUP_JOIN", "About to create request")

            getGroupJoinRequestReference(requestID).set(request)
                .addOnSuccessListener {
                    Log.d("GROUP_JOIN", "✅ Request created successfully!")
                    onSuccess()
                }
                .addOnFailureListener { e ->
                    Log.e("GROUP_JOIN", "❌ Failed to create request", e)
                    onFailure(e)
                }
        }.addOnFailureListener { e ->
            Log.e("GROUP_JOIN", "❌ Failed to get user details", e)
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
        getGroupJoinRequestReference(requestID).update("status", "accepted")
            .addOnSuccessListener {
                getGroupReference(groupID).update("memberIDs", FieldValue.arrayUnion(userID))
                    .addOnSuccessListener {
                        // Send notification
                        getGroupReference(groupID).get().addOnSuccessListener { groupDoc ->
                            val group = groupDoc.toObject(groupModel::class.java)
                            FirebaseNotifications.createNotification(
                                type = "GROUP_JOIN_REQUEST_ACCEPTED",
                                recipientID = userID,
                                senderID = FirebaseAuth.currentUserID() ?: "",
                                senderName = "Admin",
                                groupID = groupID,
                                groupName = group?.groupName,
                                message = "Your request to join ${group?.groupName} has been accepted"
                            )
                        }
                        onSuccess()
                    }
                    .addOnFailureListener { onFailure(it) }
            }
            .addOnFailureListener { onFailure(it) }
    }

    @JvmStatic
    fun rejectGroupJoinRequest(
        requestID: String,
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        getGroupJoinRequestReference(requestID).update("status", "rejected")
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onFailure(it) }
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
            .addOnFailureListener { onFailure(it) }
    }

    @JvmStatic
    fun getAllPendingGroupJoinRequestsForAdmin(
        onSuccess: (List<GroupJoinRequestModel>) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        val currentUserID = FirebaseAuth.currentUserID() ?: return

        allGroupsCollection()
            .whereArrayContains("adminIDs", currentUserID)
            .get()
            .addOnSuccessListener { groupDocs ->
                val groupIDs = groupDocs.map { it.id }

                if (groupIDs.isEmpty()) {
                    onSuccess(emptyList())
                    return@addOnSuccessListener
                }

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
                    .addOnFailureListener { onFailure(it) }
            }
            .addOnFailureListener { onFailure(it) }
    }
}