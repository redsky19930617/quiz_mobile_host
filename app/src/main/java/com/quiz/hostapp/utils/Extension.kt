package com.quiz.hostapp.utils

import android.app.Activity
import android.content.Context
import android.graphics.Color
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.quiz.hostapp.R
import java.util.*

fun AppCompatActivity.hideKeyboard() {
    val view = this.currentFocus
    view?.let { v ->
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
        val hideSoftInputFromWindow =
            imm?.hideSoftInputFromWindow(v.windowToken, InputMethodManager.HIDE_IMPLICIT_ONLY)
                ?: false
        if (!hideSoftInputFromWindow) imm?.hideSoftInputFromWindow(
            v.windowToken,
            InputMethodManager.HIDE_NOT_ALWAYS
        )
    }
}

fun Fragment.hideKeyboard() {
    val activity = context as AppCompatActivity
    val imm = activity.getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
    var view = activity.currentFocus
    if (view == null) {
        view = View(activity)
    }
    imm.hideSoftInputFromWindow(view.windowToken, 0)
}

fun getRandomUid(): Int = Random().nextInt(100)
fun getRandomColor(): Int {
    val random = java.util.Random()
    val color = Color.rgb(random.nextInt(96), random.nextInt(96), random.nextInt(96))

    return color
}

fun getEmojiList() = listOf<Int>(
    R.drawable.emo1,
    R.drawable.emo2,
    R.drawable.emo3,
    R.drawable.emo4,
    R.drawable.emo5,
    R.drawable.emo6,
    R.drawable.emo7,
    R.drawable.emo8,
)

fun showMessage(message: String, context: Context) {
    Toast.makeText(context, message, Toast.LENGTH_SHORT).show()

}