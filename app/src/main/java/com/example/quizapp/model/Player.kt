package com.example.quizapp.model

data class Player(
    val userId: String = "",
    val name: String = "",
    val avatarResId: Int = 0,
    val score: Int = 0,
    val status: String = ""
)
