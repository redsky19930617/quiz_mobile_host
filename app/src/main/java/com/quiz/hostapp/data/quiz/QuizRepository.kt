package com.quiz.hostapp.data.quiz

import com.quiz.hostapp.network.model.QuizListResponse
import com.quiz.hostapp.network.model.QuizOverviewResponse

class QuizRepository(
    private val quizRemoteDataSource: QuizRemoteDataSource
) {
    suspend fun getUpComingQuizzes(): Result<QuizListResponse> {
        return quizRemoteDataSource.getUpcomingQuiz()
    }

    suspend fun getQuizOverview(): Result<QuizOverviewResponse> {
        return quizRemoteDataSource.getQuizOverview()
    }
}