package com.example.smart_chat.models.shared

import com.google.firebase.Timestamp

data class SharedMediaItem(
    val url: String,
    val isVideo: Boolean,
    val timestamp: Timestamp? = null
)
