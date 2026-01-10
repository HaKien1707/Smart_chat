package com.example.smart_chat.adapters.user_chat

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.smart_chat.R
import com.example.smart_chat.activities.user_chat.ChatActivity
import com.example.smart_chat.models.UserChatModel
import com.example.smart_chat.models.userModel
import com.example.smart_chat.utils.others.androidUtils
import com.example.smart_chat.utils.firebase.*
import com.firebase.ui.firestore.FirestoreRecyclerAdapter
import com.firebase.ui.firestore.FirestoreRecyclerOptions

class RecentChatRecyclerAdapter(
    options: FirestoreRecyclerOptions<UserChatModel>,
    private val context: Context,
    private val isDeletedView: Boolean = false
) : FirestoreRecyclerAdapter<UserChatModel, RecentChatRecyclerAdapter.ChatRoomViewHolder>(options) {

    private fun prefixPreview(prefix: String, message: String?): String {
        val cleanedPrefix = prefix.trimEnd()
        val cleanedMessage = message.orEmpty()
        return if (cleanedMessage.isBlank()) cleanedPrefix else "$cleanedPrefix $cleanedMessage"
    }

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
                        prefixPreview(context.getString(R.string.you_prefix), model.lastMsg)
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