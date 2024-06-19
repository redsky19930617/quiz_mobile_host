package com.quiz.hostapp.data.leaderboard

import android.util.Log
import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.quiz.hostapp.network.ApiService
import com.quiz.hostapp.network.model.asParticipantList
import com.quiz.hostapp.ui.host.Participant
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext

class LeaderBoardPagingSource(
    private val apiService: ApiService,
    private val quizId: String,
    private val queryDetails: Map<String, Any>
) : PagingSource<Int, Participant>() {

    companion object {
        private const val TAG = "LeaderBoardPagingSource"
    }

    override fun getRefreshKey(state: PagingState<Int, Participant>): Int? {
        return state.anchorPosition?.let { anchorPosition ->
            state.closestPageToPosition(anchorPosition)?.prevKey?.plus(1)
                ?: state.closestPageToPosition(anchorPosition)?.nextKey?.minus(1)
        }
    }

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, Participant> {
        val nextPage = params.key ?: 1
        return try {
            Log.e(TAG, "getting leaderboard")
            var participantList = listOf<Participant>()
            val response = apiService.getLeaderBoard(quizId, queryDetails)
            if (response.isSuccessful) {
                response.body()?.let {
                    Log.e(TAG, "leaderboard response: ${it.status}")
                    participantList = it.data.asParticipantList()
                } ?: let {
                    Log.e(TAG, "leaderboard body is null")
                }
            } else {
                val error = response.errorBody()?.string()
                Log.e(TAG, "leaderboard response unsuccessful: $error")
            }
            Log.e(TAG, "number of participants: ${participantList.size}")
            val nextKey = if (participantList.isEmpty()) {
                null
            } else {
                // initial load size = 3 * NETWORK_PAGE_SIZE
                // ensure we're not requesting duplicating items, at the 2nd request
                nextPage + (params.loadSize / 10)
            }
            LoadResult.Page(
                participantList,
                if (nextPage == 1) null else nextPage - 1,
                nextKey
            )
        } catch (e: Exception) {
            Log.e(TAG, "failed to send: ${e.message}")
            LoadResult.Error(Throwable("Failed to get leaderboard: ${e.message}"))
        }

    }

    private suspend fun getLeaderBoardData(
        quizId: String,
        queryDetails: Map<String, Any>
    ): List<Participant> {
        var leaderBoardList = listOf<Participant>()
        val currentScope = CoroutineScope(Dispatchers.IO + Job())
        val leaderBoardDeferred = currentScope.async {
            try {
                Log.e(TAG, "getting leaderboard")
                val response = apiService.getLeaderBoard(quizId, queryDetails)
                if (response.isSuccessful) {
                    response.body()?.let {
                        Log.e(TAG, "leaderboard response: ${it.status}")
                        leaderBoardList = it.data.asParticipantList()
                    } ?: let {
                        Log.e(TAG, "leaderboard body is null")
                    }
                } else {
                    val error = response.errorBody()?.string()
                    Log.e(TAG, "leaderboard response unsuccessful: $error")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to get leaderboard: ${e.message}")
            }
            leaderBoardList.toList()

        }
        return leaderBoardDeferred.await()
    }
}