package com.quiz.hostapp.network.model

import com.google.gson.annotations.SerializedName


data class RegisterResponse(
    @SerializedName("user")
    val user: RegisterUser,
    @SerializedName("tokens")
    val tokens: Tokens
)

data class RegisterUser(
    @SerializedName("role")
    val role: String,
    @SerializedName("agora")
    val agoraData: AgoraData,
    @SerializedName("isEmailVerified")
    val isEmailVerified: Boolean,
    @SerializedName("name")
    val name: String,
    @SerializedName("email")
    val email: String,
    @SerializedName("id")
    val id: String
)

data class Tokens(
    @SerializedName("access")
    val access: Access,
    @SerializedName("refresh")
    val refresh: Refresh
)

data class Access(
    @SerializedName("token")
    val token: String,
    @SerializedName("expires")
    val expires: String
)

data class Refresh(
    @SerializedName("token")
    val token: String,
    @SerializedName("expires")
    val expires: String
)

data class AgoraData(
    @SerializedName("uuid")
    val uuid: String,
    @SerializedName("username")
    val username: String
)