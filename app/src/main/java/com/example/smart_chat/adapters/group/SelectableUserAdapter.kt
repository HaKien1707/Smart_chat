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
    private val singleSelection: Boolean = false,
    private val onSelectionChanged: ((String, Boolean) -> Unit)? = null
) : RecyclerView.Adapter<SelectableUserAdapter.UserViewHolder>() {

    private val users = mutableListOf<userModel>()
    private val selectedIds = linkedSetOf<String>()
    private val disabledIds = linkedSetOf<String>()
    private var disabledSubtitle: String? = null

    fun submitUsers(
        newUsers: List<userModel>,
        selectedUserIds: Set<String> = emptySet(),
        disabledUserIds: Set<String> = emptySet(),
        disabledSubtitle: String? = null
    ) {
        users.clear()
        users.addAll(newUsers)
        selectedIds.clear()
        selectedIds.addAll(selectedUserIds)
        disabledIds.clear()
        disabledIds.addAll(disabledUserIds)
        this.disabledSubtitle = disabledSubtitle
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
        val isDisabled = userId.isNotBlank() && disabledIds.contains(userId)

        holder.title.text = user.username ?: ""
        holder.subtitle.text = if (isDisabled) {
            val phone = user.phoneNumber?.trim().orEmpty()
            val label = disabledSubtitle.orEmpty()
            when {
                phone.isNotBlank() && label.isNotBlank() -> "$phone • $label"
                phone.isNotBlank() -> phone
                else -> label
            }
        } else {
            user.phoneNumber ?: ""
        }

        if (!user.profileImage.isNullOrBlank()) {
            androidUtils.setProfileImageFromBase64(context, user.profileImage, holder.avatar)
        } else {
            holder.avatar.setImageResource(R.drawable.ic_profile)
        }

        holder.check.visibility = if (isSelected) View.VISIBLE else View.INVISIBLE

        holder.itemView.alpha = if (isDisabled) 0.6f else 1f

        if (!selectable) {
            holder.itemView.isClickable = false
            holder.itemView.isFocusable = false
            return
        }

        if (isDisabled) {
            holder.itemView.setOnClickListener(null)
            return
        }

        holder.itemView.setOnClickListener {
            if (userId.isBlank()) return@setOnClickListener

            // Toggle selection; if singleSelection is enabled, selecting a new user clears any previous selection.
            val wasSelected = selectedIds.contains(userId)
            if (wasSelected) {
                selectedIds.remove(userId)
                notifyItemChanged(position)
                onSelectionChanged?.invoke(userId, false)
                return@setOnClickListener
            }

            var previousSelectedId: String? = null
            if (singleSelection && selectedIds.isNotEmpty()) {
                previousSelectedId = selectedIds.firstOrNull()
                selectedIds.clear()
            }

            selectedIds.add(userId)

            if (previousSelectedId != null) {
                val prevPos = users.indexOfFirst { it.userID == previousSelectedId }
                if (prevPos >= 0) notifyItemChanged(prevPos)
            }

            notifyItemChanged(position)
            onSelectionChanged?.invoke(userId, true)
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
