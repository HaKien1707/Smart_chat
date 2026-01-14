package com.example.smart_chat.fragment

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import android.widget.Button
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.smart_chat.R
import com.example.smart_chat.activities.user_chat.UserInfoActivity
import com.example.smart_chat.adapters.user_chat.MsgRecyclerAdapter
import com.example.smart_chat.models.MsgModel
import com.example.smart_chat.models.msg_action.ReplyMessageData
import com.example.smart_chat.models.userModel
import com.example.smart_chat.utils.AI.BotMessageHelper
import com.example.smart_chat.utils.AI.GeminiHelper
import com.example.smart_chat.utils.firebase.FirebaseAuthentication
import com.example.smart_chat.utils.firebase.FirebaseBlocking
import com.example.smart_chat.utils.firebase.FirebaseChat
import com.example.smart_chat.utils.media.CloudinaryHelper
import com.example.smart_chat.utils.media.MediaMessageHelper
import com.example.smart_chat.utils.notification.UserChatNotificationHelper
import com.example.smart_chat.utils.others.ChatStateManager
import com.example.smart_chat.utils.others.androidUtils
import com.firebase.ui.firestore.FirestoreRecyclerOptions
import com.github.dhaval2404.imagepicker.ImagePicker
import com.google.firebase.Timestamp
import com.google.firebase.firestore.Query
import kotlinx.coroutines.launch

class UserChatFragment : Fragment() {

    // UI Components
    private lateinit var backBtn: ImageButton
    private lateinit var moreBtn: ImageButton
    private lateinit var profileImage: ImageView
    private lateinit var userName: TextView
    private lateinit var msgInput: EditText
    private lateinit var sendBtn: ImageButton
    private lateinit var sendImageBtn: ImageButton
    private lateinit var attachBtn: ImageButton
    private lateinit var msgRecycler: RecyclerView

    private lateinit var bottomPanel: View

    private lateinit var chatBoxContainer: View
    private lateinit var blockedStateContainer: View
    private lateinit var blockedStateText: TextView
    private lateinit var unblockBtn: Button

    // Reply preview views
    private lateinit var replyPreviewContainer: View
    private lateinit var replyText: TextView
    private lateinit var replyImage: ImageView
    private lateinit var replySenderName: TextView
    private lateinit var replyTextContainer: LinearLayout
    private lateinit var cancelReplyBtn: ImageButton

    // Data
    private var otherUser: userModel? = null
    private var chatRoomID: String? = null
    private lateinit var adapter: MsgRecyclerAdapter
    private var currentReplyData: ReplyMessageData? = null
    private var isBotProcessing = false
    private var isCheckingFriendship = true

    private var isChatBlocked: Boolean = false
    private var isBlockedByMe: Boolean = false

    // Activity Result Launchers
    private val imagePickerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result: ActivityResult ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            val uri = result.data?.data
            if (uri != null) {
                sendImageMessage(uri)
            }
        }
    }

    private val filePickerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result: ActivityResult ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            val uri = result.data?.data
            if (uri != null) {
                sendFileMessage(uri)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            otherUser = androidUtils.getUserModelFromBundle(it)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_user_chat, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (otherUser == null) {
            Toast.makeText(requireContext(), "Error loading user", Toast.LENGTH_SHORT).show()
            activity?.finish()
            return
        }

        chatRoomID = FirebaseChat.getChatRoomID(
            FirebaseAuthentication.currentUserID(),
            otherUser?.userID
        )

        CloudinaryHelper.initCloudinary(requireContext())

        initViews(view)
        setupImeInsets(view)
        setupUI()
        setupListeners()
        setupRecycler()

        refreshBlockState()
    }

    private fun initViews(view: View) {
        backBtn = view.findViewById(R.id.back_btn)
        moreBtn = view.findViewById(R.id.more_btn)
        userName = view.findViewById(R.id.user_name)
        msgInput = view.findViewById(R.id.chatBox)
        sendBtn = view.findViewById(R.id.sendBtn)
        attachBtn = view.findViewById(R.id.send_file_btn)
        sendImageBtn = view.findViewById(R.id.send_image_btn)
        msgRecycler = view.findViewById(R.id.chatList)

        bottomPanel = view.findViewById(R.id.bottomPanel)

        chatBoxContainer = view.findViewById(R.id.chatBoxContainer)
        blockedStateContainer = view.findViewById(R.id.blocked_state_container)
        blockedStateText = view.findViewById(R.id.blocked_state_text)
        unblockBtn = view.findViewById(R.id.unblock_btn)

        val profileContainer = view.findViewById<View>(R.id.profile_image_container)
        profileImage = profileContainer.findViewById(R.id.profile_image)

        // Reply preview views
        replyPreviewContainer = view.findViewById(R.id.reply_preview)
        replyText = replyPreviewContainer.findViewById(R.id.reply_text)
        replyImage = replyPreviewContainer.findViewById(R.id.reply_image)
        replySenderName = replyPreviewContainer.findViewById(R.id.reply_sender_name)
        replyTextContainer = replyPreviewContainer.findViewById(R.id.reply_text_container)
        cancelReplyBtn = replyPreviewContainer.findViewById(R.id.cancel_reply_btn)
    }

    private fun setupImeInsets(root: View) {
        val initialBottomMargin =
            (bottomPanel.layoutParams as? ViewGroup.MarginLayoutParams)?.bottomMargin ?: 0

        ViewCompat.setOnApplyWindowInsetsListener(root) { _, insets ->
            val imeBottom = insets.getInsets(WindowInsetsCompat.Type.ime()).bottom
            val systemBarsBottom = insets.getInsets(WindowInsetsCompat.Type.systemBars()).bottom
            val extraBottom = maxOf(0, imeBottom - systemBarsBottom)

            val layoutParams = bottomPanel.layoutParams
            if (layoutParams is ViewGroup.MarginLayoutParams) {
                layoutParams.bottomMargin = initialBottomMargin + extraBottom
                bottomPanel.layoutParams = layoutParams
            }
            insets
        }

        ViewCompat.requestApplyInsets(root)
    }

    private fun setupListeners() {
        backBtn.setOnClickListener {
            activity?.onBackPressedDispatcher?.onBackPressed()
        }

        sendBtn.setOnClickListener {
            sendMessage()
        }

        sendImageBtn.setOnClickListener {
            pickImage()
        }

        attachBtn.setOnClickListener {
            pickFile()
        }

        moreBtn.setOnClickListener {
            val intent = Intent(requireContext(), UserInfoActivity::class.java)
            androidUtils.passUserModelAsIntent(intent, otherUser)
            startActivity(intent)
        }

        cancelReplyBtn.setOnClickListener {
            cancelReply()
        }

        unblockBtn.setOnClickListener {
            val targetUserID = otherUser?.userID
            if (targetUserID.isNullOrBlank()) return@setOnClickListener

            FirebaseBlocking.unblockUser(
                targetUserID,
                onSuccess = {
                    Toast.makeText(requireContext(), getString(R.string.unblock), Toast.LENGTH_SHORT).show()
                    refreshBlockState()
                },
                onFailure = { e ->
                    Toast.makeText(requireContext(), "Failed to unblock: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            )
        }

        profileImage.setOnClickListener {
            // Open profile activity if needed
        }
    }

    private fun setupUI() {
        userName.text = otherUser?.username

        if (!otherUser?.profileImage.isNullOrEmpty()) {
            androidUtils.setProfileImageFromBase64(
                requireContext(),
                otherUser?.profileImage!!,
                profileImage
            )
        }
    }

    private fun setupRecycler() {
        val query = FirebaseChat.getChatRoomMessagesReference(chatRoomID!!)
            .orderBy("timestamp", Query.Direction.ASCENDING)

        val options = FirestoreRecyclerOptions.Builder<MsgModel>()
            .setQuery(query, MsgModel::class.java)
            .setLifecycleOwner(viewLifecycleOwner)
            .build()

        adapter = MsgRecyclerAdapter(options, requireActivity())

        // Disable item animator to prevent inconsistency crashes
        try {
            val animator = msgRecycler.itemAnimator
            if (animator is androidx.recyclerview.widget.SimpleItemAnimator) {
                animator.supportsChangeAnimations = false
            }
            msgRecycler.itemAnimator = null
        } catch (e: Exception) {
            Log.w("UserChatFragment", "Failed to modify itemAnimator: ${e.message}")
        }

        val layoutManager = LinearLayoutManager(requireContext())
        layoutManager.stackFromEnd = true
        msgRecycler.layoutManager = layoutManager
        msgRecycler.adapter = adapter

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
        try {
            // Stop any ongoing scroll first
            msgRecycler.stopScroll()

            // Use post to ensure adapter is ready
            msgRecycler.post {
                try {
                    // Validate position is within bounds
                    if (position >= 0 && position < adapter.itemCount) {
                        // Use scrollToPosition instead of smoothScrollToPosition
                        msgRecycler.scrollToPosition(position)

                        // Optional: Highlight the message briefly
                        msgRecycler.postDelayed({
                            try {
                                val viewHolder = msgRecycler.findViewHolderForAdapterPosition(position)
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
                                Log.e("UserChatFragment", "Error highlighting message", e)
                            }
                        }, 300)
                    } else {
                        Log.w("UserChatFragment", "Invalid scroll position: $position, itemCount: ${adapter.itemCount}")
                    }
                } catch (e: Exception) {
                    Log.e("UserChatFragment", "Error scrolling to position", e)
                }
            }
        } catch (e: Exception) {
            Log.e("UserChatFragment", "Error in scrollToPosition", e)
        }
    }

    private fun sendMessage() {
        if (isChatBlocked) {
            Toast.makeText(requireContext(), blockedStateText.text, Toast.LENGTH_SHORT).show()
            return
        }

        val messageText = msgInput.text.toString().trim()

        if (messageText.isEmpty()) {
            msgInput.error = getString(R.string.enter_a_message)
            return
        }

        // Check if it's a bot command
        if (BotMessageHelper.isBotCommand(messageText)) {
            handleBotCommand(messageText)
            return
        }

        // Normal message sending
        val msgModel = if (currentReplyData != null) {
            MsgModel(
                senderID = FirebaseAuthentication.currentUserID(),
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
                senderID = FirebaseAuthentication.currentUserID(),
                msg = messageText,
                timestamp = Timestamp.now(),
                messageType = "text"
            )
        }

        FirebaseChat.getChatRoomMessagesReference(chatRoomID!!)
            .add(msgModel)
            .addOnSuccessListener {
                msgInput.setText("")
                cancelReply()
                updateChatRoom(messageText, "text")

                // Send notification to receiver
                sendNotificationToReceiver(messageText)
            }
            .addOnFailureListener { e ->
                Log.e("UserChatFragment", "Failed to send message", e)
                Toast.makeText(requireContext(), "Failed to send message", Toast.LENGTH_SHORT).show()
            }
    }

    // Send notification to the other user
    private fun sendNotificationToReceiver(message: String) {
        val receiverID = otherUser?.userID ?: return

        // Get current user's name
        FirebaseAuthentication.currentUserDetails().get()
            .addOnSuccessListener { document ->
                val currentUser = document.toObject(userModel::class.java)
                val senderName = currentUser?.username ?: "Someone"

                // Send notification
                UserChatNotificationHelper.sendMessageNotification(
                    requireContext(),
                    receiverID,
                    senderName,
                    message
                )
            }
            .addOnFailureListener { e ->
                Log.e("UserChatFragment", "Failed to get sender name", e)
            }
    }

    private fun pickImage() {
        if (isChatBlocked) {
            Toast.makeText(requireContext(), blockedStateText.text, Toast.LENGTH_SHORT).show()
            return
        }

        ImagePicker.with(this)
            .compress(1024)
            .maxResultSize(1080, 1080)
            .createIntent { intent -> imagePickerLauncher.launch(intent) }
    }

    private fun pickFile() {
        if (isChatBlocked) {
            Toast.makeText(requireContext(), blockedStateText.text, Toast.LENGTH_SHORT).show()
            return
        }

        val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
            type = "*/*"
            addCategory(Intent.CATEGORY_OPENABLE)
            putExtra(
                Intent.EXTRA_MIME_TYPES, arrayOf(
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
                )
            )
        }
        filePickerLauncher.launch(intent)
    }

    private fun sendImageMessage(imageUri: Uri) {
        if (isChatBlocked) return
        attachBtn.isEnabled = false

        MediaMessageHelper.uploadAndSendImage(
            requireContext(),
            imageUri,
            FirebaseChat.getChatRoomReference(chatRoomID!!),
            FirebaseChat.getChatRoomMessagesReference(chatRoomID!!),
            FirebaseAuthentication.currentUserID()!!,
            null,
            MediaMessageHelper.MessageType.ONE_TO_ONE,
            null,
            onSuccess = {
                activity?.runOnUiThread {
                    attachBtn.isEnabled = true
                    cancelReply()
                    sendNotificationToReceiver("📷 Photo")
                }
            },
            onError = {
                activity?.runOnUiThread {
                    attachBtn.isEnabled = true
                }
            }
        )
    }

    private fun sendFileMessage(fileUri: Uri) {
        if (isChatBlocked) return
        attachBtn.isEnabled = false

        MediaMessageHelper.uploadAndSendFile(
            requireContext(),
            fileUri,
            FirebaseChat.getChatRoomReference(chatRoomID!!),
            FirebaseChat.getChatRoomMessagesReference(chatRoomID!!),
            FirebaseAuthentication.currentUserID()!!,
            null, // No sender name for 1-on-1
            MediaMessageHelper.MessageType.ONE_TO_ONE,
            null, // No encryption for regular chat
            onSuccess = {
                activity?.runOnUiThread {
                    attachBtn.isEnabled = true
                    cancelReply()
                    val fileInfo = androidUtils.getFileInfo(requireContext(), fileUri)
                    sendNotificationToReceiver("📎 ${fileInfo.name}")
                }
            },
            onError = {
                activity?.runOnUiThread {
                    attachBtn.isEnabled = true
                }
            }
        )
    }

    // ==============================================================
    //                          CHAT BOT
    // ==============================================================

    private fun refreshBlockState() {
        val targetUserID = otherUser?.userID
        if (targetUserID.isNullOrBlank()) return

        FirebaseBlocking.isUserBlocked(targetUserID) { blockedByMe ->
            FirebaseBlocking.isBlockedByUser(targetUserID) { blockedMe ->
                applyBlockUi(blockedByMe = blockedByMe, blockedMe = blockedMe)
            }
        }
    }

    private fun applyBlockUi(blockedByMe: Boolean, blockedMe: Boolean) {
        isBlockedByMe = blockedByMe
        isChatBlocked = blockedByMe || blockedMe

        if (isChatBlocked) {
            // Hide input area and reply preview
            chatBoxContainer.visibility = View.GONE
            sendBtn.visibility = View.GONE
            replyPreviewContainer.visibility = View.GONE

            blockedStateContainer.visibility = View.VISIBLE

            if (blockedByMe) {
                blockedStateText.text = getString(R.string.youBlockedThisUser)
                unblockBtn.visibility = View.VISIBLE
            } else {
                blockedStateText.text = getString(R.string.youAreBlocked)
                unblockBtn.visibility = View.GONE
            }
        } else {
            blockedStateContainer.visibility = View.GONE
            chatBoxContainer.visibility = View.VISIBLE
            sendBtn.visibility = View.VISIBLE
        }
    }
    private fun handleBotCommand(command: String) {
        if (isBotProcessing) {
            Toast.makeText(requireContext(), "Bot is processing. Please wait...", Toast.LENGTH_SHORT).show()
            return
        }

        // Check rate limit
        val (canProcess, errorMsg) = BotMessageHelper.canProcessBotRequest(requireContext())
        if (!canProcess) {
            Toast.makeText(requireContext(), errorMsg, Toast.LENGTH_LONG).show()
            return
        }

        // Extract prompt
        val userPrompt = BotMessageHelper.extractPrompt(command)

        if (userPrompt.isEmpty()) {
            Toast.makeText(requireContext(), "Please provide a command after @Bot", Toast.LENGTH_SHORT).show()
            return
        }

        // Show user's command as a message
        val userCommandMsg = MsgModel(
            senderID = FirebaseAuthentication.currentUserID(),
            msg = command,
            timestamp = Timestamp.now(),
            messageType = "text"
        )

        FirebaseChat.getChatRoomMessagesReference(chatRoomID!!)
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
                    messagesRef = FirebaseChat.getChatRoomMessagesReference(chatRoomID!!),
                    currentUserId = FirebaseAuthentication.currentUserID()!!,
                    chatType = BotMessageHelper.ChatType.USER_CHAT,
                    otherUserName = otherUser?.username
                )

                // Call Gemini API
                val result = GeminiHelper.getBotResponse(requireContext(), messages, userPrompt)

                result.onSuccess { response ->
                    // Send bot response
                    BotMessageHelper.sendBotResponse(
                        messagesRef = FirebaseChat.getChatRoomMessagesReference(chatRoomID!!),
                        chatRef = FirebaseChat.getChatRoomReference(chatRoomID!!),
                        response = response,
                        chatType = BotMessageHelper.ChatType.USER_CHAT,
                        currentUserId = FirebaseAuthentication.currentUserID()!!
                    )

                    // Send usage info message
                    BotMessageHelper.sendUsageMessage(
                        context = requireContext(),
                        messagesRef = FirebaseChat.getChatRoomMessagesReference(chatRoomID!!),
                        chatType = BotMessageHelper.ChatType.USER_CHAT
                    )

                    Toast.makeText(
                        requireContext(),
                        "Bot responded!",
                        Toast.LENGTH_SHORT
                    ).show()
                }

                result.onFailure { error ->
                    Toast.makeText(
                        requireContext(),
                        "Bot error: ${error.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }

                hideBotTyping()

            } catch (e: Exception) {
                Log.e("UserChatFragment", "Bot error", e)
                hideBotTyping()
                Toast.makeText(
                    requireContext(),
                    "Failed to get bot response",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun showBotTyping() {
        isBotProcessing = true
        sendBtn.isEnabled = false
        Toast.makeText(requireContext(), "🤖 Bot is thinking...", Toast.LENGTH_SHORT).show()
    }

    private fun hideBotTyping() {
        isBotProcessing = false
        sendBtn.isEnabled = true
    }

    private fun updateChatRoom(lastMsg: String, messageType: String) {
        val currentId = FirebaseAuthentication.currentUserID()
        val otherId = otherUser?.userID

        if (currentId == null || otherId == null) return

        // You MUST include the 'userID' array here so the Security Rules
        // allow the "create" operation.
        val roomData = mapOf(
            "userID" to listOf(currentId, otherId),
            "lastMsg" to lastMsg,
            "lastMsgSenderID" to currentId,
            "lastMsgTimestamp" to Timestamp.now(),
            "deletedBy" to emptyList<String>(),
            "chatRoomID" to chatRoomID
        )

        FirebaseChat.getChatRoomReference(chatRoomID!!)
            .set(roomData, com.google.firebase.firestore.SetOptions.merge())
            .addOnFailureListener { e ->
                Log.e("UserChatFragment", "Failed to initialize/update room", e)
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

    override fun onResume() {
        super.onResume()
        // Track that user is in this chat
        chatRoomID?.let { ChatStateManager.setCurrentChat(it) }

        // Refresh blocked state whenever returning to the chat
        refreshBlockState()
    }

    override fun onPause() {
        super.onPause()
        // Clear when leaving chat
        ChatStateManager.clearCurrentChat()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        ChatStateManager.clearCurrentChat()
    }

    companion object {
        fun newInstance(otherUser: userModel): UserChatFragment {
            return UserChatFragment().apply {
                arguments = androidUtils.putUserModelInBundle(otherUser)
            }
        }
    }
}
