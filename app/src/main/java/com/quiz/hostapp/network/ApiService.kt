package com.quiz.hostapp.network

import android.content.Context
import com.quiz.hostapp.network.model.*
import retrofit2.Response
import retrofit2.http.*

fun apiService(context: Context): ApiService = ApiClient.getApiClient(context).create(ApiService::class.java)

interface ApiService {

    @POST(NetworkUtils.REGISTER_USER)
    @FormUrlEncoded
    suspend fun registerUser(@FieldMap requestBody: Map<String, String>): Response<RegisterResponse>

    @POST(NetworkUtils.LOGIN_USER)
    suspend fun loginUser(@Body requestBody: Map<String, String>): Response<LoginResponse>


    @POST(NetworkUtils.REFRESH_TOKEN)
    @FormUrlEncoded
    fun refreshToken(@Field("refresh_token")refreshToken:String):Response<RefreshTokenResponse>

    @POST(NetworkUtils.LOGOUT_USER)
    @FormUrlEncoded
    suspend fun logout(@Field("refresh_token") refresh_token: String): Response<Unit>

    @GET("${NetworkUtils.QUESTIONS_LIST}/{quiz_id}/questions")
    suspend fun getQuestionsList(@Path("quiz_id") quizId: String): Response<QuestionResponse>

    @GET("${NetworkUtils.GET_RTC_TOKEN}/{channel}/{role}/{token_type}/{uid}")
    suspend fun getRtcToken(
        @Path("channel") channel: String,
        @Path("role") role: String,
        @Path("token_type") tokenType: String,
        @Path("uid") uid: Int,
    ): Response<RtcTokenResponse>

    @GET(NetworkUtils.QUIZ_LIST)
    suspend fun getUpcomingQuizList(@Query("upcoming") isFree: Boolean): Response<QuizListResponse>

    @GET(NetworkUtils.QUIZ_OVERVIEW)
    suspend fun getQuizOverview(@Query("free") isFree: Boolean): Response<QuizOverviewResponse>

    @GET("${NetworkUtils.VOL}/${NetworkUtils.QUIZ}/{quiz_id}/${NetworkUtils.LEADERBOARD}")
    suspend fun getLeaderBoard(
        @Path("quiz_id") quizId: String,
        @QueryMap requestBody: @JvmSuppressWildcards Map<String, Any>?=null
    ): Response<LeaderBoardResponse>

    @DELETE("${NetworkUtils.VOL}/${NetworkUtils.QUIZ}/temp")
    suspend fun deleteQuizDetails()

}