package com.example.Smart_Chat.activities.others

import android.os.Bundle
import android.widget.ImageButton
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.Smart_Chat.R
import com.example.Smart_Chat.adapters.RecentChatRecyclerAdapter
import com.example.Smart_Chat.models.chatRoomModel
import com.example.Smart_Chat.utils.FireBase_utils
import com.example.Smart_Chat.utils.LanguageManager
import com.example.Smart_Chat.utils.ThemeManager
import com.firebase.ui.firestore.FirestoreRecyclerOptions

class DeletedChatsActivity : AppCompatActivity() {

    private lateinit var backBtn: ImageButton
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: RecentChatRecyclerAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        ThemeManager.applySavedTheme(this)
        LanguageManager.applySavedLanguage(this)

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_deleted_chats)

        backBtn = findViewById(R.id.back_btn)
        recyclerView = findViewById(R.id.deleted_chats_recycler)

        backBtn.setOnClickListener {
            finish()
        }

        setupRecycler()
    }

    private fun setupRecycler() {
        val query = FireBase_utils.getDeletedChatRoomsQuery()

        val options = FirestoreRecyclerOptions.Builder<chatRoomModel>()
            .setQuery(query, chatRoomModel::class.java)
            .setLifecycleOwner(this)
            .build()

        adapter = RecentChatRecyclerAdapter(options, this, isDeletedView = true)
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter
    }
}