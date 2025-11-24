package com.example.Smart_Chat.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.Smart_Chat.models.MsgModel
import com.example.Smart_Chat.utils.FireBase_utils.currentUserID
import com.example.Smart_Chat.R
import com.firebase.ui.firestore.FirestoreRecyclerAdapter
import com.firebase.ui.firestore.FirestoreRecyclerOptions

class MsgRecyclerAdapter(
    options: FirestoreRecyclerOptions<MsgModel>,
    private val context: Context
) : FirestoreRecyclerAdapter<MsgModel, MsgRecyclerAdapter.MsgViewHolder>(options) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MsgViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.msg_row_item, parent, false)
        return MsgViewHolder(view)
    }

    override fun onBindViewHolder(holder: MsgViewHolder, position: Int, model: MsgModel) {

        if (model.senderID == currentUserID()) {
            // Message from other user → show on right
            holder.sender.visibility = View.GONE
            holder.receiver.visibility = View.VISIBLE
            holder.receiverMsg.text = model.msg
        } else {
            // Message from me → show on left
            holder.sender.visibility = View.VISIBLE
            holder.receiver.visibility = View.GONE
            holder.senderMsg.text = model.msg
        }
    }

    class MsgViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val sender: LinearLayout = itemView.findViewById(R.id.sender)
        val receiver: LinearLayout = itemView.findViewById(R.id.receiver)
        val senderMsg: TextView = itemView.findViewById(R.id.senderMsg)
        val receiverMsg: TextView = itemView.findViewById(R.id.receiverMsg)
    }
}
