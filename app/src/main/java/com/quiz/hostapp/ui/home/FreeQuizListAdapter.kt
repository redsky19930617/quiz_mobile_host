package com.quiz.hostapp.ui.home

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView
import com.quiz.hostapp.R
import com.quiz.hostapp.network.model.FreeQuiz
import com.quiz.hostapp.utils.getRandomColor

class FreeQuizListAdapter :
    ListAdapter<FreeQuiz, FreeQuizListAdapter.FreeQuizListViewHolder>(FreeQuizDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FreeQuizListViewHolder {
        val itemView =
            LayoutInflater.from(parent.context).inflate(R.layout.free_quiz_list_item, parent, false)
        return FreeQuizListViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: FreeQuizListViewHolder, position: Int) {
        val FreeQuiz = getItem(position)
        holder.bind(FreeQuiz)
    }

    inner class FreeQuizListViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val categoryNameTextView: TextView = itemView.findViewById(R.id.categoryTv)
        private val availableQuizTextView: TextView = itemView.findViewById(R.id.availableTv)
        private val catCard: MaterialCardView = itemView.findViewById(R.id.quizCategoryLayout)

        fun bind(FreeQuiz: FreeQuiz) {
            catCard.setCardBackgroundColor(getRandomColor())
            categoryNameTextView.text = FreeQuiz.category
            availableQuizTextView.text =
                "Available Quiz: ${FreeQuiz.count} . Completed: ${FreeQuiz.completed}"
        }
    }

    class FreeQuizDiffCallback : DiffUtil.ItemCallback<FreeQuiz>() {
        override fun areItemsTheSame(oldItem: FreeQuiz, newItem: FreeQuiz): Boolean {
            return oldItem.category == newItem.category
        }

        override fun areContentsTheSame(oldItem: FreeQuiz, newItem: FreeQuiz): Boolean {
            return oldItem == newItem
        }
    }
}
