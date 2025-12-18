package com.example.Smart_Chat.adapters

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.util.Log
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
import com.example.Smart_Chat.activities.user_chat.ChatActivity
import com.example.Smart_Chat.models.UserChatModel
import com.example.Smart_Chat.models.userModel
import com.example.Smart_Chat.utils.FireBase_utils
import com.example.Smart_Chat.utils.androidUtils
import com.example.Smart_Chat.utils.firebase.FirebaseFriends
import com.firebase.ui.firestore.FirestoreRecyclerAdapter
import com.firebase.ui.firestore.FirestoreRecyclerOptions

class RecentChatRecyclerAdapter(
    options: FirestoreRecyclerOptions<UserChatModel>,
    private val context: Context,
    private val isDeletedView: Boolean = false
) : FirestoreRecyclerAdapter<UserChatModel, RecentChatRecyclerAdapter.ChatRoomViewHolder>(options) {

    // Filter items based on deleted status
    private val filteredItems = mutableListOf<UserChatModel>()
    private val currentUserID = FireBase_utils.currentUserID()

    private val unreadListeners = mutableMapOf<String, com.google.firebase.firestore.ListenerRegistration>()

    override fun onDataChanged() {
        super.onDataChanged()
        updateFilteredItems()
    }

    private fun updateFilteredItems() {
        filteredItems.clear()

        for (i in 0 until snapshots.size) {
            val model = snapshots.getSnapshot(i).toObject(UserChatModel::class.java)
            if (model != null) {
                val isDeletedByCurrentUser = model.deletedBy.contains(currentUserID)

                // Add to filtered list based on view type
                if (isDeletedView && isDeletedByCurrentUser) {
                    filteredItems.add(model)
                } else if (!isDeletedView && !isDeletedByCurrentUser) {
                    filteredItems.add(model)
                }
            }
        }

        notifyDataSetChanged()
    }

    override fun getItemCount(): Int {
        return filteredItems.size
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChatRoomViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_recent_chat_recycler, parent, false)
        return ChatRoomViewHolder(view)
    }

    override fun onBindViewHolder(holder: ChatRoomViewHolder, position: Int, model: UserChatModel) {
        // This won't be called - we override the other onBindViewHolder below
    }

    override fun onBindViewHolder(holder: ChatRoomViewHolder, position: Int) {
        if (position >= filteredItems.size) {
            return
        }

        val model = filteredItems[position]

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
                    FirebaseFriends.checkFriendshipStatus(otherUser?.userID ?: "") { status ->
                        (context as? Activity)?.runOnUiThread {
                            if (status == FirebaseFriends.FriendshipStatus.FRIENDS) {
                                holder.lastMsg.text = model.lastMsg
                                holder.lastMsg.setTextColor(context.getColor(R.color.black))
                                holder.lastMsgTime.visibility = View.VISIBLE
                                holder.lastMsgTime.text = androidUtils.timestampToString(model.lastMsgTimestamp)
                                // Count unread messages for this chat
                                val chatRoomID = model.chatRoomID ?: ""
                                if (chatRoomID.isNotEmpty()) {
                                    // Remove old listener if exists
                                    unreadListeners[chatRoomID]?.remove()

                                    // Add real-time listener
                                    val listener = FireBase_utils.getChatRoomMessagesReferences(chatRoomID)
                                        .whereEqualTo("senderID", otherUser?.userID)
                                        .whereEqualTo("isRead", false)
                                        .addSnapshotListener { snapshots, error ->
                                            if (error != null) {
                                                Log.e("RecentChatAdapter", "Error listening to unread count", error)
                                                return@addSnapshotListener
                                            }

                                            if (snapshots != null) {
                                                val unreadCount = snapshots.size()
                                                context.runOnUiThread {
                                                    // Make sure the view is still valid
                                                    if (holder.bindingAdapterPosition != RecyclerView.NO_POSITION) {
                                                        if (unreadCount > 0) {
                                                            holder.unreadCount.visibility = View.VISIBLE
                                                            holder.unreadCount.text = if (unreadCount > 9) "9+" else unreadCount.toString()
                                                        } else {
                                                            holder.unreadCount.visibility = View.GONE
                                                        }
                                                    }
                                                }
                                            }
                                        }

                                    // Store listener for cleanup
                                    unreadListeners[chatRoomID] = listener
                                }
                            } else {
                                holder.lastMsg.text = "You and this user are not friends"
                                holder.lastMsg.setTextColor(context.getColor(R.color.red))
                                holder.lastMsgTime.visibility = View.GONE
                            }
                        }
                    }

                    // Click to open chat (only for active chats)
                    if (!isDeletedView) {
                        holder.itemView.setOnClickListener {
                            val intent = Intent(context, ChatActivity::class.java)
                            androidUtils.passUserModelAsIntent(intent, otherUser)
                            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                            context.startActivity(intent)
                        }
                    } else {
                        holder.itemView.setOnClickListener(null)
                        holder.itemView.isClickable = false
                    }

                    // Configure action button based on view type
                    // Use holder.bindingAdapterPosition to get current position
                    if (isDeletedView) {
                        // In deleted view: show recover icon
                        holder.deleteBtn.setImageResource(R.drawable.ic_restore)
                        holder.deleteBtn.setOnClickListener {
                            val currentPosition = holder.bindingAdapterPosition
                            if (currentPosition != RecyclerView.NO_POSITION && currentPosition < filteredItems.size) {
                                showRecoverDialog(filteredItems[currentPosition], currentPosition)
                            }
                        }
                    } else {
                        // In normal view: show delete button
                        holder.deleteBtn.setImageResource(R.drawable.ic_close)
                        holder.deleteBtn.setOnClickListener {
                            val currentPosition = holder.bindingAdapterPosition
                            if (currentPosition != RecyclerView.NO_POSITION && currentPosition < filteredItems.size) {
                                showDeleteChatDialog(filteredItems[currentPosition], currentPosition)
                            }
                        }
                    }
                }
            }
    }

    private fun showDeleteChatDialog(chatRoom: UserChatModel, position: Int) {
        AlertDialog.Builder(context)
            .setTitle("Delete Chat")
            .setMessage("This chat will be moved to Deleted Chats. You can recover it later or delete it permanently.")
            .setPositiveButton("Delete") { _, _ ->
                softDeleteChat(chatRoom, position)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showRecoverDialog(chatRoom: UserChatModel, position: Int) {
        AlertDialog.Builder(context)
            .setTitle("Recover Chat")
            .setMessage("What would you like to do?")
            .setPositiveButton("Recover") { _, _ ->
                recoverChat(chatRoom, position)
            }
            .setNegativeButton("Delete Forever") { _, _ ->
                showPermanentDeleteDialog(chatRoom, position)
            }
            .setNeutralButton("Cancel", null)
            .show()
    }

    private fun showPermanentDeleteDialog(chatRoom: UserChatModel, position: Int) {
        AlertDialog.Builder(context)
            .setTitle("Delete Permanently")
            .setMessage("Are you sure? This will permanently delete all messages. This cannot be undone.")
            .setPositiveButton("Delete Forever") { _, _ ->
                permanentlyDeleteChat(chatRoom, position)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun softDeleteChat(chatRoom: UserChatModel, position: Int) {
        Log.d("ADAPTER_DELETE", "Deleting chat at position $position: ${chatRoom.chatRoomID}")

        FireBase_utils.softDeleteChatRoom(
            chatRoom.chatRoomID ?: "",
            onSuccess = {
                (context as? Activity)?.runOnUiThread {
                    Toast.makeText(context, "Chat moved to Deleted Chats", Toast.LENGTH_SHORT).show()

                    // Verify position is still valid
                    if (position >= 0 && position < filteredItems.size) {
                        // Double-check we're removing the right item
                        if (filteredItems[position].chatRoomID == chatRoom.chatRoomID) {
                            filteredItems.removeAt(position)
                            notifyItemRemoved(position)
                            // Update remaining items
                            if (position < filteredItems.size) {
                                notifyItemRangeChanged(position, filteredItems.size - position)
                            }
                        }
                    }
                }
            },
            onFailure = { e ->
                (context as? Activity)?.runOnUiThread {
                    Toast.makeText(context, "Failed to delete: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        )
    }

    private fun recoverChat(chatRoom: UserChatModel, position: Int) {
        Log.d("ADAPTER_RECOVER", "Recovering chat at position $position: ${chatRoom.chatRoomID}")

        FireBase_utils.recoverChatRoom(
            chatRoom.chatRoomID ?: "",
            onSuccess = {
                (context as? Activity)?.runOnUiThread {
                    Toast.makeText(context, "Chat recovered", Toast.LENGTH_SHORT).show()

                    // Verify position is still valid
                    if (position >= 0 && position < filteredItems.size) {
                        // Double-check we're removing the right item
                        if (filteredItems[position].chatRoomID == chatRoom.chatRoomID) {
                            filteredItems.removeAt(position)
                            notifyItemRemoved(position)
                            // Update remaining items
                            if (position < filteredItems.size) {
                                notifyItemRangeChanged(position, filteredItems.size - position)
                            }
                        }
                    }
                }
            },
            onFailure = { e ->
                (context as? Activity)?.runOnUiThread {
                    Toast.makeText(context, "Failed to recover: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        )
    }

    private fun permanentlyDeleteChat(chatRoom: UserChatModel, position: Int) {
        Log.d("ADAPTER_PERM_DELETE", "Permanently deleting chat at position $position: ${chatRoom.chatRoomID}")

        FireBase_utils.permanentlyDeleteChatRoom(
            chatRoom.chatRoomID ?: "",
            onSuccess = {
                (context as? Activity)?.runOnUiThread {
                    Toast.makeText(context, "Chat permanently deleted", Toast.LENGTH_SHORT).show()

                    // Verify position is still valid
                    if (position >= 0 && position < filteredItems.size) {
                        // Double-check we're removing the right item
                        if (filteredItems[position].chatRoomID == chatRoom.chatRoomID) {
                            filteredItems.removeAt(position)
                            notifyItemRemoved(position)
                            // Update remaining items
                            if (position < filteredItems.size) {
                                notifyItemRangeChanged(position, filteredItems.size - position)
                            }
                        }
                    }
                }
            },
            onFailure = { e ->
                (context as? Activity)?.runOnUiThread {
                    Toast.makeText(context, "Failed to delete: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        )
    }

    override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
        super.onDetachedFromRecyclerView(recyclerView)
        // Clean up all listeners
        unreadListeners.values.forEach { it.remove() }
        unreadListeners.clear()
    }

    class ChatRoomViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val profileImage: ImageView = itemView.findViewById(R.id.profile_image)
        val username: TextView = itemView.findViewById(R.id.username)
        val lastMsg: TextView = itemView.findViewById(R.id.lastMsg)
        val lastMsgTime: TextView = itemView.findViewById(R.id.lastMsgTime)
        val unreadCount: TextView = itemView.findViewById(R.id.unreadCount)
        val deleteBtn: ImageButton = itemView.findViewById(R.id.delete_btn)
    }
}