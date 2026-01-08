package com.example.smart_chat.activities.others

import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.smart_chat.R
import com.example.smart_chat.adapters.social.BlockedUserAdapter
import com.example.smart_chat.utils.UI.LanguageManager
import com.example.smart_chat.utils.UI.ThemeManager
import com.example.smart_chat.utils.firebase.FirebaseAuthentication

class BlockedUsersActivity : AppCompatActivity() {

    private lateinit var backBtn: ImageButton
    private lateinit var recyclerView: RecyclerView
    private lateinit var emptyState: TextView
    private lateinit var adapter: BlockedUserAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        ThemeManager.applySavedTheme(this)
        LanguageManager.applySavedLanguage(this)

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_blocked_users)

        backBtn = findViewById(R.id.back_btn)
        recyclerView = findViewById(R.id.blocked_users_recycler)
        emptyState = findViewById(R.id.empty_state)

        backBtn.setOnClickListener { finish() }

        setupRecyclerView()
        loadBlockedUsers()
    }

    private fun setupRecyclerView() {
        adapter = BlockedUserAdapter(this, mutableListOf()) {
            // Refresh list after unblocking
            loadBlockedUsers()
        }
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter
    }

    private fun loadBlockedUsers() {
        FirebaseAuthentication.getBlockedUsers {
            if (it.isEmpty()) {
                emptyState.visibility = View.VISIBLE
                recyclerView.visibility = View.GONE
            } else {
                emptyState.visibility = View.GONE
                recyclerView.visibility = View.VISIBLE
                adapter.updateData(it)
            }
        }
    }
}