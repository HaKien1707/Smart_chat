package com.example.Smart_Chat.activities.temporary_chat

import android.net.Uri
import android.os.Bundle
import android.os.CountDownTimer
import android.util.Log
import android.view.View
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.Smart_Chat.R
import com.example.Smart_Chat.adapters.TempChatMsgAdapter
import com.example.Smart_Chat.models.DecryptedTempMessage
import com.example.Smart_Chat.models.TempChatMsgModel
import com.example.Smart_Chat.models.TemporaryChatModel
import com.example.Smart_Chat.models.userModel
import com.example.Smart_Chat.utils.CloudinaryHelper
import com.example.Smart_Chat.utils.EncryptionUtils
import com.example.Smart_Chat.utils.FireBase_utils
import com.example.Smart_Chat.utils.LanguageManager
import com.example.Smart_Chat.utils.ThemeManager
import com.example.Smart_Chat.utils.androidUtils
import com.github.dhaval2404.imagepicker.ImagePicker
import com.google.firebase.Timestamp
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query

class TemporaryChatActivity : AppCompatActivity() {

    private var user2nd: userModel? = null
    private lateinit var backBTN: ImageButton
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

    private val decryptedMessages = mutableListOf<DecryptedTempMessage>()
    private var messageListener: ListenerRegistration? = null

    private val imagePickerLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result: ActivityResult ->
            if (result.resultCode == RESULT_OK) {
                val uri = result.data?.data
                if (uri != null) {
                    uploadAndSendImage(uri)
                }
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        ThemeManager.applySavedTheme(this)
        LanguageManager.applySavedLanguage(this)

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_temporary_chat)

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
        panelName = findViewById(R.id.panelName)
        expiryTimer = findViewById(R.id.expiry_timer)
        securityIndicator = findViewById(R.id.security_indicator)
        chatBox = findViewById(R.id.chatBox)
        sendBtn = findViewById(R.id.sendBtn)
        sendImageBtn = findViewById(R.id.send_image_btn)
        chatList = findViewById(R.id.chatList)
        val profileContainer = findViewById<View>(R.id.profile_image_container)
        profileImage = profileContainer.findViewById(R.id.profile_image)

        backBTN.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        panelName.text = "${user2nd?.username}"

        // Show encryption indicator
        securityIndicator.text = "🔒 Encrypted • Expires in 5 min"
        securityIndicator.visibility = View.VISIBLE

        // Load profile image
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

        loadChatDetails()
    }

    private fun loadChatDetails() {
        FireBase_utils.getTemporaryChatReference(chatID).get()
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
                FireBase_utils.markUserAsActiveInTempChat(chatID)

                // Start countdown timer
                val expiresAt = tempChat?.expiresAt?.toDate()?.time ?: 0
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

    private fun startListeningForMessages() {
        messageListener = FireBase_utils.getTemporaryChatMessagesReference(chatID)
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshots, error ->
                if (error != null) {
                    Log.e("TemporaryChatActivity", "Listen failed", error)
                    return@addSnapshotListener
                }

                if (snapshots != null) {
                    // Clear and rebuild decrypted messages list
                    decryptedMessages.clear()

                    for (doc in snapshots.documents) {
                        try {
                            val encryptedMsg = doc.toObject(TempChatMsgModel::class.java)

                            if (encryptedMsg != null) {
                                // Decrypt the message
                                val decryptedText = EncryptionUtils.decrypt(
                                    encryptedMsg.encryptedMsg ?: "",
                                    encryptionKey
                                )

                                val decryptedMessage = DecryptedTempMessage(
                                    senderID = encryptedMsg.senderID ?: "",
                                    message = decryptedText,
                                    timestamp = encryptedMsg.timestamp ?: Timestamp.now(),
                                    messageType = encryptedMsg.messageType ?: "text",
                                    imageUrl = encryptedMsg.encryptedImageUrl?.let {
                                        try {
                                            EncryptionUtils.decrypt(it, encryptionKey)
                                        } catch (e: Exception) {
                                            null
                                        }
                                    }
                                )

                                decryptedMessages.add(decryptedMessage)
                            }
                        } catch (e: Exception) {
                            Log.e("TemporaryChatActivity", "Failed to decrypt message", e)
                        }
                    }

                    // Notify adapter
                    adapter.notifyDataSetChanged()

                    // Scroll to bottom
                    if (decryptedMessages.isNotEmpty()) {
                        chatList.scrollToPosition(decryptedMessages.size - 1)
                    }
                }
            }
    }

    private fun pickImage() {
        ImagePicker.with(this)
            .compress(512)
            .maxResultSize(1080, 1080)
            .createIntent { intent -> imagePickerLauncher.launch(intent) }
    }

    private fun uploadAndSendImage(imageUri: Uri) {
        sendImageBtn.isEnabled = false
        Toast.makeText(this, "Uploading image...", Toast.LENGTH_SHORT).show()

        CloudinaryHelper.uploadImage(
            this,
            imageUri,
            onSuccess = { imageUrl ->
                runOnUiThread {
                    sendEncryptedImageMessage(imageUrl)
                    sendImageBtn.isEnabled = true
                    Toast.makeText(this, "Image sent!", Toast.LENGTH_SHORT).show()
                }
            },
            onError = { error ->
                runOnUiThread {
                    sendImageBtn.isEnabled = true
                    Toast.makeText(this, "Upload failed: $error", Toast.LENGTH_LONG).show()
                }
            }
        )
    }

    private fun sendEncryptedImageMessage(imageUrl: String) {
        try {
            // Encrypt image URL
            val encryptedImageUrl = EncryptionUtils.encrypt(imageUrl, encryptionKey)
            val encryptedPhotoText = EncryptionUtils.encrypt("📷 Photo", encryptionKey)

            FireBase_utils.getTemporaryChatReference(chatID)
                .update(
                    mapOf(
                        "lastMsg" to "📷 Photo",
                        "lastMsgSenderID" to FireBase_utils.currentUserID(),
                        "lastMsgTimestamp" to Timestamp.now()
                    )
                )

            val msgModel = TempChatMsgModel(
                FireBase_utils.currentUserID(),
                encryptedPhotoText,
                Timestamp.now(),
                encryptedImageUrl,
                "image"
            )

            FireBase_utils.getTemporaryChatMessagesReference(chatID)
                .add(msgModel)
                .addOnFailureListener { e ->
                    Log.e("TemporaryChatActivity", "Failed to send image", e)
                }
        } catch (e: Exception) {
            Log.e("TemporaryChatActivity", "Encryption failed", e)
            Toast.makeText(this, "Failed to encrypt image", Toast.LENGTH_SHORT).show()
        }
    }

    private fun sendEncryptedMsg(plainText: String) {
        try {
            // Encrypt the message
            val encryptedText = EncryptionUtils.encrypt(plainText, encryptionKey)

            FireBase_utils.getTemporaryChatReference(chatID)
                .update(
                    mapOf(
                        "lastMsg" to plainText,
                        "lastMsgSenderID" to FireBase_utils.currentUserID(),
                        "lastMsgTimestamp" to Timestamp.now()
                    )
                )

            val msgModel = TempChatMsgModel(
                FireBase_utils.currentUserID(),
                encryptedText,
                Timestamp.now()
            )

            FireBase_utils.getTemporaryChatMessagesReference(chatID)
                .add(msgModel)
                .addOnSuccessListener {
                    chatBox.setText("")
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

        // Mark user as inactive (will delete chat if both users left)
        if (::chatID.isInitialized) {
            FireBase_utils.markUserAsInactiveInTempChat(chatID) {
                Log.d("TemporaryChatActivity", "Chat deleted - both users left")
            }
        }
    }

    override fun onPause() {
        super.onPause()
        if (::chatID.isInitialized) {
            FireBase_utils.markUserAsInactiveInTempChat(chatID)
        }
    }

    override fun onResume() {
        super.onResume()
        if (::chatID.isInitialized) {
            FireBase_utils.markUserAsActiveInTempChat(chatID)
        }
    }
}