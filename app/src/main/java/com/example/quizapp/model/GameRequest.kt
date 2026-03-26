package com.example.quizapp.model

data class GameRequest(
    val requestId: String = "",
    val fromPlayerId: String = "",
    val fromPlayerName: String = "",
    val fromPlayerAvatar: Int = 0,
    val status: String = "pending",
    val timestamp: Long = 0L
)
