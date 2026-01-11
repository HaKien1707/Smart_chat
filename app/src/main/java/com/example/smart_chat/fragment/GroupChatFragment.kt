package com.example.smart_chat.fragment

import android.content.Intent
import android.net.Uri
import android.os.Build
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
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.SimpleItemAnimator
import com.bumptech.glide.Glide
import com.example.smart_chat.R
import com.example.smart_chat.activities.group_chat.GroupChatSettingsActivity
import com.example.smart_chat.adapters.group.GroupMsgRecyclerAdapter
import com.example.smart_chat.models.group.GroupMsgModel
import com.example.smart_chat.models.msg_action.ReplyMessageData
import com.example.smart_chat.models.group.groupModel
import com.example.smart_chat.models.userModel
import com.example.smart_chat.utils.AI.BotMessageHelper
import com.example.smart_chat.utils.AI.GeminiHelper
import com.example.smart_chat.utils.firebase.FirebaseAuthentication
import com.example.smart_chat.utils.firebase.FirebaseGroups
import com.example.smart_chat.utils.media.CloudinaryHelper
import com.example.smart_chat.utils.media.MediaMessageHelper
import com.example.smart_chat.utils.notification.FCMTokenManager
import com.example.smart_chat.utils.others.ChatStateManager
import com.example.smart_chat.utils.others.androidUtils
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

class GroupChatFragment : Fragment() {

    // UI Components
    private lateinit var backBtn: ImageButton
    private lateinit var groupSettingsBtn: ImageButton
    private lateinit var groupName: TextView
    private lateinit var chatBox: EditText
    private lateinit var sendBtn: ImageButton
    private lateinit var sendImageBtn: ImageButton
    private lateinit var sendFileBtn: ImageButton
    private lateinit var chatList: RecyclerView
    private lateinit var groupImage: ImageView

    // Reply preview views
    private lateinit var replyPreviewContainer: View
    private lateinit var replyText: TextView
    private lateinit var replyImage: ImageView
    private lateinit var replySenderName: TextView
    private lateinit var replyTextContainer: LinearLayout
    private lateinit var cancelReplyBtn: ImageButton

    // Data
    private var groupID: String? = null
    private var group: groupModel? = null
    private lateinit var adapter: GroupMsgRecyclerAdapter
    private var currentUserName: String? = null
    private var currentReplyData: ReplyMessageData? = null
    private var isBotProcessing = false

    // Activity Result Launchers
    private val settingsLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        loadGroupDetails()
    }

    private val imagePickerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result: ActivityResult ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            val uri = result.data?.data
            if (uri != null) {
                uploadAndSendImage(uri)
            }
        }
    }

    private val filePickerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result: ActivityResult ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            val uri = result.data?.data
            if (uri != null) {
                uploadAndSendFile(uri)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            groupID = it.getString("groupID")
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_group_chat, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (groupID == null) {
            activity?.finish()
            return
        }

        // Set status bar color to match header
        activity?.window?.let { window ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                window.statusBarColor = ContextCompat.getColor(requireContext(), R.color.lazuli)
            }
        }

        CloudinaryHelper.initCloudinary(requireContext())

        initViews(view)
        setupListeners()
        getCurrentUserName()
        loadGroupDetails()
        listenForGroupChanges()
        setupChatRecycler()
    }

    private fun initViews(view: View) {
        backBtn = view.findViewById(R.id.back_btn)
        groupSettingsBtn = view.findViewById(R.id.group_settings_btn)
        groupName = view.findViewById(R.id.group_name)
        chatBox = view.findViewById(R.id.chatBox)
        sendBtn = view.findViewById(R.id.sendBtn)
        sendImageBtn = view.findViewById(R.id.send_image_btn)
        sendFileBtn = view.findViewById(R.id.send_file_btn)
        chatList = view.findViewById(R.id.chatList)

        val profileContainer = view.findViewById<View>(R.id.profile_image_container)
        groupImage = profileContainer.findViewById(R.id.profile_image)

        // Reply preview views
        replyPreviewContainer = view.findViewById(R.id.reply_preview)
        replyText = replyPreviewContainer.findViewById(R.id.reply_text)
        replyImage = replyPreviewContainer.findViewById(R.id.reply_image)
        replySenderName = replyPreviewContainer.findViewById(R.id.reply_sender_name)
        replyTextContainer = replyPreviewContainer.findViewById(R.id.reply_text_container)
        cancelReplyBtn = replyPreviewContainer.findViewById(R.id.cancel_reply_btn)
    }

    private fun setupListeners() {
        backBtn.setOnClickListener {
            activity?.onBackPressedDispatcher?.onBackPressed()
        }

        groupSettingsBtn.setOnClickListener {
            val intent = Intent(requireContext(), GroupChatSettingsActivity::class.java)
            intent.putExtra("groupID", groupID)
            settingsLauncher.launch(intent)
        }

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

        cancelReplyBtn.setOnClickListener {
            cancelReply()
        }
    }

    private fun getCurrentUserName() {
        FirebaseAuthentication.currentUserDetails().get()
            .addOnSuccessListener { document ->
                val user = document.toObject(userModel::class.java)
                currentUserName = user?.username
            }
            .addOnFailureListener { e ->
                Log.e("GroupChatFragment", "Failed to load username: ${e.message}")
            }
    }

    private fun loadGroupDetails() {
        FirebaseGroups.getGroupReference(groupID!!).get()
            .addOnSuccessListener { document ->
                group = document.toObject(groupModel::class.java)

                groupName.text = group?.groupName ?: "Group"

                // Load group image
                val imageUrl = group?.groupImage
                if (!imageUrl.isNullOrBlank()) {
                    androidUtils.setProfileImageFromBase64(requireContext(), imageUrl, groupImage)
                } else {
                    groupImage.setImageResource(R.drawable.ic_group)
                }

                // Settings screen should be accessible for all members.
                groupSettingsBtn.visibility = View.VISIBLE
            }
            .addOnFailureListener { e ->
                Log.e("GroupChatFragment", "Failed to load group: ${e.message}")
            }
    }

    private fun listenForGroupChanges() {
        // Listen to group document changes in real-time
        FirebaseGroups.getGroupReference(groupID!!)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("GroupChatFragment", "Listen failed: ${error.message}")
                    return@addSnapshotListener
                }

                if (snapshot != null && snapshot.exists()) {
                    val updatedGroup = snapshot.toObject(groupModel::class.java)
                    val memberIDs = updatedGroup?.memberIDs ?: emptyList()

                    // Check if current user is still a member
                    if (!memberIDs.contains(FirebaseAuthentication.currentUserID())) {
                        // User was removed from group
                        Toast.makeText(
                            requireContext(),
                            "You have been removed from this group",
                            Toast.LENGTH_SHORT
                        ).show()
                        activity?.finish()
                    }
                } else {
                    // Group was deleted
                    Toast.makeText(
                        requireContext(),
                        "This group has been deleted",
                        Toast.LENGTH_SHORT
                    ).show()
                    activity?.finish()
                }
            }
    }

    private fun setupChatRecycler() {
        val query = FirebaseGroups.getGroupMessagesReference(groupID!!)
            .orderBy("timestamp", Query.Direction.ASCENDING)

        val options = FirestoreRecyclerOptions.Builder<GroupMsgModel>()
            .setQuery(query, GroupMsgModel::class.java)
            .setLifecycleOwner(viewLifecycleOwner)
            .build()

        adapter = GroupMsgRecyclerAdapter(options, requireActivity(), groupID)
        
        // Set up callbacks
        adapter.setOnReplyCallback { replyData ->
            setReplyMessage(replyData)
        }
        
        adapter.setOnScrollCallback { position ->
            scrollToPosition(position)
        }
        
        adapter.setOnGetGroupIDCallback {
            groupID ?: ""
        }

        try {
            val animator = chatList.itemAnimator
            if (animator is SimpleItemAnimator) {
                animator.supportsChangeAnimations = false
            }
            chatList.itemAnimator = null
        } catch (e: Exception) {
            Log.w("GroupChatFragment", "Failed to modify itemAnimator: ${e.message}")
        }

        val manager = LinearLayoutManager(requireContext()).apply {
            stackFromEnd = true
        }

        chatList.layoutManager = manager
        chatList.adapter = adapter

        adapter.registerAdapterDataObserver(object : RecyclerView.AdapterDataObserver() {
            override fun onItemRangeInserted(positionStart: Int, itemCount: Int) {
                super.onItemRangeInserted(positionStart, itemCount)
                chatList.post {
                    if (adapter.itemCount > 0) {
                        chatList.scrollToPosition(adapter.itemCount - 1)
                    }
                }
            }
        })
    }

    private fun sendMsgToGroup(msg: String) {
        // Check if it's a bot command
        if (BotMessageHelper.isBotCommand(msg)) {
            handleBotCommand(msg)
            return
        }

        FirebaseGroups.getGroupReference(groupID!!)
            .update(
                mapOf(
                    "lastMsg" to msg,
                    "lastMsgSenderID" to FirebaseAuthentication.currentUserID(),
                    "lastMsgTimestamp" to Timestamp.now()
                )
            )

        val msgModel = if (currentReplyData != null) {
            GroupMsgModel(
                FirebaseAuthentication.currentUserID(),
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
                FirebaseAuthentication.currentUserID(),
                currentUserName ?: "Unknown",
                msg,
                Timestamp.now()
            )
        }

        FirebaseGroups.getGroupMessagesReference(groupID!!)
            .add(msgModel)
            .addOnSuccessListener {
                chatBox.setText("")
                cancelReply()
                sendNotificationToMembers(msg)
            }
            .addOnFailureListener { e ->
                Log.e("GroupChatFragment", "Failed to send message", e)
                Toast.makeText(requireContext(), "Failed to send message", Toast.LENGTH_SHORT).show()
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

    private fun uploadAndSendImage(imageUri: Uri) {
        sendImageBtn.isEnabled = false

        MediaMessageHelper.uploadAndSendImage(
            requireContext(),
            imageUri,
            FirebaseGroups.getGroupReference(groupID!!),
            FirebaseGroups.getGroupMessagesReference(groupID!!),
            FirebaseAuthentication.currentUserID()!!,
            currentUserName,
            MediaMessageHelper.MessageType.GROUP,
            null,
            onSuccess = {
                activity?.runOnUiThread {
                    sendImageBtn.isEnabled = true
                    sendNotificationToMembers("📷 Photo")
                }
            },
            onError = {
                activity?.runOnUiThread {
                    sendImageBtn.isEnabled = true
                }
            }
        )
    }

    private fun uploadAndSendFile(fileUri: Uri) {
        sendFileBtn.isEnabled = false

        MediaMessageHelper.uploadAndSendFile(
            requireContext(),
            fileUri,
            FirebaseGroups.getGroupReference(groupID!!),
            FirebaseGroups.getGroupMessagesReference(groupID!!),
            FirebaseAuthentication.currentUserID()!!,
            currentUserName,
            MediaMessageHelper.MessageType.GROUP,
            null,
            onSuccess = {
                activity?.runOnUiThread {
                    sendFileBtn.isEnabled = true
                    sendNotificationToMembers("📎 File")
                }
            },
            onError = {
                activity?.runOnUiThread {
                    sendFileBtn.isEnabled = true
                }
            }
        )
    }

    // Reply functionality
    fun setReplyMessage(replyData: ReplyMessageData) {
        currentReplyData = replyData
        showReplyPreview(replyData)
    }

    private fun showReplyPreview(replyData: ReplyMessageData) {
        replyPreviewContainer.visibility = View.VISIBLE

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

    private fun cancelReply() {
        currentReplyData = null
        replyPreviewContainer.visibility = View.GONE
    }

    fun scrollToPosition(position: Int) {
        try {
            chatList.stopScroll()

            chatList.post {
                try {
                    if (position >= 0 && position < adapter.itemCount) {
                        chatList.scrollToPosition(position)

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
                                Log.e("GroupChatFragment", "Error highlighting message", e)
                            }
                        }, 300)
                    } else {
                        Log.w("GroupChatFragment", "Invalid scroll position: $position, itemCount: ${adapter.itemCount}")
                    }
                } catch (e: Exception) {
                    Log.e("GroupChatFragment", "Error scrolling to position", e)
                }
            }
        } catch (e: Exception) {
            Log.e("GroupChatFragment", "Error in scrollToPosition", e)
        }
    }

    private fun formatFileSize(size: Long): String {
        return when {
            size < 1024 -> "$size B"
            size < 1024 * 1024 -> "${size / 1024} KB"
            else -> String.format("%.2f MB", size / (1024.0 * 1024.0))
        }
    }

    // =============================================================================
    //                                  CHAT BOT
    // =============================================================================
    private fun handleBotCommand(command: String) {
        if (isBotProcessing) {
            Toast.makeText(requireContext(), "Bot is processing. Please wait...", Toast.LENGTH_SHORT).show()
            return
        }

        val (canProcess, errorMsg) = BotMessageHelper.canProcessBotRequest(requireContext())
        if (!canProcess) {
            Toast.makeText(requireContext(), errorMsg, Toast.LENGTH_LONG).show()
            return
        }

        val userPrompt = BotMessageHelper.extractPrompt(command)

        if (userPrompt.isEmpty()) {
            Toast.makeText(requireContext(), "Please provide a command after @Bot", Toast.LENGTH_SHORT).show()
            return
        }

        val userCommandMsg = GroupMsgModel(
            FirebaseAuthentication.currentUserID(),
            currentUserName ?: "Unknown",
            command,
            Timestamp.now()
        )

        FirebaseGroups.getGroupMessagesReference(groupID!!)
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
        Toast.makeText(requireContext(), "🤖 Bot is thinking...", Toast.LENGTH_SHORT).show()
    }

    private fun hideBotTyping() {
        isBotProcessing = false
        sendBtn.isEnabled = true
    }

    private fun processBotRequest(userPrompt: String) {
        lifecycleScope.launch {
            try {
                val messages = BotMessageHelper.fetchAndFormatMessages(
                    messagesRef = FirebaseGroups.getGroupMessagesReference(groupID!!),
                    currentUserId = FirebaseAuthentication.currentUserID()!!,
                    chatType = BotMessageHelper.ChatType.GROUP_CHAT
                )

                if (messages.isEmpty()) {
                    Toast.makeText(
                        requireContext(),
                        "No messages to analyze",
                        Toast.LENGTH_SHORT
                    ).show()
                    hideBotTyping()
                    return@launch
                }

                val result = GeminiHelper.getBotResponse(requireContext(), messages, userPrompt)

                result.onSuccess { response ->
                    BotMessageHelper.sendBotResponse(
                        messagesRef = FirebaseGroups.getGroupMessagesReference(groupID!!),
                        chatRef = FirebaseGroups.getGroupReference(groupID!!),
                        response = response,
                        chatType = BotMessageHelper.ChatType.GROUP_CHAT,
                        currentUserId = FirebaseAuthentication.currentUserID()!!,
                        currentUserName = currentUserName
                    )

                    BotMessageHelper.sendUsageMessage(
                        context = requireContext(),
                        messagesRef = FirebaseGroups.getGroupMessagesReference(groupID!!),
                        chatType = BotMessageHelper.ChatType.GROUP_CHAT
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
                Log.e("GroupChatFragment", "Bot error", e)
                hideBotTyping()
                Toast.makeText(
                    requireContext(),
                    "Failed to get bot response",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun sendNotificationToMembers(msg: String) {
        Log.d("GROUP_NOTIFICATION", "=== Starting sendNotificationToMembers ===")

        val memberIDs = group?.memberIDs ?: return

        FirebaseAuthentication.currentUserDetails().get().addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val currentUser = task.result.toObject(userModel::class.java)

                memberIDs.forEach { memberID ->
                    if (memberID != FirebaseAuthentication.currentUserID() && memberID != null) {
                        sendNotificationToMember(memberID, currentUser?.username ?: "Someone", msg)
                    }
                }
            }
        }
    }

    private fun sendNotificationToMember(memberID: String, senderName: String, msg: String) {
        FirebaseAuthentication.allUsersCollection().document(memberID).get()
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

                    callAPI(jsonObject, memberID)
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
                        val responseBody = response.body.string()
                        if (response.isSuccessful) {
                            Log.d("GROUP_NOTIFICATION", "Notification sent successfully to $memberID")
                        } else {
                            Log.e("GROUP_NOTIFICATION", "Failed: ${response.code} - $responseBody")

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
            .fromStream(requireActivity().resources.openRawResource(R.raw.service_account))
            .createScoped(listOf("https://www.googleapis.com/auth/firebase.messaging"))

        googleCredentials.refresh()
        return googleCredentials.accessToken.tokenValue
    }

    fun getGroupID(): String {
        return groupID ?: ""
    }

    override fun onResume() {
        super.onResume()
        ChatStateManager.setCurrentGroup(groupID ?: "")
    }

    override fun onPause() {
        super.onPause()
        ChatStateManager.clearCurrentChat()
    }

    override fun onDestroy() {
        super.onDestroy()
        ChatStateManager.clearCurrentChat()
    }

    companion object {
        fun newInstance(groupID: String): GroupChatFragment {
            return GroupChatFragment().apply {
                arguments = Bundle().apply {
                    putString("groupID", groupID)
                }
            }
        }
    }
}
