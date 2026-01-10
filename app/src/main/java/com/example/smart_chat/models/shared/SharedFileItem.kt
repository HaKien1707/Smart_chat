package com.example.smart_chat.models.shared

import com.google.firebase.Timestamp

data class SharedFileItem(
    val url: String,
    val fileName: String?,
    val fileSize: Long?,
    val timestamp: Timestamp? = null
)
