package com.quiz.hostapp.network.model


import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize

data class QuizListResponse(
    @SerializedName("status")
    val status: Int,
    @SerializedName("message")
    val message: String,
    @SerializedName("data")
    val `data`: QuizListData
)

data class QuizListData(
    @SerializedName("results")
    val results: List<QuizResult>
)

@Parcelize
data class QuizResult(
    @SerializedName("host")
    val host: HostDetailsModel,
    @SerializedName("voting_category")
    val voting_category: List<VotingCategoryItem>?,
    @SerializedName("status")
    val status: String,
    @SerializedName("category")
    val category: String? = "",
    @SerializedName("start_date")
    val startDate: String,
    @SerializedName("is_paid")
    val isPaid: Boolean,
    @SerializedName("description")
    val description: String,
    @SerializedName("is_live")
    val is_live: Boolean,
    @SerializedName("image")
    val image: String,
    @SerializedName("_id")
    val id: String,
    @SerializedName("total_votes")
    val totalVotes:Int,
    @SerializedName("has_voted")
    val has_voted:Boolean
):Parcelable

@Parcelize
data class HostDetailsModel(
    @SerializedName("_id")
    var _id:String,
    @SerializedName("name")
    var name:String
):Parcelable

@Parcelize
data class VotingCategoryItem(
    @SerializedName("name")
    val category: String?,
    @SerializedName("total_votes")
    var votes: Int?,
    @SerializedName("_id")
    val id: String?,
):Parcelable