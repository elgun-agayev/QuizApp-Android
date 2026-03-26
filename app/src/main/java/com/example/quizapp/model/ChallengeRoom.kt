package com.example.quizapp.model

data class ChallengeRoom(
    val roomId: String = "",
    val requestId: String = "",
    val hostPlayerId: String = "",
    val hostPlayerName: String = "",
    val hostPlayerAvatar: Int = 0,
    val guestPlayerId: String = "",
    val guestPlayerName: String = "",
    val guestPlayerAvatar: Int = 0,
    val category: String = "Random",
    val mode: String = "1 vs 1",
    val status: String = "waiting",
    val createdAt: Long = 0L,
    val hostJoined: Boolean = false,
    val guestJoined: Boolean = false,
    val hostState: String = "waiting",
    val guestState: String = "waiting",
    val cancelledBy: String = "",
    val cancelReason: String = "",
    val lastActionAt: Long = 0L,
    val closeAt: Long = 0L
)