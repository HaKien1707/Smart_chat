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
import com.example.Smart_Chat.adapters.social.NotificationAdapter
import com.example.Smart_Chat.models.notification.NotificationItemModel
import com.example.Smart_Chat.models.notification.NotificationType
import com.example.Smart_Chat.models.userModel
import com.example.Smart_Chat.utils.UI.LanguageManager
import com.example.Smart_Chat.utils.UI.ThemeManager
import com.example.Smart_Chat.utils.firebase.*

class NotificationActivity : AppCompatActivity() {

    private lateinit var backBtn: ImageButton
    private lateinit var title: TextView
    private lateinit var requestRecycler: RecyclerView
    private lateinit var emptyState: LinearLayout
    private lateinit var adapter: NotificationAdapter
    private val notificationList = mutableListOf<NotificationItemModel>()

    private var isFirstLoad = true

    override fun onResume() {
        super.onResume()
        if (!isFirstLoad) {
            loadAllNotifications()
        }
        isFirstLoad = false
    }

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
        adapter = NotificationAdapter(this, notificationList) {
            // Callback when notification is removed
            if (notificationList.isEmpty()) {
                showEmptyState()
            }
        }
        requestRecycler.layoutManager = LinearLayoutManager(this)
        requestRecycler.adapter = adapter
    }

    private fun loadAllNotifications() {
        notificationList.clear()
        var completedTasks = 0
        val totalTasks = 3

        fun checkComplete() {
            completedTasks++
            if (completedTasks == totalTasks) {
                // Sort by timestamp (newest first)
                notificationList.sortByDescending {
                    when (it.type) {
                        NotificationType.FRIEND_REQUEST -> it.friendRequest?.timestamp
                        NotificationType.GROUP_JOIN_REQUEST -> it.groupJoinRequest?.timestamp
                        else -> it.notification?.timestamp
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

        // Load friend requests
        FirebaseFriends.getPendingFriendRequests(
            onSuccess = { requests ->
                var loadedCount = 0

                if (requests.isEmpty()) {
                    checkComplete()
                    return@getPendingFriendRequests
                }

                requests.forEach { request ->
                    FirebaseAuthentication.allUsersCollection()
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
                                checkComplete()
                            }
                        }
                        .addOnFailureListener {
                            loadedCount++
                            if (loadedCount == requests.size) {
                                checkComplete()
                            }
                        }
                }
            },
            onFailure = { e ->
                Log.e("NotificationActivity", "Failed to load friend requests", e)
                checkComplete()
            }
        )

        // Load group join requests
        FirebaseGroups.getAllPendingGroupJoinRequestsForAdmin(
            onSuccess = { requests ->
                var loadedCount = 0

                if (requests.isEmpty()) {
                    checkComplete()
                    return@getAllPendingGroupJoinRequestsForAdmin
                }

                requests.forEach { request ->
                    FirebaseAuthentication.allUsersCollection()
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
                                checkComplete()
                            }
                        }
                        .addOnFailureListener {
                            loadedCount++
                            if (loadedCount == requests.size) {
                                checkComplete()
                            }
                        }
                }
            },
            onFailure = { e ->
                Log.e("NotificationActivity", "Failed to load group requests", e)
                checkComplete()
            }
        )

        // Load other notifications (SAFE - handles if collection doesn't exist)
        FirebaseNotifications.getUserNotifications(
            FirebaseAuthentication.currentUserID() ?: "",
            onSuccess = { notifications ->
                var loadedCount = 0

                if (notifications.isEmpty()) {
                    checkComplete()
                    return@getUserNotifications
                }

                notifications.forEach { notif ->
                    // Map notification type string to enum
                    val type = try {
                        NotificationType.valueOf(notif.type ?: "")
                    } catch (e: Exception) {
                        Log.e("NotificationActivity", "Unknown notification type: ${notif.type}")
                        null
                    }

                    if (type != null) {
                        // Load sender user info if available
                        if (!notif.senderID.isNullOrEmpty()) {
                            FirebaseAuthentication.allUsersCollection()
                                .document(notif.senderID ?: "")
                                .get()
                                .addOnSuccessListener { userDoc ->
                                    val user = userDoc.toObject(userModel::class.java)
                                    notificationList.add(
                                        NotificationItemModel(
                                            type = type,
                                            user = user,
                                            notification = notif
                                        )
                                    )

                                    loadedCount++
                                    if (loadedCount == notifications.size) {
                                        checkComplete()
                                    }
                                }
                                .addOnFailureListener {
                                    loadedCount++
                                    if (loadedCount == notifications.size) {
                                        checkComplete()
                                    }
                                }
                        } else {
                            // No sender, just add notification
                            notificationList.add(
                                NotificationItemModel(
                                    type = type,
                                    user = null,
                                    notification = notif
                                )
                            )

                            loadedCount++
                            if (loadedCount == notifications.size) {
                                checkComplete()
                            }
                        }
                    } else {
                        loadedCount++
                        if (loadedCount == notifications.size) {
                            checkComplete()
                        }
                    }
                }
            },
            onFailure = { e ->
                // Don't show error - notifications collection might not exist yet
                Log.w("NotificationActivity", "Info notifications not available: ${e.message}")
                checkComplete()
            }
        )
    }

    private fun mapStringToNotificationType(type: String): NotificationType {
        return when (type) {
            "FRIEND_REQUEST_ACCEPTED" -> NotificationType.FRIEND_REQUEST_ACCEPTED
            "GROUP_JOIN_REQUEST_ACCEPTED" -> NotificationType.GROUP_JOIN_REQUEST_ACCEPTED
            "ADDED_TO_GROUP" -> NotificationType.ADDED_TO_GROUP
            "REMOVED_FROM_GROUP" -> NotificationType.REMOVED_FROM_GROUP
            "BANNED_FROM_COMMUNITY" -> NotificationType.BANNED_FROM_COMMUNITY
            "UNBANNED_FROM_COMMUNITY" -> NotificationType.UNBANNED_FROM_COMMUNITY
            "BLOCKED_BY_USER" -> NotificationType.BLOCKED_BY_USER
            else -> NotificationType.FRIEND_REQUEST  // Default
        }
    }

    private fun checkIfAllLoaded(friendsLoaded: Boolean, groupsLoaded: Boolean, infoLoaded: Boolean) {
        if (friendsLoaded && groupsLoaded && infoLoaded) {
            // Sort by timestamp (newest first)
            notificationList.sortByDescending {
                when (it.type) {
                    NotificationType.FRIEND_REQUEST -> it.friendRequest?.timestamp
                    NotificationType.GROUP_JOIN_REQUEST -> it.groupJoinRequest?.timestamp
                    else -> it.notification?.timestamp
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