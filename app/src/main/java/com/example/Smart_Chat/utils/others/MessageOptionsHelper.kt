package com.example.Smart_Chat.utils.others

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import android.widget.PopupWindow
import android.widget.TextView
import com.example.Smart_Chat.R
import com.example.Smart_Chat.models.msg_action.ReplyMessageData

object MessageOptionsHelper {

    fun showMessageOptions(
        context: Context,
        view: View,
        canDelete: Boolean,
        messageData: ReplyMessageData,
        onReply: (ReplyMessageData) -> Unit,
        onForward: () -> Unit,
        onDelete: () -> Unit
    ) {
        val popupView = LayoutInflater.from(context).inflate(R.layout.popup_message_options, null)
        val popupWindow = PopupWindow(
            popupView,
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT,
            true
        )

        val optionReply = popupView.findViewById<TextView>(R.id.option_reply)
        val optionForward = popupView.findViewById<TextView>(R.id.option_forward)
        val optionDelete = popupView.findViewById<TextView>(R.id.option_delete)

        // Reply option
        optionReply.setOnClickListener {
            popupWindow.dismiss()
            onReply(messageData)
        }

        // Forward option
        optionForward.setOnClickListener {
            popupWindow.dismiss()
            onForward()
        }

        // Delete option (only show if user can delete)
        if (canDelete) {
            optionDelete.visibility = View.VISIBLE
            optionDelete.setOnClickListener {
                popupWindow.dismiss()
                onDelete()
            }
        } else {
            optionDelete.visibility = View.GONE
        }

        popupWindow.showAsDropDown(view, 0, -view.height)
    }
}