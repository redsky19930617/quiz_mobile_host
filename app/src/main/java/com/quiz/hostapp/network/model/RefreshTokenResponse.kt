package com.quiz.hostapp.network.model
import com.google.gson.annotations.SerializedName

data class RefreshTokenResponse(
    @SerializedName("access")
    var access: Access,
    @SerializedName("refresh")
    var refresh: Refresh
)