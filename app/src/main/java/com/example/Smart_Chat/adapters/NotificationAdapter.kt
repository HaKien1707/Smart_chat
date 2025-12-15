package com.example.Smart_Chat.adapters

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.example.Smart_Chat.R
import com.example.Smart_Chat.activities.community.CommunityChatActivity
import com.example.Smart_Chat.activities.group_chat.GroupChatActivity
import com.example.Smart_Chat.models.NotificationItemModel
import com.example.Smart_Chat.models.NotificationType
import com.example.Smart_Chat.utils.FireBase_utils
import com.example.Smart_Chat.utils.androidUtils

class NotificationAdapter(
    private val context: Context,
    private val notifications: MutableList<NotificationItemModel>,
    private val onNotificationRemoved: () -> Unit = {}
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val VIEW_TYPE_FRIEND_REQUEST = 0
        private const val VIEW_TYPE_GROUP_JOIN_REQUEST = 1
        private const val VIEW_TYPE_INFO = 2  // For informational notifications
    }

    override fun getItemViewType(position: Int): Int {
        return when (notifications[position].type) {
            NotificationType.FRIEND_REQUEST -> VIEW_TYPE_FRIEND_REQUEST
            NotificationType.GROUP_JOIN_REQUEST -> VIEW_TYPE_GROUP_JOIN_REQUEST
            else -> VIEW_TYPE_INFO
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            VIEW_TYPE_FRIEND_REQUEST -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_friend_request, parent, false)
                FriendRequestViewHolder(view)
            }
            VIEW_TYPE_GROUP_JOIN_REQUEST -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_group_join_request, parent, false)
                GroupJoinRequestViewHolder(view)
            }
            VIEW_TYPE_INFO -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_notification_info, parent, false)
                InfoNotificationViewHolder(view)
            }
            else -> throw IllegalArgumentException("Invalid view type")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val notification = notifications[position]

        when (holder) {
            is FriendRequestViewHolder -> bindFriendRequest(holder, notification, position)
            is GroupJoinRequestViewHolder -> bindGroupJoinRequest(holder, notification, position)
            is InfoNotificationViewHolder -> bindInfoNotification(holder, notification, position)
        }
    }

    private fun bindFriendRequest(
        holder: FriendRequestViewHolder,
        notification: NotificationItemModel,
        position: Int
    ) {
        val user = notification.user
        val request = notification.friendRequest!!

        holder.userName.text = user?.username ?: "Unknown"
        holder.requestDescription.text = "sent you a friend request"

        // Load profile image
        if (!user?.profileImage.isNullOrBlank()) {
            androidUtils.setProfileImageFromBase64(context, user?.profileImage, holder.profileImage)
        } else {
            holder.profileImage.setImageResource(R.drawable.ic_profile)
        }

        // Accept button
        holder.acceptBtn.setOnClickListener {
            FireBase_utils.acceptFriendRequest(
                request.senderID ?: "",
                onSuccess = {
                    Toast.makeText(context, "Friend request accepted", Toast.LENGTH_SHORT).show()
                    notifications.removeAt(position)
                    notifyItemRemoved(position)
                    notifyItemRangeChanged(position, notifications.size)
                    onNotificationRemoved()
                },
                onFailure = { e ->
                    Toast.makeText(context, "Failed: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            )
        }

        // Reject button
        holder.rejectBtn.setOnClickListener {
            FireBase_utils.rejectFriendRequest(
                request.senderID ?: "",
                onSuccess = {
                    Toast.makeText(context, "Friend request rejected", Toast.LENGTH_SHORT).show()
                    notifications.removeAt(position)
                    notifyItemRemoved(position)
                    notifyItemRangeChanged(position, notifications.size)
                    onNotificationRemoved()
                },
                onFailure = { e ->
                    Toast.makeText(context, "Failed: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            )
        }
    }

    private fun bindGroupJoinRequest(
        holder: GroupJoinRequestViewHolder,
        notification: NotificationItemModel,
        position: Int
    ) {
        val user = notification.user
        val request = notification.groupJoinRequest!!

        holder.userName.text = user?.username ?: "Unknown"
        holder.requestDescription.text = "sent a request to join"
        holder.groupName.text = request.groupName ?: "Unknown Group"

        // Load profile image
        if (!user?.profileImage.isNullOrBlank()) {
            androidUtils.setProfileImageFromBase64(context, user?.profileImage, holder.profileImage)
        } else {
            holder.profileImage.setImageResource(R.drawable.ic_profile)
        }

        // Accept button
        holder.acceptBtn.setOnClickListener {
            FireBase_utils.acceptGroupJoinRequest(
                request.requestID ?: "",
                request.groupID ?: "",
                request.senderID ?: "",
                onSuccess = {
                    Toast.makeText(context, "Request accepted", Toast.LENGTH_SHORT).show()
                    notifications.removeAt(position)
                    notifyItemRemoved(position)
                    notifyItemRangeChanged(position, notifications.size)
                    onNotificationRemoved()
                },
                onFailure = { e ->
                    Toast.makeText(context, "Failed: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            )
        }

        // Reject button
        holder.rejectBtn.setOnClickListener {
            FireBase_utils.rejectGroupJoinRequest(
                request.requestID ?: "",
                onSuccess = {
                    Toast.makeText(context, "Request rejected", Toast.LENGTH_SHORT).show()
                    notifications.removeAt(position)
                    notifyItemRemoved(position)
                    notifyItemRangeChanged(position, notifications.size)
                    onNotificationRemoved()
                },
                onFailure = { e ->
                    Toast.makeText(context, "Failed: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            )
        }
    }

    private fun bindInfoNotification(
        holder: InfoNotificationViewHolder,
        notification: NotificationItemModel,
        position: Int
    ) {
        val notif = notification.notification!!
        val user = notification.user

        // Set icon based on notification type
        when (notification.type) {
            NotificationType.FRIEND_REQUEST_ACCEPTED -> {
                holder.notificationIcon.setImageResource(R.drawable.ic_person_add)
                holder.notificationIcon.setColorFilter(context.getColor(R.color.green))
            }
            NotificationType.GROUP_JOIN_REQUEST_ACCEPTED -> {
                holder.notificationIcon.setImageResource(R.drawable.ic_group)
                holder.notificationIcon.setColorFilter(context.getColor(R.color.green))
            }
            NotificationType.ADDED_TO_GROUP -> {
                holder.notificationIcon.setImageResource(R.drawable.ic_group)
                holder.notificationIcon.setColorFilter(context.getColor(R.color.blue))
            }
            NotificationType.REMOVED_FROM_GROUP -> {
                holder.notificationIcon.setImageResource(R.drawable.ic_group)
                holder.notificationIcon.setColorFilter(context.getColor(R.color.red))
            }
            NotificationType.BLOCKED_FROM_GROUP -> {
                holder.notificationIcon.setImageResource(R.drawable.ic_group)
                holder.notificationIcon.setColorFilter(context.getColor(R.color.red))
            }
            NotificationType.UNBLOCKED_FROM_GROUP -> {
                holder.notificationIcon.setImageResource(R.drawable.ic_group)
                holder.notificationIcon.setColorFilter(context.getColor(R.color.blue))
            }
            NotificationType.BANNED_FROM_COMMUNITY -> {
                holder.notificationIcon.setImageResource(R.drawable.ic_community)
                holder.notificationIcon.setColorFilter(context.getColor(R.color.red))
            }
            NotificationType.UNBANNED_FROM_COMMUNITY -> {
                holder.notificationIcon.setImageResource(R.drawable.ic_community)
                holder.notificationIcon.setColorFilter(context.getColor(R.color.green))
            }
            NotificationType.BLOCKED_BY_USER -> {
                holder.notificationIcon.setImageResource(R.drawable.ic_block)
                holder.notificationIcon.setColorFilter(context.getColor(R.color.red))
            }
            else -> {
                holder.notificationIcon.setImageResource(R.drawable.ic_notifications)
                holder.notificationIcon.clearColorFilter()
            }
        }

        // Set message
        holder.notificationMessage.text = notif.message

        // Set timestamp
        holder.notificationTime.text = formatTimestamp(notif.timestamp?.toDate())

        // Dismiss button
        holder.dismissBtn.setOnClickListener {
            FireBase_utils.deleteNotification(
                notif.notificationID ?: "",
                onSuccess = {
                    notifications.removeAt(position)
                    notifyItemRemoved(position)
                    notifyItemRangeChanged(position, notifications.size)
                    onNotificationRemoved()
                },
                onFailure = { e ->
                    Toast.makeText(context, "Failed to dismiss", Toast.LENGTH_SHORT).show()
                }
            )
        }

        // Make clickable for certain types
        holder.itemView.setOnClickListener {
            when (notification.type) {
                NotificationType.GROUP_JOIN_REQUEST_ACCEPTED,
                NotificationType.ADDED_TO_GROUP -> {
                    // Open group chat
                    notif.groupID?.let { groupID ->
                        val intent = Intent(context, GroupChatActivity::class.java)
                        intent.putExtra("groupID", groupID)
                        intent.putExtra("groupName", notif.groupName)
                        context.startActivity(intent)

                        // Mark as read
                        FireBase_utils.markNotificationAsRead(notif.notificationID ?: "")
                    }
                }
                NotificationType.UNBANNED_FROM_COMMUNITY -> {
                    // Open community chat
                    notif.communityID?.let { communityID ->
                        val intent = Intent(context, CommunityChatActivity::class.java)
                        intent.putExtra("communityID", communityID)
                        intent.putExtra("communityName", notif.communityName)
                        context.startActivity(intent)

                        // Mark as read
                        FireBase_utils.markNotificationAsRead(notif.notificationID ?: "")
                    }
                }
                else -> {
                    // Just mark as read
                    FireBase_utils.markNotificationAsRead(notif.notificationID ?: "")
                }
            }
        }
    }

    private fun formatTimestamp(date: java.util.Date?): String {
        if (date == null) return ""

        val now = System.currentTimeMillis()
        val diff = now - date.time

        return when {
            diff < 60_000 -> "Just now"
            diff < 3600_000 -> "${diff / 60_000}m ago"
            diff < 86400_000 -> "${diff / 3600_000}h ago"
            diff < 604800_000 -> "${diff / 86400_000}d ago"
            else -> java.text.SimpleDateFormat("MMM d", java.util.Locale.getDefault()).format(date)
        }
    }

    override fun getItemCount(): Int = notifications.size

    // ViewHolder for Friend Requests
    class FriendRequestViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val profileImage: ImageView = itemView.findViewById(R.id.profile_image)
        val userName: TextView = itemView.findViewById(R.id.user_name)
        val requestDescription: TextView = itemView.findViewById(R.id.request_description)
        val acceptBtn: ImageButton = itemView.findViewById(R.id.accept_btn)
        val rejectBtn: ImageButton = itemView.findViewById(R.id.reject_btn)
    }

    // ViewHolder for Group Join Requests
    class GroupJoinRequestViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val profileImage: ImageView = itemView.findViewById(R.id.profile_image)
        val userName: TextView = itemView.findViewById(R.id.user_name)
        val requestDescription: TextView = itemView.findViewById(R.id.request_description)
        val groupName: TextView = itemView.findViewById(R.id.group_name)
        val acceptBtn: ImageButton = itemView.findViewById(R.id.accept_btn)
        val rejectBtn: ImageButton = itemView.findViewById(R.id.reject_btn)
    }

    // ViewHolder for Info Notifications
    class InfoNotificationViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val notificationIcon: ImageView = itemView.findViewById(R.id.notification_icon)
        val notificationMessage: TextView = itemView.findViewById(R.id.notification_message)
        val notificationTime: TextView = itemView.findViewById(R.id.notification_time)
        val dismissBtn: ImageButton = itemView.findViewById(R.id.dismiss_btn)
    }
}