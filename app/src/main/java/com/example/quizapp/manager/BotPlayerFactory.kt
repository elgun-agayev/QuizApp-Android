package com.example.quizapp.manager

import com.example.quizapp.model.RoomPlayer

object BotPlayerFactory {

    fun createBotPlayer(team: String, index: Int): RoomPlayer {
        return RoomPlayer(
            uid = "bot_${team.lowercase()}_$index",
            name = "CPU $team$index",
            avatarUrl = "",
            team = team,
            isBot = true,
            correctCount = 0,
            wrongCount = 0,
            score = 0,
            joinedAt = System.currentTimeMillis(),
            isReady = true
        )
    }

}