package com.example.smart_chat.utils.media

import android.content.Context
import android.net.Uri
import android.util.Log
import com.cloudinary.android.MediaManager
import com.cloudinary.android.callback.ErrorInfo
import com.cloudinary.android.callback.UploadCallback
import com.example.smart_chat.utils.firebase.FirebaseAuthentication
import kotlin.collections.get

object CloudinaryHelper {

    private var isInitialized = false

    fun initCloudinary(context: Context) {
        if (!isInitialized) {
            val config = hashMapOf(
                "cloud_name" to "dtfu6e0ia",  // Replace with your cloud name
                "api_key" to "847358819539111",        // Replace with your API key
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
            initCloudinary(context)

            Log.d("Cloudinary", "Starting upload: $imageUri")

            // ✅ Pass the URI directly - MediaManager handles it
            MediaManager.get().upload(imageUri)
                .option("folder", "chat_images")
                .option("resource_type", "image")
                .callback(object : UploadCallback {
                    override fun onStart(requestId: String) {
                        Log.d("Cloudinary", "Upload started: $requestId")
                    }

                    override fun onProgress(requestId: String, bytes: Long, totalBytes: Long) {
                        if (totalBytes <= 0) {
                            Log.d("Cloudinary", "Upload progress: waiting for total size… ($bytes bytes uploaded)")
                            return
                        }

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

    fun uploadImageWithHash(
        context: Context,
        imageUri: Uri,
        publicId: String,
        onSuccess: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        try {
            initCloudinary(context)

            MediaManager.get().upload(imageUri)
                .option("folder", "chat_images/${FirebaseAuthentication.currentUserID()}")
                .option("resource_type", "image")
                .option("public_id", publicId) // Use hash as public_id
                .option("resource_type", "image")
                .option("overwrite", false) // Don't overwrite if exists
                .callback(object : UploadCallback {
                    override fun onSuccess(requestId: String, resultData: Map<*, *>) {
                        val url = resultData["secure_url"] as? String
                        if (url != null) {
                            onSuccess(url)
                        } else {
                            onError("Failed to get image URL")
                        }
                    }

                    override fun onError(requestId: String, error: ErrorInfo) {
                        onError(error.description ?: "Upload failed")
                    }

                    override fun onStart(requestId: String) {}
                    override fun onProgress(requestId: String, bytes: Long, totalBytes: Long) {}
                    override fun onReschedule(requestId: String, error: ErrorInfo) {}
                })
                .dispatch()

        } catch (e: Exception) {
            onError(e.message ?: "Upload failed")
        }
    }

    fun uploadFile(
        context: Context,
        fileUri: Uri,
        fileName: String,
        onSuccess: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        try {
            initCloudinary(context)

            Log.d("Cloudinary", "Starting file upload: $fileName")

            MediaManager.get().upload(fileUri)
                .option("folder", "chat_files/${FirebaseAuthentication.currentUserID()}")
                .option("resource_type", "auto") // Auto-detect file type
                .option("public_id", System.currentTimeMillis().toString() + "_" + fileName)
                .callback(object : UploadCallback {
                    override fun onStart(requestId: String) {
                        Log.d("Cloudinary", "File upload started: $requestId")
                    }

                    override fun onProgress(requestId: String, bytes: Long, totalBytes: Long) {
                        if (totalBytes > 0) {
                            val progress = (bytes * 100 / totalBytes).toInt()
                            Log.d("Cloudinary", "File upload progress: $progress%")
                        }
                    }

                    override fun onSuccess(requestId: String, resultData: Map<*, *>) {
                        val url = resultData["secure_url"] as? String
                        Log.d("Cloudinary", "File upload successful: $url")
                        if (url != null) {
                            onSuccess(url)
                        } else {
                            onError("Failed to get file URL")
                        }
                    }

                    override fun onError(requestId: String, error: ErrorInfo) {
                        Log.e("Cloudinary", "File upload failed: ${error.description}")
                        onError(error.description ?: "File upload failed")
                    }

                    override fun onReschedule(requestId: String, error: ErrorInfo) {
                        Log.w("Cloudinary", "File upload rescheduled: ${error.description}")
                    }
                })
                .dispatch()

        } catch (e: Exception) {
            Log.e("Cloudinary", "File upload error", e)
            onError(e.message ?: "File upload failed")
        }
    }
}