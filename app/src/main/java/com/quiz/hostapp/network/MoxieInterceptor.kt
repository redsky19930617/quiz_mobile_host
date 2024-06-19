package com.quiz.hostapp.network

import android.app.Application
import android.content.Context
import android.content.Intent
import android.util.Log
import com.quiz.hostapp.MainActivity
import com.quiz.hostapp.MyApplication
import com.quiz.hostapp.MyApplication.Companion.session
import com.quiz.hostapp.MyApplication.Companion.sessionManager
import com.quiz.hostapp.utils.SessionManager
import com.quiz.hostapp.utils.TokenManager
import okhttp3.Interceptor
import okhttp3.Response

class MoxieInterceptor(private val application: Context) : Interceptor {

    companion object {
        private const val TAG = "MoxieInterceptor"
    }

    override fun intercept(chain: Interceptor.Chain): Response {
        var request = chain.request()
        val token = sessionManager?.getPrefString(SessionManager.ACCESS_TOKEN)
        Log.e(TAG, "request url: ${request.url}, body: ${request.body}")
        Log.e(TAG, "token: $token")
//        val expireTime = getDateFromString(expireDate!!)
//        Log.e(TAG, "current time: $currentTime, expire time: $expireTime")
//        if (sessionManager!!.getPrefBool(SessionManager.IS_LOGIN)) {
        request = request.newBuilder()
            .addHeader(
                "Authorization",
                "Bearer $token"
            )
            .build()
//        }

        Log.e(TAG, "request: ${request.body}, header: ${request.header("Authorization")}")
        val response = chain.proceed(request)
        val responseCode = response.code
        Log.e(TAG, "response code: ${responseCode}, body: ${response.body}")
        if (responseCode in 200..299) {
            if (request.url.toString().contains("/v1/auth/logout")) {
                sessionManager?.deleteSaveData()
                application.startActivity(
                    Intent(application, MainActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    }
                )
            }
            return response
        } else{
//            sessionManager?.deleteSaveData()
            response.close()
            val refreshToken = session(application).getPrefString(SessionManager.REFRESH_TOKEN)?:""
            Log.e(TAG,"refresh token : $refreshToken")
            val tokenManager = TokenManager(application)
            tokenManager.refreshToken(refreshToken,"")
            val accessToken = session(application).getPrefString(SessionManager.ACCESS_TOKEN)?:""
            request = request.newBuilder()
                .addHeader(
                    "Authorization",
                    "Bearer $accessToken"
                )
                .build()
            return chain.proceed(request)
//            application.startActivity(Intent(application, MainActivity::class.java).apply {
//                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
//            })
        }

    }
}
