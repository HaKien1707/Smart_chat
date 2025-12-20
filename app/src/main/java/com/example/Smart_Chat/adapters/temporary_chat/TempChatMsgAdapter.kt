package com.example.Smart_Chat.adapters.temporary_chat

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
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.Smart_Chat.R
import com.example.Smart_Chat.activities.others.FullScreenImageActivity
import com.example.Smart_Chat.activities.temporary_chat.TemporaryChatActivity
import com.example.Smart_Chat.models.temp_chat.DecryptedTempMessage
import com.example.Smart_Chat.models.msg_action.ReplyMessageData
import com.example.Smart_Chat.utils.media.FileDownloadHelper
import com.example.Smart_Chat.utils.others.MessageOptionsHelper
import com.example.Smart_Chat.utils.firebase.FirebaseAuthentication
import java.text.SimpleDateFormat
import java.util.*

class TempChatMsgAdapter(
    private val messages: List<DecryptedTempMessage>,
    private val context: Context
) : RecyclerView.Adapter<TempChatMsgAdapter.MsgViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MsgViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_msg_row, parent, false)
        return MsgViewHolder(view)
    }

    override fun onBindViewHolder(holder: MsgViewHolder, position: Int) {
        val message = messages[position]

        if (message.senderID == FirebaseAuthentication.currentUserID()) {
            // My message
            holder.sender.visibility = View.GONE
            holder.receiver.visibility = View.VISIBLE

            holder.receiverTimestamp.text = formatTimestamp(message.timestamp?.toDate())
            // Show replied message if exists
            if (!message.replyToMessageId.isNullOrEmpty()) {
                showRepliedMessage(
                    holder.receiverRepliedContainer,
                    holder.receiverRepliedText,
                    holder.receiverRepliedImage,
                    holder.receiverRepliedSenderName,
                    message
                )
            } else {
                holder.receiverRepliedContainer.visibility = View.GONE
            }

            when (message.messageType) {
                "file" -> {
                    holder.receiverMsg.visibility = View.VISIBLE
                    holder.receiverImage.visibility = View.GONE

                    val fileName = message.fileName ?: "File"
                    val fileSize = formatFileSize(message.fileSize ?: 0)
                    holder.receiverMsg.text = "📎 $fileName\n$fileSize"

                    holder.receiverMessageContainer.setBackgroundResource(R.drawable.input_box)
                    holder.receiverMessageContainer.backgroundTintList =
                        context.getColorStateList(R.color.violet)
                    val padding = context.resources.getDimensionPixelSize(R.dimen.message_padding)
                    holder.receiverMessageContainer.setPadding(padding, padding, padding, padding)

                    holder.receiverMsg.setOnClickListener(null)
                    holder.receiverMsg.setOnLongClickListener(null)

                    if (!message.isDeleted) {
                        // Enable download only if NOT deleted
                        holder.receiverMsg.setOnClickListener {
                            FileDownloadHelper.showDownloadDialog(
                                context,
                                message.fileName ?: "File",
                                message.fileSize ?: 0,
                                message.fileUrl ?: ""
                            )
                        }

                        holder.receiverMsg.setOnLongClickListener {
                            showMessageOptions(holder.receiverMsg, position, message)
                            true
                        }
                    }
                }
                "image" -> {
                    if (!message.imageUrl.isNullOrEmpty()) {
                        holder.receiverImage.visibility = View.VISIBLE
                        holder.receiverMsg.visibility = View.GONE

                        holder.receiverMessageContainer.setBackgroundResource(0)
                        holder.receiverMessageContainer.setPadding(0, 0, 0, 0)

                        Glide.with(context)
                            .load(message.imageUrl)
                            .placeholder(R.drawable.ic_image_loading)
                            .error(R.drawable.ic_image_error)
                            .into(holder.receiverImage)

                        holder.receiverImage.setOnClickListener {
                            val intent = Intent(context, FullScreenImageActivity::class.java)
                            intent.putExtra("imageUrl", message.imageUrl)
                            context.startActivity(intent)
                        }

                        holder.receiverImage.setOnLongClickListener {
                            showMessageOptions(holder.receiverImage, position, message)
                            true
                        }
                    }
                }
                else -> {
                    holder.receiverImage.visibility = View.GONE
                    holder.receiverMsg.visibility = View.VISIBLE
                    holder.receiverMsg.text = message.msg

                    holder.receiverMessageContainer.setBackgroundResource(R.drawable.input_box)
                    holder.receiverMessageContainer.backgroundTintList =
                        context.getColorStateList(R.color.violet)
                    val padding = context.resources.getDimensionPixelSize(R.dimen.message_padding)
                    holder.receiverMessageContainer.setPadding(padding, padding, padding, padding)

                    holder.receiver.setOnLongClickListener {
                        showMessageOptions(holder.receiver, position, message)
                        true
                    }
                }
            }

            // Hide read status for temporary chats
            holder.readStatusIcon.visibility = View.GONE

        } else {
            // Other user's message
            holder.sender.visibility = View.VISIBLE
            holder.receiver.visibility = View.GONE

            holder.senderTimestamp.text = formatTimestamp(message.timestamp?.toDate())

            // Show replied message if exists
            if (!message.replyToMessageId.isNullOrEmpty()) {
                showRepliedMessage(
                    holder.senderRepliedContainer,
                    holder.senderRepliedText,
                    holder.senderRepliedImage,
                    holder.senderRepliedSenderName,
                    message
                )
            } else {
                holder.senderRepliedContainer.visibility = View.GONE
            }

            when (message.messageType) {
                "file" -> {
                    holder.senderMsg.visibility = View.VISIBLE
                    holder.senderImage.visibility = View.GONE

                    val fileName = message.fileName ?: "File"
                    val fileSize = formatFileSize(message.fileSize ?: 0)
                    holder.senderMsg.text = "📎 $fileName\n$fileSize"

                    holder.senderMessageContainer.setBackgroundResource(R.drawable.input_box)
                    holder.senderMessageContainer.backgroundTintList =
                        context.getColorStateList(R.color.lime)
                    val padding = context.resources.getDimensionPixelSize(R.dimen.message_padding)
                    holder.senderMessageContainer.setPadding(padding, padding, padding, padding)

                    holder.senderMsg.setOnClickListener(null)
                    holder.senderMsg.setOnLongClickListener(null)

                    if (!message.isDeleted) {
                        holder.senderMsg.setOnClickListener {
                            FileDownloadHelper.showDownloadDialog(
                                context,
                                message.fileName ?: "File",
                                message.fileSize ?: 0,
                                message.fileUrl ?: ""
                            )
                        }

                        holder.senderMsg.setOnLongClickListener {
                            showMessageOptions(holder.senderMsg, position, message)
                            true
                        }
                    }

                }
                "image" -> {
                    if (!message.imageUrl.isNullOrEmpty()) {
                        holder.senderImage.visibility = View.VISIBLE
                        holder.senderMsg.visibility = View.GONE

                        holder.senderMessageContainer.setBackgroundResource(0)
                        holder.senderMessageContainer.setPadding(0, 0, 0, 0)

                        Glide.with(context)
                            .load(message.imageUrl)
                            .placeholder(R.drawable.ic_image_loading)
                            .error(R.drawable.ic_image_error)
                            .into(holder.senderImage)

                        holder.senderImage.setOnClickListener {
                            val intent = Intent(context, FullScreenImageActivity::class.java)
                            intent.putExtra("imageUrl", message.imageUrl)
                            context.startActivity(intent)
                        }

                        holder.sender.setOnLongClickListener {
                            showMessageOptions(holder.sender, position, message)
                            true
                        }
                    }
                }
                else -> {
                    holder.senderImage.visibility = View.GONE
                    holder.senderMsg.visibility = View.VISIBLE
                    holder.senderMsg.text = message.msg
                    holder.senderMessageContainer.setBackgroundResource(R.drawable.input_box)
                    holder.senderMessageContainer.backgroundTintList =
                        context.getColorStateList(R.color.lime)
                    val padding = context.resources.getDimensionPixelSize(R.dimen.message_padding)
                    holder.senderMessageContainer.setPadding(padding, padding, padding, padding)
                    holder.sender.setOnLongClickListener {
                        showMessageOptions(holder.sender, position, message)
                        true
                    }
                }
            }
        }
    }

    private fun showMessageOptions(view: View, position: Int, message: DecryptedTempMessage) {
        val messageData = ReplyMessageData(
            messageId = position.toString(), // Use position as ID since temp messages don't have firestore IDs
            text = message.msg,
            type = message.messageType ?: "text",
            imageUrl = message.imageUrl,
            fileName = message.fileName,
            fileSize = message.fileSize,
            senderName = null // No sender name for 1-on-1 temp chat
        )

        MessageOptionsHelper.showMessageOptions(
            context = context,
            view = view,
            canDelete = message.senderID == FirebaseAuthentication.currentUserID(),
            messageData = messageData,
            onReply = { replyData ->
                if (context is TemporaryChatActivity) {
                    context.setReplyMessage(replyData)
                }
            },
            onForward = {
                Toast.makeText(
                    context,
                    "Cannot forward temporary messages for security reasons",
                    Toast.LENGTH_SHORT
                ).show()
            },
            onDelete = {
                // Temp chat messages can't be deleted individually
                Toast.makeText(context, "Temporary messages are auto-deleted", Toast.LENGTH_SHORT).show()
            }
        )
    }

    // Show replied message preview
    private fun showRepliedMessage(
        container: View,
        textView: TextView,
        imageView: ImageView,
        senderNameView: TextView,
        message: DecryptedTempMessage
    ) {
        container.visibility = View.VISIBLE

        // Hide sender name for 1-on-1 temp chat
        senderNameView.visibility = View.GONE

        when (message.replyToType) {
            "text" -> {
                textView.visibility = View.VISIBLE
                imageView.visibility = View.GONE
                textView.text = message.replyToText
            }
            "image" -> {
                textView.visibility = View.GONE
                imageView.visibility = View.VISIBLE
                Glide.with(context)
                    .load(message.replyToImageUrl)
                    .placeholder(R.drawable.ic_image_loading)
                    .into(imageView)
            }
            "file" -> {
                textView.visibility = View.VISIBLE
                imageView.visibility = View.GONE
                val fileName = message.replyToFileName ?: "File"
                val fileSize = formatFileSize(message.replyToFileSize ?: 0)
                textView.text = "📎 $fileName\n$fileSize"
            }
        }

        // Click to scroll to original message
        container.setOnClickListener {
            scrollToMessage(message.replyToMessageId)
        }
    }

    // Scroll to the replied message
    private fun scrollToMessage(messageId: String?) {
        if (messageId == null) return

        try {
            val position = messageId.toIntOrNull()
            if (position != null && position >= 0 && position < messages.size) {
                if (context is TemporaryChatActivity) {
                    context.scrollToPosition(position)
                }
            }
        } catch (e: Exception) {
            Log.e("MSG_SCROLL", "Error finding message", e)
        }
    }

    override fun getItemCount(): Int = messages.size

    private fun formatFileSize(size: Long): String {
        return when {
            size < 1024 -> "$size B"
            size < 1024 * 1024 -> "${size / 1024} KB"
            else -> String.format("%.2f MB", size / (1024.0 * 1024.0))
        }
    }

    private fun formatTimestamp(date: Date?): String {
        if (date == null) return ""
        return SimpleDateFormat("h:mm a", Locale.getDefault()).format(date)
    }

    class MsgViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val sender: LinearLayout = itemView.findViewById(R.id.sender)
        val receiver: LinearLayout = itemView.findViewById(R.id.receiver)
        val senderMessageContainer: FrameLayout = itemView.findViewById(R.id.senderMessageContainer)
        val receiverMessageContainer: FrameLayout = itemView.findViewById(R.id.receiverMessageContainer)
        val senderMsg: TextView = itemView.findViewById(R.id.senderMsg)
        val receiverMsg: TextView = itemView.findViewById(R.id.receiverMsg)
        val senderImage: ImageView = itemView.findViewById(R.id.senderImage)
        val receiverImage: ImageView = itemView.findViewById(R.id.receiverImage)
        val senderTimestamp: TextView = itemView.findViewById(R.id.senderTimestamp)
        val receiverTimestamp: TextView = itemView.findViewById(R.id.receiverTimestamp)
        val readStatusIcon: ImageView = itemView.findViewById(R.id.readStatusIcon)
        // Replied message views
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