package com.example.smart_chat.adapters.shared

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.smart_chat.R
import com.example.smart_chat.models.shared.SharedMediaItem

class SharedMediaAdapter(
    private val context: Context,
    private val items: MutableList<SharedMediaItem> = mutableListOf()
) : RecyclerView.Adapter<SharedMediaAdapter.ViewHolder>() {

    fun submit(newItems: List<SharedMediaItem>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_shared_media, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]

        Glide.with(holder.image)
            .load(item.url)
            .placeholder(R.drawable.ic_profile)
            .error(R.drawable.ic_profile)
            .centerCrop()
            .into(holder.image)

        holder.itemView.setOnClickListener {
            // For now open the URL externally. Images can also be handled by FullScreenImageActivity if needed.
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(item.url))
            context.startActivity(intent)
        }
    }

    override fun getItemCount(): Int = items.size

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val image: ImageView = itemView.findViewById(R.id.media_image)
    }
}
