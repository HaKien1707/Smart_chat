package com.example.smart_chat.adapters.shared

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.smart_chat.R
import com.example.smart_chat.models.shared.SharedFileItem
import com.example.smart_chat.utils.media.MediaMessageHelper

class SharedFilesAdapter(
    private val context: Context,
    private val items: MutableList<SharedFileItem> = mutableListOf()
) : RecyclerView.Adapter<SharedFilesAdapter.ViewHolder>() {

    fun submit(newItems: List<SharedFileItem>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_shared_file, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.fileName.text = item.fileName ?: context.getString(R.string.app_name)

        val size = item.fileSize
        holder.fileSize.text = if (size != null) MediaMessageHelper.formatFileSize(size) else ""

        holder.icon.setImageResource(R.drawable.ic_attach_file)

        holder.itemView.setOnClickListener {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(item.url))
            context.startActivity(intent)
        }
    }

    override fun getItemCount(): Int = items.size

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val icon: ImageView = itemView.findViewById(R.id.file_icon)
        val fileName: TextView = itemView.findViewById(R.id.file_name)
        val fileSize: TextView = itemView.findViewById(R.id.file_size)
    }
}
