package com.quiz.hostapp.network.model

import com.google.gson.annotations.SerializedName

data class ShowPoolResponse (
    @SerializedName("status")
    var status: String? = null,

    @SerializedName("pool")
    var pool: Int? = null,

    @SerializedName("playCount")
    var playCount: Int? = null,

    @SerializedName("viewer_count")
    var viewerCount: Int? = null
)
