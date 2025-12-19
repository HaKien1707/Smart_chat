package com.example.Smart_Chat.activities.group_chat

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.SimpleItemAnimator
import com.bumptech.glide.Glide
import com.example.Smart_Chat.R
import com.example.Smart_Chat.adapters.GroupMsgRecyclerAdapter
import com.example.Smart_Chat.models.GroupMsgModel
import com.example.Smart_Chat.models.ReplyMessageData
import com.example.Smart_Chat.models.groupModel
import com.example.Smart_Chat.models.userModel
import com.example.Smart_Chat.utils.BotMessageHelper
import com.example.Smart_Chat.utils.CloudinaryHelper
import com.example.Smart_Chat.utils.FCMTokenManager
import com.example.Smart_Chat.utils.FireBase_utils
import com.example.Smart_Chat.utils.GeminiHelper
import com.example.Smart_Chat.utils.LanguageManager
import com.example.Smart_Chat.utils.MediaMessageHelper
import com.example.Smart_Chat.utils.ThemeManager
import com.example.Smart_Chat.utils.androidUtils
import com.firebase.ui.firestore.FirestoreRecyclerOptions
import com.github.dhaval2404.imagepicker.ImagePicker
import com.google.auth.oauth2.GoogleCredentials
import com.google.firebase.FirebaseApp
import com.google.firebase.Timestamp
import com.google.firebase.firestore.Query
import kotlinx.coroutines.launch
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException

class GroupChatActivity : AppCompatActivity() {

    private lateinit var backBTN: ImageButton
    private lateinit var groupSettingsBtn: ImageButton
    private lateinit var panelName: TextView
    private lateinit var chatBox: EditText
    private lateinit var sendBtn: ImageButton
    private lateinit var sendImageBtn: ImageButton
    private lateinit var chatList: RecyclerView
    private lateinit var groupImage: ImageView

    private lateinit var replyPreviewContainer: View
    private lateinit var replyText: TextView
    private lateinit var replyImage: ImageView
    private lateinit var replySenderName: TextView
    private lateinit var replyTextContainer: LinearLayout
    private lateinit var cancelReplyBtn: ImageButton

    private var currentReplyData: ReplyMessageData? = null

    private var isBotProcessing = false

    private val settingsLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        // Reload group details after returning from settings
        loadGroupDetails()
    }

    private lateinit var groupID: String
    private var groupName: String? = null
    private var group: groupModel? = null
    private lateinit var adapter: GroupMsgRecyclerAdapter
    private var currentUserName: String? = null

    private lateinit var sendFileBtn: ImageButton

    private val imagePickerLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result: ActivityResult ->
            if (result.resultCode == RESULT_OK) {
                val uri = result.data?.data
                if (uri != null) {
                    uploadAndSendImage(uri)
                }
            }
        }

    private val filePickerLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result: ActivityResult ->
            if (result.resultCode == RESULT_OK) {
                val uri = result.data?.data
                if (uri != null) {
                    uploadAndSendFile(uri)
                }
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        ThemeManager.applySavedTheme(this)
        LanguageManager.applySavedLanguage(this)

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_group_chat)

        CloudinaryHelper.initCloudinary(this)

        groupID = intent.getStringExtra("groupID") ?: ""
        groupName = intent.getStringExtra("groupName")

        if (groupID.isEmpty()) {
            Log.e("GroupChatActivity", "groupID is null or empty!")
            finish()
            return
        }

        // Initialize views
        backBTN = findViewById(R.id.back_btn)
        groupSettingsBtn = findViewById(R.id.group_settings_btn)
        panelName = findViewById(R.id.panelName)
        chatBox = findViewById(R.id.chatBox)
        sendBtn = findViewById(R.id.sendBtn)
        sendImageBtn = findViewById(R.id.send_image_btn)
        sendFileBtn = findViewById(R.id.send_file_btn)
        chatList = findViewById(R.id.chatList)
        val profileContainer = findViewById<View>(R.id.profile_image_container)
        groupImage = profileContainer.findViewById(R.id.profile_image)

        // NEW: Reply preview views
        replyPreviewContainer = findViewById(R.id.reply_preview)
        replyText = replyPreviewContainer.findViewById(R.id.reply_text)
        replyImage = replyPreviewContainer.findViewById(R.id.reply_image)
        replySenderName = replyPreviewContainer.findViewById(R.id.reply_sender_name)
        replyTextContainer = replyPreviewContainer.findViewById(R.id.reply_text_container)
        cancelReplyBtn = replyPreviewContainer.findViewById(R.id.cancel_reply_btn)

        cancelReplyBtn.setOnClickListener {
            cancelReply()
        }

        // Set click listeners
        backBTN.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        groupSettingsBtn.setOnClickListener {
            val intent = Intent(this, GroupChatSettingsActivity::class.java)
            intent.putExtra("groupID", groupID)
            settingsLauncher.launch(intent)
        }

        panelName.text = groupName

        getCurrentUserName()
        loadGroupDetails()
        listenForGroupChanges()

        sendBtn.setOnClickListener {
            val msg = chatBox.text.toString().trim()
            if (msg.isNotEmpty()) {
                sendMsgToGroup(msg)
            }
        }

        sendImageBtn.setOnClickListener {
            pickImage()
        }

        sendFileBtn.setOnClickListener {
            pickFile()
        }

        setupChatRecycler()
    }

    private fun pickImage() {
        ImagePicker.with(this)
            .compress(1024)
            .maxResultSize(1080, 1080)
            .createIntent { intent -> imagePickerLauncher.launch(intent) }
    }

    private fun pickFile() {
        val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
            type = "*/*"
            addCategory(Intent.CATEGORY_OPENABLE)
            putExtra(Intent.EXTRA_MIME_TYPES, arrayOf(
                "application/pdf",
                "application/msword",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                "application/vnd.ms-excel",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                "application/vnd.ms-powerpoint",
                "application/vnd.openxmlformats-officedocument.presentationml.presentation",
                "text/*",
                "application/zip",
                "application/x-rar-compressed"
            ))
        }
        filePickerLauncher.launch(intent)
    }

    private fun uploadAndSendImage(imageUri: Uri) {
        sendImageBtn.isEnabled = false

        MediaMessageHelper.uploadAndSendImage(
            this,
            imageUri,
            FireBase_utils.getGroupReference(groupID),
            FireBase_utils.getGroupMessagesReference(groupID),
            FireBase_utils.currentUserID()!!,
            currentUserName,
            MediaMessageHelper.MessageType.GROUP,
            null,
            onSuccess = {
                runOnUiThread {
                    sendImageBtn.isEnabled = true
                    sendNotificationToMembers("📷 Photo")
                }
            },
            onError = {
                runOnUiThread {
                    sendImageBtn.isEnabled = true
                }
            }
        )
    }

    private fun uploadAndSendFile(fileUri: Uri) {
        sendFileBtn.isEnabled = false

        MediaMessageHelper.uploadAndSendFile(
            this,
            fileUri,
            FireBase_utils.getGroupReference(groupID),
            FireBase_utils.getGroupMessagesReference(groupID),
            FireBase_utils.currentUserID()!!,
            currentUserName,
            MediaMessageHelper.MessageType.GROUP,
            null,
            onSuccess = {
                runOnUiThread {
                    sendFileBtn.isEnabled = true
                    sendNotificationToMembers("📎 File")
                }
            },
            onError = {
                runOnUiThread {
                    sendFileBtn.isEnabled = true
                }
            }
        )
    }

    private fun getCurrentUserName() {
        FireBase_utils.currentUserDetails().get()
            .addOnSuccessListener { document ->
                val user = document.toObject(userModel::class.java)
                currentUserName = user?.username
            }
            .addOnFailureListener { e ->
                Log.e("GroupChatActivity", "Failed to load username: ${e.message}")
            }
    }

    private fun loadGroupDetails() {
        FireBase_utils.getGroupReference(groupID).get()
            .addOnSuccessListener { document ->
                group = document.toObject(groupModel::class.java)

                // Update group name in case it changed
                panelName.text = group?.groupName ?: groupName

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

    // NEW: Set reply message from adapter
    fun setReplyMessage(replyData: ReplyMessageData) {
        currentReplyData = replyData
        showReplyPreview(replyData)
    }

    // NEW: Show reply preview
    private fun showReplyPreview(replyData: ReplyMessageData) {
        replyPreviewContainer.visibility = View.VISIBLE

        // Show sender name for group chat
        if (!replyData.senderName.isNullOrEmpty()) {
            replySenderName.visibility = View.VISIBLE
            replySenderName.text = replyData.senderName
        } else {
            replySenderName.visibility = View.GONE
        }

        when (replyData.type) {
            "text" -> {
                replyTextContainer.visibility = View.VISIBLE
                replyImage.visibility = View.GONE
                replyText.text = replyData.text
            }
            "image" -> {
                replyTextContainer.visibility = View.GONE
                replyImage.visibility = View.VISIBLE
                Glide.with(this)
                    .load(replyData.imageUrl)
                    .placeholder(R.drawable.ic_image_loading)
                    .into(replyImage)
            }
            "file" -> {
                replyTextContainer.visibility = View.VISIBLE
                replyImage.visibility = View.GONE
                val fileName = replyData.fileName ?: "File"
                val fileSize = formatFileSize(replyData.fileSize ?: 0)
                replyText.text = "📎 $fileName\n$fileSize"
            }
        }
    }

    // NEW: Cancel reply
    private fun cancelReply() {
        currentReplyData = null
        replyPreviewContainer.visibility = View.GONE
    }

    // Scroll to specific position
    fun scrollToPosition(position: Int) {
        try {
            // Stop any ongoing scroll first
            chatList.stopScroll()

            // Use post to ensure adapter is ready
            chatList.post {
                try {
                    // Validate position is within bounds
                    if (position >= 0 && position < adapter.itemCount) {
                        // Use scrollToPosition instead of smoothScrollToPosition
                        chatList.scrollToPosition(position)

                        // Optional: Highlight the message briefly
                        chatList.postDelayed({
                            try {
                                val viewHolder = chatList.findViewHolderForAdapterPosition(position)
                                viewHolder?.itemView?.let { view ->
                                    view.animate()
                                        .alpha(0.3f)
                                        .setDuration(200)
                                        .withEndAction {
                                            view.animate()
                                                .alpha(1f)
                                                .setDuration(200)
                                                .start()
                                        }
                                        .start()
                                }
                            } catch (e: Exception) {
                                Log.e("GroupChatActivity", "Error highlighting message", e)
                            }
                        }, 300)
                    } else {
                        Log.w("GroupChatActivity", "Invalid scroll position: $position, itemCount: ${adapter.itemCount}")
                    }
                } catch (e: Exception) {
                    Log.e("GroupChatActivity", "Error scrolling to position", e)
                }
            }
        } catch (e: Exception) {
            Log.e("GroupChatActivity", "Error in scrollToPosition", e)
        }
    }

    private fun formatFileSize(size: Long): String {
        return when {
            size < 1024 -> "$size B"
            size < 1024 * 1024 -> "${size / 1024} KB"
            else -> String.format("%.2f MB", size / (1024.0 * 1024.0))
        }
    }

    private fun listenForGroupChanges() {
        // Listen to group document changes in real-time
        FireBase_utils.getGroupReference(groupID)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("GroupChatActivity", "Listen failed: ${error.message}")
                    return@addSnapshotListener
                }

                if (snapshot != null && snapshot.exists()) {
                    val updatedGroup = snapshot.toObject(groupModel::class.java)
                    val memberIDs = updatedGroup?.memberIDs ?: emptyList()

                    // Check if current user is still a member
                    if (!memberIDs.contains(FireBase_utils.currentUserID())) {
                        // User was removed from group
                        Toast.makeText(
                            this,
                            "You have been removed from this group",
                            Toast.LENGTH_SHORT
                        ).show()
                        finish()
                    }
                } else {
                    // Group was deleted
                    Toast.makeText(
                        this,
                        "This group has been deleted",
                        Toast.LENGTH_SHORT
                    ).show()
                    finish()
                }
            }
    }

    private fun sendMsgToGroup(msg: String) {
        // Check if it's a bot command
        if (BotMessageHelper.isBotCommand(msg)) {
            handleBotCommand(msg)
            return
        }

        FireBase_utils.getGroupReference(groupID)
            .update(
                mapOf(
                    "lastMsg" to msg,
                    "lastMsgSenderID" to FireBase_utils.currentUserID(),
                    "lastMsgTimestamp" to Timestamp.now()
                )
            )

        val msgModel = if (currentReplyData != null) {
            GroupMsgModel(
                FireBase_utils.currentUserID(),
                currentUserName ?: "Unknown",
                msg,
                Timestamp.now(),
                messageType = "text",
                replyToMessageId = currentReplyData!!.messageId,
                replyToText = currentReplyData!!.text,
                replyToType = currentReplyData!!.type,
                replyToImageUrl = currentReplyData!!.imageUrl,
                replyToFileName = currentReplyData!!.fileName,
                replyToFileSize = currentReplyData!!.fileSize,
                replyToSenderName = currentReplyData!!.senderName
            )
        } else {
            GroupMsgModel(
                FireBase_utils.currentUserID(),
                currentUserName ?: "Unknown",
                msg,
                Timestamp.now()
            )
        }

        FireBase_utils.getGroupMessagesReference(groupID)
            .add(msgModel)
            .addOnSuccessListener {
                chatBox.setText("")
                cancelReply() // NEW: Clear reply state
                sendNotificationToMembers(msg)
            }
            .addOnFailureListener { e ->
                Log.e("GroupChatActivity", "Failed to send message", e)
                Toast.makeText(this, "Failed to send message", Toast.LENGTH_SHORT).show()
            }
    }

    private fun setupChatRecycler() {
        val query = FireBase_utils.getGroupMessagesReference(groupID)
            .orderBy("timestamp", Query.Direction.ASCENDING)

        val options = FirestoreRecyclerOptions.Builder<GroupMsgModel>()
            .setQuery(query, GroupMsgModel::class.java)
            .setLifecycleOwner(this)
            .build()

        adapter = GroupMsgRecyclerAdapter(options, this)

        // Disable item animator to avoid RecyclerView inconsistency crashes
        try {
            val animator = chatList.itemAnimator
            if (animator is SimpleItemAnimator) {
                animator.supportsChangeAnimations = false
            }
            chatList.itemAnimator = null
        } catch (e: Exception) {
            Log.w("GroupChatActivity", "Failed to modify itemAnimator: ${e.message}")
        }

        val manager = LinearLayoutManager(this).apply {
            stackFromEnd = true
        }

        chatList.layoutManager = manager
        chatList.adapter = adapter

        adapter.registerAdapterDataObserver(object : RecyclerView.AdapterDataObserver() {
            override fun onItemRangeInserted(positionStart: Int, itemCount: Int) {
                super.onItemRangeInserted(positionStart, itemCount)
                // Use post to avoid crash during layout
                chatList.post {
                    if (adapter.itemCount > 0) {
                        chatList.scrollToPosition(adapter.itemCount - 1)
                    }
                }
            }
        })
    }

    // ========================================================================
    //                              CHAT BOT
    // ========================================================================

    private fun handleBotCommand(command: String) {
        if (isBotProcessing) {
            Toast.makeText(this, "Bot is processing. Please wait...", Toast.LENGTH_SHORT).show()
            return
        }

        // Check rate limit
        val (canProcess, errorMsg) = BotMessageHelper.canProcessBotRequest(this)
        if (!canProcess) {
            Toast.makeText(this, errorMsg, Toast.LENGTH_LONG).show()
            return
        }

        // Extract prompt
        val userPrompt = BotMessageHelper.extractPrompt(command)

        if (userPrompt.isEmpty()) {
            Toast.makeText(this, "Please provide a command after @Bot", Toast.LENGTH_SHORT).show()
            return
        }

        // Show user's command as a message
        val userCommandMsg = GroupMsgModel(
            FireBase_utils.currentUserID(),
            currentUserName ?: "Unknown",
            command,
            Timestamp.now()
        )

        FireBase_utils.getGroupMessagesReference(groupID)
            .add(userCommandMsg)
            .addOnSuccessListener {
                chatBox.setText("")

                showBotTyping()
                processBotRequest(userPrompt)
            }
    }

    private fun showBotTyping() {
        isBotProcessing = true
        sendBtn.isEnabled = false
        Toast.makeText(this, "🤖 Bot is thinking...", Toast.LENGTH_SHORT).show()
    }

    private fun hideBotTyping() {
        isBotProcessing = false
        sendBtn.isEnabled = true
    }

    private fun processBotRequest(userPrompt: String) {
        lifecycleScope.launch {
            try {
                // Fetch and format messages
                val messages = BotMessageHelper.fetchAndFormatMessages(
                    messagesRef = FireBase_utils.getGroupMessagesReference(groupID),
                    currentUserId = FireBase_utils.currentUserID()!!,
                    chatType = BotMessageHelper.ChatType.GROUP_CHAT
                )

                if (messages.isEmpty()) {
                    Toast.makeText(
                        this@GroupChatActivity,
                        "No messages to analyze",
                        Toast.LENGTH_SHORT
                    ).show()
                    hideBotTyping()
                    return@launch
                }

                // Call Gemini API
                val result = GeminiHelper.getBotResponse(this@GroupChatActivity, messages, userPrompt)

                result.onSuccess { response ->
                    // Send bot response
                    BotMessageHelper.sendBotResponse(
                        messagesRef = FireBase_utils.getGroupMessagesReference(groupID),
                        chatRef = FireBase_utils.getGroupReference(groupID),
                        response = response,
                        chatType = BotMessageHelper.ChatType.GROUP_CHAT,
                        currentUserId = FireBase_utils.currentUserID()!!,
                        currentUserName = currentUserName
                    )

                    // Send usage info message
                    BotMessageHelper.sendUsageMessage(
                        context = this@GroupChatActivity,
                        messagesRef = FireBase_utils.getGroupMessagesReference(groupID),
                        chatType = BotMessageHelper.ChatType.GROUP_CHAT
                    )

                    Toast.makeText(
                        this@GroupChatActivity,
                        "Bot responded!",
                        Toast.LENGTH_SHORT
                    ).show()
                }

                result.onFailure { error ->
                    Toast.makeText(
                        this@GroupChatActivity,
                        "Bot error: ${error.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }

                hideBotTyping()

            } catch (e: Exception) {
                Log.e("GroupChatActivity", "Bot error", e)
                hideBotTyping()
                Toast.makeText(
                    this@GroupChatActivity,
                    "Failed to get bot response",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
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
                    Log.w("GROUP_NOTIFICATION", "Member $memberID has no FCM token")
                    return@addOnSuccessListener
                }

                try {
                    val jsonObject = JSONObject().apply {
                        put("message", JSONObject().apply {
                            put("token", fcmToken)
                            put("notification", JSONObject().apply {
                                put("title", "$senderName (${group?.groupName ?: "Group"})")
                                put("body", msg)
                            })
                            put("data", JSONObject().apply {
                                put("groupID", groupID)
                                put("type", "group")
                            })
                        })
                    }

                    callAPI(jsonObject, memberID) // Pass memberID to handle errors
                } catch (e: Exception) {
                    Log.e("GROUP_NOTIFICATION", "Error creating notification", e)
                }
            }
            .addOnFailureListener { e ->
                Log.e("GROUP_NOTIFICATION", "Failed to get member token", e)
            }
    }

    private fun callAPI(jsonObject: JSONObject, memberID: String) {
        Thread {
            try {
                val accessToken = getAccessToken()
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

                client.newCall(request).enqueue(object : Callback {
                    override fun onFailure(call: Call, e: IOException) {
                        Log.e("GROUP_NOTIFICATION", "Failed to send notification", e)
                    }

                    override fun onResponse(call: Call, response: Response) {
                        val responseBody = response.body?.string() ?: ""
                        if (response.isSuccessful) {
                            Log.d("GROUP_NOTIFICATION", "Notification sent successfully to $memberID")
                        } else {
                            Log.e("GROUP_NOTIFICATION", "Failed: ${response.code} - $responseBody")

                            // Handle UNREGISTERED token error
                            if (responseBody.contains("UNREGISTERED") || responseBody.contains("NotRegistered")) {
                                Log.w("GROUP_NOTIFICATION", "Token is invalid, removing from user $memberID")
                                FCMTokenManager.removeInvalidToken(memberID)
                            }
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

    fun getGroupID(): String {
        return groupID
    }
}