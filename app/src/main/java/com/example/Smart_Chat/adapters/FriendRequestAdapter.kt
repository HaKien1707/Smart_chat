package com.example.Smart_Chat.adapters

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.example.Smart_Chat.R
import com.example.Smart_Chat.models.FriendRequestModel
import com.example.Smart_Chat.models.userModel
import com.example.Smart_Chat.utils.FireBase_utils
import com.example.Smart_Chat.utils.androidUtils

class FriendRequestAdapter(
    private val context: Context,
    private val requestList: MutableList<Pair<FriendRequestModel, userModel>>
) : RecyclerView.Adapter<FriendRequestAdapter.RequestViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RequestViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.friend_request_item, parent, false)
        return RequestViewHolder(view)
    }

    override fun onBindViewHolder(holder: RequestViewHolder, position: Int) {
        val (request, user) = requestList[position]

        holder.userName.text = user.username ?: "Unknown"
        holder.userPhone.text = user.phoneNumber ?: ""

        // Load profile image
        if (!user.profileImage.isNullOrBlank()) {
            androidUtils.setProfileImageFromBase64(context, user.profileImage, holder.profileImage)
        } else {
            holder.profileImage.setImageResource(R.drawable.ic_profile)
        }

        // Accept button
        holder.acceptBtn.setOnClickListener {
            FireBase_utils.acceptFriendRequest(
                request.senderID ?: "",
                onSuccess = {
                    Toast.makeText(context, "Friend request accepted", Toast.LENGTH_SHORT).show()
                    requestList.removeAt(position)
                    notifyItemRemoved(position)
                    notifyItemRangeChanged(position, requestList.size)
                },
                onFailure = { e ->
                    Toast.makeText(context, "Failed to accept: ${e.message}", Toast.LENGTH_SHORT).show()
                    Log.e("FriendRequestAdapter", "Accept failed", e)
                }
            )
        }

        // Reject button
        holder.rejectBtn.setOnClickListener {
            FireBase_utils.rejectFriendRequest(
                request.senderID ?: "",
                onSuccess = {
                    Toast.makeText(context, "Friend request rejected", Toast.LENGTH_SHORT).show()
                    requestList.removeAt(position)
                    notifyItemRemoved(position)
                    notifyItemRangeChanged(position, requestList.size)
                },
                onFailure = { e ->
                    Toast.makeText(context, "Failed to reject: ${e.message}", Toast.LENGTH_SHORT).show()
                    Log.e("FriendRequestAdapter", "Reject failed", e)
                }
            )
        }
    }

    override fun getItemCount(): Int = requestList.size

    class RequestViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val profileImage: ImageView = itemView.findViewById(R.id.profile_image)
        val userName: TextView = itemView.findViewById(R.id.user_name)
        val userPhone: TextView = itemView.findViewById(R.id.user_phone)
        val acceptBtn: ImageButton = itemView.findViewById(R.id.accept_btn)
        val rejectBtn: ImageButton = itemView.findViewById(R.id.reject_btn)
    }
}