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
import com.example.Smart_Chat.activities.user_chat.chatActivity
import com.example.Smart_Chat.models.MsgModel
import com.example.Smart_Chat.utils.FireBase_utils.currentUserID
import com.firebase.ui.firestore.FirestoreRecyclerAdapter
import com.firebase.ui.firestore.FirestoreRecyclerOptions
import com.google.firebase.Timestamp
import java.text.SimpleDateFormat
import java.util.*

class MsgRecyclerAdapter(
    options: FirestoreRecyclerOptions<MsgModel>,
    private val context: Context
) : FirestoreRecyclerAdapter<MsgModel, MsgRecyclerAdapter.MsgViewHolder>(options) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MsgViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_msg_row, parent, false)
        return MsgViewHolder(view)
    }

    override fun onBindViewHolder(holder: MsgViewHolder, position: Int, model: MsgModel) {
        if (model.senderID == currentUserID()) {
            // My message → show on right (receiver side)
            holder.sender.visibility = View.GONE
            holder.receiver.visibility = View.VISIBLE

            // Format and show timestamp
            holder.receiverTimestamp.text = formatTimestamp(model.timestamp?.toDate())

            // Check if deleted
            if (model.isDeleted) {
                holder.receiverMsg.visibility = View.VISIBLE
                holder.receiverImage.visibility = View.GONE
                holder.receiverMsg.text = "🚫 This message was deleted"
                holder.receiverMsg.setTextColor(context.getColor(R.color.gray))
                holder.receiverMsg.setTypeface(null, android.graphics.Typeface.ITALIC)

                // Remove bubble background for deleted messages
                holder.receiverMessageContainer.setBackgroundResource(0)

                // Hide read status for deleted messages
                holder.readStatusIcon.visibility = View.GONE
            } else {
                // Update read status icon
                holder.readStatusIcon.visibility = View.VISIBLE
                if (model.isRead) {
                    holder.readStatusIcon.setImageResource(R.drawable.ic_message_read)
                } else {
                    holder.readStatusIcon.setImageResource(R.drawable.ic_message_sent)
                }

                when (model.messageType) {
                    "file" -> {
                        // Show file message
                        holder.receiverMsg.visibility = View.VISIBLE
                        holder.receiverImage.visibility = View.GONE

                        // Format file info
                        val fileName = model.fileName ?: "File"
                        val fileSize = formatFileSize(model.fileSize ?: 0)
                        holder.receiverMsg.text = "📎 $fileName\n$fileSize"
                        holder.receiverMsg.setTextColor(context.getColor(R.color.white))
                        holder.receiverMsg.setTypeface(null, android.graphics.Typeface.NORMAL)

                        // Restore background and padding for file messages
                        holder.receiverMessageContainer.setBackgroundResource(R.drawable.input_box)
                        holder.receiverMessageContainer.backgroundTintList =
                            context.getColorStateList(R.color.violet)
                        val padding = context.resources.getDimensionPixelSize(R.dimen.message_padding)
                        holder.receiverMessageContainer.setPadding(padding, padding, padding, padding)

                        // Make it clickable to open/download
                        holder.receiverMsg.setOnClickListener {
                            val intent = Intent(Intent.ACTION_VIEW)
                            intent.data = android.net.Uri.parse(model.fileUrl)
                            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                            try {
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                android.widget.Toast.makeText(
                                    context,
                                    "No app found to open this file",
                                    android.widget.Toast.LENGTH_SHORT
                                ).show()
                            }
                        }

                        // Long press options
                        holder.receiver.setOnLongClickListener {
                            showMessageOptions(holder.receiver, position, model, isGroup = false)
                            true
                        }
                    }
                    "image" -> {
                        if (!model.imageUrl.isNullOrEmpty()) {
                            // Show image - REMOVE BUBBLE BACKGROUND
                            holder.receiverImage.visibility = View.VISIBLE
                            holder.receiverMsg.visibility = View.GONE

                            // Make container transparent for images
                            holder.receiverMessageContainer.setBackgroundResource(0)
                            holder.receiverMessageContainer.setPadding(0, 0, 0, 0)

                            Glide.with(context)
                                .load(model.imageUrl)
                                .placeholder(R.drawable.ic_image_loading)
                                .error(R.drawable.ic_image_error)
                                .into(holder.receiverImage)

                            // Click to open full screen
                            holder.receiverImage.setOnClickListener {
                                val intent = Intent(context, FullScreenImageActivity::class.java)
                                intent.putExtra("imageUrl", model.imageUrl)
                                context.startActivity(intent)
                            }

                            // Long press to show options
                            holder.receiverImage.setOnLongClickListener {
                                showMessageOptions(holder.receiverImage, position, model, isGroup = false)
                                true
                            }
                        }
                    }
                    else -> {
                        // Show text - KEEP BUBBLE BACKGROUND
                        holder.receiverImage.visibility = View.GONE
                        holder.receiverMsg.visibility = View.VISIBLE
                        holder.receiverMsg.text = model.msg
                        holder.receiverMsg.setTextColor(context.getColor(R.color.white))
                        holder.receiverMsg.setTypeface(null, android.graphics.Typeface.NORMAL)

                        // Restore background and padding for text messages
                        holder.receiverMessageContainer.setBackgroundResource(R.drawable.input_box)
                        holder.receiverMessageContainer.backgroundTintList =
                            context.getColorStateList(R.color.violet)
                        val padding = context.resources.getDimensionPixelSize(R.dimen.message_padding)
                        holder.receiverMessageContainer.setPadding(padding, padding, padding, padding)

                        // Add long click listener for text message options
                        holder.receiver.setOnLongClickListener {
                            showMessageOptions(holder.receiver, position, model, isGroup = false)
                            true
                        }
                    }
                }
            }
        } else {
            // Other user's message → show on left (sender side)
            holder.sender.visibility = View.VISIBLE
            holder.receiver.visibility = View.GONE

            // Format and show timestamp
            holder.senderTimestamp.text = formatTimestamp(model.timestamp?.toDate())

            // Check if deleted
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
                        // Show file message
                        holder.senderMsg.visibility = View.VISIBLE
                        holder.senderImage.visibility = View.GONE

                        // Format file info
                        val fileName = model.fileName ?: "File"
                        val fileSize = formatFileSize(model.fileSize ?: 0)
                        holder.senderMsg.text = "📎 $fileName\n$fileSize"
                        holder.senderMsg.setTextColor(context.getColor(R.color.black))
                        holder.senderMsg.setTypeface(null, android.graphics.Typeface.NORMAL)

                        // Restore background and padding for file messages
                        holder.senderMessageContainer.setBackgroundResource(R.drawable.input_box)
                        holder.senderMessageContainer.backgroundTintList =
                            context.getColorStateList(R.color.lime)
                        val padding = context.resources.getDimensionPixelSize(R.dimen.message_padding)
                        holder.senderMessageContainer.setPadding(padding, padding, padding, padding)

                        // Make it clickable to open/download
                        holder.senderMsg.setOnClickListener {
                            val intent = Intent(Intent.ACTION_VIEW)
                            intent.data = android.net.Uri.parse(model.fileUrl)
                            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                            try {
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                android.widget.Toast.makeText(
                                    context,
                                    "No app found to open this file",
                                    android.widget.Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    }
                    "image" -> {
                        if (!model.imageUrl.isNullOrEmpty()) {
                            // Show image - REMOVE BUBBLE BACKGROUND
                            holder.senderImage.visibility = View.VISIBLE
                            holder.senderMsg.visibility = View.GONE

                            // Make container transparent for images
                            holder.senderMessageContainer.setBackgroundResource(0)
                            holder.senderMessageContainer.setPadding(0, 0, 0, 0)

                            Glide.with(context)
                                .load(model.imageUrl)
                                .placeholder(R.drawable.ic_image_loading)
                                .error(R.drawable.ic_image_error)
                                .into(holder.senderImage)

                            // Click to open full screen
                            holder.senderImage.setOnClickListener {
                                val intent = Intent(context, FullScreenImageActivity::class.java)
                                intent.putExtra("imageUrl", model.imageUrl)
                                context.startActivity(intent)
                            }
                        }
                    }
                    else -> {
                        // Show text - KEEP BUBBLE BACKGROUND
                        holder.senderImage.visibility = View.GONE
                        holder.senderMsg.visibility = View.VISIBLE
                        holder.senderMsg.text = model.msg
                        holder.senderMsg.setTextColor(context.getColor(R.color.black))
                        holder.senderMsg.setTypeface(null, android.graphics.Typeface.NORMAL)

                        // Restore background and padding for text messages
                        holder.senderMessageContainer.setBackgroundResource(R.drawable.input_box)
                        holder.senderMessageContainer.backgroundTintList =
                            context.getColorStateList(R.color.lime)
                        val padding = context.resources.getDimensionPixelSize(R.dimen.message_padding)
                        holder.senderMessageContainer.setPadding(padding, padding, padding, padding)
                    }
                }

                // Mark message as read if it's from the other user and not already read
                if (!model.isRead) {
                    markMessageAsRead(position)
                }
            }
        }
    }

    // ✅ Add this helper function to the adapter
    private fun formatFileSize(size: Long): String {
        return when {
            size < 1024 -> "$size B"
            size < 1024 * 1024 -> "${size / 1024} KB"
            else -> String.format("%.2f MB", size / (1024.0 * 1024.0))
        }
    }

    private fun showMessageOptions(view: View, position: Int, model: MsgModel, isGroup: Boolean) {
        val popupView = LayoutInflater.from(context).inflate(R.layout.popup_message_options, null)
        val popupWindow = android.widget.PopupWindow(
            popupView,
            android.widget.LinearLayout.LayoutParams.WRAP_CONTENT,
            android.widget.LinearLayout.LayoutParams.WRAP_CONTENT,
            true
        )

        val optionForward = popupView.findViewById<TextView>(R.id.option_forward)
        val optionDelete = popupView.findViewById<TextView>(R.id.option_delete)

        optionForward.setOnClickListener {
            popupWindow.dismiss()
            openForwardActivity(model, isGroup)
        }

        optionDelete.setOnClickListener {
            popupWindow.dismiss()
            deleteMessage(position, model)
        }

        popupWindow.showAsDropDown(view, 0, -view.height)
    }

    private fun openForwardActivity(model: MsgModel, isGroup: Boolean) {
        val intent = Intent(context, ForwardMessageActivity::class.java)
        intent.putExtra("messageText", model.msg)
        intent.putExtra("imageUrl", model.imageUrl)
        intent.putExtra("messageType", model.messageType)
        intent.putExtra("isFromGroup", isGroup)

        // We need to get the chatRoomID from the activity
        if (context is chatActivity) {
            // Get chatRoomID from chatActivity
            val chatRoomID = context.getChatRoomID()
            intent.putExtra("currentChatId", chatRoomID)
        }

        context.startActivity(intent)
    }

    private fun deleteMessage(position: Int, model: MsgModel) {
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

    private fun markMessageAsRead(position: Int) {
        try {
            val snapshot = snapshots.getSnapshot(position)
            snapshot.reference.update(
                mapOf(
                    "isRead" to true,
                    "readTimestamp" to Timestamp.now()
                )
            ).addOnSuccessListener {
                Log.d("MSG_READ", "Message marked as read")
            }.addOnFailureListener { e ->
                Log.e("MSG_READ", "Failed to mark as read", e)
            }
        } catch (e: Exception) {
            Log.e("MSG_READ", "Error marking message as read", e)
        }
    }

    private fun formatTimestamp(date: Date?): String {
        if (date == null) return ""

        val now = Calendar.getInstance()
        val messageTime = Calendar.getInstance().apply { time = date }

        return when {
            // Today - show time only
            now.get(Calendar.DAY_OF_YEAR) == messageTime.get(Calendar.DAY_OF_YEAR) &&
                    now.get(Calendar.YEAR) == messageTime.get(Calendar.YEAR) -> {
                SimpleDateFormat("h:mm a", Locale.getDefault()).format(date)
            }
            // Yesterday
            now.get(Calendar.DAY_OF_YEAR) - messageTime.get(Calendar.DAY_OF_YEAR) == 1 &&
                    now.get(Calendar.YEAR) == messageTime.get(Calendar.YEAR) -> {
                "Yesterday ${SimpleDateFormat("h:mm a", Locale.getDefault()).format(date)}"
            }
            // This year - show date without year
            now.get(Calendar.YEAR) == messageTime.get(Calendar.YEAR) -> {
                SimpleDateFormat("MMM d, h:mm a", Locale.getDefault()).format(date)
            }
            // Different year - show full date
            else -> {
                SimpleDateFormat("MMM d, yyyy h:mm a", Locale.getDefault()).format(date)
            }
        }
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
    }
}