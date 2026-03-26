package com.example.quizapp.model

data class RoomPlayer(

    val uid: String = "",                // Player unique id (Firebase id və ya local id)
    val name: String = "",               // Player name
    val avatarUrl: String = "",          // Firebase avatar (istifadə etməsən boş qala bilər)
    val team: String = "",               // "A" və ya "B"
    val isBot: Boolean = false,          // Bot olub olmadığını göstərir
    val correctCount: Int = 0,           // Doğru cavab sayı
    val wrongCount: Int = 0,             // Səhv cavab sayı
    val score: Int = 0,                  // Ümumi score
    val joinedAt: Long = System.currentTimeMillis(), // Rooma qoşulma vaxtı
    val isReady: Boolean = false         // Oyuna hazır olub olmadığını göstərir
)