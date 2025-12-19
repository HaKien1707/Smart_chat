package com.example.Smart_Chat.utils.media

import android.app.AlertDialog
import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.widget.Toast

object FileDownloadHelper {

    fun showDownloadDialog(
        context: Context,
        fileName: String,
        fileSize: Long,
        fileUrl: String
    ) {
        val formattedSize = formatFileSize(fileSize)

        AlertDialog.Builder(context)
            .setTitle("Download File?")
            .setMessage("$fileName\nSize: $formattedSize\n\nDo you want to download this file?")
            .setPositiveButton("Download") { dialog, _ ->
                dialog.dismiss()
                downloadFile(context, fileUrl, fileName)
            }
            .setNegativeButton("Open in Browser") { dialog, _ ->
                dialog.dismiss()
                openInBrowser(context, fileUrl)
            }
            .setNeutralButton("Cancel", null)
            .show()
    }

    private fun downloadFile(context: Context, fileUrl: String, fileName: String) {
        try {
            val request = DownloadManager.Request(Uri.parse(fileUrl)).apply {
                setTitle(fileName)
                setDescription("Downloading file...")
                setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName)
                setAllowedOverMetered(true)
                setAllowedOverRoaming(true)
            }

            val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            downloadManager.enqueue(request)

            Toast.makeText(context, "Downloading $fileName...", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(context, "Download failed: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun openInBrowser(context: Context, fileUrl: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                data = Uri.parse(fileUrl)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "Could not open file", Toast.LENGTH_SHORT).show()
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