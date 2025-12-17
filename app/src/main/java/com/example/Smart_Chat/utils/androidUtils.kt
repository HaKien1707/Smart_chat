package com.example.Smart_Chat.utils

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.MediaStore
import android.util.Base64
import android.util.Log
import android.widget.ImageView
import android.widget.Toast
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.example.Smart_Chat.models.userModel
import com.google.firebase.Timestamp
import java.io.ByteArrayOutputStream
import java.text.SimpleDateFormat

object androidUtils {
    @JvmStatic
    fun showToast(context: Context?, message: String?) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }

    @JvmStatic
    fun passUserModelAsIntent(intent: Intent, user: userModel?) {
        intent.putExtra("userID", user?.userID)
        intent.putExtra("username", user?.username)
        intent.putExtra("phoneNumber", user?.phoneNumber)
        intent.putExtra("profileImage", user?.profileImage)
        intent.putExtra("fcmToken", user?.fcmToken)
    }

    @JvmStatic
    fun getUserModelFromIntent(intent: Intent): userModel {
        val UserModel = userModel()
        UserModel.userID = intent.getStringExtra("userID")
        UserModel.username = intent.getStringExtra("username")
        UserModel.phoneNumber = intent.getStringExtra("phoneNumber")
        UserModel.profileImage = intent.getStringExtra("profileImage")
        UserModel.fcmToken = intent.getStringExtra("fcmToken")
        return UserModel
    }

    @JvmStatic
    fun timestampToString(timestamp: Timestamp?): String {
        return SimpleDateFormat("HH:MM").format(timestamp?.toDate())
    }

    @JvmStatic
    fun setProfileImage(context: Context, uri: Uri?, imageView: ImageView) {
        Glide.with(context).load(uri).apply(RequestOptions.circleCropTransform()).into(imageView)
    }

    @JvmStatic
    fun setProfileImageFromBase64(context: Context, base64: String?, imageView: ImageView) {
        if (base64 == null || base64.isEmpty()) return

        val decoded = Base64.decode(base64, Base64.DEFAULT)
        val bitmap = BitmapFactory.decodeByteArray(decoded, 0, decoded.size)

        Glide.with(context)
            .load(bitmap)
            .apply(RequestOptions.circleCropTransform())
            .into(imageView)
    }

    /**
     * Convert image URI to Base64 string
     * @param context Application context
     * @param uri Image URI
     * @param maxSize Maximum size for the image (default 200x200)
     * @param quality JPEG compression quality (0-100, default 40)
     * @return Base64 encoded string or null if conversion fails
     */
    @JvmStatic
    fun convertImageToBase64(
        context: Context,
        uri: Uri,
        maxSize: Int = 200,
        quality: Int = 40
    ): String? {
        return try {
            val bitmap = MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
            val resized = Bitmap.createScaledBitmap(bitmap, maxSize, maxSize, true)

            val baos = ByteArrayOutputStream()
            resized.compress(Bitmap.CompressFormat.JPEG, quality, baos)
            Base64.encodeToString(baos.toByteArray(), Base64.DEFAULT)
        } catch (e: Exception) {
            Log.e("androidUtils", "Failed to convert image to Base64", e)
            null
        }
    }

    @JvmStatic
    fun getFileInfo(context: Context, uri: Uri): FileInfo {
        var fileName = "unknown"
        var fileSize = 0L

        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                val sizeIndex = cursor.getColumnIndex(android.provider.OpenableColumns.SIZE)

                if (nameIndex != -1) {
                    fileName = cursor.getString(nameIndex)
                }
                if (sizeIndex != -1) {
                    fileSize = cursor.getLong(sizeIndex)
                }
            }
        }

        return FileInfo(fileName, fileSize)
    }

    /**
     * Data class to hold file information
     */
    data class FileInfo(
        val name: String,
        val size: Long
    )
}