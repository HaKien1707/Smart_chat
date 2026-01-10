package com.example.smart_chat.activities.group_chat

import android.content.Intent
import android.os.Bundle
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.smart_chat.R
import com.example.smart_chat.models.group.groupModel
import com.example.smart_chat.models.userModel
import com.example.smart_chat.utils.UI.LanguageManager
import com.example.smart_chat.utils.UI.ThemeManager
import com.example.smart_chat.utils.firebase.*
import com.example.smart_chat.utils.others.androidUtils
import com.github.dhaval2404.imagepicker.ImagePicker
import com.google.firebase.Timestamp
import java.util.UUID

class CreateGroupActivity : AppCompatActivity() {

    private lateinit var backBtn: ImageButton
    private lateinit var tickBtn: ImageButton
    private lateinit var groupImage: ImageView
    private lateinit var groupNameInput: EditText
    private lateinit var memberRecycler: RecyclerView
    private lateinit var memberCountText: TextView

    private var selectedImageBase64: String? = null
    private val selectedMembers = mutableListOf<String>()
    private lateinit var memberAdapter: com.example.smart_chat.adapters.group.SelectableUserAdapter

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
        tickBtn = findViewById(R.id.tick_btn)
        groupImage = findViewById(R.id.group_image)
        groupNameInput = findViewById(R.id.group_name_input)
        memberRecycler = findViewById(R.id.member_recycler)
        memberCountText = findViewById(R.id.member_count)

        val incoming = intent.getStringArrayListExtra(EXTRA_SELECTED_USER_IDS) ?: arrayListOf()
        selectedMembers.clear()
        selectedMembers.addAll(incoming.distinct())

        backBtn.setOnClickListener {
            finish()
        }

        tickBtn.setOnClickListener {
            createGroup()
        }

        groupImage.setOnClickListener {
            ImagePicker.with(this)
                .cropSquare()
                .compress(512)
                .maxResultSize(512, 512)
                .createIntent { intent -> imagePickerLauncher.launch(intent) }
        }

        setupMemberRecycler()
        updateMemberCount()
    }

    private fun setupMemberRecycler() {
        val selectedSet = selectedMembers.toSet()
        if (selectedSet.isEmpty()) {
            memberAdapter = com.example.smart_chat.adapters.group.SelectableUserAdapter(this, selectable = false)
            memberRecycler.layoutManager = LinearLayoutManager(this)
            memberRecycler.adapter = memberAdapter
            memberAdapter.submitUsers(emptyList(), emptySet())
            return
        }

        FirebaseAuthentication.allUsersCollection().get()
            .addOnSuccessListener { documents ->
                val users = mutableListOf<userModel>()
                for (doc in documents) {
                    val user = doc.toObject(userModel::class.java)
                    val id = user.userID
                    if (!id.isNullOrBlank() && selectedSet.contains(id)) {
                        users.add(user)
                    }
                }
                memberAdapter = com.example.smart_chat.adapters.group.SelectableUserAdapter(this, selectable = false)
                memberRecycler.layoutManager = LinearLayoutManager(this)
                memberRecycler.adapter = memberAdapter
                memberAdapter.submitUsers(users, selectedSet)
            }
            .addOnFailureListener {
                memberAdapter = com.example.smart_chat.adapters.group.SelectableUserAdapter(this, selectable = false)
                memberRecycler.layoutManager = LinearLayoutManager(this)
                memberRecycler.adapter = memberAdapter
                memberAdapter.submitUsers(emptyList(), selectedSet)
            }
    }

    private fun updateMemberCount() {
        val total = selectedMembers.size + 1 // include current user
        memberCountText.text = resources.getQuantityString(R.plurals.memberCount, total, total)
    }

    private fun createGroup() {
        val groupName = groupNameInput.text.toString().trim()

        if (groupName.isEmpty()) {
            groupNameInput.error = "Enter group name"
            return
        }

        if (selectedMembers.isEmpty()) {
            Toast.makeText(this, "Select at least 1 member", Toast.LENGTH_SHORT).show()
            return
        }

        tickBtn.isEnabled = false

        // Generate group ID
        val groupID = UUID.randomUUID().toString()

        // Add current user to members
        val allMembers = mutableListOf(FirebaseAuthentication.currentUserID())
        allMembers.addAll(selectedMembers)

        // Creator is admin
        val adminIDs = mutableListOf(FirebaseAuthentication.currentUserID())

        // Create group model
        val group = groupModel(
            groupID,
            groupName,
            selectedImageBase64 ?: "",
            allMembers,
            adminIDs,
            Timestamp.now(),
            FirebaseAuthentication.currentUserID()
        )

        // Save to Firestore
        FirebaseGroups.getGroupReference(groupID)
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
                tickBtn.isEnabled = true
            }
    }

    companion object {
        const val EXTRA_SELECTED_USER_IDS = "extra_selected_user_ids"
    }
}
