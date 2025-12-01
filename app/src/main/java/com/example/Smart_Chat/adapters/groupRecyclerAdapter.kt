package com.example.Smart_Chat.adapters

import android.content.Context
import android.content.Intent
import android.widget.ImageButton
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.Smart_Chat.R
import com.example.Smart_Chat.activities.GroupChatActivity
import com.example.Smart_Chat.activities.GroupChatSettingsActivity
import com.example.Smart_Chat.models.groupModel
import com.example.Smart_Chat.utils.FireBase_utils
import com.example.Smart_Chat.utils.androidUtils
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
            if (model.lastMsgSenderID == FireBase_utils.currentUserID()) {
                holder.lastMsg.text = "You: ${model.lastMsg}"
            } else {
                // Show just the message or sender name if available
                holder.lastMsg.text = model.lastMsg
            }
        } else {
            holder.lastMsg.text = "No messages yet"
        }

        // --- Last message time (handle null timestamp) ---
        if (model.lastMsgTimestamp != null) {
            holder.lastMsgTime.text = androidUtils.timestampToString(model.lastMsgTimestamp)
        } else {
            holder.lastMsgTime.text = ""
        }

        // --- Member count ---
        val memberCount = model.memberIDs?.size ?: 0
        holder.memberCount.text = "$memberCount members"

        // --- Click listener to open group chat ---
        holder.itemView.setOnClickListener {
            val intent = Intent(context, GroupChatActivity::class.java)
            intent.putExtra("groupID", model.groupID)
            intent.putExtra("groupName", model.groupName)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            context.startActivity(intent)
        }

        // --- Settings button click ---
        holder.settingsBtn.setOnClickListener {
            val intent = Intent(context, GroupChatSettingsActivity::class.java)
            intent.putExtra("groupID", model.groupID)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            context.startActivity(intent)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GroupViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.group_item, parent, false)
        return GroupViewHolder(view)
    }

    inner class GroupViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val profileContainer: View = itemView.findViewById(R.id.group_image_container)
        val groupImage: ImageView = profileContainer.findViewById(R.id.profile_image)
        val groupNameText: TextView = itemView.findViewById(R.id.groupNameText)
        val lastMsg: TextView = itemView.findViewById(R.id.lastMsg)
        val lastMsgTime: TextView = itemView.findViewById(R.id.lastMsgTime)
        val memberCount: TextView = itemView.findViewById(R.id.memberCount)
        val settingsBtn: ImageButton = itemView.findViewById(R.id.group_settings_btn)
    }
}