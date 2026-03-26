package com.example.quizapp.model

data class FavoriteQuestion(
    val category: String,
    val question: String,
    val correctAnswer: String,
    val categoryIconResId: Int,
    var isFavorite: Boolean = true
)