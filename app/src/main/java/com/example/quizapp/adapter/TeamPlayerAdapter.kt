package com.example.quizapp.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.example.quizapp.R
import com.example.quizapp.manager.UserManager
import com.example.quizapp.model.RoomPlayer
import com.google.android.material.imageview.ShapeableImageView
import java.util.Locale
import kotlin.math.absoluteValue

class TeamPlayerAdapter(
    private val players: MutableList<RoomPlayer>,
    private val isRoomCreator: Boolean,
    private val roomCreatorName: String,
    private val onKickClick: (RoomPlayer) -> Unit
) : RecyclerView.Adapter<TeamPlayerAdapter.TeamPlayerViewHolder>() {

    inner class TeamPlayerViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        private val avatarImageView: ShapeableImageView =
            itemView.findViewById(R.id.playerAvatarImageView)

        private val onlineStatusView: View =
            itemView.findViewById(R.id.playerOnlineStatusView)

        private val playerNameTextView: TextView =
            itemView.findViewById(R.id.playerNameTextView)

        private val playerIdTextView: TextView =
            itemView.findViewById(R.id.playerIdTextView)

        private val playerScoreTextView: TextView =
            itemView.findViewById(R.id.playerScoreTextView)

        private val playerKickImageView: ImageView =
            itemView.findViewById(R.id.playerKickImageView)

        fun bind(player: RoomPlayer) {

            val currentUser = UserManager.currentUser

            val safeName = player.name.ifBlank { "Unknown Player" }
            playerNameTextView.text = safeName

            playerIdTextView.text = "ID: ${player.uid}"

            playerScoreTextView.text = player.score.toString()

            onlineStatusView.isVisible = !player.isBot

            avatarImageView.setImageResource(getAvatarRes(player, currentUser))

            val canShowKick = isRoomCreator &&
                    !player.name.equals(roomCreatorName, ignoreCase = true)

            playerKickImageView.isVisible = canShowKick

            playerKickImageView.setOnClickListener {
                val adapterPosition = bindingAdapterPosition
                if (adapterPosition != RecyclerView.NO_POSITION && canShowKick) {
                    onKickClick(players[adapterPosition])
                }
            }
        }

        private fun getAvatarRes(player: RoomPlayer, currentUser: com.example.quizapp.model.User): Int {

            if (player.uid == currentUser.userId) {
                return if (currentUser.avatarResId != 0) {
                    currentUser.avatarResId
                } else {
                    R.drawable.ic_avatar_placeholder
                }
            }

            val avatarList = listOf(
                R.drawable.avatar_1,
                R.drawable.avatar_2,
                R.drawable.avatar_3,
                R.drawable.avatar_4
            )

            val index = (player.uid.hashCode().absoluteValue) % avatarList.size

            return when (player.name.trim().lowercase(Locale.ROOT)) {
                "you" -> currentUser.avatarResId
                else -> avatarList[index]
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TeamPlayerViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_team_player, parent, false)
        return TeamPlayerViewHolder(view)
    }

    override fun onBindViewHolder(holder: TeamPlayerViewHolder, position: Int) {
        holder.bind(players[position])
    }

    override fun getItemCount(): Int = players.size
}