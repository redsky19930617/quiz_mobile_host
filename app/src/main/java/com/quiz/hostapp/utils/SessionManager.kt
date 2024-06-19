package com.quiz.hostapp.utils

import android.content.Context
import android.content.SharedPreferences
import com.quiz.hostapp.R

class SessionManager(context: Context) {

    companion object {
        const val IS_LOGIN = "is_login"
        const val IS_GOOGLE_LOG_IN = "is_google_login"
        const val IS_FACEBOOK_LOGIN = "is_fb_login"
        const val USER_TYPE = "user_type"
        const val ACCESS_TOKEN = "access_token"
        const val REFRESH_TOKEN = "refresh_token"
        const val ACCESS_TOKEN_TIME = "access_token_time"
        const val USER_ID = "user_id"
    }

    private val pref: SharedPreferences =
        context.getSharedPreferences(context.getString(R.string.app_name), Context.MODE_PRIVATE)

    fun removeKeys(vararg keys: String) {
        for (i in keys) {
            pref.edit().remove(i).apply()
        }
    }

    fun deleteSaveData() {
        pref.edit().clear().apply()
    }

    fun savePrefBool(key: String, value: Boolean) {
        pref.edit().putBoolean(key, value).apply()
    }

    fun savePrefString(key: String,value: String){
        pref.edit().putString(key,value).apply()
    }

    fun getPrefString(key: String) = pref.getString(key,"")

    fun getPrefBool(key: String) = pref.getBoolean(key, false)
}