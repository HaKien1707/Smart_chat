package com.example.smart_chat.adapters.social

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.smart_chat.R
import com.example.smart_chat.models.msg_action.ForwardChatItemModel
import com.example.smart_chat.models.msg_action.ForwardChatType
import com.example.smart_chat.utils.others.androidUtils

class ForwardChatAdapter(
    private val context: Context,
    private val chats: MutableList<ForwardChatItemModel>,
    private val onSelectionChanged: (List<ForwardChatItemModel>) -> Unit
) : RecyclerView.Adapter<ForwardChatAdapter.ForwardChatViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ForwardChatViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_forward_chat, parent, false)
        return ForwardChatViewHolder(view)
    }

    override fun onBindViewHolder(holder: ForwardChatViewHolder, position: Int) {
        val chat = chats[position]

        holder.chatName.text = chat.name
        holder.checkbox.isChecked = chat.isSelected

        // Load profile image
        if (!chat.imageUrl.isNullOrBlank()) {
            androidUtils.setProfileImageFromBase64(context, chat.imageUrl, holder.profileImage)
        } else {
            val iconRes = when (chat.type) {
                ForwardChatType.USER -> R.drawable.ic_profile
                ForwardChatType.GROUP -> R.drawable.ic_group
            }
            holder.profileImage.setImageResource(iconRes)
        }

        // Handle checkbox click
        holder.checkbox.setOnCheckedChangeListener(null) // Clear previous listener
        holder.checkbox.setOnCheckedChangeListener { _, isChecked ->
            chats[position] = chat.copy(isSelected = isChecked)
            onSelectionChanged(getSelectedChats())
        }

        // Handle item click
        holder.itemView.setOnClickListener {
            holder.checkbox.isChecked = !holder.checkbox.isChecked
        }
    }

    override fun getItemCount(): Int = chats.size

    private fun getSelectedChats(): List<ForwardChatItemModel> {
        return chats.filter { it.isSelected }
    }

    class ForwardChatViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val checkbox: CheckBox = itemView.findViewById(R.id.checkbox)
        val profileImage: ImageView = itemView.findViewById(R.id.profile_image)
        val chatName: TextView = itemView.findViewById(R.id.chat_name)
    }
}