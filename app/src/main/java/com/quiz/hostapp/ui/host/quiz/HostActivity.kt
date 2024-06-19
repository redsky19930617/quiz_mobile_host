package com.quiz.hostapp.ui.host.quiz
//import io.agora.agorauikit_android.*
import android.Manifest
import android.app.PictureInPictureParams
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.Rect
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.util.Rational
import android.view.SurfaceView
import android.view.View
import android.widget.CompoundButton
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.RadioGroup
import android.widget.RadioGroup.OnCheckedChangeListener
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.viewModels
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.Observer
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.google.android.material.switchmaterial.SwitchMaterial
import com.quiz.hostapp.R
import com.quiz.hostapp.ServiceLocator
import com.quiz.hostapp.network.apiService
import com.quiz.hostapp.ui.host.HostViewModel
import com.quiz.hostapp.utils.ConstUtils
import com.quiz.hostapp.utils.showMessage
import io.agora.agorauikit_android.AgoraVideoViewer
import io.agora.rtc2.*
import io.agora.rtc2.video.SegmentationProperty
import io.agora.rtc2.video.VideoCanvas
import io.agora.rtc2.video.VirtualBackgroundSource


class HostActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "HostActivity"
    }

    private lateinit var myBroadcastReceiver: MyBroadcastReceiver

    private val viewModel: HostViewModel by viewModels {
        ServiceLocator.provideViewModelFactory(
            this,
            apiService(this)
        )
    }
    private val PERMISSION_REQ_ID = 22
    private val REQUESTED_PERMISSIONS = arrayOf<String>(
        Manifest.permission.RECORD_AUDIO,
        Manifest.permission.CAMERA
    )

    private var agView: AgoraVideoViewer? = null

    // Fill the App ID of your project generated on Agora Console.
    private val appId = "b75cc48b972d4ccc92edb71a1c75fb23"

    // Fill the channel name.
    private val channelName = "test"

    // Fill the temp token generated on Agora Console.
    private lateinit var rtcToken: String

    private val APP_KEY = "ff54bdcd8a8f44c4913b868e5c45ba06"

    // An integer that identifies the local user.
    private var uid = -1
    private var isJoined = false

    private var agoraEngine: RtcEngine? = null
    private var isPip = false

    //SurfaceView to render local video in a Container.
    private var localSurfaceView: SurfaceView? = null
    private lateinit var localContainer: FrameLayout
    private lateinit var options: ChannelMediaOptions
    private lateinit var virtualBackgroundSource: VirtualBackgroundSource
    private lateinit var segmentationProperty: SegmentationProperty

    private var audioEffectManager: IAudioEffectManager? = null
    private val soundEffectId = 1 // Unique identifier for the sound effect file
    private val soundEffectIdTwo = 2
    private val soundEffectIdThree = 3
    private var currentEffectId = -1
    private val soundEffectAppluse =
        "https://www.soundjay.com/human/applause-01.mp3" // URL or path to the sound effect
    private val soundEffectFail =
        "https://www.soundjay.com/misc/sounds/fail-buzzer-01.mp3" // URL or path to the sound effect

    private val soundEffectClock =
        "https://assets.mixkit.co/active_storage/sfx/1047/1047-preview.mp3"
    private var soundEffectStatus = 0
    private var audioPlaying = false // Manage the audio mixing state


    private var audioFilePath = "https://www.kozco.com/tech/organfinale.mp3"
    //https://www.kozco.com/tech/organfinale.mp3

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_host)
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                // in here you can do logic when backPress is clicked
                goPip()
            }
        })
        localContainer = findViewById(R.id.local_video_view_container)
        rtcToken = intent.getStringExtra(ConstUtils.RTC_TOKEN) ?: ""
        uid = intent.getIntExtra(ConstUtils.UID, -1)

        findViewById<ImageView>(R.id.close_iv).setOnClickListener {
            leaveChannel()
        }

        setUpVirtualBackground()
        setupVideoSDKEngine()
        viewModel.muteLiveData.observe(this, Observer {
            it?.let {
                Log.e("host","mute:" + it)
                agoraEngine?.muteLocalAudioStream(it)

                agoraEngine?.muteAllRemoteAudioStreams(it)
            }
        })
        val vbSwitch = findViewById<SwitchMaterial>(R.id.virtual_bg_switch)
        vbSwitch.setOnCheckedChangeListener(object :OnCheckedChangeListener,
            CompoundButton.OnCheckedChangeListener {
            override fun onCheckedChanged(p0: RadioGroup?, p1: Int) {

            }

            override fun onCheckedChanged(p0: CompoundButton?, p1: Boolean) {
                enableVirtualBackground(p1)
            }
        })
    }

    override fun onUserLeaveHint() {
        goPip()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onPictureInPictureModeChanged(
        isInPictureInPictureMode: Boolean,
        newConfig: Configuration
    ) {
        isPip = isInPictureInPictureMode
//        if (lifecycle.currentState == Lifecycle.State.CREATED) {
//            //user clicked on close button of PiP window
//            finishAndRemoveTask()
//        }
//        else if (lifecycle.currentState == Lifecycle.State.STARTED){
//            if (isInPictureInPictureMode) {
//                // user clicked on minimize button
//            } else {
//                // user clicked on maximize button of PiP window
//            }
//        }
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
    }

    private fun goPip() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val params = updatePictureInPictureParams()
            params?.let {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    enterPictureInPictureMode(it)
                } else {
                    showMessage("PIP not supported on your device", this@HostActivity)
                }
            }
        } else {
            showMessage("PIP not supported on your device", this@HostActivity)
        }
    }

    private fun checkSelfPermission(): Boolean {
        return !(ContextCompat.checkSelfPermission(
            this,
            REQUESTED_PERMISSIONS[0]
        ) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(
                    this,
                    REQUESTED_PERMISSIONS[1]
                ) != PackageManager.PERMISSION_GRANTED)
    }

    private fun setupVideoSDKEngine() {
        try {
            Log.i(TAG, "starting agora enghine")
            val config = RtcEngineConfig()
            config.mContext = baseContext
            config.mAppId = appId
            config.mEventHandler = mRtcEventHandler
            agoraEngine = RtcEngine.create(config)
            // By default, the video module is disabled, call enableVideo to enable it.
            agoraEngine?.enableVideo()
            joinChannel()
        } catch (e: Exception) {
//            showMessage(e.toString(), this)
            Log.e(TAG, "erro set up video engine: ${e.message}")
        }
    }

    //Event handler for live streaming
    private val mRtcEventHandler: IRtcEngineEventHandler = object : IRtcEngineEventHandler() {
        // Listen for the remote host joining the channel to get the uid of the host.
        override fun onUserJoined(uid: Int, elapsed: Int) {
            runOnUiThread {
                Log.e(TAG, "uid: $uid")
//                showMessage("Remote user joined $uid", this@HostActivity)
            }
        }


        override fun onJoinChannelSuccess(channel: String, uid: Int, elapsed: Int) {
            isJoined = true
            Log.e(TAG, "joined channel: $channel")
            runOnUiThread {
                setAudioEffects()
//                showMessage("You are Live", this@HostActivity)
                Handler(Looper.getMainLooper()).postDelayed({
                    goPip()
                }, 2000)
//                loginToChatAgora()
            }
        }

        override fun onUserOffline(uid: Int, reason: Int) {
            runOnUiThread {
                
//                remoteSurfaceView!!.visibility = View.GONE
//                showMessage("Remote user offline $uid $reason", this@HostActivity)
                Log.e(TAG,"remote user goes offline: $uid")
//                leaveChannel()
            }

        }

        override fun onConnectionLost() {
            Log.e(TAG, "connection lost")
        }

        override fun onLeaveChannel(stats: RtcStats?) {
            super.onLeaveChannel(stats)
        }
    }

    private fun setupLocalVideo() {
        // Create a SurfaceView object and add it as a child to the FrameLayout.
        localSurfaceView = SurfaceView(baseContext)
        localContainer.addView(localSurfaceView)
        // Call setupLocalVideo with a VideoCanvas having uid set to 0.
        agoraEngine?.setupLocalVideo(
            VideoCanvas(
                localSurfaceView,
                VideoCanvas.RENDER_MODE_HIDDEN,
                uid
            )
        )
    }

    private fun joinChannel() {
        if (checkSelfPermission()) {
            if (rtcToken.isNotEmpty() || uid == -1) {

                Log.e(TAG, "joining channel")
                try {
                    options = ChannelMediaOptions()
                    // For Live Streaming, set the channel profile as LIVE_BROADCASTING.
                    options.channelProfile = Constants.CHANNEL_PROFILE_LIVE_BROADCASTING
                    // Set the client role as BROADCASTER or AUDIENCE according to the scenario.
                    options.clientRoleType = Constants.CLIENT_ROLE_BROADCASTER
                    setupLocalVideo()
                    localSurfaceView!!.visibility = View.VISIBLE
                    // Start local preview.
                    agoraEngine?.startPreview()
                    // Join the channel with a temp token.
                    // You need to specify the user ID yourself, and ensure that it is unique in the channel.
                    startLiveStreaming(options)
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to join channel: ${e.message}")
                }
            } else {
                Log.e(TAG, "rtc token is empty or invalid uid: $uid")
            }
        } else {
            Toast.makeText(this, "Permissions was not granted", Toast.LENGTH_SHORT)
                .show()
        }
    }

    private fun setUpVirtualBackground() {
        virtualBackgroundSource = VirtualBackgroundSource()
        virtualBackgroundSource.backgroundSourceType = VirtualBackgroundSource.BACKGROUND_COLOR
        val color = "#662FBF"
        val vColor = Integer.parseInt(color.replaceFirst("#", ""), 16)
        Log.e(TAG,"color is: $color, virtual color: $vColor")
        virtualBackgroundSource.color = vColor
        segmentationProperty = SegmentationProperty()
        segmentationProperty.modelType = SegmentationProperty.SEG_MODEL_AI // Use SEG_MODEL_GREEN if you have a green background
        segmentationProperty.greenCapacity = 0.5f
    }

    private fun enableVirtualBackground(enable:Boolean){
        agoraEngine?.enableVirtualBackground(enable,virtualBackgroundSource,segmentationProperty)
    }

    private fun startLiveStreaming(options: ChannelMediaOptions) {
        agoraEngine?.joinChannel(rtcToken, channelName, uid, options)
    }

    private fun stopAudio() {
        agoraEngine?.stopAudioMixing()
    }

    private fun stopSoundEffect() {
        Log.e(TAG, "current effect id: $currentEffectId")
        if (currentEffectId != -1) {
            audioEffectManager?.stopEffect(currentEffectId)
        }

    }

    private fun setAudioEffects() {
        // Set up the audio effects manager
        audioEffectManager = agoraEngine?.audioEffectManager
        audioEffectManager?.preloadEffect(soundEffectId, soundEffectAppluse)
        audioEffectManager?.preloadEffect(soundEffectIdTwo, soundEffectFail)
        audioEffectManager?.preloadEffect(soundEffectIdThree, soundEffectClock)
    }

    private fun playAudioEffect() {
        Log.e(TAG, "playing audio effect")
        try {
            agoraEngine?.startAudioMixing(audioFilePath, true, 1, 0)
            audioPlaying = true
        } catch (e: java.lang.Exception) {
            Log.e(TAG,"Exception playing audio\n$e")
        }
    }

    private fun playSoundEffect(effectId: Int, path: String, loop: Int = 0) {
        currentEffectId = effectId
        audioEffectManager?.playEffect(
            effectId,   // The ID of the sound effect file.
            path,   // The path of the sound effect file.
            loop,  // The number of sound effect loops. -1 means an infinite loop. 0 means once.
            1.0,   // The pitch of the audio effect. 1 represents the original pitch.
            0.0, // The spatial position of the audio effect. 0.0 represents that the audio effect plays in the front.
            100.0, // The volume of the audio effect. 100 represents the original volume.
            false,// Whether to publish the audio effect to remote users.
            0    // The playback starting position of the audio effect file in ms.
        )
    }

    fun leaveChannel() {
        if (!isJoined) {
//            showMessage("Join a channel first", this)
            joinChannel()
        } else {
            agoraEngine?.leaveChannel()
//            showMessage("You left the channel", this)
            if (localSurfaceView != null) localSurfaceView!!.visibility = View.GONE
            isJoined = false
            sendEndBroadCast()
            finish()
        }
    }

    private fun updatePictureInPictureParams(): PictureInPictureParams? {
        // Calculate the aspect ratio of the PiP screen.
        val aspectRatio = Rational(70, 100)
        // The movie view turns into the picture-in-picture mode.
        val visibleRect = Rect()
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
            setPictureInPictureParams(params)
            return params
        } else {
            return null
        }
    }

    private fun sendEndBroadCast() {
        Log.e(TAG, "sending end broadcast")
        val intent = Intent("end")
        intent.putExtra("data", true)
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent)
    }

    class MyBroadcastReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent) {
            // Handle the received data here
            val data = intent.getBooleanExtra("data", false)
            val green = intent.getBooleanExtra("green_screen",false)
            val playAudio = intent.getBooleanExtra("play_audio",false)
            Log.e(TAG, "end data: $data")
            Log.e(TAG, "audio data: $playAudio")
//            if(data) {
            val handler = (context as? HostActivity)?.broadCastHandler
            handler?.sendMessage(handler.obtainMessage(1, data))
            handler?.sendMessage(handler.obtainMessage(2, green))
            handler?.sendMessage(handler.obtainMessage(3, playAudio))
//            }
            // Update UI or perform any other operation
        }
    }

    override fun onResume() {
        super.onResume()
        Log.e(TAG, "onResume")
        myBroadcastReceiver = MyBroadcastReceiver()
        registerReceiver(myBroadcastReceiver, IntentFilter("live"))
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.e(TAG, "onDestroy")
        unregisterReceiver(myBroadcastReceiver)
        agoraEngine!!.stopPreview()
        agoraEngine!!.leaveChannel()

        // Destroy the engine in a sub-thread to avoid congestion

        // Destroy the engine in a sub-thread to avoid congestion
        Thread {
            RtcEngine.destroy()
            agoraEngine = null
        }.start()
    }

    // Handle the received data using a handler
    val broadCastHandler = Handler(Looper.getMainLooper()) { msg ->
        when (msg.what) {
            1 -> {
                val data = msg.obj as? Boolean
                Log.e(TAG, "end data: $data")
                data?.let {
                    if (data == true) leaveChannel()
                }
                // Do something with the received data
                // ...
                true
            }
            2->{
                val data = msg.obj as? Boolean
                data?.let {
                    enableVirtualBackground(it)
                }
                true
            }
            3->{
                val data = msg.obj as? Boolean
                data?.let {
                  if(data){
                      playSoundEffect(soundEffectIdThree,soundEffectClock,0)
                  }else{
                      stopSoundEffect()
                  }
                }
                true
            }
            else -> false
        }
    }
}