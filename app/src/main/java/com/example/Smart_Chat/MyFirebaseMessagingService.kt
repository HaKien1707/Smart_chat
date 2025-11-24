package com.example.Smart_Chat

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class MyFirebaseMessagingService : FirebaseMessagingService() {

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        Log.d("FCM", "========== onMessageReceived CALLED ==========")
        Log.d("FCM", "Thread: ${Thread.currentThread().name}")
        Log.d("FCM", "Message from: ${remoteMessage.from}")

        // Check if message contains a notification payload
        if (remoteMessage.notification != null) {
            Log.d("FCM", "Processing notification payload")
            val title = remoteMessage.notification?.title
            val body = remoteMessage.notification?.body
            val userID = remoteMessage.data["userID"]

            Log.d("FCM", "Notification - Title: $title, Body: $body, UserID: $userID")

            showNotification(title, body, userID)
        } else {
            Log.d("FCM", "No notification payload, checking data only")

            // Handle data-only messages
            if (remoteMessage.data.isNotEmpty()) {
                val userID = remoteMessage.data["userID"]
                Log.d("FCM", "Data-only message from userID: $userID")

                // You might need to extract title/body from data if sent that way
                val title = remoteMessage.data["title"]
                val body = remoteMessage.data["body"]

                if (title != null && body != null) {
                    showNotification(title, body, userID)
                }
            }
        }
    }

    private fun showNotification(title: String?, body: String?, userID: String?) {
        Log.d("FCM", "showNotification called - Title: $title, Body: $body")

        val channelId = "chat_notifications"

        // Create notification channel (required for Android 8.0+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Chat Notifications",
                NotificationManager.IMPORTANCE_HIGH
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }

        // Create intent to open chat when notification is clicked
        val intent = Intent(this, splashScreenActivity::class.java).apply {
            putExtra("userID", userID)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        // Build notification
        val builder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)

        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(0, builder.build())

        Log.d("FCM", "Notification displayed")
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d("FCM", "New token: $token")

        // Update token in Firestore if user is logged in
        val userId = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
        if (userId != null) {
            com.google.firebase.firestore.FirebaseFirestore.getInstance()
                .collection("users")
                .document(userId)
                .update("fcmToken", token)
                .addOnSuccessListener {
                    Log.d("FCM", "Token updated in Firestore")
                }
                .addOnFailureListener { e ->
                    Log.e("FCM", "Failed to update token: ${e.message}")
                }
        }
    }
}