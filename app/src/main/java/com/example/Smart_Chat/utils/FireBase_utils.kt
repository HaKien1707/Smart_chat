package com.example.Smart_Chat.utils

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
        return FirebaseFirestore.getInstance().collection("groups")
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

    @JvmStatic
    fun logout() {
        FirebaseAuth.getInstance().signOut()
    }

    @JvmStatic
    val isLoggedIn: Boolean
        get() = currentUserID() != null
}