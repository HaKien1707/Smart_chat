package com.example.smart_chat.utils.firebase

import android.util.Log
import com.example.smart_chat.models.notification.NotificationModel
import com.google.firebase.Timestamp
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.WriteBatch

object FirebaseNotifications {
    @JvmStatic
    fun notificationsCollection(): CollectionReference {
        return FirebaseFirestore.getInstance().collection("notifications")
    }

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
        notificationIDOverride: String? = null,
        onSuccess: () -> Unit = {},
        onFailure: (Exception) -> Unit = {}
    ) {
        try {
            val notificationID = notificationIDOverride ?: notificationsCollection().document().id

            val notification = NotificationModel(
                notificationID,
                type,
                recipientID,
                senderID,
                senderName,
                groupID,
                groupName,
                communityID,
                communityName,
                message,
                false,
                Timestamp.now()
            )

            notificationsCollection().document(notificationID).set(notification)
                .addOnSuccessListener {
                    Log.d("FirebaseNotifications", "Notification created: $type")
                    onSuccess()
                }
                .addOnFailureListener {
                    Log.e("FirebaseNotifications", "Failed to create notification: ${it.message}", it)
                    onFailure(it)
                }
        } catch (e: Exception) {
            Log.e("FirebaseNotifications", "Error creating notification", e)
            onFailure(e)
        }
    }

    @JvmStatic
    fun getUserNotifications(
        userID: String,
        onSuccess: (List<NotificationModel>) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        try {
            notificationsCollection()
                .whereEqualTo("recipientID", userID)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener { documents ->
                    val notifications = documents.mapNotNull {
                        try {
                            it.toObject(NotificationModel::class.java)
                        } catch (e: Exception) {
                            Log.e("FirebaseNotifications", "Failed to parse notification", e)
                            null
                        }
                    }
                    onSuccess(notifications)
                }
                .addOnFailureListener {
                    Log.e("FirebaseNotifications", "Failed to get notifications", it)
                    onFailure(it)
                }
        } catch (e: Exception) {
            Log.e("FirebaseNotifications", "Error getting notifications", e)
            onFailure(e)
        }
    }

    @JvmStatic
    fun markNotificationAsRead(
        notificationID: String,
        onSuccess: () -> Unit = {},
        onFailure: (Exception) -> Unit = {}
    ) {
        if (notificationID.isBlank()) {
            onFailure(IllegalArgumentException("notificationID is blank"))
            return
        }
        notificationsCollection().document(notificationID)
            .update("isRead", true)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onFailure(it) }
    }

    @JvmStatic
    fun deleteNotification(
        notificationID: String,
        onSuccess: () -> Unit = {},
        onFailure: (Exception) -> Unit = {}
    ) {
        if (notificationID.isBlank()) {
            onFailure(IllegalArgumentException("notificationID is blank"))
            return
        }
        notificationsCollection().document(notificationID)
            .delete()
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onFailure(it) }
    }

    @JvmStatic
    fun updateNotification(
        notificationID: String,
        updates: Map<String, Any>,
        onSuccess: () -> Unit = {},
        onFailure: (Exception) -> Unit = {}
    ) {
        if (notificationID.isBlank()) {
            onFailure(IllegalArgumentException("notificationID is blank"))
            return
        }
        notificationsCollection().document(notificationID)
            .update(updates)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onFailure(it) }
    }

    @JvmStatic
    fun markAllUserNotificationsAsRead(
        userID: String,
        onSuccess: () -> Unit = {},
        onFailure: (Exception) -> Unit = {}
    ) {
        try {
            notificationsCollection()
                .whereEqualTo("recipientID", userID)
                .whereEqualTo("isRead", false)
                .get()
                .addOnSuccessListener { docs ->
                    if (docs.isEmpty) {
                        onSuccess()
                        return@addOnSuccessListener
                    }
                    val batch: WriteBatch = FirebaseFirestore.getInstance().batch()
                    docs.documents.forEach { doc ->
                        batch.update(doc.reference, "isRead", true)
                    }
                    batch.commit()
                        .addOnSuccessListener { onSuccess() }
                        .addOnFailureListener { onFailure(it) }
                }
                .addOnFailureListener { onFailure(it) }
        } catch (e: Exception) {
            onFailure(e)
        }
    }
}