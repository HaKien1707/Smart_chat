package com.example.smart_chat.adapters.group

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

class SelectableUserAdapter(
    private val context: Context,
    private val selectable: Boolean,
    private val onSelectionChanged: ((String, Boolean) -> Unit)? = null
) : RecyclerView.Adapter<SelectableUserAdapter.UserViewHolder>() {

    private val users = mutableListOf<userModel>()
    private val selectedIds = linkedSetOf<String>()

    fun submitUsers(newUsers: List<userModel>, selectedUserIds: Set<String> = emptySet()) {
        users.clear()
        users.addAll(newUsers)
        selectedIds.clear()
        selectedIds.addAll(selectedUserIds)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_user_row_telegram, parent, false)
        return UserViewHolder(view)
    }

    override fun onBindViewHolder(holder: UserViewHolder, position: Int) {
        val user = users[position]
        val userId = user.userID.orEmpty()
        val isSelected = userId.isNotBlank() && selectedIds.contains(userId)

        holder.title.text = user.username ?: ""
        holder.subtitle.text = user.phoneNumber ?: ""

        if (!user.profileImage.isNullOrBlank()) {
            androidUtils.setProfileImageFromBase64(context, user.profileImage, holder.avatar)
        } else {
            holder.avatar.setImageResource(R.drawable.ic_profile)
        }

        holder.check.visibility = if (isSelected) View.VISIBLE else View.INVISIBLE

        if (!selectable) {
            holder.itemView.isClickable = false
            holder.itemView.isFocusable = false
            return
        }

        holder.itemView.setOnClickListener {
            if (userId.isBlank()) return@setOnClickListener
            val newSelected = if (selectedIds.contains(userId)) {
                selectedIds.remove(userId)
                false
            } else {
                selectedIds.add(userId)
                true
            }
            notifyItemChanged(position)
            onSelectionChanged?.invoke(userId, newSelected)
        }
    }

    override fun getItemCount(): Int = users.size

    class UserViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val avatar: ImageView = itemView.findViewById(R.id.avatar)
        val title: TextView = itemView.findViewById(R.id.title)
        val subtitle: TextView = itemView.findViewById(R.id.subtitle)
        val check: ImageView = itemView.findViewById(R.id.selected_icon)
    }
}
