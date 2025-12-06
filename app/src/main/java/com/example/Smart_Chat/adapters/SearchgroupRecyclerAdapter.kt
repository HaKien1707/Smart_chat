package com.example.Smart_Chat.adapters

import android.app.Activity
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.Smart_Chat.R
import com.example.Smart_Chat.activities.GroupChatActivity
import com.example.Smart_Chat.models.groupModel
import com.example.Smart_Chat.utils.FireBase_utils
import com.example.Smart_Chat.utils.androidUtils
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

        // Check if user is already a member
        val isMember = model.memberIDs?.contains(FireBase_utils.currentUserID()) == true

        if (isMember) {
            holder.groupName.text = "${model.groupName} (Joined)"
            holder.itemView.setOnClickListener {
                val intent = Intent(activity, GroupChatActivity::class.java)
                intent.putExtra("groupID", model.groupID)
                intent.putExtra("groupName", model.groupName)
                activity.startActivity(intent)
                activity.finish()
            }
        } else {
            // Not a member - show as disabled or request to join
            holder.itemView.alpha = 0.5f
            holder.itemView.setOnClickListener(null)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GroupModelViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.search_group_item, parent, false)
        return GroupModelViewHolder(view)
    }

    inner class GroupModelViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val groupImage: ImageView = itemView.findViewById(R.id.group_image)
        val groupName: TextView = itemView.findViewById(R.id.groupName)
        val memberCount: TextView = itemView.findViewById(R.id.memberCount)
    }
}