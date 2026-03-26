package com.example.quizapp.manager

import android.content.Context
import androidx.lifecycle.MutableLiveData
import com.example.quizapp.R
import com.example.quizapp.model.User
import com.example.quizapp.model.UserStatus

object UserManager {

    private const val PREFS_NAME = "user_prefs"

    private const val KEY_USER_ID = "user_id"
    private const val KEY_NAME = "name"
    private const val KEY_AVATAR = "avatar"
    private const val KEY_SCORE = "score"
    private const val KEY_STATUS = "status"
    private const val KEY_TOTAL_GAMES = "total_games"
    private const val KEY_CORRECT_ANSWERS = "correct_answers"

    val userLiveData = MutableLiveData<User>()

    private val defaultUser = User(
        userId = "",
        name = "",
        avatarResId = R.drawable.ic_avatar_placeholder,
        score = 0,
        status = UserStatus.OFFLINE,
        totalGames = 0,
        correctAnswers = 0
    )

    var currentUser = defaultUser

    fun updateProfile(name: String, avatarResId: Int) {
        currentUser = currentUser.copy(
            name = name,
            avatarResId = if (avatarResId != 0) avatarResId else R.drawable.ic_avatar_placeholder
        )
        userLiveData.postValue(currentUser)
    }

    fun updateUser(
        userId: String = currentUser.userId,
        name: String = currentUser.name,
        avatarResId: Int = currentUser.avatarResId,
        score: Int = currentUser.score,
        status: UserStatus = currentUser.status,
        totalGames: Int = currentUser.totalGames,
        correctAnswers: Int = currentUser.correctAnswers
    ) {
        currentUser = User(
            userId = userId,
            name = name,
            avatarResId = if (avatarResId != 0) avatarResId else R.drawable.ic_avatar_placeholder,
            score = score,
            status = status,
            totalGames = totalGames,
            correctAnswers = correctAnswers
        )
        userLiveData.postValue(currentUser)
    }

    fun saveUser(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        val statusString = when (currentUser.status) {
            UserStatus.ONLINE -> "online"
            UserStatus.OFFLINE -> "offline"
            UserStatus.IN_GAME -> "in_game"
        }

        prefs.edit()
            .putString(KEY_USER_ID, currentUser.userId)
            .putString(KEY_NAME, currentUser.name)
            .putInt(KEY_AVATAR, if (currentUser.avatarResId != 0) currentUser.avatarResId else R.drawable.ic_avatar_placeholder)
            .putInt(KEY_SCORE, currentUser.score)
            .putString(KEY_STATUS, statusString)
            .putInt(KEY_TOTAL_GAMES, currentUser.totalGames)
            .putInt(KEY_CORRECT_ANSWERS, currentUser.correctAnswers)
            .apply()
    }

    fun loadUser(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        if (!prefs.contains(KEY_USER_ID)) {
            currentUser = defaultUser
            userLiveData.postValue(currentUser)
            return
        }

        val userId = prefs.getString(KEY_USER_ID, "") ?: ""
        val name = prefs.getString(KEY_NAME, "") ?: ""
        val avatar = prefs.getInt(KEY_AVATAR, R.drawable.ic_avatar_placeholder)
        val score = prefs.getInt(KEY_SCORE, 0)
        val statusString = prefs.getString(KEY_STATUS, "offline") ?: "offline"
        val status = when (statusString) {
            "online" -> UserStatus.ONLINE
            "offline" -> UserStatus.OFFLINE
            "in_game" -> UserStatus.IN_GAME
            else -> UserStatus.OFFLINE
        }
        val totalGames = prefs.getInt(KEY_TOTAL_GAMES, 0)
        val correctAnswers = prefs.getInt(KEY_CORRECT_ANSWERS, 0)

        currentUser = User(
            userId = userId,
            name = name,
            avatarResId = if (avatar != 0) avatar else R.drawable.ic_avatar_placeholder,
            score = score,
            status = status,
            totalGames = totalGames,
            correctAnswers = correctAnswers
        )
        userLiveData.postValue(currentUser)
    }

    fun resetToDefault(context: Context) {
        currentUser = defaultUser
        userLiveData.postValue(currentUser)
        saveUser(context)
    }

    fun clearUser(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().clear().apply()
        currentUser = defaultUser
        userLiveData.postValue(currentUser)
    }

    fun switchToJoseph(context: Context) {
        currentUser = User(
            userId = "AEL-29481",
            name = "Joseph",
            avatarResId = R.drawable.avatar_1,
            score = 12500,
            status = UserStatus.ONLINE,
            totalGames = 120,
            correctAnswers = 95
        )
        userLiveData.postValue(currentUser)
        saveUser(context)
    }

    fun switchToAlex(context: Context) {
        currentUser = User(
            userId = "AEL-1001",
            name = "Alex",
            avatarResId = R.drawable.avatar_3,
            score = 980,
            status = UserStatus.ONLINE,
            totalGames = 97,
            correctAnswers = 720
        )
        userLiveData.postValue(currentUser)
        saveUser(context)
    }
}