package com.quiz.hostapp.ui.host

import android.app.Application
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.google.gson.Gson
import com.quiz.hostapp.data.leaderboard.LeaderBoardPagingSource
import com.quiz.hostapp.data.leaderboard.LeaderBoardRepository
import com.quiz.hostapp.network.ApiService
import com.quiz.hostapp.network.NetworkUtils
import com.quiz.hostapp.network.model.LeaderBoardResponse
import com.quiz.hostapp.network.model.QuestionResponse
import com.quiz.hostapp.network.model.RtcTokenResponse
import com.quiz.hostapp.network.model.ShowPoolResponse
import com.quiz.hostapp.utils.SingleLiveEvent
import com.quiz.hostapp.utils.SocketManager
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.json.JSONException
import org.json.JSONObject
import android.os.Handler
import android.os.Looper
import android.media.MediaPlayer
import androidx.lifecycle.AndroidViewModel
import com.quiz.hostapp.R
import com.quiz.hostapp.network.model.AnswerResultResponse

enum class Loading {
    PROGRESS, SUCCESS, FAILED
}

class HostViewModel(
    private val application: Application,
    private val apiService: ApiService,
    private val leaderBoardRepository: LeaderBoardRepository
) : AndroidViewModel(application), SocketManager.ConnectionListener {

    companion object {
        private const val TAG = "HostViewModel"
    }

    private val _questionsMutableLiveData = SingleLiveEvent<QuestionResponse>()
    val questionsLiveData: LiveData<QuestionResponse> = _questionsMutableLiveData

    private var _loadingMutableLiveData = MutableLiveData<Loading>()
    val loadingLiveData: LiveData<Loading> = _loadingMutableLiveData

    private var _rtcTokenMutableLiveData = SingleLiveEvent<RtcTokenResponse>()
    val rtcTokenLiveData: LiveData<RtcTokenResponse> = _rtcTokenMutableLiveData

    private var _testMutableLiveData = MutableLiveData<Int>()
    val testLiveData: LiveData<Int> = _testMutableLiveData

    private var _leaderBoardLiveData = SingleLiveEvent<LeaderBoardResponse>()
    val leaderBoardLiveData: LiveData<LeaderBoardResponse> = _leaderBoardLiveData

    private var _muteLiveData = MutableLiveData<Boolean>()
    val muteLiveData: LiveData<Boolean> = _muteLiveData
    fun setMute(mute: Boolean) {
        _muteLiveData.postValue(mute);
    }

    fun getQuestions(quizId: String) {
        viewModelScope.launch {
            try {
                _loadingMutableLiveData.value = Loading.PROGRESS
                val response = apiService.getQuestionsList(quizId)
                if (response.isSuccessful) {
                    response.body()?.let {
                        Log.e(TAG, "questions response is successful: $it")
                        _questionsMutableLiveData.value = it
                    } ?: Log.e(TAG, "questions response body is null")
                    _loadingMutableLiveData.value = Loading.SUCCESS
                } else {
                    val error = response.errorBody()?.string()
                    Log.e(TAG, "response is unsuccessful: $error")
                    _loadingMutableLiveData.value = Loading.FAILED
                }
            } catch (e: Exception) {
                Log.e(TAG, "response failed: ${e.message}")
                _loadingMutableLiveData.value = Loading.FAILED
            }
        }
    }

    fun getRtcToken(channelName: String, role: String, uid: Int) {

        Log.e(TAG, "input: $channelName: $channelName, role: $role, uid: $uid")
        viewModelScope.launch {
            try {
                Log.e(TAG, "getting rtc")
                val response = apiService.getRtcToken(channelName, role, "uid", uid)
                if (response.isSuccessful) {
                    response.body()?.let {
                        Log.e(TAG, "rtc token response: $it")
                        _rtcTokenMutableLiveData.value = it
                    } ?: Log.e(TAG, "rtc token body is null")
                } else {
                    val error = response.errorBody()?.string()
                    Log.e(TAG, "rtc token response unsuccessful: $error")
                }
            } catch (e: Exception) {

                Log.e(TAG, "Failed to get rtc token: ${e.message}")
            }
        }
    }

    fun deleteQuiz() {
        viewModelScope.launch {
            apiService.deleteQuizDetails()
        }
    }

    fun getLeaderBoardData(
        quizId: String,
        queryDetails: Map<String, Any>
    ) {
//        leaderBoardRepository.getLeaderBoard(quizId, queryDetails).cachedIn(viewModelScope)


        viewModelScope.launch {
            try {
                Log.e(TAG, "getting leaderboard")
                val response = apiService.getLeaderBoard(quizId, queryDetails)

                if (response.isSuccessful) {
                    response.body()?.let {
                        Log.e(TAG, "leaderboard response: $it")
                        _leaderBoardLiveData.value = it
                    } ?: Log.e(TAG, "leaderboard body is null")
                } else {
                    val error = response.errorBody()?.string()
                    Log.e(TAG, "leaderboard response unsuccessful: $error")
                }
            } catch (e: Exception) {

                Log.e(TAG, "Failed to get leaderboard: ${e.message}")
            }
        }
    }

    fun logout(refreshToken: String) {
        viewModelScope.launch {
            apiService.logout(refreshToken)
        }
    }

    fun getNumber(number: Int) {
        _testMutableLiveData.value = number
    }


    override fun onCleared() {
        viewModelScope.cancel()
        socketManager.disconnect()
    }

    private val socketManager: SocketManager = SocketManager()
    private val messageLiveData = MutableLiveData<String>()

    private var _emojiMutableLiveData = MutableLiveData<Int>()
    val emojiLiveData: LiveData<Int> = _emojiMutableLiveData

    private var _viewerCountMutableLiveData = MutableLiveData<Int>()
    val viewerCountLiveData: LiveData<Int> = _viewerCountMutableLiveData

    private var _viewPoolDataMutableLiveData = MutableLiveData<ShowPoolResponse>()
    val viewPoolDataLiveData: LiveData<ShowPoolResponse> = _viewPoolDataMutableLiveData

    private var _answerResultMutableLiveData = MutableLiveData<AnswerResultResponse>()
    val answerResultLiveData: LiveData<AnswerResultResponse> = _answerResultMutableLiveData


    init {
        socketManager.setConnectionListener(this)

        socketManager.setEventListener(NetworkUtils.SOCKET_HOST_CALCULATION_END,
            object : SocketManager.EventListener {
                override fun onEventReceived(args: Array<Any>) {
                    Log.e(TAG, "mesage from socket: $args")
                    val message = args[0].toString()
                    messageLiveData.postValue(message)
                }
            })

        socketManager.setEventListener(
            NetworkUtils.SOCKET_RECEIVE_EMOJI,
            object : SocketManager.EventListener {
                override fun onEventReceived(args: Array<Any>) {
                    Log.e(TAG, "emoji received: $args")
                    val message = args[0].toString()
                    Log.e(TAG, "emoji received: $message")
                    _emojiMutableLiveData.postValue(message.toInt())
                }
            })

        val mediaPlayer: MediaPlayer = MediaPlayer.create(application, R.raw.coundown_timer_mixdown)
        socketManager.setEventListener(
            NetworkUtils.SOCKET_USER_ANSWER,
            object : SocketManager.EventListener {
                override fun onEventReceived(args: Array<Any>) {
                    // Start sound immediately
                    mediaPlayer.start()

                    // Delay 2 seconds then stop sound
                    Handler(Looper.getMainLooper()).postDelayed({
                        mediaPlayer.pause()
                        mediaPlayer.seekTo(0)
                    }, 2000)
                }
            })

        socketManager.setEventListener(
            NetworkUtils.SOCKET_VIEWER_COUNT,
            object : SocketManager.EventListener {
                override fun onEventReceived(args: Array<Any>) {
                    val message = args[0].toString()
                    Log.e(TAG, "viewer count received from socket: $message")
                    _viewerCountMutableLiveData.postValue(getViewerCount(message))
                }
            })

        socketManager.setEventListener(
            NetworkUtils.SOCKET_AMOUNT_POOL_HOST_USER,
            object : SocketManager.EventListener {
                override fun onEventReceived(args: Array<Any>) {
                    val message = args[0].toString()
                    Log.e(TAG, "pool received: $message")
                    _viewPoolDataMutableLiveData.postValue(getPoolData(message))
                }
            }
        )

        socketManager.setEventListener(
            NetworkUtils.SOCKET_LIVE_QUESTION_RESULT,
            object : SocketManager.EventListener {
                override fun onEventReceived(args: Array<Any>) {
                    val message = args[0].toString()
                    Log.e(TAG, "answer percent received: $message")
                    _answerResultMutableLiveData.postValue(getAnswerResult(message))
                }
            }
        )


//         Connect to the Socket.IO server
        socketManager.connect()
    }

    private fun getViewerCount(message: String): Int {
        return try {
            val obj = JSONObject(message)
            val count = obj.getInt("viewer_count")
            count
        } catch (e: Exception) {
            Log.e(TAG, "failed to parse viewer count: ${e.message}")
            -1
        }
    }

    private fun getPoolData(message: String): ShowPoolResponse? {
        return try {
            val obj = JSONObject(message)
            val amount = obj.getInt("amount")
            val playCount = obj.getInt("playCount")
            val channelViewerCount = obj.getInt("channelViewerCount")

            val response = ShowPoolResponse()
            response.pool = amount
            response.playCount = playCount
            response.viewerCount = channelViewerCount
            response
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse viewer count: ${e.message}")
            null
        }
    }

    private fun getAnswerResult(message: String): AnswerResultResponse? {
        return try {
            val obj = JSONObject(message)
            val position = obj.getInt("position")
            val percent = obj.getDouble("correctAnswerPercent")

            val response = AnswerResultResponse()
            response.position = position
            response.percent = percent
            response
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get answer percent: ${e.message}")
            null
        }
    }

    fun getMessageLiveData(): LiveData<String> {
        return messageLiveData
    }

    fun sendMessage(message: JSONObject, channelName: String) {
        socketManager.sendMessage(message, channelName)
    }

    override fun onConnected() {
        Log.e(TAG, "socket connected")
    }

    override fun onDisconnected() {
        Log.e(TAG, "socket disconnected")
    }

    override fun onConnectError(args: Array<Any>) {
        val error = args[0]
        Log.e(TAG, "socket connect error:$error")
    }
}
