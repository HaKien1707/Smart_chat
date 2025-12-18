package com.example.Smart_Chat.activities.user_chat

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.*
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.Smart_Chat.R
import com.example.Smart_Chat.adapters.MsgRecyclerAdapter
import com.example.Smart_Chat.models.*
import com.example.Smart_Chat.utils.*
import com.example.Smart_Chat.utils.CloudinaryHelper
import com.example.Smart_Chat.utils.MediaMessageHelper
import com.example.Smart_Chat.utils.firebase.FirebaseFriends
import com.firebase.ui.firestore.FirestoreRecyclerOptions
import com.github.dhaval2404.imagepicker.ImagePicker
import com.google.firebase.Timestamp
import com.google.firebase.firestore.Query
import kotlinx.coroutines.launch

class ChatActivity : AppCompatActivity() {

    private lateinit var backBtn: ImageButton
    private lateinit var profileImage: ImageView
    private lateinit var userName: TextView
    private lateinit var msgInput: EditText
    private lateinit var sendBtn: ImageButton
    private lateinit var sendImageBtn: ImageButton
    private lateinit var attachBtn: ImageButton
    private lateinit var msgRecycler: RecyclerView

    private var isCheckingFriendship = true

    // Reply preview views
    private lateinit var replyPreviewContainer: View
    private lateinit var replyText: TextView
    private lateinit var replyImage: ImageView
    private lateinit var replySenderName: TextView
    private lateinit var replyTextContainer: LinearLayout
    private lateinit var cancelReplyBtn: ImageButton

    private var otherUser: userModel? = null
    private var chatRoomID: String? = null
    private lateinit var adapter: MsgRecyclerAdapter

    // Current reply state
    private var currentReplyData: ReplyMessageData? = null

    private var isBotProcessing = false

    private val imagePickerLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result: ActivityResult ->
            if (result.resultCode == RESULT_OK) {
                val uri = result.data?.data
                if (uri != null) {
                    sendImageMessage(uri)
                }
            }
        }

    private val filePickerLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result: ActivityResult ->
            if (result.resultCode == RESULT_OK) {
                val uri = result.data?.data
                if (uri != null) {
                    sendFileMessage(uri)
                }
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        ThemeManager.applySavedTheme(this)
        LanguageManager.applySavedLanguage(this)

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat)

        otherUser = androidUtils.getUserModelFromIntent(intent)

        CloudinaryHelper.initCloudinary(this)

        if (otherUser == null) {
            Toast.makeText(this, "Error loading user", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        chatRoomID = FireBase_utils.getChatRoomID(
            FireBase_utils.currentUserID(),
            otherUser?.userID
        )

        initViews()
        setupUI()
        checkFriendshipStatus()
        setupRecycler()
    }

    private fun initViews() {
        backBtn = findViewById(R.id.back_btn)
        profileImage = findViewById(R.id.profile_image)
        userName = findViewById(R.id.user_name)
        msgInput = findViewById(R.id.chatBox)
        sendBtn = findViewById(R.id.sendBtn)
        attachBtn = findViewById(R.id.send_file_btn)
        sendImageBtn = findViewById(R.id.send_image_btn)
        msgRecycler = findViewById(R.id.chatList)

        // NEW: Reply preview views
        replyPreviewContainer = findViewById(R.id.reply_preview)
        replyText = replyPreviewContainer.findViewById(R.id.reply_text)
        replyImage = replyPreviewContainer.findViewById(R.id.reply_image)
        replySenderName = replyPreviewContainer.findViewById(R.id.reply_sender_name)
        replyTextContainer = replyPreviewContainer.findViewById(R.id.reply_text_container)
        cancelReplyBtn = replyPreviewContainer.findViewById(R.id.cancel_reply_btn)

        backBtn.setOnClickListener { finish() }
        sendBtn.setOnClickListener { sendMessage() }
        sendImageBtn.setOnClickListener { pickImage() }
        attachBtn.setOnClickListener { pickFile() }

        // NEW: Cancel reply button
        cancelReplyBtn.setOnClickListener {
            cancelReply()
        }
    }

    private fun setupUI() {
        userName.text = otherUser?.username

        if (!otherUser?.profileImage.isNullOrEmpty()) {
            androidUtils.setProfileImageFromBase64(
                this,
                otherUser?.profileImage!!,
                profileImage
            )
        }

        // Open user profile on click
        profileImage.setOnClickListener {
            // Open profile activity if needed
        }
    }

    private fun checkFriendshipStatus() {
        FirebaseFriends.checkFriendshipStatus(otherUser?.userID ?: "") { status ->
            runOnUiThread {
                isCheckingFriendship = false

                if (status != FirebaseFriends.FriendshipStatus.FRIENDS) {
                    // Not friends - redirect to NotFriendsActivity
                    val intent = Intent(this, NotFriendsActivity::class.java)
                    androidUtils.passUserModelAsIntent(intent, otherUser)
                    startActivity(intent)
                    finish()
                } else {
                    // Friends - setup chat normally
                    setupRecycler()
                }
            }
        }
    }

    private fun setupRecycler() {
        val query = FireBase_utils.getChatRoomMessagesReferences(chatRoomID!!)
            .orderBy("timestamp", Query.Direction.ASCENDING)

        val options = FirestoreRecyclerOptions.Builder<MsgModel>()
            .setQuery(query, MsgModel::class.java)
            .build()

        adapter = MsgRecyclerAdapter(options, this)

        // Disable item animator to prevent inconsistency crashes
        try {
            val animator = msgRecycler.itemAnimator
            if (animator is androidx.recyclerview.widget.SimpleItemAnimator) {
                animator.supportsChangeAnimations = false
            }
            msgRecycler.itemAnimator = null
        } catch (e: Exception) {
            Log.w("ChatActivity", "Failed to modify itemAnimator: ${e.message}")
        }

        val layoutManager = LinearLayoutManager(this)
        layoutManager.stackFromEnd = true
        msgRecycler.layoutManager = layoutManager
        msgRecycler.adapter = adapter

        adapter.startListening()

        adapter.registerAdapterDataObserver(object : RecyclerView.AdapterDataObserver() {
            override fun onItemRangeInserted(positionStart: Int, itemCount: Int) {
                super.onItemRangeInserted(positionStart, itemCount)
                msgRecycler.smoothScrollToPosition(adapter.itemCount)
            }
        })
    }

    // Set reply message from adapter
    fun setReplyMessage(replyData: ReplyMessageData) {
        currentReplyData = replyData
        showReplyPreview(replyData)
    }

    // Show reply preview
    private fun showReplyPreview(replyData: ReplyMessageData) {
        replyPreviewContainer.visibility = View.VISIBLE

        // Hide sender name for 1-on-1 chat
        replySenderName.visibility = View.GONE

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

    // Cancel reply
    private fun cancelReply() {
        currentReplyData = null
        replyPreviewContainer.visibility = View.GONE
    }

    // Scroll to specific position
    fun scrollToPosition(position: Int) {
        msgRecycler.smoothScrollToPosition(position)

        // Optional: Highlight the message briefly
        msgRecycler.postDelayed({
            val viewHolder = msgRecycler.findViewHolderForAdapterPosition(position)
            viewHolder?.itemView?.let { view ->
                // Flash animation
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
        }, 300)
    }

    private fun sendMessage() {
        val messageText = msgInput.text.toString().trim()

        if (messageText.isEmpty()) {
            msgInput.error = "Enter a message"
            return
        }

        // Check if it's a bot command using helper
        if (BotMessageHelper.isBotCommand(messageText)) {
            handleBotCommand(messageText)
            return
        }

        // Check if it's a bot command
        if (messageText.startsWith("@Bot", ignoreCase = true) ||
            messageText.startsWith("@bot", ignoreCase = true) ||
            messageText.startsWith("@BOT")) {

            handleBotCommand(messageText)
            return
        }

        // Normal message sending (existing code)
        val msgModel = if (currentReplyData != null) {
            MsgModel(
                senderID = FireBase_utils.currentUserID(),
                msg = messageText,
                timestamp = Timestamp.now(),
                messageType = "text",
                replyToMessageId = currentReplyData!!.messageId,
                replyToText = currentReplyData!!.text,
                replyToType = currentReplyData!!.type,
                replyToImageUrl = currentReplyData!!.imageUrl,
                replyToFileName = currentReplyData!!.fileName,
                replyToFileSize = currentReplyData!!.fileSize
            )
        } else {
            MsgModel(
                senderID = FireBase_utils.currentUserID(),
                msg = messageText,
                timestamp = Timestamp.now(),
                messageType = "text"
            )
        }

        FireBase_utils.getChatRoomMessagesReferences(chatRoomID!!)
            .add(msgModel)
            .addOnSuccessListener {
                msgInput.setText("")
                cancelReply()
                updateChatRoom(messageText, "text")
            }
            .addOnFailureListener { e ->
                Log.e("CHAT", "Failed to send message", e)
                Toast.makeText(this, "Failed to send message", Toast.LENGTH_SHORT).show()
            }
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

    private fun sendImageMessage(imageUri: Uri) {
        attachBtn.isEnabled = false

        MediaMessageHelper.uploadAndSendImage(
            this,
            imageUri,
            FireBase_utils.getChatRoomReferences(chatRoomID!!),
            FireBase_utils.getChatRoomMessagesReferences(chatRoomID!!),
            FireBase_utils.currentUserID()!!,
            null,
            MediaMessageHelper.MessageType.ONE_TO_ONE,
            null,
            onSuccess = {
                runOnUiThread {
                    attachBtn.isEnabled = true
                    cancelReply()
                }
            },
            onError = {
                runOnUiThread {
                    attachBtn.isEnabled = true
                }
            }
        )
    }

    private fun sendFileMessage(fileUri: Uri) {
        attachBtn.isEnabled = false

        MediaMessageHelper.uploadAndSendFile(
            this,
            fileUri,
            FireBase_utils.getChatRoomReferences(chatRoomID!!),
            FireBase_utils.getChatRoomMessagesReferences(chatRoomID!!),
            FireBase_utils.currentUserID()!!,
            null, // No sender name for 1-on-1
            MediaMessageHelper.MessageType.ONE_TO_ONE,
            null, // No encryption for regular chat
            onSuccess = {
                runOnUiThread {
                    attachBtn.isEnabled = true
                    cancelReply()
                }
            },
            onError = {
                runOnUiThread {
                    attachBtn.isEnabled = true
                }
            }
        )
    }

    // ==============================================================
    //                          CHAT BOT
    // ==============================================================
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
        val userCommandMsg = MsgModel(
            senderID = FireBase_utils.currentUserID(),
            msg = command,
            timestamp = Timestamp.now(),
            messageType = "text"
        )

        FireBase_utils.getChatRoomMessagesReferences(chatRoomID!!)
            .add(userCommandMsg)
            .addOnSuccessListener {
                msgInput.setText("")
                cancelReply()

                showBotTyping()
                processBotRequest(userPrompt)
            }
    }

    private fun processBotRequest(userPrompt: String) {
        lifecycleScope.launch {
            try {
                // Fetch and format messages
                val messages = BotMessageHelper.fetchAndFormatMessages(
                    messagesRef = FireBase_utils.getChatRoomMessagesReferences(chatRoomID!!),
                    currentUserId = FireBase_utils.currentUserID()!!,
                    chatType = BotMessageHelper.ChatType.USER_CHAT,
                    otherUserName = otherUser?.username
                )

                // Call Gemini API
                val result = GeminiHelper.getBotResponse(this@ChatActivity, messages, userPrompt)

                result.onSuccess { response ->
                    // Send bot response
                    BotMessageHelper.sendBotResponse(
                        messagesRef = FireBase_utils.getChatRoomMessagesReferences(chatRoomID!!),
                        chatRef = FireBase_utils.getChatRoomReferences(chatRoomID!!),
                        response = response,
                        chatType = BotMessageHelper.ChatType.USER_CHAT,
                        currentUserId = FireBase_utils.currentUserID()!!
                    )

                    // Send usage info message
                    BotMessageHelper.sendUsageMessage(
                        context = this@ChatActivity,
                        messagesRef = FireBase_utils.getChatRoomMessagesReferences(chatRoomID!!),
                        chatType = BotMessageHelper.ChatType.USER_CHAT
                    )

                    Toast.makeText(
                        this@ChatActivity,
                        "Bot responded!",
                        Toast.LENGTH_SHORT
                    ).show()
                }

                result.onFailure { error ->
                    Toast.makeText(
                        this@ChatActivity,
                        "Bot error: ${error.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }

                hideBotTyping()

            } catch (e: Exception) {
                Log.e("CHAT", "Bot error", e)
                hideBotTyping()
                Toast.makeText(
                    this@ChatActivity,
                    "Failed to get bot response",
                    Toast.LENGTH_SHORT
                ).show()
            }
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

    private fun updateChatRoom(lastMsg: String, messageType: String) {
        val currentId = FireBase_utils.currentUserID()
        val otherId = otherUser?.userID

        if (currentId == null || otherId == null) return

        // You MUST include the 'userID' array here so the Security Rules
        // allow the "create" operation.
        val roomData = mapOf(
            "userID" to listOf(currentId, otherId),
            "lastMsg" to lastMsg,
            "lastMsgSenderID" to currentId,
            "lastMsgTimestamp" to com.google.firebase.Timestamp.now(),
            "deletedBy" to emptyList<String>(),
            "chatRoomID" to chatRoomID
        )

        FireBase_utils.getChatRoomReferences(chatRoomID!!)
            .set(roomData, com.google.firebase.firestore.SetOptions.merge())
            .addOnFailureListener { e ->
                Log.e("CHAT", "Failed to initialize/update room", e)
            }
    }

    private fun formatFileSize(size: Long): String {
        return when {
            size < 1024 -> "$size B"
            size < 1024 * 1024 -> "${size / 1024} KB"
            else -> String.format("%.2f MB", size / (1024.0 * 1024.0))
        }
    }

    // Public method to get chat room ID (for adapter)
    fun getChatRoomID(): String? = chatRoomID

    override fun onStart() {
        super.onStart()
        adapter.startListening()
    }

    override fun onStop() {
        super.onStop()
        adapter.stopListening()
    }
}