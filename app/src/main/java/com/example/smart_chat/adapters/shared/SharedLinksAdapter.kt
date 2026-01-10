package com.example.smart_chat.adapters.shared

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.smart_chat.R
import com.example.smart_chat.models.shared.SharedLinkItem
import com.example.smart_chat.utils.shared.SharedContentClassifier

class SharedLinksAdapter(
    private val context: Context,
    private val items: MutableList<SharedLinkItem> = mutableListOf()
) : RecyclerView.Adapter<SharedLinksAdapter.ViewHolder>() {

    fun submit(newItems: List<SharedLinkItem>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_shared_link, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.text.text = item.text ?: ""
        holder.url.text = item.url

        holder.itemView.setOnClickListener {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(SharedContentClassifier.normalizeUrlForOpen(item.url)))
            context.startActivity(intent)
        }
    }

    override fun getItemCount(): Int = items.size

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val text: TextView = itemView.findViewById(R.id.link_text)
        val url: TextView = itemView.findViewById(R.id.link_url)
    }
}
