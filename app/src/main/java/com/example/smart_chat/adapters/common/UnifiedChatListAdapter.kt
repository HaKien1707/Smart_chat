package com.example.smart_chat.adapters.common

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.smart_chat.R
import com.example.smart_chat.activities.community.CommunityChatActivity
import com.example.smart_chat.activities.group_chat.GroupChatActivity
import com.example.smart_chat.activities.user_chat.ChatActivity
import com.example.smart_chat.models.UserChatModel
import com.example.smart_chat.models.community.CommunityModel
import com.example.smart_chat.models.group.groupModel
import com.example.smart_chat.models.userModel
import com.example.smart_chat.utils.firebase.FirebaseAuthentication
import com.example.smart_chat.utils.firebase.FirebaseChat
import com.example.smart_chat.utils.others.androidUtils
import com.google.firebase.Timestamp
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

sealed class UnifiedChatItem {
    abstract val id: String
    abstract val sortTimestampMs: Long

    data class UserChat(
        override val id: String,
        override val sortTimestampMs: Long,
        val model: UserChatModel
    ) : UnifiedChatItem()

    data class Community(
        override val id: String,
        override val sortTimestampMs: Long,
        val model: CommunityModel
    ) : UnifiedChatItem()

    data class Group(
        override val id: String,
        override val sortTimestampMs: Long,
        val model: groupModel
    ) : UnifiedChatItem()
}

class UnifiedChatListAdapter(
    private val context: Context,
    private val onDeleteUserChat: ((chatRoomId: String) -> Unit)? = null,
) : ListAdapter<UnifiedChatItem, RecyclerView.ViewHolder>(DiffCallback) {

    private fun prefixPreview(prefix: String, message: String?): String {
        val cleanedPrefix = prefix.trimEnd()
        val cleanedMessage = message.orEmpty()
        return if (cleanedMessage.isBlank()) cleanedPrefix else "$cleanedPrefix $cleanedMessage"
    }

    private fun formatTimeOrDate(timestamp: Timestamp?): String {
        if (timestamp == null) return ""
        val date = timestamp.toDate()

        val now = Calendar.getInstance()
        val cal = Calendar.getInstance()
        cal.time = date

        val isSameDay = now.get(Calendar.YEAR) == cal.get(Calendar.YEAR)
                && now.get(Calendar.DAY_OF_YEAR) == cal.get(Calendar.DAY_OF_YEAR)

        return if (isSameDay) {
            SimpleDateFormat("HH:mm", Locale.getDefault()).format(date)
        } else {
            val locale = Locale.getDefault()
            if (locale.language.equals("vi", ignoreCase = true)) {
                // Vietnamese: day then month (e.g. 09 Thg 5)
                SimpleDateFormat("dd 'Thg' M", Locale("vi", "VN")).format(date)
            } else {
                // Telegram-like: month + day (e.g. Jan 03)
                SimpleDateFormat("MMM dd", locale).format(date)
            }
        }
    }

    companion object {
        private const val VT_USER_CHAT = 1
        private const val VT_COMMUNITY = 2
        private const val VT_GROUP = 3

        private val DiffCallback = object : DiffUtil.ItemCallback<UnifiedChatItem>() {
            override fun areItemsTheSame(oldItem: UnifiedChatItem, newItem: UnifiedChatItem): Boolean {
                return oldItem::class == newItem::class && oldItem.id == newItem.id
            }

            override fun areContentsTheSame(oldItem: UnifiedChatItem, newItem: UnifiedChatItem): Boolean {
                return oldItem == newItem
            }
        }
    }

    override fun getItemViewType(position: Int): Int {
        return when (getItem(position)) {
            is UnifiedChatItem.UserChat -> VT_USER_CHAT
            is UnifiedChatItem.Community -> VT_COMMUNITY
            is UnifiedChatItem.Group -> VT_GROUP
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            VT_USER_CHAT -> UserChatViewHolder(inflater.inflate(R.layout.item_recent_chat_recycler, parent, false))
            VT_COMMUNITY -> CommunityViewHolder(inflater.inflate(R.layout.item_community, parent, false))
            VT_GROUP -> GroupViewHolder(inflater.inflate(R.layout.item_group, parent, false))
            else -> error("Unsupported viewType: $viewType")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = getItem(position)) {
            is UnifiedChatItem.UserChat -> (holder as UserChatViewHolder).bind(item)
            is UnifiedChatItem.Community -> (holder as CommunityViewHolder).bind(item)
            is UnifiedChatItem.Group -> (holder as GroupViewHolder).bind(item)
        }
    }

    private inner class UserChatViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val profileImage: ImageView = itemView.findViewById(R.id.profile_image)
        private val username: TextView = itemView.findViewById(R.id.username)
        private val lastMsg: TextView = itemView.findViewById(R.id.lastMsg)
        private val lastMsgTime: TextView = itemView.findViewById(R.id.lastMsgTime)

        private val currentUserID = FirebaseAuthentication.currentUserID()

        fun bind(item: UnifiedChatItem.UserChat) {
            val model = item.model

            FirebaseChat.get2ndUserInChatRoom(model.userID)?.get()
                ?.addOnSuccessListener { documentSnapshot ->
                    val otherUser = documentSnapshot.toObject(userModel::class.java)
                    if (otherUser != null) {
                        username.text = otherUser.username

                        if (!otherUser.profileImage.isNullOrBlank()) {
                            androidUtils.setProfileImageFromBase64(context, otherUser.profileImage, profileImage)
                        } else {
                            profileImage.setImageResource(R.drawable.ic_profile)
                        }

                        val lastMessageText = if (model.lastMsgSenderID == currentUserID) {
                                prefixPreview(context.getString(R.string.you_prefix), model.lastMsg)
                        } else {
                            model.lastMsg ?: ""
                        }
                        lastMsg.text = lastMessageText
                        lastMsgTime.text = formatTimeOrDate(model.lastMsgTimestamp)

                        itemView.setOnClickListener {
                            val intent = Intent(context, ChatActivity::class.java)
                            androidUtils.passUserModelAsIntent(intent, otherUser)
                            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                            context.startActivity(intent)
                        }

                    }
                }
        }
    }

    private inner class CommunityViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val communityImage: ImageView = itemView.findViewById(R.id.community_image)
        private val communityName: TextView = itemView.findViewById(R.id.community_name)
        private val communityDescription: TextView = itemView.findViewById(R.id.community_description)
        private val communityTime: TextView = itemView.findViewById(R.id.community_time)

        fun bind(item: UnifiedChatItem.Community) {
            val model = item.model

            communityName.text = model.communityName ?: "Unknown Community"
            val currentUserId = FirebaseAuthentication.currentUserID()
            val preview = if (!model.lastMsg.isNullOrBlank()) {
                if (model.lastMsgSenderID == currentUserId) {
                    prefixPreview(context.getString(R.string.you_prefix), model.lastMsg)
                } else {
                    model.lastMsg
                }
            } else {
                model.communityDescription ?: ""
            }
            communityDescription.text = preview
            communityTime.text = formatTimeOrDate(model.lastMsgTimestamp)

            if (!model.communityImage.isNullOrBlank()) {
                androidUtils.setProfileImageFromBase64(context, model.communityImage, communityImage)
            } else {
                communityImage.setImageResource(R.drawable.ic_community)
            }

            itemView.setOnClickListener {
                val intent = Intent(context, CommunityChatActivity::class.java)
                intent.putExtra("communityID", model.communityID)
                intent.putExtra("communityName", model.communityName)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                context.startActivity(intent)
            }
        }
    }

    private inner class GroupViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val groupImage: ImageView = itemView.findViewById(R.id.group_image)
        private val groupNameText: TextView = itemView.findViewById(R.id.groupNameText)
        private val lastMsg: TextView = itemView.findViewById(R.id.lastMsg)
        private val lastMsgTime: TextView = itemView.findViewById(R.id.lastMsgTime)

        fun bind(item: UnifiedChatItem.Group) {
            val model = item.model

            groupNameText.text = model.groupName ?: "Unnamed Group"

            if (!model.groupImage.isNullOrEmpty()) {
                androidUtils.setProfileImageFromBase64(context, model.groupImage!!, groupImage)
            } else {
                groupImage.setImageResource(R.drawable.ic_group)
            }

            val currentUserId = FirebaseAuthentication.currentUserID()
            val msgText = if (model.lastMsg != null) {
                if (model.lastMsgSenderID == currentUserId) {
                    prefixPreview(context.getString(R.string.you_prefix), model.lastMsg)
                } else {
                    model.lastMsg
                }
            } else {
                ""
            }
            lastMsg.text = msgText
            lastMsgTime.text = formatTimeOrDate(model.lastMsgTimestamp)

            itemView.setOnClickListener {
                val intent = Intent(context, GroupChatActivity::class.java)
                intent.putExtra("groupID", model.groupID)
                intent.putExtra("groupName", model.groupName)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                context.startActivity(intent)
            }
        }
    }
}
