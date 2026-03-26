package com.example.quizapp.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.quizapp.R
import com.example.quizapp.databinding.ItemFavoriteQuestionBinding
import com.example.quizapp.manager.FavoriteManager
import com.example.quizapp.model.FavoriteQuestion

class FavoriteAdapter(
    private val favoriteList: MutableList<FavoriteQuestion>,
    private val onFavoriteRemoved: () -> Unit
) : RecyclerView.Adapter<FavoriteAdapter.FavoriteViewHolder>() {

    inner class FavoriteViewHolder(
        private val binding: ItemFavoriteQuestionBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: FavoriteQuestion) {
            binding.favoriteCategoryIconImageView.setImageResource(
                if (item.categoryIconResId != 0) {
                    item.categoryIconResId
                } else {
                    R.drawable.ic_category_informatics
                }
            )
            binding.favoriteCategoryTextView.text = item.category.ifBlank { "Unknown Category" }
            binding.favoriteQuestionTextView.text = item.question.ifBlank { "No question available" }
            binding.favoriteCorrectAnswerTextView.text =
                item.correctAnswer.ifBlank { "No answer available" }
            binding.removeFavoriteImageView.setImageResource(R.drawable.ic_bookmark_selected)

            binding.removeFavoriteImageView.setOnClickListener {
                val position = bindingAdapterPosition

                if (position != RecyclerView.NO_POSITION) {
                    val currentItem = favoriteList[position]

                    FavoriteManager.removeFavorite(currentItem.question)

                    favoriteList.removeAt(position)
                    notifyItemRemoved(position)

                    onFavoriteRemoved()
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FavoriteViewHolder {
        val binding = ItemFavoriteQuestionBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return FavoriteViewHolder(binding)
    }

    override fun onBindViewHolder(holder: FavoriteViewHolder, position: Int) {
        holder.bind(favoriteList[position])
    }

    override fun getItemCount(): Int = favoriteList.size

    fun submitList(newList: List<FavoriteQuestion>) {
        favoriteList.clear()
        favoriteList.addAll(newList)
        notifyDataSetChanged()
    }

    fun updateData(newList: List<FavoriteQuestion>) {
        favoriteList.clear()
        favoriteList.addAll(newList)
        notifyDataSetChanged()
    }
}