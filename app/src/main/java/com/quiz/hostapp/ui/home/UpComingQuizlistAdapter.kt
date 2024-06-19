package com.quiz.hostapp.ui.home

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView
import com.quiz.hostapp.R
import com.quiz.hostapp.network.model.QuizResult
import com.quiz.hostapp.utils.getFormatMillis
import com.quiz.hostapp.utils.getTimeFromMillis
import java.util.concurrent.TimeUnit

class UpComingQuizlistAdapter(
    private val listener: OnQuizItemClickListener
) : RecyclerView.Adapter<UpComingQuizlistAdapter.QuizDetailsViewHolder>() {

    private var quizDetailsList: List<QuizResult> = listOf()

    companion object {
        private const val TAG = "UpcomingQuizlistAdapter"
    }

    interface OnQuizItemClickListener {
        fun onQuizItemClick(quizDetails: QuizResult, isVoting: Boolean)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): QuizDetailsViewHolder {
        val itemView = LayoutInflater.from(parent.context)
            .inflate(R.layout.upcoming_quiz_list_item, parent, false)
        return QuizDetailsViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: QuizDetailsViewHolder, position: Int) {
        val quizDetails = quizDetailsList[position]
        holder.bind(quizDetails, listener)
    }

    override fun getItemCount(): Int {
        Log.e(TAG, "no of item in item count: ${quizDetailsList.size}")
        return quizDetailsList.size
    }

    fun submitList(quizList: List<QuizResult>) {
        quizDetailsList = quizList
        notifyDataSetChanged()
    }

    inner class QuizDetailsViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val quizImageView: ImageView = itemView.findViewById(R.id.thumbnailImageView)
        private val quizTimeTextView: TextView = itemView.findViewById(R.id.quizDateTv)
        private val quizCard: MaterialCardView = itemView.findViewById(R.id.upcoming_quiz_item_card)
        private val timeLeftTv: TextView = itemView.findViewById(R.id.timeLeftTv)
        private val castVoteButton: ImageView = itemView.findViewById(R.id.castVoteButton)
        private val categoryText: TextView = itemView.findViewById(R.id.category_quiz)

        fun bind(quizDetails: QuizResult, listener: OnQuizItemClickListener) {
            // Load image into the ImageView using a library like Picasso or Glide
            // quizImageView.load(quizDetails.imageLink)

//            Log.e(TAG,"voting category: ${quizDetails.voting_category}")
//            castVoteButton.isVisible = quizDetails.status.lowercase() == "voting"
            quizCard.setOnClickListener {
                listener.onQuizItemClick(quizDetails, quizDetails.status.lowercase() == "voting")
            }
            val quizdateinMili = quizDetails.startDate.getFormatMillis()
            val timeRemaining = quizdateinMili.minus(System.currentTimeMillis())
            Log.e(TAG, "time remaining: $timeRemaining")
            val duration = String.format(
                "%02d min, %02d sec",
                TimeUnit.MILLISECONDS.toMinutes(timeRemaining),
                TimeUnit.MILLISECONDS.toSeconds(timeRemaining) -
                        TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(timeRemaining))
            )
            val seconds = (timeRemaining / 1000).toInt() % 60
            val minutes = (timeRemaining / (1000 * 60) % 60)
            val hours = (timeRemaining / (1000 * 60 * 60) % 24)
            val days = (timeRemaining / (1000 * 60 * 60) / 24)

            Log.e(TAG, "duration: $duration")
            val today = getTimeFromMillis(System.currentTimeMillis(), "EEEE")
            val quizDate = getTimeFromMillis(quizdateinMili, "dd MMM yyyy, hh:mma")
            val quiztime = getTimeFromMillis(quizdateinMili, "h:mm a")
            quizTimeTextView.text = if (quizDate.contains(today)) "Today $quiztime" else quizDate
            timeLeftTv.text = "$days days $hours hours $minutes minutes left"
            categoryText.isSelected = true
            categoryText.text = quizDetails.category
        }
    }
}
