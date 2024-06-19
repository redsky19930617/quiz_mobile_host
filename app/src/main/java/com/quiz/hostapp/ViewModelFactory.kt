package com.quiz.hostapp

import android.app.Application
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.quiz.hostapp.network.ApiService
import com.quiz.hostapp.ui.auth.LoginViewModel
import com.quiz.hostapp.ui.home.HomeViewModel
import com.quiz.hostapp.ui.host.HostViewModel

class ViewModelFactory(private val context: Context, private val apiService: ApiService) :
    ViewModelProvider.NewInstanceFactory() {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return with(modelClass) {
            when {
                isAssignableFrom(LoginViewModel::class.java) -> {
                    LoginViewModel(ServiceLocator.provideAuthRepository(context, apiService))
                }

                isAssignableFrom(HostViewModel::class.java) -> {
                    HostViewModel(
                        context.applicationContext as Application,
                        apiService,
                        ServiceLocator.provideLeaderBoardRepository(apiService)
                    )
                }

                isAssignableFrom(HomeViewModel::class.java) -> {
                    HomeViewModel(ServiceLocator.provideQuizRepository(apiService))
                }

                else -> throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
            }
        } as T
    }
}