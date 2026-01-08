package com.example.smart_chat.activities.video_call

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.smart_chat.R
import com.example.smart_chat.models.video_call.IceCandidateModel
import com.example.smart_chat.utils.UI.LanguageManager
import com.example.smart_chat.utils.UI.ThemeManager
import com.example.smart_chat.utils.others.WebRTCClient
import com.example.smart_chat.utils.others.androidUtils
import com.example.smart_chat.utils.firebase.FirebaseAuthentication
import com.example.smart_chat.utils.firebase.FirebaseVideoCalls
import com.google.firebase.firestore.ListenerRegistration
import org.webrtc.DataChannel
import org.webrtc.IceCandidate
import org.webrtc.MediaStream
import org.webrtc.PeerConnection
import org.webrtc.RtpReceiver
import org.webrtc.SessionDescription
import org.webrtc.SurfaceViewRenderer

class VideoCallActivity : AppCompatActivity() {

    private lateinit var localView: SurfaceViewRenderer
    private lateinit var remoteView: SurfaceViewRenderer
    private lateinit var userProfileImage: ImageView
    private lateinit var userNameText: TextView
    private lateinit var callDurationText: TextView
    private lateinit var muteAudioBtn: ImageButton
    private lateinit var endCallBtn: ImageButton
    private lateinit var switchCameraBtn: ImageButton

    private var webRTCClient: WebRTCClient? = null
    private var callId: String? = null
    private var isCaller: Boolean = false
    private var receiverId: String? = null
    private var receiverName: String? = null
    private var receiverImage: String? = null

    private var isAudioMuted = false
    private var callUpdateListener: ListenerRegistration? = null
    private var iceCandidateListener: ListenerRegistration? = null

    // Call duration tracking
    private var callStartTime: Long = 0
    private val durationHandler = Handler(Looper.getMainLooper())
    private val durationRunnable = object : Runnable {
        override fun run() {
            updateCallDuration()
            durationHandler.postDelayed(this, 1000)
        }
    }

    companion object {
        private const val CAMERA_PERMISSION_REQUEST_CODE = 100
        private const val TAG = "VideoCallActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        ThemeManager.applySavedTheme(this)
        LanguageManager.applySavedLanguage(this)

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_video_call)

        // Get data from intent
        callId = intent.getStringExtra("callId")
        isCaller = intent.getBooleanExtra("isCaller", false)
        receiverId = intent.getStringExtra("receiverId")
        receiverName = intent.getStringExtra("receiverName")
        receiverImage = intent.getStringExtra("receiverImage")

        if (callId == null || receiverId == null) {
            finish()
            return
        }

        initViews()
        setupUI()

        if (checkPermissions()) {
            startCall()
        } else {
            requestPermissions()
        }
    }

    private fun initViews() {
        localView = findViewById(R.id.local_view)
        remoteView = findViewById(R.id.remote_view)
        userProfileImage = findViewById(R.id.user_profile_image)
        userNameText = findViewById(R.id.user_name)
        callDurationText = findViewById(R.id.call_duration)
        muteAudioBtn = findViewById(R.id.mute_audio_btn)
        endCallBtn = findViewById(R.id.end_call_btn)
        switchCameraBtn = findViewById(R.id.switch_camera_btn)

        muteAudioBtn.setOnClickListener {
            toggleAudio()
        }

        endCallBtn.setOnClickListener {
            endCall()
        }

        switchCameraBtn.setOnClickListener {
            switchCamera()
        }
    }

    private fun setupUI() {
        userNameText.text = receiverName

        if (!receiverImage.isNullOrBlank()) {
            androidUtils.setProfileImageFromBase64(
                this,
                receiverImage,
                userProfileImage
            )
        }
    }

    private fun checkPermissions(): Boolean {
        val cameraPermission = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.CAMERA
        )
        val audioPermission = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.RECORD_AUDIO
        )

        return cameraPermission == PackageManager.PERMISSION_GRANTED &&
                audioPermission == PackageManager.PERMISSION_GRANTED
    }

    private fun requestPermissions() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(
                Manifest.permission.CAMERA,
                Manifest.permission.RECORD_AUDIO
            ),
            CAMERA_PERMISSION_REQUEST_CODE
        )
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == CAMERA_PERMISSION_REQUEST_CODE) {
            if (grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                startCall()
            } else {
                Toast.makeText(
                    this,
                    "Camera and microphone permissions are required",
                    Toast.LENGTH_LONG
                ).show()
                finish()
            }
        }
    }

    private fun startCall() {
        callStartTime = System.currentTimeMillis()
        durationHandler.post(durationRunnable)

        // Initialize WebRTC
        webRTCClient = WebRTCClient(this, object : PeerConnection.Observer {
            override fun onIceCandidate(iceCandidate: IceCandidate?) {
                iceCandidate?.let {
                    Log.d(TAG, "New ICE candidate: ${it.sdp}")
                    sendIceCandidate(it)
                }
            }

            override fun onAddStream(mediaStream: MediaStream?) {
                Log.d(TAG, "Remote stream added")
                runOnUiThread {
                    mediaStream?.videoTracks?.firstOrNull()?.addSink(remoteView)
                }
            }

            override fun onSignalingChange(newState: PeerConnection.SignalingState?) {
                Log.d(TAG, "Signaling state changed: $newState")
            }

            override fun onIceConnectionChange(newState: PeerConnection.IceConnectionState?) {
                Log.d(TAG, "ICE connection state: $newState")

                runOnUiThread {
                    when (newState) {
                        PeerConnection.IceConnectionState.CONNECTED -> {
                            Log.d(TAG, "Call connected!")
                        }
                        PeerConnection.IceConnectionState.DISCONNECTED,
                        PeerConnection.IceConnectionState.FAILED -> {
                            Log.e(TAG, "Connection lost")
                            Toast.makeText(
                                this@VideoCallActivity,
                                "Connection lost",
                                Toast.LENGTH_SHORT
                            ).show()
                            endCall()
                        }
                        else -> {}
                    }
                }
            }

            override fun onIceConnectionReceivingChange(receiving: Boolean) {
                Log.d(TAG, "ICE connection receiving: $receiving")
            }

            override fun onIceGatheringChange(newState: PeerConnection.IceGatheringState?) {
                Log.d(TAG, "ICE gathering state: $newState")
            }

            override fun onRemoveStream(mediaStream: MediaStream?) {
                Log.d(TAG, "Remote stream removed")
            }

            override fun onDataChannel(dataChannel: DataChannel?) {
                Log.d(TAG, "Data channel: $dataChannel")
            }

            override fun onRenegotiationNeeded() {
                Log.d(TAG, "Renegotiation needed")
            }

            override fun onAddTrack(
                receiver: RtpReceiver?,
                mediaStreams: Array<out MediaStream>?
            ) {
                Log.d(TAG, "Track added")
            }

            override fun onIceCandidatesRemoved(candidates: Array<IceCandidate>?) {
                Log.d(TAG, "ICE candidates removed: ${candidates?.size}")
            }
        })

        webRTCClient?.initializePeerConnection()
        webRTCClient?.startLocalVideoCapture(localView)

        // Listen for call updates
        listenForCallUpdates()
        listenForIceCandidates()

        // Start signaling
        if (isCaller) {
            // Caller creates offer
            createOffer()
        } else {
            // Receiver waits for offer, then creates answer
            listenForOffer()
        }
    }

    private fun createOffer() {
        webRTCClient?.call(
            onSuccess = { sdpOffer ->
                Log.d(TAG, "Offer created")
                sendOffer(sdpOffer)
            },
            onFailure = { e ->
                Log.e(TAG, "Failed to create offer", e)
                Toast.makeText(this, "Failed to establish connection", Toast.LENGTH_SHORT).show()
                endCall()
            }
        )
    }

    private fun listenForOffer() {
        callUpdateListener = FirebaseVideoCalls.listenForCallUpdates(callId!!) { call ->
            if (!call.offer.isNullOrEmpty()) {
                Log.d(TAG, "Offer received")
                val sessionDescription = SessionDescription(
                    SessionDescription.Type.OFFER,
                    call.offer
                )
                webRTCClient?.onRemoteSessionReceived(sessionDescription)
                createAnswer()
            }
        }
    }

    private fun createAnswer() {
        webRTCClient?.answer(
            onSuccess = { sdpAnswer ->
                Log.d(TAG, "Answer created")
                sendAnswer(sdpAnswer)
            },
            onFailure = { e ->
                Log.e(TAG, "Failed to create answer", e)
                Toast.makeText(this, "Failed to establish connection", Toast.LENGTH_SHORT).show()
                endCall()
            }
        )
    }

    private fun sendOffer(sdpOffer: String) {
        FirebaseVideoCalls.sendOffer(
            callId!!,
            sdpOffer,
            onSuccess = {
                Log.d(TAG, "Offer sent to Firebase")
                listenForAnswer()
            },
            onFailure = { e ->
                Log.e(TAG, "Failed to send offer", e)
            }
        )
    }

    private fun listenForAnswer() {
        callUpdateListener = FirebaseVideoCalls.listenForCallUpdates(callId!!) { call ->
            if (!call.answer.isNullOrEmpty()) {
                Log.d(TAG, "Answer received")
                val sessionDescription = SessionDescription(
                    SessionDescription.Type.ANSWER,
                    call.answer
                )
                webRTCClient?.onRemoteSessionReceived(sessionDescription)
            }
        }
    }

    private fun sendAnswer(sdpAnswer: String) {
        FirebaseVideoCalls.sendAnswer(
            callId!!,
            sdpAnswer,
            onSuccess = {
                Log.d(TAG, "Answer sent to Firebase")
            },
            onFailure = { e ->
                Log.e(TAG, "Failed to send answer", e)
            }
        )
    }

    private fun sendIceCandidate(iceCandidate: IceCandidate) {
        val candidate = IceCandidateModel(
            iceCandidate.sdpMid,
            iceCandidate.sdpMLineIndex,
            iceCandidate.sdp,
            FirebaseAuthentication.currentUserID()
        )

        FirebaseVideoCalls.addIceCandidate(
            callId!!,
            candidate,
            onSuccess = {
                Log.d(TAG, "ICE candidate sent")
            },
            onFailure = { e ->
                Log.e(TAG, "Failed to send ICE candidate", e)
            }
        )
    }

    private fun listenForIceCandidates() {
        iceCandidateListener = FirebaseVideoCalls.listenForIceCandidates(callId!!) { candidate ->
            // Only process candidates from the other user
            if (candidate.userId != FirebaseAuthentication.currentUserID()) {
                Log.d(TAG, "Remote ICE candidate received")
                val iceCandidate = IceCandidate(
                    candidate.sdpMid,
                    candidate.sdpMLineIndex ?: 0,
                    candidate.sdp
                )
                webRTCClient?.addIceCandidate(iceCandidate)
            }
        }
    }

    private fun listenForCallUpdates() {
        callUpdateListener = FirebaseVideoCalls.listenForCallUpdates(callId!!) { call ->
            if (call.status == "ended") {
                runOnUiThread {
                    Toast.makeText(this, "Call ended", Toast.LENGTH_SHORT).show()
                    finish()
                }
            }
        }
    }

    private fun toggleAudio() {
        isAudioMuted = !isAudioMuted
        webRTCClient?.toggleAudio(isAudioMuted)

        muteAudioBtn.setImageResource(
            if (isAudioMuted) R.drawable.ic_mic_off else R.drawable.ic_mic
        )
    }

    private fun switchCamera() {
        webRTCClient?.switchCamera()
    }

    private fun updateCallDuration() {
        val duration = (System.currentTimeMillis() - callStartTime) / 1000
        val minutes = duration / 60
        val seconds = duration % 60
        callDurationText.text = String.format("%02d:%02d", minutes, seconds)
    }

    private fun endCall() {
        durationHandler.removeCallbacks(durationRunnable)

        FirebaseVideoCalls.endCall(
            callId!!,
            onSuccess = {
                Log.d(TAG, "Call ended")
            },
            onFailure = { e ->
                Log.e(TAG, "Failed to end call", e)
            }
        )

        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        durationHandler.removeCallbacks(durationRunnable)
        callUpdateListener?.remove()
        iceCandidateListener?.remove()
        webRTCClient?.close()
    }
}