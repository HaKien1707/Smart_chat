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
    private val onChatMember: (userModel) -> Unit,
    private val onAddAdmin: (String) -> Unit,
    private val onRemoveAdmin: (String) -> Unit,
    private val onRemoveMember: (userModel) -> Unit
) : RecyclerView.Adapter<CommunityMemberAdapter.MemberViewHolder>() {

    private companion object {
        const val MENU_ID_CHAT = 1
        const val MENU_ID_ADD_ADMIN = 2
        const val MENU_ID_REMOVE_ADMIN = 3
        const val MENU_ID_REMOVE_MEMBER = 4
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MemberViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_community_member, parent, false)
        return MemberViewHolder(view)
    }

    override fun onBindViewHolder(holder: MemberViewHolder, position: Int) {
        val member = membersList[position]

        val userId = member.userID
        val displayName = member.username ?: "Unknown"
        holder.memberName.text = if (!userId.isNullOrBlank() && userId == currentUserID) {
            "$displayName (You)"
        } else {
            displayName
        }

        // Set status - show "last seen recently" for all users
        holder.memberStatus.text = "last seen recently"
        holder.memberStatus.setTextColor(
            context.getColor(android.R.color.darker_gray)
        )

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
        val currentUserIsAdmin = currentUserID != null && adminIDs.contains(currentUserID)

        val isCurrentUser = userId != null && userId == currentUserID
        if (isCurrentUser) {
            holder.optionsBtn.visibility = View.GONE
            holder.optionsBtn.setOnClickListener(null)
        } else {
            // Always show 3-dots for other members; actions depend on role.
            holder.optionsBtn.visibility = View.VISIBLE
            holder.optionsBtn.setOnClickListener {
                showMemberMenu(
                    anchor = holder.optionsBtn,
                    member = member,
                    currentUserIsOwner = currentUserIsOwner,
                    currentUserIsAdmin = currentUserIsAdmin
                )
            }
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

    private fun showMemberMenu(
        anchor: View,
        member: userModel,
        currentUserIsOwner: Boolean,
        currentUserIsAdmin: Boolean
    ) {
        val popup = PopupMenu(context, anchor)
        popup.menu.add(0, MENU_ID_CHAT, 0, "Chat")

        val memberId = member.userID
        val isOwnerMember = memberId != null && memberId == ownerID
        val memberIsAdmin = memberId != null && adminIDs.contains(memberId)

        // Owner-only: add/remove admin. Admins and regular users will not see this.
        if (currentUserIsOwner && memberId != null && !isOwnerMember && memberId != currentUserID) {
            if (memberIsAdmin) {
                popup.menu.add(0, MENU_ID_REMOVE_ADMIN, 1, "Remove from admin")
            } else {
                popup.menu.add(0, MENU_ID_ADD_ADMIN, 1, "Add admin")
            }
        }

        // Remove member
        val canRemoveMember = when {
            memberId.isNullOrBlank() -> false
            memberId == currentUserID -> false
            isOwnerMember -> false
            currentUserIsOwner -> true
            currentUserIsAdmin && !memberIsAdmin -> true
            else -> false
        }

        if (canRemoveMember) {
            popup.menu.add(0, MENU_ID_REMOVE_MEMBER, 2, "Remove")
        }

        popup.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                MENU_ID_CHAT -> {
                    onChatMember(member)
                    true
                }
                MENU_ID_ADD_ADMIN -> {
                    if (!memberId.isNullOrBlank()) onAddAdmin(memberId)
                    true
                }
                MENU_ID_REMOVE_ADMIN -> {
                    if (!memberId.isNullOrBlank()) onRemoveAdmin(memberId)
                    true
                }
                MENU_ID_REMOVE_MEMBER -> {
                    onRemoveMember(member)
                    true
                }
                else -> false
            }
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
