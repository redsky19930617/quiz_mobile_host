package com.quiz.hostapp.ui.host

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.quiz.hostapp.R
import com.quiz.hostapp.network.model.Option

class AnswersAdapter() :
    ListAdapter<Option, AnswersAdapter.AnswerViewHolder>(AnswerDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AnswerViewHolder {
        val itemView =
            LayoutInflater.from(parent.context).inflate(R.layout.answer_item, parent, false)
        return AnswerViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: AnswerViewHolder, position: Int) {
        val currentAnswer = getItem(position)
        holder.bind(currentAnswer, position)
    }

    inner class AnswerViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val answerTextView: TextView = itemView.findViewById(R.id.answer_text_view)

        init {
            itemView.setOnClickListener {
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    val answer = getItem(position)
                }
            }
        }

        fun bind(answer: Option, position: Int) {
            answerTextView.text = "${position.plus(1)}: ${answer.text}"
            if (answer.isCorrect) {
                answerTextView.setTextColor(Color.parseColor("#00E95D"))
                if(answer.isShowingQuestion) {
                    answerTextView.alpha = if (answer.isShowingOptions) 1f else 0.3f
                }
            } else {
                answerTextView.setTextColor(if (answer.isShowingQuestion) Color.BLACK else Color.WHITE
                )
                if(answer.isShowingQuestion) {
                    answerTextView.alpha = if (answer.isShowingOptions) 1f else 0.3f
                }
            }
        }
    }

    interface AnswerClickListener {
        fun onAnswerClick(answer: Option)
    }

    class AnswerDiffCallback : DiffUtil.ItemCallback<Option>() {
        override fun areItemsTheSame(oldItem: Option, newItem: Option): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Option, newItem: Option): Boolean {
            return oldItem == newItem
        }
    }
}


