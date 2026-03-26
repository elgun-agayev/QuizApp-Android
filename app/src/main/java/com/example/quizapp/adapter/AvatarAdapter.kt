package com.example.quizapp.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.quizapp.databinding.ItemAvatarBinding

class AvatarAdapter(
    private val avatarList: List<Int>,
    private var selectedAvatarResId: Int,
    private val onAvatarSelected: (Int) -> Unit
) : RecyclerView.Adapter<AvatarAdapter.AvatarViewHolder>() {

    inner class AvatarViewHolder(val binding: ItemAvatarBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AvatarViewHolder {
        val binding = ItemAvatarBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return AvatarViewHolder(binding)
    }

    override fun onBindViewHolder(holder: AvatarViewHolder, position: Int) {
        val avatarResId = avatarList[position]

        with(holder.binding) {
            avatarOptionImageView.setImageResource(avatarResId)

            if (avatarResId == selectedAvatarResId) {
                avatarOptionImageView.strokeWidth = 4f
            } else {
                avatarOptionImageView.strokeWidth = 0f
            }

            root.setOnClickListener {
                val previousSelected = selectedAvatarResId
                selectedAvatarResId = avatarResId

                val previousIndex = avatarList.indexOf(previousSelected)
                if (previousIndex != -1) notifyItemChanged(previousIndex)

                notifyItemChanged(position)
                onAvatarSelected(avatarResId)
            }
        }
    }

    override fun getItemCount(): Int = avatarList.size
}