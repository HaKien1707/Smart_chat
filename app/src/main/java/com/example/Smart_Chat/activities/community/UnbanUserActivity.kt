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
import com.example.Smart_Chat.adapters.SelectUserAdapter
import com.example.Smart_Chat.models.CommunityModel
import com.example.Smart_Chat.models.userModel
import com.example.Smart_Chat.utils.FireBase_utils
import com.example.Smart_Chat.utils.LanguageManager
import com.example.Smart_Chat.utils.ThemeManager

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
        FireBase_utils.getCommunityReference(communityID!!).get()
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
                    FireBase_utils.allUsersCollection().document(userID).get()
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
        FireBase_utils.unbanUserFromCommunity(
            communityID!!,
            user.userID ?: "",
            onSuccess = {
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