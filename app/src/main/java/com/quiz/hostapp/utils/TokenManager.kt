package com.quiz.hostapp.utils

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.quiz.hostapp.MyApplication.Companion.session
import com.quiz.hostapp.network.ApiClient
import com.quiz.hostapp.network.ApiService
import com.quiz.hostapp.network.model.RefreshTokenResponse
import kotlinx.coroutines.withContext
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response

class TokenManager(private val context: Context) {

    companion object{
        private const val TAG = "TokenManager"
    }
    fun refreshToken(refreshToken:String, accessToken:String){
        val client = OkHttpClient()
        val mediaType = "application/x-www-form-urlencoded".toMediaType()
        val body = ("refresh_token=$refreshToken").toRequestBody(mediaType)
        val request = Request.Builder()
            .url("https://api.quizmobb.com/v1/auth/refresh-tokens")
            .post(body)
            .addHeader("Content-Type", "application/x-www-form-urlencoded")
            .addHeader("Authorization", "Bearer " +
                    "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiI2NDI5NDlhMmQ0Nzc5NjAwMDhiYTVjMTYiLCJpYXQiOjE2ODc0MTI2NzMsImV4cCI6MTY4NzQzMDY3MywidHlwZSI6ImFjY2VzcyJ9.wx9KfM62mYyY7s735N-lHWHF47FciIc7yFNVk4X33eM")
            .build()
        val response = client.newCall(request).execute()
        Log.e(TAG,"refresh token response: ${response.body?.string()}")
        val gson = Gson()
        val refreshTokenResponse = gson.fromJson(response.body?.string(),RefreshTokenResponse::class.java)
        session(context).savePrefString(SessionManager.REFRESH_TOKEN,refreshTokenResponse.refresh.token)
        session(context).savePrefString(SessionManager.ACCESS_TOKEN,refreshTokenResponse.access.token)

    }
}