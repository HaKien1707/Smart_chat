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
import com.example.Smart_Chat.activities.FullScreenImageActivity
import com.example.Smart_Chat.models.GroupMsgModel
import com.example.Smart_Chat.utils.FireBase_utils.currentUserID
import com.firebase.ui.firestore.FirestoreRecyclerAdapter
import com.firebase.ui.firestore.FirestoreRecyclerOptions
import java.text.SimpleDateFormat
import java.util.*

class GroupMsgRecyclerAdapter(
    options: FirestoreRecyclerOptions<GroupMsgModel>,
    private val context: Context
) : FirestoreRecyclerAdapter<GroupMsgModel, GroupMsgRecyclerAdapter.GroupMsgViewHolder>(options) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GroupMsgViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.group_msg_row_item, parent, false)
        return GroupMsgViewHolder(view)
    }

    override fun onBindViewHolder(holder: GroupMsgViewHolder, position: Int, model: GroupMsgModel) {

        val isMe = model.senderID == currentUserID()

        if (isMe) {
            // My message → show on right (receiver side)
            holder.sender.visibility = View.GONE
            holder.receiver.visibility = View.VISIBLE

            // Format and show timestamp
            holder.receiverTimestamp.text = formatTimestamp(model.timestamp?.toDate())

            if (model.messageType == "image" && !model.imageUrl.isNullOrEmpty()) {
                // Show image - REMOVE BUBBLE BACKGROUND
                holder.receiverImage.visibility = View.VISIBLE
                holder.receiverMsg.visibility = View.GONE

                // Make container transparent for images
                holder.receiverMessageContainer.setBackgroundResource(0) // Remove background
                holder.receiverMessageContainer.setPadding(0, 0, 0, 0) // Remove padding

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
            } else {
                // Show text - KEEP BUBBLE BACKGROUND
                holder.receiverImage.visibility = View.GONE
                holder.receiverMsg.visibility = View.VISIBLE
                holder.receiverMsg.text = model.msg

                // Restore background and padding for text messages
                holder.receiverMessageContainer.setBackgroundResource(R.drawable.input_box)
                holder.receiverMessageContainer.backgroundTintList =
                    context.getColorStateList(R.color.violet)
                val padding = context.resources.getDimensionPixelSize(R.dimen.message_padding) // 10dp
                holder.receiverMessageContainer.setPadding(padding, padding, padding, padding)
            }

            // Clear sender fields (avoid recycled trash)
            holder.senderName.text = ""
            holder.senderMsg.text = ""
            holder.senderImage.visibility = View.GONE

        } else {
            // Other user's message → show on left (sender side)
            holder.sender.visibility = View.VISIBLE
            holder.receiver.visibility = View.GONE

            // Show sender name
            holder.senderName.text = model.senderName ?: "Unknown"

            // Format and show timestamp
            holder.senderTimestamp.text = formatTimestamp(model.timestamp?.toDate())

            if (model.messageType == "image" && !model.imageUrl.isNullOrEmpty()) {
                // Show image - REMOVE BUBBLE BACKGROUND
                holder.senderImage.visibility = View.VISIBLE
                holder.senderMsg.visibility = View.GONE

                // Make container transparent for images
                holder.senderMessageContainer.setBackgroundResource(0) // Remove background
                holder.senderMessageContainer.setPadding(0, 0, 0, 0) // Remove padding

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
            } else {
                // Show text - KEEP BUBBLE BACKGROUND
                holder.senderImage.visibility = View.GONE
                holder.senderMsg.visibility = View.VISIBLE
                holder.senderMsg.text = model.msg

                // Restore background and padding for text messages
                holder.senderMessageContainer.setBackgroundResource(R.drawable.input_box)
                holder.senderMessageContainer.backgroundTintList =
                    context.getColorStateList(R.color.lime)
                val padding = context.resources.getDimensionPixelSize(R.dimen.message_padding) // 10dp
                holder.senderMessageContainer.setPadding(padding, padding, padding, padding)
            }

            // Clear receiver fields (avoid recycled trash)
            holder.receiverMsg.text = ""
            holder.receiverImage.visibility = View.GONE
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

    class GroupMsgViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val sender: LinearLayout = itemView.findViewById(R.id.sender)
        val receiver: LinearLayout = itemView.findViewById(R.id.receiver)
        val senderMessageContainer: FrameLayout = itemView.findViewById(R.id.senderMessageContainer)
        val receiverMessageContainer: FrameLayout = itemView.findViewById(R.id.receiverMessageContainer)
        val senderName: TextView = itemView.findViewById(R.id.senderName)
        val senderMsg: TextView = itemView.findViewById(R.id.senderMsg)
        val receiverMsg: TextView = itemView.findViewById(R.id.receiverMsg)
        val senderImage: ImageView = itemView.findViewById(R.id.senderImage)
        val receiverImage: ImageView = itemView.findViewById(R.id.receiverImage)
        val senderTimestamp: TextView = itemView.findViewById(R.id.senderTimestamp)
        val receiverTimestamp: TextView = itemView.findViewById(R.id.receiverTimestamp)
    }
}