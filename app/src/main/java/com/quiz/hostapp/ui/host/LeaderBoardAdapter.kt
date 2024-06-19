package com.quiz.hostapp.ui.host

import android.annotation.SuppressLint
import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.quiz.hostapp.MyApplication.Companion.session
import com.quiz.hostapp.MyApplication.Companion.sessionManager
import com.quiz.hostapp.R
import com.quiz.hostapp.databinding.LeaderBoardListItemBinding
import com.quiz.hostapp.utils.SessionManager
import com.bumptech.glide.Glide

data class Participant(
    val rank: Int,
    val profile_pic: String,
    val name: String,
    val timeTaken: Double,
    val total_question: Int,
    val correct_answer: Int,
    val isParticipant: Boolean = false,
    val userId: String = "",
    val avatar: String = ""
)

class LeaderBoardAdapter :
    ListAdapter<Participant, LeaderBoardAdapter.ParticipantViewHolder>(ParticipantDiffCallback()) {
    var total_question = 0

    fun submitTotalQuestions(value:Int){
        total_question = value
    }
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ParticipantViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = LeaderBoardListItemBinding.inflate(inflater, parent, false)
        return ParticipantViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ParticipantViewHolder, position: Int) {
        val participant = getItem(position)
        participant?.let {
            holder.bind(participant,holder)
        }
    }

    inner class ParticipantViewHolder(private val binding: LeaderBoardListItemBinding) :
        RecyclerView.ViewHolder(binding.root) {
        @SuppressLint("StringFormatMatches")
        fun bind(participant: Participant, holder: ParticipantViewHolder) {
            binding.apply {
                scoreTv.text = participant.rank.toString()
                participantNameTv.text = participant.name

                timeTakenTv.text = String.format("%.2f sec", participant.timeTaken)
                answerTv.text = "${participant.correct_answer}/${total_question}"

                scoreTv.setTextColor(if(participant.isParticipant) ContextCompat.getColor(scoreTv.context,R.color.dark_purple) else Color.WHITE)
                participantNameTv.setTextColor(if(participant.isParticipant) ContextCompat.getColor(participantNameTv.context,R.color.dark_purple) else Color.WHITE)
                timeTakenTv.setTextColor(if(participant.isParticipant) ContextCompat.getColor(timeTakenTv.context,R.color.dark_purple)  else Color.WHITE)
                answerTv.setTextColor(if(participant.isParticipant) ContextCompat.getColor(answerTv.context,R.color.dark_purple)  else Color.WHITE)

                Glide.with(holder.itemView)
                    .load(participant.avatar)
                    .placeholder(R.drawable.profile)
                    .circleCrop()   // This makes the loaded image circular
                    .into(participantIv)

                val userId = sessionManager?.getPrefString(SessionManager.USER_ID)?:""
                if (userId == participant.userId) {
                    leaderboardContainer.background = ContextCompat.getDrawable(
                        binding.leaderboardContainer.context,
                        R.drawable.while_rounded_bg
                    )
                } else {
                    leaderboardContainer.setBackgroundColor(
                        ContextCompat.getColor(
                            binding.leaderboardContainer.context,
                            R.color.dark_purple
                        )
                    )
                }
            }
        }
    }

    class ParticipantDiffCallback : DiffUtil.ItemCallback<Participant>() {
        override fun areItemsTheSame(oldItem: Participant, newItem: Participant): Boolean {
            return oldItem.rank == newItem.rank
        }

        override fun areContentsTheSame(oldItem: Participant, newItem: Participant): Boolean {
            return oldItem == newItem
        }
    }
}

