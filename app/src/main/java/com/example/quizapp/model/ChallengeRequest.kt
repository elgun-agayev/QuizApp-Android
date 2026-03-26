package com.example.quizapp.model

data class ChallengeRequest(
    val requestId: String = "",
    val fromPlayerId: String = "",
    val fromPlayerName: String = "",
    val fromPlayerAvatar: Int = 0,
    val toPlayerId: String = "",
    val toPlayerName: String = "",
    val toPlayerAvatar: Int = 0,
    val category: String = "",
    val mode: String = "1 vs 1",
    val status: String = "pending",
    val timestamp: Long = System.currentTimeMillis()
)
