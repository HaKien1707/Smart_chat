package com.example.Smart_Chat.adapters.user_chat

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
import com.example.Smart_Chat.utils.others.androidUtils
import com.example.Smart_Chat.utils.firebase.*
import com.firebase.ui.firestore.FirestoreRecyclerAdapter
import com.firebase.ui.firestore.FirestoreRecyclerOptions
import com.google.firebase.firestore.ListenerRegistration

class RecentChatRecyclerAdapter(
    options: FirestoreRecyclerOptions<UserChatModel>,
    private val context: Context,
    private val isDeletedView: Boolean = false
) : FirestoreRecyclerAdapter<UserChatModel, RecentChatRecyclerAdapter.ChatRoomViewHolder>(options) {

    private val currentUserID = FirebaseAuthentication.currentUserID()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChatRoomViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_recent_chat_recycler, parent, false)
        return ChatRoomViewHolder(view)
    }

    override fun onBindViewHolder(holder: ChatRoomViewHolder, position: Int, model: UserChatModel) {
        FirebaseChat.get2ndUserInChatRoom(model.userID)?.get()
            ?.addOnSuccessListener { documentSnapshot ->
                val otherUser = documentSnapshot.toObject(userModel::class.java)
                if (otherUser != null) {
                    holder.username.text = otherUser.username
                    if (!otherUser.profileImage.isNullOrBlank()) {
                        androidUtils.setProfileImageFromBase64(context, otherUser.profileImage, holder.profileImage)
                    } else {
                        holder.profileImage.setImageResource(R.drawable.ic_profile)
                    }

                    val lastMessageText = if (model.lastMsgSenderID == currentUserID) {
                        context.getString(R.string.you_prefix) + model.lastMsg
                    } else {
                        model.lastMsg
                    }
                    holder.lastMsg.text = lastMessageText

                    holder.lastMsgTime.text = androidUtils.timestampToString(model.lastMsgTimestamp)

                    holder.itemView.setOnClickListener {
                        val intent = Intent(context, ChatActivity::class.java)
                        androidUtils.passUserModelAsIntent(intent, otherUser)
                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        context.startActivity(intent)
                    }
                }
            }
    }

    class ChatRoomViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val profileImage: ImageView = itemView.findViewById(R.id.profile_image)
        val username: TextView = itemView.findViewById(R.id.username)
        val lastMsg: TextView = itemView.findViewById(R.id.lastMsg)
        val lastMsgTime: TextView = itemView.findViewById(R.id.lastMsgTime)
    }
}