package com.example.Smart_Chat.utils.firebase

import android.util.Log
import com.example.Smart_Chat.models.notification.NotificationModel
import com.google.firebase.Timestamp
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

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
        onSuccess: () -> Unit = {},
        onFailure: (Exception) -> Unit = {}
    ) {
        try {
            val notificationID = notificationsCollection().document().id

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
        notificationsCollection().document(notificationID)
            .delete()
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onFailure(it) }
    }
}