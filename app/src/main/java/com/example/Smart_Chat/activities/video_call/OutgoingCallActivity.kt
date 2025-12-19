package com.example.Smart_Chat.activities.video_call

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.Smart_Chat.R
import com.example.Smart_Chat.utils.UI.LanguageManager
import com.example.Smart_Chat.utils.UI.ThemeManager
import com.example.Smart_Chat.utils.others.androidUtils
import com.example.Smart_Chat.utils.firebase.FirebaseVideoCalls
import com.google.firebase.firestore.ListenerRegistration

class OutgoingCallActivity : AppCompatActivity() {

    private lateinit var receiverProfileImage: ImageView
    private lateinit var receiverNameText: TextView
    private lateinit var callStatusText: TextView
    private lateinit var hangUpBtn: ImageButton

    private var receiverId: String? = null
    private var receiverName: String? = null
    private var receiverImage: String? = null
    private var callType: String = "video"
    private var callId: String? = null

    private var callUpdateListener: ListenerRegistration? = null
    private val timeoutHandler = Handler(Looper.getMainLooper())
    private val timeoutRunnable = Runnable {
        // Call not answered within 30 seconds
        markCallAsMissedAndFinish()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        ThemeManager.applySavedTheme(this)
        LanguageManager.applySavedLanguage(this)

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_outgoing_call)

        // Get data from intent
        receiverId = intent.getStringExtra("receiverId")
        receiverName = intent.getStringExtra("receiverName")
        receiverImage = intent.getStringExtra("receiverImage")
        callType = intent.getStringExtra("callType") ?: "video"

        if (receiverId == null) {
            finish()
            return
        }

        initViews()
        setupUI()
        initiateCall()

        // Set timeout for 30 seconds
        timeoutHandler.postDelayed(timeoutRunnable, 30000)
    }

    private fun initViews() {
        receiverProfileImage = findViewById(R.id.receiver_profile_image)
        receiverNameText = findViewById(R.id.receiver_name)
        callStatusText = findViewById(R.id.call_status)
        hangUpBtn = findViewById(R.id.hang_up_btn)

        hangUpBtn.setOnClickListener {
            endCallAndFinish()
        }
    }

    private fun setupUI() {
        receiverNameText.text = receiverName

        if (!receiverImage.isNullOrBlank()) {
            androidUtils.setProfileImageFromBase64(
                this,
                receiverImage,
                receiverProfileImage
            )
        }

        callStatusText.text = "Calling..."
    }

    private fun initiateCall() {
        FirebaseVideoCalls.initiateCall(
            receiverId!!,
            receiverName ?: "Unknown",
            callType,
            onSuccess = { callId ->
                this.callId = callId
                Log.d("OutgoingCall", "Call initiated: $callId")
                listenForCallUpdates(callId)
            },
            onFailure = { e ->
                Log.e("OutgoingCall", "Failed to initiate call", e)
                Toast.makeText(this, "Failed to start call", Toast.LENGTH_SHORT).show()
                finish()
            }
        )
    }

    private fun listenForCallUpdates(callId: String) {
        callUpdateListener = FirebaseVideoCalls.listenForCallUpdates(callId) { call ->
            when (call.status) {
                "accepted" -> {
                    // Call accepted, move to video call screen
                    timeoutHandler.removeCallbacks(timeoutRunnable)
                    Log.d("OutgoingCall", "Call accepted")
                    startVideoCall()
                }
                "rejected" -> {
                    // Call rejected
                    timeoutHandler.removeCallbacks(timeoutRunnable)
                    runOnUiThread {
                        Toast.makeText(this, "Call rejected", Toast.LENGTH_SHORT).show()
                    }
                    finish()
                }
                "ended" -> {
                    // Call ended by other user
                    timeoutHandler.removeCallbacks(timeoutRunnable)
                    finish()
                }
            }
        }
    }

    private fun startVideoCall() {
        val intent = Intent(this, VideoCallActivity::class.java)
        intent.putExtra("callId", callId)
        intent.putExtra("isCaller", true)
        intent.putExtra("receiverId", receiverId)
        intent.putExtra("receiverName", receiverName)
        intent.putExtra("receiverImage", receiverImage)
        startActivity(intent)
        finish()
    }

    private fun endCallAndFinish() {
        timeoutHandler.removeCallbacks(timeoutRunnable)

        callId?.let { id ->
            FirebaseVideoCalls.endCall(id)
        }

        finish()
    }

    private fun markCallAsMissedAndFinish() {
        callId?.let { id ->
            FirebaseVideoCalls.markCallAsMissed(id)
        }

        runOnUiThread {
            Toast.makeText(this, "No answer", Toast.LENGTH_SHORT).show()
        }

        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        callUpdateListener?.remove()
        timeoutHandler.removeCallbacks(timeoutRunnable)
    }
}