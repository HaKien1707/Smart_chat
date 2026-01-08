package com.example.smart_chat.utils.firebase

import android.util.Log
import com.example.smart_chat.models.temp_chat.TemporaryChatModel
import com.example.smart_chat.utils.security.EncryptionUtils
import com.google.firebase.Timestamp
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

object FirebaseTemporaryChat {

    @JvmStatic
    fun allTemporaryChatsCollection(): CollectionReference {
        return FirebaseFirestore.getInstance().collection("temporaryChats")
    }

    @JvmStatic
    fun getTemporaryChatReference(chatID: String): DocumentReference {
        return allTemporaryChatsCollection().document(chatID)
    }

    @JvmStatic
    fun getTemporaryChatMessagesReference(chatID: String): CollectionReference {
        return getTemporaryChatReference(chatID).collection("messages")
    }

    @JvmStatic
    fun createTemporaryChat(
        friendID: String,
        onSuccess: (String, String) -> Unit, // Returns chatID and encryptionKey
        onFailure: (Exception) -> Unit
    ) {
        val currentUserID = FirebaseAuthentication.currentUserID() ?: return
        val chatID = allTemporaryChatsCollection().document().id

        // Generate encryption key
        val encryptionKey = EncryptionUtils.generateKey()

        val tempChat = TemporaryChatModel(
            chatID,
            mutableListOf(currentUserID, friendID),
            Timestamp.now(),
            encryptionKey
        )

        getTemporaryChatReference(chatID).set(tempChat)
            .addOnSuccessListener { onSuccess(chatID, encryptionKey) }
            .addOnFailureListener { onFailure(it) }
    }

    @JvmStatic
    fun markUserAsActiveInTempChat(chatID: String) {
        val currentUserID = FirebaseAuthentication.currentUserID() ?: return

        getTemporaryChatReference(chatID)
            .update("activeUsers", FieldValue.arrayUnion(currentUserID))
            .addOnSuccessListener {
                Log.d("FirebaseTemporaryChat", "User marked as active in temp chat")
            }
            .addOnFailureListener { e ->
                Log.e("FirebaseTemporaryChat", "Failed to mark user as active", e)
            }
    }

    @JvmStatic
    fun markUserAsInactiveInTempChat(
        chatID: String,
        onBothLeft: () -> Unit = {}
    ) {
        val currentUserID = FirebaseAuthentication.currentUserID() ?: return

        getTemporaryChatReference(chatID)
            .update("activeUsers", FieldValue.arrayRemove(currentUserID))
            .addOnSuccessListener {
                Log.d("FirebaseTemporaryChat", "User marked as inactive")

                // Check if both users have left
                getTemporaryChatReference(chatID).get()
                    .addOnSuccessListener { document ->
                        val chat = document.toObject(TemporaryChatModel::class.java)
                        if (chat?.activeUsers.isNullOrEmpty()) {
                            // Both users have left, delete everything
                            deleteTemporaryChat(chatID)
                            onBothLeft()
                        }
                    }
            }
            .addOnFailureListener { e ->
                Log.e("FirebaseTemporaryChat", "Failed to mark user as inactive", e)
            }
    }

    @JvmStatic
    fun deleteTemporaryChat(chatID: String) {
        // Delete all messages first
        getTemporaryChatMessagesReference(chatID)
            .get()
            .addOnSuccessListener { messages ->
                val batch = FirebaseFirestore.getInstance().batch()

                messages.documents.forEach { msgDoc ->
                    batch.delete(msgDoc.reference)
                }

                // Delete the chat document
                batch.delete(getTemporaryChatReference(chatID))

                batch.commit()
                    .addOnSuccessListener {
                        Log.d("FirebaseTemporaryChat", "Temporary chat deleted successfully")
                    }
                    .addOnFailureListener { e ->
                        Log.e("FirebaseTemporaryChat", "Failed to delete temporary chat", e)
                    }
            }
            .addOnFailureListener { e ->
                Log.e("FirebaseTemporaryChat", "Failed to get messages for deletion", e)
            }
    }

    @JvmStatic
    fun deleteExpiredTemporaryChats() {
        val now = Timestamp.now()

        allTemporaryChatsCollection()
            .whereLessThan("expiresAt", now)
            .get()
            .addOnSuccessListener { chats ->
                if (chats.isEmpty) {
                    return@addOnSuccessListener
                }

                val batch = FirebaseFirestore.getInstance().batch()

                chats.forEach { chatDoc ->
                    val chatID = chatDoc.id
                    batch.delete(getTemporaryChatReference(chatID))
                }

                batch.commit()
                    .addOnSuccessListener {
                        Log.d("FirebaseTemporaryChat", "Deleted ${chats.size()} expired chats")
                    }
                    .addOnFailureListener { e ->
                        Log.e("FirebaseTemporaryChat", "Failed to delete expired chats", e)
                    }
            }
            .addOnFailureListener { e ->
                Log.e("FirebaseTemporaryChat", "Failed to query expired chats", e)
            }
    }

    @JvmStatic
    fun getUserTemporaryChatsQuery(): Query {
        val currentUserID = FirebaseAuthentication.currentUserID() ?: return allTemporaryChatsCollection()
            .whereArrayContains("userIDs", "")

        return allTemporaryChatsCollection()
            .whereArrayContains("userIDs", currentUserID)
            .orderBy("lastMsgTimestamp", Query.Direction.DESCENDING)
    }
}