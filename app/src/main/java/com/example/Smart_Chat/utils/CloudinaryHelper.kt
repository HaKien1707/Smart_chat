package com.example.Smart_Chat.utils

import android.content.Context
import android.net.Uri
import android.util.Log
import com.cloudinary.android.MediaManager
import com.cloudinary.android.callback.ErrorInfo
import com.cloudinary.android.callback.UploadCallback
import java.io.File

object CloudinaryHelper {

    private var isInitialized = false

    fun initCloudinary(context: Context) {
        if (!isInitialized) {
            val config = hashMapOf(
                "cloud_name" to "Root",  // Replace with your cloud name
                "api_key" to "8847358819539111",        // Replace with your API key
                "api_secret" to "nyFKBIj_khUl-CxCFUveSl6AE6Y"   // Replace with your API secret
            )
            MediaManager.init(context, config)
            isInitialized = true
            Log.d("Cloudinary", "Initialized successfully")
        }
    }

    fun uploadImage(
        context: Context,
        imageUri: Uri,
        onSuccess: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        try {
            // Initialize if not already done
            initCloudinary(context)

            // Get file path from URI
            val file = File(imageUri.path ?: "")

            Log.d("Cloudinary", "Starting upload: ${file.absolutePath}")

            MediaManager.get().upload(imageUri)
                .option("folder", "chat_images")
                .option("resource_type", "image")
                .callback(object : UploadCallback {
                    override fun onStart(requestId: String) {
                        Log.d("Cloudinary", "Upload started: $requestId")
                    }

                    override fun onProgress(requestId: String, bytes: Long, totalBytes: Long) {
                        val progress = (bytes * 100 / totalBytes).toInt()
                        Log.d("Cloudinary", "Upload progress: $progress%")
                    }

                    override fun onSuccess(requestId: String, resultData: Map<*, *>) {
                        val url = resultData["secure_url"] as? String
                        Log.d("Cloudinary", "Upload successful: $url")
                        if (url != null) {
                            onSuccess(url)
                        } else {
                            onError("Failed to get image URL")
                        }
                    }

                    override fun onError(requestId: String, error: ErrorInfo) {
                        Log.e("Cloudinary", "Upload failed: ${error.description}")
                        onError(error.description ?: "Upload failed")
                    }

                    override fun onReschedule(requestId: String, error: ErrorInfo) {
                        Log.w("Cloudinary", "Upload rescheduled: ${error.description}")
                    }
                })
                .dispatch()

        } catch (e: Exception) {
            Log.e("Cloudinary", "Upload error", e)
            onError(e.message ?: "Upload failed")
        }
    }
}