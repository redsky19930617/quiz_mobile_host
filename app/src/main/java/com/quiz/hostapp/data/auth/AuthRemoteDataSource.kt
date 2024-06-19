package com.quiz.hostapp.data.auth

import android.util.Log
import com.quiz.hostapp.network.ApiService
import com.quiz.hostapp.network.model.LoginResponse
import com.quiz.hostapp.network.model.RegisterResponse
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AuthRemoteDataSource(
    private val apiService: ApiService,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO
) {

    companion object {
        private const val TAG = "AuthRemoteDataSource"
    }

    suspend fun registerUser(requestBody: Map<String, String>): Result<RegisterResponse> {

        return withContext(dispatcher) {
            try {
                val response = apiService.registerUser(requestBody)
                if (response.isSuccessful) {
                    response.body()?.let {
                        Log.e(TAG, "register successful: $it")
                        Result.success(it)
                    } ?: let {
                        Log.e(TAG, "response body is null")
                        Result.failure(Throwable("Register response is null"))
                    }
                } else {
                    val error = response.errorBody()?.string()
                    Log.e(TAG, "Register response is unsuccessful: $error")
                    Result.failure(Throwable(error))
                }
            } catch (throwable: Throwable) {
                throwable.printStackTrace()
                Log.e(TAG, "failed to register user: ${throwable.message}")
                Result.failure(throwable)
            }
        }
    }

    suspend fun loginUser(requestBody: Map<String, String>): Result<LoginResponse> {

        return withContext(dispatcher) {
            try {
                val response = apiService.loginUser(requestBody)
                if (response.isSuccessful) {
                    response.body()?.let {
                        Log.e(TAG, "login successful: $it")
                        Result.success(it)
                    } ?: let {
                        Log.e(TAG, "response body is null")
                        Result.failure(Throwable("login response is null"))
                    }
                } else {
                    val error = response.errorBody()?.string()
                    Log.e(TAG, "login response is unsuccessful: $error")
                    Result.failure(Throwable(error))
                }
            } catch (throwable: Throwable) {
                throwable.printStackTrace()
                Log.e(TAG, "failed to login user: ${throwable.message}")
                Result.failure(throwable)
            }
        }
    }
}