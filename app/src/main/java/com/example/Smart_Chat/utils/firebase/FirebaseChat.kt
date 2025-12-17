package com.example.Smart_Chat.utils.firebase

import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

object FirebaseChat {
    @JvmStatic
    fun getChatRoomReference(chatRoomID: String): DocumentReference {
        return FirebaseFirestore.getInstance()
            .collection("chatRooms")
            .document(chatRoomID)
    }

    @JvmStatic
    fun getChatRoomID(userID1: String?, userID2: String?): String {
        return if (userID1.hashCode() < userID2.hashCode()) {
            "${userID1}_${userID2}"
        } else {
            "${userID2}_${userID1}"
        }
    }

    @JvmStatic
    fun getChatRoomMessagesReference(chatRoomID: String): CollectionReference {
        return getChatRoomReference(chatRoomID).collection("messages")
    }

    @JvmStatic
    fun allChatRoomsCollection(): CollectionReference {
        return FirebaseFirestore.getInstance().collection("chatRooms")
    }

    @JvmStatic
    fun get2ndUserInChatRoom(userID: MutableList<String?>?): DocumentReference? {
        if (userID != null && userID.size >= 2) {
            val currentUserID = FirebaseAuth.currentUserID()
            val otherUserId = if (userID[0] == currentUserID) {
                userID[1]
            } else {
                userID[0]
            }
            return otherUserId?.let { FirebaseAuth.allUsersCollection().document(it) }
        }
        return null
    }

    // ========== CHAT ROOM DELETION ==========

    @JvmStatic
    fun softDeleteChatRoom(
        chatRoomID: String,
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        val currentUserID = FirebaseAuth.currentUserID() ?: return

        getChatRoomReference(chatRoomID)
            .update("deletedBy", FieldValue.arrayUnion(currentUserID))
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onFailure(it) }
    }

    @JvmStatic
    fun recoverChatRoom(
        chatRoomID: String,
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        val currentUserID = FirebaseAuth.currentUserID() ?: return

        getChatRoomReference(chatRoomID)
            .update("deletedBy", FieldValue.arrayRemove(currentUserID))
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onFailure(it) }
    }

    @JvmStatic
    fun permanentlyDeleteChatRoom(
        chatRoomID: String,
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        val chatRoomRef = getChatRoomReference(chatRoomID)

        getChatRoomMessagesReference(chatRoomID)
            .get()
            .addOnSuccessListener { messages ->
                val batch = FirebaseFirestore.getInstance().batch()

                messages.documents.forEach { doc ->
                    batch.delete(doc.reference)
                }

                batch.delete(chatRoomRef)

                batch.commit()
                    .addOnSuccessListener { onSuccess() }
                    .addOnFailureListener { onFailure(it) }
            }
            .addOnFailureListener { onFailure(it) }
    }

    @JvmStatic
    fun getActiveChatRoomsQuery(): Query {
        val currentUserID = FirebaseAuth.currentUserID() ?: return allChatRoomsCollection()
            .whereArrayContains("userID", "")

        return allChatRoomsCollection()
            .whereArrayContains("userID", currentUserID)
            .orderBy("lastMsgTimestamp", Query.Direction.DESCENDING)
    }

    @JvmStatic
    fun getDeletedChatRoomsQuery(): Query {
        val currentUserID = FirebaseAuth.currentUserID() ?: return allChatRoomsCollection()
            .whereArrayContains("userID", "")

        return allChatRoomsCollection()
            .whereArrayContains("deletedBy", currentUserID)
            .orderBy("lastMsgTimestamp", Query.Direction.DESCENDING)
    }
}