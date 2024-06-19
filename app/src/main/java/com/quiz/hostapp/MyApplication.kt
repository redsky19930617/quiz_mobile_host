package com.quiz.hostapp

import android.app.Application
import android.content.Context
import com.quiz.hostapp.utils.SessionManager

class MyApplication :Application(){
    companion object {
        var sessionManager: SessionManager? = null

        fun session(context: Context): SessionManager {
            if (sessionManager == null)
                sessionManager = SessionManager(context)
            return sessionManager!!
        }
        private lateinit var context: Context

        fun getAppContext() = context
    }



    override fun onCreate() {
        super.onCreate()
        sessionManager = SessionManager(this)
        context = applicationContext
    }
}