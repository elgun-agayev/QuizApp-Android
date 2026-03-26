package com.example.quizapp.model

data class AnswerReviewItem(
    val category: String,
    val question: String,
    val options: List<String>,
    val correctAnswerIndex: Int,
    val selectedAnswerIndex: Int?
)