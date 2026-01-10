package com.example.smart_chat.fragment

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.smart_chat.R
import com.example.smart_chat.activities.video_call.VideoCallActivity
import com.example.smart_chat.utils.firebase.FirebaseVideoCalls
import com.example.smart_chat.utils.others.androidUtils
import com.google.firebase.firestore.ListenerRegistration

class CallFragment : Fragment() {

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
        markCallAsMissedAndExit()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            receiverId = it.getString(ARG_RECEIVER_ID)
            receiverName = it.getString(ARG_RECEIVER_NAME)
            receiverImage = it.getString(ARG_RECEIVER_IMAGE)
            callType = it.getString(ARG_CALL_TYPE) ?: "video"
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_call, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (receiverId.isNullOrBlank()) {
            Toast.makeText(requireContext(), "User not found", Toast.LENGTH_SHORT).show()
            exit()
            return
        }

        initViews(view)
        setupUI()
        initiateCall()

        // Timeout 30 seconds (same as OutgoingCallActivity)
        timeoutHandler.postDelayed(timeoutRunnable, 30000)
    }

    private fun initViews(view: View) {
        receiverProfileImage = view.findViewById(R.id.receiver_profile_image)
        receiverNameText = view.findViewById(R.id.receiver_name)
        callStatusText = view.findViewById(R.id.call_status)
        hangUpBtn = view.findViewById(R.id.hang_up_btn)

        hangUpBtn.setOnClickListener {
            endCallAndExit()
        }
    }

    private fun setupUI() {
        receiverNameText.text = receiverName ?: "Unknown"

        if (!receiverImage.isNullOrBlank()) {
            androidUtils.setProfileImageFromBase64(
                requireContext(),
                receiverImage,
                receiverProfileImage
            )
        } else {
            receiverProfileImage.setImageResource(R.drawable.ic_profile)
        }

        callStatusText.text = "Calling..."
    }

    private fun initiateCall() {
        FirebaseVideoCalls.initiateCall(
            receiverId = receiverId!!,
            receiverName = receiverName ?: "Unknown",
            type = callType,
            onSuccess = { id ->
                callId = id
                listenForCallUpdates(id)
            },
            onFailure = {
                Toast.makeText(requireContext(), "Failed to start call", Toast.LENGTH_SHORT).show()
                exit()
            }
        )
    }

    private fun listenForCallUpdates(callId: String) {
        callUpdateListener?.remove()
        callUpdateListener = FirebaseVideoCalls.listenForCallUpdates(callId) { call ->
            when (call.status) {
                "accepted" -> {
                    timeoutHandler.removeCallbacks(timeoutRunnable)
                    startVideoCall()
                }

                "rejected" -> {
                    timeoutHandler.removeCallbacks(timeoutRunnable)
                    runOnUiThreadSafe {
                        Toast.makeText(requireContext(), "Call rejected", Toast.LENGTH_SHORT).show()
                    }
                    exit()
                }

                "ended" -> {
                    timeoutHandler.removeCallbacks(timeoutRunnable)
                    exit()
                }

                "missed" -> {
                    timeoutHandler.removeCallbacks(timeoutRunnable)
                    exit()
                }
            }
        }
    }

    private fun startVideoCall() {
        val intent = Intent(requireContext(), VideoCallActivity::class.java)
        intent.putExtra("callId", callId)
        intent.putExtra("isCaller", true)
        intent.putExtra("receiverId", receiverId)
        intent.putExtra("receiverName", receiverName)
        intent.putExtra("receiverImage", receiverImage)
        startActivity(intent)

        // Close the user-info flow once call screen opens
        requireActivity().finish()
    }

    private fun endCallAndExit() {
        timeoutHandler.removeCallbacks(timeoutRunnable)
        callId?.let { FirebaseVideoCalls.endCall(it) }
        exit()
    }

    private fun markCallAsMissedAndExit() {
        callId?.let { FirebaseVideoCalls.markCallAsMissed(it) }
        runOnUiThreadSafe {
            Toast.makeText(requireContext(), "No answer", Toast.LENGTH_SHORT).show()
        }
        exit()
    }

    private fun exit() {
        if (isAdded) {
            val fm = parentFragmentManager
            if (fm.backStackEntryCount > 0) {
                fm.popBackStack()
            } else {
                requireActivity().finish()
            }
        }
    }

    private fun runOnUiThreadSafe(block: () -> Unit) {
        if (!isAdded) return
        requireActivity().runOnUiThread(block)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        callUpdateListener?.remove()
        timeoutHandler.removeCallbacks(timeoutRunnable)
    }

    companion object {
        private const val ARG_RECEIVER_ID = "receiverId"
        private const val ARG_RECEIVER_NAME = "receiverName"
        private const val ARG_RECEIVER_IMAGE = "receiverImage"
        private const val ARG_CALL_TYPE = "callType"

        fun newInstance(
            receiverId: String,
            receiverName: String?,
            receiverImage: String?,
            callType: String = "video"
        ): CallFragment {
            return CallFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_RECEIVER_ID, receiverId)
                    putString(ARG_RECEIVER_NAME, receiverName)
                    putString(ARG_RECEIVER_IMAGE, receiverImage)
                    putString(ARG_CALL_TYPE, callType)
                }
            }
        }
    }
}
