package com.example.Smart_Chat.activities.user_chat

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.Smart_Chat.R
import com.example.Smart_Chat.models.userModel
import com.example.Smart_Chat.utils.UI.LanguageManager
import com.example.Smart_Chat.utils.UI.ThemeManager
import com.example.Smart_Chat.utils.firebase.FirebaseFriends
import com.example.Smart_Chat.utils.others.androidUtils

class NotFriendsActivity : AppCompatActivity() {

    private lateinit var backBtn: ImageButton
    private lateinit var profileImage: ImageView
    private lateinit var userName: TextView
    private lateinit var userPhone: TextView
    private lateinit var statusText: TextView
    private lateinit var actionButton: Button
    private lateinit var cancelButton: Button

    private var otherUser: userModel? = null
    private var currentStatus: FirebaseFriends.FriendshipStatus? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        // Apply theme and language
        ThemeManager.applySavedTheme(this)
        LanguageManager.applySavedLanguage(this)

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_not_friends)

        // Get user from intent
        otherUser = androidUtils.getUserModelFromIntent(intent)

        if (otherUser == null) {
            finish()
            return
        }

        // Initialize views
        backBtn = findViewById(R.id.back_btn)
        profileImage = findViewById(R.id.profile_image)
        userName = findViewById(R.id.user_name)
        userPhone = findViewById(R.id.user_phone)
        statusText = findViewById(R.id.status_text)
        actionButton = findViewById(R.id.action_button)
        cancelButton = findViewById(R.id.cancel_button)

        backBtn.setOnClickListener {
            finish()
        }

        // Load user info
        loadUserInfo()

        // Check friendship status
        checkFriendshipStatus()
    }

    private fun loadUserInfo() {
        userName.text = otherUser?.username ?: "Unknown"
        userPhone.text = otherUser?.phoneNumber ?: ""

        if (!otherUser?.profileImage.isNullOrBlank()) {
            androidUtils.setProfileImageFromBase64(
                this,
                otherUser?.profileImage,
                profileImage
            )
        } else {
            profileImage.setImageResource(R.drawable.ic_profile)
        }
    }

    private fun checkFriendshipStatus() {
        FirebaseFriends.checkFriendshipStatus(otherUser?.userID ?: "") { status ->
            currentStatus = status
            runOnUiThread {
                updateUI(status)
            }
        }
    }

    private fun updateUI(status: FirebaseFriends.FriendshipStatus) {
        when (status) {
            FirebaseFriends.FriendshipStatus.NOT_FRIENDS -> {
                statusText.text = "You are not friends with this user"
                actionButton.text = "Send Friend Request"
                actionButton.visibility = View.VISIBLE
                cancelButton.visibility = View.GONE

                actionButton.setOnClickListener {
                    sendFriendRequest()
                }
            }

            FirebaseFriends.FriendshipStatus.REQUEST_SENT -> {
                statusText.text = "Waiting for ${otherUser?.username} to accept your request"
                actionButton.visibility = View.GONE
                cancelButton.visibility = View.VISIBLE
                cancelButton.text = "Cancel Request"

                cancelButton.setOnClickListener {
                    cancelFriendRequest()
                }
            }

            FirebaseFriends.FriendshipStatus.REQUEST_RECEIVED -> {
                statusText.text = "${otherUser?.username} sent you a friend request"
                actionButton.text = "Accept Request"
                actionButton.visibility = View.VISIBLE
                cancelButton.visibility = View.VISIBLE
                cancelButton.text = "Reject"

                actionButton.setOnClickListener {
                    acceptFriendRequest()
                }

                cancelButton.setOnClickListener {
                    rejectFriendRequest()
                }
            }

            FirebaseFriends.FriendshipStatus.FRIENDS -> {
                // They're friends, shouldn't be here - redirect to chat
                finish()
            }
        }
    }

    private fun sendFriendRequest() {
        actionButton.isEnabled = false
        FirebaseFriends.sendFriendRequest(
            otherUser?.userID ?: "",
            otherUser?.username ?: "",
            onSuccess = {
                runOnUiThread {
                    Toast.makeText(this, "Friend request sent", Toast.LENGTH_SHORT).show()
                    checkFriendshipStatus() // Refresh UI
                }
            },
            onFailure = { e ->
                runOnUiThread {
                    actionButton.isEnabled = true
                    Toast.makeText(this, "Failed: ${e.message}", Toast.LENGTH_SHORT).show()
                    Log.e("NotFriendsActivity", "Send request failed", e)
                }
            }
        )
    }

    private fun cancelFriendRequest() {
        cancelButton.isEnabled = false
        FirebaseFriends.cancelFriendRequest(
            otherUser?.userID ?: "",
            onSuccess = {
                runOnUiThread {
                    Toast.makeText(this, "Request cancelled", Toast.LENGTH_SHORT).show()
                    checkFriendshipStatus() // Refresh UI
                }
            },
            onFailure = { e ->
                runOnUiThread {
                    cancelButton.isEnabled = true
                    Toast.makeText(this, "Failed: ${e.message}", Toast.LENGTH_SHORT).show()
                    Log.e("NotFriendsActivity", "Cancel failed", e)
                }
            }
        )
    }

    private fun acceptFriendRequest() {
        actionButton.isEnabled = false
        FirebaseFriends.acceptFriendRequest(
            otherUser?.userID ?: "",
            onSuccess = {
                runOnUiThread {
                    Toast.makeText(this, "You are now friends!", Toast.LENGTH_SHORT).show()
                    finish() // Close this screen
                }
            },
            onFailure = { e ->
                runOnUiThread {
                    actionButton.isEnabled = true
                    Toast.makeText(this, "Failed: ${e.message}", Toast.LENGTH_SHORT).show()
                    Log.e("NotFriendsActivity", "Accept failed", e)
                }
            }
        )
    }

    private fun rejectFriendRequest() {
        cancelButton.isEnabled = false
        FirebaseFriends.rejectFriendRequest(
            otherUser?.userID ?: "",
            onSuccess = {
                runOnUiThread {
                    Toast.makeText(this, "Request rejected", Toast.LENGTH_SHORT).show()
                    checkFriendshipStatus() // Refresh UI
                }
            },
            onFailure = { e ->
                runOnUiThread {
                    cancelButton.isEnabled = true
                    Toast.makeText(this, "Failed: ${e.message}", Toast.LENGTH_SHORT).show()
                    Log.e("NotFriendsActivity", "Reject failed", e)
                }
            }
        )
    }
}