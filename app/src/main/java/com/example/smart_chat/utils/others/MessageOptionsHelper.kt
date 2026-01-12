package com.example.smart_chat.utils.others

import android.content.Context
import android.content.res.Resources
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import android.widget.PopupWindow
import android.widget.TextView
import com.example.smart_chat.R
import com.example.smart_chat.models.msg_action.ReplyMessageData

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

        val optionReplyContainer = popupView.findViewById<View>(R.id.option_reply_container)
        val optionForwardContainer = popupView.findViewById<View>(R.id.option_forward_container)
        val optionDeleteContainer = popupView.findViewById<View>(R.id.option_delete_container)

        val optionReply = popupView.findViewById<TextView>(R.id.option_reply)
        val optionForward = popupView.findViewById<TextView>(R.id.option_forward)
        val optionDelete = popupView.findViewById<TextView>(R.id.option_delete)

        val dividerForward = popupView.findViewById<View>(R.id.divider_forward)

        // Reply option
        val replyClickListener = View.OnClickListener {
            popupWindow.dismiss()
            onReply(messageData)
        }
        optionReplyContainer.setOnClickListener(replyClickListener)
        optionReply.setOnClickListener(replyClickListener)

        // Forward option
        val forwardClickListener = View.OnClickListener {
            popupWindow.dismiss()
            onForward()
        }
        optionForwardContainer.setOnClickListener(forwardClickListener)
        optionForward.setOnClickListener(forwardClickListener)

        // Delete option (only show if user can delete)
        if (canDelete) {
            optionDeleteContainer.visibility = View.VISIBLE
            dividerForward.visibility = View.VISIBLE

            val deleteClickListener = View.OnClickListener {
                popupWindow.dismiss()
                onDelete()
            }

            optionDeleteContainer.setOnClickListener(deleteClickListener)
            optionDelete.setOnClickListener(deleteClickListener)
        } else {
            optionDeleteContainer.visibility = View.GONE
            dividerForward.visibility = View.GONE
        }

        val gapPx = dpToPx(6)
        popupWindow.showAsDropDown(view, 0, -view.height - gapPx)
    }

    private fun dpToPx(dp: Int): Int {
        return (dp * Resources.getSystem().displayMetrics.density).toInt()
    }
}