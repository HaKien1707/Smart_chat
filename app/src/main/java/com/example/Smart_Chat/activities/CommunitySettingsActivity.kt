package com.example.Smart_Chat.activities

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.Smart_Chat.R
import com.example.Smart_Chat.models.CommunityModel
import com.example.Smart_Chat.utils.FireBase_utils
import com.example.Smart_Chat.utils.LanguageManager
import com.example.Smart_Chat.utils.ThemeManager
import com.example.Smart_Chat.utils.androidUtils
import com.github.dhaval2404.imagepicker.ImagePicker

class CommunitySettingsActivity : AppCompatActivity() {

    private lateinit var backBtn: ImageButton
    private lateinit var communityImage: ImageView
    private lateinit var communityNameInput: EditText
    private lateinit var saveBtn: Button
    private lateinit var banUserBtn: Button
    private lateinit var unbanUserBtn: Button
    private lateinit var deleteCommunityBtn: Button

    private var communityID: String? = null
    private var community: CommunityModel? = null
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
        setContentView(R.layout.activity_community_settings)

        communityID = intent.getStringExtra("communityID")

        if (communityID == null) {
            finish()
            return
        }

        // Initialize views
        backBtn = findViewById(R.id.back_btn)
        communityImage = findViewById(R.id.community_image)
        communityNameInput = findViewById(R.id.community_name_input)
        saveBtn = findViewById(R.id.save_btn)
        banUserBtn = findViewById(R.id.ban_user_btn)
        unbanUserBtn = findViewById(R.id.unban_user_btn)
        deleteCommunityBtn = findViewById(R.id.delete_community_btn)

        backBtn.setOnClickListener {
            finish()
        }

        communityImage.setOnClickListener {
            pickImage()
        }

        saveBtn.setOnClickListener {
            saveCommunityChanges()
        }

        banUserBtn.setOnClickListener {
            val intent = Intent(this, BanUserActivity::class.java)
            intent.putExtra("communityID", communityID)
            startActivity(intent)
        }

        unbanUserBtn.setOnClickListener {
            val intent = Intent(this, UnbanUserActivity::class.java)
            intent.putExtra("communityID", communityID)
            startActivity(intent)
        }

        deleteCommunityBtn.setOnClickListener {
            showDeleteCommunityDialog()
        }

        loadCommunityDetails()
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

    private fun loadCommunityDetails() {
        FireBase_utils.getCommunityReference(communityID!!).get()
            .addOnSuccessListener { document ->
                community = document.toObject(CommunityModel::class.java)

                communityNameInput.setText(community?.communityName)

                // Load community image
                val imageUrl = community?.communityImage
                if (!imageUrl.isNullOrBlank()) {
                    androidUtils.setProfileImageFromBase64(this, imageUrl, communityImage)
                } else {
                    communityImage.setImageResource(R.drawable.ic_community)
                }
            }
            .addOnFailureListener { e ->
                Log.e("CommunitySettings", "Failed to load community", e)
                Toast.makeText(this, "Failed to load community", Toast.LENGTH_SHORT).show()
            }
    }

    private fun saveCommunityChanges() {
        val newName = communityNameInput.text.toString().trim()

        if (newName.isEmpty()) {
            Toast.makeText(this, "Community name cannot be empty", Toast.LENGTH_SHORT).show()
            return
        }

        saveBtn.isEnabled = false

        val updates = mutableMapOf<String, Any>(
            "communityName" to newName
        )

        // Only update image if a new one was selected
        if (selectedImageBase64 != null) {
            updates["communityImage"] = selectedImageBase64!!
        }

        FireBase_utils.getCommunityReference(communityID!!)
            .update(updates)
            .addOnSuccessListener {
                Toast.makeText(this, "Community updated successfully", Toast.LENGTH_SHORT).show()
                setResult(RESULT_OK)
                finish()
            }
            .addOnFailureListener { e ->
                Log.e("CommunitySettings", "Failed to update community", e)
                Toast.makeText(this, "Failed to update community", Toast.LENGTH_SHORT).show()
                saveBtn.isEnabled = true
            }
    }

    private fun showDeleteCommunityDialog() {
        AlertDialog.Builder(this)
            .setTitle("Delete Community")
            .setMessage("Are you sure you want to permanently delete this community? This action cannot be undone.")
            .setPositiveButton("Delete") { _, _ ->
                deleteCommunity()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun deleteCommunity() {
        deleteCommunityBtn.isEnabled = false

        // First delete all messages
        FireBase_utils.getCommunityMessagesReference(communityID!!)
            .get()
            .addOnSuccessListener { messages ->
                val batch = com.google.firebase.firestore.FirebaseFirestore.getInstance().batch()

                // Add all message deletions to batch
                messages.documents.forEach { doc ->
                    batch.delete(doc.reference)
                }

                // Add community deletion to batch
                batch.delete(FireBase_utils.getCommunityReference(communityID!!))

                // Commit batch delete
                batch.commit()
                    .addOnSuccessListener {
                        Toast.makeText(this, "Community deleted", Toast.LENGTH_SHORT).show()
                        setResult(RESULT_OK)
                        finish()
                    }
                    .addOnFailureListener { e ->
                        Log.e("CommunitySettings", "Failed to delete community", e)
                        Toast.makeText(this, "Failed to delete community", Toast.LENGTH_SHORT).show()
                        deleteCommunityBtn.isEnabled = true
                    }
            }
            .addOnFailureListener { e ->
                Log.e("CommunitySettings", "Failed to fetch messages", e)
                Toast.makeText(this, "Failed to delete community", Toast.LENGTH_SHORT).show()
                deleteCommunityBtn.isEnabled = true
            }
    }
}