package com.example.smart_chat.utils.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.smart_chat.R
import com.example.smart_chat.activities.login.splashScreenActivity
import com.example.smart_chat.utils.others.ChatStateManager
import com.example.smart_chat.utils.firebase.FirebaseChat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class FirebaseMessagingService : FirebaseMessagingService() {

    companion object {
        private const val CHANNEL_ID = "chat_notifications"
        private const val TAG = "FCM_SERVICE"
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        Log.d(TAG, "Message received from: ${remoteMessage.from}")

        // Extract notification data
        val title = remoteMessage.notification?.title ?: remoteMessage.data["title"]
        val body = remoteMessage.notification?.body ?: remoteMessage.data["body"]

        // Extract navigation data
        val userID = remoteMessage.data["userID"]
        val groupID = remoteMessage.data["groupID"]
        val chatType = remoteMessage.data["type"] // "group" or "private"

        Log.d(TAG, "Title: $title, Body: $body, UserID: $userID, GroupID: $groupID, Type: $chatType")

        if (title != null && body != null) {
            showNotification(title, body, userID, groupID, chatType)
        } else {
            Log.w(TAG, "Notification title or body is null")
        }
    }

    private fun showNotification(
        title: String,
        body: String,
        userID: String?,
        groupID: String?,
        chatType: String?
    ) {
        Log.d(TAG, "Showing notification - Title: $title")
        if (chatType == "group" && groupID != null) {
            if (ChatStateManager.isInGroup(groupID)) {
                Log.d(TAG, "User is in this group chat, suppressing notification")
                return
            }
        } else if (userID != null) {
            val chatRoomID = FirebaseChat.getChatRoomID(
                FirebaseAuth.getInstance().currentUser?.uid,
                userID
            )
            if (ChatStateManager.isInChat(chatRoomID)) {
                Log.d(TAG, "User is in this chat, suppressing notification")
                return
            }
        }

        // Create notification channel (required for Android 8.0+)
        createNotificationChannel()

        // Create intent to open chat when notification is clicked
        val intent = Intent(this, splashScreenActivity::class.java).apply {
            putExtra("userID", userID)
            putExtra("groupID", groupID)
            putExtra("chatType", chatType)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            System.currentTimeMillis().toInt(), // Unique ID for each notification
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        // Build notification
        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notifications)
            .setContentTitle(title)
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)

        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager

        // Use unique ID so multiple messages show multiple notifications
        val notificationId = System.currentTimeMillis().toInt()
        notificationManager.notify(notificationId, builder.build())

        Log.d(TAG, "Notification displayed with ID: $notificationId")
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Chat Notifications",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications for chat messages"
                enableLights(true)
                enableVibration(true)
            }

            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)

            Log.d(TAG, "Notification channel created")
        }
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "New FCM token generated: $token")

        // Update token in Firestore if user is logged in
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        if (userId != null) {
            updateTokenInFirestore(userId, token)
        } else {
            Log.w(TAG, "User not logged in, token not saved")
        }
    }

    private fun updateTokenInFirestore(userId: String, token: String) {
        FirebaseFirestore.getInstance()
            .collection("users")
            .document(userId)
            .update("fcmToken", token)
            .addOnSuccessListener {
                Log.d(TAG, "FCM token updated in Firestore for user: $userId")
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Failed to update token in Firestore: ${e.message}")
            }
    }
}