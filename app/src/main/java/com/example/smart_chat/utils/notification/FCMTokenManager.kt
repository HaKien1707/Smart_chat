package com.example.smart_chat.utils.notification

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore

object FCMTokenManager {
    private const val TAG = "FCM_TOKEN_MANAGER"

    /**
     * Remove invalid FCM token from user document
     */
    fun removeInvalidToken(userId: String) {
        FirebaseFirestore.getInstance()
            .collection("users")
            .document(userId)
            .update("fcmToken", null)
            .addOnSuccessListener {
                Log.d(TAG, "Removed invalid token for user: $userId")
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Failed to remove invalid token: ${e.message}")
            }
    }

    /**
     * Validate and clean up token before sending notification
     */
    fun cleanupInvalidTokens(userIds: List<String>) {
        userIds.forEach { userId ->
            FirebaseFirestore.getInstance()
                .collection("users")
                .document(userId)
                .get()
                .addOnSuccessListener { document ->
                    val token = document.getString("fcmToken")
                    if (token.isNullOrEmpty()) {
                        Log.w(TAG, "User $userId has no FCM token")
                    }
                }
        }
    }
}