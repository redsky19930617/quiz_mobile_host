package com.quiz.hostapp.network.model
import com.google.gson.annotations.SerializedName
import com.quiz.hostapp.ui.host.QuizDetails


data class QuestionResponse(
    @SerializedName("status")
    val status: Int,
    @SerializedName("message")
    val message: String,
    @SerializedName("data")
    val `data`: QuestionData
)

data class QuestionData(
    @SerializedName("_id")
    val id: String,
    @SerializedName("start_date")
    val startDate: String,
    @SerializedName("questions")
    val questions: List<Question>,
    @SerializedName("category")
    val category: String,
    @SerializedName("room_id")
    val roomId: Int
)

data class Question(
    @SerializedName("_id")
    val id: String,
    @SerializedName("text")
    val question: String,
    @SerializedName("options")
    val options: List<Option>
)

data class Option(
    @SerializedName("_id")
    val id: String,
    @SerializedName("text")
    val text: String,
    @SerializedName("is_correct")
    val isCorrect: Boolean,
    var isShowingQuestion:Boolean = false,
    var isShowingOptions:Boolean = false
)

fun List<Question>.asQuizDetailsList():List<QuizDetails>{
    return map {
        QuizDetails(
            it.id,
            it.question,
            it.options
        )
    }
}