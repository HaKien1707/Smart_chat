package com.example.Smart_Chat.activities.community

import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.example.Smart_Chat.R
import com.example.Smart_Chat.utils.LanguageManager
import com.example.Smart_Chat.utils.ThemeManager
import com.example.Smart_Chat.utils.androidUtils
import com.example.Smart_Chat.utils.firebase.FirebaseCommunity
import com.github.dhaval2404.imagepicker.ImagePicker

class CreateCommunityActivity : AppCompatActivity() {

    private lateinit var backBtn: ImageButton
    private lateinit var communityImage: ImageView
    private lateinit var communityNameInput: EditText
    private lateinit var communityDescInput: EditText
    private lateinit var createBtn: Button

    private var selectedImageBase64: String? = null

    private val imagePickerLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                val uri = result.data?.data
                if (uri != null) {
                    handleImageSelection(uri)
                }
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        ThemeManager.applySavedTheme(this)
        LanguageManager.applySavedLanguage(this)

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create_community)

        backBtn = findViewById(R.id.back_btn)
        communityImage = findViewById(R.id.community_image)
        communityNameInput = findViewById(R.id.community_name_input)
        communityDescInput = findViewById(R.id.community_desc_input)
        createBtn = findViewById(R.id.create_btn)

        backBtn.setOnClickListener {
            finish()
        }

        communityImage.setOnClickListener {
            pickImage()
        }

        createBtn.setOnClickListener {
            createCommunity()
        }
    }

    private fun pickImage() {
        ImagePicker.with(this)
            .compress(512)
            .maxResultSize(1080, 1080)
            .createIntent { intent -> imagePickerLauncher.launch(intent) }
    }

    private fun handleImageSelection(uri: Uri) {
        selectedImageBase64 = androidUtils.convertImageToBase64(this, uri)
        communityImage.setImageURI(uri)
    }

    private fun createCommunity() {
        val name = communityNameInput.text.toString().trim()
        val description = communityDescInput.text.toString().trim()

        if (name.isEmpty()) {
            Toast.makeText(this, "Please enter community name", Toast.LENGTH_SHORT).show()
            return
        }

        if (description.isEmpty()) {
            Toast.makeText(this, "Please enter community description", Toast.LENGTH_SHORT).show()
            return
        }

        createBtn.isEnabled = false

        FirebaseCommunity.createCommunity(
            name,
            description,
            selectedImageBase64,
            onSuccess = { communityID ->
                Toast.makeText(this, "Community created successfully", Toast.LENGTH_SHORT).show()
                finish()
            },
            onFailure = { e ->
                Log.e("CreateCommunity", "Failed to create community", e)
                Toast.makeText(this, "Failed to create community", Toast.LENGTH_SHORT).show()
                createBtn.isEnabled = true
            }
        )
    }
}