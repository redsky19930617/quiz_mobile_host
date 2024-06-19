package com.quiz.hostapp.ui.host.quiz

import android.Manifest
import android.app.PictureInPictureParams
import android.content.pm.PackageManager
import android.graphics.Rect
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.util.Rational
import android.view.LayoutInflater
import android.view.SurfaceView
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.quiz.hostapp.ServiceLocator
import com.quiz.hostapp.databinding.FragmentHostBinding
import com.quiz.hostapp.network.apiService
import com.quiz.hostapp.ui.host.HostViewModel
import com.quiz.hostapp.utils.showMessage
import io.agora.rtc2.*
import io.agora.rtc2.video.VideoCanvas

class HostFragment : Fragment() {
    companion object {
        private const val TAG = "HostFragment"
    }

    private lateinit var binding: FragmentHostBinding
    private val PERMISSION_REQ_ID = 22
    private val REQUESTED_PERMISSIONS = arrayOf<String>(
        Manifest.permission.RECORD_AUDIO,
        Manifest.permission.CAMERA
    )

    // Fill the App ID of your project generated on Agora Console.
    private val appId = "b75cc48b972d4ccc92edb71a1c75fb23"

    // Fill the channel name.
    private val channelName = "test"

    // Fill the temp token generated on Agora Console.
    private lateinit var rtcToken: String

    private val APP_KEY = "ff54bdcd8a8f44c4913b868e5c45ba06#1095855"

    // An integer that identifies the local user.
    private var uid = -1
    private var isJoined = false

    private var agoraEngine: RtcEngine? = null

    //SurfaceView to render local video in a Container.
    private var localSurfaceView: SurfaceView? = null
    private lateinit var localContainer: FrameLayout

    private val viewModel: HostViewModel by activityViewModels {
        ServiceLocator.provideViewModelFactory(
            requireContext(),
            apiService(requireContext())
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        activity?.onBackPressedDispatcher?.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                // in here you can do logic when backPress is clicked
                val params = updatePictureInPictureParams()
                params?.let {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        requireActivity().enterPictureInPictureMode(it)
                    } else {
                        showMessage("PIP not supported on your device", requireContext())
                    }
                }
            }
        })
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        binding = FragmentHostBinding.inflate(inflater, container, false)

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        if (!checkSelfPermission()) {
            ActivityCompat.requestPermissions(
                requireActivity(),
                REQUESTED_PERMISSIONS,
                PERMISSION_REQ_ID
            )
        }
        rtcToken = requireArguments().getString("rtc_token") ?: ""
        localContainer = binding.localVideoViewContainer
        if (rtcToken.isNotEmpty()) setupVideoSDKEngine() else findNavController().navigateUp()

        binding.closeIv.setOnClickListener {
            leaveChannel()
        }

        viewModel.testLiveData.observe(viewLifecycleOwner) {
            it?.let {
                showMessage("From view model $it", requireContext())
            }
        }
    }

    override fun onPictureInPictureModeChanged(isInPictureInPictureMode: Boolean) {

    }


    private fun checkSelfPermission(): Boolean {
        return !(ContextCompat.checkSelfPermission(
            requireContext(),
            REQUESTED_PERMISSIONS[0]
        ) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(
                    requireContext(),
                    REQUESTED_PERMISSIONS[1]
                ) != PackageManager.PERMISSION_GRANTED)
    }

    private fun setupVideoSDKEngine() {
        try {
            Log.i(TAG, "starting agora enghine")
            val config = RtcEngineConfig()
            config.mContext = requireActivity().baseContext
            config.mAppId = appId
            config.mEventHandler = mRtcEventHandler
            agoraEngine = RtcEngine.create(config)
            // By default, the video module is disabled, call enableVideo to enable it.
            agoraEngine?.enableVideo()
            joinChannel()
        } catch (e: Exception) {
            showMessage(e.toString(), requireContext())
            Log.e(TAG, "erro set up video engine: ${e.message}")
        }
    }

    //Event handler for live streaming
    private val mRtcEventHandler: IRtcEngineEventHandler = object : IRtcEngineEventHandler() {
        // Listen for the remote host joining the channel to get the uid of the host.
        override fun onUserJoined(uid: Int, elapsed: Int) {
            showMessage("Remote user joined $uid", requireContext())

            // Set the remote video view
//            requireActivity().runOnUiThread {
//                if (type == Constants.CLIENT_ROLE_AUDIENCE) {
//                    setupRemoteVideo(uid)
//                }
//            }
        }

        override fun onJoinChannelSuccess(channel: String, uid: Int, elapsed: Int) {
            isJoined = true
            showMessage("Joined Channel $channel, uid: $uid", requireContext())
//            runOnUiThread {
//                loginToChatAgora()
//            }
        }

        override fun onUserOffline(uid: Int, reason: Int) {
            showMessage("Remote user offline $uid $reason", requireContext())
            requireActivity().runOnUiThread {
//                remoteSurfaceView!!.visibility = View.GONE
                leaveChannel()
            }

        }

        override fun onConnectionLost() {
            Log.e(TAG, "connection lost")
        }

        override fun onLeaveChannel(stats: RtcStats?) {
            super.onLeaveChannel(stats)
        }
    }

//    private fun setupRemoteVideo(uid: Int) {
//        remoteSurfaceView = SurfaceView(baseContext)
//        remoteSurfaceView?.setZOrderMediaOverlay(true)
//        remoteContainer.addView(remoteSurfaceView)
//        agoraEngine?.setupRemoteVideo(
//            VideoCanvas(
//                remoteSurfaceView,
//                VideoCanvas.RENDER_MODE_FIT,
//                uid
//            )
//        )
//        // Display RemoteSurfaceView.
//        remoteSurfaceView?.visibility = View.VISIBLE
//    }

    private fun setupLocalVideo() {
        // Create a SurfaceView object and add it as a child to the FrameLayout.
        localSurfaceView = SurfaceView(requireActivity().baseContext)
        localContainer.addView(localSurfaceView)
        // Call setupLocalVideo with a VideoCanvas having uid set to 0.
        agoraEngine?.setupLocalVideo(
            VideoCanvas(
                localSurfaceView,
                VideoCanvas.RENDER_MODE_HIDDEN,
                0
            )
        )
    }

    private fun joinChannel() {
        if (checkSelfPermission()) {
            if (rtcToken.isNotEmpty() || uid == -1) {
                Log.e(TAG, "joining channel")
                try {
                    val options = ChannelMediaOptions()
                    // For Live Streaming, set the channel profile as LIVE_BROADCASTING.
                    options.channelProfile = Constants.CHANNEL_PROFILE_LIVE_BROADCASTING
                    // Set the client role as BROADCASTER or AUDIENCE according to the scenario.
                    options.clientRoleType = Constants.CLIENT_ROLE_BROADCASTER
//                    if (type == Constants.CLIENT_ROLE_BROADCASTER) { //Audience
                    setupLocalVideo()
                    localSurfaceView!!.visibility = View.VISIBLE
                    // Start local preview.
                    agoraEngine?.startPreview()
//                    }
                    // Join the channel with a temp token.
                    // You need to specify the user ID yourself, and ensure that it is unique in the channel.
                    agoraEngine?.joinChannel(rtcToken, channelName, uid, options)
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to join channel: ${e.message}")
                }
            } else {
                Log.e(TAG, "rtc token is empty or invalid uid: $uid")
//                finish()
            }
        } else {
            Toast.makeText(requireContext(), "Permissions was not granted", Toast.LENGTH_SHORT)
                .show()
        }
    }

    fun leaveChannel() {
        if (!isJoined) {
            showMessage("Join a channel first", requireContext())
            joinChannel()
        } else {
            agoraEngine?.leaveChannel()
            showMessage("You left the channel", requireContext())
            if (localSurfaceView != null) localSurfaceView!!.visibility = View.GONE
            isJoined = false
            findNavController().navigateUp()
        }
    }

    private fun updatePictureInPictureParams(): PictureInPictureParams? {
        // Calculate the aspect ratio of the PiP screen.
        val aspectRatio = Rational(50, 100)
        // The movie view turns into the picture-in-picture mode.
        val visibleRect = Rect()
//        binding.movie.getGlobalVisibleRect(visibleRect)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val paramsBuilder = PictureInPictureParams.Builder()
                .setAspectRatio(aspectRatio)
                // Specify the portion of the screen that turns into the picture-in-picture mode.
                // This makes the transition animation smoother.
                .setSourceRectHint(visibleRect)
            // The screen automatically turns into the picture-in-picture mode when it is hidden
            // by the "Home" button.

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                paramsBuilder.setAutoEnterEnabled(true).setSeamlessResizeEnabled(true)
            }

            val params = paramsBuilder.build()
            requireActivity().setPictureInPictureParams(params)
            return params
        } else {
            return null
        }
    }
}