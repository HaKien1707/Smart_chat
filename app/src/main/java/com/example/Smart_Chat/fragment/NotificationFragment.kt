package com.example.Smart_Chat.fragment

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.Smart_Chat.R
import com.example.Smart_Chat.adapters.social.NotificationAdapter
import com.example.Smart_Chat.models.notification.NotificationItemModel
import com.example.Smart_Chat.models.notification.NotificationType
import com.example.Smart_Chat.models.userModel
import com.example.Smart_Chat.utils.firebase.FirebaseAuthentication
import com.example.Smart_Chat.utils.firebase.FirebaseFriends
import com.example.Smart_Chat.utils.firebase.FirebaseGroups
import com.example.Smart_Chat.utils.firebase.FirebaseNotifications

class NotificationFragment : Fragment() {

    private lateinit var notificationRecycler: RecyclerView
    private lateinit var emptyState: LinearLayout
    private lateinit var adapter: NotificationAdapter
    private val notificationList = mutableListOf<NotificationItemModel>()

    private var isFirstLoad = true

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_notification, container, false)

        notificationRecycler = view.findViewById(R.id.notification_recycler)
        emptyState = view.findViewById(R.id.empty_state)

        setupRecyclerView()
        loadAllNotifications()
        isFirstLoad = true

        return view
    }

    override fun onResume() {
        super.onResume()
        if (!isFirstLoad) {
            loadAllNotifications()
        }
        isFirstLoad = false
    }

    private fun setupRecyclerView() {
        adapter = NotificationAdapter(requireContext(), notificationList) {
            if (notificationList.isEmpty()) {
                showEmptyState()
            }
        }
        notificationRecycler.layoutManager = LinearLayoutManager(context)
        notificationRecycler.adapter = adapter
    }

    private fun loadAllNotifications() {
        notificationList.clear()
        var completedTasks = 0
        val totalTasks = 3

        fun checkComplete() {
            completedTasks++
            if (completedTasks == totalTasks) {
                notificationList.sortByDescending {
                    when (it.type) {
                        NotificationType.FRIEND_REQUEST -> it.friendRequest?.timestamp
                        NotificationType.GROUP_JOIN_REQUEST -> it.groupJoinRequest?.timestamp
                        else -> it.notification?.timestamp
                    }
                }

                if (isAdded) { // Ensure fragment is attached
                    adapter.notifyDataSetChanged()
                    if (notificationList.isEmpty()) {
                        showEmptyState()
                    } else {
                        hideEmptyState()
                    }
                }
            }
        }

        FirebaseFriends.getPendingFriendRequests(onSuccess = { requests ->
            if (requests.isEmpty()) {
                checkComplete()
            } else {
                var loadedCount = 0
                requests.forEach { request ->
                    FirebaseAuthentication.allUsersCollection().document(request.senderID ?: "").get()
                        .addOnSuccessListener { userDoc ->
                            val user = userDoc.toObject(userModel::class.java)
                            if (user != null) {
                                notificationList.add(NotificationItemModel(type = NotificationType.FRIEND_REQUEST, user = user, friendRequest = request))
                            }
                        }.addOnCompleteListener { loadedCount++; if (loadedCount == requests.size) checkComplete() }
                }
            }
        }, onFailure = { checkComplete() })

        FirebaseGroups.getAllPendingGroupJoinRequestsForAdmin(onSuccess = { requests ->
            if (requests.isEmpty()) {
                checkComplete()
            } else {
                var loadedCount = 0
                requests.forEach { request ->
                    FirebaseAuthentication.allUsersCollection().document(request.senderID ?: "").get()
                        .addOnSuccessListener { userDoc ->
                            val user = userDoc.toObject(userModel::class.java)
                            if (user != null) {
                                notificationList.add(NotificationItemModel(type = NotificationType.GROUP_JOIN_REQUEST, user = user, groupJoinRequest = request))
                            }
                        }.addOnCompleteListener { loadedCount++; if (loadedCount == requests.size) checkComplete() }
                }
            }
        }, onFailure = { checkComplete() })

        FirebaseNotifications.getUserNotifications(FirebaseAuthentication.currentUserID() ?: "", onSuccess = { notifications ->
            if (notifications.isEmpty()) {
                checkComplete()
            } else {
                var loadedCount = 0
                notifications.forEach { notif ->
                    val type = try { NotificationType.valueOf(notif.type ?: "") } catch (e: Exception) { null }
                    if (type != null) {
                        if (!notif.senderID.isNullOrEmpty()) {
                            FirebaseAuthentication.allUsersCollection().document(notif.senderID!!).get()
                                .addOnSuccessListener { userDoc ->
                                    val user = userDoc.toObject(userModel::class.java)
                                    notificationList.add(NotificationItemModel(type = type, user = user, notification = notif))
                                }.addOnCompleteListener { loadedCount++; if (loadedCount == notifications.size) checkComplete() }
                        } else {
                            notificationList.add(NotificationItemModel(type = type, user = null, notification = notif))
                            loadedCount++
                            if (loadedCount == notifications.size) checkComplete()
                        }
                    } else {
                        loadedCount++
                        if (loadedCount == notifications.size) checkComplete()
                    }
                }
            }
        }, onFailure = { checkComplete() })
    }

    private fun showEmptyState() {
        emptyState.visibility = View.VISIBLE
        notificationRecycler.visibility = View.GONE
    }

    private fun hideEmptyState() {
        emptyState.visibility = View.GONE
        notificationRecycler.visibility = View.VISIBLE
    }
}
