package com.example.Smart_Chat.adapters

import android.content.Context
import android.content.Intent
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
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

    override fun onBindViewHolder(holder: ChatRoomViewHolder, position: Int, model: chatRoomModel) {

        // Use the safe call operator (?.) to handle a potentially null DocumentReference
        FireBase_utils.get2ndUserInChatRoom(model.userID)
            ?.get() // This will only be called if the result is not null
            ?.addOnSuccessListener { document ->
                // Check if the document exists and can be converted
                if (document.exists()) {
                    val otherUser = document.toObject(userModel::class.java)

                    if (otherUser != null) {
                        Log.d(
                            "RecentChat",
                            "User: ${otherUser.username}, Has image: ${otherUser.profileImage != null}"
                        )

                        // --- Load profile image ---
                        if (!otherUser.profileImage.isNullOrEmpty()) {
                            androidUtils.setProfileImageFromBase64(
                                context,
                                otherUser.profileImage,
                                holder.profileImage
                            )
                        } else {
                            holder.profileImage.setImageResource(R.drawable.ic_person)
                        }

                        // --- Username ---
                        holder.usernameText.text = otherUser.username

                        // --- Last message display ---
                        holder.lastMsg.text =
                            if (model.lastMsgSenderID == FireBase_utils.currentUserID())
                                "You: ${model.lastMsg}"
                            else model.lastMsg

                        // --- Last message time ---
                        holder.lastMsgTime.text =
                            androidUtils.timestampToString(model.lastMsgTimestamp)

                        // --- Click listener ---
                        holder.itemView.setOnClickListener {
                            val intent = Intent(context, chatActivity::class.java)
                            androidUtils.passUserModelAsIntent(intent, otherUser)
                            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                            context.startActivity(intent)
                        }
                    }
                }
            }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChatRoomViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.recent_chat_recycler_item, parent, false)
        return ChatRoomViewHolder(view)
    }

    inner class ChatRoomViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val profileImage: ImageView = itemView.findViewById(R.id.profile_image)
        val usernameText: TextView = itemView.findViewById(R.id.usernameText)
        val lastMsg: TextView = itemView.findViewById(R.id.lastMsg)
        val lastMsgTime: TextView = itemView.findViewById(R.id.lastMsgTime)
    }
}
