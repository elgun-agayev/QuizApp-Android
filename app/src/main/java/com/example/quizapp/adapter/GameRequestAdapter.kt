package com.example.quizapp.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.quizapp.R
import com.example.quizapp.databinding.ItemGameRequestBinding
import com.example.quizapp.model.GameRequest

class GameRequestAdapter(
    private val onAcceptClick: (GameRequest) -> Unit,
    private val onDeclineClick: (GameRequest) -> Unit
) : RecyclerView.Adapter<GameRequestAdapter.GameRequestViewHolder>() {

    private val requestList = mutableListOf<GameRequest>()

    fun submitList(list: List<GameRequest>) {
        requestList.clear()
        requestList.addAll(list)
        notifyDataSetChanged()
    }

    inner class GameRequestViewHolder(
        private val binding: ItemGameRequestBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(request: GameRequest) {
            binding.requestPlayerNameTextView.text =
                request.fromPlayerName.trim().ifBlank { "Unknown Player" }

            binding.requestMessageTextView.text =
                binding.root.context.getString(R.string.item_game_request_wants_to_play_with_you)

            binding.requestPlayerAvatarImageView.setImageResource(
                if (request.fromPlayerAvatar != 0) {
                    request.fromPlayerAvatar
                } else {
                    R.drawable.ic_avatar_placeholder
                }
            )

            binding.acceptRequestImageView.setOnClickListener {
                val adapterPosition = bindingAdapterPosition
                if (adapterPosition != RecyclerView.NO_POSITION) {
                    onAcceptClick(requestList[adapterPosition])
                }
            }

            binding.declineRequestImageView.setOnClickListener {
                val adapterPosition = bindingAdapterPosition
                if (adapterPosition != RecyclerView.NO_POSITION) {
                    onDeclineClick(requestList[adapterPosition])
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GameRequestViewHolder {
        val binding = ItemGameRequestBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return GameRequestViewHolder(binding)
    }

    override fun onBindViewHolder(holder: GameRequestViewHolder, position: Int) {
        holder.bind(requestList[position])
    }

    override fun getItemCount(): Int = requestList.size
}