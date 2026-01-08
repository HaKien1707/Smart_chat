package com.example.smart_chat.activities.group_chat

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.smart_chat.R
import com.example.smart_chat.adapters.group.SelectGroupMemberAdapter
import com.example.smart_chat.models.group.groupModel
import com.example.smart_chat.models.userModel
import com.example.smart_chat.utils.UI.LanguageManager
import com.example.smart_chat.utils.UI.ThemeManager
import com.example.smart_chat.utils.firebase.*

class AddMembersActivity : AppCompatActivity() {

    private lateinit var backBtn: ImageButton
    private lateinit var memberRecycler: RecyclerView
    private lateinit var emptyState: LinearLayout
    private lateinit var addBtn: Button

    private var groupID: String? = null
    private var group: groupModel? = null
    private val selectedMembers = mutableListOf<String>()
    private lateinit var memberAdapter: SelectGroupMemberAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        // Apply theme and language
        ThemeManager.applySavedTheme(this)
        LanguageManager.applySavedLanguage(this)

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_members)

        groupID = intent.getStringExtra("groupID")

        if (groupID == null) {
            Toast.makeText(this, "Error loading group", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        backBtn = findViewById(R.id.back_btn)
        memberRecycler = findViewById(R.id.member_recycler)
        emptyState = findViewById(R.id.empty_state)
        addBtn = findViewById(R.id.add_btn)

        backBtn.setOnClickListener { finish() }

        addBtn.setOnClickListener {
            addSelectedMembers()
        }

        loadGroupAndUsers()
    }

    private fun loadGroupAndUsers() {
        // Load group first to get existing members
        FirebaseGroups.getGroupReference(groupID!!).get()
            .addOnSuccessListener { document ->
                group = document.toObject(groupModel::class.java)

                if (group == null) {
                    Toast.makeText(this, "Group not found", Toast.LENGTH_SHORT).show()
                    finish()
                    return@addOnSuccessListener
                }

                // Now load users who are NOT in the group
                loadAvailableUsers()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to load group", Toast.LENGTH_SHORT).show()
                finish()
            }
    }

    private fun loadAvailableUsers() {
        val existingMemberIDs = group?.memberIDs ?: listOf()

        FirebaseAuthentication.allUsersCollection()
            .get()
            .addOnSuccessListener { documents ->
                val availableUsers = mutableListOf<userModel>()

                for (doc in documents) {
                    val user = doc.toObject(userModel::class.java)
                    // Add user if they're not already in the group and not the current user
                    if (user.userID !in existingMemberIDs &&
                        user.userID != FirebaseAuthentication.currentUserID()) {
                        availableUsers.add(user)
                    }
                }

                if (availableUsers.isEmpty()) {
                    showEmptyState()
                }

                setupMemberRecycler(availableUsers)
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to load users", Toast.LENGTH_SHORT).show()
                finish()
            }
    }

    private fun setupMemberRecycler(users: List<userModel>) {
        memberAdapter = SelectGroupMemberAdapter(users, this) { userID, isSelected ->
            if (isSelected) {
                selectedMembers.add(userID)
            } else {
                selectedMembers.remove(userID)
            }
            updateAddButton()
        }

        memberRecycler.layoutManager = LinearLayoutManager(this)
        memberRecycler.adapter = memberAdapter
    }

    private fun updateAddButton() {
        addBtn.isEnabled = selectedMembers.isNotEmpty()
        addBtn.text = if (selectedMembers.isNotEmpty()) {
            "Add ${selectedMembers.size} member${if (selectedMembers.size > 1) "s" else ""}"
        } else {
            "Select members to add"
        }
    }

    private fun addSelectedMembers() {
        if (selectedMembers.isEmpty()) return

        addBtn.isEnabled = false
        addBtn.text = getString(R.string.adding)

        val updatedMembers = group?.memberIDs?.toMutableList() ?: mutableListOf()
        updatedMembers.addAll(selectedMembers)

        FirebaseGroups.getGroupReference(groupID!!)
            .update("memberIDs", updatedMembers)
            .addOnSuccessListener {
                // Send notification to each added member
                selectedMembers.forEach { memberID ->
                    FirebaseNotifications.createNotification(
                        type = "ADDED_TO_GROUP",
                        recipientID = memberID,
                        senderID = FirebaseAuthentication.currentUserID() ?: "",
                        senderName = "Admin",
                        groupID = groupID,
                        groupName = group?.groupName,
                        message = "You have been added to ${group?.groupName}"
                    )
                }

                Toast.makeText(
                    this,
                    "Added ${selectedMembers.size} member${if (selectedMembers.size > 1) "s" else ""}",
                    Toast.LENGTH_SHORT
                ).show()
                finish()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Failed to add members: ${e.message}", Toast.LENGTH_SHORT).show()
                addBtn.isEnabled = true
                updateAddButton()
            }
    }

    private fun showEmptyState() {
        emptyState.visibility = View.VISIBLE
        memberRecycler.visibility = View.GONE
        addBtn.visibility = View.GONE
    }
}