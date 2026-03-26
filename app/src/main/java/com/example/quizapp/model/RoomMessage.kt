package com.example.quizapp.model

data class RoomMessage(
    val senderName: String,
    val message: String,
    val isSystemMessage: Boolean = false,
    val timestamp: Long = System.currentTimeMillis()
)