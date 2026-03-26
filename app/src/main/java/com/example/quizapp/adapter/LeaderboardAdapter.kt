package com.example.quizapp.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.quizapp.R
import com.example.quizapp.databinding.ItemLeaderboardPlayerBinding
import java.util.Locale

class LeaderboardAdapter(
    private val onItemClick: (LeaderboardPlayer) -> Unit,
    private val onMoreOptionsClick: (View, LeaderboardPlayer) -> Unit
) : RecyclerView.Adapter<LeaderboardAdapter.LeaderboardViewHolder>() {

    private val playerList = mutableListOf<LeaderboardPlayer>()

    fun submitList(list: List<LeaderboardPlayer>) {
        playerList.clear()
        playerList.addAll(list)
        notifyDataSetChanged()
    }

    inner class LeaderboardViewHolder(
        val binding: ItemLeaderboardPlayerBinding
    ) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LeaderboardViewHolder {
        val binding = ItemLeaderboardPlayerBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return LeaderboardViewHolder(binding)
    }

    override fun onBindViewHolder(holder: LeaderboardViewHolder, position: Int) {
        val player = playerList[position]
        val rank = getDisplayRank(position)

        with(holder.binding) {
            playerRankTextView.text = rank.toString()
            playerNameTextView.text = player.playerName.trim().ifBlank { "Unknown Player" }
            playerScoreTextView.text = player.playerScore.toString()

            playerAvatarImageView.setImageResource(
                if (player.playerAvatar != 0) {
                    player.playerAvatar
                } else {
                    R.drawable.ic_avatar_placeholder
                }
            )

            playerStatusIndicator.setBackgroundResource(
                getStatusBackground(player.playerStatus)
            )

            leaderboardPlayerItemContainer.setOnClickListener {
                val adapterPosition = holder.bindingAdapterPosition
                if (adapterPosition != RecyclerView.NO_POSITION) {
                    onItemClick(playerList[adapterPosition])
                }
            }

            playerMoreOptionsImageView.setOnClickListener { anchorView ->
                val adapterPosition = holder.bindingAdapterPosition
                if (adapterPosition != RecyclerView.NO_POSITION) {
                    onMoreOptionsClick(anchorView, playerList[adapterPosition])
                }
            }
        }
    }

    override fun getItemCount(): Int = playerList.size

    private fun getDisplayRank(position: Int): Int {
        return position + 4
    }

    private fun getStatusBackground(status: String?): Int {
        return when (status?.trim()?.lowercase(Locale.ROOT)) {
            "online" -> R.drawable.status_online
            "offline" -> R.drawable.status_offline
            "in_game", "ingame", "in game" -> R.drawable.status_ingame
            "searching", "searching_match", "searching match" -> R.drawable.status_online
            else -> R.drawable.status_offline
        }
    }
}