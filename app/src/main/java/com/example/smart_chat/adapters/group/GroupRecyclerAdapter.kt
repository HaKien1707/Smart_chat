package com.example.smart_chat.adapters.group

import android.content.Context
import android.content.Intent
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.smart_chat.R
import com.example.smart_chat.activities.group_chat.GroupChatActivity
import com.example.smart_chat.models.group.groupModel
import com.example.smart_chat.utils.others.androidUtils
import com.example.smart_chat.utils.firebase.FirebaseAuthentication
import com.firebase.ui.firestore.FirestoreRecyclerAdapter
import com.firebase.ui.firestore.FirestoreRecyclerOptions

class GroupRecyclerAdapter(
    options: FirestoreRecyclerOptions<groupModel>,
    private val context: Context
) : FirestoreRecyclerAdapter<groupModel, GroupRecyclerAdapter.GroupViewHolder>(options) {

    override fun onBindViewHolder(holder: GroupViewHolder, position: Int, model: groupModel) {
        Log.d("GroupAdapter", "Binding group: ${model.groupName}")

        // --- Group name ---
        holder.groupNameText.text = model.groupName ?: "Unnamed Group"

        // --- Group image ---
        if (!model.groupImage.isNullOrEmpty()) {
            androidUtils.setProfileImageFromBase64(
                context,
                model.groupImage!!,
                holder.groupImage
            )
        } else {
            holder.groupImage.setImageResource(R.drawable.ic_group)
        }

        // --- Last message ---
        if (model.lastMsg != null) {
            // Get sender name for last message
            if (model.lastMsgSenderID == FirebaseAuthentication.currentUserID()) {
                holder.lastMsg.text = "You: ${model.lastMsg}"
            } else {
                // Show just the message or sender name if available
                holder.lastMsg.text = model.lastMsg
            }
        } else {
            holder.lastMsg.text = "No messages yet"
        }

        // --- Last message time (handle null timestamp) ---
        holder.lastMsgTime.text = if (model.lastMsgTimestamp != null) {
            androidUtils.timestampToString(model.lastMsgTimestamp)
        } else {
            ""
        }

        // --- Click listener to open group chat ---
        holder.itemView.setOnClickListener {
            val intent = Intent(context, GroupChatActivity::class.java)
            intent.putExtra("groupID", model.groupID)
            intent.putExtra("groupName", model.groupName)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            context.startActivity(intent)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GroupViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_group, parent, false)
        return GroupViewHolder(view)
    }

    inner class GroupViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val groupImage: ImageView = itemView.findViewById(R.id.group_image)
        val groupNameText: TextView = itemView.findViewById(R.id.groupNameText)
        val lastMsg: TextView = itemView.findViewById(R.id.lastMsg)
        val lastMsgTime: TextView = itemView.findViewById(R.id.lastMsgTime)
        // Removed: val settingsBtn: ImageButton
    }
}