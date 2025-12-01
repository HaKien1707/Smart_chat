package com.example.Smart_Chat.adapters

import android.content.Context
import android.content.Intent
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.Smart_Chat.R
import com.example.Smart_Chat.activities.chatActivity
import com.example.Smart_Chat.models.userModel
import com.example.Smart_Chat.utils.FireBase_utils
import com.example.Smart_Chat.utils.androidUtils
import com.firebase.ui.firestore.FirestoreRecyclerAdapter
import com.firebase.ui.firestore.FirestoreRecyclerOptions

class SearchUserRecyclerAdapter(
    options: FirestoreRecyclerOptions<userModel>,
    private val context: Context
) : FirestoreRecyclerAdapter<userModel, SearchUserRecyclerAdapter.UserModelViewHolder>(options) {

    override fun onBindViewHolder(holder: UserModelViewHolder, position: Int, model: userModel) {

        Log.d("SearchUser", "Bind user: ${model.username}, Has image: ${model.profileImage != null}")

        // --- Load image ---
        if (!model.profileImage.isNullOrEmpty()) {
            androidUtils.setProfileImageFromBase64(
                context,
                model.profileImage,
                holder.profileImage
            )
        } else {
            holder.profileImage.setImageResource(R.drawable.ic_person)
        }

        // --- Username & phone ---
        holder.usernameText.text = model.username
        holder.usernamePhone.text = model.phoneNumber

        // Check if this is the current user
        val isCurrentUser = model.userID == FireBase_utils.currentUserID()

        if (isCurrentUser) {
            // Mark "Me" and disable clicking
            holder.usernameText.text = "${model.username} (Me)"
            holder.itemView.isEnabled = false
            holder.itemView.alpha = 0.5f  // Make it look disabled
            holder.itemView.setOnClickListener(null)  // Remove click listener
        } else {
            // Enable clicking for other users
            holder.itemView.isEnabled = true
            holder.itemView.alpha = 1.0f
            holder.itemView.setOnClickListener {
                val intent = Intent(context, chatActivity::class.java)
                androidUtils.passUserModelAsIntent(intent, model)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                context.startActivity(intent)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserModelViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.search_user_item, parent, false)
        return UserModelViewHolder(view)
    }

    inner class UserModelViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val profileImage: ImageView = itemView.findViewById(R.id.profile_image)
        val usernameText: TextView = itemView.findViewById(R.id.usernameText)
        val usernamePhone: TextView = itemView.findViewById(R.id.usernamePhone)
    }
}