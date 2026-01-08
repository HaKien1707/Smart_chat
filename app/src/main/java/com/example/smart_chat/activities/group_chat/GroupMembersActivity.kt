package com.example.smart_chat.activities.group_chat

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.smart_chat.R
import com.example.smart_chat.activities.user_chat.ChatActivity
import com.example.smart_chat.adapters.group.GroupMemberAdapter
import com.example.smart_chat.models.group.groupModel
import com.example.smart_chat.models.userModel
import com.example.smart_chat.utils.UI.LanguageManager
import com.example.smart_chat.utils.UI.ThemeManager
import com.example.smart_chat.utils.others.androidUtils
import com.example.smart_chat.utils.firebase.*

class GroupMembersActivity : AppCompatActivity() {

    private lateinit var backBtn: ImageButton
    private lateinit var titleText: TextView
    private lateinit var membersRecycler: RecyclerView
    private lateinit var emptyState: LinearLayout
    private lateinit var adapter: GroupMemberAdapter

    private var groupID: String? = null
    private var group: groupModel? = null
    private var isAdmin = false
    private val membersList = mutableListOf<Pair<userModel, Boolean>>()

    override fun onCreate(savedInstanceState: Bundle?) {
        ThemeManager.applySavedTheme(this)
        LanguageManager.applySavedLanguage(this)

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_group_members)

        groupID = intent.getStringExtra("groupID")

        if (groupID == null) {
            finish()
            return
        }

        initViews()
        loadGroupDetails()
    }

    private fun initViews() {
        backBtn = findViewById(R.id.back_btn)
        titleText = findViewById(R.id.title)
        membersRecycler = findViewById(R.id.members_recycler)
        emptyState = findViewById(R.id.empty_state)

        titleText.text = "Members"

        backBtn.setOnClickListener {
            finish()
        }

        membersRecycler.layoutManager = LinearLayoutManager(this)
    }

    private fun loadGroupDetails() {
        FirebaseGroups.getGroupReference(groupID!!).get()
            .addOnSuccessListener { document ->
                group = document.toObject(groupModel::class.java)

                if (group == null) {
                    finish()
                    return@addOnSuccessListener
                }

                isAdmin = group?.adminIDs?.contains(FirebaseAuthentication.currentUserID()) == true
                loadMembers()
            }
            .addOnFailureListener {
                Log.e("GroupMembers", "Failed to load group", it)
                finish()
            }
    }

    private fun loadMembers() {
        val memberIDs = group?.memberIDs ?: return
        membersList.clear()

        if (memberIDs.isEmpty()) {
            showEmptyState()
            return
        }

        var loadedCount = 0

        memberIDs.forEach { memberID ->
            if (memberID != null) {
                FirebaseAuthentication.allUsersCollection().document(memberID).get()
                    .addOnSuccessListener { doc ->
                        val user = doc.toObject(userModel::class.java)
                        if (user != null) {
                            val isMemberAdmin = group?.adminIDs?.contains(memberID) == true
                            membersList.add(Pair(user, isMemberAdmin))
                        }

                        loadedCount++
                        if (loadedCount == memberIDs.size) {
                            setupAdapter()
                        }
                    }
                    .addOnFailureListener {
                        loadedCount++
                        if (loadedCount == memberIDs.size) {
                            setupAdapter()
                        }
                    }
            }
        }
    }

    private fun setupAdapter() {
        adapter = GroupMemberAdapter(
            membersList,
            this,
            isAdmin,
            FirebaseAuthentication.currentUserID(),
            onMemberClick = { user ->
                openChatWithMember(user)
            },
            onRemoveMember = { userID ->
                removeMember(userID)
            },
            onBlockMember = { userID ->
                blockMember(userID)
            }
        )

        membersRecycler.adapter = adapter
        hideEmptyState()
    }

    private fun openChatWithMember(user: userModel) {
        val intent = Intent(this, ChatActivity::class.java)
        androidUtils.passUserModelAsIntent(intent, user)
        startActivity(intent)
    }

    private fun removeMember(userID: String) {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Remove Member")
            .setMessage("Are you sure you want to remove this member?")
            .setPositiveButton("Remove") { _, _ ->
                val updatedMembers = group?.memberIDs?.toMutableList()
                updatedMembers?.remove(userID)

                FirebaseGroups.getGroupReference(groupID!!)
                    .update("memberIDs", updatedMembers)
                    .addOnSuccessListener {
                        android.widget.Toast.makeText(this, "Member removed", android.widget.Toast.LENGTH_SHORT).show()

                        FirebaseNotifications.createNotification(
                            type = "REMOVED_FROM_GROUP",
                            recipientID = userID,
                            senderID = FirebaseAuthentication.currentUserID() ?: "",
                            senderName = "Admin",
                            groupID = groupID,
                            groupName = group?.groupName,
                            message = "You have been removed from ${group?.groupName}"
                        )

                        loadGroupDetails() // Reload
                    }
                    .addOnFailureListener {
                        android.widget.Toast.makeText(this, "Failed to remove member", android.widget.Toast.LENGTH_SHORT).show()
                    }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun blockMember(userID: String) {
        val member = membersList.find { it.first.userID == userID }?.first

        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Block Member")
            .setMessage("Block ${member?.username}? They will be removed from the group and won't be able to rejoin.")
            .setPositiveButton("Block & Remove") { _, _ ->
                FirebaseBlocking.blockUserFromGroup(
                    groupID!!,
                    userID,
                    onSuccess = {
                        android.widget.Toast.makeText(this, "Member blocked and removed", android.widget.Toast.LENGTH_SHORT).show()
                        loadGroupDetails()
                    },
                    onFailure = { e ->
                        android.widget.Toast.makeText(this, "Failed to block: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
                    }
                )
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showEmptyState() {
        emptyState.visibility = View.VISIBLE
        membersRecycler.visibility = View.GONE
    }

    private fun hideEmptyState() {
        emptyState.visibility = View.GONE
        membersRecycler.visibility = View.VISIBLE
    }

    override fun onResume() {
        super.onResume()
        if (::adapter.isInitialized) {
            loadGroupDetails()
        }
    }
}