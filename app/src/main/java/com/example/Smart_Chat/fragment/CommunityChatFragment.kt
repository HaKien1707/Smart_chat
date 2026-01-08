package com.example.Smart_Chat.fragment

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.text.InputType
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
import androidx.appcompat.app.AlertDialog
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.SimpleItemAnimator
import com.bumptech.glide.Glide
import com.example.Smart_Chat.R
import com.example.Smart_Chat.activities.community.CommunitySettingsActivity
import com.example.Smart_Chat.adapters.community.CommunityMsgRecyclerAdapter
import com.example.Smart_Chat.models.community.CommunityModel
import com.example.Smart_Chat.models.community.CommunityMsgModel
import com.example.Smart_Chat.models.msg_action.ReplyMessageData
import com.example.Smart_Chat.models.userModel
import com.example.Smart_Chat.utils.AI.BotMessageHelper
import com.example.Smart_Chat.utils.AI.GeminiHelper
import com.example.Smart_Chat.utils.firebase.FirebaseAuthentication
import com.example.Smart_Chat.utils.firebase.FirebaseCommunity
import com.example.Smart_Chat.utils.media.CloudinaryHelper
import com.example.Smart_Chat.utils.media.MediaMessageHelper
import com.example.Smart_Chat.utils.others.androidUtils
import com.firebase.ui.firestore.FirestoreRecyclerOptions
import com.github.dhaval2404.imagepicker.ImagePicker
import com.google.firebase.Timestamp
import com.google.firebase.firestore.Query
import kotlinx.coroutines.launch

class CommunityChatFragment : Fragment() {

    // UI Components
    private lateinit var backBtn: ImageButton
    private lateinit var communitySettingsBtn: ImageButton
    private lateinit var communityName: TextView
    private lateinit var chatBox: EditText
    private lateinit var sendBtn: ImageButton
    private lateinit var sendImageBtn: ImageButton
    private lateinit var sendFileBtn: ImageButton
    private lateinit var chatList: RecyclerView
    private lateinit var communityImage: ImageView

    // Reply preview views
    private lateinit var replyPreviewContainer: View
    private lateinit var replyText: TextView
    private lateinit var replyImage: ImageView
    private lateinit var replySenderName: TextView
    private lateinit var replyTextContainer: LinearLayout
    private lateinit var cancelReplyBtn: ImageButton

    // Data
    private var communityID: String? = null
    private var community: CommunityModel? = null
    private lateinit var adapter: CommunityMsgRecyclerAdapter
    private var currentUserName: String? = null
    private var currentReplyData: ReplyMessageData? = null
    private var isBotProcessing = false

    // Activity Result Launchers
    private val settingsLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        loadCommunityDetails()
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
            communityID = it.getString("communityID")
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_community_chat, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (communityID == null) {
            activity?.finish()
            return
        }

        // Set status bar color to match header
        activity?.window?.let { window ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                window.statusBarColor = ContextCompat.getColor(requireContext(), R.color.header_green)
            }
        }

        CloudinaryHelper.initCloudinary(requireContext())

        initViews(view)
        setupListeners()
        checkBanStatus()
        getCurrentUserName()
        loadCommunityDetails()
        setupChatRecycler()
    }

    private fun initViews(view: View) {
        backBtn = view.findViewById(R.id.back_btn)
        communitySettingsBtn = view.findViewById(R.id.community_settings_btn)
        communityName = view.findViewById(R.id.community_name)
        chatBox = view.findViewById(R.id.chatBox)
        sendBtn = view.findViewById(R.id.sendBtn)
        sendImageBtn = view.findViewById(R.id.send_image_btn)
        sendFileBtn = view.findViewById(R.id.send_file_btn)
        chatList = view.findViewById(R.id.chatList)

        val profileContainer = view.findViewById<View>(R.id.profile_image_container)
        communityImage = profileContainer.findViewById(R.id.profile_image)

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

        communitySettingsBtn.setOnClickListener {
            val intent = Intent(requireContext(), CommunitySettingsActivity::class.java)
            intent.putExtra("communityID", communityID)
            settingsLauncher.launch(intent)
        }

        sendBtn.setOnClickListener {
            val msg = chatBox.text.toString().trim()
            if (msg.isNotEmpty()) {
                sendMsgToCommunity(msg)
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

    private fun checkBanStatus() {
        val currentUserID = FirebaseAuthentication.currentUserID() ?: return

        FirebaseCommunity.isBannedFromCommunity(communityID!!, currentUserID) { isBanned ->
            activity?.runOnUiThread {
                if (isBanned) {
                    Toast.makeText(
                        requireContext(),
                        "You are banned from this community",
                        Toast.LENGTH_LONG
                    ).show()
                    activity?.finish()
                }
            }
        }
    }

    private fun getCurrentUserName() {
        FirebaseAuthentication.currentUserDetails().get()
            .addOnSuccessListener { document ->
                val user = document.toObject(userModel::class.java)
                currentUserName = user?.username
            }
            .addOnFailureListener { e ->
                Log.e("CommunityChatFragment", "Failed to load username: ${e.message}")
            }
    }

    private fun loadCommunityDetails() {
        FirebaseCommunity.getCommunityReference(communityID!!).get()
            .addOnSuccessListener { document ->
                community = document.toObject(CommunityModel::class.java)

                communityName.text = community?.communityName ?: "Community"

                // Load community image
                val imageUrl = community?.communityImage
                if (!imageUrl.isNullOrBlank()) {
                    androidUtils.setProfileImageFromBase64(requireContext(), imageUrl, communityImage)
                } else {
                    communityImage.setImageResource(R.drawable.ic_community)
                }

                // Show/hide settings button based on admin status
                val isAdmin = community?.adminID == FirebaseAuthentication.currentUserID()
                communitySettingsBtn.visibility = if (isAdmin) View.VISIBLE else View.VISIBLE
            }
            .addOnFailureListener { e ->
                Log.e("CommunityChatFragment", "Failed to load community: ${e.message}")
            }
    }

    private fun setupChatRecycler() {
        val query = FirebaseCommunity.getCommunityMessagesReference(communityID!!)
            .orderBy("timestamp", Query.Direction.ASCENDING)

        val options = FirestoreRecyclerOptions.Builder<CommunityMsgModel>()
            .setQuery(query, CommunityMsgModel::class.java)
            .setLifecycleOwner(viewLifecycleOwner)
            .build()

        adapter = CommunityMsgRecyclerAdapter(options, requireActivity())

        try {
            val animator = chatList.itemAnimator
            if (animator is SimpleItemAnimator) {
                animator.supportsChangeAnimations = false
            }
            chatList.itemAnimator = null
        } catch (e: Exception) {
            Log.w("CommunityChatFragment", "Failed to modify itemAnimator: ${e.message}")
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

    private fun sendMsgToCommunity(msg: String) {
        // Check if it's a bot command
        if (BotMessageHelper.isBotCommand(msg)) {
            handleBotCommand(msg)
            return
        }

        FirebaseCommunity.getCommunityReference(communityID!!)
            .update(
                mapOf(
                    "lastMsg" to msg,
                    "lastMsgSenderID" to FirebaseAuthentication.currentUserID(),
                    "lastMsgTimestamp" to Timestamp.now()
                )
            )

        val msgModel = if (currentReplyData != null) {
            CommunityMsgModel(
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
            CommunityMsgModel(
                FirebaseAuthentication.currentUserID(),
                currentUserName ?: "Unknown",
                msg,
                Timestamp.now()
            )
        }

        FirebaseCommunity.getCommunityMessagesReference(communityID!!)
            .add(msgModel)
            .addOnSuccessListener {
                chatBox.setText("")
                cancelReply()
            }
            .addOnFailureListener { e ->
                Log.e("CommunityChatFragment", "Failed to send message", e)
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
            FirebaseCommunity.getCommunityReference(communityID!!),
            FirebaseCommunity.getCommunityMessagesReference(communityID!!),
            FirebaseAuthentication.currentUserID()!!,
            currentUserName,
            MediaMessageHelper.MessageType.COMMUNITY,
            null,
            onSuccess = {
                activity?.runOnUiThread {
                    sendImageBtn.isEnabled = true
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
            FirebaseCommunity.getCommunityReference(communityID!!),
            FirebaseCommunity.getCommunityMessagesReference(communityID!!),
            FirebaseAuthentication.currentUserID()!!,
            currentUserName,
            MediaMessageHelper.MessageType.COMMUNITY,
            null,
            onSuccess = {
                activity?.runOnUiThread {
                    sendFileBtn.isEnabled = true
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
        chatList.smoothScrollToPosition(position)

        chatList.postDelayed({
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
        }, 300)
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

        val userCommandMsg = CommunityMsgModel(
            FirebaseAuthentication.currentUserID(),
            currentUserName ?: "Unknown",
            command,
            Timestamp.now()
        )

        FirebaseCommunity.getCommunityMessagesReference(communityID!!)
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
                    messagesRef = FirebaseCommunity.getCommunityMessagesReference(communityID!!),
                    currentUserId = FirebaseAuthentication.currentUserID()!!,
                    chatType = BotMessageHelper.ChatType.COMMUNITY_CHAT
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
                        messagesRef = FirebaseCommunity.getCommunityMessagesReference(communityID!!),
                        chatRef = FirebaseCommunity.getCommunityReference(communityID!!),
                        response = response,
                        chatType = BotMessageHelper.ChatType.COMMUNITY_CHAT,
                        currentUserId = FirebaseAuthentication.currentUserID()!!,
                        currentUserName = currentUserName
                    )

                    BotMessageHelper.sendUsageMessage(
                        context = requireContext(),
                        messagesRef = FirebaseCommunity.getCommunityMessagesReference(communityID!!),
                        chatType = BotMessageHelper.ChatType.COMMUNITY_CHAT
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
                Log.e("CommunityChatFragment", "Bot error", e)
                hideBotTyping()
                Toast.makeText(
                    requireContext(),
                    "Failed to get bot response",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    fun getCommunityID(): String {
        return communityID ?: ""
    }

    companion object {
        fun newInstance(communityID: String): CommunityChatFragment {
            return CommunityChatFragment().apply {
                arguments = Bundle().apply {
                    putString("communityID", communityID)
                }
            }
        }
    }
}
