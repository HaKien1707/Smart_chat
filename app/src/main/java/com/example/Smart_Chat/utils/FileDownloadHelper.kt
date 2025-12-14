package com.example.Smart_Chat.utils

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast

object FileDownloadHelper {

    fun showDownloadDialog(
        context: Context,
        fileName: String,
        fileSize: Long,
        fileUrl: String,
        onDownload: () -> Unit = {}
    ) {
        val formattedSize = formatFileSize(fileSize)

        AlertDialog.Builder(context)
            .setTitle("Download File?")
            .setMessage("$fileName\nSize: $formattedSize\n\nDo you want to download this file?")
            .setPositiveButton("Download") { dialog, _ ->
                dialog.dismiss()
                openFile(context, fileUrl, fileName)
                onDownload()
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    private fun openFile(context: Context, fileUrl: String, fileName: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                data = Uri.parse(fileUrl)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(
                context,
                "No app found to open this file",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun formatFileSize(size: Long): String {
        return when {
            size < 1024 -> "$size B"
            size < 1024 * 1024 -> "${size / 1024} KB"
            else -> String.format("%.2f MB", size / (1024.0 * 1024.0))
        }
    }
}