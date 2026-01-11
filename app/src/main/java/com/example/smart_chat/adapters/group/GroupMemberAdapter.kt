package com.example.smart_chat.adapters.group

import android.content.Context
import android.view.Gravity
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

class GroupMemberAdapter(
    val members: List<Pair<userModel, Boolean>>, // Pair<user, isAdmin>
    private val context: Context,
    private val currentUserIsAdmin: Boolean,
    private val currentUserID: String?,
    private val onChatMember: (userModel) -> Unit,
    private val onRemoveMember: (String) -> Unit
) : RecyclerView.Adapter<GroupMemberAdapter.MemberViewHolder>() {

    private companion object {
        const val MENU_ID_CHAT = 1
        const val MENU_ID_REMOVE = 2
    }

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

        val isCurrentUser = user.userID == currentUserID

        // Show menu when tapping row or 3-dots (except for yourself)
        if (isCurrentUser) {
            holder.optionsBtn.visibility = View.GONE
            holder.itemView.setOnClickListener(null)
        } else {
            holder.optionsBtn.visibility = View.VISIBLE

            val openMenu: (View) -> Unit = { anchor ->
                showMemberMenu(anchor = anchor, member = user, memberIsAdmin = isAdmin)
            }

            holder.itemView.setOnClickListener {
                openMenu(holder.optionsBtn)
            }

            holder.optionsBtn.setOnClickListener { view ->
                openMenu(view)
            }
        }
    }

    override fun getItemCount(): Int = members.size

    private fun showMemberMenu(anchor: View, member: userModel, memberIsAdmin: Boolean) {
        val popup = PopupMenu(context, anchor, Gravity.END)

        popup.menu.add(0, MENU_ID_CHAT, 0, "Chat")

        // Admins can remove members (but typically not other admins)
        val canRemove = currentUserIsAdmin && !memberIsAdmin && member.userID != currentUserID
        if (canRemove) {
            popup.menu.add(0, MENU_ID_REMOVE, 1, "Remove")
        }

        popup.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                MENU_ID_CHAT -> {
                    onChatMember(member)
                    true
                }
                MENU_ID_REMOVE -> {
                    onRemoveMember(member.userID ?: "")
                    true
                }
                else -> false
            }
        }

        popup.show()
    }

    class MemberViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val memberName: TextView = itemView.findViewById(R.id.member_name)
        val memberRole: TextView = itemView.findViewById(R.id.member_role)
        val optionsBtn: ImageButton = itemView.findViewById(R.id.member_options_btn)
    }
}