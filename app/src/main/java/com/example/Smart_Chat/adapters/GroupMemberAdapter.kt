package com.example.Smart_Chat.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.Smart_Chat.R
import com.example.Smart_Chat.models.userModel
import com.example.Smart_Chat.utils.androidUtils

class GroupMemberAdapter(
    private val members: List<Pair<userModel, Boolean>>, // Pair<user, isAdmin>
    private val context: Context,
    private val currentUserIsAdmin: Boolean,
    private val currentUserID: String?,
    private val onRemoveMember: (String) -> Unit
) : RecyclerView.Adapter<GroupMemberAdapter.MemberViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MemberViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.group_member_item, parent, false)
        return MemberViewHolder(view)
    }

    override fun onBindViewHolder(holder: MemberViewHolder, position: Int) {
        val (user, isAdmin) = members[position]

        // Set member name
        holder.memberName.text = user.username

        // Show "You" for current user
        if (user.userID == currentUserID) {
            holder.memberName.text = "${user.username} (You)"
        }

        // Show admin badge
        if (isAdmin) {
            holder.memberRole.visibility = View.VISIBLE
            holder.memberRole.text = "Admin"
        } else {
            holder.memberRole.visibility = View.GONE
        }

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

        // Show remove button only if:
        // 1. Current user is admin
        // 2. Member is not the current user (can't remove yourself)
        // 3. Member is not an admin (admins can't remove other admins)
        val canRemove = currentUserIsAdmin &&
                user.userID != currentUserID &&
                !isAdmin

        if (canRemove) {
            holder.removeBtn.visibility = View.VISIBLE
            holder.removeBtn.setOnClickListener {
                onRemoveMember(user.userID ?: "")
            }
        } else {
            holder.removeBtn.visibility = View.GONE
        }
    }

    override fun getItemCount(): Int = members.size

    class MemberViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val memberName: TextView = itemView.findViewById(R.id.member_name)
        val memberRole: TextView = itemView.findViewById(R.id.member_role)
        val removeBtn: ImageButton = itemView.findViewById(R.id.remove_member_btn)
    }
}