package com.example.Smart_Chat.adapters.group

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.Smart_Chat.R
import com.example.Smart_Chat.models.userModel
import com.example.Smart_Chat.utils.others.androidUtils

class GroupBlockedMemberAdapter(
    private val blockedMembers: List<userModel>,
    private val context: Context,
    private val onUnblockMember: (String) -> Unit
) : RecyclerView.Adapter<GroupBlockedMemberAdapter.BlockedMemberViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BlockedMemberViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_blocked_member, parent, false)
        return BlockedMemberViewHolder(view)
    }

    override fun onBindViewHolder(holder: BlockedMemberViewHolder, position: Int) {
        val user = blockedMembers[position]

        holder.memberName.text = user.username

        // Load profile image
        if (!user.profileImage.isNullOrEmpty()) {
            val profileContainer = holder.itemView.findViewById<View>(R.id.profile_image_container)
            val profileImage = profileContainer.findViewById<ImageView>(R.id.profile_image)
            androidUtils.setProfileImageFromBase64(
                context,
                user.profileImage!!,
                profileImage
            )
        } else {
            val profileContainer = holder.itemView.findViewById<View>(R.id.profile_image_container)
            val profileImage = profileContainer.findViewById<ImageView>(R.id.profile_image)
            profileImage.setImageResource(R.drawable.ic_profile)
        }

        holder.unblockBtn.setOnClickListener {
            onUnblockMember(user.userID ?: "")
        }
    }

    override fun getItemCount(): Int = blockedMembers.size

    class BlockedMemberViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val memberName: TextView = itemView.findViewById(R.id.member_name)
        val unblockBtn: Button = itemView.findViewById(R.id.unblock_btn)
    }
}