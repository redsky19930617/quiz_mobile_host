package com.quiz.hostapp.network.model
import com.google.gson.annotations.SerializedName
import com.quiz.hostapp.ui.host.Participant


data class LeaderBoardResponse(
    @SerializedName("status")
    val status: String,
    @SerializedName("message")
    val message: String,
    @SerializedName("data")
    val `data`: List<LeaderBoardResult>
)

data class LeaderBoardResult(
    @SerializedName("_id")
    val id: String,
    @SerializedName("quiz")
    val quiz: String,
    @SerializedName("user")
    val user: String,
    @SerializedName("username")
    val userName: String,
    @SerializedName("totalquestion")
    val totalQuestions: Int,
    @SerializedName("pool")
    val pool: Int,
    @SerializedName("role")
    val role: String,
    @SerializedName("allQuestionCorrect")
    val allQuestionCorrect: Boolean,
    @SerializedName("correct")
    val correct: Int,
    @SerializedName("time")
    val time: Double,
    @SerializedName("rank")
    val rank: Int,
    @SerializedName("rewardAmount")
    val rewardAmount: Double,
//    @SerializedName("rewardCredit")
//    val rewardCredit: Int,
    @SerializedName("avatar")
    val avatar: String
)
data class LeaderBoardData(
    @SerializedName("leaderboard")
    val `data`:LeaderBoard,
    @SerializedName("total_questions")
    val total_questions:Int
)

data class LeaderBoard(
    @SerializedName("results")
    val `data`:List<LeaderBoardResult>,
    @SerializedName("page")
    val page: Int,
    @SerializedName("total_pages")
    val total_pages: Int
)

//data class LeaderBoardResult(
//    @SerializedName("quiz")
//    val quiz: String,
//    @SerializedName("user")
//    val user: User,
//    @SerializedName("rank")
//    val rank: Int,
//    @SerializedName("correct_answers")
//    val correctAnswers: Int,
//    @SerializedName("total_duration")
//    val totalDuration: Double,
//    @SerializedName("_id")
//    val id: String
//)

data class User(
    @SerializedName("name")
    val name: String,
    @SerializedName("id")
    val id: String
)

fun List<LeaderBoardResult>.asParticipantList():List<Participant>{
    return map {
        Participant(
            rank = it.rank,
            profile_pic = "",
            name = it.userName,
            timeTaken = it.time,
            total_question = 10,
            correct_answer = it.correct,
            isParticipant = false,
            userId = it.user,
            avatar = it.avatar
        )
    }
}