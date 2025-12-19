package com.example.Smart_Chat.adapters.user_chat

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.RecyclerView
import com.example.Smart_Chat.R
import com.example.Smart_Chat.activities.user_chat.ChatActivity
import com.example.Smart_Chat.activities.video_call.OutgoingCallActivity
import com.example.Smart_Chat.models.userModel
import com.example.Smart_Chat.utils.FireBase_utils
import com.example.Smart_Chat.utils.androidUtils

class FriendsListAdapter(
    private val context: Context,
    private val friendsList: MutableList<userModel>
) : RecyclerView.Adapter<FriendsListAdapter.FriendViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FriendViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_friend, parent, false)
        return FriendViewHolder(view)
    }

    override fun onBindViewHolder(holder: FriendViewHolder, position: Int) {
        val friend = friendsList[position]

        holder.userName.text = friend.username ?: "Unknown"
        holder.userPhone.text = friend.phoneNumber ?: ""

        if (!friend.profileImage.isNullOrBlank()) {
            androidUtils.setProfileImageFromBase64(context, friend.profileImage, holder.profileImage)
        } else {
            holder.profileImage.setImageResource(R.drawable.ic_profile)
        }

        // Click card to open chat
        holder.itemView.setOnClickListener {
            val intent = Intent(context, ChatActivity::class.java)
            androidUtils.passUserModelAsIntent(intent, friend)
            context.startActivity(intent)
        }

        // Video call button
        holder.videoCallIcon.setOnClickListener {
            initiateVideoCall(friend)
        }

        // Block button
        holder.blockIcon.setOnClickListener {
            showBlockDialog(friend, position)
        }

        // Remove button
        holder.removeIcon.setOnClickListener {
            showRemoveFriendDialog(friend, position)
        }
    }

    private fun initiateVideoCall(friend: userModel) {
        val intent = Intent(context, OutgoingCallActivity::class.java)
        intent.putExtra("receiverId", friend.userID)
        intent.putExtra("receiverName", friend.username)
        intent.putExtra("receiverImage", friend.profileImage)
        intent.putExtra("callType", "video")
        context.startActivity(intent)
    }

    private fun showBlockDialog(friend: userModel, position: Int) {
        AlertDialog.Builder(context)
            .setTitle("Block User")
            .setMessage("Are you sure you want to block ${friend.username}? You will be unfriended and they won't be able to send you friend requests.")
            .setPositiveButton("Block") { _, _ ->
                blockUser(friend, position)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun blockUser(friend: userModel, position: Int) {
        FireBase_utils.blockUser(
            friend.userID ?: "",
            onSuccess = {
                Toast.makeText(context, "${friend.username} has been blocked", Toast.LENGTH_SHORT).show()
                friendsList.removeAt(position)
                notifyItemRemoved(position)
                notifyItemRangeChanged(position, friendsList.size)
            },
            onFailure = { e ->
                Toast.makeText(context, "Failed to block: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        )
    }

    private fun showRemoveFriendDialog(friend: userModel, position: Int) {
        AlertDialog.Builder(context)
            .setTitle("Remove Friend")
            .setMessage("Are you sure you want to remove ${friend.username} from your friends?")
            .setPositiveButton("Remove") { _, _ ->
                removeFriend(friend, position)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun removeFriend(friend: userModel, position: Int) {
        FireBase_utils.removeFriend(
            friend.userID ?: "",
            onSuccess = {
                Toast.makeText(context, "${friend.username} removed from friends", Toast.LENGTH_SHORT).show()
                friendsList.removeAt(position)
                notifyItemRemoved(position)
                notifyItemRangeChanged(position, friendsList.size)
            },
            onFailure = { e ->
                Toast.makeText(context, "Failed to remove friend: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        )
    }

    override fun getItemCount(): Int = friendsList.size

    class FriendViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val profileImage: ImageView = itemView.findViewById(R.id.profile_image)
        val userName: TextView = itemView.findViewById(R.id.user_name)
        val userPhone: TextView = itemView.findViewById(R.id.user_phone)
        val videoCallIcon: ImageView = itemView.findViewById(R.id.video_call_icon)
        val removeIcon: ImageView = itemView.findViewById(R.id.remove_icon)
        val blockIcon: ImageView = itemView.findViewById(R.id.block_icon)
    }
}