package com.example.smart_chat.adapters.community

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.smart_chat.R
import com.example.smart_chat.models.userModel
import com.example.smart_chat.utils.others.androidUtils

class CommunityMemberAdapter(
    private val context: Context,
    private val membersList: List<userModel>,
    private var adminID: String?
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

        // Show "Owner" label if this is the admin
        if (member.userID == adminID) {
            holder.ownerLabel.visibility = View.VISIBLE
        } else {
            holder.ownerLabel.visibility = View.GONE
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

    fun updateAdminID(newAdminID: String?) {
        adminID = newAdminID
        notifyDataSetChanged()
    }

    class MemberViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val profileImageContainer: View = itemView.findViewById(R.id.profile_image_container)
        val memberName: TextView = itemView.findViewById(R.id.member_name)
        val memberStatus: TextView = itemView.findViewById(R.id.member_status)
        val ownerLabel: TextView = itemView.findViewById(R.id.owner_label)
    }
}
