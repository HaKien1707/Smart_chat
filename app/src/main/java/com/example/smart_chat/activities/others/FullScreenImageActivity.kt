package com.example.smart_chat.activities.others

import android.Manifest
import android.app.DownloadManager
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.example.smart_chat.R
import com.example.smart_chat.utils.UI.LanguageManager
import com.example.smart_chat.utils.UI.ThemeManager
import com.github.chrisbanes.photoview.PhotoView

class FullScreenImageActivity : AppCompatActivity() {

    private lateinit var photoView: PhotoView
    private lateinit var closeBtn: ImageButton
    private lateinit var downloadBtn: ImageButton
    private var imageUrl: String? = null

    companion object {
        private const val PERMISSION_REQUEST_CODE = 1001
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        // Apply theme and language
        ThemeManager.applySavedTheme(this)
        LanguageManager.applySavedLanguage(this)

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_fullscreen_image)

        // Get image URL from intent
        imageUrl = intent.getStringExtra("imageUrl")

        // Initialize views
        photoView = findViewById(R.id.photo_view)
        closeBtn = findViewById(R.id.close_btn)
        downloadBtn = findViewById(R.id.download_btn)

        // Set click listeners
        closeBtn.setOnClickListener {
            finish()
        }

        downloadBtn.setOnClickListener {
            checkPermissionAndDownload()
        }

        // Load image
        loadImage()
    }

    private fun loadImage() {
        if (imageUrl.isNullOrEmpty()) {
            Toast.makeText(this, "No image URL provided", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        Glide.with(this)
            .load(imageUrl)
            .placeholder(R.drawable.ic_image_loading)
            .error(R.drawable.ic_image_error)
            .listener(object : RequestListener<Drawable> {
                override fun onLoadFailed(
                    e: GlideException?,
                    model: Any?,
                    target: Target<Drawable>?,
                    isFirstResource: Boolean
                ): Boolean {
                    Toast.makeText(
                        this@FullScreenImageActivity,
                        "Failed to load image",
                        Toast.LENGTH_SHORT
                    ).show()
                    return false
                }

                override fun onResourceReady(
                    resource: Drawable?,
                    model: Any?,
                    target: Target<Drawable>?,
                    dataSource: DataSource?,
                    isFirstResource: Boolean
                ): Boolean {
                    return false
                }
            })
            .into(photoView)
    }

    private fun checkPermissionAndDownload() {
        // Android 10+ doesn't need WRITE_EXTERNAL_STORAGE
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            downloadImage()
        } else {
            // Check permission for older Android versions
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                downloadImage()
            } else {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                    PERMISSION_REQUEST_CODE
                )
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                downloadImage()
            } else {
                Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun downloadImage() {
        if (imageUrl.isNullOrEmpty()) {
            Toast.makeText(this, "No image to download", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            val request = DownloadManager.Request(Uri.parse(imageUrl))
                .setTitle("Smart Chat Image")
                .setDescription("Downloading image...")
                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                .setDestinationInExternalPublicDir(
                    Environment.DIRECTORY_DOWNLOADS,
                    "SmartChat_${System.currentTimeMillis()}.jpg"
                )
                .setAllowedOverMetered(true)
                .setAllowedOverRoaming(true)

            val downloadManager = getSystemService(DOWNLOAD_SERVICE) as DownloadManager
            downloadManager.enqueue(request)

            Toast.makeText(this, "Downloading image...", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Log.e("FullScreenImage", "Download failed", e)
            Toast.makeText(this, "Download failed: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
}