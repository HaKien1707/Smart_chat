package com.example.smart_chat.activities.group_chat

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.smart_chat.R
import com.example.smart_chat.adapters.group.SelectableUserAdapter
import com.example.smart_chat.models.userModel
import com.example.smart_chat.utils.UI.LanguageManager
import com.example.smart_chat.utils.UI.ThemeManager
import com.example.smart_chat.utils.firebase.FirebaseAuthentication

class CreateGroupMembersActivity : AppCompatActivity() {

    private lateinit var backBtn: ImageButton
    private lateinit var searchInput: EditText
    private lateinit var recycler: RecyclerView
    private lateinit var emptyStateIcon: ImageView
    private lateinit var emptyStateText: TextView
    private lateinit var nextBtn: ImageButton

    private var allUsers: List<userModel> = emptyList()
    private val selectedUserIds = linkedSetOf<String>()

    private lateinit var adapter: SelectableUserAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        ThemeManager.applySavedTheme(this)
        LanguageManager.applySavedLanguage(this)

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create_group_members)

        backBtn = findViewById(R.id.back_btn)
        searchInput = findViewById(R.id.search_input)
        recycler = findViewById(R.id.user_recycler)
        emptyStateIcon = findViewById(R.id.empty_state_icon)
        emptyStateText = findViewById(R.id.empty_state_text)
        nextBtn = findViewById(R.id.next_btn)

        backBtn.setOnClickListener { finish() }
        nextBtn.setOnClickListener { goNext() }

        adapter = SelectableUserAdapter(
            context = this,
            selectable = true,
            onSelectionChanged = { userId, isSelected ->
                if (isSelected) selectedUserIds.add(userId) else selectedUserIds.remove(userId)
                updateNextState()
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

        loadUsers()
        updateNextState()
    }

    private fun loadUsers() {
        val currentUserId = FirebaseAuthentication.currentUserID()

        FirebaseAuthentication.allUsersCollection()
            .get()
            .addOnSuccessListener { docs ->
                val users = mutableListOf<userModel>()
                for (doc in docs) {
                    val user = doc.toObject(userModel::class.java)
                    val userId = user.userID
                    if (!userId.isNullOrBlank() && userId != currentUserId) {
                        users.add(user)
                    }
                }
                allUsers = users
                applyFilter(searchInput.text?.toString().orEmpty())
            }
            .addOnFailureListener {
                allUsers = emptyList()
                applyFilter(searchInput.text?.toString().orEmpty())
            }
    }

    private fun applyFilter(query: String) {
        val q = query.trim().lowercase()
        val filtered = if (q.isBlank()) {
            allUsers
        } else {
            allUsers.filter { user ->
                val name = user.username?.lowercase().orEmpty()
                val phone = user.phoneNumber?.lowercase().orEmpty()
                name.contains(q) || phone.contains(q)
            }
        }

        adapter.submitUsers(filtered, selectedUserIds)

        val isEmpty = filtered.isEmpty()
        emptyStateIcon.visibility = if (isEmpty) android.view.View.VISIBLE else android.view.View.GONE
        emptyStateText.visibility = if (isEmpty) android.view.View.VISIBLE else android.view.View.GONE
        recycler.visibility = if (isEmpty) android.view.View.GONE else android.view.View.VISIBLE
    }

    private fun updateNextState() {
        val enabled = selectedUserIds.isNotEmpty()
        nextBtn.isEnabled = enabled
        nextBtn.alpha = if (enabled) 1f else 0.4f
    }

    private fun goNext() {
        if (selectedUserIds.isEmpty()) return
        val intent = Intent(this, CreateGroupActivity::class.java)
        intent.putStringArrayListExtra(
            CreateGroupActivity.EXTRA_SELECTED_USER_IDS,
            ArrayList(selectedUserIds)
        )
        startActivity(intent)
        finish()
    }
}
