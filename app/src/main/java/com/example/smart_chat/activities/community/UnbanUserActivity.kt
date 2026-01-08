package com.example.smart_chat.activities.community

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
import com.example.smart_chat.R
import com.example.smart_chat.adapters.social.SelectUserAdapter
import com.example.smart_chat.models.community.CommunityModel
import com.example.smart_chat.models.userModel
import com.example.smart_chat.utils.UI.LanguageManager
import com.example.smart_chat.utils.UI.ThemeManager
import com.example.smart_chat.utils.firebase.*

class UnbanUserActivity : AppCompatActivity() {

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
        setContentView(R.layout.activity_unban_user_community)

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
        loadBannedUsers()
    }

    private fun setupRecycler() {
        adapter = SelectUserAdapter(this, userList) { user ->
            showUnbanConfirmDialog(user)
        }

        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter
    }

    private fun loadBannedUsers() {
        FirebaseCommunity.getCommunityReference(communityID!!).get()
            .addOnSuccessListener { document ->
                val community = document.toObject(CommunityModel::class.java)
                val bannedUserIDs = community?.bannedUserIDs ?: emptyList()

                if (bannedUserIDs.isEmpty()) {
                    showEmptyState()
                    return@addOnSuccessListener
                }

                // Fetch user details for banned users
                var loadedCount = 0
                bannedUserIDs.forEach { userID ->
                    FirebaseAuthentication.allUsersCollection().document(userID).get()
                        .addOnSuccessListener { userDoc ->
                            val user = userDoc.toObject(userModel::class.java)
                            if (user != null) {
                                userList.add(user)
                            }

                            loadedCount++
                            if (loadedCount == bannedUserIDs.size) {
                                adapter.notifyDataSetChanged()
                                if (userList.isEmpty()) {
                                    showEmptyState()
                                } else {
                                    hideEmptyState()
                                }
                            }
                        }
                }
            }
            .addOnFailureListener { e ->
                Log.e("UnbanUser", "Failed to load banned users", e)
                showEmptyState()
            }
    }

    private fun showUnbanConfirmDialog(user: userModel) {
        AlertDialog.Builder(this)
            .setTitle("Unban User")
            .setMessage("Are you sure you want to unban ${user.username}? They will be able to send messages in this community again.")
            .setPositiveButton("Unban") { _, _ ->
                unbanUser(user)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun unbanUser(user: userModel) {
        FirebaseCommunity.unbanUserFromCommunity(
            communityID!!,
            user.userID ?: "",
            onSuccess = {
                // Send notification
                FirebaseCommunity.getCommunityReference(communityID!!).get()
                    .addOnSuccessListener { doc ->
                        val community = doc.toObject(CommunityModel::class.java)
                        FirebaseNotifications.createNotification(
                            type = "UNBANNED_FROM_COMMUNITY",
                            recipientID = user.userID ?: "",
                            senderID = FirebaseAuthentication.currentUserID() ?: "",
                            senderName = "Admin",
                            communityID = communityID,
                            communityName = community?.communityName,
                            message = "You have been unbanned from ${community?.communityName}"
                        )
                    }

                Toast.makeText(this, "${user.username} has been unbanned", Toast.LENGTH_SHORT).show()
                userList.remove(user)
                adapter.notifyDataSetChanged()

                if (userList.isEmpty()) {
                    showEmptyState()
                }
            },
            onFailure = { e ->
                Log.e("UnbanUser", "Failed to unban user", e)
                Toast.makeText(this, "Failed to unban user", Toast.LENGTH_SHORT).show()
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