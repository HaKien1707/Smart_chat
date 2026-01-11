package com.example.smart_chat.adapters.social

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.smart_chat.R
import com.example.smart_chat.activities.user_chat.ChatActivity
import com.example.smart_chat.models.userModel
import com.example.smart_chat.utils.others.androidUtils
import com.example.smart_chat.utils.firebase.*
import com.firebase.ui.firestore.FirestoreRecyclerAdapter
import com.firebase.ui.firestore.FirestoreRecyclerOptions

class SearchUserRecyclerAdapter(
    options: FirestoreRecyclerOptions<userModel>,
    private val activity: Activity
) : FirestoreRecyclerAdapter<userModel, SearchUserRecyclerAdapter.UserModelViewHolder>(options) {

    private var searchQuery: String = ""
    private var isPhoneSearch: Boolean = false

    fun updateSearchQuery(query: String) {
        searchQuery = query
        isPhoneSearch = query.matches(Regex("^[+\\-\\d\\s]+$"))
        notifyDataSetChanged()
    }

    fun getFilteredItemCount(): Int {
        if (searchQuery.isBlank()) return snapshots.size

        var count = 0
        for (i in 0 until snapshots.size) {
            val model = snapshots[i]
            if (matchesQuery(model)) count++
        }
        return count
    }

    private fun matchesQuery(model: userModel): Boolean {
        val q = searchQuery.trim()
        if (q.isEmpty()) return true

        return if (isPhoneSearch) {
            val normalizedQuery = q.replace(Regex("[\\s-]"), "")
            val normalizedPhone = (model.phoneNumber ?: "").replace(Regex("[\\s-]"), "")
            normalizedPhone.startsWith(normalizedQuery)
        } else {
            val lowerQuery = q.lowercase()
            (model.username ?: "").lowercase().startsWith(lowerQuery)
        }
    }

    override fun onBindViewHolder(holder: UserModelViewHolder, position: Int, model: userModel) {
        // Firestore usually stores user id as the document id.
        // If userID isn't stored as a field, model.userID will be null -> can crash code paths that do document("").
        val resolvedUserId = try {
            snapshots.getSnapshot(position).id
        } catch (_: Exception) {
            model.userID ?: ""
        }
        if (model.userID.isNullOrBlank() && resolvedUserId.isNotBlank()) {
            model.userID = resolvedUserId
        }

        // Client-side filter to make search case-insensitive even if stored usernames are "Cat".
        if (!matchesQuery(model)) {
            holder.itemView.visibility = View.GONE
            holder.itemView.layoutParams = RecyclerView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                0
            )
            return
        } else {
            holder.itemView.visibility = View.VISIBLE
            holder.itemView.layoutParams = RecyclerView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }

        // Load image
        if (!model.profileImage.isNullOrEmpty()) {
            androidUtils.setProfileImageFromBase64(
                activity,
                model.profileImage,
                holder.profileImage
            )
        } else {
            holder.profileImage.setImageResource(R.drawable.ic_profile)
        }

        holder.usernameText.text = model.username
        holder.usernamePhone.text = model.phoneNumber

        val isCurrentUser = resolvedUserId.isNotBlank() && resolvedUserId == FirebaseAuthentication.currentUserID()

        if (isCurrentUser) {
            // Current user
            holder.usernameText.text = "${model.username} (Me)"
            holder.itemView.alpha = 0.5f
            holder.itemView.setOnClickListener(null)
            holder.addFriendBtn.visibility = View.GONE
            holder.removeFriendBtn.visibility = View.GONE
            holder.blockBtn.visibility = View.GONE
            holder.statusText.visibility = View.GONE
        } else {
            holder.itemView.alpha = 1.0f

            // Reset button click listeners (will be set based on status)
            holder.addFriendBtn.setOnClickListener(null)
            holder.removeFriendBtn.setOnClickListener(null)
            holder.blockBtn.setOnClickListener(null)

            if (resolvedUserId.isBlank()) {
                activity.runOnUiThread {
                    holder.statusText.text = "User not available"
                    holder.statusText.visibility = View.VISIBLE
                    holder.addFriendBtn.visibility = View.GONE
                    holder.removeFriendBtn.visibility = View.GONE
                    holder.blockBtn.visibility = View.GONE
                    holder.itemView.setOnClickListener(null)
                }
                return
            }

            // Check if blocked by other user
            FirebaseBlocking.isBlockedByUser(resolvedUserId) { isBlockedBy ->
                if (isBlockedBy) {
                    // Blocked by them
                    activity.runOnUiThread {
                        holder.statusText.text = "You are blocked by this user"
                        holder.statusText.visibility = View.VISIBLE
                        holder.addFriendBtn.visibility = View.GONE
                        holder.removeFriendBtn.visibility = View.GONE
                        holder.blockBtn.visibility = View.GONE
                        holder.itemView.setOnClickListener(null)
                    }
                    return@isBlockedByUser
                }

                // Check friendship status
                FirebaseFriends.checkFriendshipStatus(resolvedUserId) { status ->
                    activity.runOnUiThread {
                        holder.statusText.visibility = View.GONE

                        fun setAddFriendAsSend() {
                            holder.addFriendBtn.visibility = View.VISIBLE
                            holder.addFriendBtn.setImageResource(R.drawable.ic_person_add)
                            holder.addFriendBtn.backgroundTintList = ColorStateList.valueOf(
                                ContextCompat.getColor(activity, R.color.green)
                            )
                            holder.addFriendBtn.contentDescription = "Add Friend"
                            holder.addFriendBtn.setOnClickListener {
                                sendFriendRequest(model, holder)
                            }
                        }

                        fun setAddFriendAsCancelRequest() {
                            holder.addFriendBtn.visibility = View.VISIBLE
                            holder.addFriendBtn.setImageResource(R.drawable.ic_close)
                            holder.addFriendBtn.backgroundTintList = ColorStateList.valueOf(
                                ContextCompat.getColor(activity, R.color.orange)
                            )
                            holder.addFriendBtn.contentDescription = "Cancel Friend Request"
                            holder.addFriendBtn.setOnClickListener {
                                cancelFriendRequest(model, holder)
                            }
                        }

                        fun setAddFriendAsAcceptRequest() {
                            holder.addFriendBtn.visibility = View.VISIBLE
                            holder.addFriendBtn.setImageResource(R.drawable.ic_check)
                            holder.addFriendBtn.backgroundTintList = ColorStateList.valueOf(
                                ContextCompat.getColor(activity, R.color.green)
                            )
                            holder.addFriendBtn.contentDescription = "Accept Friend Request"
                            holder.addFriendBtn.setOnClickListener {
                                acceptFriendRequest(model, holder)
                            }
                        }

                        when (status) {
                            FirebaseFriends.FriendshipStatus.FRIENDS -> {
                                // Friends - show remove friend and block
                                holder.addFriendBtn.visibility = View.GONE
                                holder.removeFriendBtn.visibility = View.VISIBLE
                                holder.blockBtn.visibility = View.VISIBLE

                                holder.removeFriendBtn.setOnClickListener {
                                    removeFriend(model, holder)
                                }

                                holder.blockBtn.setOnClickListener {
                                    blockUser(model, holder)
                                }

                                holder.itemView.setOnClickListener {
                                    val intent = Intent(activity, ChatActivity::class.java)
                                    model.userID = resolvedUserId
                                    androidUtils.passUserModelAsIntent(intent, model)
                                    activity.startActivity(intent)
                                }
                            }
                            FirebaseFriends.FriendshipStatus.REQUEST_SENT -> {
                                // Request sent - show cancel request button in add-friend position
                                setAddFriendAsCancelRequest()
                                holder.removeFriendBtn.visibility = View.GONE
                                holder.blockBtn.visibility = View.VISIBLE
                                holder.statusText.visibility = View.VISIBLE
                                holder.statusText.text = "Friend request sent"

                                holder.blockBtn.setOnClickListener {
                                    blockUser(model, holder)
                                }

                                holder.itemView.setOnClickListener(null)
                            }
                            FirebaseFriends.FriendshipStatus.REQUEST_RECEIVED -> {
                                // Request received - show accept option
                                setAddFriendAsAcceptRequest()
                                holder.removeFriendBtn.visibility = View.GONE
                                holder.blockBtn.visibility = View.VISIBLE
                                holder.statusText.visibility = View.VISIBLE
                                holder.statusText.text = "Wants to be friends"

                                holder.blockBtn.setOnClickListener {
                                    blockUser(model, holder)
                                }

                                holder.itemView.setOnClickListener(null)
                            }
                            FirebaseFriends.FriendshipStatus.NOT_FRIENDS -> {
                                // Not friends - show add friend and block
                                setAddFriendAsSend()
                                holder.removeFriendBtn.visibility = View.GONE
                                holder.blockBtn.visibility = View.VISIBLE

                                holder.blockBtn.setOnClickListener {
                                    blockUser(model, holder)
                                }

                                holder.itemView.setOnClickListener {
                                    val intent = Intent(activity, ChatActivity::class.java)
                                    model.userID = resolvedUserId
                                    androidUtils.passUserModelAsIntent(intent, model)
                                    activity.startActivity(intent)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun sendFriendRequest(model: userModel, holder: UserModelViewHolder) {
        val targetId = model.userID
        if (targetId.isNullOrBlank()) {
            Toast.makeText(activity, "User not available", Toast.LENGTH_SHORT).show()
            return
        }
        FirebaseFriends.sendFriendRequest(
            targetId,
            model.username ?: "",
            onSuccess = {
                activity.runOnUiThread {
                    Toast.makeText(activity, "Friend request sent", Toast.LENGTH_SHORT).show()
                    holder.statusText.visibility = View.VISIBLE
                    holder.statusText.text = "Friend request sent"

                    // Switch add-friend button into cancel-request mode (same position)
                    holder.addFriendBtn.visibility = View.VISIBLE
                    holder.addFriendBtn.setImageResource(R.drawable.ic_close)
                    holder.addFriendBtn.backgroundTintList = ColorStateList.valueOf(
                        ContextCompat.getColor(activity, R.color.orange)
                    )
                    holder.addFriendBtn.setOnClickListener {
                        cancelFriendRequest(model, holder)
                    }
                }
            },
            onFailure = { e ->
                activity.runOnUiThread {
                    Toast.makeText(activity, "Failed: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        )
    }

    private fun cancelFriendRequest(model: userModel, holder: UserModelViewHolder) {
        val targetId = model.userID
        if (targetId.isNullOrBlank()) {
            Toast.makeText(activity, "User not available", Toast.LENGTH_SHORT).show()
            return
        }
        FirebaseFriends.cancelFriendRequest(
            targetId,
            onSuccess = {
                activity.runOnUiThread {
                    Toast.makeText(activity, "Request cancelled", Toast.LENGTH_SHORT).show()
                    holder.statusText.visibility = View.GONE

                    // Revert button to send-friend-request mode
                    holder.addFriendBtn.visibility = View.VISIBLE
                    holder.addFriendBtn.setImageResource(R.drawable.ic_person_add)
                    holder.addFriendBtn.backgroundTintList = ColorStateList.valueOf(
                        ContextCompat.getColor(activity, R.color.green)
                    )
                    holder.addFriendBtn.setOnClickListener {
                        sendFriendRequest(model, holder)
                    }
                }
            },
            onFailure = { e ->
                activity.runOnUiThread {
                    Toast.makeText(activity, "Failed: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        )
    }

    private fun acceptFriendRequest(model: userModel, holder: UserModelViewHolder) {
        val targetId = model.userID
        if (targetId.isNullOrBlank()) {
            Toast.makeText(activity, "User not available", Toast.LENGTH_SHORT).show()
            return
        }
        FirebaseFriends.acceptFriendRequest(
            senderID = targetId,
            onSuccess = {
                activity.runOnUiThread {
                    Toast.makeText(activity, "Friend request accepted", Toast.LENGTH_SHORT).show()
                    holder.statusText.visibility = View.GONE
                    holder.addFriendBtn.visibility = View.GONE
                    holder.removeFriendBtn.visibility = View.VISIBLE
                    holder.blockBtn.visibility = View.VISIBLE

                    holder.removeFriendBtn.setOnClickListener {
                        removeFriend(model, holder)
                    }
                    holder.blockBtn.setOnClickListener {
                        blockUser(model, holder)
                    }

                    holder.itemView.setOnClickListener {
                        val intent = Intent(activity, ChatActivity::class.java)
                        androidUtils.passUserModelAsIntent(intent, model)
                        activity.startActivity(intent)
                    }
                }
            },
            onFailure = { e ->
                activity.runOnUiThread {
                    Toast.makeText(activity, "Failed: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        )
    }

    private fun removeFriend(model: userModel, holder: UserModelViewHolder) {
        val targetId = model.userID
        if (targetId.isNullOrBlank()) {
            Toast.makeText(activity, "User not available", Toast.LENGTH_SHORT).show()
            return
        }
        AlertDialog.Builder(activity)
            .setTitle("Remove Friend")
            .setMessage("Remove ${model.username} from friends?")
            .setPositiveButton("Remove") { _, _ ->
                FirebaseFriends.removeFriend(
                    targetId,
                    onSuccess = {
                        activity.runOnUiThread {
                            Toast.makeText(activity, "Friend removed", Toast.LENGTH_SHORT).show()
                            holder.removeFriendBtn.visibility = View.GONE
                            holder.addFriendBtn.visibility = View.VISIBLE
                        }
                    },
                    onFailure = { e ->
                        activity.runOnUiThread {
                            Toast.makeText(activity, "Failed: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
                )
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun blockUser(model: userModel, holder: UserModelViewHolder) {
        val targetId = model.userID
        if (targetId.isNullOrBlank()) {
            Toast.makeText(activity, "User not available", Toast.LENGTH_SHORT).show()
            return
        }
        AlertDialog.Builder(activity)
            .setTitle("Block User")
            .setMessage("Block ${model.username}? They won't be able to send you friend requests.")
            .setPositiveButton("Block") { _, _ ->
                FirebaseBlocking.blockUser(
                    targetId,
                    onSuccess = {
                        activity.runOnUiThread {
                            Toast.makeText(activity, "${model.username} blocked", Toast.LENGTH_SHORT).show()
                            holder.addFriendBtn.visibility = View.GONE
                            holder.removeFriendBtn.visibility = View.GONE
                            holder.blockBtn.visibility = View.GONE
                            holder.statusText.text ="User blocked"
                            holder.statusText.visibility = View.VISIBLE
                        }
                    },
                    onFailure = { e ->
                        activity.runOnUiThread {
                            Toast.makeText(activity, "Failed: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
                )
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserModelViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_search_user, parent, false)
        return UserModelViewHolder(view)
    }

    inner class UserModelViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val profileImage: ImageView = itemView.findViewById(R.id.profile_image)
        val usernameText: TextView = itemView.findViewById(R.id.usernameText)
        val usernamePhone: TextView = itemView.findViewById(R.id.usernamePhone)
        val statusText: TextView = itemView.findViewById(R.id.status_text)
        val addFriendBtn: ImageButton = itemView.findViewById(R.id.add_friend_btn)
        val removeFriendBtn: ImageButton = itemView.findViewById(R.id.remove_friend_btn)
        val blockBtn: ImageButton = itemView.findViewById(R.id.block_btn)
    }
}