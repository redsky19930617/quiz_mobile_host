package com.quiz.hostapp.data.quiz

import android.util.Log
import com.quiz.hostapp.network.ApiService
import com.quiz.hostapp.network.model.QuizListResponse
import com.quiz.hostapp.network.model.QuizOverviewResponse
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class QuizRemoteDataSource(
    private val apiService: ApiService,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO
) {

    companion object {
        private const val TAG = "QuizRemoteDataSource"
    }

    suspend fun getUpcomingQuiz(): Result<QuizListResponse> {
        return withContext(dispatcher) {
            try {
                val response = apiService.getUpcomingQuizList(true)
                if (response.isSuccessful) {
                    response.body()?.let {
                        Log.e(TAG, "quiz list response successful: $it")
                        Result.success(it)
                    } ?: let {
                        Log.e(TAG, "response body is null")
                        Result.failure(Throwable("quiz list response is null"))
                    }
                } else {
                    val error = response.errorBody()?.string()
                    Log.e(TAG, "quiz list response is unsuccessful: $error")
                    Result.failure(Throwable(error))
                }
            } catch (e: Exception) {
                Log.e(TAG, "failed to get quiz list: ${e.message}")
                Result.failure(Throwable(e.message))
            }
        }
    }

    suspend fun getQuizOverview(): Result<QuizOverviewResponse> {
        return withContext(dispatcher) {
            try {
                val response = apiService.getQuizOverview(true)
                if (response.isSuccessful) {
                    response.body()?.let {
                        Log.e(TAG, "quiz overview response successful: $it")
                        Result.success(it)
                    } ?: let {
                        Log.e(TAG, "quiz over view response body is null")
                        Result.failure(Throwable("quiz overview response is null"))
                    }
                } else {
                    val error = response.errorBody()?.string()
                    Log.e(TAG, "quiz overview response is unsuccessful: $error")
                    Result.failure(Throwable(error))
                }
            } catch (e: Exception) {
                Log.e(TAG, "failed to get quiz overview: ${e.message}")
                Result.failure(Throwable(e.message))
            }
        }
    }
}