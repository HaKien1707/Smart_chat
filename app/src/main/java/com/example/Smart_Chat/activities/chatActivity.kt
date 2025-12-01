package com.example.Smart_Chat.activities

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.cloudinary.android.MediaManager
import com.example.Smart_Chat.R
import com.example.Smart_Chat.adapters.MsgRecyclerAdapter
import com.example.Smart_Chat.models.MsgModel
import com.example.Smart_Chat.models.chatRoomModel
import com.example.Smart_Chat.models.userModel
import com.example.Smart_Chat.utils.FireBase_utils
import com.example.Smart_Chat.utils.androidUtils
import com.firebase.ui.firestore.FirestoreRecyclerOptions
import com.google.auth.oauth2.GoogleCredentials
import com.google.firebase.FirebaseApp
import com.google.firebase.Timestamp
import com.google.firebase.firestore.Query
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException

class chatActivity : AppCompatActivity() {

    private var user2nd: userModel? = null
    private lateinit var backBTN: ImageButton
    private lateinit var panelName: TextView
    private lateinit var chatBox: EditText
    private lateinit var sendBtn: ImageButton
    private lateinit var chatList: RecyclerView
    private lateinit var profileImage: ImageView

    private lateinit var chatRoomID: String
    private var chatRoom: chatRoomModel? = null
    private lateinit var adapter: MsgRecyclerAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat)

        // Get user model
        user2nd = androidUtils.getUserModelFromIntent(intent)

        // Add null check
        if (user2nd == null) {
            Log.e("chatActivity", "user2nd is null!")
            finish()
            return
        }

        chatRoomID = FireBase_utils.getChatRoomID(
            user2nd?.userID,
            FireBase_utils.currentUserID()
        )

        // Initialize views
        backBTN = findViewById(R.id.back_btn)
        panelName = findViewById(R.id.panelName)
        chatBox = findViewById(R.id.chatBox)
        sendBtn = findViewById(R.id.sendBtn)
        chatList = findViewById(R.id.chatList)
        val profileContainer = findViewById<View>(R.id.profile_image_container)
        profileImage = profileContainer.findViewById(R.id.profile_image)

        // Set click listeners
        backBTN.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        // Set username
        panelName.text = user2nd?.username

        // Load profile image
        val imageUrl = user2nd?.profileImage
        if (!imageUrl.isNullOrBlank()) {
            Log.d("chatActivity", "Calling setProfileImageFromBase64")
            androidUtils.setProfileImageFromBase64(
                this,
                imageUrl,
                profileImage
            )
            Log.d("chatActivity", "setProfileImageFromBase64 completed")
        } else {
            Log.d("chatActivity", "Setting default icon")
            profileImage.setImageResource(R.drawable.ic_person)
        }

        sendBtn.setOnClickListener {
            val msg = chatBox.text.toString().trim()
            if (msg.isNotEmpty()) {
                sendMsgToUser(msg)
            }
        }

        getOrCreateChatRoom()
        setupChatRecycler()
    }

    private fun getOrCreateChatRoom() {
        FireBase_utils.getChatRoomReferences(chatRoomID).get()
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    chatRoom = task.result.toObject(chatRoomModel::class.java)

                    if (chatRoom == null) {
                        // First time chat → create model
                        chatRoom = chatRoomModel(
                            chatRoomID,
                            mutableListOf(FireBase_utils.currentUserID(), user2nd?.userID),
                            Timestamp.now(),
                            FireBase_utils.currentUserID()
                        )

                        FireBase_utils.getChatRoomReferences(chatRoomID)
                            .set(chatRoom!!)
                            .addOnSuccessListener {
                                Log.d("CHATROOM", "Created: $chatRoomID")
                            }
                            .addOnFailureListener { e ->
                                Log.e("CHATROOM", "ERROR: ${e.message}")
                            }
                    }
                }
            }
    }

    private fun sendMsgToUser(msg: String) {
        // Update last message info ONLY
        FireBase_utils.getChatRoomReferences(chatRoomID)
            .update(
                mapOf(
                    "lastMsg" to msg,
                    "lastMsgSenderID" to FireBase_utils.currentUserID(),
                    "lastMsgTimestamp" to Timestamp.now()
                )
            )

        // Add actual message to messages subcollection
        val msgModel = MsgModel(
            FireBase_utils.currentUserID(),
            msg,
            Timestamp.now()
        )

        FireBase_utils.getChatRoomMessagesReferences(chatRoomID)
            .add(msgModel)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    chatBox.setText("")
                    sendNotification(msg)
                }
            }
    }

    private fun setupChatRecycler() {
        val query = FireBase_utils.getChatRoomMessagesReferences(chatRoomID)
            .orderBy("timestamp", Query.Direction.ASCENDING)

        val options = FirestoreRecyclerOptions.Builder<MsgModel>()
            .setQuery(query, MsgModel::class.java)
            .build()

        adapter = MsgRecyclerAdapter(options, applicationContext)
        val manager = LinearLayoutManager(this).apply {
            stackFromEnd = true
        }

        chatList.layoutManager = manager
        chatList.adapter = adapter
        adapter.startListening()

        adapter.registerAdapterDataObserver(object : RecyclerView.AdapterDataObserver() {
            override fun onItemRangeInserted(positionStart: Int, itemCount: Int) {
                chatList.smoothScrollToPosition(adapter.itemCount - 1)
            }
        })
    }

    private fun sendNotification(msg: String) {
        Log.d("NOTIFICATION", "=== Starting sendNotification ===")

        val recipientToken = user2nd?.fcmToken
        Log.d(
            "NOTIFICATION",
            "Recipient token: ${recipientToken?.take(20) ?: "NULL"}..."
        )

        if (recipientToken.isNullOrEmpty()) {
            Log.e("NOTIFICATION", "❌ Recipient has no FCM token! Cannot send notification.")
            return
        }

        FireBase_utils.currentUserDetails().get().addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val currentUser = task.result.toObject(userModel::class.java)

                try {
                    val jsonObject = JSONObject().apply {
                        put("message", JSONObject().apply {
                            put("token", user2nd?.fcmToken)
                            put("notification", JSONObject().apply {
                                put("title", currentUser?.username ?: "")
                                put("body", msg)
                            })
                            put("data", JSONObject().apply {
                                put("userID", currentUser?.userID ?: "")
                            })
                        })
                    }

                    callAPI(jsonObject)
                } catch (e: Exception) {
                    Log.e("NOTIFICATION", "Error creating notification", e)
                }
            }
        }
    }

    private fun callAPI(jsonObject: JSONObject) {
        Thread {
            try {
                // Get access token
                val accessToken = getAccessToken()

                val json = "application/json".toMediaType()
                val client = OkHttpClient()

                // V1 API URL format
                val projectId = FirebaseApp.getInstance().options.projectId
                val url = "https://fcm.googleapis.com/v1/projects/$projectId/messages:send"

                val requestBody = jsonObject.toString().toRequestBody(json)
                val request = Request.Builder()
                    .url(url)
                    .post(requestBody)
                    .header("Authorization", "Bearer $accessToken")
                    .header("Content-Type", "application/json")
                    .build()

                client.newCall(request).enqueue(object : Callback {
                    override fun onFailure(call: Call, e: IOException) {
                        Log.e("NOTIFICATION", "Failed to send notification", e)
                    }

                    override fun onResponse(call: Call, response: Response) {
                        val responseBody = response.body?.string() ?: ""
                        if (response.isSuccessful) {
                            Log.d("NOTIFICATION", "Notification sent successfully: $responseBody")
                        } else {
                            Log.e("NOTIFICATION", "Failed: ${response.code} - $responseBody")
                        }
                    }
                })
            } catch (e: Exception) {
                Log.e("NOTIFICATION", "Error in callAPI", e)
            }
        }.start()
    }

    @Throws(IOException::class)
    private fun getAccessToken(): String {
        val googleCredentials = GoogleCredentials
            .fromStream(resources.openRawResource(R.raw.service_account))
            .createScoped(listOf("https://www.googleapis.com/auth/firebase.messaging"))

        googleCredentials.refresh()
        return googleCredentials.accessToken.tokenValue
    }
}