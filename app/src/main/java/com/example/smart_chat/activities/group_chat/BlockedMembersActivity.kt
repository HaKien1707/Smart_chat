package com.example.smart_chat.activities.group_chat

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
import com.example.smart_chat.adapters.group.GroupBlockedMemberAdapter
import com.example.smart_chat.models.userModel
import com.example.smart_chat.utils.UI.LanguageManager
import com.example.smart_chat.utils.UI.ThemeManager
import com.example.smart_chat.utils.firebase.FirebaseAuthentication
import com.example.smart_chat.utils.firebase.FirebaseBlocking

class BlockedMembersActivity : AppCompatActivity() {

    private lateinit var backBtn: ImageButton
    private lateinit var recyclerView: RecyclerView
    private lateinit var emptyState: LinearLayout
    private lateinit var adapter: GroupBlockedMemberAdapter

    private var groupID: String? = null
    private val blockedMembers = mutableListOf<userModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        ThemeManager.applySavedTheme(this)
        LanguageManager.applySavedLanguage(this)

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_blocked_members_group)

        groupID = intent.getStringExtra("groupID")

        if (groupID == null) {
            finish()
            return
        }

        backBtn = findViewById(R.id.back_btn)
        recyclerView = findViewById(R.id.blocked_members_recycler)
        emptyState = findViewById(R.id.empty_state)

        backBtn.setOnClickListener {
            finish()
        }

        setupRecycler()
        loadBlockedMembers()
    }

    private fun setupRecycler() {
        adapter = GroupBlockedMemberAdapter(
            blockedMembers,
            this
        ) { userID ->
            showUnblockDialog(userID)
        }

        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter
    }

    private fun loadBlockedMembers() {
        FirebaseBlocking.getBlockedUsersFromGroup(
            groupID!!,
            onSuccess = { blockedIDs ->
                if (blockedIDs.isEmpty()) {
                    showEmptyState()
                    return@getBlockedUsersFromGroup
                }

                var loadedCount = 0
                blockedIDs.forEach { userID ->
                    if (userID != null) {
                        FirebaseAuthentication.allUsersCollection().document(userID).get()
                            .addOnSuccessListener { userDoc ->
                                val user = userDoc.toObject(userModel::class.java)
                                if (user != null) {
                                    blockedMembers.add(user)
                                }

                                loadedCount++
                                if (loadedCount == blockedIDs.size) {
                                    adapter.notifyDataSetChanged()
                                    if (blockedMembers.isEmpty()) {
                                        showEmptyState()
                                    } else {
                                        hideEmptyState()
                                    }
                                }
                            }
                    }
                }
            },
            onFailure = { e ->
                Log.e("BlockedMembers", "Failed to load blocked members", e)
                showEmptyState()
            }
        )
    }

    private fun showUnblockDialog(userID: String) {
        val user = blockedMembers.find { it.userID == userID }

        AlertDialog.Builder(this)
            .setTitle("Unblock Member")
            .setMessage("Unblock ${user?.username}? They will be able to rejoin the group.")
            .setPositiveButton("Unblock") { _, _ ->
                unblockMember(userID)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun unblockMember(userID: String) {
        FirebaseBlocking.unblockUserFromGroup(
            groupID!!,
            userID,
            onSuccess = {
                Toast.makeText(this, "Member unblocked", Toast.LENGTH_SHORT).show()
                blockedMembers.removeIf { it.userID == userID }
                adapter.notifyDataSetChanged()

                if (blockedMembers.isEmpty()) {
                    showEmptyState()
                }
            },
            onFailure = { e ->
                Log.e("BlockedMembers", "Failed to unblock member", e)
                Toast.makeText(this, "Failed to unblock member", Toast.LENGTH_SHORT).show()
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