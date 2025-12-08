package com.example.Smart_Chat.activities

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.Smart_Chat.R
import com.example.Smart_Chat.adapters.ForwardChatAdapter
import com.example.Smart_Chat.models.*
import com.example.Smart_Chat.utils.FireBase_utils
import com.example.Smart_Chat.utils.LanguageManager
import com.example.Smart_Chat.utils.ThemeManager
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.Timestamp

class ForwardMessageActivity : AppCompatActivity() {

    private lateinit var cancelBtn: ImageButton
    private lateinit var messageText: TextView
    private lateinit var messageImage: ImageView
    private lateinit var chatsRecycler: RecyclerView
    private lateinit var sendFab: FloatingActionButton

    private lateinit var adapter: ForwardChatAdapter
    private val chatList = mutableListOf<ForwardChatItemModel>()

    private var forwardMessageText: String? = null
    private var forwardImageUrl: String? = null
    private var forwardMessageType: String = "text"
    private var isFromGroup: Boolean = false
    private var currentChatId: String? = null // NEW: Track current chat to exclude

    override fun onCreate(savedInstanceState: Bundle?) {
        ThemeManager.applySavedTheme(this)
        LanguageManager.applySavedLanguage(this)

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_forward_message)

        // Get message data from intent
        forwardMessageText = intent.getStringExtra("messageText")
        forwardImageUrl = intent.getStringExtra("imageUrl")
        forwardMessageType = intent.getStringExtra("messageType") ?: "text"
        isFromGroup = intent.getBooleanExtra("isFromGroup", false)
        currentChatId = intent.getStringExtra("currentChatId") // NEW: Get current chat ID

        // Initialize views
        cancelBtn = findViewById(R.id.cancel_btn)
        messageText = findViewById(R.id.message_text)
        messageImage = findViewById(R.id.message_image)
        chatsRecycler = findViewById(R.id.chats_recycler)
        sendFab = findViewById(R.id.send_fab)

        cancelBtn.setOnClickListener {
            finish()
        }

        // Show message preview
        if (forwardMessageType == "image" && !forwardImageUrl.isNullOrEmpty()) {
            messageText.visibility = View.GONE
            messageImage.visibility = View.VISIBLE
            Glide.with(this)
                .load(forwardImageUrl)
                .into(messageImage)
        } else {
            messageText.visibility = View.VISIBLE
            messageImage.visibility = View.GONE
            messageText.text = forwardMessageText
        }

        // Setup recycler
        setupRecycler()

        // Load chats
        loadChatsAndGroups()

        // Send button
        sendFab.setOnClickListener {
            forwardToSelectedChats()
        }
    }

    private fun setupRecycler() {
        adapter = ForwardChatAdapter(this, chatList) { selectedChats ->
            if (selectedChats.isNotEmpty()) {
                sendFab.show()
            } else {
                sendFab.hide()
            }
        }

        chatsRecycler.layoutManager = LinearLayoutManager(this)
        chatsRecycler.adapter = adapter
    }

    private fun loadChatsAndGroups() {
        val currentUserID = FireBase_utils.currentUserID() ?: return

        // Load recent chats (1-on-1)
        FireBase_utils.allChatRoomsCollectionReference()
            .whereArrayContains("userID", currentUserID)
            .orderBy("lastMsgTimestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { chatDocs ->
                chatDocs.forEach { doc ->
                    val chatRoom = doc.toObject(chatRoomModel::class.java)

                    // Skip if soft-deleted
                    if (chatRoom.deletedBy.contains(currentUserID)) {
                        return@forEach
                    }

                    // NEW: Skip if this is the current chat (don't forward to same chat)
                    if (chatRoom.chatRoomID == currentChatId) {
                        return@forEach
                    }

                    FireBase_utils.get2ndUserInChatRoom(chatRoom.userID)?.get()
                        ?.addOnSuccessListener { userDoc ->
                            val user = userDoc.toObject(userModel::class.java)
                            if (user != null) {
                                chatList.add(
                                    ForwardChatItemModel(
                                        id = chatRoom.chatRoomID ?: "",
                                        name = user.username ?: "Unknown",
                                        imageUrl = user.profileImage,
                                        type = ForwardChatType.USER
                                    )
                                )
                                adapter.notifyItemInserted(chatList.size - 1)
                            }
                        }
                }
            }

        // Load groups
        FireBase_utils.allGroupsCollection()
            .whereArrayContains("memberIDs", currentUserID)
            .orderBy("lastMsgTimestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { groupDocs ->
                groupDocs.forEach { doc ->
                    val group = doc.toObject(groupModel::class.java)

                    // NEW: Skip if this is the current group
                    if (group.groupID == currentChatId) {
                        return@forEach
                    }

                    chatList.add(
                        ForwardChatItemModel(
                            id = group.groupID ?: "",
                            name = group.groupName ?: "Unknown Group",
                            imageUrl = group.groupImage,
                            type = ForwardChatType.GROUP
                        )
                    )
                    adapter.notifyItemInserted(chatList.size - 1)
                }
            }
    }

    private fun forwardToSelectedChats() {
        val selectedChats = chatList.filter { it.isSelected }

        if (selectedChats.isEmpty()) {
            Toast.makeText(this, "Please select at least one chat", Toast.LENGTH_SHORT).show()
            return
        }

        var successCount = 0
        val totalCount = selectedChats.size

        selectedChats.forEach { chat ->
            when (chat.type) {
                ForwardChatType.USER -> forwardToUser(chat.id) {
                    successCount++
                    checkCompletion(successCount, totalCount)
                }
                ForwardChatType.GROUP -> forwardToGroup(chat.id) {
                    successCount++
                    checkCompletion(successCount, totalCount)
                }
            }
        }
    }

    private fun forwardToUser(chatRoomID: String, onComplete: () -> Unit) {
        val msgModel = MsgModel(
            FireBase_utils.currentUserID(),
            forwardMessageText,
            Timestamp.now(),
            forwardImageUrl,
            forwardMessageType
        )

        // Update chatroom
        FireBase_utils.getChatRoomReferences(chatRoomID)
            .update(
                mapOf(
                    "lastMsg" to (forwardMessageText ?: "📷 Photo"),
                    "lastMsgSenderID" to FireBase_utils.currentUserID(),
                    "lastMsgTimestamp" to Timestamp.now()
                )
            )

        // Send message
        FireBase_utils.getChatRoomMessagesReferences(chatRoomID)
            .add(msgModel)
            .addOnSuccessListener {
                onComplete()
            }
            .addOnFailureListener { e ->
                Log.e("ForwardMessage", "Failed to forward to user", e)
                onComplete()
            }
    }

    private fun forwardToGroup(groupID: String, onComplete: () -> Unit) {
        // Get current user name
        FireBase_utils.currentUserDetails().get()
            .addOnSuccessListener { doc ->
                val user = doc.toObject(userModel::class.java)

                val msgModel = GroupMsgModel(
                    FireBase_utils.currentUserID(),
                    user?.username ?: "Unknown",
                    forwardMessageText,
                    Timestamp.now(),
                    forwardImageUrl,
                    forwardMessageType
                )

                // Update group
                FireBase_utils.getGroupReference(groupID)
                    .update(
                        mapOf(
                            "lastMsg" to (forwardMessageText ?: "📷 Photo"),
                            "lastMsgSenderID" to FireBase_utils.currentUserID(),
                            "lastMsgTimestamp" to Timestamp.now()
                        )
                    )

                // Send message
                FireBase_utils.getGroupMessagesReference(groupID)
                    .add(msgModel)
                    .addOnSuccessListener {
                        onComplete()
                    }
                    .addOnFailureListener { e ->
                        Log.e("ForwardMessage", "Failed to forward to group", e)
                        onComplete()
                    }
            }
    }

    private fun checkCompletion(successCount: Int, totalCount: Int) {
        if (successCount == totalCount) {
            Toast.makeText(this, "Message forwarded successfully", Toast.LENGTH_SHORT).show()
            finish()
        }
    }
}