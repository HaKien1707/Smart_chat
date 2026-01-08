package com.example.smart_chat.utils.firebase

import android.util.Log
import com.example.smart_chat.models.video_call.IceCandidateModel
import com.example.smart_chat.models.video_call.VideoCallModel
import com.google.firebase.Timestamp
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration

object FirebaseVideoCalls {

    @JvmStatic
    fun videoCallsCollection(): CollectionReference {
        return FirebaseFirestore.getInstance().collection("videoCalls")
    }

    @JvmStatic
    fun getVideoCallReference(callId: String): DocumentReference {
        return videoCallsCollection().document(callId)
    }

    @JvmStatic
    fun getIceCandidatesReference(callId: String): CollectionReference {
        return getVideoCallReference(callId).collection("iceCandidates")
    }

    @JvmStatic
    fun initiateCall(
        receiverId: String,
        receiverName: String,
        type: String = "video",
        onSuccess: (String) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        val currentUserID = FirebaseAuthentication.currentUserID() ?: return
        val callId = videoCallsCollection().document().id

        FirebaseAuthentication.currentUserDetails().get().addOnSuccessListener { doc ->
            val currentUser = doc.toObject(com.example.smart_chat.models.userModel::class.java)

            val call = VideoCallModel(
                callId,
                currentUserID,
                currentUser?.username,
                receiverId,
                receiverName,
                "ringing",
                type,
                Timestamp.now()
            )

            getVideoCallReference(callId).set(call)
                .addOnSuccessListener {
                    Log.d("FirebaseVideoCalls", "Call initiated: $callId")
                    onSuccess(callId)
                }
                .addOnFailureListener {
                    Log.e("FirebaseVideoCalls", "Failed to initiate call", it)
                    onFailure(it)
                }
        }
    }

    @JvmStatic
    fun acceptCall(
        callId: String,
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        getVideoCallReference(callId).update("status", "accepted")
            .addOnSuccessListener {
                Log.d("FirebaseVideoCalls", "Call accepted: $callId")
                onSuccess()
            }
            .addOnFailureListener {
                Log.e("FirebaseVideoCalls", "Failed to accept call", it)
                onFailure(it)
            }
    }

    @JvmStatic
    fun rejectCall(
        callId: String,
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        getVideoCallReference(callId).update("status", "rejected")
            .addOnSuccessListener {
                Log.d("FirebaseVideoCalls", "Call rejected: $callId")
                onSuccess()
            }
            .addOnFailureListener {
                Log.e("FirebaseVideoCalls", "Failed to reject call", it)
                onFailure(it)
            }
    }

    @JvmStatic
    fun endCall(
        callId: String,
        onSuccess: () -> Unit = {},
        onFailure: (Exception) -> Unit = {}
    ) {
        getVideoCallReference(callId).update("status", "ended")
            .addOnSuccessListener {
                Log.d("FirebaseVideoCalls", "Call ended: $callId")
                onSuccess()
            }
            .addOnFailureListener {
                Log.e("FirebaseVideoCalls", "Failed to end call", it)
                onFailure(it)
            }
    }

    @JvmStatic
    fun markCallAsMissed(
        callId: String,
        onSuccess: () -> Unit = {},
        onFailure: (Exception) -> Unit = {}
    ) {
        getVideoCallReference(callId).update("status", "missed")
            .addOnSuccessListener {
                Log.d("FirebaseVideoCalls", "Call marked as missed: $callId")
                onSuccess()
            }
            .addOnFailureListener {
                Log.e("FirebaseVideoCalls", "Failed to mark call as missed", it)
                onFailure(it)
            }
    }

    @JvmStatic
    fun sendOffer(
        callId: String,
        sdpOffer: String,
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        getVideoCallReference(callId).update("offer", sdpOffer)
            .addOnSuccessListener {
                Log.d("FirebaseVideoCalls", "SDP offer sent")
                onSuccess()
            }
            .addOnFailureListener {
                Log.e("FirebaseVideoCalls", "Failed to send offer", it)
                onFailure(it)
            }
    }

    @JvmStatic
    fun sendAnswer(
        callId: String,
        sdpAnswer: String,
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        getVideoCallReference(callId).update("answer", sdpAnswer)
            .addOnSuccessListener {
                Log.d("FirebaseVideoCalls", "SDP answer sent")
                onSuccess()
            }
            .addOnFailureListener {
                Log.e("FirebaseVideoCalls", "Failed to send answer", it)
                onFailure(it)
            }
    }

    @JvmStatic
    fun addIceCandidate(
        callId: String,
        iceCandidate: IceCandidateModel,
        onSuccess: () -> Unit = {},
        onFailure: (Exception) -> Unit = {}
    ) {
        getIceCandidatesReference(callId).add(iceCandidate)
            .addOnSuccessListener {
                Log.d("FirebaseVideoCalls", "ICE candidate added")
                onSuccess()
            }
            .addOnFailureListener {
                Log.e("FirebaseVideoCalls", "Failed to add ICE candidate", it)
                onFailure(it)
            }
    }

    @JvmStatic
    fun listenForIncomingCalls(
        onCallReceived: (VideoCallModel) -> Unit
    ): ListenerRegistration {
        val currentUserID = FirebaseAuthentication.currentUserID() ?: throw Exception("User not logged in")

        return videoCallsCollection()
            .whereEqualTo("receiverId", currentUserID)
            .whereEqualTo("status", "ringing")
            .addSnapshotListener { snapshots, error ->
                if (error != null) {
                    Log.e("FirebaseVideoCalls", "Listen failed", error)
                    return@addSnapshotListener
                }

                snapshots?.forEach { doc ->
                    val call = doc.toObject(VideoCallModel::class.java)
                    onCallReceived(call)
                }
            }
    }

    @JvmStatic
    fun listenForCallUpdates(
        callId: String,
        onUpdate: (VideoCallModel) -> Unit
    ): ListenerRegistration {
        return getVideoCallReference(callId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("FirebaseVideoCalls", "Call update listen failed", error)
                    return@addSnapshotListener
                }

                snapshot?.toObject(VideoCallModel::class.java)?.let { call ->
                    onUpdate(call)
                }
            }
    }

    @JvmStatic
    fun listenForIceCandidates(
        callId: String,
        onCandidate: (IceCandidateModel) -> Unit
    ): ListenerRegistration {
        return getIceCandidatesReference(callId)
            .addSnapshotListener { snapshots, error ->
                if (error != null) {
                    Log.e("FirebaseVideoCalls", "ICE candidates listen failed", error)
                    return@addSnapshotListener
                }

                snapshots?.forEach { doc ->
                    val candidate = doc.toObject(IceCandidateModel::class.java)
                    onCandidate(candidate)
                }
            }
    }
}