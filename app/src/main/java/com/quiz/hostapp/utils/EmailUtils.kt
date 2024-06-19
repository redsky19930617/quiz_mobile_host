package com.quiz.hostapp.utils

import android.text.TextUtils
import android.util.Patterns


fun isValidEmail(target: String): Boolean {
    return if (TextUtils.isEmpty(target)) {
        false
    } else {
        Patterns.EMAIL_ADDRESS.matcher(target).matches()
    }
}