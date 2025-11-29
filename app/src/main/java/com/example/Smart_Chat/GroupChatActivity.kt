package com.example.Smart_Chat

import android.os.Bundle
import android.util.Log
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.Smart_Chat.adapters.GroupMsgRecyclerAdapter
import com.example.Smart_Chat.models.GroupMsgModel
import com.example.Smart_Chat.models.groupModel
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

class GroupChatActivity : AppCompatActivity() {

    private lateinit var backBTN: ImageButton
    private lateinit var PanelName: TextView
    private lateinit var chatBox: EditText
    private lateinit var sendBtn: ImageButton
    private lateinit var chatList: RecyclerView
    private lateinit var groupImage: ImageView

    private lateinit var groupID: String
    private var groupName: String? = null
    private var group: groupModel? = null
    private lateinit var adapter: GroupMsgRecyclerAdapter
    private var currentUserName: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_groupchat)

        // Get group data from intent
        groupID = intent.getStringExtra("groupID") ?: ""
        groupName = intent.getStringExtra("groupName")

        if (groupID.isEmpty()) {
            Log.e("GroupChatActivity", "groupID is null or empty!")
            finish()
            return
        }

        // Initialize views
        backBTN = findViewById(R.id.back_btn)
        PanelName = findViewById(R.id.panelName)
        chatBox = findViewById(R.id.chatBox)
        sendBtn = findViewById(R.id.sendBtn)
        chatList = findViewById(R.id.chatList)
        val profileContainer = findViewById<android.view.View>(R.id.profile_image_container)
        groupImage = profileContainer.findViewById(R.id.profile_image)

        // Set click listeners
        backBTN.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        // Set group name
        PanelName.text = groupName

        // Load current user's name
        getCurrentUserName()

        // Load group details
        loadGroupDetails()

        sendBtn.setOnClickListener {
            val msg = chatBox.text.toString().trim()
            if (msg.isNotEmpty()) {
                sendMsgToGroup(msg)
            }
        }

        setupChatRecycler()
    }

    private fun getCurrentUserName() {
        FireBase_utils.currentUserDetails().get()
            .addOnSuccessListener { document ->
                val user = document.toObject(userModel::class.java)
                currentUserName = user?.username
            }
    }

    private fun loadGroupDetails() {
        FireBase_utils.getGroupReference(groupID).get()
            .addOnSuccessListener { document ->
                group = document.toObject(groupModel::class.java)

                // Load group image
                val imageUrl = group?.groupImage
                if (!imageUrl.isNullOrBlank()) {
                    androidUtils.setProfileImageFromBase64(
                        this,
                        imageUrl,
                        groupImage
                    )
                } else {
                    groupImage.setImageResource(R.drawable.ic_group)
                }
            }
            .addOnFailureListener { e ->
                Log.e("GroupChatActivity", "Failed to load group: ${e.message}")
            }
    }

    private fun sendMsgToGroup(msg: String) {
        // Update last message info in group
        FireBase_utils.getGroupReference(groupID)
            .update(
                mapOf(
                    "lastMsg" to msg,
                    "lastMsgSenderID" to FireBase_utils.currentUserID(),
                    "lastMsgTimestamp" to Timestamp.now()
                )
            )

        // Add actual message to messages subcollection
        val msgModel = GroupMsgModel(
            FireBase_utils.currentUserID(),
            currentUserName ?: "Unknown",
            msg,
            Timestamp.now()
        )

        FireBase_utils.getGroupMessagesReference(groupID)
            .add(msgModel)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    chatBox.setText("")
                    sendNotificationToMembers(msg)
                }
            }
    }

    private fun setupChatRecycler() {
        val query = FireBase_utils.getGroupMessagesReference(groupID)
            .orderBy("timestamp", Query.Direction.ASCENDING)

        val options = FirestoreRecyclerOptions.Builder<GroupMsgModel>()
            .setQuery(query, GroupMsgModel::class.java)
            .build()

        adapter = GroupMsgRecyclerAdapter(options, applicationContext)
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

    private fun sendNotificationToMembers(msg: String) {
        Log.d("GROUP_NOTIFICATION", "=== Starting sendNotificationToMembers ===")

        // Get all group members
        val memberIDs = group?.memberIDs ?: return

        // Get current user info
        FireBase_utils.currentUserDetails().get().addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val currentUser = task.result.toObject(userModel::class.java)

                // Send notification to each member (except yourself)
                memberIDs.forEach { memberID ->
                    if (memberID != FireBase_utils.currentUserID() && memberID != null) {
                        sendNotificationToMember(memberID, currentUser?.username ?: "Someone", msg)
                    }
                }
            }
        }
    }

    private fun sendNotificationToMember(memberID: String, senderName: String, msg: String) {
        // Get member's FCM token
        FireBase_utils.allUsersCollection().document(memberID).get()
            .addOnSuccessListener { document ->
                val member = document.toObject(userModel::class.java)
                val fcmToken = member?.fcmToken

                if (fcmToken.isNullOrEmpty()) {
                    Log.e("GROUP_NOTIFICATION", "Member $memberID has no FCM token")
                    return@addOnSuccessListener
                }

                try {
                    val jsonObject = JSONObject().apply {
                        put("message", JSONObject().apply {
                            put("token", fcmToken)
                            put("notification", JSONObject().apply {
                                put("title", "$senderName (${groupName ?: "Group"})")
                                put("body", msg)
                            })
                            put("data", JSONObject().apply {
                                put("groupID", groupID)
                                put("type", "group")
                            })
                        })
                    }

                    callAPI(jsonObject)
                } catch (e: Exception) {
                    Log.e("GROUP_NOTIFICATION", "Error creating notification", e)
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
                        Log.e("GROUP_NOTIFICATION", "Failed to send notification", e)
                    }

                    override fun onResponse(call: Call, response: Response) {
                        val responseBody = response.body?.string() ?: ""
                        if (response.isSuccessful) {
                            Log.d("GROUP_NOTIFICATION", "Notification sent successfully")
                        } else {
                            Log.e("GROUP_NOTIFICATION", "Failed: ${response.code} - $responseBody")
                        }
                    }
                })
            } catch (e: Exception) {
                Log.e("GROUP_NOTIFICATION", "Error in callAPI", e)
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

    override fun onStart() {
        super.onStart()
        adapter.startListening()
    }

    override fun onStop() {
        super.onStop()
        adapter.stopListening()
    }
}