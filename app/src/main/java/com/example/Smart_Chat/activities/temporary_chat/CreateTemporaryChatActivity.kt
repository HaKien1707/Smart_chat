package com.example.Smart_Chat.activities.temporary_chat

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.Smart_Chat.R
import com.example.Smart_Chat.adapters.social.SelectUserAdapter
import com.example.Smart_Chat.models.userModel
import com.example.Smart_Chat.utils.UI.LanguageManager
import com.example.Smart_Chat.utils.UI.ThemeManager
import com.example.Smart_Chat.utils.others.androidUtils
import com.example.Smart_Chat.utils.firebase.*

class CreateTemporaryChatActivity : AppCompatActivity() {

    private lateinit var backBtn: ImageButton
    private lateinit var recyclerView: RecyclerView
    private lateinit var emptyState: LinearLayout
    private lateinit var adapter: SelectUserAdapter

    private val friendList = mutableListOf<userModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        ThemeManager.applySavedTheme(this)
        LanguageManager.applySavedLanguage(this)

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create_temporary_chat)

        backBtn = findViewById(R.id.back_btn)
        recyclerView = findViewById(R.id.user_recycler)
        emptyState = findViewById(R.id.empty_state)

        backBtn.setOnClickListener {
            finish()
        }

        setupRecycler()
        loadFriends()
    }

    private fun setupRecycler() {
        adapter = SelectUserAdapter(this, friendList) { user ->
            createTemporaryChat(user)
        }

        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter
    }

    private fun loadFriends() {
        FirebaseFriends.getAllFriends(
            onSuccess = { friendIDs ->
                if (friendIDs.isEmpty()) {
                    showEmptyState()
                    return@getAllFriends
                }

                var loadedCount = 0
                friendIDs.forEach { friendID ->
                    FirebaseAuthentication.allUsersCollection().document(friendID).get()
                        .addOnSuccessListener { userDoc ->
                            val user = userDoc.toObject(userModel::class.java)
                            if (user != null) {
                                friendList.add(user)
                            }

                            loadedCount++
                            if (loadedCount == friendIDs.size) {
                                adapter.notifyDataSetChanged()
                                if (friendList.isEmpty()) {
                                    showEmptyState()
                                } else {
                                    hideEmptyState()
                                }
                            }
                        }
                }
            },
            onFailure = { e ->
                Log.e("CreateTempChat", "Failed to load friends", e)
                showEmptyState()
            }
        )
    }

    private fun createTemporaryChat(user: userModel) {
        // Show loading state
        Toast.makeText(this, "Creating encrypted chat...", Toast.LENGTH_SHORT).show()

        FirebaseTemporaryChat.createTemporaryChat(
            user.userID ?: "",
            onSuccess = { chatID, encryptionKey ->
                // Chat created successfully with encryption key
                Toast.makeText(
                    this,
                    "🔒 Encrypted chat created (expires in 5 minutes)",
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
                Toast.makeText(
                    this,
                    "Failed to create temporary chat: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        )
    }

    private fun showEmptyState() {
        emptyState.visibility = View.VISIBLE
        recyclerView.visibility = View.GONE
    }

    private fun hideEmptyState() {
        emptyState.visibility = View.GONE
        recyclerView.visibility = View.VISIBLE
    }
}