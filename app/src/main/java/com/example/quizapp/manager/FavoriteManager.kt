package com.example.quizapp.manager

import com.example.quizapp.model.FavoriteQuestion

object FavoriteManager {

    private val favoriteQuestions = mutableListOf<FavoriteQuestion>()

    fun getFavorites(): MutableList<FavoriteQuestion> {
        return favoriteQuestions
    }

    fun addFavorite(question: FavoriteQuestion) {
        if (!favoriteQuestions.any { it.question == question.question }) {
            favoriteQuestions.add(question)
        }
    }

    fun removeFavorite(questionText: String) {
        favoriteQuestions.removeAll { it.question == questionText }
    }

    fun isFavorite(questionText: String): Boolean {
        return favoriteQuestions.any { it.question == questionText }
    }
}