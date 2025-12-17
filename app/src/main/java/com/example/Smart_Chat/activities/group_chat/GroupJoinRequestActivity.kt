package com.example.Smart_Chat.activities.group_chat

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.Smart_Chat.R
import com.example.Smart_Chat.models.groupModel
import com.example.Smart_Chat.utils.FireBase_utils
import com.example.Smart_Chat.utils.LanguageManager
import com.example.Smart_Chat.utils.ThemeManager

class GroupJoinRequestActivity : AppCompatActivity() {

    private lateinit var backBtn: ImageButton
    private lateinit var statusText: TextView
    private lateinit var joinButton: TextView

    private var group: groupModel? = null
    private var groupID: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        ThemeManager.applySavedTheme(this)
        LanguageManager.applySavedLanguage(this)

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_group_join_request)

        groupID = intent.getStringExtra("groupID")

        if (groupID == null) {
            finish()
            return
        }

        backBtn = findViewById(R.id.back_btn)
        statusText = findViewById(R.id.status_text)
        joinButton = findViewById(R.id.join_button)

        backBtn.setOnClickListener {
            finish()
        }

        loadGroupAndCheckStatus()
    }

    private fun loadGroupAndCheckStatus() {
        FireBase_utils.getGroupReference(groupID!!).get()
            .addOnSuccessListener { document ->
                group = document.toObject(groupModel::class.java)

                if (group == null) {
                    statusText.text = "Group not found"
                    joinButton.isEnabled = false
                    return@addOnSuccessListener
                }

                checkMembershipStatus()
            }
            .addOnFailureListener { e ->
                Log.e("GroupJoinRequest", "Failed to load group", e)
                statusText.text = "Failed to load group"
                joinButton.isEnabled = false
            }
    }

    private fun checkMembershipStatus() {
        val currentUserID = FireBase_utils.currentUserID()

        // Check if blocked
        FireBase_utils.isBlockedFromGroup(groupID!!, currentUserID!!) { isBlocked ->
            runOnUiThread {
                if (isBlocked) {
                    statusText.text = "You are blocked from this group"
                    joinButton.isEnabled = false
                    joinButton.visibility = View.GONE
                    return@runOnUiThread
                }

                // Check if already a member
                if (group?.memberIDs?.contains(currentUserID) == true) {
                    statusText.text = "You are already a member of this group"
                    joinButton.text = "Open Group Chat"
                    joinButton.isEnabled = true
                    joinButton.setOnClickListener {
                        // Open group chat
                        finish()
                    }
                    return@runOnUiThread
                }

                // Check if already sent request
                val requestID = "${groupID}_${currentUserID}"
                FireBase_utils.getGroupJoinRequestReference(requestID).get()
                    .addOnSuccessListener { doc ->
                        if (doc.exists() && doc.getString("status") == "pending") {
                            statusText.text = "Request sent. Waiting for admin approval."
                            joinButton.isEnabled = false
                        } else {
                            // Can send request
                            statusText.text = "You are not a member of this group"
                            joinButton.text = "Request to Join"
                            joinButton.isEnabled = true
                            joinButton.setOnClickListener {
                                sendJoinRequest()
                            }
                        }
                    }
            }
        }
    }

    private fun sendJoinRequest() {
        joinButton.isEnabled = false

        val currentUserID = FireBase_utils.currentUserID()
        Log.d("GROUP_JOIN", "=== Attempting to send join request ===")
        Log.d("GROUP_JOIN", "Current User ID: $currentUserID")
        Log.d("GROUP_JOIN", "Group ID: $groupID")
        Log.d("GROUP_JOIN", "Request ID will be: ${groupID}_${currentUserID}")

        FireBase_utils.sendGroupJoinRequest(
            groupID!!,
            group?.groupName ?: "",
            onSuccess = {
                runOnUiThread {
                    Toast.makeText(this, "Join request sent", Toast.LENGTH_SHORT).show()
                    statusText.text = "Request sent. Waiting for admin approval."
                    joinButton.visibility = View.GONE
                }
            },
            onFailure = { e ->
                runOnUiThread {
                    Log.e("GROUP_JOIN", "Failed to send request", e)
                    Toast.makeText(this, "Failed: ${e.message}", Toast.LENGTH_SHORT).show()
                    joinButton.isEnabled = true
                }
            }
        )
    }
}