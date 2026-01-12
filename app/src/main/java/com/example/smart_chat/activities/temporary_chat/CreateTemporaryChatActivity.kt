package com.example.smart_chat.activities.temporary_chat

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.ImageButton
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import android.text.Editable
import android.text.TextWatcher
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.smart_chat.R
import com.example.smart_chat.adapters.group.SelectableUserAdapter
import com.example.smart_chat.models.userModel
import com.example.smart_chat.utils.UI.LanguageManager
import com.example.smart_chat.utils.UI.ThemeManager
import com.example.smart_chat.utils.others.androidUtils
import com.example.smart_chat.utils.firebase.*

class CreateTemporaryChatActivity : AppCompatActivity() {

    private lateinit var backBtn: ImageButton
    private lateinit var recyclerView: RecyclerView
    private lateinit var searchInput: EditText
    private lateinit var emptyStateIcon: ImageView
    private lateinit var emptyStateText: TextView
    private lateinit var okBtn: ImageButton
    private lateinit var titleText: TextView

    private lateinit var adapter: SelectableUserAdapter

    private var allUsers: List<userModel> = emptyList()
    private val selectedUserIds = linkedSetOf<String>()
    private val userById = mutableMapOf<String, userModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        ThemeManager.applySavedTheme(this)
        LanguageManager.applySavedLanguage(this)

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_temp_chat)

        backBtn = findViewById(R.id.back_btn)
        recyclerView = findViewById(R.id.user_recycler)
        searchInput = findViewById(R.id.search_input)
        emptyStateIcon = findViewById(R.id.empty_state_icon)
        emptyStateText = findViewById(R.id.empty_state_text)
        okBtn = findViewById(R.id.ok_btn)
        titleText = findViewById(R.id.title)

        titleText.text = getString(R.string.add_temp_chat)

        backBtn.setOnClickListener {
            finish()
        }

        okBtn.setOnClickListener {
            goToTempChat()
        }

        setupRecycler()
        setupSearch()
        loadUsers()
        updateOkState()
    }

    private fun setupRecycler() {
        adapter = SelectableUserAdapter(
            context = this,
            selectable = true,
            singleSelection = true,
            onSelectionChanged = { userId, isSelected ->
                if (isSelected) {
                    selectedUserIds.clear()
                    selectedUserIds.add(userId)
                } else {
                    selectedUserIds.remove(userId)
                }
                updateOkState()
            }
        )

        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter
    }

    private fun setupSearch() {
        searchInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                applyFilter(s?.toString().orEmpty())
            }
        })
    }

    private fun loadUsers() {
        val currentUserId = FirebaseAuthentication.currentUserID()

        FirebaseAuthentication.allUsersCollection()
            .get()
            .addOnSuccessListener { docs ->
                val users = mutableListOf<userModel>()
                userById.clear()
                for (doc in docs) {
                    val user = doc.toObject(userModel::class.java)
                    val userId = user.userID
                    if (!userId.isNullOrBlank() && userId != currentUserId) {
                        users.add(user)
                        userById[userId] = user
                    }
                }
                allUsers = users
                applyFilter(searchInput.text?.toString().orEmpty())
            }
            .addOnFailureListener { e ->
                Log.e("CreateTempChat", "Failed to load users", e)
                allUsers = emptyList()
                userById.clear()
                applyFilter(searchInput.text?.toString().orEmpty())
            }
    }

    private fun applyFilter(query: String) {
        val q = query.trim().lowercase()
        val filtered = if (q.isBlank()) {
            allUsers
        } else {
            allUsers.filter { user ->
                val name = user.username?.lowercase().orEmpty()
                val phone = user.phoneNumber?.lowercase().orEmpty()
                name.contains(q) || phone.contains(q)
            }
        }

        adapter.submitUsers(filtered, selectedUserIds)

        val isEmpty = filtered.isEmpty()
        emptyStateIcon.visibility = if (isEmpty) android.view.View.VISIBLE else android.view.View.GONE
        emptyStateText.visibility = if (isEmpty) android.view.View.VISIBLE else android.view.View.GONE
        recyclerView.visibility = if (isEmpty) android.view.View.GONE else android.view.View.VISIBLE
    }

    private fun createTemporaryChat(user: userModel) {
        // Show loading state
        Toast.makeText(this, getString(R.string.creating_encrypted_chat), Toast.LENGTH_SHORT).show()

        FirebaseTemporaryChat.createTemporaryChat(
            user.userID ?: "",
            onSuccess = { chatID, encryptionKey ->
                // Chat created successfully with encryption key
                Toast.makeText(
                    this,
                    getString(R.string.encrypted_chat_created_expires),
                    Toast.LENGTH_LONG
                ).show()

                // Navigate directly to the chat
                val intent = Intent(this, TemporaryChatActivity::class.java)
                intent.putExtra("chatID", chatID)
                androidUtils.passUserModelAsIntent(intent, user)
                startActivity(intent)

                // Finish this activity so user doesn't come back here
                finish()
            },
            onFailure = { e ->
                Log.e("CreateTempChat", "Failed to create chat", e)
                val reason = e.message ?: getString(R.string.failed)
                Toast.makeText(
                    this,
                    getString(R.string.failed_to_create_temp_chat, reason),
                    Toast.LENGTH_SHORT
                ).show()
            }
        )
    }

    private fun updateOkState() {
        val enabled = selectedUserIds.isNotEmpty()
        okBtn.isEnabled = enabled
        okBtn.alpha = if (enabled) 1f else 0.4f
    }

    private fun goToTempChat() {
        val selectedId = selectedUserIds.firstOrNull() ?: return
        val user = userById[selectedId]
        if (user == null) {
            Toast.makeText(this, "User not found", Toast.LENGTH_SHORT).show()
            return
        }
        createTemporaryChat(user)
    }
}