package com.example.smart_chat.models.shared

import com.google.firebase.Timestamp

data class SharedLinkItem(
    val url: String,
    val text: String?,
    val timestamp: Timestamp? = null
)
