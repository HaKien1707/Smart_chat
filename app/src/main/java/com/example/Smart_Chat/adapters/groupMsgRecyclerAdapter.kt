package com.example.Smart_Chat.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.Smart_Chat.R
import com.example.Smart_Chat.models.GroupMsgModel
import com.example.Smart_Chat.utils.FireBase_utils.currentUserID
import com.firebase.ui.firestore.FirestoreRecyclerAdapter
import com.firebase.ui.firestore.FirestoreRecyclerOptions

class GroupMsgRecyclerAdapter(
    options: FirestoreRecyclerOptions<GroupMsgModel>,
    private val context: Context
) : FirestoreRecyclerAdapter<GroupMsgModel, GroupMsgRecyclerAdapter.GroupMsgViewHolder>(options) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GroupMsgViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.group_msg_row_item, parent, false)
        return GroupMsgViewHolder(view)
    }

    override fun onBindViewHolder(holder: GroupMsgViewHolder, position: Int, model: GroupMsgModel) {

        val isMe = model.senderID == currentUserID()

        if (isMe) {
            // My message
            holder.sender.visibility = View.GONE
            holder.receiver.visibility = View.VISIBLE

            // Always reset receiver content
            holder.receiverMsg.text = model.msg

            // Clear sender fields (avoid recycled trash)
            holder.senderName.text = ""
            holder.senderMsg.text = ""
        } else {
            // Other user's message
            holder.sender.visibility = View.VISIBLE
            holder.receiver.visibility = View.GONE

            holder.senderName.text = model.senderName ?: "Unknown"
            holder.senderMsg.text = model.msg

            // Clear receiver text (avoid recycled trash)
            holder.receiverMsg.text = ""
        }
    }

    class GroupMsgViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val sender: LinearLayout = itemView.findViewById(R.id.sender)
        val receiver: LinearLayout = itemView.findViewById(R.id.receiver)
        val senderName: TextView = itemView.findViewById(R.id.senderName)
        val senderMsg: TextView = itemView.findViewById(R.id.senderMsg)
        val receiverMsg: TextView = itemView.findViewById(R.id.receiverMsg)
    }
}