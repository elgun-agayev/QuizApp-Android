package com.example.quizapp.model

import com.example.quizapp.R

enum class UserStatus {
    ONLINE,
    OFFLINE,
    IN_GAME,
}

data class User(
    val userId: String = "",
    val name: String = "",
    val avatarResId: Int = R.drawable.ic_avatar_placeholder,
    val score: Int = 0,
    val status: UserStatus = UserStatus.OFFLINE,
    val totalGames: Int = 0,
    val correctAnswers: Int = 0
)