package com.quiz.hostapp.ui.host

import android.graphics.Color
import android.os.CountDownTimer
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AlphaAnimation
import android.view.animation.Animation
import android.widget.FrameLayout
import android.widget.TextView
import android.widget.Toast
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.quiz.hostapp.R
import com.quiz.hostapp.network.model.Option

data class QuizDetails(
    val id: String,
    val question: String,
    val answers: List<Option>,
    var showQuestion: Boolean = false,
    var showAnswer: Boolean = false,
    var answerPercent: Double = 0.0
)



class QuizListAdapter(private val clickListener: OnDisplayQuestionClickListener) :
    ListAdapter<QuizDetails, RecyclerView.ViewHolder>(QuizDetailsDiffCallback()) {

    private var onGoing = true
    fun setStreamState(paused: Boolean) {
        onGoing = paused
    }
       fun startPrizePoolIntermission () {
           ((clickListener as WaitingFragment) as StartPrizePoolListener).onStartPrizePollListener()
       }
    interface OnDisplayQuestionClickListener {
        fun onDisplayQuestionClick(quizDetails: QuizDetails, position: Int)

        fun onDisPlayOptionsClick(quizDetails: QuizDetails, position: Int)

        fun onQuestionEnd(position: Int, questionId: String)
    }

    interface StartPrizePoolListener {
        fun onStartPrizePollListener()
    }

    override fun getItemViewType(position: Int): Int {
        return when (position) {
            1 -> 1
            else -> 0
        }
    }



    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        if (viewType == 0) {
            val itemView =
                LayoutInflater.from(parent.context).inflate(R.layout.quiz_list_item, parent, false)
            return QuizViewHolder(itemView)
        } else {
            val itemView =
                LayoutInflater.from(parent.context).inflate(R.layout.quiz_list_item_2, parent, false)
            return SecondQuizViewHolder(itemView)
        }

    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val currentQuizDetails = getItem(position)
        if (position == 1) {
            (holder as SecondQuizViewHolder).bind(currentQuizDetails, position, clickListener)
        } else {
            (holder as QuizViewHolder).bind(currentQuizDetails, position, clickListener)
        }
    }

    inner class QuizViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var showingQuestion = false

        private val questionTextView: TextView = itemView.findViewById(R.id.question_text_view)
        private val questionNoTextView: TextView = itemView.findViewById(R.id.question_no_tv)
        private val answersRecyclerView: RecyclerView =
            itemView.findViewById(R.id.answers_recycler_view)
        private val displayQuestionButton =
            itemView.findViewById<MaterialButton>(R.id.display_question_button)
        private val showingTag = itemView.findViewById<TextView>(R.id.showing_button)
        private val answerTv = itemView.findViewById<TextView>(R.id.answer_text_view)
        private val questionCard = itemView.findViewById<CardView>(R.id.question_card_view)
        private val questionLayout = itemView.findViewById<FrameLayout>(R.id.question_layout)

        fun bind(
            quizDetails: QuizDetails,
            position: Int,
            clickListener: OnDisplayQuestionClickListener
        ) {
            questionTextView.text = "${quizDetails.question}"
            questionNoTextView.text = "Q.${position.plus(1)}"
            questionTextView.setTextColor(if (quizDetails.showAnswer) Color.BLACK else Color.WHITE)
            questionNoTextView.setTextColor(if (quizDetails.showAnswer) Color.BLACK else Color.WHITE)
            questionCard.setCardBackgroundColor(
                if (quizDetails.showAnswer) Color.parseColor(
                    "#C5E3FF"
                ) else Color.parseColor("#9074B8")
            )
            questionLayout.isVisible = quizDetails.showQuestion || quizDetails.showAnswer
            questionLayout.setBackgroundColor(
                if (quizDetails.showQuestion) Color.parseColor(
                    "#5C3890"
                ) else Color.parseColor("#6C90B0")
            )
            displayQuestionButton.isVisible = quizDetails.showQuestion
            displayQuestionButton.text = "Display Question"
            answerTv.isVisible = quizDetails.showAnswer
            answerTv.isVisible = quizDetails.showAnswer
            answerTv.text = itemView.context.getString(R.string.percent_correct_answer, quizDetails.answerPercent)
            val answerAdapter = AnswersAdapter()
            answersRecyclerView.adapter = answerAdapter
            answerAdapter.submitList(quizDetails.answers)

            answersRecyclerView.layoutManager = LinearLayoutManager(itemView.context)
            displayQuestionButton.setOnClickListener {
                if (onGoing.not()) {
                    Toast.makeText(
                        displayQuestionButton.context,
                        "Please Resume Stream",
                        Toast.LENGTH_SHORT
                    ).show()
                    return@setOnClickListener
                }
                if (displayQuestionButton.text == itemView.context.getString(R.string.display_question)) {
                    clickListener.onDisplayQuestionClick(quizDetails, position)
                    Handler(Looper.getMainLooper()).postDelayed({
                        questionTextView.setTextColor(Color.BLACK)
                        questionNoTextView.setTextColor(Color.BLACK)
                        questionCard.setCardBackgroundColor(Color.parseColor("#C5E3FF"))
                        questionLayout.setBackgroundColor(Color.parseColor(("#6C90B0")))
                        showingTag.visibility = View.VISIBLE
                        showingTag.text = "Displaying Question"
                        displayQuestionButton.text = "Show Answers and Start Countdown(15s)"
                        answerAdapter.currentList.forEach {
                            it.isShowingQuestion = true
                        }
                        answerAdapter.notifyDataSetChanged()
                        showingQuestion = false
                    }, 1000)
                    return@setOnClickListener
                }
                if (showingQuestion.not()) {
                    displayQuestionButton.text = "Counting down (15s)"
                    showingQuestion = true
                    questionTextView.setTextColor(Color.BLACK)
                    questionNoTextView.setTextColor(Color.BLACK)
                    questionCard.setCardBackgroundColor(Color.parseColor("#C5E3FF"))
                    questionLayout.setBackgroundColor(Color.parseColor(("#6C90B0")))
                    showingTag.visibility = View.VISIBLE
                    showingTag.text = "Showing Answers"
                    clickListener.onDisPlayOptionsClick(quizDetails, position)
                    answerAdapter.currentList.forEach {
                        it.isShowingOptions = true
                    }
                    answerAdapter.notifyDataSetChanged()
                    startCounter(position, quizDetails.id)
                }
            }
        }

        private fun startCounter(position: Int, questionId: String) {
            val counter = object : CountDownTimer(15000, 1000) {
                override fun onTick(p0: Long) {
                    try {
                        val anim = AlphaAnimation(1.0f, 0.0f)
                        anim.duration = 950
                        anim.repeatCount = 1
                        anim.repeatMode = Animation.REVERSE
                        displayQuestionButton.setBackgroundColor(
                            ContextCompat.getColor(
                                displayQuestionButton.context,
                                R.color.deep_sky_blue
                            )
                        )
                        displayQuestionButton.setTextColor(Color.WHITE)
                        displayQuestionButton.text =
                            "Counting down (${1 + (p0 / 1000)})"
                    } catch (e: Exception) {
                    }
                }

                override fun onFinish() {
                    showingTag.visibility = View.GONE
                    displayQuestionButton.setBackgroundColor(
                        ContextCompat.getColor(
                            displayQuestionButton.context,
                            R.color.yellow
                        )
                    )
                    displayQuestionButton.setTextColor(Color.BLACK)
                    clickListener.onQuestionEnd(position, questionId)
                }
            }
            counter.start()
        }
    }
    inner class SecondQuizViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var showingQuestion = false
        private val questionTextView: TextView = itemView.findViewById(R.id.question_text_view)
        private val questionNoTextView: TextView = itemView.findViewById(R.id.question_no_tv)
        private val answersRecyclerView: RecyclerView =
            itemView.findViewById(R.id.answers_recycler_view)
        private val displayQuestionButton =
            itemView.findViewById<MaterialButton>(R.id.display_question_button)
        private val showPrizePoolButton =
            itemView.findViewById<MaterialButton>(R.id.show_prize_pool_button)
        private val showingTag = itemView.findViewById<TextView>(R.id.showing_button)
        private val answerTv = itemView.findViewById<TextView>(R.id.answer_text_view)
        private val questionCard = itemView.findViewById<CardView>(R.id.question_card_view)
        private val questionLayout = itemView.findViewById<FrameLayout>(R.id.question_layout)

        fun bind(
            quizDetails: QuizDetails,
            position: Int,
            clickListener: OnDisplayQuestionClickListener
        ) {
            questionTextView.text = "${quizDetails.question}"
            questionNoTextView.text = "Q.${position.plus(1)}"
            questionTextView.setTextColor(if (quizDetails.showAnswer) Color.BLACK else Color.WHITE)
            questionNoTextView.setTextColor(if (quizDetails.showAnswer) Color.BLACK else Color.WHITE)
            questionCard.setCardBackgroundColor(
                if (quizDetails.showAnswer) Color.parseColor(
                    "#C5E3FF"
                ) else Color.parseColor("#9074B8")
            )
            questionLayout.isVisible = quizDetails.showQuestion || quizDetails.showAnswer
            questionLayout.setBackgroundColor(
                if (quizDetails.showQuestion) Color.parseColor(
                    "#5C3890"
                ) else Color.parseColor("#6C90B0")
            )
            displayQuestionButton.isVisible = quizDetails.showQuestion
            displayQuestionButton.text = "Display Question"
            answerTv.isVisible = quizDetails.showAnswer
            answerTv.text = itemView.context.getString(R.string.percent_correct_answer, quizDetails.answerPercent)
            val answerAdapter = AnswersAdapter()
            answersRecyclerView.adapter = answerAdapter
            answerAdapter.submitList(quizDetails.answers)

            answersRecyclerView.layoutManager = LinearLayoutManager(itemView.context)
            showPrizePoolButton.setOnClickListener {
                startPrizePoolIntermission()
            }
            displayQuestionButton.setOnClickListener {
                if (onGoing.not()) {
                    Toast.makeText(
                        displayQuestionButton.context,
                        "Please Resume Stream",
                        Toast.LENGTH_SHORT
                    ).show()
                    return@setOnClickListener
                }
                if (displayQuestionButton.text == itemView.context.getString(R.string.display_question)) {
                    clickListener.onDisplayQuestionClick(quizDetails, position)
                    Handler(Looper.getMainLooper()).postDelayed({
                        questionTextView.setTextColor(Color.BLACK)
                        questionNoTextView.setTextColor(Color.BLACK)
                        questionCard.setCardBackgroundColor(Color.parseColor("#C5E3FF"))
                        questionLayout.setBackgroundColor(Color.parseColor(("#6C90B0")))
                        showingTag.visibility = View.VISIBLE
                        showingTag.text = "Displaying Question"
                        displayQuestionButton.text = "Show Answers and Start Countdown(15s)"
                        answerAdapter.currentList.forEach {
                            it.isShowingQuestion = true
                        }
                        answerAdapter.notifyDataSetChanged()
                        showingQuestion = false
                    }, 1000)
                    return@setOnClickListener
                }
                if (showingQuestion.not()) {
                    displayQuestionButton.text = "Counting down (15s)"
                    showingQuestion = true
                    questionTextView.setTextColor(Color.BLACK)
                    questionNoTextView.setTextColor(Color.BLACK)
                    questionCard.setCardBackgroundColor(Color.parseColor("#C5E3FF"))
                    questionLayout.setBackgroundColor(Color.parseColor(("#6C90B0")))
                    showingTag.visibility = View.VISIBLE
                    showingTag.text = "Showing Answers"
                    clickListener.onDisPlayOptionsClick(quizDetails, position)
                    answerAdapter.currentList.forEach {
                        it.isShowingOptions = true
                    }
                    answerAdapter.notifyDataSetChanged()
                    startCounter(position, quizDetails.id)
                }
            }
        }

        private fun startCounter(position: Int, questionId: String) {
            val counter = object : CountDownTimer(15000, 1000) {
                override fun onTick(p0: Long) {
                    try {
                        val anim = AlphaAnimation(1.0f, 0.0f)
                        anim.duration = 950
                        anim.repeatCount = 1
                        anim.repeatMode = Animation.REVERSE
                        displayQuestionButton.setBackgroundColor(
                            ContextCompat.getColor(
                                displayQuestionButton.context,
                                R.color.deep_sky_blue
                            )
                        )
                        displayQuestionButton.setTextColor(Color.WHITE)
                        displayQuestionButton.text =
                            "Counting down (${1 + (p0 / 1000)})"
                    } catch (e: Exception) {
                    }
                }

                override fun onFinish() {
                    showingTag.visibility = View.GONE
                    displayQuestionButton.setBackgroundColor(
                        ContextCompat.getColor(
                            displayQuestionButton.context,
                            R.color.yellow
                        )
                    )
                    displayQuestionButton.setTextColor(Color.BLACK)
                    clickListener.onQuestionEnd(position, questionId)
                }
            }
            counter.start()
        }
    }

    class QuizDetailsDiffCallback : DiffUtil.ItemCallback<QuizDetails>() {
        override fun areItemsTheSame(oldItem: QuizDetails, newItem: QuizDetails): Boolean {
            return oldItem.question == newItem.question
        }

        override fun areContentsTheSame(oldItem: QuizDetails, newItem: QuizDetails): Boolean {
            return oldItem == newItem
        }
    }
}

