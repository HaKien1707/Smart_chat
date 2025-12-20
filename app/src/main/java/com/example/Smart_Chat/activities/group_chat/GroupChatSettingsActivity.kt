package com.example.Smart_Chat.activities.group_chat

import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Base64
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.Smart_Chat.R
import com.example.Smart_Chat.models.group.groupModel
import com.example.Smart_Chat.models.userModel
import com.example.Smart_Chat.utils.UI.LanguageManager
import com.example.Smart_Chat.utils.UI.ThemeManager
import com.example.Smart_Chat.utils.others.androidUtils
import com.example.Smart_Chat.utils.firebase.*
import com.github.dhaval2404.imagepicker.ImagePicker
import com.google.firebase.firestore.FirebaseFirestore
import java.io.ByteArrayOutputStream

class GroupChatSettingsActivity : AppCompatActivity() {

    private lateinit var backBtn: ImageButton
    private lateinit var groupImage: ImageView
    private lateinit var groupNameInput: EditText
    private lateinit var saveNameBtn: Button
    private lateinit var addMemberBtn: Button
    private lateinit var viewMembersBtn: Button
    private lateinit var leaveGroupBtn: Button
    private lateinit var deleteGroupBtn: Button

    private var groupID: String? = null
    private var group: groupModel? = null
    private var isAdmin = false

    private val imagePickerLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result: ActivityResult ->
            if (result.resultCode == RESULT_OK) {
                val uri = result.data?.data
                if (uri != null) {
                    updateGroupImage(uri)
                }
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        // Apply theme and language
        ThemeManager.applySavedTheme(this)
        LanguageManager.applySavedLanguage(this)

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_group_settings)

        groupID = intent.getStringExtra("groupID")

        if (groupID == null) {
            Toast.makeText(this, "Error loading group", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        initViews()
        loadGroupDetails()
        setupClickListeners()
    }

    override fun onResume() {
        super.onResume()
    }

    private lateinit var blockedListBtn: Button

    private fun initViews() {
        backBtn = findViewById(R.id.back_btn)
        groupImage = findViewById(R.id.group_image)
        groupNameInput = findViewById(R.id.group_name_input)
        saveNameBtn = findViewById(R.id.save_name_btn)
        addMemberBtn = findViewById(R.id.add_member_btn)
        viewMembersBtn = findViewById(R.id.view_members_btn)
        blockedListBtn = findViewById(R.id.blocked_list_btn)
        leaveGroupBtn = findViewById(R.id.leave_group_btn)
        deleteGroupBtn = findViewById(R.id.delete_group_btn)
    }

    private fun setupClickListeners() {
        backBtn.setOnClickListener { finish() }
        groupImage.setOnClickListener {
            if (isAdmin) {
                ImagePicker.with(this)
                    .cropSquare()
                    .compress(512)
                    .maxResultSize(512, 512)
                    .createIntent { intent -> imagePickerLauncher.launch(intent) }
            } else {
                Toast.makeText(this, "Only admins can change group photo", Toast.LENGTH_SHORT).show()
            }
        }

        saveNameBtn.setOnClickListener {
            if (isAdmin) {
                updateGroupName()
            } else {
                Toast.makeText(this, "Only admins can change group name", Toast.LENGTH_SHORT).show()
            }
        }

        addMemberBtn.setOnClickListener {
            // No need to check isAdmin - button only visible for admins
            val intent = Intent(this, AddMembersActivity::class.java)
            intent.putExtra("groupID", groupID)
            startActivity(intent)
        }

        viewMembersBtn.setOnClickListener {
            val intent = Intent(this, GroupMembersActivity::class.java)
            intent.putExtra("groupID", groupID)
            startActivity(intent)
        }

        blockedListBtn.setOnClickListener {
            val intent = Intent(this, BlockedMembersActivity::class.java)
            intent.putExtra("groupID", groupID)
            startActivity(intent)
        }

        leaveGroupBtn.setOnClickListener {
            showLeaveGroupDialog()
        }

        deleteGroupBtn.setOnClickListener {
            showDeleteGroupDialog()
        }
    }

    private fun loadGroupDetails() {
        FirebaseGroups.getGroupReference(groupID!!).get()
            .addOnSuccessListener { document ->
                group = document.toObject(groupModel::class.java)

                if (group == null) {
                    Toast.makeText(this, "Group not found", Toast.LENGTH_SHORT).show()
                    finish()
                    return@addOnSuccessListener
                }

                isAdmin = group?.adminIDs?.contains(FirebaseAuthentication.currentUserID()) == true

                // Load group image
                if (!group?.groupImage.isNullOrEmpty()) {
                    androidUtils.setProfileImageFromBase64(
                        this,
                        group?.groupImage!!,
                        groupImage
                    )
                }

                groupNameInput.setText(group?.groupName)

                if (isAdmin) {
                    addMemberBtn.visibility = View.VISIBLE
                    blockedListBtn.visibility = View.VISIBLE  // Show for admins
                    deleteGroupBtn.visibility = View.VISIBLE
                    leaveGroupBtn.visibility = View.GONE
                    groupImage.isClickable = true
                    saveNameBtn.visibility = View.VISIBLE
                } else {
                    addMemberBtn.visibility = View.GONE
                    blockedListBtn.visibility = View.GONE  // Hide for members
                    deleteGroupBtn.visibility = View.GONE
                    leaveGroupBtn.visibility = View.VISIBLE
                    groupImage.isClickable = false
                    saveNameBtn.visibility = View.GONE
                }

                loadMembers()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to load group", Toast.LENGTH_SHORT).show()
                finish()
            }
    }

    private fun loadMembers() {
        val memberIDs = group?.memberIDs ?: return
        val members = mutableListOf<Pair<userModel, Boolean>>() // Pair<user, isAdmin>

        memberIDs.forEach { memberID ->
            if (memberID != null) {
                FirebaseAuthentication.allUsersCollection().document(memberID).get()
                    .addOnSuccessListener { doc ->
                        val user = doc.toObject(userModel::class.java)
                        if (user != null) {
                            val isMemberAdmin = group?.adminIDs?.contains(memberID) == true
                            members.add(Pair(user, isMemberAdmin))
                        }
                    }
            }
        }
    }

    private fun updateGroupName() {
        val newName = groupNameInput.text.toString().trim()

        if (newName.isEmpty()) {
            groupNameInput.error = "Enter group name"
            return
        }

        saveNameBtn.isEnabled = false

        FirebaseGroups.getGroupReference(groupID!!)
            .update("groupName", newName)
            .addOnSuccessListener {
                Toast.makeText(this, "Group name updated", Toast.LENGTH_SHORT).show()
                saveNameBtn.isEnabled = true
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to update name", Toast.LENGTH_SHORT).show()
                saveNameBtn.isEnabled = true
            }
    }

    private fun updateGroupImage(uri: Uri) {
        try {
            val bitmap = MediaStore.Images.Media.getBitmap(contentResolver, uri)
            val resized = Bitmap.createScaledBitmap(bitmap, 200, 200, true)

            val baos = ByteArrayOutputStream()
            resized.compress(Bitmap.CompressFormat.JPEG, 40, baos)
            val base64 = Base64.encodeToString(baos.toByteArray(), Base64.DEFAULT)

            FirebaseGroups.getGroupReference(groupID!!)
                .update("groupImage", base64)
                .addOnSuccessListener {
                    groupImage.setImageURI(uri)
                    Toast.makeText(this, "Group photo updated", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Failed to update photo", Toast.LENGTH_SHORT).show()
                }
        } catch (e: Exception) {
            Log.e("GroupSettings", "Failed to update image", e)
        }
    }

    private fun removeMember(userID: String) {
        AlertDialog.Builder(this)
            .setTitle("Remove Member")
            .setMessage("Are you sure you want to remove this member?")
            .setPositiveButton("Remove") { _, _ ->
                val updatedMembers = group?.memberIDs?.toMutableList()
                updatedMembers?.remove(userID)

                FirebaseGroups.getGroupReference(groupID!!)
                    .update("memberIDs", updatedMembers)
                    .addOnSuccessListener {
                        Toast.makeText(this, "Member removed", Toast.LENGTH_SHORT).show()
                        FirebaseNotifications.createNotification(
                            type = "REMOVED_FROM_GROUP",
                            recipientID = userID,
                            senderID = FirebaseAuthentication.currentUserID() ?: "",
                            senderName = "Admin",
                            groupID = groupID,
                            groupName = group?.groupName,
                            message = "You have been removed from ${group?.groupName}"
                        )
                        loadGroupDetails() // Reload
                    }
                    .addOnFailureListener {
                        Toast.makeText(this, "Failed to remove member", Toast.LENGTH_SHORT).show()
                    }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showLeaveGroupDialog() {
        AlertDialog.Builder(this)
            .setTitle("Leave Group")
            .setMessage("Are you sure you want to leave this group?")
            .setPositiveButton("Leave") { _, _ ->
                leaveGroup()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun leaveGroup() {
        val currentUserID = FirebaseAuthentication.currentUserID()
        val updatedMembers = group?.memberIDs?.toMutableList()
        val updatedAdmins = group?.adminIDs?.toMutableList()

        updatedMembers?.remove(currentUserID)
        updatedAdmins?.remove(currentUserID)

        FirebaseGroups.getGroupReference(groupID!!)
            .update(
                mapOf(
                    "memberIDs" to updatedMembers,
                    "adminIDs" to updatedAdmins
                )
            )
            .addOnSuccessListener {
                Toast.makeText(this, "Left group", Toast.LENGTH_SHORT).show()
                finish()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to leave group", Toast.LENGTH_SHORT).show()
            }
    }

    private fun showDeleteGroupDialog() {
        AlertDialog.Builder(this)
            .setTitle("Delete Group")
            .setMessage("⚠️ This will permanently delete the group for everyone. This cannot be undone!")
            .setPositiveButton("Delete") { _, _ ->
                deleteGroup()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun deleteGroup() {
        val groupRef = FirebaseGroups.getGroupReference(groupID!!)

        // First delete all messages in the group
        groupRef.collection("messages").get()
            .addOnSuccessListener { documents ->
                val batch = FirebaseFirestore.getInstance().batch()

                // Add all message deletions to batch
                for (document in documents) {
                    batch.delete(document.reference)
                }

                // Commit batch deletion
                batch.commit().addOnSuccessListener {
                    // Now delete the group document itself
                    groupRef.delete()
                        .addOnSuccessListener {
                            Toast.makeText(this, "Group deleted", Toast.LENGTH_SHORT).show()
                            finish()
                        }
                        .addOnFailureListener {
                            Toast.makeText(this, "Failed to delete group", Toast.LENGTH_SHORT).show()
                        }
                }
            }
            .addOnFailureListener {
                // If message deletion fails, still try to delete the group
                groupRef.delete()
                    .addOnSuccessListener {
                        Toast.makeText(this, "Group deleted", Toast.LENGTH_SHORT).show()
                        finish()
                    }
                    .addOnFailureListener {
                        Toast.makeText(this, "Failed to delete group", Toast.LENGTH_SHORT).show()
                    }
            }
    }
}