package com.example.Smart_Chat.adapters

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.RecyclerView
import com.example.Smart_Chat.R
import com.example.Smart_Chat.activities.chatActivity
import com.example.Smart_Chat.models.chatRoomModel
import com.example.Smart_Chat.models.userModel
import com.example.Smart_Chat.utils.FireBase_utils
import com.example.Smart_Chat.utils.androidUtils
import com.firebase.ui.firestore.FirestoreRecyclerAdapter
import com.firebase.ui.firestore.FirestoreRecyclerOptions

class RecentChatRecyclerAdapter(
    options: FirestoreRecyclerOptions<chatRoomModel>,
    private val context: Context
) : FirestoreRecyclerAdapter<chatRoomModel, RecentChatRecyclerAdapter.ChatRoomViewHolder>(options) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChatRoomViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.recent_chat_recycler_item, parent, false)  // Make sure this matches your layout file name
        return ChatRoomViewHolder(view)
    }

    override fun onBindViewHolder(holder: ChatRoomViewHolder, position: Int, model: chatRoomModel) {
        FireBase_utils.get2ndUserInChatRoom(model.userID)?.get()
            ?.addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val otherUser = task.result.toObject(userModel::class.java)

                    holder.username.text = otherUser?.username ?: ""

                    // Load profile image
                    if (!otherUser?.profileImage.isNullOrBlank()) {
                        androidUtils.setProfileImageFromBase64(
                            context,
                            otherUser?.profileImage,
                            holder.profileImage
                        )
                    } else {
                        holder.profileImage.setImageResource(R.drawable.ic_profile)
                    }

                    // Check friendship status
                    FireBase_utils.checkFriendshipStatus(otherUser?.userID ?: "") { status ->
                        (context as? Activity)?.runOnUiThread {
                            if (status == FireBase_utils.FriendshipStatus.FRIENDS) {
                                holder.lastMsg.text = model.lastMsg
                                holder.lastMsg.setTextColor(context.getColor(R.color.black))
                                holder.lastMsgTime.visibility = View.VISIBLE
                                holder.lastMsgTime.text = androidUtils.timestampToString(model.lastMsgTimestamp)
                            } else {
                                holder.lastMsg.text = "You and this user are not friends"
                                holder.lastMsg.setTextColor(context.getColor(R.color.red))
                                holder.lastMsgTime.visibility = View.GONE
                            }
                        }
                    }

                    // Click to open chat
                    holder.itemView.setOnClickListener {
                        val intent = Intent(context, chatActivity::class.java)
                        androidUtils.passUserModelAsIntent(intent, otherUser)
                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        context.startActivity(intent)
                    }

                    // Delete button
                    holder.deleteBtn.setOnClickListener {
                        showDeleteChatDialog(model, position)
                    }
                }
            }
    }

    private fun showDeleteChatDialog(chatRoom: chatRoomModel, position: Int) {
        AlertDialog.Builder(context)
            .setTitle("Delete Chat")
            .setMessage("Are you sure you want to permanently delete this chat? This cannot be undone.")
            .setPositiveButton("Delete") { _, _ ->
                deleteChat(chatRoom, position)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun deleteChat(chatRoom: chatRoomModel, position: Int) {
        FireBase_utils.deleteChatRoom(
            chatRoom.chatRoomID ?: "",
            onSuccess = {
                Toast.makeText(context, "Chat deleted", Toast.LENGTH_SHORT).show()
                notifyItemRemoved(position)
            },
            onFailure = { e ->
                Toast.makeText(context, "Failed to delete: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        )
    }

    class ChatRoomViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val profileImage: ImageView = itemView.findViewById(R.id.profile_image)
        val username: TextView = itemView.findViewById(R.id.username)
        val lastMsg: TextView = itemView.findViewById(R.id.lastMsg)
        val lastMsgTime: TextView = itemView.findViewById(R.id.lastMsgTime)
        val deleteBtn: ImageButton = itemView.findViewById(R.id.delete_btn)
    }
}