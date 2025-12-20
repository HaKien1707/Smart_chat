package com.example.Smart_Chat.activities.video_call

import android.content.Intent
import android.media.Ringtone
import android.media.RingtoneManager
import android.os.Bundle
import android.util.Log
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.Smart_Chat.R
import com.example.Smart_Chat.utils.UI.LanguageManager
import com.example.Smart_Chat.utils.UI.ThemeManager
import com.example.Smart_Chat.utils.others.androidUtils
import com.example.Smart_Chat.utils.firebase.FirebaseVideoCalls

class IncomingCallActivity : AppCompatActivity() {

    private lateinit var callerProfileImage: ImageView
    private lateinit var callerNameText: TextView
    private lateinit var callStatusText: TextView
    private lateinit var acceptBtn: ImageButton
    private lateinit var rejectBtn: ImageButton

    private var callId: String? = null
    private var callerId: String? = null
    private var callerName: String? = null
    private var callerImage: String? = null
    private var callType: String = "video"

    private var ringtone: Ringtone? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        ThemeManager.applySavedTheme(this)
        LanguageManager.applySavedLanguage(this)

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_incoming_call)

        // Get data from intent
        callId = intent.getStringExtra("callId")
        callerId = intent.getStringExtra("callerId")
        callerName = intent.getStringExtra("callerName")
        callerImage = intent.getStringExtra("callerImage")
        callType = intent.getStringExtra("callType") ?: "video"

        if (callId == null || callerId == null) {
            finish()
            return
        }

        initViews()
        setupUI()
        playRingtone()
    }

    private fun initViews() {
        callerProfileImage = findViewById(R.id.caller_profile_image)
        callerNameText = findViewById(R.id.caller_name)
        callStatusText = findViewById(R.id.call_status)
        acceptBtn = findViewById(R.id.accept_btn)
        rejectBtn = findViewById(R.id.reject_btn)

        acceptBtn.setOnClickListener {
            acceptCall()
        }

        rejectBtn.setOnClickListener {
            rejectCall()
        }
    }

    private fun setupUI() {
        callerNameText.text = callerName

        if (!callerImage.isNullOrBlank()) {
            androidUtils.setProfileImageFromBase64(
                this,
                callerImage,
                callerProfileImage
            )
        }

        callStatusText.text = if (callType == "video") {
            "Incoming video call..."
        } else {
            "Incoming call..."
        }
    }

    private fun playRingtone() {
        try {
            val ringtoneUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
            ringtone = RingtoneManager.getRingtone(this, ringtoneUri)
            ringtone?.play()
        } catch (e: Exception) {
            Log.e("IncomingCall", "Failed to play ringtone", e)
        }
    }

    private fun stopRingtone() {
        try {
            ringtone?.stop()
        } catch (e: Exception) {
            Log.e("IncomingCall", "Failed to stop ringtone", e)
        }
    }

    private fun acceptCall() {
        stopRingtone()

        FirebaseVideoCalls.acceptCall(
            callId!!,
            onSuccess = {
                Log.d("IncomingCall", "Call accepted")
                startVideoCall()
            },
            onFailure = { e ->
                Log.e("IncomingCall", "Failed to accept call", e)
                finish()
            }
        )
    }

    private fun rejectCall() {
        stopRingtone()

        FirebaseVideoCalls.rejectCall(
            callId!!,
            onSuccess = {
                Log.d("IncomingCall", "Call rejected")
                finish()
            },
            onFailure = { e ->
                Log.e("IncomingCall", "Failed to reject call", e)
                finish()
            }
        )
    }

    private fun startVideoCall() {
        val intent = Intent(this, VideoCallActivity::class.java)
        intent.putExtra("callId", callId)
        intent.putExtra("isCaller", false)
        intent.putExtra("receiverId", callerId)
        intent.putExtra("receiverName", callerName)
        intent.putExtra("receiverImage", callerImage)
        startActivity(intent)
        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        stopRingtone()
    }
}