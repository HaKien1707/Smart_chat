package com.example.smart_chat.adapters.community

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.widget.PopupMenu
import androidx.recyclerview.widget.RecyclerView
import com.example.smart_chat.R
import com.example.smart_chat.models.userModel
import com.example.smart_chat.utils.others.androidUtils

class CommunityMemberAdapter(
    private val context: Context,
    private val membersList: List<userModel>,
    private var ownerID: String?,
    private var adminIDs: Set<String>,
    private val currentUserID: String?,
    private val onAddAdmin: (String) -> Unit,
    private val onRemoveAdmin: (String) -> Unit
) : RecyclerView.Adapter<CommunityMemberAdapter.MemberViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MemberViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_community_member, parent, false)
        return MemberViewHolder(view)
    }

    override fun onBindViewHolder(holder: MemberViewHolder, position: Int) {
        val member = membersList[position]

        holder.memberName.text = member.username ?: "Unknown"

        // Set status - show "last seen recently" for all users
        holder.memberStatus.text = "last seen recently"
        holder.memberStatus.setTextColor(
            context.getColor(android.R.color.darker_gray)
        )

        val userId = member.userID
        val isOwner = userId != null && userId == ownerID
        val isAdmin = userId != null && adminIDs.contains(userId)

        if (isOwner) {
            holder.ownerLabel.visibility = View.VISIBLE
            holder.ownerLabel.text = "Owner"
        } else if (isAdmin) {
            holder.ownerLabel.visibility = View.VISIBLE
            holder.ownerLabel.text = "Admin"
        } else {
            holder.ownerLabel.visibility = View.GONE
        }

        val currentUserIsOwner = ownerID != null && ownerID == currentUserID
        val canManageThisMember = currentUserIsOwner && userId != null && userId != ownerID && userId != currentUserID

        if (canManageThisMember) {
            holder.optionsBtn.visibility = View.VISIBLE
            holder.optionsBtn.setOnClickListener {
                showMemberMenu(anchor = holder.optionsBtn, memberId = userId, memberIsAdmin = isAdmin)
            }
        } else {
            holder.optionsBtn.visibility = View.GONE
            holder.optionsBtn.setOnClickListener(null)
        }

        // Load profile image
        val profileImageUrl = member.profileImage
        val profileImageView = holder.profileImageContainer.findViewById<ImageView>(R.id.profile_image)

        if (!profileImageUrl.isNullOrBlank()) {
            androidUtils.setProfileImageFromBase64(context, profileImageUrl, profileImageView)
        } else {
            profileImageView.setImageResource(R.drawable.ic_profile)
        }
    }

    override fun getItemCount(): Int = membersList.size

    fun updateRoles(newOwnerID: String?, newAdminIDs: Set<String>) {
        ownerID = newOwnerID
        adminIDs = newAdminIDs
        notifyDataSetChanged()
    }

    private fun showMemberMenu(anchor: View, memberId: String, memberIsAdmin: Boolean) {
        val popup = PopupMenu(context, anchor)
        if (memberIsAdmin) {
            popup.menu.add("Remove from admin")
        } else {
            popup.menu.add("Add admin")
        }

        popup.setOnMenuItemClickListener {
            if (memberIsAdmin) {
                onRemoveAdmin(memberId)
            } else {
                onAddAdmin(memberId)
            }
            true
        }

        popup.show()
    }

    class MemberViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val profileImageContainer: View = itemView.findViewById(R.id.profile_image_container)
        val memberName: TextView = itemView.findViewById(R.id.member_name)
        val memberStatus: TextView = itemView.findViewById(R.id.member_status)
        val ownerLabel: TextView = itemView.findViewById(R.id.owner_label)
        val optionsBtn: ImageButton = itemView.findViewById(R.id.member_options_btn)
    }
}
