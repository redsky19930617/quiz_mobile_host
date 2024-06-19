package com.quiz.hostapp.utils

import com.quiz.hostapp.ui.host.Participant

object ConstUtils {
    const val CUSTOMER_TYPE = "customer_type"
    const val EMAIL = "email"
    const val PHONE_NUMBER = "phone_number"
    const val PASSWORD = "password"
    const val TYPE_HOST = "publisher"
    const val TYPE_VIEWER = "audience"
    const val RTC_TOKEN = "rtc_token"
    const val UID = "uid"
    const val QUIZ_RESULT = "quiz_result"

    // Create a list of Participant objects as dummy data
    val participantsList = listOf(
        Participant(1002, "profile_pic_1", "John", 10.5, 20, 15),
        Participant(2012, "profile_pic_2", "Jane", 15.2, 20, 16),
        Participant(1200, "profile_pic_3", "Mike", 9.8, 20, 17,true),
        Participant(1900, "profile_pic_4", "Emily", 11.0, 20, 14)
    )

}