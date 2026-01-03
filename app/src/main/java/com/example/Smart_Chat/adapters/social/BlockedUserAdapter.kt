package com.example.Smart_Chat.adapters.social

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.example.Smart_Chat.R
import com.example.Smart_Chat.models.userModel
import com.example.Smart_Chat.utils.firebase.FirebaseAuthentication
import com.example.Smart_Chat.utils.others.androidUtils

class BlockedUserAdapter(
    private val context: Context,
    private var blockedUsers: MutableList<userModel>,
    private val onUnblock: () -> Unit
) : RecyclerView.Adapter<BlockedUserAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.item_blocked_user, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val user = blockedUsers[position]
        holder.username.text = user.username

        if (!user.profileImage.isNullOrBlank()) {
            androidUtils.setProfileImageFromBase64(context, user.profileImage, holder.profileImage)
        } else {
            holder.profileImage.setImageResource(R.drawable.ic_profile)
        }

        holder.unblockButton.setOnClickListener {
            FirebaseAuthentication.unblockUser(user.userID ?: "") {
                Toast.makeText(context, "${user.username} unblocked", Toast.LENGTH_SHORT).show()
                onUnblock()
            }
        }
    }

    override fun getItemCount(): Int = blockedUsers.size

    fun updateData(newUsers: List<userModel>) {
        blockedUsers.clear()
        blockedUsers.addAll(newUsers)
        notifyDataSetChanged()
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val profileImage: ImageView = itemView.findViewById(R.id.profile_image)
        val username: TextView = itemView.findViewById(R.id.username)
        val unblockButton: Button = itemView.findViewById(R.id.unblock_btn)
    }
}