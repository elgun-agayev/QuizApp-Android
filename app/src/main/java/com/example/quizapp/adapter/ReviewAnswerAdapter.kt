package com.example.quizapp.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.quizapp.R
import com.example.quizapp.databinding.ItemReviewAnswerBinding
import com.example.quizapp.manager.FavoriteManager
import com.example.quizapp.model.AnswerReviewItem
import com.example.quizapp.model.FavoriteQuestion

class ReviewAnswerAdapter(
    private val reviewList: List<AnswerReviewItem>
) : RecyclerView.Adapter<ReviewAnswerAdapter.ReviewAnswerViewHolder>() {

    inner class ReviewAnswerViewHolder(
        private val binding: ItemReviewAnswerBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: AnswerReviewItem) {
            val userAnswerText = if (item.selectedAnswerIndex != null) {
                item.options[item.selectedAnswerIndex]
            } else {
                "Not answered"
            }

            val correctAnswerText = item.options[item.correctAnswerIndex]

            binding.reviewQuestionTextView.text = item.question
            binding.reviewUserAnswerTextView.text = "Your answer: $userAnswerText"
            binding.reviewCorrectAnswerTextView.text = "Correct answer: $correctAnswerText"

            val isFavorite = FavoriteManager.isFavorite(item.question)

            if (isFavorite) {
                binding.reviewFavoriteImageView.setImageResource(R.drawable.ic_bookmark_selected)
            } else {
                binding.reviewFavoriteImageView.setImageResource(R.drawable.ic_bookmark)
            }

            binding.reviewFavoriteImageView.setOnClickListener {
                if (FavoriteManager.isFavorite(item.question)) {
                    FavoriteManager.removeFavorite(item.question)
                    binding.reviewFavoriteImageView.setImageResource(R.drawable.ic_bookmark)
                } else {
                    val favoriteQuestion = FavoriteQuestion(
                        category = item.category,
                        question = item.question,
                        correctAnswer = correctAnswerText,
                        categoryIconResId = getCategoryIconResId(item.category),
                        isFavorite = true
                    )

                    FavoriteManager.addFavorite(favoriteQuestion)
                    binding.reviewFavoriteImageView.setImageResource(R.drawable.ic_bookmark_selected)
                }
            }
        }

        private fun getCategoryIconResId(category: String): Int {
            return when (category.lowercase()) {
                "informatics" -> R.drawable.ic_category_informatics
                "mathematics" -> R.drawable.ic_catagory_math_pi
                "english" -> R.drawable.ic_category_english
                "history" -> R.drawable.ic_category_history
                "world knowledge", "world", "worldview", "world_knowledge" -> R.drawable.ic_category_knowledge
                "logic" -> R.drawable.ic_category_logic
                else -> R.drawable.ic_category_informatics
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ReviewAnswerViewHolder {
        val binding = ItemReviewAnswerBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ReviewAnswerViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ReviewAnswerViewHolder, position: Int) {
        holder.bind(reviewList[position])
    }

    override fun getItemCount(): Int = reviewList.size
}