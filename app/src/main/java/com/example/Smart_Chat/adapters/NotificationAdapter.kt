package com.example.Smart_Chat.adapters

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.example.Smart_Chat.R
import com.example.Smart_Chat.models.NotificationItemModel
import com.example.Smart_Chat.models.NotificationType
import com.example.Smart_Chat.utils.FireBase_utils
import com.example.Smart_Chat.utils.androidUtils

class NotificationAdapter(
    private val context: Context,
    private val notifications: MutableList<NotificationItemModel>
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val VIEW_TYPE_FRIEND_REQUEST = 0
        private const val VIEW_TYPE_GROUP_JOIN_REQUEST = 1
    }

    override fun getItemViewType(position: Int): Int {
        return when (notifications[position].type) {
            NotificationType.FRIEND_REQUEST -> VIEW_TYPE_FRIEND_REQUEST
            NotificationType.GROUP_JOIN_REQUEST -> VIEW_TYPE_GROUP_JOIN_REQUEST
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            VIEW_TYPE_FRIEND_REQUEST -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.friend_request_item, parent, false)
                FriendRequestViewHolder(view)
            }
            VIEW_TYPE_GROUP_JOIN_REQUEST -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_group_join_request, parent, false)
                GroupJoinRequestViewHolder(view)
            }
            else -> throw IllegalArgumentException("Invalid view type")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val notification = notifications[position]

        when (holder) {
            is FriendRequestViewHolder -> bindFriendRequest(holder, notification, position)
            is GroupJoinRequestViewHolder -> bindGroupJoinRequest(holder, notification, position)
        }
    }

    private fun bindFriendRequest(
        holder: FriendRequestViewHolder,
        notification: NotificationItemModel,
        position: Int
    ) {
        val user = notification.user
        val request = notification.friendRequest!!

        holder.userName.text = user.username ?: "Unknown"
        holder.requestDescription.text = "sent you a friend request"

        // Load profile image
        if (!user.profileImage.isNullOrBlank()) {
            androidUtils.setProfileImageFromBase64(context, user.profileImage, holder.profileImage)
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

        holder.userName.text = user.username ?: "Unknown"
        holder.requestDescription.text = "sent a request to join"
        holder.groupName.text = request.groupName ?: "Unknown Group"

        // Load profile image
        if (!user.profileImage.isNullOrBlank()) {
            androidUtils.setProfileImageFromBase64(context, user.profileImage, holder.profileImage)
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
                },
                onFailure = { e ->
                    Toast.makeText(context, "Failed: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            )
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
}