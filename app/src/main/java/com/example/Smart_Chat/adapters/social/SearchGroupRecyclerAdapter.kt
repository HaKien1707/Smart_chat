package com.example.Smart_Chat.adapters.social

import android.app.Activity
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.Smart_Chat.R
import com.example.Smart_Chat.activities.group_chat.GroupChatActivity
import com.example.Smart_Chat.activities.group_chat.GroupJoinRequestActivity
import com.example.Smart_Chat.models.group.groupModel
import com.example.Smart_Chat.utils.others.androidUtils
import com.example.Smart_Chat.utils.firebase.FirebaseAuthentication
import com.firebase.ui.firestore.FirestoreRecyclerAdapter
import com.firebase.ui.firestore.FirestoreRecyclerOptions

class SearchGroupRecyclerAdapter(
    options: FirestoreRecyclerOptions<groupModel>,
    private val activity: Activity
) : FirestoreRecyclerAdapter<groupModel, SearchGroupRecyclerAdapter.GroupModelViewHolder>(options) {

    override fun onBindViewHolder(holder: GroupModelViewHolder, position: Int, model: groupModel) {
        // Load group image
        if (!model.groupImage.isNullOrEmpty()) {
            androidUtils.setProfileImageFromBase64(
                activity,
                model.groupImage,
                holder.groupImage
            )
        } else {
            holder.groupImage.setImageResource(R.drawable.ic_group)
        }

        holder.groupName.text = model.groupName
        holder.memberCount.text = "${model.memberIDs?.size ?: 0} members"

        // Check if user is a member
        val isMember = model.memberIDs?.contains(FirebaseAuthentication.currentUserID()) == true

        holder.itemView.setOnClickListener {
            if (isMember) {
                // Open group chat
                val intent = Intent(activity, GroupChatActivity::class.java)
                intent.putExtra("groupID", model.groupID)
                intent.putExtra("groupName", model.groupName)
                activity.startActivity(intent)
            } else {
                // Open join request screen
                val intent = Intent(activity, GroupJoinRequestActivity::class.java)
                intent.putExtra("groupID", model.groupID)
                activity.startActivity(intent)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GroupModelViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_search_group, parent, false)
        return GroupModelViewHolder(view)
    }

    inner class GroupModelViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val groupImage: ImageView = itemView.findViewById(R.id.group_image)
        val groupName: TextView = itemView.findViewById(R.id.groupName)
        val memberCount: TextView = itemView.findViewById(R.id.memberCount)
    }
}