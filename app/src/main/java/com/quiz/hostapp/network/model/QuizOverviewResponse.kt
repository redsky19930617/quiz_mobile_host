package com.quiz.hostapp.network.model

import com.google.gson.annotations.SerializedName


data class QuizOverviewResponse(
    @SerializedName("status")
    val status: Int,
    @SerializedName("message")
    val message: String,
    @SerializedName("data")
    val `data`: QuizOverviewData
)

data class QuizOverviewData(
    @SerializedName("free")
    val free: List<FreeQuiz>
)

data class FreeQuiz(
    @SerializedName("count")
    val count: Int,
    @SerializedName("completed")
    val completed: Int,
    @SerializedName("category")
    val category: String
)