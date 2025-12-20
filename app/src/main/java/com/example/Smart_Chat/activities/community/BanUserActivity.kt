package com.example.Smart_Chat.activities.community

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.Smart_Chat.R
import com.example.Smart_Chat.adapters.social.SelectUserAdapter
import com.example.Smart_Chat.models.community.CommunityModel
import com.example.Smart_Chat.models.userModel
import com.example.Smart_Chat.utils.UI.LanguageManager
import com.example.Smart_Chat.utils.UI.ThemeManager
import com.example.Smart_Chat.utils.firebase.*

class BanUserActivity : AppCompatActivity() {

    private lateinit var backBtn: ImageButton
    private lateinit var recyclerView: RecyclerView
    private lateinit var emptyState: LinearLayout
    private lateinit var adapter: SelectUserAdapter

    private var communityID: String? = null
    private val userList = mutableListOf<userModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        ThemeManager.applySavedTheme(this)
        LanguageManager.applySavedLanguage(this)

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ban_user_community)

        communityID = intent.getStringExtra("communityID")

        if (communityID == null) {
            finish()
            return
        }

        backBtn = findViewById(R.id.back_btn)
        recyclerView = findViewById(R.id.user_recycler)
        emptyState = findViewById(R.id.empty_state)

        backBtn.setOnClickListener {
            finish()
        }

        setupRecycler()
        loadCommunityMembers()
    }

    private fun setupRecycler() {
        adapter = SelectUserAdapter(this, userList) { user ->
            showBanConfirmDialog(user)
        }

        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter
    }

    private fun loadCommunityMembers() {
        // Load ALL users from database (except current user and already banned users)
        FirebaseCommunity.getCommunityReference(communityID!!).get()
            .addOnSuccessListener { communityDoc ->
                val community = communityDoc.toObject(CommunityModel::class.java)
                val bannedUserIDs = community?.bannedUserIDs ?: emptyList()

                // Get all users
                FirebaseAuthentication.allUsersCollection().get()
                    .addOnSuccessListener { usersSnapshot ->
                        usersSnapshot.forEach { userDoc ->
                            val user = userDoc.toObject(userModel::class.java)

                            // Exclude current user and already banned users
                            if (user.userID != FirebaseAuthentication.currentUserID() &&
                                !bannedUserIDs.contains(user.userID)) {
                                userList.add(user)
                            }
                        }

                        adapter.notifyDataSetChanged()

                        if (userList.isEmpty()) {
                            showEmptyState()
                        } else {
                            hideEmptyState()
                        }
                    }
                    .addOnFailureListener { e ->
                        Log.e("BanUser", "Failed to load users", e)
                        showEmptyState()
                    }
            }
            .addOnFailureListener { e ->
                Log.e("BanUser", "Failed to load community", e)
                showEmptyState()
            }
    }

    private fun showBanConfirmDialog(user: userModel) {
        AlertDialog.Builder(this)
            .setTitle("Ban User")
            .setMessage("Are you sure you want to ban ${user.username}? They will no longer be able to send messages in this community.")
            .setPositiveButton("Ban") { _, _ ->
                banUser(user)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun banUser(user: userModel) {
        FirebaseCommunity.banUserFromCommunity(
            communityID!!,
            user.userID ?: "",
            onSuccess = {
                // Send notification
                FirebaseCommunity.getCommunityReference(communityID!!).get()
                    .addOnSuccessListener { doc ->
                        val community = doc.toObject(CommunityModel::class.java)
                        FirebaseNotifications.createNotification(
                            type = "BANNED_FROM_COMMUNITY",
                            recipientID = user.userID ?: "",
                            senderID = FirebaseAuthentication.currentUserID() ?: "",
                            senderName = "Admin",
                            communityID = communityID,
                            communityName = community?.communityName,
                            message = "You have been banned from ${community?.communityName}"
                        )
                    }

                Toast.makeText(this, "${user.username} has been banned", Toast.LENGTH_SHORT).show()
                userList.remove(user)
                adapter.notifyDataSetChanged()

                if (userList.isEmpty()) {
                    showEmptyState()
                }
            },
            onFailure = { e ->
                Log.e("BanUser", "Failed to ban user", e)
                Toast.makeText(this, "Failed to ban user", Toast.LENGTH_SHORT).show()
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