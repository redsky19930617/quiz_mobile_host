package com.quiz.hostapp.utils

import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*

fun String.getFormatMillis(from: String = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'"): Long {
    val sdf = SimpleDateFormat(from, Locale.ENGLISH);
    val cal = Calendar.getInstance(Locale.getDefault())
    cal.time = sdf.parse(this)!!
    try {
        return cal.timeInMillis
    } catch (e1: ParseException) {
        e1.printStackTrace();
    }
    return Calendar.getInstance(Locale.getDefault()).timeInMillis
}

fun getTimeFromMillis(milliSeconds: Long, format: String = "h:mm a"): String {
    val formatter = SimpleDateFormat(format, Locale.getDefault())

    // Create a calendar object that will convert the date and time value in milliseconds to date.
    val calendar = Calendar.getInstance()
    calendar.timeInMillis = milliSeconds
    return formatter.format(calendar.time)
}

fun getDateFromString(
    input: String,
    format: String? = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
): Date {
    val formatter = SimpleDateFormat(format, Locale.getDefault())
    return formatter.parse(input)!!
}