package com.quiz.hostapp.ui.home

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.quiz.hostapp.data.quiz.QuizRepository
import com.quiz.hostapp.network.model.FreeQuiz
import com.quiz.hostapp.network.model.QuizResult
import kotlinx.coroutines.launch

class HomeViewModel(private val quizRepository: QuizRepository) : ViewModel() {

    private var _upComingQuizMutableLiveData = MutableLiveData<Result<List<QuizResult>?>>()
    val upcomingQuizLiveData: LiveData<Result<List<QuizResult>?>> = _upComingQuizMutableLiveData

    private var _quizOverViewMutableLiveData = MutableLiveData<Result<List<FreeQuiz>?>>()
    val quizOverviewLiveData: LiveData<Result<List<FreeQuiz>?>> = _quizOverViewMutableLiveData

    fun getUpComingQuizzes() {
        viewModelScope.launch {
            val quizListResponse = quizRepository.getUpComingQuizzes()
            if (quizListResponse.isSuccess) {
                _upComingQuizMutableLiveData.value =
                    Result.success(quizListResponse.getOrNull()?.data?.results)
            } else {
                _upComingQuizMutableLiveData.value =
                    Result.failure(Throwable(quizListResponse.exceptionOrNull()))
            }
            getQuizOverview()
        }
    }

    private fun getQuizOverview() {
        viewModelScope.launch {
            val response = quizRepository.getQuizOverview()
            _quizOverViewMutableLiveData.value = if (response.isSuccess) {
                Result.success(response.getOrNull()?.data?.free)
            } else {
                Result.failure(Throwable(response.exceptionOrNull()))
            }
        }
    }
}