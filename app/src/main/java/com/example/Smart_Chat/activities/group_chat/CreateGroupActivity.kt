package com.example.Smart_Chat.activities.group_chat

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.Smart_Chat.R
import com.example.Smart_Chat.adapters.SelectMemberAdapter
import com.example.Smart_Chat.models.groupModel
import com.example.Smart_Chat.models.userModel
import com.example.Smart_Chat.utils.FireBase_utils
import com.example.Smart_Chat.utils.LanguageManager
import com.example.Smart_Chat.utils.ThemeManager
import com.example.Smart_Chat.utils.androidUtils
import com.github.dhaval2404.imagepicker.ImagePicker
import com.google.firebase.Timestamp
import java.util.UUID

class CreateGroupActivity : AppCompatActivity() {

    private lateinit var backBtn: ImageButton
    private lateinit var groupImage: ImageView
    private lateinit var groupNameInput: EditText
    private lateinit var memberRecycler: RecyclerView
    private lateinit var createBtn: Button

    private var selectedImageBase64: String? = null
    private val selectedMembers = mutableListOf<String>()
    private lateinit var memberAdapter: SelectMemberAdapter

    private val imagePickerLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result: ActivityResult ->
            if (result.resultCode == RESULT_OK) {
                val uri = result.data?.data
                if (uri != null) {
                    selectedImageBase64 = androidUtils.convertImageToBase64(this, uri)
                    groupImage.setImageURI(uri)
                }
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        // Apply theme and language
        ThemeManager.applySavedTheme(this)
        LanguageManager.applySavedLanguage(this)

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create_group)

        backBtn = findViewById(R.id.back_btn)
        groupImage = findViewById(R.id.group_image)
        groupNameInput = findViewById(R.id.group_name_input)
        memberRecycler = findViewById(R.id.member_recycler)
        createBtn = findViewById(R.id.create_btn)

        backBtn.setOnClickListener {
            finish()
        }

        groupImage.setOnClickListener {
            ImagePicker.with(this)
                .cropSquare()
                .compress(512)
                .maxResultSize(512, 512)
                .createIntent { intent -> imagePickerLauncher.launch(intent) }
        }

        createBtn.setOnClickListener {
            createGroup()
        }

        setupMemberRecycler()
    }

    private fun setupMemberRecycler() {
        // Load all users except current user
        FireBase_utils.allUsersCollection()
            .get()
            .addOnSuccessListener { documents ->
                val users = mutableListOf<userModel>()
                for (doc in documents) {
                    val user = doc.toObject(userModel::class.java)
                    if (user.userID != FireBase_utils.currentUserID()) {
                        users.add(user)
                    }
                }

                memberAdapter = SelectMemberAdapter(users, this) { userID, isSelected ->
                    if (isSelected) {
                        selectedMembers.add(userID)
                    } else {
                        selectedMembers.remove(userID)
                    }
                    updateCreateButton()
                }

                memberRecycler.layoutManager = LinearLayoutManager(this)
                memberRecycler.adapter = memberAdapter
            }
    }

    private fun updateCreateButton() {
        createBtn.isEnabled = selectedMembers.size >= 2
        createBtn.text = if (selectedMembers.size >= 2) {
            "Create Group (${selectedMembers.size} members)"
        } else {
            "Select at least 2 members"
        }
    }

    private fun convertImageToBase64(uri: Uri) {
        try {
            selectedImageBase64 = androidUtils.convertImageToBase64(this, uri)
        } catch (e: Exception) {
            Log.e("CreateGroup", "Failed to convert image", e)
        }
    }

    private fun createGroup() {
        val groupName = groupNameInput.text.toString().trim()

        if (groupName.isEmpty()) {
            groupNameInput.error = "Enter group name"
            return
        }

        if (selectedMembers.size < 2) {
            Toast.makeText(this, "Select at least 2 members", Toast.LENGTH_SHORT).show()
            return
        }

        // Disable button while creating
        createBtn.isEnabled = false
        createBtn.text = "Creating..."

        // Generate group ID
        val groupID = UUID.randomUUID().toString()

        // Add current user to members
        val allMembers = mutableListOf(FireBase_utils.currentUserID())
        allMembers.addAll(selectedMembers)

        // Creator is admin
        val adminIDs = mutableListOf(FireBase_utils.currentUserID())

        // Create group model
        val group = groupModel(
            groupID,
            groupName,
            selectedImageBase64 ?: "",
            allMembers,
            adminIDs,
            Timestamp.now(),
            FireBase_utils.currentUserID()
        )

        // Save to Firestore
        FireBase_utils.getGroupReference(groupID)
            .set(group)
            .addOnSuccessListener {
                Toast.makeText(this, "Group created!", Toast.LENGTH_SHORT).show()

                // Open the group chat
                val intent = Intent(this, GroupChatActivity::class.java)
                intent.putExtra("groupID", groupID)
                intent.putExtra("groupName", groupName)
                startActivity(intent)
                finish()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Failed: ${e.message}", Toast.LENGTH_SHORT).show()
                createBtn.isEnabled = true
                updateCreateButton()
            }
    }
}