package com.example.Smart_Chat.activities

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.Smart_Chat.R
import com.example.Smart_Chat.adapters.FriendRequestAdapter
import com.example.Smart_Chat.models.FriendRequestModel
import com.example.Smart_Chat.models.userModel
import com.example.Smart_Chat.utils.FireBase_utils
import com.example.Smart_Chat.utils.LanguageManager
import com.example.Smart_Chat.utils.ThemeManager

class FriendRequestActivity : AppCompatActivity() {

    private lateinit var backBtn: ImageButton
    private lateinit var title: TextView
    private lateinit var requestRecycler: RecyclerView
    private lateinit var emptyState: LinearLayout
    private lateinit var adapter: FriendRequestAdapter
    private val requestList = mutableListOf<Pair<FriendRequestModel, userModel>>()

    override fun onCreate(savedInstanceState: Bundle?) {
        // Apply theme and language
        ThemeManager.applySavedTheme(this)
        LanguageManager.applySavedLanguage(this)

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_friend_request)

        backBtn = findViewById(R.id.back_btn)
        title = findViewById(R.id.title)
        requestRecycler = findViewById(R.id.request_recycler)
        emptyState = findViewById(R.id.empty_state)

        title.text = getString(R.string.friendRequest)

        backBtn.setOnClickListener {
            finish()
        }

        setupRecyclerView()
        loadFriendRequests()
    }

    private fun setupRecyclerView() {
        adapter = FriendRequestAdapter(this, requestList)
        requestRecycler.layoutManager = LinearLayoutManager(this)
        requestRecycler.adapter = adapter
    }

    private fun loadFriendRequests() {
        FireBase_utils.getPendingFriendRequests(
            onSuccess = { requests ->
                if (requests.isEmpty()) {
                    showEmptyState()
                    return@getPendingFriendRequests
                }

                requestList.clear()

                // Load user details for each request
                requests.forEach { request ->
                    FireBase_utils.allUsersCollection()
                        .document(request.senderID ?: "")
                        .get()
                        .addOnSuccessListener { userDoc ->
                            val user = userDoc.toObject(userModel::class.java)
                            if (user != null) {
                                requestList.add(Pair(request, user))
                                adapter.notifyDataSetChanged()
                                hideEmptyState()
                            }
                        }
                }
            },
            onFailure = { e ->
                Log.e("FriendRequestActivity", "Failed to load requests", e)
                showEmptyState()
            }
        )
    }

    private fun showEmptyState() {
        emptyState.visibility = View.VISIBLE
        requestRecycler.visibility = View.GONE
    }

    private fun hideEmptyState() {
        emptyState.visibility = View.GONE
        requestRecycler.visibility = View.VISIBLE
    }

    override fun onResume() {
        super.onResume()
        loadFriendRequests()
    }
}