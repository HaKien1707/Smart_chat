package com.example.Smart_Chat.utils.media

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import android.widget.Toast
import com.example.Smart_Chat.models.MsgModel
import com.example.Smart_Chat.models.temp_chat.TempChatMsgModel
import com.example.Smart_Chat.models.community.CommunityMsgModel
import com.example.Smart_Chat.models.group.GroupMsgModel
import com.example.Smart_Chat.utils.security.EncryptionUtils
import com.google.firebase.Timestamp
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentReference
import java.security.MessageDigest
import java.util.UUID

object MediaMessageHelper {

    // ========== IMAGE HANDLING ==========

    fun uploadAndSendImage(
        context: Context,
        imageUri: Uri,
        chatReference: DocumentReference,
        messagesReference: CollectionReference,
        senderID: String,
        senderName: String? = null,
        messageType: MessageType,
        encryptionKey: String? = null,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        Toast.makeText(context, "Uploading image...", Toast.LENGTH_SHORT).show()

        val imageHash = generateImageHash(context, imageUri)

        CloudinaryHelper.uploadImageWithHash(
            context,
            imageUri,
            imageHash,
            onSuccess = { imageUrl ->
                when (messageType) {
                    MessageType.ONE_TO_ONE -> {
                        sendImageMessage_OneToOne(
                            chatReference,
                            messagesReference,
                            senderID,
                            imageUrl,
                            onSuccess,
                            onError
                        )
                    }
                    MessageType.GROUP -> {
                        sendImageMessage_Group(
                            chatReference,
                            messagesReference,
                            senderID,
                            senderName ?: "Unknown",
                            imageUrl,
                            onSuccess,
                            onError
                        )
                    }
                    MessageType.COMMUNITY -> {
                        sendImageMessage_Community(
                            chatReference,
                            messagesReference,
                            senderID,
                            senderName ?: "Unknown",
                            imageUrl,
                            onSuccess,
                            onError
                        )
                    }
                    MessageType.PRIVATE_TEMP -> {
                        if (encryptionKey == null) {
                            onError("Encryption key required for temp chat")
                            return@uploadImageWithHash
                        }
                        sendImageMessage_Temp(
                            chatReference,
                            messagesReference,
                            senderID,
                            imageUrl,
                            encryptionKey,
                            onSuccess,
                            onError
                        )
                    }
                }
                Toast.makeText(context, "Image sent!", Toast.LENGTH_SHORT).show()
            },
            onError = { error ->
                onError(error)
                Toast.makeText(context, "Upload failed: $error", Toast.LENGTH_LONG).show()
            }
        )
    }

    // ========== FILE HANDLING ==========

    fun uploadAndSendFile(
        context: Context,
        fileUri: Uri,
        chatReference: DocumentReference,
        messagesReference: CollectionReference,
        senderID: String,
        senderName: String? = null,
        messageType: MessageType,
        encryptionKey: String? = null,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val fileName = getFileName(context, fileUri)
        val fileSize = getFileSize(context, fileUri)

        // Check file size (10MB limit)
        val maxSize = 10 * 1024 * 1024
        if (fileSize > maxSize) {
            Toast.makeText(
                context,
                "File too large. Maximum size is 10MB. Selected: ${formatFileSize(fileSize)}",
                Toast.LENGTH_LONG
            ).show()
            return
        }

        Toast.makeText(context, "Uploading file...", Toast.LENGTH_SHORT).show()

        CloudinaryHelper.uploadFile(
            context,
            fileUri,
            fileName,
            onSuccess = { fileUrl ->
                when (messageType) {
                    MessageType.ONE_TO_ONE -> {
                        sendFileMessage_OneToOne(
                            chatReference,
                            messagesReference,
                            senderID,
                            fileName,
                            fileSize,
                            fileUrl,
                            onSuccess,
                            onError
                        )
                    }
                    MessageType.GROUP -> {
                        sendFileMessage_Group(
                            chatReference,
                            messagesReference,
                            senderID,
                            senderName ?: "Unknown",
                            fileName,
                            fileSize,
                            fileUrl,
                            onSuccess,
                            onError
                        )
                    }
                    MessageType.COMMUNITY -> {
                        sendFileMessage_Community(
                            chatReference,
                            messagesReference,
                            senderID,
                            senderName ?: "Unknown",
                            fileName,
                            fileSize,
                            fileUrl,
                            onSuccess,
                            onError
                        )
                    }
                    MessageType.PRIVATE_TEMP -> {
                        if (encryptionKey == null) {
                            onError("Encryption key required for temp chat")
                            return@uploadFile
                        }
                        sendFileMessage_Temp(
                            chatReference,
                            messagesReference,
                            senderID,
                            fileName,
                            fileSize,
                            fileUrl,
                            encryptionKey,
                            onSuccess,
                            onError
                        )
                    }
                }
                Toast.makeText(context, "File sent!", Toast.LENGTH_SHORT).show()
            },
            onError = { error ->
                onError(error)
                Toast.makeText(context, "Upload failed: $error", Toast.LENGTH_LONG).show()
            }
        )
    }

    // ========== IMAGE MESSAGE SENDERS ==========

    private fun sendImageMessage_OneToOne(
        chatReference: DocumentReference,
        messagesReference: CollectionReference,
        senderID: String,
        imageUrl: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        chatReference.update(
            mapOf(
                "lastMsg" to "📷 Photo",
                "lastMsgSenderID" to senderID,
                "lastMsgTimestamp" to Timestamp.now()
            )
        )

        val msgModel = MsgModel(
            senderID,
            "📷 Photo",
            Timestamp.now(),
            imageUrl,
            "image"
        )

        messagesReference.add(msgModel)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onError(it.message ?: "Failed to send") }
    }

    private fun sendImageMessage_Group(
        chatReference: DocumentReference,
        messagesReference: CollectionReference,
        senderID: String,
        senderName: String,
        imageUrl: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        chatReference.update(
            mapOf(
                "lastMsg" to "📷 Photo",
                "lastMsgSenderID" to senderID,
                "lastMsgTimestamp" to Timestamp.now()
            )
        )

        val msgModel = GroupMsgModel(
            senderID,
            senderName,
            "📷 Photo",
            Timestamp.now(),
            imageUrl,
            "image"
        )

        messagesReference.add(msgModel)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onError(it.message ?: "Failed to send") }
    }

    private fun sendImageMessage_Community(
        chatReference: DocumentReference,
        messagesReference: CollectionReference,
        senderID: String,
        senderName: String,
        imageUrl: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        chatReference.update(
            mapOf(
                "lastMsg" to "📷 Photo",
                "lastMsgSenderID" to senderID,
                "lastMsgTimestamp" to Timestamp.now()
            )
        )

        val msgModel = CommunityMsgModel(
            senderID,
            senderName,
            "📷 Photo",
            Timestamp.now(),
            imageUrl,
            "image"
        )

        messagesReference.add(msgModel)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onError(it.message ?: "Failed to send") }
    }

    private fun sendImageMessage_Temp(
        chatReference: DocumentReference,
        messagesReference: CollectionReference,
        senderID: String,
        imageUrl: String,
        encryptionKey: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        try {
            val encryptedImageUrl = EncryptionUtils.encrypt(imageUrl, encryptionKey)
            val encryptedPhotoText = EncryptionUtils.encrypt("📷 Photo", encryptionKey)

            chatReference.update(
                mapOf(
                    "lastMsg" to "📷 Photo",
                    "lastMsgSenderID" to senderID,
                    "lastMsgTimestamp" to Timestamp.now()
                )
            )

            val msgModel = TempChatMsgModel(
                senderID,
                encryptedPhotoText,
                Timestamp.now(),
                encryptedImageUrl,
                "image"
            )

            messagesReference.add(msgModel)
                .addOnSuccessListener { onSuccess() }
                .addOnFailureListener { onError(it.message ?: "Failed to send") }
        } catch (e: Exception) {
            Log.e("MediaMessageHelper", "Encryption failed", e)
            onError("Encryption failed")
        }
    }

    // ========== FILE MESSAGE SENDERS ==========

    private fun sendFileMessage_OneToOne(
        chatReference: DocumentReference,
        messagesReference: CollectionReference,
        senderID: String,
        fileName: String,
        fileSize: Long,
        fileUrl: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        chatReference.update(
            mapOf(
                "lastMsg" to "📎 $fileName",
                "lastMsgSenderID" to senderID,
                "lastMsgTimestamp" to Timestamp.now()
            )
        )

        val msgModel = MsgModel(
            senderID,
            "📎 $fileName",
            Timestamp.now(),
            fileUrl,
            fileName,
            fileSize,
            "file"
        )

        messagesReference.add(msgModel)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onError(it.message ?: "Failed to send") }
    }

    private fun sendFileMessage_Group(
        chatReference: DocumentReference,
        messagesReference: CollectionReference,
        senderID: String,
        senderName: String,
        fileName: String,
        fileSize: Long,
        fileUrl: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        chatReference.update(
            mapOf(
                "lastMsg" to "📎 $fileName",
                "lastMsgSenderID" to senderID,
                "lastMsgTimestamp" to Timestamp.now()
            )
        )

        val msgModel = GroupMsgModel(
            senderID,
            senderName,
            "📎 $fileName",
            Timestamp.now(),
            fileUrl,
            fileName,
            fileSize,
            "file"
        )

        messagesReference.add(msgModel)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onError(it.message ?: "Failed to send") }
    }

    private fun sendFileMessage_Community(
        chatReference: DocumentReference,
        messagesReference: CollectionReference,
        senderID: String,
        senderName: String,
        fileName: String,
        fileSize: Long,
        fileUrl: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        chatReference.update(
            mapOf(
                "lastMsg" to "📎 $fileName",
                "lastMsgSenderID" to senderID,
                "lastMsgTimestamp" to Timestamp.now()
            )
        )

        val msgModel = CommunityMsgModel(
            senderID,
            senderName,
            "📎 $fileName",
            Timestamp.now(),
            fileUrl,
            fileName,
            fileSize,
            "file"
        )

        messagesReference.add(msgModel)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onError(it.message ?: "Failed to send") }
    }

    private fun sendFileMessage_Temp(
        chatReference: DocumentReference,
        messagesReference: CollectionReference,
        senderID: String,
        fileName: String,
        fileSize: Long,
        fileUrl: String,
        encryptionKey: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        try {
            val encryptedFileUrl = EncryptionUtils.encrypt(fileUrl, encryptionKey)
            val encryptedFileName = EncryptionUtils.encrypt(fileName, encryptionKey)
            val encryptedMsg = EncryptionUtils.encrypt("📎 $fileName", encryptionKey)

            chatReference.update(
                mapOf(
                    "lastMsg" to "📎 $fileName",
                    "lastMsgSenderID" to senderID,
                    "lastMsgTimestamp" to Timestamp.now()
                )
            )

            val msgModel = TempChatMsgModel(
                senderID,
                encryptedMsg,
                Timestamp.now(),
                encryptedFileUrl,
                encryptedFileName,
                fileSize,
                "file"
            )

            messagesReference.add(msgModel)
                .addOnSuccessListener { onSuccess() }
                .addOnFailureListener { onError(it.message ?: "Failed to send") }
        } catch (e: Exception) {
            Log.e("MediaMessageHelper", "Encryption failed", e)
            onError("Encryption failed")
        }
    }

    // ========== UTILITY METHODS ==========

    private fun generateImageHash(context: Context, uri: Uri): String {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri)
            val digest = MessageDigest.getInstance("MD5")
            val buffer = ByteArray(8192)
            var read: Int
            while (inputStream?.read(buffer).also { read = it ?: -1 } != -1) {
                digest.update(buffer, 0, read)
            }
            inputStream?.close()
            digest.digest().joinToString("") { "%02x".format(it) }
        } catch (e: Exception) {
            UUID.randomUUID().toString()
        }
    }

    private fun getFileName(context: Context, uri: Uri): String {
        var fileName = "file"
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (nameIndex != -1) {
                    fileName = cursor.getString(nameIndex)
                }
            }
        }
        return fileName
    }

    private fun getFileSize(context: Context, uri: Uri): Long {
        var fileSize = 0L
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                if (sizeIndex != -1) {
                    fileSize = cursor.getLong(sizeIndex)
                }
            }
        }
        return fileSize
    }

    fun formatFileSize(size: Long): String {
        return when {
            size < 1024 -> "$size B"
            size < 1024 * 1024 -> "${size / 1024} KB"
            else -> String.format("%.2f MB", size / (1024.0 * 1024.0))
        }
    }

    enum class MessageType {
        ONE_TO_ONE,
        GROUP,
        COMMUNITY,
        PRIVATE_TEMP
    }
}