package com.example.Smart_Chat.activities.user_chat

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
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
import androidx.recyclerview.widget.SimpleItemAnimator
import com.example.Smart_Chat.R
import com.example.Smart_Chat.activities.user_chat.NotFriendsActivity
import com.example.Smart_Chat.adapters.MsgRecyclerAdapter
import com.example.Smart_Chat.models.MsgModel
import com.example.Smart_Chat.models.chatRoomModel
import com.example.Smart_Chat.models.userModel
import com.example.Smart_Chat.utils.CloudinaryHelper
import com.example.Smart_Chat.utils.FireBase_utils
import com.example.Smart_Chat.utils.LanguageManager
import com.example.Smart_Chat.utils.ThemeManager
import com.example.Smart_Chat.utils.androidUtils
import com.firebase.ui.firestore.FirestoreRecyclerOptions
import com.github.dhaval2404.imagepicker.ImagePicker
import com.google.auth.oauth2.GoogleCredentials
import com.google.firebase.FirebaseApp
import com.google.firebase.Timestamp
import com.google.firebase.firestore.Query
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.json.JSONObject
import java.io.IOException
import java.security.MessageDigest
import java.util.UUID

class chatActivity : AppCompatActivity() {

    private var user2nd: userModel? = null
    private lateinit var backBTN: ImageButton
    private lateinit var panelName: TextView
    private lateinit var chatBox: EditText
    private lateinit var sendBtn: ImageButton
    private lateinit var sendImageBtn: ImageButton
    private lateinit var sendFileBtn: ImageButton
    private lateinit var chatList: RecyclerView
    private lateinit var profileImage: ImageView

    private lateinit var chatRoomID: String
    private var chatRoom: chatRoomModel? = null
    private lateinit var adapter: MsgRecyclerAdapter

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
        // Apply theme and language
        ThemeManager.applySavedTheme(this)
        LanguageManager.applySavedLanguage(this)

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat)

        // Initialize Cloudinary
        CloudinaryHelper.initCloudinary(this)

        // Get user model
        user2nd = androidUtils.getUserModelFromIntent(intent)

        if (user2nd == null) {
            Log.e("chatActivity", "user2nd is null!")
            finish()
            return
        }

        // CHECK FRIENDSHIP STATUS FIRST
        checkFriendshipBeforeChat()
    }

    private fun checkFriendshipBeforeChat() {
        FireBase_utils.checkFriendshipStatus(user2nd?.userID ?: "") { status ->
            runOnUiThread {
                when (status) {
                    FireBase_utils.FriendshipStatus.FRIENDS -> {
                        // They're friends - proceed with chat setup
                        setupChat()
                    }
                    else -> {
                        // Not friends - redirect to NotFriendsActivity
                        val intent = Intent(this, NotFriendsActivity::class.java)
                        androidUtils.passUserModelAsIntent(intent, user2nd)
                        startActivity(intent)
                        finish()
                    }
                }
            }
        }
    }

    fun getChatRoomID(): String {
        return chatRoomID
    }

    private fun setupChat() {
        chatRoomID = FireBase_utils.getChatRoomID(
            user2nd?.userID,
            FireBase_utils.currentUserID()
        )

        // Initialize views
        backBTN = findViewById(R.id.back_btn)
        panelName = findViewById(R.id.panelName)
        chatBox = findViewById(R.id.chatBox)
        sendBtn = findViewById(R.id.sendBtn)
        sendImageBtn = findViewById(R.id.send_image_btn)
        sendFileBtn = findViewById(R.id.send_file_btn)  // ✅ Move here
        chatList = findViewById(R.id.chatList)
        val profileContainer = findViewById<View>(R.id.profile_image_container)
        profileImage = profileContainer.findViewById(R.id.profile_image)

        backBTN.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        panelName.text = user2nd?.username

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
                sendMsgToUser(msg)
            }
        }

        sendImageBtn.setOnClickListener {
            pickImage()
        }

        // ✅ Add file button click listener here
        sendFileBtn.setOnClickListener {
            pickFile()
        }

        getOrCreateChatRoom()
        setupChatRecycler()
    }

    private fun pickImage() {
        ImagePicker.Companion.with(this)
            .compress(512)
            .maxResultSize(1080, 1080)
            .createIntent { intent -> imagePickerLauncher.launch(intent) }
    }

    private fun uploadAndSendImage(imageUri: Uri) {
        sendImageBtn.isEnabled = false
        Toast.makeText(this, "Uploading image...", Toast.LENGTH_SHORT).show()

        val imageHash = generateImageHash(imageUri)

        CloudinaryHelper.uploadImageWithHash(
            this,
            imageUri,
            imageHash,
            onSuccess = { imageUrl ->
                runOnUiThread {
                    sendImageMessage(imageUrl)
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

    private fun generateImageHash(uri: Uri): String {
        return try {
            val inputStream = contentResolver.openInputStream(uri)
            val digest = MessageDigest.getInstance("MD5")
            val buffer = ByteArray(8192)
            var read: Int
            while (inputStream?.read(buffer).also { read = it ?: -1 } != -1) {
                digest.update(buffer, 0, read)
            }
            inputStream?.close()
            digest.digest().joinToString("") { "%02x".format(it) }
        } catch (e: Exception) {
            Log.e("chatActivity", "Hash generation failed", e)
            UUID.randomUUID().toString()
        }
    }

    private fun sendImageMessage(imageUrl: String) {
        if (imageUrl.isEmpty()) {
            Log.e("chatActivity", "Cannot send empty image URL")
            return
        }

        FireBase_utils.getChatRoomReferences(chatRoomID)
            .update(
                mapOf(
                    "lastMsg" to "📷 Photo",
                    "lastMsgSenderID" to FireBase_utils.currentUserID(),
                    "lastMsgTimestamp" to Timestamp.Companion.now()
                )
            )
            .addOnFailureListener { e ->
                Log.e("chatActivity", "Failed to update chatroom", e)
            }

        val msgModel = MsgModel(
            FireBase_utils.currentUserID(),
            "📷 Photo",
            Timestamp.Companion.now(),
            imageUrl,
            "image"
        )

        FireBase_utils.getChatRoomMessagesReferences(chatRoomID)
            .add(msgModel)
            .addOnSuccessListener {
                Log.d("chatActivity", "Image message sent successfully")
                sendNotification("📷 Photo")
            }
            .addOnFailureListener { e ->
                Log.e("chatActivity", "Failed to send image message", e)
                Toast.makeText(this, "Failed to send image", Toast.LENGTH_SHORT).show()
            }
    }

    private fun pickFile() {
        val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
            type = "*/*" // All file types
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

    private fun uploadAndSendFile(fileUri: Uri) {
        // Get file info
        val fileName = getFileName(fileUri)
        val fileSize = getFileSize(fileUri)

        // Check file size (10MB limit)
        val maxSize = 10 * 1024 * 1024 // 10MB in bytes
        if (fileSize > maxSize) {
            Toast.makeText(
                this,
                "File too large. Maximum size is 10MB. Selected: ${formatFileSize(fileSize)}",
                Toast.LENGTH_LONG
            ).show()
            return
        }

        sendFileBtn.isEnabled = false
        Toast.makeText(this, "Uploading file...", Toast.LENGTH_SHORT).show()

        CloudinaryHelper.uploadFile(
            this,
            fileUri,
            fileName,
            onSuccess = { fileUrl ->
                runOnUiThread {
                    sendFileMessage(fileUrl, fileName, fileSize)
                    sendFileBtn.isEnabled = true
                    Toast.makeText(this, "File sent!", Toast.LENGTH_SHORT).show()
                }
            },
            onError = { error ->
                runOnUiThread {
                    sendFileBtn.isEnabled = true
                    Toast.makeText(this, "Upload failed: $error", Toast.LENGTH_LONG).show()
                }
            }
        )
    }

    private fun getFileName(uri: Uri): String {
        var fileName = "file"
        contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (nameIndex != -1) {
                    fileName = cursor.getString(nameIndex)
                }
            }
        }
        return fileName
    }

    private fun getFileSize(uri: Uri): Long {
        var fileSize = 0L
        contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                if (sizeIndex != -1) {
                    fileSize = cursor.getLong(sizeIndex)
                }
            }
        }
        return fileSize
    }

    private fun formatFileSize(size: Long): String {
        return when {
            size < 1024 -> "$size B"
            size < 1024 * 1024 -> "${size / 1024} KB"
            else -> String.format("%.2f MB", size / (1024.0 * 1024.0))
        }
    }

    private fun sendFileMessage(fileUrl: String, fileName: String, fileSize: Long) {
        if (fileUrl.isEmpty()) {
            Log.e("chatActivity", "Cannot send empty file URL")
            return
        }

        FireBase_utils.getChatRoomReferences(chatRoomID)
            .update(
                mapOf(
                    "lastMsg" to "📎 $fileName",
                    "lastMsgSenderID" to FireBase_utils.currentUserID(),
                    "lastMsgTimestamp" to Timestamp.now()
                )
            )
            .addOnFailureListener { e ->
                Log.e("chatActivity", "Failed to update chatroom", e)
            }

        val msgModel = MsgModel(
            FireBase_utils.currentUserID(),
            "📎 $fileName",
            Timestamp.now(),
            fileUrl,
            fileName,
            fileSize,
            "file"
        )

        FireBase_utils.getChatRoomMessagesReferences(chatRoomID)
            .add(msgModel)
            .addOnSuccessListener {
                Log.d("chatActivity", "File message sent successfully")
                sendNotification("📎 $fileName")
            }
            .addOnFailureListener { e ->
                Log.e("chatActivity", "Failed to send file message", e)
                Toast.makeText(this, "Failed to send file", Toast.LENGTH_SHORT).show()
            }
    }

    private fun getOrCreateChatRoom() {
        FireBase_utils.getChatRoomReferences(chatRoomID).get()
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    chatRoom = task.result.toObject(chatRoomModel::class.java)

                    if (chatRoom == null) {
                        // Create new chatroom
                        chatRoom = chatRoomModel(
                            chatRoomID,
                            mutableListOf(FireBase_utils.currentUserID(), user2nd?.userID),
                            "",
                            "",
                            Timestamp.Companion.now()
                        )

                        FireBase_utils.getChatRoomReferences(chatRoomID)
                            .set(chatRoom!!)
                            .addOnSuccessListener {
                                Log.d("CHATROOM", "Created: $chatRoomID")
                            }
                            .addOnFailureListener { e ->
                                Log.e("CHATROOM", "ERROR: ${e.message}")
                            }
                    } else {
                        // Check if chat was soft-deleted by current user
                        val currentUserID = FireBase_utils.currentUserID()
                        if (chatRoom?.deletedBy?.contains(currentUserID) == true) {
                            // Auto-recover the chat when user opens it
                            FireBase_utils.recoverChatRoom(
                                chatRoomID,
                                onSuccess = {
                                    Log.d("CHATROOM", "Chat auto-recovered")
                                },
                                onFailure = { e ->
                                    Log.e("CHATROOM", "Failed to auto-recover: ${e.message}")
                                }
                            )
                        }
                    }
                }
            }
    }

    private fun sendMsgToUser(msg: String) {
        FireBase_utils.getChatRoomReferences(chatRoomID)
            .update(
                mapOf(
                    "lastMsg" to msg,
                    "lastMsgSenderID" to FireBase_utils.currentUserID(),
                    "lastMsgTimestamp" to Timestamp.Companion.now()
                )
            )

        val msgModel = MsgModel(
            FireBase_utils.currentUserID(),
            msg,
            Timestamp.Companion.now()
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
            .setLifecycleOwner(this)
            .build()

        adapter = MsgRecyclerAdapter(options, this)

        try {
            val animator = chatList.itemAnimator
            if (animator is SimpleItemAnimator) {
                animator.supportsChangeAnimations = false
            }
            chatList.itemAnimator = null
        } catch (e: Exception) {
            Log.w("chatActivity", "Failed to modify itemAnimator: ${e.message}")
        }

        val manager = LinearLayoutManager(this).apply {
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

    private fun sendNotification(msg: String) {
        val recipientToken = user2nd?.fcmToken

        if (recipientToken.isNullOrEmpty()) {
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
                        Log.e("NOTIFICATION", "Failed to send notification", e)
                    }

                    override fun onResponse(call: Call, response: Response) {
                        val responseBody = response.body?.string() ?: ""
                        if (response.isSuccessful) {
                            Log.d("NOTIFICATION", "Notification sent successfully")
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