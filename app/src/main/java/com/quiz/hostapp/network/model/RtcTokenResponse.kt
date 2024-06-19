package com.quiz.hostapp.network.model

import com.google.gson.annotations.SerializedName

data class RtcTokenResponse(
    @SerializedName("status")
    val status: Int,
    @SerializedName("message")
    val message: String,
    @SerializedName("data")
    val `data`: String
)