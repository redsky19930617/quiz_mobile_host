package com.quiz.hostapp.network

object NetworkUtils {

    const val VOL = "v1"
    const val QUIZ = "quizes"
    const val REGISTER_USER = "$VOL/auth/register"
    const val LOGIN_USER = "$VOL/auth/host/login"
    const val LOGOUT_USER = "$VOL/auth/logout"
    const val REFRESH_TOKEN = "$VOL/auth/refresh-tokens"

    const val GET_RTC_TOKEN = "$VOL/agora/rtc"
    const val QUESTIONS_LIST = "$VOL/$QUIZ"
    const val QUIZ_LIST = "$VOL/quizes"
    const val QUIZ_OVERVIEW = "$VOL/quizes/overview"
    const val LEADERBOARD = "leaderboard"


    //Socket
    const val SOCKET_LIVE_QUESTION_RESULT = "user_quiz_live_question_result"
    const val SOCKET_AMOUNT_POOL_HOST_USER = "increase_amount_pool_host_user"
    const val SOCKET_HOST_LIVE_START = "host_live_start"
    const val SOCKET_HOST_LIVE_END = "host_live_end"
    const val SOCKET_DISPLAY_QUESTION = "host_live_quiz_question_start"
    const val SOCKET_DISPLAY_OPTION = "host_live_quiz_question_options"
    const val SOCKET_END_QUESTION = "host_live_quiz_question_end"
    const val SOCKET_LIVE_STREAM_STATE_CHANGE = "host_live_change"
    const val SOCKET_CALCULATION_START = "host_live_quiz_calculation_start"
    const val SOCKET_HOST_CALCULATION_END = "host_quiz_live_calculation_end"
    const val SOCKET_RECEIVE_EMOJI = "host_emoji_received"
    const val SOCKET_VIEWER_COUNT = "user_quiz_live_viewer_count"
    const val SOCKET_SHOW_POOL = "host_show_pool"
    const val SOCKET_MUTE_STATE = "host_mute_state"
    const val SOCKET_USER_ANSWER = "user_submit_live_quiz_answer"
}