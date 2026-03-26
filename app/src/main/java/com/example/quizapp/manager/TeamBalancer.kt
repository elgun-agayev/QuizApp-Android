package com.example.quizapp.manager

import com.example.quizapp.model.RoomPlayer

object TeamBalancer {

    fun fillTeamsWithBotsIfNeeded(
        teamAPlayers: MutableList<RoomPlayer>,
        teamBPlayers: MutableList<RoomPlayer>,
        teamFormat: String
    ): Boolean {
        val requiredPlayersPerTeam = getRequiredPlayersPerTeam(teamFormat)

        val realA = teamAPlayers.count { !it.isBot }
        val realB = teamBPlayers.count { !it.isBot }

        // Hər iki komandada ən azı 1 real user yoxdursa match başlamasın
        if (realA < 1 || realB < 1) {
            return false
        }

        // Team A boş yerlərini bot ilə doldur
        var botIndexA = 1
        while (teamAPlayers.size < requiredPlayersPerTeam) {
            teamAPlayers.add(BotPlayerFactory.createBotPlayer("A", botIndexA))
            botIndexA++
        }

        // Team B boş yerlərini bot ilə doldur
        var botIndexB = 1
        while (teamBPlayers.size < requiredPlayersPerTeam) {
            teamBPlayers.add(BotPlayerFactory.createBotPlayer("B", botIndexB))
            botIndexB++
        }

        return true
    }

    fun getRequiredPlayersPerTeam(teamFormat: String): Int {
        return when (teamFormat.lowercase()) {
            "2v2" -> 2
            "3v3" -> 3
            "4v4" -> 4
            else -> 2
        }
    }

    fun getTopPlayer(players: List<RoomPlayer>): RoomPlayer? {
        return players.sortedWith(
            compareByDescending<RoomPlayer> { it.correctCount }
                .thenBy { it.wrongCount }
                .thenBy { it.joinedAt }
        ).firstOrNull()
    }
}