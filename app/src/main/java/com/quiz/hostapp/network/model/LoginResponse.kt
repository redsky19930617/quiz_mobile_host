package com.quiz.hostapp.network.model

import com.google.gson.annotations.SerializedName


data class LoginResponse(
    @SerializedName("user")
    val user: LoginUser,
    @SerializedName("tokens")
    val tokens: Tokens,
    @SerializedName("coin")
    val coinData: CoinData
)

data class LoginUser(
    @SerializedName("role")
    val role: String,
    @SerializedName("agora")
    val agoraData: AgoraData? = null,
    @SerializedName("isEmailVerified")
    val isEmailVerified: Boolean,
    @SerializedName("name")
    val name: String,
    @SerializedName("email")
    val email: String,
    @SerializedName("id")
    val id: String
)

data class CoinData(
    @SerializedName("coin")
    val coin: Int,
    @SerializedName("super_coin")
    val superCoin: Int
)

