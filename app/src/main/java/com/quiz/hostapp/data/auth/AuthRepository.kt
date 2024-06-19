package com.quiz.hostapp.data.auth

import com.quiz.hostapp.network.model.LoginResponse
import com.quiz.hostapp.network.model.RegisterResponse

class AuthRepository(
    private val authRemoteDataSource: AuthRemoteDataSource
) {

    suspend fun registerUser(requestBody: Map<String, String>): Result<RegisterResponse> {
        return authRemoteDataSource.registerUser(requestBody)
    }

    suspend fun loginUser(requestBody: Map<String, String>): Result<LoginResponse> {
        return authRemoteDataSource.loginUser(requestBody)
    }
}