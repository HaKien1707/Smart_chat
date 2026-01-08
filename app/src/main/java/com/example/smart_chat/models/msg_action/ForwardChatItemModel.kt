package com.example.smart_chat.models.msg_action

data class ForwardChatItemModel(
    val id: String,
    val name: String,
    val imageUrl: String?,
    val type: ForwardChatType, // "USER" or "GROUP"
    val isSelected: Boolean = false
)

enum class ForwardChatType {
    USER,
    GROUP
}