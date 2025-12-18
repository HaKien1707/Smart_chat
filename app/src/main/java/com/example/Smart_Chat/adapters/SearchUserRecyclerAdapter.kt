package com.example.Smart_Chat.adapters

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.example.Smart_Chat.R
import com.example.Smart_Chat.activities.user_chat.ChatActivity
import com.example.Smart_Chat.models.userModel
import com.example.Smart_Chat.utils.FireBase_utils
import com.example.Smart_Chat.utils.androidUtils
import com.firebase.ui.firestore.FirestoreRecyclerAdapter
import com.firebase.ui.firestore.FirestoreRecyclerOptions

class SearchUserRecyclerAdapter(
    options: FirestoreRecyclerOptions<userModel>,
    private val activity: Activity
) : FirestoreRecyclerAdapter<userModel, SearchUserRecyclerAdapter.UserModelViewHolder>(options) {

    override fun onBindViewHolder(holder: UserModelViewHolder, position: Int, model: userModel) {
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

        val isCurrentUser = model.userID == FireBase_utils.currentUserID()

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

            // Check if blocked by other user
            FireBase_utils.isBlockedByUser(model.userID ?: "") { isBlockedBy ->
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
                FireBase_utils.checkFriendshipStatus(model.userID ?: "") { status ->
                    activity.runOnUiThread {
                        holder.statusText.visibility = View.GONE

                        when (status) {
                            FireBase_utils.FriendshipStatus.FRIENDS -> {
                                // Friends - show remove friend and block
                                holder.addFriendBtn.visibility = View.GONE
                                holder.removeFriendBtn.visibility = View.VISIBLE
                                holder.blockBtn.visibility = View.VISIBLE

                                holder.itemView.setOnClickListener {
                                    val intent = Intent(activity, ChatActivity::class.java)
                                    androidUtils.passUserModelAsIntent(intent, model)
                                    activity.startActivity(intent)
                                }
                            }
                            FireBase_utils.FriendshipStatus.REQUEST_SENT -> {
                                // Request sent - HIDE add friend button
                                holder.addFriendBtn.visibility = View.GONE
                                holder.removeFriendBtn.visibility = View.GONE
                                holder.blockBtn.visibility = View.VISIBLE
                                holder.statusText.visibility = View.VISIBLE
                                holder.statusText.text = "Friend request sent"

                                holder.itemView.setOnClickListener(null)
                            }
                            FireBase_utils.FriendshipStatus.REQUEST_RECEIVED -> {
                                // Request received - show accept option
                                holder.addFriendBtn.visibility = View.VISIBLE
                                holder.addFriendBtn.setImageResource(R.drawable.ic_check) // Change to checkmark icon
                                holder.removeFriendBtn.visibility = View.GONE
                                holder.blockBtn.visibility = View.VISIBLE
                                holder.statusText.visibility = View.VISIBLE
                                holder.statusText.text = "Wants to be friends"

                                holder.itemView.setOnClickListener(null)
                            }
                            FireBase_utils.FriendshipStatus.NOT_FRIENDS -> {
                                // Not friends - show add friend and block
                                holder.addFriendBtn.visibility = View.VISIBLE
                                holder.addFriendBtn.setImageResource(R.drawable.ic_person_add) // Reset icon
                                holder.removeFriendBtn.visibility = View.GONE
                                holder.blockBtn.visibility = View.VISIBLE

                                holder.itemView.setOnClickListener {
                                    val intent = Intent(activity, ChatActivity::class.java)
                                    androidUtils.passUserModelAsIntent(intent, model)
                                    activity.startActivity(intent)
                                }
                            }
                        }
                    }
                }
            }

            // Button click listeners
            holder.addFriendBtn.setOnClickListener {
                sendFriendRequest(model, holder)
            }

            holder.removeFriendBtn.setOnClickListener {
                removeFriend(model, holder)
            }

            holder.blockBtn.setOnClickListener {
                blockUser(model, holder)
            }
        }
    }

    private fun sendFriendRequest(model: userModel, holder: UserModelViewHolder) {
        FireBase_utils.sendFriendRequest(
            model.userID ?: "",
            model.username ?: "",
            onSuccess = {
                activity.runOnUiThread {
                    Toast.makeText(activity, "Friend request sent", Toast.LENGTH_SHORT).show()
                    // Hide button and show status
                    holder.addFriendBtn.visibility = View.GONE
                    holder.statusText.visibility = View.VISIBLE
                    holder.statusText.text = "Friend request sent"
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
        AlertDialog.Builder(activity)
            .setTitle("Remove Friend")
            .setMessage("Remove ${model.username} from friends?")
            .setPositiveButton("Remove") { _, _ ->
                FireBase_utils.removeFriend(
                    model.userID ?: "",
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
        AlertDialog.Builder(activity)
            .setTitle("Block User")
            .setMessage("Block ${model.username}? They won't be able to send you friend requests.")
            .setPositiveButton("Block") { _, _ ->
                FireBase_utils.blockUser(
                    model.userID ?: "",
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