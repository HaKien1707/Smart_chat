package com.example.Smart_Chat.models.msg_action

data class ReplyMessageData(
    val messageId: String,
    val text: String?,
    val type: String, // "text", "image", "file"
    val imageUrl: String?,
    val fileName: String?,
    val fileSize: Long?,
    val senderName: String? = null // For group/community chats
)