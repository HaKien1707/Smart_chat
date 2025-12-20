package com.example.Smart_Chat.adapters.group

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
import com.example.Smart_Chat.utils.others.androidUtils

class GroupMemberAdapter(
    val members: List<Pair<userModel, Boolean>>, // Pair<user, isAdmin>
    private val context: Context,
    private val currentUserIsAdmin: Boolean,
    private val currentUserID: String?,
    private val onMemberClick: (userModel) -> Unit,
    private val onRemoveMember: (String) -> Unit,
    private val onBlockMember: (String) -> Unit
) : RecyclerView.Adapter<GroupMemberAdapter.MemberViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MemberViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_group_member, parent, false)
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

        // Click to open chat (except for yourself)
        if (user.userID != currentUserID) {
            holder.itemView.setOnClickListener {
                onMemberClick(user)
            }
        }

        // Show action buttons only for admins, not for current user, not for other admins
        val canTakeAction = currentUserIsAdmin &&
                user.userID != currentUserID &&
                !isAdmin

        if (canTakeAction) {
            holder.blockBtn.visibility = View.VISIBLE
            holder.blockBtn.setOnClickListener {
                onBlockMember(user.userID ?: "")
            }

            holder.removeBtn.visibility = View.VISIBLE
            holder.removeBtn.setOnClickListener {
                onRemoveMember(user.userID ?: "")
            }
        } else {
            holder.blockBtn.visibility = View.GONE
            holder.removeBtn.visibility = View.GONE
        }
    }

    override fun getItemCount(): Int = members.size

    class MemberViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val memberName: TextView = itemView.findViewById(R.id.member_name)
        val memberRole: TextView = itemView.findViewById(R.id.member_role)
        val blockBtn: ImageButton = itemView.findViewById(R.id.block_member_btn)
        val removeBtn: ImageButton = itemView.findViewById(R.id.remove_member_btn)
    }
}