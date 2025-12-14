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
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.Smart_Chat.R
import com.example.Smart_Chat.adapters.GroupMemberAdapter
import com.example.Smart_Chat.models.groupModel
import com.example.Smart_Chat.models.userModel
import com.example.Smart_Chat.utils.FireBase_utils
import com.example.Smart_Chat.utils.LanguageManager
import com.example.Smart_Chat.utils.ThemeManager
import com.example.Smart_Chat.utils.androidUtils
import com.github.dhaval2404.imagepicker.ImagePicker
import com.google.firebase.firestore.FirebaseFirestore
import java.io.ByteArrayOutputStream

class GroupChatSettingsActivity : AppCompatActivity() {

    private lateinit var backBtn: ImageButton
    private lateinit var groupImage: ImageView
    private lateinit var groupNameInput: EditText
    private lateinit var saveNameBtn: Button
    private lateinit var addMemberBtn: Button
    private lateinit var membersRecycler: RecyclerView
    private lateinit var leaveGroupBtn: Button
    private lateinit var deleteGroupBtn: Button

    private var groupID: String? = null
    private var group: groupModel? = null
    private var isAdmin = false
    private lateinit var memberAdapter: GroupMemberAdapter

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
        // Only reload if group data might have changed (after add members)
        if (::memberAdapter.isInitialized) {
            loadGroupDetails()
        }
    }

    private fun initViews() {
        backBtn = findViewById(R.id.back_btn)
        groupImage = findViewById(R.id.group_image)
        groupNameInput = findViewById(R.id.group_name_input)
        saveNameBtn = findViewById(R.id.save_name_btn)
        addMemberBtn = findViewById(R.id.add_member_btn)
        membersRecycler = findViewById(R.id.members_recycler)
        leaveGroupBtn = findViewById(R.id.leave_group_btn)
        deleteGroupBtn = findViewById(R.id.delete_group_btn)

        membersRecycler.layoutManager = LinearLayoutManager(this)
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

        leaveGroupBtn.setOnClickListener {
            // No need to check - button only visible for members
            showLeaveGroupDialog()
        }

        deleteGroupBtn.setOnClickListener {
            Log.d("GroupSettings", "Delete button clicked")
            showDeleteGroupDialog()
        }
    }

    private fun loadGroupDetails() {
        FireBase_utils.getGroupReference(groupID!!).get()
            .addOnSuccessListener { document ->
                group = document.toObject(groupModel::class.java)

                if (group == null) {
                    Toast.makeText(this, "Group not found", Toast.LENGTH_SHORT).show()
                    finish()
                    return@addOnSuccessListener
                }

                // Check if current user is admin
                isAdmin = group?.adminIDs?.contains(FireBase_utils.currentUserID()) == true

                // Load group image
                if (!group?.groupImage.isNullOrEmpty()) {
                    androidUtils.setProfileImageFromBase64(
                        this,
                        group?.groupImage!!,
                        groupImage
                    )
                }

                // Set group name
                groupNameInput.setText(group?.groupName)

                // Enable/disable controls based on admin status
                if (isAdmin) {
                    // Admin: Show add/delete, hide leave
                    addMemberBtn.visibility = View.VISIBLE
                    deleteGroupBtn.visibility = View.VISIBLE
                    leaveGroupBtn.visibility = View.GONE
                    groupImage.isClickable = true
                    saveNameBtn.visibility = View.VISIBLE
                } else {
                    // Member: Hide add/delete, show leave
                    addMemberBtn.visibility = View.GONE
                    deleteGroupBtn.visibility = View.GONE
                    leaveGroupBtn.visibility = View.VISIBLE
                    groupImage.isClickable = false
                    saveNameBtn.visibility = View.GONE
                }

                // Load members
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
                FireBase_utils.allUsersCollection().document(memberID).get()
                    .addOnSuccessListener { doc ->
                        val user = doc.toObject(userModel::class.java)
                        if (user != null) {
                            val isMemberAdmin = group?.adminIDs?.contains(memberID) == true
                            members.add(Pair(user, isMemberAdmin))

                            // Update adapter when all loaded
                            if (members.size == memberIDs.size) {
                                setupMembersAdapter(members)
                            }
                        }
                    }
            }
        }
    }

    private fun setupMembersAdapter(members: List<Pair<userModel, Boolean>>) {
        memberAdapter = GroupMemberAdapter(
            members,
            this,
            isAdmin,
            FireBase_utils.currentUserID(),
            onRemoveMember = { userID ->
                removeMember(userID)
            },
            onBlockMember = { userID -> // NEW: Block callback
                blockMember(userID)
            }
        )
        membersRecycler.adapter = memberAdapter
    }

    // Add this function
    private fun blockMember(userID: String) {
        val member = memberAdapter.members.find { it.first.userID == userID }?.first

        AlertDialog.Builder(this)
            .setTitle("Block Member")
            .setMessage("Block ${member?.username}? They will be removed from the group and won't be able to rejoin.")
            .setPositiveButton("Block & Remove") { _, _ ->
                FireBase_utils.blockUserFromGroup(
                    groupID!!,
                    userID,
                    onSuccess = {
                        Toast.makeText(this, "Member blocked and removed", Toast.LENGTH_SHORT).show()
                        loadGroupDetails() // Reload members
                    },
                    onFailure = { e ->
                        Toast.makeText(this, "Failed to block: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                )
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun updateGroupName() {
        val newName = groupNameInput.text.toString().trim()

        if (newName.isEmpty()) {
            groupNameInput.error = "Enter group name"
            return
        }

        saveNameBtn.isEnabled = false

        FireBase_utils.getGroupReference(groupID!!)
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

            FireBase_utils.getGroupReference(groupID!!)
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

                FireBase_utils.getGroupReference(groupID!!)
                    .update("memberIDs", updatedMembers)
                    .addOnSuccessListener {
                        Toast.makeText(this, "Member removed", Toast.LENGTH_SHORT).show()
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
        val currentUserID = FireBase_utils.currentUserID()
        val updatedMembers = group?.memberIDs?.toMutableList()
        val updatedAdmins = group?.adminIDs?.toMutableList()

        updatedMembers?.remove(currentUserID)
        updatedAdmins?.remove(currentUserID)

        FireBase_utils.getGroupReference(groupID!!)
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
        val groupRef = FireBase_utils.getGroupReference(groupID!!)

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