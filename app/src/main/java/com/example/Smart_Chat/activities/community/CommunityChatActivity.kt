package com.example.Smart_Chat.activities.community

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.InputType
import android.util.Log
import android.view.View
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.SimpleItemAnimator
import com.example.Smart_Chat.R
import com.example.Smart_Chat.adapters.CommunityMsgRecyclerAdapter
import com.example.Smart_Chat.models.CommunityModel
import com.example.Smart_Chat.models.CommunityMsgModel
import com.example.Smart_Chat.models.userModel
import com.example.Smart_Chat.utils.CloudinaryHelper
import com.example.Smart_Chat.utils.FireBase_utils
import com.example.Smart_Chat.utils.LanguageManager
import com.example.Smart_Chat.utils.MediaMessageHelper
import com.example.Smart_Chat.utils.ThemeManager
import com.example.Smart_Chat.utils.androidUtils
import com.firebase.ui.firestore.FirestoreRecyclerOptions
import com.github.dhaval2404.imagepicker.ImagePicker
import com.google.firebase.Timestamp
import com.google.firebase.firestore.Query

class CommunityChatActivity : AppCompatActivity() {

    private lateinit var backBTN: ImageButton
    private lateinit var communitySettingsBtn: ImageButton
    private lateinit var panelName: TextView
    private lateinit var chatBox: EditText
    private lateinit var sendBtn: ImageButton
    private lateinit var sendImageBtn: ImageButton
    private lateinit var chatList: RecyclerView
    private lateinit var communityImage: ImageView
    private lateinit var announcementCard: CardView
    private lateinit var announcementText: TextView
    private lateinit var announcementActionBtn: ImageButton

    private var isAnnouncementExpanded = false

    private val settingsLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        loadCommunityDetails()
    }

    private lateinit var communityID: String
    private var communityName: String? = null
    private var community: CommunityModel? = null
    private lateinit var adapter: CommunityMsgRecyclerAdapter
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
        setContentView(R.layout.activity_community_chat)

        CloudinaryHelper.initCloudinary(this)

        communityID = intent.getStringExtra("communityID") ?: ""
        communityName = intent.getStringExtra("communityName")

        if (communityID.isEmpty()) {
            Log.e("CommunityChatActivity", "communityID is null or empty!")
            finish()
            return
        }

        checkBanStatus()

        // Initialize views
        backBTN = findViewById(R.id.back_btn)
        communitySettingsBtn = findViewById(R.id.community_settings_btn)
        panelName = findViewById(R.id.panelName)
        chatBox = findViewById(R.id.chatBox)
        sendBtn = findViewById(R.id.sendBtn)
        sendImageBtn = findViewById(R.id.send_image_btn)
        sendFileBtn = findViewById(R.id.send_file_btn)
        chatList = findViewById(R.id.chatList)
        announcementCard = findViewById(R.id.announcement_card)
        announcementText = findViewById(R.id.announcement_text)
        announcementActionBtn = findViewById(R.id.announcement_action_btn)
        val profileContainer = findViewById<View>(R.id.profile_image_container)
        communityImage = profileContainer.findViewById(R.id.profile_image)

        backBTN.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        communitySettingsBtn.setOnClickListener {
            val intent = Intent(this, CommunitySettingsActivity::class.java)
            intent.putExtra("communityID", communityID)
            settingsLauncher.launch(intent)
        }

        announcementCard.setOnClickListener {
            toggleAnnouncementExpansion()
        }

        announcementActionBtn.setOnClickListener {
            handleAnnouncementAction()
        }

        panelName.text = communityName

        getCurrentUserName()
        loadCommunityDetails()

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
            FireBase_utils.getCommunityReference(communityID),
            FireBase_utils.getCommunityMessagesReference(communityID),
            FireBase_utils.currentUserID()!!,
            currentUserName,
            MediaMessageHelper.MessageType.COMMUNITY,
            null,
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
            FireBase_utils.getCommunityReference(communityID),
            FireBase_utils.getCommunityMessagesReference(communityID),
            FireBase_utils.currentUserID()!!,
            currentUserName,
            MediaMessageHelper.MessageType.COMMUNITY,
            null,
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

    private fun toggleAnnouncementExpansion() {
        if (community?.announcement.isNullOrEmpty()) return

        isAnnouncementExpanded = !isAnnouncementExpanded

        if (isAnnouncementExpanded) {
            announcementText.maxLines = Int.MAX_VALUE
        } else {
            announcementText.maxLines = 1
        }
    }

    private fun handleAnnouncementAction() {
        val isAdmin = community?.adminID == FireBase_utils.currentUserID()
        if (!isAdmin) return

        if (community?.announcement.isNullOrEmpty()) {
            // Add announcement
            showAddAnnouncementDialog()
        } else {
            // Delete announcement
            showDeleteAnnouncementDialog()
        }
    }

    private fun showAddAnnouncementDialog() {
        val input = EditText(this)
        input.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE
        input.hint = "Enter announcement"
        input.maxLines = 5

        AlertDialog.Builder(this)
            .setTitle("Add Announcement")
            .setView(input)
            .setPositiveButton("Add") { _, _ ->
                val announcement = input.text.toString().trim()
                if (announcement.isNotEmpty()) {
                    updateAnnouncement(announcement)
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showDeleteAnnouncementDialog() {
        AlertDialog.Builder(this)
            .setTitle("Delete Announcement")
            .setMessage("Are you sure you want to delete the announcement?")
            .setPositiveButton("Delete") { _, _ ->
                updateAnnouncement(null)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun updateAnnouncement(announcement: String?) {
        FireBase_utils.getCommunityReference(communityID)
            .update("announcement", announcement)
            .addOnSuccessListener {
                community?.announcement = announcement
                updateAnnouncementUI()
                Toast.makeText(
                    this,
                    if (announcement == null) "Announcement deleted" else "Announcement updated",
                    Toast.LENGTH_SHORT
                ).show()
            }
            .addOnFailureListener { e ->
                Log.e("CommunityChatActivity", "Failed to update announcement", e)
                Toast.makeText(this, "Failed to update announcement", Toast.LENGTH_SHORT).show()
            }
    }

    private fun updateAnnouncementUI() {
        val announcement = community?.announcement
        val isAdmin = community?.adminID == FireBase_utils.currentUserID()

        if (announcement.isNullOrEmpty()) {
            announcementText.text = "No announcement"
            announcementText.maxLines = 1

            if (isAdmin) {
                announcementActionBtn.visibility = View.VISIBLE
                announcementActionBtn.setImageResource(R.drawable.ic_add)
            } else {
                announcementActionBtn.visibility = View.GONE
            }
        } else {
            announcementText.text = announcement
            announcementText.maxLines = 1
            isAnnouncementExpanded = false

            if (isAdmin) {
                announcementActionBtn.visibility = View.VISIBLE
                announcementActionBtn.setImageResource(R.drawable.ic_delete)
            } else {
                announcementActionBtn.visibility = View.GONE
            }
        }
    }

    private fun checkBanStatus() {
        val currentUserID = FireBase_utils.currentUserID() ?: return

        FireBase_utils.isBannedFromCommunity(communityID, currentUserID) { isBanned ->
            runOnUiThread {
                if (isBanned) {
                    Toast.makeText(
                        this,
                        "You are banned from this community",
                        Toast.LENGTH_LONG
                    ).show()
                    finish()
                }
            }
        }
    }

    private fun getCurrentUserName() {
        FireBase_utils.currentUserDetails().get()
            .addOnSuccessListener { document ->
                val user = document.toObject(userModel::class.java)
                currentUserName = user?.username
            }
            .addOnFailureListener { e ->
                Log.e("CommunityChatActivity", "Failed to load username: ${e.message}")
            }
    }

    private fun loadCommunityDetails() {
        FireBase_utils.getCommunityReference(communityID).get()
            .addOnSuccessListener { document ->
                community = document.toObject(CommunityModel::class.java)

                panelName.text = community?.communityName ?: communityName

                // Load community image
                val imageUrl = community?.communityImage
                if (!imageUrl.isNullOrBlank()) {
                    androidUtils.setProfileImageFromBase64(this, imageUrl, communityImage)
                } else {
                    communityImage.setImageResource(R.drawable.ic_community)
                }

                // Show/hide settings button based on admin status
                val isAdmin = community?.adminID == FireBase_utils.currentUserID()
                communitySettingsBtn.visibility = if (isAdmin) View.VISIBLE else View.GONE

                // Update announcement UI
                updateAnnouncementUI()
            }
            .addOnFailureListener { e ->
                Log.e("CommunityChatActivity", "Failed to load community: ${e.message}")
            }
    }

    private fun sendMsgToCommunity(msg: String) {
        FireBase_utils.getCommunityReference(communityID)
            .update(
                mapOf(
                    "lastMsg" to msg,
                    "lastMsgSenderID" to FireBase_utils.currentUserID(),
                    "lastMsgTimestamp" to Timestamp.now()
                )
            )

        val msgModel = CommunityMsgModel(
            FireBase_utils.currentUserID(),
            currentUserName ?: "Unknown",
            msg,
            Timestamp.now()
        )

        FireBase_utils.getCommunityMessagesReference(communityID)
            .add(msgModel)
            .addOnSuccessListener {
                chatBox.setText("")
            }
            .addOnFailureListener { e ->
                Log.e("CommunityChatActivity", "Failed to send message", e)
                Toast.makeText(this, "Failed to send message", Toast.LENGTH_SHORT).show()
            }
    }

    private fun setupChatRecycler() {
        val query = FireBase_utils.getCommunityMessagesReference(communityID)
            .orderBy("timestamp", Query.Direction.ASCENDING)

        val options = FirestoreRecyclerOptions.Builder<CommunityMsgModel>()
            .setQuery(query, CommunityMsgModel::class.java)
            .setLifecycleOwner(this)
            .build()

        adapter = CommunityMsgRecyclerAdapter(options, this)

        try {
            val animator = chatList.itemAnimator
            if (animator is SimpleItemAnimator) {
                animator.supportsChangeAnimations = false
            }
            chatList.itemAnimator = null
        } catch (e: Exception) {
            Log.w("CommunityChatActivity", "Failed to modify itemAnimator: ${e.message}")
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

    fun getCommunityID(): String {
        return communityID
    }
}