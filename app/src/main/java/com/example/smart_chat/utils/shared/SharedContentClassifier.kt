package com.example.smart_chat.utils.shared

import android.util.Patterns

object SharedContentClassifier {

    private val imageExtensions = setOf("jpg", "jpeg", "png", "gif", "webp", "heic")
    private val videoExtensions = setOf("mp4", "mov", "mkv", "webm", "3gp", "avi")

    fun extractUrls(text: String?): List<String> {
        if (text.isNullOrBlank()) return emptyList()
        val matcher = Patterns.WEB_URL.matcher(text)
        val urls = mutableListOf<String>()
        while (matcher.find()) {
            val match = matcher.group() ?: continue
            urls.add(match)
        }
        return urls.distinct()
    }

    fun isMediaMessage(
        messageType: String?,
        imageUrl: String?,
        fileUrl: String?,
        fileName: String?
    ): Boolean {
        if (!imageUrl.isNullOrBlank()) return true
        val type = messageType?.lowercase()
        if (type == "image") return true

        // Some videos/images can be sent as "file"
        if (!fileUrl.isNullOrBlank() && (isVideoFile(fileName, fileUrl) || isImageFile(fileName, fileUrl))) {
            return true
        }

        return false
    }

    fun isFileMessage(
        messageType: String?,
        fileUrl: String?,
        fileName: String?
    ): Boolean {
        val type = messageType?.lowercase()
        if (type != "file") return false
        if (fileUrl.isNullOrBlank()) return false

        // If it's image/video by extension, we treat it as media instead of file list.
        if (isVideoFile(fileName, fileUrl) || isImageFile(fileName, fileUrl)) return false

        return true
    }

    fun isLinkMessage(messageType: String?, text: String?): Boolean {
        val type = messageType?.lowercase()
        if (type != null && type != "text") return false
        return extractUrls(text).isNotEmpty()
    }

    fun isVideoFile(fileName: String?, fileUrl: String?): Boolean {
        return extensionOf(fileName, fileUrl)?.let { it in videoExtensions } == true
    }

    fun isImageFile(fileName: String?, fileUrl: String?): Boolean {
        return extensionOf(fileName, fileUrl)?.let { it in imageExtensions } == true
    }

    private fun extensionOf(fileName: String?, fileUrl: String?): String? {
        val candidate = when {
            !fileName.isNullOrBlank() -> fileName
            !fileUrl.isNullOrBlank() -> fileUrl.substringAfterLast('/', fileUrl)
            else -> null
        } ?: return null

        val ext = candidate.substringAfterLast('.', "").lowercase()
        return ext.ifBlank { null }
    }
}
