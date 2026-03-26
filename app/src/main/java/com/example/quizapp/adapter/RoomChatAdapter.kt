package com.example.quizapp.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.quizapp.databinding.ItemRoomMessageBinding
import com.example.quizapp.model.RoomMessage

class RoomChatAdapter(
    private val messages: List<RoomMessage>
) : RecyclerView.Adapter<RoomChatAdapter.RoomMessageViewHolder>() {

    inner class RoomMessageViewHolder(
        private val binding: ItemRoomMessageBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(message: RoomMessage) {
            binding.senderNameTextView.text = message.senderName
            binding.messageTextView.text = message.message

            if (message.isSystemMessage) {
                binding.senderNameTextView.alpha = 0.7f
                binding.messageTextView.alpha = 0.8f
            } else {
                binding.senderNameTextView.alpha = 1f
                binding.messageTextView.alpha = 1f
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RoomMessageViewHolder {
        val binding = ItemRoomMessageBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return RoomMessageViewHolder(binding)
    }

    override fun onBindViewHolder(holder: RoomMessageViewHolder, position: Int) {
        holder.bind(messages[position])
    }

    override fun getItemCount(): Int = messages.size
}