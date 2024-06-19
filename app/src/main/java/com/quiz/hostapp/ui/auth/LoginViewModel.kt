package com.quiz.hostapp.ui.auth

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.quiz.hostapp.data.auth.AuthRepository
import com.quiz.hostapp.network.model.LoginResponse
import kotlinx.coroutines.launch

class LoginViewModel(private val authRepository: AuthRepository) : ViewModel() {
    companion object{
        private const val TAG = "LoginViewModel"
    }

    private var _loginMutableLiveData = MutableLiveData<Result<LoginResponse>>()
    val loginLiveData: LiveData<Result<LoginResponse>> = _loginMutableLiveData

    fun loginUser(requestBody:Map<String,String>){
        viewModelScope.launch {
            _loginMutableLiveData.value = authRepository.loginUser(requestBody)
        }
    }
}