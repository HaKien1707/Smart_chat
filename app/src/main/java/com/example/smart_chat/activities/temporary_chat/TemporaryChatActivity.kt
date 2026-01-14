package com.example.smart_chat.activities.temporary_chat

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.CountDownTimer
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.RadioGroup
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.smart_chat.R
import com.example.smart_chat.adapters.temporary_chat.TempChatMsgAdapter
import com.example.smart_chat.models.temp_chat.DecryptedTempMessage
import com.example.smart_chat.models.msg_action.ReplyMessageData
import com.example.smart_chat.models.temp_chat.TempChatMsgModel
import com.example.smart_chat.models.temp_chat.TemporaryChatModel
import com.example.smart_chat.models.userModel
import com.example.smart_chat.utils.media.CloudinaryHelper
import com.example.smart_chat.utils.security.EncryptionUtils
import com.example.smart_chat.utils.UI.LanguageManager
import com.example.smart_chat.utils.media.MediaMessageHelper
import com.example.smart_chat.utils.UI.ThemeManager
import com.example.smart_chat.utils.others.androidUtils
import com.example.smart_chat.utils.firebase.*
import com.github.dhaval2404.imagepicker.ImagePicker
import com.google.firebase.Timestamp
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import java.util.Date

class TemporaryChatActivity : AppCompatActivity() {

    private var user2nd: userModel? = null
    private lateinit var backBTN: ImageButton
    private lateinit var moreBtn: ImageButton
    private lateinit var panelName: TextView
    private lateinit var expiryTimer: TextView
    private lateinit var securityIndicator: TextView
    private lateinit var chatBox: EditText
    private lateinit var sendBtn: ImageButton
    private lateinit var sendImageBtn: ImageButton
    private lateinit var chatList: RecyclerView
    private lateinit var profileImage: ImageView

    private lateinit var chatID: String
    private lateinit var encryptionKey: String
    private var tempChat: TemporaryChatModel? = null
    private lateinit var adapter: TempChatMsgAdapter
    private var countDownTimer: CountDownTimer? = null
    private var expiresAtMillis: Long = 0L
    private var isDeletingChat: Boolean = false

    private val decryptedMessages = mutableListOf<DecryptedTempMessage>()
    private var messageListener: ListenerRegistration? = null

    private lateinit var sendFileBtn: ImageButton

    // Reply preview views
    private lateinit var replyPreviewContainer: View
    private lateinit var replyText: TextView
    private lateinit var replyImage: ImageView
    private lateinit var replySenderName: TextView
    private lateinit var replyTextContainer: LinearLayout
    private lateinit var cancelReplyBtn: ImageButton

    private var currentReplyData: ReplyMessageData? = null

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
        setContentView(R.layout.activity_temporary_chat)

        setupImeInsets()

        // Match User chat header color.
        window.statusBarColor = ContextCompat.getColor(this, R.color.header_green)

        CloudinaryHelper.initCloudinary(this)

        user2nd = androidUtils.getUserModelFromIntent(intent)
        chatID = intent.getStringExtra("chatID") ?: ""

        if (chatID.isEmpty() || user2nd == null) {
            Log.e("TemporaryChatActivity", "chatID or user2nd is null!")
            finish()
            return
        }

        // Initialize views
        backBTN = findViewById(R.id.back_btn)
        moreBtn = findViewById(R.id.more_btn)
        panelName = findViewById(R.id.panelName)
        expiryTimer = findViewById(R.id.expiry_timer)
        securityIndicator = findViewById(R.id.security_indicator)
        chatBox = findViewById(R.id.chatBox)
        sendBtn = findViewById(R.id.sendBtn)
        sendImageBtn = findViewById(R.id.send_image_btn)
        sendFileBtn = findViewById(R.id.send_file_btn)
        chatList = findViewById(R.id.chatList)
        val profileContainer = findViewById<View>(R.id.profile_image_container)
        profileImage = profileContainer.findViewById(R.id.profile_image)

        // Reply preview views
        replyPreviewContainer = findViewById(R.id.reply_preview)
        replyText = replyPreviewContainer.findViewById(R.id.reply_text)
        replyImage = replyPreviewContainer.findViewById(R.id.reply_image)
        replySenderName = replyPreviewContainer.findViewById(R.id.reply_sender_name)
        replyTextContainer = replyPreviewContainer.findViewById(R.id.reply_text_container)
        cancelReplyBtn = replyPreviewContainer.findViewById(R.id.cancel_reply_btn)

        cancelReplyBtn.setOnClickListener {
            cancelReply()
        }

        backBTN.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        moreBtn.setOnClickListener {
            showMoreOptionsMenu()
        }

        panelName.text = "${user2nd?.username}"

        securityIndicator.text = getString(R.string.temp_chat_security_indicator)
        securityIndicator.visibility = View.VISIBLE

        val imageUrl = user2nd?.profileImage
        if (!imageUrl.isNullOrBlank()) {
            androidUtils.setProfileImageFromBase64(this, imageUrl, profileImage)
        } else {
            profileImage.setImageResource(R.drawable.ic_profile)
        }

        sendBtn.setOnClickListener {
            val msg = chatBox.text.toString().trim()
            if (msg.isNotEmpty()) {
                sendEncryptedMsg(msg)
            }
        }

        sendImageBtn.setOnClickListener {
            pickImage()
        }

        sendFileBtn.setOnClickListener {
            pickFile()
        }

        loadChatDetails()
    }

    private fun setupImeInsets() {
        val root = findViewById<View>(android.R.id.content) ?: return
        val bottomPanel = findViewById<View>(R.id.bottomPanel) ?: return

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

    private fun showMoreOptionsMenu() {
        val popupMenu = android.widget.PopupMenu(this, moreBtn)
        popupMenu.menuInflater.inflate(R.menu.menu_temp_chat_more, popupMenu.menu)

        popupMenu.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.action_extend_time -> {
                    showExtendTimeDialog()
                    true
                }
                R.id.action_delete_temp_chat -> {
                    showDeleteTempChatDialog()
                    true
                }
                else -> false
            }
        }

        popupMenu.show()
    }

    private fun showExtendTimeDialog() {
        val view = layoutInflater.inflate(R.layout.dialog_extend_temp_chat_time, null)
        val radioGroup = view.findViewById<RadioGroup>(R.id.extend_time_radio_group)
        radioGroup.check(R.id.extend_10)

        val dialog = AlertDialog.Builder(this)
            .setTitle(getString(R.string.extend_time_title))
            .setView(view)
            .setPositiveButton(getString(R.string.ok)) { _, _ ->
                val minutesToAdd = when (radioGroup.checkedRadioButtonId) {
                    R.id.extend_30 -> 30
                    R.id.extend_60 -> 60
                    else -> 10
                }
                extendTempChatTime(minutesToAdd)
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .create()

        dialog.show()
        val white = ContextCompat.getColor(this, R.color.white)
        dialog.getButton(AlertDialog.BUTTON_POSITIVE)?.setTextColor(white)
        dialog.getButton(AlertDialog.BUTTON_NEGATIVE)?.setTextColor(white)
    }

    private fun extendTempChatTime(addMinutes: Int) {
        if (!::chatID.isInitialized) return

        val now = System.currentTimeMillis()
        val base = maxOf(expiresAtMillis, now)
        val newExpiresAt = base + addMinutes * 60_000L

        FirebaseTemporaryChat.getTemporaryChatReference(chatID)
            .update("expiresAt", Timestamp(Date(newExpiresAt)))
            .addOnSuccessListener {
                expiresAtMillis = newExpiresAt
                tempChat?.expiresAt = Timestamp(Date(newExpiresAt))

                val newRemaining = newExpiresAt - System.currentTimeMillis()
                if (newRemaining > 0) {
                    countDownTimer?.cancel()
                    startCountdownTimer(newRemaining)
                }

                Toast.makeText(this, "Time extended", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Log.e("TemporaryChatActivity", "Failed to extend time", e)
                Toast.makeText(this, "Failed to extend time", Toast.LENGTH_SHORT).show()
            }
    }

    private fun showDeleteTempChatDialog() {
        val dialog = AlertDialog.Builder(this)
            .setTitle(getString(R.string.delete_temp_chat_title))
            .setMessage(getString(R.string.delete_temp_chat_message))
            .setPositiveButton(getString(R.string.ok)) { _, _ ->
                deleteTempChatAndExit()
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .create()

        dialog.show()
        val white = ContextCompat.getColor(this, R.color.white)
        dialog.getButton(AlertDialog.BUTTON_POSITIVE)?.setTextColor(white)
        dialog.getButton(AlertDialog.BUTTON_NEGATIVE)?.setTextColor(white)
    }

    private fun deleteTempChatAndExit() {
        if (!::chatID.isInitialized) {
            finish()
            return
        }

        isDeletingChat = true
        FirebaseTemporaryChat.deleteTemporaryChat(chatID)
        finish()
    }

    private fun pickImage() {
        ImagePicker.with(this)
            .compress(512)
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
            FirebaseTemporaryChat.getTemporaryChatReference(chatID),
            FirebaseTemporaryChat.getTemporaryChatMessagesReference(chatID),
            FirebaseAuthentication.currentUserID()!!,
            null,
            MediaMessageHelper.MessageType.PRIVATE_TEMP,
            encryptionKey,
            onSuccess = {
                runOnUiThread {
                    sendImageBtn.isEnabled = true
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
            FirebaseTemporaryChat.getTemporaryChatReference(chatID),
            FirebaseTemporaryChat.getTemporaryChatMessagesReference(chatID),
            FirebaseAuthentication.currentUserID()!!,
            null,
            MediaMessageHelper.MessageType.PRIVATE_TEMP,
            encryptionKey,
            onSuccess = {
                runOnUiThread {
                    sendFileBtn.isEnabled = true
                }
            },
            onError = {
                runOnUiThread {
                    sendFileBtn.isEnabled = true
                }
            }
        )
    }

    // NEW: Set reply message from adapter
    fun setReplyMessage(replyData: ReplyMessageData) {
        currentReplyData = replyData
        showReplyPreview(replyData)
    }

    // NEW: Show reply preview
    private fun showReplyPreview(replyData: ReplyMessageData) {
        replyPreviewContainer.visibility = View.VISIBLE

        // Hide sender name for 1-on-1 temp chat
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

    // NEW: Cancel reply
    private fun cancelReply() {
        currentReplyData = null
        replyPreviewContainer.visibility = View.GONE
    }

    // NEW: Scroll to specific position
    fun scrollToPosition(position: Int) {
        chatList.smoothScrollToPosition(position)

        chatList.postDelayed({
            if (position >= 0 && position < decryptedMessages.size) {
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
                }, 100)
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

    private fun startListeningForMessages() {
        messageListener = FirebaseTemporaryChat.getTemporaryChatMessagesReference(chatID)
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshots, error ->
                if (error != null) {
                    Log.e("TemporaryChatActivity", "Listen failed", error)
                    return@addSnapshotListener
                }

                if (snapshots != null) {
                    decryptedMessages.clear()

                    for (doc in snapshots.documents) {
                        try {
                            val encryptedMsg = doc.toObject(TempChatMsgModel::class.java)

                            if (encryptedMsg != null) {
                                val decryptedText = EncryptionUtils.decrypt(
                                    encryptedMsg.encryptedMsg ?: "",
                                    encryptionKey
                                )

                                val decryptedMessage = DecryptedTempMessage(
                                    senderID = encryptedMsg.senderID ?: "",
                                    msg = decryptedText,  // CORRECT parameter name
                                    timestamp = encryptedMsg.timestamp ?: Timestamp.now(),
                                    imageUrl = encryptedMsg.encryptedImageUrl?.let {
                                        try {
                                            EncryptionUtils.decrypt(it, encryptionKey)
                                        } catch (e: Exception) {
                                            null
                                        }
                                    },
                                    messageType = encryptedMsg.messageType ?: "text",
                                    isDeleted = false,  // Temp messages don't support deletion
                                    fileUrl = encryptedMsg.encryptedFileUrl?.let {
                                        try {
                                            EncryptionUtils.decrypt(it, encryptionKey)
                                        } catch (e: Exception) {
                                            null
                                        }
                                    },
                                    fileName = encryptedMsg.encryptedFileName?.let {
                                        try {
                                            EncryptionUtils.decrypt(it, encryptionKey)
                                        } catch (e: Exception) {
                                            null
                                        }
                                    },
                                    fileSize = encryptedMsg.fileSize,
                                    replyToMessageId = encryptedMsg.replyToMessageId,
                                    replyToText = encryptedMsg.encryptedReplyToText?.let {
                                        try {
                                            EncryptionUtils.decrypt(it, encryptionKey)
                                        } catch (e: Exception) {
                                            null
                                        }
                                    },
                                    replyToType = encryptedMsg.replyToType,
                                    replyToImageUrl = encryptedMsg.encryptedReplyToImageUrl?.let {
                                        try {
                                            EncryptionUtils.decrypt(it, encryptionKey)
                                        } catch (e: Exception) {
                                            null
                                        }
                                    },
                                    replyToFileName = encryptedMsg.encryptedReplyToFileName?.let {
                                        try {
                                            EncryptionUtils.decrypt(it, encryptionKey)
                                        } catch (e: Exception) {
                                            null
                                        }
                                    },
                                    replyToFileSize = encryptedMsg.replyToFileSize
                                )

                                decryptedMessages.add(decryptedMessage)
                            }
                        } catch (e: Exception) {
                            Log.e("TemporaryChatActivity", "Failed to decrypt message", e)
                        }
                    }

                    adapter.notifyDataSetChanged()

                    if (decryptedMessages.isNotEmpty()) {
                        chatList.scrollToPosition(decryptedMessages.size - 1)
                    }
                }
            }
    }

    private fun loadChatDetails() {
        FirebaseTemporaryChat.getTemporaryChatReference(chatID).get()
            .addOnSuccessListener { document ->
                tempChat = document.toObject(TemporaryChatModel::class.java)

                // Get encryption key
                encryptionKey = tempChat?.encryptionKey ?: ""

                if (encryptionKey.isEmpty()) {
                    Toast.makeText(this, "Encryption key not found", Toast.LENGTH_SHORT).show()
                    finish()
                    return@addOnSuccessListener
                }

                // Mark user as active
                FirebaseTemporaryChat.markUserAsActiveInTempChat(chatID)

                // Start countdown timer
                val expiresAt = tempChat?.expiresAt?.toDate()?.time ?: 0
                expiresAtMillis = expiresAt
                val now = System.currentTimeMillis()
                val remainingMillis = expiresAt - now

                if (remainingMillis > 0) {
                    startCountdownTimer(remainingMillis)
                    setupChatRecycler()
                    startListeningForMessages()
                } else {
                    Toast.makeText(this, "This chat has expired", Toast.LENGTH_SHORT).show()
                    finish()
                }
            }
            .addOnFailureListener { e ->
                Log.e("TemporaryChatActivity", "Failed to load chat", e)
                finish()
            }
    }

    private fun startCountdownTimer(remainingMillis: Long) {
        countDownTimer?.cancel()
        countDownTimer = object : CountDownTimer(remainingMillis, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                val minutes = (millisUntilFinished / 1000) / 60
                val seconds = (millisUntilFinished / 1000) % 60
                expiryTimer.text = String.format("Expires in %02d:%02d", minutes, seconds)
            }

            override fun onFinish() {
                Toast.makeText(
                    this@TemporaryChatActivity,
                    "This chat has expired and will be deleted",
                    Toast.LENGTH_LONG
                ).show()
                finish()
            }
        }
        countDownTimer?.start()
    }

    private fun setupChatRecycler() {
        adapter = TempChatMsgAdapter(decryptedMessages, this)

        try {
            chatList.itemAnimator = null
        } catch (e: Exception) {
            Log.w("TemporaryChatActivity", "Failed to disable itemAnimator: ${e.message}")
        }

        val manager = LinearLayoutManager(this).apply {
            stackFromEnd = true
        }

        chatList.layoutManager = manager
        chatList.adapter = adapter
    }

    private fun sendEncryptedMsg(plainText: String) {
        try {
            val encryptedText = EncryptionUtils.encrypt(plainText, encryptionKey)

            FirebaseTemporaryChat.getTemporaryChatReference(chatID)
                .update(
                    mapOf(
                        "lastMsg" to plainText,
                        "lastMsgSenderID" to FirebaseAuthentication.currentUserID(),
                        "lastMsgTimestamp" to Timestamp.now()
                    )
                )

            val msgModel = if (currentReplyData != null) {
                // Encrypt reply data
                val encryptedReplyText = currentReplyData!!.text?.let {
                    EncryptionUtils.encrypt(it, encryptionKey)
                }
                val encryptedReplyImageUrl = currentReplyData!!.imageUrl?.let {
                    EncryptionUtils.encrypt(it, encryptionKey)
                }
                val encryptedReplyFileName = currentReplyData!!.fileName?.let {
                    EncryptionUtils.encrypt(it, encryptionKey)
                }

                TempChatMsgModel(
                    FirebaseAuthentication.currentUserID(),
                    encryptedText,
                    Timestamp.now(),
                    messageType = "text",
                    replyToMessageId = currentReplyData!!.messageId,
                    encryptedReplyToText = encryptedReplyText,
                    replyToType = currentReplyData!!.type,
                    encryptedReplyToImageUrl = encryptedReplyImageUrl,
                    encryptedReplyToFileName = encryptedReplyFileName,
                    replyToFileSize = currentReplyData!!.fileSize
                )
            } else {
                TempChatMsgModel(
                    FirebaseAuthentication.currentUserID(),
                    encryptedText,
                    Timestamp.now()
                )
            }

            FirebaseTemporaryChat.getTemporaryChatMessagesReference(chatID)
                .add(msgModel)
                .addOnSuccessListener {
                    chatBox.setText("")
                    cancelReply() // NEW: Clear reply state
                }
                .addOnFailureListener { e ->
                    Log.e("TemporaryChatActivity", "Failed to send message", e)
                    Toast.makeText(this, "Failed to send message", Toast.LENGTH_SHORT).show()
                }
        } catch (e: Exception) {
            Log.e("TemporaryChatActivity", "Encryption failed", e)
            Toast.makeText(this, "Encryption failed", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        countDownTimer?.cancel()

        // Stop listening
        messageListener?.remove()

        // Clear decrypted messages from RAM
        decryptedMessages.clear()

        // Mark user as inactive (do NOT delete chat here; chat expires via expiresAt)
        if (::chatID.isInitialized && !isDeletingChat) {
            FirebaseTemporaryChat.markUserAsInactiveInTempChat(chatID)
        }
    }

    override fun onPause() {
        super.onPause()
        if (::chatID.isInitialized && !isDeletingChat) {
            FirebaseTemporaryChat.markUserAsInactiveInTempChat(chatID)
        }
    }

    override fun onResume() {
        super.onResume()
        if (::chatID.isInitialized) {
            FirebaseTemporaryChat.markUserAsActiveInTempChat(chatID)
        }
    }
}