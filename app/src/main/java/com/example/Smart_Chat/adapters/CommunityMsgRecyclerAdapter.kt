package com.example.Smart_Chat.adapters

import android.content.Context
import android.content.Intent
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.Smart_Chat.R
import com.example.Smart_Chat.activities.others.ForwardMessageActivity
import com.example.Smart_Chat.activities.others.FullScreenImageActivity
import com.example.Smart_Chat.activities.community.CommunityChatActivity
import com.example.Smart_Chat.models.CommunityMsgModel
import com.example.Smart_Chat.models.ReplyMessageData
import com.example.Smart_Chat.models.userModel
import com.example.Smart_Chat.utils.FileDownloadHelper
import com.example.Smart_Chat.utils.FireBase_utils
import com.example.Smart_Chat.utils.FireBase_utils.currentUserID
import com.example.Smart_Chat.utils.MessageOptionsHelper
import com.example.Smart_Chat.utils.androidUtils
import com.firebase.ui.firestore.FirestoreRecyclerAdapter
import com.firebase.ui.firestore.FirestoreRecyclerOptions
import java.text.SimpleDateFormat
import java.util.*

class CommunityMsgRecyclerAdapter(
    options: FirestoreRecyclerOptions<CommunityMsgModel>,
    private val context: Context
) : FirestoreRecyclerAdapter<CommunityMsgModel, CommunityMsgRecyclerAdapter.CommunityMsgViewHolder>(options) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CommunityMsgViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_group_msg_row, parent, false)
        return CommunityMsgViewHolder(view)
    }

    override fun onBindViewHolder(holder: CommunityMsgViewHolder, position: Int, model: CommunityMsgModel) {
        // Handle bot and system messages
        if (model.senderID == "BOT" || model.senderID == "SYSTEM") {
            holder.sender.visibility = View.VISIBLE
            holder.receiver.visibility = View.GONE

            holder.senderTimestamp.text = formatTimestamp(model.timestamp?.toDate())

            // Hide profile image for bot messages
            holder.senderProfileImage.visibility = View.GONE
            holder.senderName.text = model.senderName ?: "🤖 Bot"

            holder.senderImage.visibility = View.GONE
            holder.senderMsg.visibility = View.VISIBLE

            // Different styling for system vs bot messages
            if (model.senderID == "SYSTEM") {
                holder.senderMsg.text = model.msg
                holder.senderMessageContainer.backgroundTintList =
                    context.getColorStateList(R.color.gray)
            } else {
                holder.senderMsg.text = model.msg
                holder.senderMessageContainer.backgroundTintList =
                    context.getColorStateList(R.color.cyan)
            }

            holder.senderMessageContainer.setBackgroundResource(R.drawable.input_box)
            val padding = context.resources.getDimensionPixelSize(R.dimen.message_padding)
            holder.senderMessageContainer.setPadding(padding, padding, padding, padding)

            if (model.senderID != "SYSTEM") {
                holder.sender.setOnLongClickListener {
                    showMessageOptions(holder.sender, position, model)
                    true
                }
            }

            return
        }

        val isMe = model.senderID == currentUserID()

        if (isMe) {
            // My message
            holder.sender.visibility = View.GONE
            holder.receiver.visibility = View.VISIBLE

            holder.receiverTimestamp.text = formatTimestamp(model.timestamp?.toDate())

            // NEW: Show replied message if exists
            if (!model.replyToMessageId.isNullOrEmpty()) {
                showRepliedMessage(
                    holder.receiverRepliedContainer,
                    holder.receiverRepliedText,
                    holder.receiverRepliedImage,
                    holder.receiverRepliedSenderName,
                    model
                )
            } else {
                holder.receiverRepliedContainer.visibility = View.GONE
            }

            if (model.isDeleted) {
                holder.receiverMsg.visibility = View.VISIBLE
                holder.receiverImage.visibility = View.GONE
                holder.receiverMsg.text = "🚫 This message was deleted"
                holder.receiverMsg.setTextColor(context.getColor(R.color.gray))
                holder.receiverMsg.setTypeface(null, android.graphics.Typeface.ITALIC)
                holder.receiverMessageContainer.setBackgroundResource(0)
            } else {
                when (model.messageType) {
                    "file" -> {
                        holder.receiverMsg.visibility = View.VISIBLE
                        holder.receiverImage.visibility = View.GONE

                        val fileName = model.fileName ?: "File"
                        val fileSize = formatFileSize(model.fileSize ?: 0)
                        holder.receiverMsg.text = "📎 $fileName\n$fileSize"
                        holder.receiverMsg.setTextColor(context.getColor(R.color.white))
                        holder.receiverMsg.setTypeface(null, android.graphics.Typeface.NORMAL)

                        holder.receiverMessageContainer.setBackgroundResource(R.drawable.input_box)
                        holder.receiverMessageContainer.backgroundTintList =
                            context.getColorStateList(R.color.violet)
                        val padding = context.resources.getDimensionPixelSize(R.dimen.message_padding)
                        holder.receiverMessageContainer.setPadding(padding, padding, padding, padding)

                        holder.receiverMsg.setOnClickListener(null)
                        holder.receiverMsg.setOnLongClickListener(null)

                        if (!model.isDeleted) {
                            // Enable download only if NOT deleted
                            holder.receiverMsg.setOnClickListener {
                                FileDownloadHelper.showDownloadDialog(
                                    context,
                                    model.fileName ?: "File",
                                    model.fileSize ?: 0,
                                    model.fileUrl ?: ""
                                )
                            }

                            holder.receiverMsg.setOnLongClickListener {
                                showMessageOptions(holder.receiverMsg, position, model)
                                true
                            }
                        }
                    }
                    "image" -> {
                        if (!model.imageUrl.isNullOrEmpty()) {
                            holder.receiverImage.visibility = View.VISIBLE
                            holder.receiverMsg.visibility = View.GONE
                            holder.receiverMessageContainer.setBackgroundResource(0)
                            holder.receiverMessageContainer.setPadding(0, 0, 0, 0)

                            Glide.with(context)
                                .load(model.imageUrl)
                                .placeholder(R.drawable.ic_image_loading)
                                .error(R.drawable.ic_image_error)
                                .into(holder.receiverImage)

                            holder.receiverImage.setOnClickListener {
                                val intent = Intent(context, FullScreenImageActivity::class.java)
                                intent.putExtra("imageUrl", model.imageUrl)
                                context.startActivity(intent)
                            }

                            holder.receiverImage.setOnLongClickListener {
                                showMessageOptions(holder.receiverImage, position, model)
                                true
                            }
                        }
                    }
                    else -> {
                        holder.receiverImage.visibility = View.GONE
                        holder.receiverMsg.visibility = View.VISIBLE
                        holder.receiverMsg.text = model.msg
                        holder.receiverMsg.setTextColor(context.getColor(R.color.white))
                        holder.receiverMsg.setTypeface(null, android.graphics.Typeface.NORMAL)

                        holder.receiverMessageContainer.setBackgroundResource(R.drawable.input_box)
                        holder.receiverMessageContainer.backgroundTintList =
                            context.getColorStateList(R.color.violet)
                        val padding = context.resources.getDimensionPixelSize(R.dimen.message_padding)
                        holder.receiverMessageContainer.setPadding(padding, padding, padding, padding)

                        holder.receiverMsg.setOnLongClickListener {
                            showMessageOptions(holder.receiver, position, model)
                            true
                        }
                    }
                }
            }

            holder.senderName.text = ""
            holder.senderMsg.text = ""
            holder.senderImage.visibility = View.GONE

        } else {
            // Other user's message
            holder.sender.visibility = View.VISIBLE
            holder.receiver.visibility = View.GONE

            holder.senderName.text = model.senderName ?: "Unknown"
            loadSenderProfileImage(holder, model.senderID)
            holder.senderTimestamp.text = formatTimestamp(model.timestamp?.toDate())

            // NEW: Show replied message if exists
            if (!model.replyToMessageId.isNullOrEmpty()) {
                showRepliedMessage(
                    holder.senderRepliedContainer,
                    holder.senderRepliedText,
                    holder.senderRepliedImage,
                    holder.senderRepliedSenderName,
                    model
                )
            } else {
                holder.senderRepliedContainer.visibility = View.GONE
            }

            if (model.isDeleted) {
                holder.senderMsg.visibility = View.VISIBLE
                holder.senderImage.visibility = View.GONE
                holder.senderMsg.text = "🚫 This message was deleted"
                holder.senderMsg.setTextColor(context.getColor(R.color.gray))
                holder.senderMsg.setTypeface(null, android.graphics.Typeface.ITALIC)
                holder.senderMessageContainer.setBackgroundResource(0)
            } else {
                when (model.messageType) {
                    "file" -> {
                        holder.senderMsg.visibility = View.VISIBLE
                        holder.senderImage.visibility = View.GONE

                        val fileName = model.fileName ?: "File"
                        val fileSize = formatFileSize(model.fileSize ?: 0)
                        holder.senderMsg.text = "📎 $fileName\n$fileSize"
                        holder.senderMsg.setTextColor(context.getColor(R.color.black))
                        holder.senderMsg.setTypeface(null, android.graphics.Typeface.NORMAL)

                        holder.senderMessageContainer.setBackgroundResource(R.drawable.input_box)
                        holder.senderMessageContainer.backgroundTintList =
                            context.getColorStateList(R.color.lime)
                        val padding = context.resources.getDimensionPixelSize(R.dimen.message_padding)
                        holder.senderMessageContainer.setPadding(padding, padding, padding, padding)

                        holder.senderMsg.setOnClickListener(null)
                        holder.senderMsg.setOnLongClickListener(null)

                        if (!model.isDeleted) {
                            holder.senderMsg.setOnClickListener {
                                FileDownloadHelper.showDownloadDialog(
                                    context,
                                    model.fileName ?: "File",
                                    model.fileSize ?: 0,
                                    model.fileUrl ?: ""
                                )
                            }

                            holder.senderMsg.setOnLongClickListener {
                                showMessageOptions(holder.senderMsg, position, model)
                                true
                            }
                        }

                    }
                    "image" -> {
                        if (!model.imageUrl.isNullOrEmpty()) {
                            holder.senderImage.visibility = View.VISIBLE
                            holder.senderMsg.visibility = View.GONE
                            holder.senderMessageContainer.setBackgroundResource(0)
                            holder.senderMessageContainer.setPadding(0, 0, 0, 0)

                            Glide.with(context)
                                .load(model.imageUrl)
                                .placeholder(R.drawable.ic_image_loading)
                                .error(R.drawable.ic_image_error)
                                .into(holder.senderImage)

                            holder.senderImage.setOnClickListener {
                                val intent = Intent(context, FullScreenImageActivity::class.java)
                                intent.putExtra("imageUrl", model.imageUrl)
                                context.startActivity(intent)
                            }
                            holder.senderImage.setOnLongClickListener {
                                showMessageOptions(holder.sender, position, model)
                                true
                            }
                        }
                    }
                    else -> {
                        holder.senderImage.visibility = View.GONE
                        holder.senderMsg.visibility = View.VISIBLE
                        holder.senderMsg.text = model.msg
                        holder.senderMsg.setTextColor(context.getColor(R.color.black))
                        holder.senderMsg.setTypeface(null, android.graphics.Typeface.NORMAL)

                        holder.senderMessageContainer.setBackgroundResource(R.drawable.input_box)
                        holder.senderMessageContainer.backgroundTintList =
                            context.getColorStateList(R.color.lime)
                        val padding = context.resources.getDimensionPixelSize(R.dimen.message_padding)
                        holder.senderMessageContainer.setPadding(padding, padding, padding, padding)
                        holder.senderMsg.setOnLongClickListener {
                            showMessageOptions(holder.sender, position, model)
                            true
                        }
                    }
                }
            }

            holder.receiverMsg.text = ""
            holder.receiverImage.visibility = View.GONE
        }
    }

    private fun showMessageOptions(view: View, position: Int, model: CommunityMsgModel) {
        val messageData = ReplyMessageData(
            messageId = snapshots.getSnapshot(position).id,
            text = model.msg,
            type = model.messageType ?: "text",
            imageUrl = model.imageUrl,
            fileName = model.fileName,
            fileSize = model.fileSize,
            senderName = model.senderName
        )

        MessageOptionsHelper.showMessageOptions(
            context = context,
            view = view,
            canDelete = model.senderID == currentUserID(),
            messageData = messageData,
            onReply = { replyData ->
                if (context is CommunityChatActivity) {
                    context.setReplyMessage(replyData)
                }
            },
            onForward = {
                openForwardActivity(model)
            },
            onDelete = {
                deleteMessage(position)
            }
        )
    }

    // NEW: Show replied message preview
    private fun showRepliedMessage(
        container: View,
        textView: TextView,
        imageView: ImageView,
        senderNameView: TextView,
        model: CommunityMsgModel
    ) {
        container.visibility = View.VISIBLE

        // Show sender name for community messages
        if (!model.replyToSenderName.isNullOrEmpty()) {
            senderNameView.visibility = View.VISIBLE
            senderNameView.text = model.replyToSenderName
        } else {
            senderNameView.visibility = View.GONE
        }

        when (model.replyToType) {
            "text" -> {
                textView.visibility = View.VISIBLE
                imageView.visibility = View.GONE
                textView.text = model.replyToText
            }
            "image" -> {
                textView.visibility = View.GONE
                imageView.visibility = View.VISIBLE
                Glide.with(context)
                    .load(model.replyToImageUrl)
                    .placeholder(R.drawable.ic_image_loading)
                    .into(imageView)
            }
            "file" -> {
                textView.visibility = View.VISIBLE
                imageView.visibility = View.GONE
                val fileName = model.replyToFileName ?: "File"
                val fileSize = formatFileSize(model.replyToFileSize ?: 0)
                textView.text = "📎 $fileName\n$fileSize"
            }
        }

        // Click to scroll to original message
        container.setOnClickListener {
            scrollToMessage(model.replyToMessageId)
        }
    }

    // NEW: Scroll to the replied message
    private fun scrollToMessage(messageId: String?) {
        if (messageId == null) return

        for (i in 0 until itemCount) {
            try {
                val snapshot = snapshots.getSnapshot(i)
                if (snapshot.id == messageId) {
                    if (context is CommunityChatActivity) {
                        context.scrollToPosition(i)
                    }
                    break
                }
            } catch (e: Exception) {
                Log.e("MSG_SCROLL", "Error finding message", e)
            }
        }
    }

    private fun openForwardActivity(model: CommunityMsgModel) {
        val intent = Intent(context, ForwardMessageActivity::class.java)
        intent.putExtra("messageText", model.msg)
        intent.putExtra("imageUrl", model.imageUrl)
        intent.putExtra("messageType", model.messageType)
        intent.putExtra("isFromGroup", true)

        if (context is CommunityChatActivity) {
            val communityID = context.getCommunityID()
            intent.putExtra("currentChatId", communityID)
        }

        context.startActivity(intent)
    }

    private fun deleteMessage(position: Int) {
        try {
            val snapshot = snapshots.getSnapshot(position)
            snapshot.reference.update("isDeleted", true)
                .addOnSuccessListener {
                    android.widget.Toast.makeText(context, "Message deleted", android.widget.Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener { e ->
                    Log.e("MSG_DELETE", "Failed to delete", e)
                    android.widget.Toast.makeText(context, "Failed to delete message", android.widget.Toast.LENGTH_SHORT).show()
                }
        } catch (e: Exception) {
            Log.e("MSG_DELETE", "Error deleting message", e)
        }
    }

    private fun loadSenderProfileImage(holder: CommunityMsgViewHolder, senderID: String?) {
        if (senderID.isNullOrEmpty()) {
            holder.senderProfileImage.setImageResource(R.drawable.ic_profile)
            return
        }

        FireBase_utils.allUsersCollection().document(senderID).get()
            .addOnSuccessListener { document ->
                val user = document.toObject(userModel::class.java)
                val profileImage = user?.profileImage

                if (!profileImage.isNullOrBlank()) {
                    androidUtils.setProfileImageFromBase64(
                        context,
                        profileImage,
                        holder.senderProfileImage
                    )
                } else {
                    holder.senderProfileImage.setImageResource(R.drawable.ic_profile)
                }
            }
            .addOnFailureListener { e ->
                Log.e("CommunityMsgAdapter", "Failed to load profile image", e)
                holder.senderProfileImage.setImageResource(R.drawable.ic_profile)
            }
    }

    private fun formatFileSize(size: Long): String {
        return when {
            size < 1024 -> "$size B"
            size < 1024 * 1024 -> "${size / 1024} KB"
            else -> String.format("%.2f MB", size / (1024.0 * 1024.0))
        }
    }

    private fun formatTimestamp(date: Date?): String {
        if (date == null) return ""

        val now = Calendar.getInstance()
        val messageTime = Calendar.getInstance().apply { time = date }

        return when {
            now.get(Calendar.DAY_OF_YEAR) == messageTime.get(Calendar.DAY_OF_YEAR) &&
                    now.get(Calendar.YEAR) == messageTime.get(Calendar.YEAR) -> {
                SimpleDateFormat("h:mm a", Locale.getDefault()).format(date)
            }
            now.get(Calendar.DAY_OF_YEAR) - messageTime.get(Calendar.DAY_OF_YEAR) == 1 &&
                    now.get(Calendar.YEAR) == messageTime.get(Calendar.YEAR) -> {
                "Yesterday ${SimpleDateFormat("h:mm a", Locale.getDefault()).format(date)}"
            }
            now.get(Calendar.YEAR) == messageTime.get(Calendar.YEAR) -> {
                SimpleDateFormat("MMM d, h:mm a", Locale.getDefault()).format(date)
            }
            else -> {
                SimpleDateFormat("MMM d, yyyy h:mm a", Locale.getDefault()).format(date)
            }
        }
    }

    class CommunityMsgViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val sender: LinearLayout = itemView.findViewById(R.id.sender)
        val receiver: LinearLayout = itemView.findViewById(R.id.receiver)
        val senderMessageContainer: FrameLayout = itemView.findViewById(R.id.senderMessageContainer)
        val receiverMessageContainer: FrameLayout = itemView.findViewById(R.id.receiverMessageContainer)
        val senderProfileImage: ImageView = itemView.findViewById(R.id.senderProfileImage)
        val senderName: TextView = itemView.findViewById(R.id.senderName)
        val senderMsg: TextView = itemView.findViewById(R.id.senderMsg)
        val receiverMsg: TextView = itemView.findViewById(R.id.receiverMsg)
        val senderImage: ImageView = itemView.findViewById(R.id.senderImage)
        val receiverImage: ImageView = itemView.findViewById(R.id.receiverImage)
        val senderTimestamp: TextView = itemView.findViewById(R.id.senderTimestamp)
        val receiverTimestamp: TextView = itemView.findViewById(R.id.receiverTimestamp)
        // NEW: Replied message views
        val senderRepliedContainer: View = itemView.findViewById(R.id.sender_replied_message)
        val senderRepliedText: TextView = senderRepliedContainer.findViewById(R.id.replied_text)
        val senderRepliedImage: ImageView = senderRepliedContainer.findViewById(R.id.replied_image)
        val senderRepliedSenderName: TextView = senderRepliedContainer.findViewById(R.id.replied_sender_name)

        val receiverRepliedContainer: View = itemView.findViewById(R.id.receiver_replied_message)
        val receiverRepliedText: TextView = receiverRepliedContainer.findViewById(R.id.replied_text)
        val receiverRepliedImage: ImageView = receiverRepliedContainer.findViewById(R.id.replied_image)
        val receiverRepliedSenderName: TextView = receiverRepliedContainer.findViewById(R.id.replied_sender_name)
    }
}