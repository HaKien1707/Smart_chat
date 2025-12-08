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
import com.example.Smart_Chat.activities.ForwardMessageActivity
import com.example.Smart_Chat.activities.FullScreenImageActivity
import com.example.Smart_Chat.models.GroupMsgModel
import com.example.Smart_Chat.models.userModel
import com.example.Smart_Chat.utils.FireBase_utils
import com.example.Smart_Chat.utils.FireBase_utils.currentUserID
import com.example.Smart_Chat.utils.androidUtils
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

            // Check if deleted
            if (model.isDeleted) {
                holder.receiverMsg.visibility = View.VISIBLE
                holder.receiverImage.visibility = View.GONE
                holder.receiverMsg.text = "🚫 This message was deleted"
                holder.receiverMsg.setTextColor(context.getColor(R.color.gray))
                holder.receiverMsg.setTypeface(null, android.graphics.Typeface.ITALIC)

                // Remove bubble background for deleted messages
                holder.receiverMessageContainer.setBackgroundResource(0)
            } else {
                if (model.messageType == "image" && !model.imageUrl.isNullOrEmpty()) {
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

                    // NEW: Long press to show options
                    holder.receiverImage.setOnLongClickListener {
                        showMessageOptions(holder.receiverImage, position, model)
                        true
                    }
                } else {
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
                        showMessageOptions(holder.receiver, position, model)
                        true
                    }
                }
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

            // Load sender's profile image
            loadSenderProfileImage(holder, model.senderID)

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
                if (model.messageType == "image" && !model.imageUrl.isNullOrEmpty()) {
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
                } else {
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

            // Clear receiver fields (avoid recycled trash)
            holder.receiverMsg.text = ""
            holder.receiverImage.visibility = View.GONE
        }
    }

    private fun showMessageOptions(view: View, position: Int, model: GroupMsgModel) {
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
            openForwardActivity(model)
        }

        optionDelete.setOnClickListener {
            popupWindow.dismiss()
            deleteMessage(position)
        }

        popupWindow.showAsDropDown(view, 0, -view.height)
    }

    private fun openForwardActivity(model: GroupMsgModel) {
        val intent = Intent(context, ForwardMessageActivity::class.java)
        intent.putExtra("messageText", model.msg)
        intent.putExtra("imageUrl", model.imageUrl)
        intent.putExtra("messageType", model.messageType)
        intent.putExtra("isFromGroup", true)

        // NEW: Pass current group ID to exclude it from forward list
        if (context is com.example.Smart_Chat.activities.GroupChatActivity) {
            val groupID = (context as com.example.Smart_Chat.activities.GroupChatActivity).getGroupID()
            intent.putExtra("currentChatId", groupID)
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

    private fun loadSenderProfileImage(holder: GroupMsgViewHolder, senderID: String?) {
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
                Log.e("GroupMsgAdapter", "Failed to load profile image", e)
                holder.senderProfileImage.setImageResource(R.drawable.ic_profile)
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
        val senderProfileImage: ImageView = itemView.findViewById(R.id.senderProfileImage)
        val senderName: TextView = itemView.findViewById(R.id.senderName)
        val senderMsg: TextView = itemView.findViewById(R.id.senderMsg)
        val receiverMsg: TextView = itemView.findViewById(R.id.receiverMsg)
        val senderImage: ImageView = itemView.findViewById(R.id.senderImage)
        val receiverImage: ImageView = itemView.findViewById(R.id.receiverImage)
        val senderTimestamp: TextView = itemView.findViewById(R.id.senderTimestamp)
        val receiverTimestamp: TextView = itemView.findViewById(R.id.receiverTimestamp)
    }
}