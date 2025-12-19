package com.example.Smart_Chat.adapters.community

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.Smart_Chat.R
import com.example.Smart_Chat.activities.community.CommunityChatActivity
import com.example.Smart_Chat.models.community.CommunityModel
import com.example.Smart_Chat.utils.others.androidUtils
import com.firebase.ui.firestore.FirestoreRecyclerAdapter
import com.firebase.ui.firestore.FirestoreRecyclerOptions

class CommunityAdapter(
    options: FirestoreRecyclerOptions<CommunityModel>,
    private val context: Context
) : FirestoreRecyclerAdapter<CommunityModel, CommunityAdapter.CommunityViewHolder>(options) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CommunityViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_community, parent, false)
        return CommunityViewHolder(view)
    }

    override fun onBindViewHolder(holder: CommunityViewHolder, position: Int, model: CommunityModel) {
        holder.communityName.text = model.communityName ?: "Unknown Community"
        holder.communityDescription.text = model.communityDescription ?: "No description"
        // Remove member count line

        // Load community image
        if (!model.communityImage.isNullOrBlank()) {
            androidUtils.setProfileImageFromBase64(
                context,
                model.communityImage,
                holder.communityImage
            )
        } else {
            holder.communityImage.setImageResource(R.drawable.ic_community)
        }

        // Click to open community chat
        holder.itemView.setOnClickListener {
            val intent = Intent(context, CommunityChatActivity::class.java)
            intent.putExtra("communityID", model.communityID)
            intent.putExtra("communityName", model.communityName)
            context.startActivity(intent)
        }
    }

    class CommunityViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val communityImage: ImageView = itemView.findViewById(R.id.community_image)
        val communityName: TextView = itemView.findViewById(R.id.community_name)
        val communityDescription: TextView = itemView.findViewById(R.id.community_description)
    }
}