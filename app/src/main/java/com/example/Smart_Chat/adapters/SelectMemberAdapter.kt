package com.example.Smart_Chat.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.Smart_Chat.R
import com.example.Smart_Chat.models.userModel
import com.example.Smart_Chat.utils.androidUtils

class SelectMemberAdapter(
    private val users: List<userModel>,
    private val context: Context,
    private val onMemberSelected: (String, Boolean) -> Unit
) : RecyclerView.Adapter<SelectMemberAdapter.MemberViewHolder>() {

    private val selectedUserIDs = mutableSetOf<String>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MemberViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_select_member, parent, false)
        return MemberViewHolder(view)
    }

    override fun onBindViewHolder(holder: MemberViewHolder, position: Int) {
        val user = users[position]

        // Set username and phone
        holder.usernameText.text = user.username
        holder.phoneText.text = user.phoneNumber

        // Set profile image
        if (!user.profileImage.isNullOrEmpty()) {
            androidUtils.setProfileImageFromBase64(
                context,
                user.profileImage!!,
                holder.profileImage
            )
        } else {
            holder.profileImage.setImageResource(R.drawable.ic_profile)
        }

        // Set checkbox state
        val isSelected = selectedUserIDs.contains(user.userID)
        holder.checkBox.isChecked = isSelected

        // Handle checkbox click
        holder.checkBox.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                user.userID?.let { selectedUserIDs.add(it) }
            } else {
                selectedUserIDs.remove(user.userID)
            }
            onMemberSelected(user.userID ?: "", isChecked)
        }

        // Handle item click (toggle checkbox)
        holder.itemView.setOnClickListener {
            holder.checkBox.isChecked = !holder.checkBox.isChecked
        }
    }

    override fun getItemCount(): Int = users.size

    class MemberViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val profileImage: ImageView = itemView.findViewById(R.id.profile_image)
        val usernameText: TextView = itemView.findViewById(R.id.usernameText)
        val phoneText: TextView = itemView.findViewById(R.id.phoneText)
        val checkBox: CheckBox = itemView.findViewById(R.id.member_checkbox)
    }
}