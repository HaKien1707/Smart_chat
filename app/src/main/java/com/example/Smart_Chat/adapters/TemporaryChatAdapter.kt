package com.example.Smart_Chat.adapters

import android.content.Context
import android.content.Intent
import android.os.CountDownTimer
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.Smart_Chat.R
import com.example.Smart_Chat.activities.TemporaryChatActivity
import com.example.Smart_Chat.models.TemporaryChatModel
import com.example.Smart_Chat.models.userModel
import com.example.Smart_Chat.utils.FireBase_utils
import com.example.Smart_Chat.utils.androidUtils
import com.firebase.ui.firestore.FirestoreRecyclerAdapter
import com.firebase.ui.firestore.FirestoreRecyclerOptions

class TemporaryChatAdapter(
    options: FirestoreRecyclerOptions<TemporaryChatModel>,
    private val context: Context
) : FirestoreRecyclerAdapter<TemporaryChatModel, TemporaryChatAdapter.TempChatViewHolder>(options) {

    private val timers = mutableMapOf<String, CountDownTimer>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TempChatViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_temporary_chat, parent, false)
        return TempChatViewHolder(view)
    }

    override fun onBindViewHolder(holder: TempChatViewHolder, position: Int, model: TemporaryChatModel) {
        // Check if position is valid
        if (position < 0 || position >= itemCount) {
            Log.w("TemporaryChatAdapter", "Invalid position: $position, itemCount: $itemCount")
            return
        }

        // Calculate remaining time FIRST
        val expiresAt = model.expiresAt?.toDate()?.time ?: 0
        val now = System.currentTimeMillis()
        val remainingMillis = expiresAt - now

        // If expired, skip binding and delete
        if (remainingMillis <= 0) {
            holder.expiryTimer.text = "Expired"
            model.chatID?.let {
                // Delete this specific chat
                FireBase_utils.getTemporaryChatReference(it).delete()
            }
            return
        }

        // Get other user
        FireBase_utils.get2ndUserInChatRoom(model.userIDs)?.get()
            ?.addOnSuccessListener { userDoc ->
                // Check if holder is still valid
                if (holder.bindingAdapterPosition == RecyclerView.NO_POSITION) {
                    return@addOnSuccessListener
                }

                val otherUser = userDoc.toObject(userModel::class.java)

                holder.username.text = otherUser?.username ?: "Unknown"
                holder.lastMsg.text = model.lastMsg ?: "No messages yet"

                // Load profile image
                if (!otherUser?.profileImage.isNullOrBlank()) {
                    androidUtils.setProfileImageFromBase64(
                        context,
                        otherUser?.profileImage,
                        holder.profileImage
                    )
                } else {
                    holder.profileImage.setImageResource(R.drawable.ic_profile)
                }

                // Start countdown
                startCountdown(holder, model.chatID ?: "", remainingMillis)

                // Click to open chat
                holder.itemView.setOnClickListener {
                    val intent = Intent(context, TemporaryChatActivity::class.java)
                    intent.putExtra("chatID", model.chatID)
                    androidUtils.passUserModelAsIntent(intent, otherUser)
                    context.startActivity(intent)
                }
            }
            ?.addOnFailureListener { e ->
                Log.e("TemporaryChatAdapter", "Failed to load user", e)
            }
    }

    private fun startCountdown(holder: TempChatViewHolder, chatID: String, remainingMillis: Long) {
        // Cancel existing timer for this chat
        timers[chatID]?.cancel()

        val timer = object : CountDownTimer(remainingMillis, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                // Check if holder is still valid
                if (holder.bindingAdapterPosition == RecyclerView.NO_POSITION) {
                    cancel()
                    return
                }

                val minutes = (millisUntilFinished / 1000) / 60
                val seconds = (millisUntilFinished / 1000) % 60
                holder.expiryTimer.text = String.format("%02d:%02d remaining", minutes, seconds)
            }

            override fun onFinish() {
                if (holder.bindingAdapterPosition != RecyclerView.NO_POSITION) {
                    holder.expiryTimer.text = "Expired"
                }
                // Delete the chat
                FireBase_utils.getTemporaryChatReference(chatID).delete()
                timers.remove(chatID)
            }
        }

        timer.start()
        timers[chatID] = timer
    }

    override fun onViewRecycled(holder: TempChatViewHolder) {
        super.onViewRecycled(holder)
        // Don't cancel all timers, only cancel if needed per item
    }

    override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
        super.onDetachedFromRecyclerView(recyclerView)
        // Cancel all timers when adapter is detached
        timers.values.forEach { it.cancel() }
        timers.clear()
    }

    class TempChatViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val profileImage: ImageView = itemView.findViewById(R.id.profile_image)
        val username: TextView = itemView.findViewById(R.id.username)
        val lastMsg: TextView = itemView.findViewById(R.id.lastMsg)
        val expiryTimer: TextView = itemView.findViewById(R.id.expiry_timer)
    }
}