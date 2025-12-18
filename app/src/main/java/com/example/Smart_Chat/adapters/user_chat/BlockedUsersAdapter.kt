package com.example.Smart_Chat.adapters.user_chat

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.RecyclerView
import com.example.Smart_Chat.R
import com.example.Smart_Chat.models.userModel
import com.example.Smart_Chat.utils.FireBase_utils
import com.example.Smart_Chat.utils.androidUtils

class BlockedUsersAdapter(
    private val context: Context,
    private val blockedList: MutableList<userModel>
) : RecyclerView.Adapter<BlockedUsersAdapter.BlockedUserViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BlockedUserViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_user_blocked, parent, false)
        return BlockedUserViewHolder(view)
    }

    override fun onBindViewHolder(holder: BlockedUserViewHolder, position: Int) {
        val user = blockedList[position]

        holder.userName.text = user.username ?: "Unknown"
        holder.userPhone.text = user.phoneNumber ?: ""

        // Load profile image
        if (!user.profileImage.isNullOrBlank()) {
            androidUtils.setProfileImageFromBase64(context, user.profileImage, holder.profileImage)
        } else {
            holder.profileImage.setImageResource(R.drawable.ic_profile)
        }

        // Unblock button
        holder.unblockBtn.setOnClickListener {
            showUnblockDialog(user, position)
        }
    }

    private fun showUnblockDialog(user: userModel, position: Int) {
        AlertDialog.Builder(context)
            .setTitle("Unblock User")
            .setMessage("Unblock ${user.username}? They will be able to send you friend requests again.")
            .setPositiveButton("Unblock") { _, _ ->
                unblockUser(user, position)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun unblockUser(user: userModel, position: Int) {
        FireBase_utils.unblockUser(
            user.userID ?: "",
            onSuccess = {
                Toast.makeText(context, "${user.username} unblocked", Toast.LENGTH_SHORT).show()
                blockedList.removeAt(position)
                notifyItemRemoved(position)
                notifyItemRangeChanged(position, blockedList.size)
            },
            onFailure = { e ->
                Toast.makeText(context, "Failed to unblock: ${e.message}", Toast.LENGTH_SHORT).show()
                Log.e("BlockedUsersAdapter", "Unblock failed", e)
            }
        )
    }

    override fun getItemCount(): Int = blockedList.size

    class BlockedUserViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val profileImage: ImageView = itemView.findViewById(R.id.profile_image)
        val userName: TextView = itemView.findViewById(R.id.user_name)
        val userPhone: TextView = itemView.findViewById(R.id.user_phone)
        val unblockBtn: TextView = itemView.findViewById(R.id.unblock_btn)
    }
}