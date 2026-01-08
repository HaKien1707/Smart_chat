package com.example.Smart_Chat.adapters.group

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.Smart_Chat.R
import com.example.Smart_Chat.activities.group_chat.GroupChatActivity
import com.example.Smart_Chat.models.group.groupModel
import com.example.Smart_Chat.utils.others.androidUtils
import com.firebase.ui.firestore.FirestoreRecyclerAdapter
import com.firebase.ui.firestore.FirestoreRecyclerOptions

class SearchGroupAdapter(
    options: FirestoreRecyclerOptions<groupModel>,
    private val context: Context
) : FirestoreRecyclerAdapter<groupModel, SearchGroupAdapter.GroupViewHolder>(options) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GroupViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_search_group, parent, false)
        return GroupViewHolder(view)
    }

    override fun onBindViewHolder(holder: GroupViewHolder, position: Int, model: groupModel) {
        holder.groupName.text = model.groupName ?: "Unnamed Group"
        
        val memberCount = model.memberIDs?.size ?: 0
        holder.memberCount.text = "$memberCount members"

        // Load group image
        if (!model.groupImage.isNullOrEmpty()) {
            androidUtils.setProfileImageFromBase64(
                context,
                model.groupImage!!,
                holder.groupImage
            )
        } else {
            holder.groupImage.setImageResource(R.drawable.ic_group)
        }

        // Click to open group chat
        holder.itemView.setOnClickListener {
            val intent = Intent(context, GroupChatActivity::class.java)
            intent.putExtra("groupID", model.groupID)
            intent.putExtra("groupName", model.groupName)
            context.startActivity(intent)
        }
    }

    class GroupViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val groupImage: ImageView = itemView.findViewById(R.id.group_image)
        val groupName: TextView = itemView.findViewById(R.id.groupName)
        val memberCount: TextView = itemView.findViewById(R.id.memberCount)
    }
}
