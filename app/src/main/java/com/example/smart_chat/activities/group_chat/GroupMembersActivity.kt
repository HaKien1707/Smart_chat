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
import com.google.firebase.firestore.FieldValue

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

                val currentUserId = FirebaseAuthentication.currentUserID()
                val ownerId = group?.ownerID ?: group?.createdBy ?: group?.adminIDs?.filterNotNull()?.firstOrNull()
                val isAdminOnly = group?.adminIDs?.contains(currentUserId) == true

                // Staff permissions (owner OR admin)
                isAdmin = (ownerId != null && ownerId == currentUserId) || isAdminOnly
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
                            val ownerId = group?.ownerID ?: group?.createdBy
                            val isMemberAdmin = memberID != ownerId && group?.adminIDs?.contains(memberID) == true
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
        val currentUserId = FirebaseAuthentication.currentUserID()
        val ownerId = group?.ownerID ?: group?.createdBy
        val currentUserIsOwner = ownerId != null && ownerId == currentUserId

        // Sort: Owner first, then Admins, then others
        membersList.sortWith(
            compareBy<Pair<userModel, Boolean>> {
                val userId = it.first.userID
                val isMemberAdmin = it.second
                when {
                    userId == ownerId -> 0
                    isMemberAdmin -> 1
                    else -> 2
                }
            }.thenBy { it.first.username ?: "" }
        )

        adapter = GroupMemberAdapter(
            members = membersList,
            context = this,
            currentUserIsAdmin = isAdmin,
            currentUserIsOwner = currentUserIsOwner,
            currentUserID = currentUserId,
            ownerID = ownerId,
            onChatMember = { user -> openChatWithMember(user) },
            onAddAdmin = { userId -> addAdminForMember(userId) },
            onRemoveAdmin = { userId -> removeAdminForMember(userId) },
            onRemoveMember = { userID -> removeMember(userID) }
        )

        membersRecycler.adapter = adapter
        hideEmptyState()
    }

    private fun addAdminForMember(userId: String) {
        val id = groupID ?: return
        val currentUserId = FirebaseAuthentication.currentUserID() ?: return
        val ownerId = group?.ownerID ?: group?.createdBy
        val currentUserIsOwner = ownerId != null && ownerId == currentUserId
        if (!currentUserIsOwner) return
        if (userId == ownerId) return

        FirebaseGroups.getGroupReference(id)
            .update("adminIDs", FieldValue.arrayUnion(userId))
            .addOnSuccessListener {
                android.widget.Toast.makeText(this, "Admin added", android.widget.Toast.LENGTH_SHORT).show()
                loadGroupDetails()
            }
            .addOnFailureListener {
                android.widget.Toast.makeText(this, it.message ?: "Failed", android.widget.Toast.LENGTH_SHORT).show()
            }
    }

    private fun removeAdminForMember(userId: String) {
        val id = groupID ?: return
        val currentUserId = FirebaseAuthentication.currentUserID() ?: return
        val ownerId = group?.ownerID ?: group?.createdBy
        val currentUserIsOwner = ownerId != null && ownerId == currentUserId
        if (!currentUserIsOwner) return
        if (userId == ownerId) return

        FirebaseGroups.getGroupReference(id)
            .update("adminIDs", FieldValue.arrayRemove(userId))
            .addOnSuccessListener {
                android.widget.Toast.makeText(this, "Admin removed", android.widget.Toast.LENGTH_SHORT).show()
                loadGroupDetails()
            }
            .addOnFailureListener {
                android.widget.Toast.makeText(this, it.message ?: "Failed", android.widget.Toast.LENGTH_SHORT).show()
            }
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