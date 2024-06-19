package com.quiz.hostapp.ui

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.quiz.hostapp.MyApplication.Companion.session
import com.quiz.hostapp.R
import com.quiz.hostapp.utils.SessionManager

class SplashFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_splash, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val isLogin = session(requireActivity()).getPrefBool(SessionManager.IS_LOGIN)
        Handler(Looper.getMainLooper()).postDelayed({
            findNavController().navigate(if (isLogin) SplashFragmentDirections.actionSplashFragmentToHomeFragment() else SplashFragmentDirections.actionSplashFragmentToLoginFragment())
        }, 2000)
    }
}