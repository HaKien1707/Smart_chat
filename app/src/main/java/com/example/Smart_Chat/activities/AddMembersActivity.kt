package com.example.Smart_Chat

import android.os.Bundle
import android.widget.Button
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.Smart_Chat.adapters.SelectMemberAdapter
import com.example.Smart_Chat.models.groupModel
import com.example.Smart_Chat.models.userModel
import com.example.Smart_Chat.utils.FireBase_utils

class AddMembersActivity : AppCompatActivity() {

    private lateinit var backBtn: ImageButton
    private lateinit var memberRecycler: RecyclerView
    private lateinit var addBtn: Button

    private var groupID: String? = null
    private var group: groupModel? = null
    private val selectedMembers = mutableListOf<String>()
    private lateinit var memberAdapter: SelectMemberAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
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
        addBtn = findViewById(R.id.add_btn)

        backBtn.setOnClickListener { finish() }

        addBtn.setOnClickListener {
            addSelectedMembers()
        }

        loadGroupAndUsers()
    }

    private fun loadGroupAndUsers() {
        // Load group first to get existing members
        FireBase_utils.getGroupReference(groupID!!).get()
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

        FireBase_utils.allUsersCollection()
            .get()
            .addOnSuccessListener { documents ->
                val availableUsers = mutableListOf<userModel>()

                for (doc in documents) {
                    val user = doc.toObject(userModel::class.java)
                    // Add user if they're not already in the group and not the current user
                    if (user.userID !in existingMemberIDs &&
                        user.userID != FireBase_utils.currentUserID()) {
                        availableUsers.add(user)
                    }
                }

                if (availableUsers.isEmpty()) {
                    Toast.makeText(this, "No users available to add", Toast.LENGTH_SHORT).show()
                    finish()
                    return@addOnSuccessListener
                }

                setupMemberRecycler(availableUsers)
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to load users", Toast.LENGTH_SHORT).show()
                finish()
            }
    }

    private fun setupMemberRecycler(users: List<userModel>) {
        memberAdapter = SelectMemberAdapter(users, this) { userID, isSelected ->
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
        addBtn.text = "Adding..."

        // Get current members and add new ones
        val updatedMembers = group?.memberIDs?.toMutableList() ?: mutableListOf()
        updatedMembers.addAll(selectedMembers)

        // Update in Firestore
        FireBase_utils.getGroupReference(groupID!!)
            .update("memberIDs", updatedMembers)
            .addOnSuccessListener {
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
}