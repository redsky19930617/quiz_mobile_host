package com.quiz.hostapp
import android.content.Context
import androidx.lifecycle.ViewModelProvider
import com.quiz.hostapp.data.auth.AuthRemoteDataSource
import com.quiz.hostapp.data.auth.AuthRepository
import com.quiz.hostapp.data.leaderboard.LeaderBoardPagingSource
import com.quiz.hostapp.data.leaderboard.LeaderBoardRepository
import com.quiz.hostapp.data.quiz.QuizRemoteDataSource
import com.quiz.hostapp.data.quiz.QuizRepository
import com.quiz.hostapp.network.ApiClient
import com.quiz.hostapp.network.ApiService

object ServiceLocator {

    var authRepository: AuthRepository? = null
    var quizRepository: QuizRepository? = null
    var leaderBoardRepository: LeaderBoardRepository? = null


    fun provideViewModelFactory(
        context: Context,
        apiService: ApiService
    ): ViewModelProvider.NewInstanceFactory {
        return ViewModelFactory(context, apiService)
    }

    fun provideApiService(context: Context): ApiService {
        return ApiClient.getApiClient(context).create(ApiService::class.java)
    }

    fun provideAuthRepository(context: Context, apiService: ApiService): AuthRepository {

        synchronized(this) {
            return authRepository ?: createAuthRepository(context, apiService)
        }
    }

    fun provideQuizRepository(apiService: ApiService): QuizRepository {
        synchronized(this) {
            return quizRepository ?: createQuizRepository(apiService)
        }
    }

    fun provideLeaderBoardRepository(apiService: ApiService): LeaderBoardRepository {
        synchronized(this) {
            return leaderBoardRepository ?: createLeaderBoardRepository(apiService)
        }
    }

    private fun createAuthRepository(context: Context, apiService: ApiService): AuthRepository {
        return AuthRepository(
            AuthRemoteDataSource(apiService)
        )
    }

    private fun createQuizRepository(apiService: ApiService): QuizRepository {
        return QuizRepository(
            QuizRemoteDataSource(apiService)
        )
    }

    private fun createLeaderBoardRepository(apiService: ApiService): LeaderBoardRepository {
        leaderBoardRepository = LeaderBoardRepository(
            apiService
        )
        return leaderBoardRepository!!
    }


}