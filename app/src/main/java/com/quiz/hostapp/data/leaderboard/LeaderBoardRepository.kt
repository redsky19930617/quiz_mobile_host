package com.quiz.hostapp.data.leaderboard

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import com.quiz.hostapp.network.ApiService
import com.quiz.hostapp.ui.host.Participant
import kotlinx.coroutines.flow.Flow

class LeaderBoardRepository(private val apiService: ApiService) {

    fun getLeaderBoard(
        quizId: String,
        queryDetails: Map<String, Any>
    ): Flow<PagingData<Participant>> {

        return Pager(
            config = PagingConfig(
                pageSize = 10,
                enablePlaceholders = false
            ),
            pagingSourceFactory = { LeaderBoardPagingSource(apiService, quizId, queryDetails) }
        ).flow
    }
}