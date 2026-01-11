package com.example.smart_chat.activities.group_chat

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.smart_chat.R
import com.example.smart_chat.models.group.groupModel
import com.example.smart_chat.models.userModel
import com.example.smart_chat.utils.UI.LanguageManager
import com.example.smart_chat.utils.UI.ThemeManager
import com.example.smart_chat.utils.firebase.*
import com.google.firebase.firestore.FieldValue

class AddMembersActivity : AppCompatActivity() {

    private lateinit var backBtn: ImageButton
    private lateinit var searchInput: EditText
    private lateinit var recycler: RecyclerView
    private lateinit var emptyStateIcon: ImageView
    private lateinit var emptyStateText: TextView
    private lateinit var doneBtn: ImageButton

    private var groupID: String? = null
    private var group: groupModel? = null

    private var allUsers: List<userModel> = emptyList()
    private var usersNotInGroup: List<userModel> = emptyList()
    private var existingMemberIds: Set<String> = emptySet()
    private val selectedUserIds = linkedSetOf<String>()

    private lateinit var adapter: com.example.smart_chat.adapters.group.SelectableUserAdapter

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
        searchInput = findViewById(R.id.search_input)
        recycler = findViewById(R.id.user_recycler)
        emptyStateIcon = findViewById(R.id.empty_state_icon)
        emptyStateText = findViewById(R.id.empty_state_text)
        doneBtn = findViewById(R.id.next_btn)

        backBtn.setOnClickListener { finish() }

        doneBtn.setOnClickListener { addSelectedMembers() }

        adapter = com.example.smart_chat.adapters.group.SelectableUserAdapter(
            context = this,
            selectable = true,
            onSelectionChanged = { userId, isSelected ->
                if (isSelected) selectedUserIds.add(userId) else selectedUserIds.remove(userId)
                updateDoneState()
            }
        )

        recycler.layoutManager = LinearLayoutManager(this)
        recycler.adapter = adapter

        searchInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                applyFilter(s?.toString().orEmpty())
            }
        })

        loadGroupAndUsers()
        updateDoneState()
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
        val currentUserId = FirebaseAuthentication.currentUserID()
        val blockedIDs = group?.blockedUserIDs ?: emptyList()
        existingMemberIds = group?.memberIDs?.filterNotNull()?.toSet().orEmpty()

        FirebaseAuthentication.allUsersCollection()
            .get()
            .addOnSuccessListener { documents ->
                val availableUsers = mutableListOf<userModel>()

                for (doc in documents) {
                    val user = doc.toObject(userModel::class.java)
                    val userId = user.userID
                    // Match Create Group behavior: show all users except current user.
                    // Also hide blocked users.
                    if (!userId.isNullOrBlank() &&
                        userId != currentUserId &&
                        !blockedIDs.contains(userId)) {
                        availableUsers.add(user)
                    }
                }

                allUsers = availableUsers
                usersNotInGroup = availableUsers.filter { user ->
                    val id = user.userID
                    !id.isNullOrBlank() && !existingMemberIds.contains(id)
                }

                applyFilter(searchInput.text?.toString().orEmpty())
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to load users", Toast.LENGTH_SHORT).show()
                allUsers = emptyList()
                usersNotInGroup = emptyList()
                existingMemberIds = group?.memberIDs?.filterNotNull()?.toSet().orEmpty()
                applyFilter(searchInput.text?.toString().orEmpty())
            }
    }

    private fun applyFilter(query: String) {
        val q = query.trim().lowercase()
        val source = if (q.isBlank()) usersNotInGroup else allUsers

        val filtered = if (q.isBlank()) {
            source
        } else {
            source.filter { user ->
                val name = user.username?.lowercase().orEmpty()
                val phone = user.phoneNumber?.lowercase().orEmpty()
                name.contains(q) || phone.contains(q)
            }
        }

        val disabledIds = if (q.isBlank()) {
            emptySet()
        } else {
            filtered.mapNotNull { it.userID }.filter { existingMemberIds.contains(it) }.toSet()
        }

        adapter.submitUsers(
            newUsers = filtered,
            selectedUserIds = selectedUserIds,
            disabledUserIds = disabledIds,
            disabledSubtitle = if (disabledIds.isNotEmpty()) getString(R.string.alreadyInGroup) else null
        )

        val isEmpty = filtered.isEmpty()
        emptyStateIcon.visibility = if (isEmpty) View.VISIBLE else View.GONE
        emptyStateText.visibility = if (isEmpty) View.VISIBLE else View.GONE
        recycler.visibility = if (isEmpty) View.GONE else View.VISIBLE
    }

    private fun updateDoneState() {
        val enabled = selectedUserIds.isNotEmpty()
        doneBtn.isEnabled = enabled
        doneBtn.alpha = if (enabled) 1f else 0.4f
    }

    private fun addSelectedMembers() {
        if (selectedUserIds.isEmpty()) return

        doneBtn.isEnabled = false
        doneBtn.alpha = 0.4f

        val existingMemberIDs = group?.memberIDs?.filterNotNull()?.toSet().orEmpty()
        val toAdd = selectedUserIds.filter { !existingMemberIDs.contains(it) }

        if (toAdd.isEmpty()) {
            Toast.makeText(this, "No new members selected", Toast.LENGTH_SHORT).show()
            updateDoneState()
            return
        }

        FirebaseGroups.getGroupReference(groupID!!)
            .update("memberIDs", FieldValue.arrayUnion(*toAdd.toTypedArray()))
            .addOnSuccessListener {
                // Send notification to each added member
                toAdd.forEach { memberID ->
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
                    "Added ${toAdd.size} member${if (toAdd.size > 1) "s" else ""}",
                    Toast.LENGTH_SHORT
                ).show()
                setResult(RESULT_OK)
                finish()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Failed to add members: ${e.message}", Toast.LENGTH_SHORT).show()
                updateDoneState()
            }
    }
}