package com.quiz.hostapp.network.model

import com.google.gson.annotations.SerializedName

data class AnswerResultResponse (
    @SerializedName("position")
    var position: Int? = null,

    @SerializedName("correctAnswerPercent")
    var percent: Double? = 0.0,
)