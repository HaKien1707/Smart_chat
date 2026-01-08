package com.example.smart_chat.utils.others

import android.content.Context
import android.util.Log
import org.webrtc.*

class WebRTCClient(
    private val context: Context,
    private val observer: PeerConnection.Observer
) {
    private var peerConnectionFactory: PeerConnectionFactory? = null
    private var peerConnection: PeerConnection? = null
    private var localVideoTrack: VideoTrack? = null
    private var localAudioTrack: AudioTrack? = null
    private var videoCapturer: CameraVideoCapturer? = null

    companion object {
        private const val TAG = "WebRTCClient"
        private val ICE_SERVERS = listOf(
            PeerConnection.IceServer.builder("stun:stun.l.google.com:19302").createIceServer()
        )
    }

    init {
        initPeerConnectionFactory()
    }

    private fun initPeerConnectionFactory() {
        val options = PeerConnectionFactory.InitializationOptions.builder(context)
            .setEnableInternalTracer(true)
            .createInitializationOptions()
        PeerConnectionFactory.initialize(options)

        val encoderFactory = DefaultVideoEncoderFactory(
            EglBase.create().eglBaseContext,
            true,
            true
        )
        val decoderFactory = DefaultVideoDecoderFactory(EglBase.create().eglBaseContext)

        peerConnectionFactory = PeerConnectionFactory.builder()
            .setVideoEncoderFactory(encoderFactory)
            .setVideoDecoderFactory(decoderFactory)
            .setOptions(PeerConnectionFactory.Options())
            .createPeerConnectionFactory()
    }

    fun initializePeerConnection() {
        val rtcConfig = PeerConnection.RTCConfiguration(ICE_SERVERS)
        rtcConfig.sdpSemantics = PeerConnection.SdpSemantics.UNIFIED_PLAN

        peerConnection = peerConnectionFactory?.createPeerConnection(rtcConfig, observer)
    }

    fun startLocalVideoCapture(localVideoView: SurfaceViewRenderer) {
        val rootEglBase = EglBase.create()
        localVideoView.init(rootEglBase.eglBaseContext, null)
        localVideoView.setMirror(true)
        localVideoView.setEnableHardwareScaler(true)

        // Video capturer
        videoCapturer = createCameraVideoCapturer()
        val surfaceTextureHelper = SurfaceTextureHelper.create(
            "CaptureThread",
            rootEglBase.eglBaseContext
        )

        val videoSource = peerConnectionFactory?.createVideoSource(false)
        videoCapturer?.initialize(
            surfaceTextureHelper,
            context,
            videoSource?.capturerObserver
        )
        videoCapturer?.startCapture(1280, 720, 30)

        localVideoTrack =
            peerConnectionFactory?.createVideoTrack("local_video_track", videoSource)
        localVideoTrack?.addSink(localVideoView)

        // Audio
        val audioSource =
            peerConnectionFactory?.createAudioSource(MediaConstraints())
        localAudioTrack =
            peerConnectionFactory?.createAudioTrack("local_audio_track", audioSource)

        // ✅ Unified Plan: addTrack()
        val streamId = "ARDAMS"
        localVideoTrack?.let {
            peerConnection?.addTrack(it, listOf(streamId))
        }
        localAudioTrack?.let {
            peerConnection?.addTrack(it, listOf(streamId))
        }
    }

    private fun createCameraVideoCapturer(): CameraVideoCapturer? {
        val enumerator = Camera2Enumerator(context)
        val deviceNames = enumerator.deviceNames

        // Try front camera first
        for (deviceName in deviceNames) {
            if (enumerator.isFrontFacing(deviceName)) {
                val capturer = enumerator.createCapturer(deviceName, null)
                if (capturer != null) {
                    return capturer
                }
            }
        }

        // Try back camera
        for (deviceName in deviceNames) {
            if (enumerator.isBackFacing(deviceName)) {
                val capturer = enumerator.createCapturer(deviceName, null)
                if (capturer != null) {
                    return capturer
                }
            }
        }

        return null
    }

    fun call(onSuccess: (String) -> Unit, onFailure: (Exception) -> Unit) {
        val constraints = MediaConstraints().apply {
            mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveVideo", "true"))
            mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"))
        }

        peerConnection?.createOffer(object : SdpObserver {
            override fun onCreateSuccess(sdp: SessionDescription?) {
                peerConnection?.setLocalDescription(object : SdpObserver {
                    override fun onSetSuccess() {
                        onSuccess(sdp?.description ?: "")
                    }

                    override fun onSetFailure(error: String?) {
                        onFailure(Exception("Failed to set local description: $error"))
                    }

                    override fun onCreateSuccess(p0: SessionDescription?) {}
                    override fun onCreateFailure(p0: String?) {}
                }, sdp)
            }

            override fun onCreateFailure(error: String?) {
                onFailure(Exception("Failed to create offer: $error"))
            }

            override fun onSetSuccess() {}
            override fun onSetFailure(error: String?) {}
        }, constraints)
    }

    fun answer(onSuccess: (String) -> Unit, onFailure: (Exception) -> Unit) {
        val constraints = MediaConstraints().apply {
            mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveVideo", "true"))
            mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"))
        }

        peerConnection?.createAnswer(object : SdpObserver {
            override fun onCreateSuccess(sdp: SessionDescription?) {
                peerConnection?.setLocalDescription(object : SdpObserver {
                    override fun onSetSuccess() {
                        onSuccess(sdp?.description ?: "")
                    }

                    override fun onSetFailure(error: String?) {
                        onFailure(Exception("Failed to set local description: $error"))
                    }

                    override fun onCreateSuccess(p0: SessionDescription?) {}
                    override fun onCreateFailure(p0: String?) {}
                }, sdp)
            }

            override fun onCreateFailure(error: String?) {
                onFailure(Exception("Failed to create answer: $error"))
            }

            override fun onSetSuccess() {}
            override fun onSetFailure(error: String?) {}
        }, constraints)
    }

    fun onRemoteSessionReceived(sessionDescription: SessionDescription) {
        peerConnection?.setRemoteDescription(object : SdpObserver {
            override fun onSetSuccess() {
                Log.d(TAG, "Remote description set successfully")
            }

            override fun onSetFailure(error: String?) {
                Log.e(TAG, "Failed to set remote description: $error")
            }

            override fun onCreateSuccess(p0: SessionDescription?) {}
            override fun onCreateFailure(p0: String?) {}
        }, sessionDescription)
    }

    fun addIceCandidate(iceCandidate: IceCandidate) {
        peerConnection?.addIceCandidate(iceCandidate)
    }

    fun toggleAudio(mute: Boolean) {
        localAudioTrack?.setEnabled(!mute)
    }

    fun switchCamera() {
        videoCapturer?.switchCamera(null)
    }

    fun close() {
        try {
            videoCapturer?.stopCapture()
            videoCapturer?.dispose()
            localVideoTrack?.dispose()
            localAudioTrack?.dispose()
            peerConnection?.close()
            peerConnection?.dispose()
            peerConnectionFactory?.dispose()
        } catch (e: Exception) {
            Log.e(TAG, "Error closing WebRTC", e)
        }
    }
}