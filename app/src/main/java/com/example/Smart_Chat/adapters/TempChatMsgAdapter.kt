package com.example.Smart_Chat.adapters

import android.content.Context
import android.content.Intent
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
import com.example.Smart_Chat.activities.others.FullScreenImageActivity
import com.example.Smart_Chat.models.DecryptedTempMessage
import com.example.Smart_Chat.utils.FileDownloadHelper
import com.example.Smart_Chat.utils.FireBase_utils
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

        if (message.senderID == FireBase_utils.currentUserID()) {
            // My message
            holder.sender.visibility = View.GONE
            holder.receiver.visibility = View.VISIBLE

            holder.receiverTimestamp.text = formatTimestamp(message.timestamp.toDate())

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

                    holder.receiverMsg.setOnClickListener {
                        FileDownloadHelper.showDownloadDialog(
                            context,
                            message.fileName ?: "File",
                            message.fileSize ?: 0,
                            message.fileUrl ?: ""
                        )
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
                    }
                }
                else -> {
                    holder.receiverImage.visibility = View.GONE
                    holder.receiverMsg.visibility = View.VISIBLE
                    holder.receiverMsg.text = message.message

                    holder.receiverMessageContainer.setBackgroundResource(R.drawable.input_box)
                    holder.receiverMessageContainer.backgroundTintList =
                        context.getColorStateList(R.color.violet)
                    val padding = context.resources.getDimensionPixelSize(R.dimen.message_padding)
                    holder.receiverMessageContainer.setPadding(padding, padding, padding, padding)
                }
            }

            // Hide read status for temporary chats
            holder.readStatusIcon.visibility = View.GONE

        } else {
            // Other user's message
            holder.sender.visibility = View.VISIBLE
            holder.receiver.visibility = View.GONE

            holder.senderTimestamp.text = formatTimestamp(message.timestamp.toDate())

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

                    holder.senderMsg.setOnClickListener {
                        FileDownloadHelper.showDownloadDialog(
                            context,
                            message.fileName ?: "File",
                            message.fileSize ?: 0,
                            message.fileUrl ?: ""
                        )
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
                    }
                }
                else -> {
                    holder.senderImage.visibility = View.GONE
                    holder.senderMsg.visibility = View.VISIBLE
                    holder.senderMsg.text = message.message

                    holder.senderMessageContainer.setBackgroundResource(R.drawable.input_box)
                    holder.senderMessageContainer.backgroundTintList =
                        context.getColorStateList(R.color.lime)
                    val padding = context.resources.getDimensionPixelSize(R.dimen.message_padding)
                    holder.senderMessageContainer.setPadding(padding, padding, padding, padding)
                }
            }
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
    }
}