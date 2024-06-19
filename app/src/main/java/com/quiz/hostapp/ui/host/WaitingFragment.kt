package com.quiz.hostapp.ui.host

import android.Manifest
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.media.AudioManager
import android.os.Bundle
import android.os.CountDownTimer
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AlphaAnimation
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.navigation.fragment.findNavController
import androidx.paging.PagingData
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.google.android.material.button.MaterialButton
import com.quiz.hostapp.MainActivity
import com.quiz.hostapp.MyApplication.Companion.session
import com.quiz.hostapp.R
import com.quiz.hostapp.ServiceLocator
import com.quiz.hostapp.databinding.FragmentWaitingBinding
import com.quiz.hostapp.network.NetworkUtils
import com.quiz.hostapp.network.apiService
import com.quiz.hostapp.network.model.LeaderBoardResponse
import com.quiz.hostapp.network.model.LeaderBoardResult
import com.quiz.hostapp.network.model.QuestionData
import com.quiz.hostapp.network.model.asParticipantList
import com.quiz.hostapp.network.model.asQuizDetailsList
import com.quiz.hostapp.ui.host.quiz.HostActivity
import com.quiz.hostapp.utils.ConstUtils
import com.quiz.hostapp.utils.SessionManager
import com.quiz.hostapp.utils.getFormatMillis
import com.quiz.hostapp.utils.getRandomUid
import com.quiz.hostapp.utils.getTimeFromMillis
import io.agora.rtc2.IAudioEffectManager
import kotlinx.coroutines.Job
import org.json.JSONObject


class WaitingFragment : Fragment(), View.OnClickListener,
    QuizListAdapter.OnDisplayQuestionClickListener, QuizListAdapter.StartPrizePoolListener {
    private lateinit var audioManager: AudioManager
    private var countdownTimer: CountDownTimer? = null
    private var audioEffectManager: IAudioEffectManager? = null
    private var audioFilePath = "https://www.kozco.com/tech/organfinale.mp3"
    private lateinit var endBroadCastManager: BroadcastReceiver
    private val viewModel: HostViewModel by viewModels {
        ServiceLocator.provideViewModelFactory(
            requireContext(),
            apiService(requireContext())
        )
    }

    private lateinit var binding: FragmentWaitingBinding
    private lateinit var quizListAdapter: QuizListAdapter
    private lateinit var leaderBoardAdapter: LeaderBoardAdapter
    private lateinit var layoutManager: LinearLayoutManager
    private var uid = -1
    private var currentPage = 0
    private var totalPages = 0

    private var quizId: String = ""
    private lateinit var hostId: String
    private var isPermissionGranted = false
    private var ongoing = false
    private var isShowPool = false
    private var isDisPlayingQuestion = false
    private var counter = 0
    private var leaderBoardJob: Job? = null
    private var isGreenScreenEnabled = false
    private lateinit var quizCategory: String
    private lateinit var quizStartDate: String
    private var isMute: Boolean = true

    private val REQUESTED_PERMISSIONS = arrayOf<String>(
        Manifest.permission.RECORD_AUDIO,
        Manifest.permission.CAMERA
    )

    companion object {
        private const val TAG = "HostActivity"
        private const val PERMISSION_REQUEST_CODE = 22
        private const val TEMP_RTC_TOKEN =
            "007eJxTYMj//5kneGsW1/0L7/e7sNQsml1XWhol+DXVZttCM039PFcFhhRzc9Nkw+RkS5MkQ5NEi+QkY7OklFTDpMTExDSDZBNz0yW5qQ2BjAwPdpkzMEIhiM/CUJJaXMLAAABwHSAS"
    }

    private fun showEndDialog() {
        val layout = LayoutInflater.from(requireContext()).inflate(R.layout.en_stream_dialog, null)
        val builder = AlertDialog.Builder(requireContext())
        builder.setView(layout)
        val endButton = layout.findViewById<MaterialButton>(R.id.end_live_button)
        val pauseButton = layout.findViewById<MaterialButton>(R.id.pause_resume_live_button)
        val dialog = builder.create()
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.show()
        endButton.setOnClickListener {
            dialog.dismiss()
            if (isDisPlayingQuestion) {
                Toast.makeText(
                    requireContext(),
                    "Try after displaying current question",
                    Toast.LENGTH_LONG
                ).show()
            } else {
                val endJson = JSONObject()
                endJson.put("quiz_id", quizId)
                endJson.put("host_id", hostId)
                Log.e(TAG, "socket start input: $endJson")
                viewModel.sendMessage(endJson, NetworkUtils.SOCKET_HOST_LIVE_END)
                quizListAdapter.currentList.forEachIndexed { index, quizDetails ->
                    quizDetails.answerPercent = 0.0
                    quizDetails.showAnswer = false
                    quizDetails.showQuestion = false
                    quizDetails.answers.forEach {
                        it.isShowingOptions = false
                        it.isShowingQuestion = false
                    }
                    quizListAdapter.notifyItemChanged(index)
                }
                sendBroadcastWithData()
                findNavController().navigateUp()
            }
        }
        pauseButton.setOnClickListener {
            dialog.dismiss()
            if (isDisPlayingQuestion) {
                Toast.makeText(
                    requireContext(),
                    "Try after displaying current question",
                    Toast.LENGTH_LONG
                ).show()
            } else {
                ongoing = ongoing.not()
                binding.quizRecyclerview.isEnabled = ongoing
                quizListAdapter.setStreamState(ongoing)
                updateButtonState(true, true)
                changeLiveStreamState()
            }
        }
    }

    private fun updatePauseResume() {
        if (ongoing.not()) {
            binding.pauseResumeLayout.visibility = View.VISIBLE
            binding.timerGroup.visibility = View.VISIBLE
        } else {
            binding.timerGroup.visibility = View.GONE
            binding.counterGroup.visibility = View.VISIBLE
            startCounter()
        }
    }

    private fun updateShowPrizePool() {
        if (isShowPool) {
            binding.resultGroup.visibility = View.VISIBLE
            binding.leaderboardLayout.visibility = View.GONE
        } else {
            binding.resultGroup.visibility = View.GONE
            binding.leaderboardLayout.visibility = View.VISIBLE
        }
    }

    private fun startCounter() {
        countdownTimer = object : CountDownTimer(5000, 1000) {
            override fun onTick(p0: Long) {
                try {
                    val anim = AlphaAnimation(1.0f, 0.0f)
                    anim.duration = 950
                    anim.repeatCount = 1
                    anim.repeatMode = Animation.REVERSE
                    binding.counterTv.startAnimation(anim)
                    binding.counterTv.text = "${1 + (p0 / 1000)}"
                } catch (e: Exception) {
                    Log.e(TAG, "counter exception: ${e.message}")
                }
            }

            override fun onFinish() {
                binding.counterGroup.visibility = View.GONE
                binding.pauseResumeLayout.visibility = View.GONE
            }
        }
        countdownTimer?.start()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        activity?.onBackPressedDispatcher?.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (ongoing) {
                    showEndDialog()
                } else {
                    findNavController().navigateUp()
                }
            }
        })
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        binding = FragmentWaitingBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onStart() {
        super.onStart()

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        setMicrophoneMute(false)
        isMute = false
        // Define the broadcast receiver
        endBroadCastManager = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                // Call the method of the containing activity
                val data = intent?.getBooleanExtra("data", false)
                Log.e(TAG, "end data after live: $data")
                data?.let {
                    ongoing = false
                    updateButtonState(it.not())
                    binding.calculateScoreButton.visibility = View.GONE
                    updateLayoutVisibility(HostUiState.ASKING)
                }
            }
        }
        LocalBroadcastManager.getInstance(requireContext())
            .registerReceiver(endBroadCastManager, IntentFilter("end"))

        hostId = session(requireContext()).getPrefString(SessionManager.USER_ID) ?: ""
        quizId = requireArguments().getString("quiz_id", "")
        quizCategory = requireArguments().getString("category", "")
        quizStartDate = requireArguments().getString("start_date", "")
        binding.categoryTv.text = "Category\n$quizCategory"
        val quizTimeInMilli = quizStartDate.getFormatMillis()
        binding.quizDateTimeTv.text = "Quiz Date\n${
            getTimeFromMillis(
                quizTimeInMilli,
                "d MMM yyyy, h:mm a"
            )
        }"

        viewModel.getQuestions(quizId)

        setUpAdapters()
        isPermissionGranted = isPermissionGranted()
        Log.e(TAG, "is permission granted: $isPermissionGranted")
        registerObservers()
        setClicks()
    }

    private fun setUpAdapters() {
        quizListAdapter = QuizListAdapter(this)
        binding.quizRecyclerview.adapter = quizListAdapter
    }

    private fun setUpLeaderBoardAdapter() {
        layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)
        leaderBoardAdapter = LeaderBoardAdapter()
        binding.leaderBoardRecyclerview.layoutManager = layoutManager
        binding.leaderBoardRecyclerview.adapter = leaderBoardAdapter
//        leaderBoardAdapter.addLoadStateListener { combinedLoadStates ->
//            val isEmptyList =
//                combinedLoadStates.refresh is LoadState.NotLoading && leaderBoardAdapter.itemCount == 0
//            val isPopulated =
//                combinedLoadStates.refresh is LoadState.NotLoading && leaderBoardAdapter.itemCount > 0
//            if (isPopulated) {
//                counter++
//            }
//            Log.e(TAG, "is empty list: $isEmptyList, is Populated: $isPopulated, counter: $counter")
//        }
    }

    private fun getLeaderBoard(currentPage: Int, queryDetails: Map<String, Any>) {
        viewModel.getLeaderBoardData(quizId, queryDetails)
    }

    private fun initLeaderBoardData() {
//        lifecycleScope.launch {
//            leaderBoardAdapter.loadStateFlow
//                .distinctUntilChangedBy { it.refresh }
//                .filter { it.refresh is LoadState.NotLoading }
//                .collect {
//                    val snapshot = leaderBoardAdapter.snapshot()
//                    val itemCount = snapshot.items.size
//                    Log.e(TAG, "no. of items in ui: $itemCount")
////                    binding.leaderBoardRecyclerview.scrollToPosition(0)
//
//                    binding.calculateLayout.visibility = View.GONE
//                    if (itemCount > 0 && counter == 1) {
//                        Log.e(TAG, "updating leaderboard")
//                        updateLayoutVisibility(HostUiState.SHOW_LEADERBOARD)
//
//                    }
//
//                }
//
//
//        }
    }

    private suspend fun updateLeaderBoard(it: PagingData<Participant>) {
        Log.e(TAG, "submitting data and showing in the ui")
//        leaderBoardAdapter.submitTotalQuestions(6)
//        leaderBoardAdapter.submitData(it)
//        leaderBoardAdapter.notifyDataSetChanged()

    }

    private fun setClicks() {
        binding.logOutButton.setOnClickListener(this)
        binding.goLiveButton.setOnClickListener(this)
        binding.endLiveButton.setOnClickListener(this)
        binding.pauseResumeLiveButton.setOnClickListener(this)
        binding.calculateScoreButton.setOnClickListener(this)
        binding.pauseResumeLiveButton.setOnClickListener(this)
        binding.greenScreenButton.setOnClickListener(this)
        binding.backIv.setOnClickListener(this)
        binding.showPrizePoolButton.setOnClickListener(this)
        binding.greenMuteButton.setOnClickListener(this)
    }

    private fun updateButtonState(
        isLive: Boolean,
        showPause: Boolean = true
    ) {
        binding.imageView3.isVisible = isLive
        binding.goLiveButton.isVisible = isLive.not()
        binding.endLiveContainer.isVisible = isLive
        binding.pauseResumeLiveButton.isVisible = showPause
        binding.pauseResumeLiveButton.setBackgroundColor(Color.parseColor(if (ongoing.not()) "#3FB26D" else "#FDC453"))
        binding.pauseResumeLiveButton.setTextColor(if (ongoing.not()) Color.WHITE else Color.BLACK)
        binding.pauseResumeLiveButton.text = if (ongoing.not()) "Resume" else "Pause"
    }

    private fun updateLayoutVisibility(
        hostUiState: HostUiState
    ) {
        Log.e(TAG, "changing ui state to: $hostUiState")
        binding.questionLayout.isVisible = hostUiState == HostUiState.ASKING
        binding.calculateLayout.isVisible = hostUiState == HostUiState.CALCULATING
    }

    private fun registerObservers() {

        viewModel.leaderBoardLiveData.observe(viewLifecycleOwner) {
            it?.let {
                Log.e(TAG, "no. of leaderboard item: ${it.data}")
                currentPage = 0
                totalPages = 1

                if (it.data.isEmpty()) {
                    updateLayoutVisibility(HostUiState.SHOW_LEADERBOARD)
                    binding.winnerTv.isVisible = false
                    binding.winnerIv.isVisible = false
                    binding.secondTv.isVisible = false
                    binding.secondIv.isVisible = false
                    binding.thirdIv.isVisible = false
                    binding.thirdTv.isVisible = false
                } else {
                    if (it.data.size < 3) {

                        if (it.data.size == 2) {

                            var winnerDetail: String = getString(R.string.dummy_winner_detail,
                                it.data[0].userName, it.data[0].rewardAmount)
                            var secondDetail: String = getString(R.string.dummy_winner_detail,
                                it.data[1].userName, it.data[1].rewardAmount)

                            binding.thirdIv.isVisible = false
                            binding.thirdTv.isVisible = false

                            binding.winnerTv.text = winnerDetail
                            binding.secondTv.text = secondDetail

                            Glide.with(this)
                                .load(it.data.get(0).avatar)
                                .placeholder(R.drawable.profile)
                                .circleCrop()   // This makes the loaded image circular
                                .into(binding.winnerIv)

                            Glide.with(this)
                                .load(it.data.get(1).avatar)
                                .placeholder(R.drawable.profile)
                                .circleCrop()   // This makes the loaded image circular
                                .into(binding.secondIv)

                        } else {

                            var winnerDetail: String = getString(R.string.dummy_winner_detail,
                                it.data[0].userName, it.data[0].rewardAmount)


                            binding.secondTv.isVisible = false
                            binding.secondIv.isVisible = false
                            binding.thirdIv.isVisible = false
                            binding.thirdTv.isVisible = false
                            binding.winnerTv.text = winnerDetail

                            Glide.with(this)
                                .load(it.data.get(0).avatar)
                                .placeholder(R.drawable.profile)
                                .circleCrop()   // This makes the loaded image circular
                                .into(binding.winnerIv)

                        }
                    } else {

                        var winnerDetail: String = getString(R.string.dummy_winner_detail,
                            it.data[0].userName, it.data[0].rewardAmount)
                        var secondDetail: String = getString(R.string.dummy_winner_detail,
                            it.data[1].userName, it.data[1].rewardAmount)
                        var thirdDetail: String = getString(R.string.dummy_winner_detail,
                            it.data[2].userName, it.data[2].rewardAmount)

                        binding.winnerTv.text = winnerDetail
                        binding.secondTv.text = secondDetail
                        binding.thirdTv.text = thirdDetail

                        Glide.with(this)
                            .load(it.data.get(0).avatar)
                            .placeholder(R.drawable.profile)
                            .circleCrop()   // This makes the loaded image circular
                            .into(binding.winnerIv)

                        Glide.with(this)
                            .load(it.data.get(1).avatar)
                            .placeholder(R.drawable.profile)
                            .circleCrop()   // This makes the loaded image circular
                            .into(binding.secondIv)

                        Glide.with(this)
                            .load(it.data.get(2).avatar)
                            .placeholder(R.drawable.profile)
                            .circleCrop()   // This makes the loaded image circular
                            .into(binding.thirdIv)

                    }
                    Handler(Looper.getMainLooper()).postDelayed({
                        updateLayoutVisibility(HostUiState.SHOW_LEADERBOARD)
                        leaderBoardAdapter.submitTotalQuestions(it.data.get(0).totalQuestions)
                        leaderBoardAdapter.submitList(it.data.asParticipantList())

                    }, 1000)
                }
            }
        }

//        viewModel.leaderBoardLiveData.observe(viewLifecycleOwner) {
//            it?.let {
//                Log.e(TAG, "no. of leaderboard item: ${it.data}")
//                currentPage = 0
//                totalPages = 1
//
//                val avatarViews = listOf(binding.winnerIv, binding.secondIv, binding.thirdIv)
//                val textViews = listOf(binding.winnerTv, binding.secondTv, binding.thirdTv)
//
//                for (i in it.data.indices) {
//                    textViews[i].text = getWinnerDetail(it.data[i])
//                    bindAvatar(it.data[i], avatarViews[i])
//                    avatarViews[i].isVisible = true
//                    textViews[i].isVisible = true
//                }
//
//                // If less than 3 users, hide remaining image and text views
//                for (i in it.data.size until 3) {
//                    avatarViews[i].isVisible = false
//                    textViews[i].isVisible = false
//                }
//
//                Handler(Looper.getMainLooper()).postDelayed({
//                    leaderBoardAdapter.submitTotalQuestions(it.data[0].totalQuestions)
//                    leaderBoardAdapter.submitList(it.data.asParticipantList())
//                    updateLayoutVisibility(HostUiState.SHOW_LEADERBOARD)
//                }, 1000)
//            }
//        }

        viewModel.questionsLiveData.observe(viewLifecycleOwner) {
            it?.let {
                updateUi(it.data)
            }
        }

        viewModel.loadingLiveData.observe(viewLifecycleOwner) {
            it?.let {
                binding.waitingProgressbar.isVisible = it == Loading.PROGRESS
                binding.quizRecyclerview.isVisible = it != Loading.PROGRESS
            }
        }

        viewModel.rtcTokenLiveData.observe(viewLifecycleOwner) {
            it?.let {
                ongoing = it.data.isNotEmpty()
                updateButtonState(it.data.isNotEmpty())
                if (it.data.isEmpty().not()) {
                    quizListAdapter.currentList[0].showQuestion = true
                    quizListAdapter.notifyItemChanged(0)
                    startLiveStreaming(it.data)
                }
            }
        }

        viewModel.getMessageLiveData().observe(viewLifecycleOwner) {
            it?.let {
                Log.e(TAG, "socket calculation data : $it")
                HashMap<String, Any>().apply {
                    put("user_rank", false)
                    put("page", "1")
                    put("limit", 50)
                    getLeaderBoard(currentPage, this)
                }
            }
        }

        viewModel.emojiLiveData.observe(viewLifecycleOwner, Observer {
            it?.let {
                Log.e(TAG, "emoji no in ui: $it")
                startBubbleAnimation(binding.buttonLayout, it)
            }
        })

        viewModel.viewerCountLiveData.observe(viewLifecycleOwner) {
            it?.let {
                binding.viewerCountTv.text = if (it >= 0) "$it" else "0"
                binding.viewerCountChannel.text = if (it >= 0) "$it" else "0"
            }
        }

        viewModel.viewPoolDataLiveData.observe(viewLifecycleOwner) {
            it?.let {
                binding.amount.text = "$" + it.pool.toString()
                binding.playCount.text = it.playCount.toString()
                binding.viewerCountChannel.text = it.viewerCount.toString()
                binding.titleAmount.text = "$" + it.pool.toString()
            }
        }

        viewModel.answerResultLiveData.observe(viewLifecycleOwner) {
            it?.let {
                Log.e(TAG,"answerPercent"+it.percent)
                val quiz = quizListAdapter.currentList[it.position!!]
                quiz?.showAnswer = true
                quiz?.showQuestion = false
                quiz?.answerPercent = it.percent!!
                quizListAdapter.notifyItemChanged(it.position!!)
            }
        }
    }


    private fun bindAvatar(data: LeaderBoardResult, imageView: ImageView) {  // Replace `YourDataType` with actual type of your `data`
        Glide.with(this)
            .load(data.avatar)
            .placeholder(R.drawable.profile)
            .circleCrop()
            .into(imageView)
    }

    private fun getWinnerDetail(data: LeaderBoardResult): String {
        return getString(R.string.dummy_winner_detail,
            data.userName, data.rewardAmount)
    }

    private fun startBubbleAnimation(clickedView: View, position: Int) {
        val animation: Animation =
            AnimationUtils.loadAnimation(requireContext(), R.anim.bubble_animation)

        val drawable: Int = when (position) {
            0 -> R.drawable.emo1
            1 -> R.drawable.emo2
            2 -> R.drawable.emo3
            3 -> R.drawable.emo4
            4 -> R.drawable.emo5
            5 -> R.drawable.emo6
            6 -> R.drawable.emo7
            else -> R.drawable.emo8
        }
        val parentLayout = binding.mainContainer
        val location = IntArray(2)
        clickedView.getLocationOnScreen(location)
        val startX = location[0].plus(100)
        val startY = location[1]

        Log.e(TAG, "start: $startX, start y: $startY")

        val bubbleImageView = ImageView(requireContext())
        bubbleImageView.setImageResource(drawable)

        bubbleImageView.x = startX.toFloat()
        bubbleImageView.y = startY.toFloat()

        // Start the bubble animation
        bubbleImageView.startAnimation(animation)

        // Add the ImageView to the parent layout
        parentLayout.addView(bubbleImageView)
        animation.setAnimationListener(object : Animation.AnimationListener {
            override fun onAnimationStart(animation: Animation?) {
                animation?.let {
                    bubbleImageView.visibility = View.VISIBLE
                }
            }

            override fun onAnimationEnd(animation: Animation?) {
                // Remove the bubble ImageView from the parent layout
                animation?.let {
                    bubbleImageView.visibility = View.GONE
                }
            }

            override fun onAnimationRepeat(animation: Animation?) {}
        })
    }

    private fun startLiveStreaming(token: String) {
        sendStartStreamSocket()
        val intent = Intent(requireActivity(), HostActivity::class.java).apply {
            putExtra(ConstUtils.RTC_TOKEN, token)
            putExtra(ConstUtils.UID, uid)
        }
//          startActivity(intent)
    }

    private fun sendStartStreamSocket() {
        val startJson = JSONObject()
        startJson.put("quiz_id", quizId)
        startJson.put("host_id", hostId)
        Log.e(TAG, "socket start input: $startJson")
        viewModel.sendMessage(startJson, NetworkUtils.SOCKET_HOST_LIVE_START)
    }

    private fun updateUi(questionData: QuestionData) {
        quizId = questionData.id
        quizListAdapter.submitList(questionData.questions.asQuizDetailsList())
    }

    private fun isPermissionGranted(): Boolean {
        return !(ContextCompat.checkSelfPermission(
            requireContext(),
            REQUESTED_PERMISSIONS[0]
        ) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(
                    requireContext(),
                    REQUESTED_PERMISSIONS[1]
                ) != PackageManager.PERMISSION_GRANTED)
    }

    private fun checkSelfPermission() {
        if (ContextCompat.checkSelfPermission(
                requireContext(),
                REQUESTED_PERMISSIONS[0]
            ) == PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(
                requireContext(),
                REQUESTED_PERMISSIONS[1]
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            isPermissionGranted = true
        } else {
            requestPermissions(REQUESTED_PERMISSIONS, PERMISSION_REQUEST_CODE)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>, grantResults: IntArray
    ) {
        when (requestCode) {
            PERMISSION_REQUEST_CODE -> {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.isNotEmpty() && grantResults.size > 1) {
                    if ((grantResults[0] == PackageManager.PERMISSION_GRANTED) && (grantResults[1] == PackageManager.PERMISSION_GRANTED)) {
                        isPermissionGranted = true
                    }
                    // Permission is granted. Continue the action or workflow
                    // in your app.
                } else {
                    // Explain to the user that the feature is unavailable because
                    // the feature requires a permission that the user has denied.
                    // At the same time, respect the user's decision. Don't link to
                    // system settings in an effort to convince the user to change
                    // their decision.
                    isPermissionGranted = false
                }
                return
            }

            // Add other 'when' lines to check for other
            // permissions this app might request.
            else -> {
                // Ignore all other requests.
            }
        }
    }

    override fun onClick(p0: View?) {
        when (p0) {
            binding.logOutButton -> {

            }

            binding.backIv -> {
                if (ongoing) {
                    showEndDialog()
                } else {
                    findNavController().navigateUp()
                }
            }

            binding.greenScreenButton -> {
                if (ongoing.not()) {
                    Toast.makeText(requireContext(), "Start Streaming First", Toast.LENGTH_SHORT)
                        .show()
                    return
                }
                isGreenScreenEnabled = !isGreenScreenEnabled
                binding.greenScreenButton.setImageDrawable(
                    ContextCompat.getDrawable(
                        requireContext(),
                        if (isGreenScreenEnabled) R.drawable.green_screen_on else R.drawable.green_screen_off
                    )
                )
                sendGreenScreenData()
            }

            binding.goLiveButton -> {
                if (isPermissionGranted) {
//                    viewModel.deleteQuiz().apply {
                    uid = getRandomUid()
                    viewModel.getRtcToken("test", ConstUtils.TYPE_HOST, uid)
//                    }
                } else {
                    checkSelfPermission()
                }
            }

            binding.endLiveButton -> {
                if (isDisPlayingQuestion) {
                    Toast.makeText(
                        requireContext(),
                        "Try after displaying current question",
                        Toast.LENGTH_LONG
                    ).show()
                    return
                }
                val endJson = JSONObject()
                endJson.put("quiz_id", quizId)
                endJson.put("host_id", hostId)
                Log.e(TAG, "socket start input: $endJson")
                viewModel.sendMessage(endJson, NetworkUtils.SOCKET_HOST_LIVE_END)
                quizListAdapter.currentList.forEachIndexed { index, quizDetails ->
                    quizDetails.showAnswer = false
                    quizDetails.showQuestion = false
                    quizListAdapter.notifyItemChanged(index)
                }
                sendBroadcastWithData()
                findNavController().navigateUp()
            }

            binding.calculateScoreButton -> {
                val calculationStartJson = JSONObject()
                calculationStartJson.put("quiz_id", quizId)
                calculationStartJson.put("host_id", hostId)
                Log.e(TAG, "socket calculation start input: $calculationStartJson")
                viewModel.sendMessage(calculationStartJson, NetworkUtils.SOCKET_CALCULATION_START)
                Toast.makeText(requireContext(), "calculate", Toast.LENGTH_SHORT).show()
                updateButtonState(true, false)
                updateLayoutVisibility(HostUiState.CALCULATING)
                setUpLeaderBoardAdapter()
                initLeaderBoardData()
            }

            binding.pauseResumeLiveButton -> {
                if (isDisPlayingQuestion) {
                    Toast.makeText(
                        requireContext(),
                        "Try after displaying current question",
                        Toast.LENGTH_LONG
                    ).show()
                    return
                }
                ongoing = ongoing.not()
                binding.quizRecyclerview.isEnabled = ongoing
                quizListAdapter.setStreamState(ongoing)
                updateButtonState(true, true)
                updatePauseResume()
                changeLiveStreamState()
            }

            binding.showPrizePoolButton -> {
                changeShowPrizeState()
                updateShowPrizePool()
            }

            binding.greenMuteButton -> {
                isMute = isMute.not()
                val json = JSONObject().apply {
                    put("quiz_id", quizId)
                    put("host_id", hostId)
                    put("status", if (isMute) "paused" else "ongoing")
                }

                Log.e(TAG, "live stream state change to: $json")
                viewModel.sendMessage(json, NetworkUtils.SOCKET_MUTE_STATE)
                setMicrophoneMute(isMute)
//                (context?.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager)?.let { notificationManager ->
//                    if (notificationManager.isNotificationPolicyAccessGranted) {
//                        (context?.getSystemService(Context.AUDIO_SERVICE) as? AudioManager)?.let { audioManager ->
//                            if (isMute) {
//                                audioManager.setStreamVolume(
//                                    AudioManager.STREAM_MUSIC,
//                                    AudioManager.ADJUST_MUTE,
//                                    0
//                                )
////                                audioManager.setStreamVolume(AudioManager.STREAM_NOTIFICATION, AudioManager.ADJUST_MUTE, 0)
////                                audioManager.setStreamVolume(AudioManager.STREAM_ALARM, AudioManager.ADJUST_MUTE, 0)
////                                audioManager.setStreamVolume(AudioManager.STREAM_RING, AudioManager.ADJUST_MUTE, 0)
//                                audioManager.setStreamVolume(
//                                    AudioManager.STREAM_SYSTEM,
//                                    AudioManager.ADJUST_MUTE,
//                                    0
//                                )
//                                binding.greenMuteButton.setBackgroundResource(R.drawable.mute_on)
//                            } else {
//                                audioManager.setStreamVolume(
//                                    AudioManager.STREAM_MUSIC,
//                                    AudioManager.ADJUST_UNMUTE,
//                                    0
//                                )
////                                audioManager.setStreamVolume(AudioManager.STREAM_NOTIFICATION, AudioManager.ADJUST_UNMUTE, 0)
////                                audioManager.setStreamVolume(AudioManager.STREAM_ALARM, AudioManager.ADJUST_UNMUTE, 0)
////                                audioManager.setStreamVolume(AudioManager.STREAM_RING, AudioManager.ADJUST_UNMUTE, 0)
//                                audioManager.setStreamVolume(
//                                    AudioManager.STREAM_SYSTEM,
//                                    AudioManager.ADJUST_UNMUTE,
//                                    0
//                                )
//                                binding.greenMuteButton.setBackgroundResource(R.drawable.mute_off)
//                            }
//                        }
//                    } else {
//                        // Ask the user to grant the permission.
//                        startActivity(Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS))
//                    }
//                }


            }
        }
    }

    private fun changeLiveStreamState() {

        val json = JSONObject()
        json.put("quiz_id", quizId)
        json.put("host_id", hostId)
        json.put("status", if (ongoing) "ongoing" else "paused")
        Log.e(TAG, "live stream state change to: $json")
        viewModel.sendMessage(json, NetworkUtils.SOCKET_LIVE_STREAM_STATE_CHANGE)

    }

    private fun changeShowPrizeState() {
        isShowPool = isShowPool.not()
        val json = JSONObject()
        json.put("quiz_id", quizId)
        json.put("host_id", hostId)
        json.put("status", if (isShowPool) "show" else "hide")
        viewModel.sendMessage(json, NetworkUtils.SOCKET_SHOW_POOL)
    }

    private fun sendBroadcastWithData() {
        Log.e(TAG, "sending broadcast")
        val intent = Intent("live")
        intent.putExtra("data", true)
        (requireActivity() as MainActivity).sendBroadcast(intent)
    }

    private fun sendGreenScreenData() {
        val intent = Intent("live")
        intent.putExtra("green_screen", isGreenScreenEnabled)
        (requireActivity() as MainActivity).sendBroadcast(intent)
    }

    private fun playAudioEffectBroadcast(play: Boolean) {
        val intent = Intent("live")
        intent.putExtra("play_audio", play)
        (requireActivity() as MainActivity).sendBroadcast(intent)
    }


    override fun onDestroy() {
        super.onDestroy()
        countdownTimer?.cancel()
        Log.e(TAG, "onDestroy")
        LocalBroadcastManager.getInstance(requireContext()).unregisterReceiver(endBroadCastManager)
    }

    override fun onDisplayQuestionClick(quizDetails: QuizDetails, position: Int) {
        Log.e(TAG, "on display question click: $quizDetails")
        Log.e(TAG, "user id: $hostId")
        isDisPlayingQuestion = true
        val questionJson = JSONObject()
        questionJson.put("quiz_id", quizId)
        questionJson.put("host_id", hostId)
        questionJson.put("question_id", quizDetails.id)
        questionJson.put("question_index", position.plus(1))
        Log.e(TAG, "socket question input: $questionJson")
        viewModel.sendMessage(questionJson, NetworkUtils.SOCKET_DISPLAY_QUESTION)
    }

    override fun onDisPlayOptionsClick(quizDetails: QuizDetails, position: Int) {
        Log.e(TAG, "on display options click: $quizDetails")
        val optionJson = JSONObject()
        optionJson.put("quiz_id", quizId)
        optionJson.put("host_id", hostId)
        optionJson.put("question_id", quizDetails.id)
        Log.e(TAG, "socket options input: $optionJson")
        viewModel.sendMessage(optionJson, NetworkUtils.SOCKET_DISPLAY_OPTION)
        playAudioEffectBroadcast(true)
    }

    override fun onQuestionEnd(position: Int, questionId: String) {
        val questionJson = JSONObject()


        questionJson.put("quiz_id", quizId)
        questionJson.put("host_id", hostId)
        questionJson.put("question_id", questionId)
        Log.e(TAG, "socket end question input: $questionJson")
        viewModel.sendMessage(questionJson, NetworkUtils.SOCKET_END_QUESTION)
        val quiz = quizListAdapter.currentList[position]
        quiz?.showAnswer = true
        quiz?.showQuestion = false
        quizListAdapter.notifyItemChanged(position)
        playAudioEffectBroadcast(false)
        Handler(Looper.getMainLooper()).postDelayed({
            if (quizListAdapter.currentList.size > position.plus(1)) {
                val nextQuiz = quizListAdapter.currentList[position.plus(1)]
                nextQuiz.showQuestion = true
                quizListAdapter.notifyItemChanged(position.plus(1))
                val nextpositon = position.plus(2)
                binding.quizRecyclerview.smoothScrollToPosition(nextpositon)
            }
//            else {
//                binding.calculateScoreButton.visibility = View.VISIBLE
//            }
            if (position >= 1) {
                binding.calculateScoreButton.visibility = View.VISIBLE
            } else {
                binding.calculateScoreButton.visibility = View.GONE
            }
            isDisPlayingQuestion = false
        }, 3000)

    }

    override fun onStartPrizePollListener() {
        Log.e(TAG, "click")
//        findNavController().navigate(
//            WaitingFragmentDirections.actionWaitingFragmentToResultFragment()
//        )
        Handler(Looper.getMainLooper()).postDelayed({
            audioEffectManager?.playEffect(
                3,   // The ID of the sound effect file.
                audioFilePath,   // The path of the sound effect file.
                0,  // The number of sound effect loops. -1 means an infinite loop. 0 means once.
                1.0,   // The pitch of the audio effect. 1 represents the original pitch.
                0.0, // The spatial position of the audio effect. 0.0 represents that the audio effect plays in the front.
                100.0, // The volume of the audio effect. 100 represents the original volume.
                false,// Whether to publish the audio effect to remote users.
                0    // The playback starting position of the audio effect file in ms.
            )
        }, 2000)

        if (isDisPlayingQuestion) {
            Toast.makeText(
                requireContext(),
                "Try after displaying current question",
                Toast.LENGTH_LONG
            ).show()
            return
        }
        changeShowPrizeState()
        updateShowPrizePool()
    }

    fun setMicrophoneMute(state: Boolean) {
        val audioManager = context?.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        audioManager.mode = AudioManager.MODE_IN_CALL
        audioManager.isMicrophoneMute = state
        if (state) {
            binding.greenMuteButton.setBackgroundResource(R.drawable.mute_on)
        } else {
            binding.greenMuteButton.setBackgroundResource(R.drawable.mute_off)
        }

    }
}