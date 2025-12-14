package com.example.Smart_Chat.activities.others

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
import com.example.Smart_Chat.adapters.NotificationAdapter
import com.example.Smart_Chat.models.NotificationItemModel
import com.example.Smart_Chat.models.NotificationType
import com.example.Smart_Chat.models.userModel
import com.example.Smart_Chat.utils.FireBase_utils
import com.example.Smart_Chat.utils.LanguageManager
import com.example.Smart_Chat.utils.ThemeManager

class NotificationActivity : AppCompatActivity() {

    private lateinit var backBtn: ImageButton
    private lateinit var title: TextView
    private lateinit var requestRecycler: RecyclerView
    private lateinit var emptyState: LinearLayout
    private lateinit var adapter: NotificationAdapter
    private val notificationList = mutableListOf<NotificationItemModel>()

    override fun onResume() {
        super.onResume()
        // Only reload if we're resuming from another activity
        if (!isFirstLoad) {
            loadAllNotifications()
        }
        isFirstLoad = false
    }

    private var isFirstLoad = true

    override fun onCreate(savedInstanceState: Bundle?) {
        ThemeManager.applySavedTheme(this)
        LanguageManager.applySavedLanguage(this)

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_notification)

        backBtn = findViewById(R.id.back_btn)
        title = findViewById(R.id.title)
        requestRecycler = findViewById(R.id.request_recycler)
        emptyState = findViewById(R.id.empty_state)

        title.text = getString(R.string.notification)

        backBtn.setOnClickListener {
            finish()
        }

        setupRecyclerView()
        loadAllNotifications()
        isFirstLoad = true
    }

    private fun setupRecyclerView() {
        adapter = NotificationAdapter(this, notificationList)
        requestRecycler.layoutManager = LinearLayoutManager(this)
        requestRecycler.adapter = adapter
    }

    private fun loadAllNotifications() {
        notificationList.clear()
        var friendRequestsLoaded = false
        var groupRequestsLoaded = false

        // Load friend requests
        FireBase_utils.getPendingFriendRequests(
            onSuccess = { requests ->
                var loadedCount = 0

                if (requests.isEmpty()) {
                    friendRequestsLoaded = true
                    checkIfAllLoaded(friendRequestsLoaded, groupRequestsLoaded)
                    return@getPendingFriendRequests
                }

                requests.forEach { request ->
                    FireBase_utils.allUsersCollection()
                        .document(request.senderID ?: "")
                        .get()
                        .addOnSuccessListener { userDoc ->
                            val user = userDoc.toObject(userModel::class.java)
                            if (user != null) {
                                notificationList.add(
                                    NotificationItemModel(
                                        type = NotificationType.FRIEND_REQUEST,
                                        user = user,
                                        friendRequest = request
                                    )
                                )
                            }

                            loadedCount++
                            if (loadedCount == requests.size) {
                                friendRequestsLoaded = true
                                checkIfAllLoaded(friendRequestsLoaded, groupRequestsLoaded)
                            }
                        }
                }
            },
            onFailure = { e ->
                Log.e("NotificationActivity", "Failed to load friend requests", e)
                friendRequestsLoaded = true
                checkIfAllLoaded(friendRequestsLoaded, groupRequestsLoaded)
            }
        )

        // Load group join requests (if user is admin of any groups)
        FireBase_utils.getAllPendingGroupJoinRequestsForAdmin(
            onSuccess = { requests ->
                var loadedCount = 0

                if (requests.isEmpty()) {
                    groupRequestsLoaded = true
                    checkIfAllLoaded(friendRequestsLoaded, groupRequestsLoaded)
                    return@getAllPendingGroupJoinRequestsForAdmin
                }

                requests.forEach { request ->
                    FireBase_utils.allUsersCollection()
                        .document(request.senderID ?: "")
                        .get()
                        .addOnSuccessListener { userDoc ->
                            val user = userDoc.toObject(userModel::class.java)
                            if (user != null) {
                                notificationList.add(
                                    NotificationItemModel(
                                        type = NotificationType.GROUP_JOIN_REQUEST,
                                        user = user,
                                        groupJoinRequest = request
                                    )
                                )
                            }

                            loadedCount++
                            if (loadedCount == requests.size) {
                                groupRequestsLoaded = true
                                checkIfAllLoaded(friendRequestsLoaded, groupRequestsLoaded)
                            }
                        }
                }
            },
            onFailure = { e ->
                Log.e("NotificationActivity", "Failed to load group requests", e)
                groupRequestsLoaded = true
                checkIfAllLoaded(friendRequestsLoaded, groupRequestsLoaded)
            }
        )
    }

    private fun checkIfAllLoaded(friendsLoaded: Boolean, groupsLoaded: Boolean) {
        if (friendsLoaded && groupsLoaded) {
            // Sort by timestamp (newest first)
            notificationList.sortByDescending {
                when (it.type) {
                    NotificationType.FRIEND_REQUEST -> it.friendRequest?.timestamp
                    NotificationType.GROUP_JOIN_REQUEST -> it.groupJoinRequest?.timestamp
                }
            }

            adapter.notifyDataSetChanged()

            if (notificationList.isEmpty()) {
                showEmptyState()
            } else {
                hideEmptyState()
            }
        }
    }

    private fun showEmptyState() {
        emptyState.visibility = View.VISIBLE
        requestRecycler.visibility = View.GONE
    }

    private fun hideEmptyState() {
        emptyState.visibility = View.GONE
        requestRecycler.visibility = View.VISIBLE
    }
}