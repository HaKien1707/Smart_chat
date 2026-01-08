package com.example.smart_chat.utils.notification

import android.util.Log
import com.example.smart_chat.R
import com.example.smart_chat.models.userModel
import com.google.auth.oauth2.GoogleCredentials
import com.google.firebase.FirebaseApp
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException
import android.content.Context
import com.example.smart_chat.utils.firebase.FirebaseAuthentication

object UserChatNotificationHelper {
    private const val TAG = "USER_CHAT_NOTIF"

    fun sendMessageNotification(
        context: Context,
        receiverID: String,
        senderName: String,
        message: String
    ) {
        Log.d(TAG, "Preparing to send notification to: $receiverID")

        // Get receiver's FCM token
        FirebaseAuthentication.allUsersCollection().document(receiverID).get()
            .addOnSuccessListener { document ->
                val receiver = document.toObject(userModel::class.java)
                val fcmToken = receiver?.fcmToken

                if (fcmToken.isNullOrEmpty()) {
                    Log.w(TAG, "Receiver $receiverID has no FCM token")
                    return@addOnSuccessListener
                }

                Log.d(TAG, "Found FCM token for receiver: $receiverID")

                try {
                    val jsonObject = JSONObject().apply {
                        put("message", JSONObject().apply {
                            put("token", fcmToken)
                            put("notification", JSONObject().apply {
                                put("title", senderName)
                                put("body", message)
                            })
                            put("data", JSONObject().apply {
                                put("userID", FirebaseAuthentication.currentUserID())
                                put("type", "private")
                            })
                        })
                    }

                    sendFCMNotification(context, jsonObject, receiverID)
                } catch (e: Exception) {
                    Log.e(TAG, "Error creating notification JSON", e)
                }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Failed to get receiver data", e)
            }
    }

    private fun sendFCMNotification(context: Context, jsonObject: JSONObject, receiverID: String) {
        Thread {
            try {
                val accessToken = getAccessToken(context)
                val json = "application/json".toMediaType()
                val client = OkHttpClient()
                val projectId = FirebaseApp.getInstance().options.projectId
                val url = "https://fcm.googleapis.com/v1/projects/$projectId/messages:send"

                val requestBody = jsonObject.toString().toRequestBody(json)
                val request = Request.Builder()
                    .url(url)
                    .post(requestBody)
                    .header("Authorization", "Bearer $accessToken")
                    .header("Content-Type", "application/json")
                    .build()

                Log.d(TAG, "Sending FCM request to: $url")

                client.newCall(request).enqueue(object : Callback {
                    override fun onFailure(call: Call, e: IOException) {
                        Log.e(TAG, "Failed to send notification", e)
                    }

                    override fun onResponse(call: Call, response: Response) {
                        val responseBody = response.body.string()
                        if (response.isSuccessful) {
                            Log.d(TAG, "Notification sent successfully to $receiverID")
                        } else {
                            Log.e(TAG, "Failed: ${response.code} - $responseBody")

                            // Handle UNREGISTERED token error
                            if (responseBody.contains("UNREGISTERED") ||
                                responseBody.contains("NotRegistered")) {
                                Log.w(TAG, "Token is invalid, removing from user $receiverID")
                                FCMTokenManager.removeInvalidToken(receiverID)
                            }
                        }
                    }
                })
            } catch (e: Exception) {
                Log.e(TAG, "Error in sendFCMNotification", e)
            }
        }.start()
    }

    @Throws(IOException::class)
    private fun getAccessToken(context: Context): String {
        val googleCredentials = GoogleCredentials
            .fromStream(context.resources.openRawResource(R.raw.service_account))
            .createScoped(listOf("https://www.googleapis.com/auth/firebase.messaging"))

        googleCredentials.refresh()
        return googleCredentials.accessToken.tokenValue
    }
}