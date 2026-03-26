package com.example.quizapp.ui

import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import androidx.activity.addCallback
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.quizapp.R
import com.example.quizapp.databinding.FragmentResultBinding
import com.example.quizapp.manager.UserManager

class ResultFragment : Fragment(R.layout.fragment_result) {

    private var _binding: FragmentResultBinding? = null
    private val binding get() = _binding!!

    private var score: Int = 0
    private var correctCount: Int = 0
    private var wrongCount: Int = 0
    private var categoryName: String = "Informatics"
    private var difficultyName: String = "easy"
    private var questionCount: Int = 10

    private var gameMode: String = "single"

    private var isOnlineDuel: Boolean = false
    private var isBotMatch: Boolean = false
    private var isTeamBattleMode: Boolean = false
    private var isMultiplayer: Boolean = false

    private var opponentName: String = "Opponent"
    private var opponentId: String = ""
    private var opponentAvatarRes: Int = R.drawable.ic_avatar_placeholder
    private var opponentCorrectCount: Int = 0
    private var playerName: String = "You"
    private var playerAvatarRes: Int = R.drawable.ic_avatar_placeholder
    private var playerId: String = ""

    private var teamAScore: Int = 7
    private var teamBScore: Int = 5
    private var topPerformerName: String = "Elgun"
    private var topPerformerCorrect: Int = 4
    private var topPerformerWrong: Int = 1
    private var playerTeamName: String = "Team A"

    private var isForfeitResult: Boolean = false
    private var didCurrentPlayerForfeit: Boolean = false
    private var forfeitTitle: String = "Match Forfeited"
    private var forfeitMessage: String = "You left the match"
    private var forfeitWinnerMessage: String = "Opponent wins by forfeit"

    private var roomId: String = ""

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        _binding = FragmentResultBinding.bind(view)

        UserManager.loadUser(requireContext())

        getResultData()
        setupBackNavigation()
        setupResultUI()
        setupPressedEffects()
        setupClickListeners()

        UserManager.userLiveData.observe(viewLifecycleOwner) {
            if (!isAdded || _binding == null) return@observe
            getResultData()
            setupResultUI()
        }
    }

    private fun setupBackNavigation() {
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner) {
            handleBackNavigation()
        }
    }

    private fun handleBackNavigation() {
        when {
            isChallengeResult() -> {
                findNavController().navigate(R.id.action_resultFragment_to_homeFragment)
            }

            isMultiplayerTeamResult() -> {
                findNavController().navigate(R.id.action_resultFragment_to_homeFragment)
            }

            isFriendsRoomMultiplayerResult() -> {
                findNavController().navigate(R.id.action_resultFragment_to_homeFragment)
            }

            else -> {
                findNavController().navigate(R.id.action_resultFragment_to_homeFragment)
            }
        }
    }

    private fun isChallengeResult(): Boolean {
        return gameMode.equals("challenge", ignoreCase = true)
    }

    private fun isFriendsRoomMultiplayerResult(): Boolean {
        return isMultiplayer &&
                roomId.isNotBlank() &&
                !isOnlineDuel &&
                !isChallengeResult()
    }

    private fun isMultiplayerTeamResult(): Boolean {
        return isTeamBattleMode && isMultiplayer
    }

    private fun shouldShowReplayButton(): Boolean {
        return !isOnlineDuel &&
                !isTeamBattleMode &&
                !isMultiplayerTeamResult()
    }

    private fun getResultData() {
        val bundle = arguments ?: return
        val currentUser = UserManager.currentUser

        score = bundle.getInt("score", 0)
        correctCount = bundle.getInt("correct", 0)
        wrongCount = bundle.getInt("wrong", 0)
        categoryName = bundle.getString("category", "Informatics").orEmpty()
        difficultyName = bundle.getString("difficulty", "easy").orEmpty()
        questionCount = bundle.getInt("questionCount", 10)

        gameMode = bundle.getString("gameMode", "single").orEmpty()
        roomId = bundle.getString("roomId", "").orEmpty()

        isTeamBattleMode = bundle.getBoolean("isTeamBattleMode", false)
        isMultiplayer = bundle.getBoolean("isMultiplayer", false)
        isOnlineDuel = bundle.getBoolean("isOnlineDuel", false)
        isBotMatch = bundle.getBoolean("isBotMatch", false)

        isForfeitResult = bundle.getBoolean("isForfeitResult", false)
        didCurrentPlayerForfeit = bundle.getBoolean("didCurrentPlayerForfeit", false)
        forfeitTitle = bundle.getString("forfeitTitle", "Match Forfeited").orEmpty()
        forfeitMessage = bundle.getString("forfeitMessage", "You left the match").orEmpty()
        forfeitWinnerMessage = bundle.getString("forfeitWinnerMessage", "Opponent wins by forfeit").orEmpty()

        opponentName = bundle.getString("opponentName", "Opponent").orEmpty()
        opponentId = bundle.getString("opponentId", "").orEmpty()
        opponentAvatarRes = bundle.getInt("opponentAvatar", R.drawable.ic_avatar_placeholder)
        opponentCorrectCount = bundle.getInt("opponentCorrect", 0)

        playerName = bundle.getString("playerName", currentUser.name).orEmpty()
        playerId = bundle.getString("playerId", currentUser.userId).orEmpty()
        playerAvatarRes = bundle.getInt("playerAvatar", currentUser.avatarResId)

        if (playerName.isBlank() || playerName.equals("You", ignoreCase = true)) {
            playerName = currentUser.name
        }

        if (playerId.isBlank()) {
            playerId = currentUser.userId
        }

        if (playerAvatarRes == 0) {
            playerAvatarRes = currentUser.avatarResId
        }

        if (playerAvatarRes == 0) {
            playerAvatarRes = R.drawable.ic_avatar_placeholder
        }

        if (opponentName.isBlank()) {
            opponentName = "Opponent"
        }

        if (opponentAvatarRes == 0) {
            opponentAvatarRes = R.drawable.ic_avatar_placeholder
        }

        if (forfeitTitle.isBlank()) {
            forfeitTitle = "Match Forfeited"
        }

        if (forfeitMessage.isBlank()) {
            forfeitMessage = "You left the match"
        }

        if (forfeitWinnerMessage.isBlank()) {
            forfeitWinnerMessage = "Opponent wins by forfeit"
        }

        teamAScore = bundle.getInt("teamAScore", 7)
        teamBScore = bundle.getInt("teamBScore", 5)

        topPerformerName = bundle.getString("topPerformerName", currentUser.name).orEmpty()
        if (topPerformerName.isBlank() || topPerformerName.equals("You", ignoreCase = true)) {
            topPerformerName = currentUser.name
        }

        topPerformerCorrect = bundle.getInt("topPerformerCorrect", 4)
        topPerformerWrong = bundle.getInt("topPerformerWrong", 1)
        playerTeamName = bundle.getString("playerTeamName", "Team A").orEmpty()

        if (playerTeamName.isBlank()) playerTeamName = "Team A"
        if (opponentName.isBlank()) opponentName = "Opponent"
        if (playerName.isBlank()) playerName = currentUser.name
        if (playerId.isBlank()) playerId = currentUser.userId

        if (gameMode.equals("challenge", ignoreCase = true)) {
            isOnlineDuel = true
        }
    }

    private fun setupResultUI() {
        val totalQuestions = correctCount + wrongCount
        val accuracy = if (totalQuestions == 0) 0 else (correctCount * 100) / totalQuestions

        binding.scoreTextView.text = score.toString()
        binding.correctCountTextView.text = correctCount.toString()
        binding.wrongCountTextView.text = wrongCount.toString()
        binding.accuracyTextView.text = "$accuracy%"

        setBackgroundByCategory(categoryName)

        when {
            isTeamBattleMode -> setupTeamBattleResultUI(accuracy)
            isOnlineDuel && isForfeitResult -> setupForfeitResultUI(accuracy)
            isOnlineDuel -> setupDuelResultUI(accuracy)
            else -> setupNormalResultUI()
        }
    }

    private fun setupNormalResultUI() = with(binding) {
        duelResultCardView.isVisible = false
        teamResultCardView.isVisible = false
        resultScoreCardView.isVisible = true

        resultTitleTextView.text = "Quiz Completed!"
        resultTitleTextView.setTextColor(
            ContextCompat.getColor(requireContext(), R.color.color_navy_blue)
        )
        resultSubtitleTextView.text = "${formatCategoryName(categoryName)} quiz finished"
        scoreLabelTextView.text = "Your Score"

        replayButton.isVisible = shouldShowReplayButton()
        replayButton.text = "Play Again"
        homeButton.isVisible = true
        homeButton.text = "Home"
        leaderboardButton.text = "View Leaderboard"
    }

    private fun setupDuelResultUI(accuracy: Int) = with(binding) {
        duelResultCardView.isVisible = true
        teamResultCardView.isVisible = false
        resultScoreCardView.isVisible = false

        val isChallengeMode = gameMode.equals("challenge", ignoreCase = true)

        val resultStatus = when {
            correctCount > opponentCorrectCount -> "Victory!"
            correctCount < opponentCorrectCount -> "Defeat"
            else -> "Draw"
        }

        val subtitle = when {
            correctCount > opponentCorrectCount && isChallengeMode ->
                "You defeated $opponentName in this challenge"
            correctCount < opponentCorrectCount && isChallengeMode ->
                "$opponentName won this challenge"
            correctCount == opponentCorrectCount && isChallengeMode ->
                "You and $opponentName finished equally in this challenge"
            correctCount > opponentCorrectCount ->
                "You defeated $opponentName in this duel"
            correctCount < opponentCorrectCount ->
                "$opponentName won this duel"
            else ->
                "You and $opponentName finished equally"
        }

        resultTitleTextView.text = resultStatus
        resultSubtitleTextView.text = subtitle

        val titleColor = when (resultStatus) {
            "Victory!" -> R.color.color_success
            "Defeat" -> R.color.color_error
            else -> R.color.color_orange_primary
        }

        resultTitleTextView.setTextColor(
            ContextCompat.getColor(requireContext(), titleColor)
        )

        playerOneResultNameTextView.text = playerName
        playerTwoResultNameTextView.text = opponentName

        playerOneCorrectTextView.text = "Correct: $correctCount"
        playerTwoCorrectTextView.text = "Correct: $opponentCorrectCount"
        duelMyWrongCountTextView.text = wrongCount.toString()
        duelOpponentCorrectCountTextView.text = opponentCorrectCount.toString()
        duelAccuracyTextView.text = "$accuracy%"
        duelResultScoreTextView.text = "$correctCount - $opponentCorrectCount"

        duelResultStatusTextView.text = when {
            correctCount > opponentCorrectCount -> "WIN"
            correctCount < opponentCorrectCount -> "LOSE"
            else -> "DRAW"
        }

        val duelStatusColor = when {
            correctCount > opponentCorrectCount -> R.color.color_success
            correctCount < opponentCorrectCount -> R.color.color_error
            else -> R.color.color_orange_primary
        }

        duelResultStatusTextView.setTextColor(
            ContextCompat.getColor(requireContext(), duelStatusColor)
        )

        playerOneAvatarImageView.setImageResource(
            if (playerAvatarRes != 0) playerAvatarRes else R.drawable.ic_avatar_placeholder
        )
        playerTwoAvatarImageView.setImageResource(
            if (isBotMatch) {
                R.drawable.avatar_2
            } else {
                if (opponentAvatarRes != 0) opponentAvatarRes else R.drawable.ic_avatar_placeholder
            }
        )

        replayButton.isVisible = shouldShowReplayButton()
        replayButton.text = if (isChallengeMode) "Back to Profile" else "Find New Opponent"
        homeButton.isVisible = true
        homeButton.text = "Home"
        leaderboardButton.text = "Leaderboard"
    }

    private fun setupForfeitResultUI(accuracy: Int) = with(binding) {
        duelResultCardView.isVisible = true
        teamResultCardView.isVisible = false
        resultScoreCardView.isVisible = false

        val resolvedTitle = if (forfeitTitle.isBlank()) "Match Forfeited" else forfeitTitle
        val resolvedMessage = if (forfeitMessage.isBlank()) "You left the match" else forfeitMessage
        val resolvedWinnerMessage = if (forfeitWinnerMessage.isBlank()) {
            "Opponent wins by forfeit"
        } else {
            forfeitWinnerMessage
        }

        val resultStatusText = if (didCurrentPlayerForfeit) "FORFEIT" else "WIN"
        val resultStatusColor = if (didCurrentPlayerForfeit) {
            R.color.color_error
        } else {
            R.color.color_success
        }

        resultTitleTextView.text = resolvedTitle
        resultSubtitleTextView.text = "$resolvedMessage. $resolvedWinnerMessage"
        resultTitleTextView.setTextColor(
            ContextCompat.getColor(requireContext(), resultStatusColor)
        )

        playerOneResultNameTextView.text = playerName
        playerTwoResultNameTextView.text = opponentName

        playerOneCorrectTextView.text = "Correct: $correctCount"
        playerTwoCorrectTextView.text = "Correct: $opponentCorrectCount"
        duelMyWrongCountTextView.text = wrongCount.toString()
        duelOpponentCorrectCountTextView.text = opponentCorrectCount.toString()
        duelAccuracyTextView.text = "$accuracy%"
        duelResultScoreTextView.text = "$correctCount - $opponentCorrectCount"

        duelResultStatusTextView.text = resultStatusText
        duelResultStatusTextView.setTextColor(
            ContextCompat.getColor(requireContext(), resultStatusColor)
        )

        playerOneAvatarImageView.setImageResource(
            if (playerAvatarRes != 0) playerAvatarRes else R.drawable.ic_avatar_placeholder
        )
        playerTwoAvatarImageView.setImageResource(
            if (isBotMatch) {
                R.drawable.avatar_2
            } else {
                if (opponentAvatarRes != 0) opponentAvatarRes else R.drawable.ic_avatar_placeholder
            }
        )

        replayButton.isVisible = shouldShowReplayButton()
        replayButton.text =
            if (gameMode.equals("challenge", ignoreCase = true)) "Back to Profile" else "Find New Opponent"
        homeButton.isVisible = true
        homeButton.text = "Home"
        leaderboardButton.text = "Leaderboard"
    }

    private fun setupTeamBattleResultUI(accuracy: Int) = with(binding) {
        duelResultCardView.isVisible = false
        teamResultCardView.isVisible = true
        resultScoreCardView.isVisible = false

        val didPlayerTeamWin = when (playerTeamName) {
            "Team A" -> teamAScore > teamBScore
            "Team B" -> teamBScore > teamAScore
            else -> false
        }

        val isDraw = teamAScore == teamBScore

        resultTitleTextView.text = when {
            isDraw -> "Draw Match"
            didPlayerTeamWin -> "Victory!"
            else -> "Defeat"
        }

        resultSubtitleTextView.text = when {
            isDraw -> "Both teams finished equally"
            didPlayerTeamWin -> "$playerTeamName won the battle match"
            else -> "$playerTeamName lost the battle match"
        }

        val titleColor = when {
            isDraw -> R.color.color_orange_primary
            didPlayerTeamWin -> R.color.color_success
            else -> R.color.color_error
        }

        resultTitleTextView.setTextColor(
            ContextCompat.getColor(requireContext(), titleColor)
        )

        teamResultAScoreTextView.text = teamAScore.toString()
        teamResultBScoreTextView.text = teamBScore.toString()

        teamResultCenterStatusTextView.text = when {
            isDraw -> "DRAW"
            didPlayerTeamWin -> "WIN"
            else -> "LOSE"
        }

        val centerStatusColor = when {
            isDraw -> R.color.color_orange_primary
            didPlayerTeamWin -> R.color.color_success
            else -> R.color.color_error
        }

        teamResultCenterStatusTextView.setTextColor(
            ContextCompat.getColor(requireContext(), centerStatusColor)
        )

        topPerformerNameTextView.text = topPerformerName
        topPerformerStatsTextView.text = "Correct $topPerformerCorrect • Wrong $topPerformerWrong"
        topPerformerAvatarImageView.setImageResource(getAvatarForName(topPerformerName))
        topPerformerBadgeTextView.text = "MVP"

        teamYourCorrectCountTextView.text = correctCount.toString()
        teamYourWrongCountTextView.text = wrongCount.toString()
        teamYourAccuracyTextView.text = "$accuracy%"

        replayButton.isVisible = shouldShowReplayButton()
        replayButton.text = "Play Again"
        homeButton.isVisible = true
        homeButton.text = "Home"
        leaderboardButton.text = "View Leaderboard"
    }

    private fun getAvatarForName(name: String): Int {
        val currentUser = UserManager.currentUser

        return when {
            name.equals(currentUser.name, ignoreCase = true) -> {
                if (currentUser.avatarResId != 0) currentUser.avatarResId else R.drawable.ic_avatar_placeholder
            }

            name.equals("you", ignoreCase = true) -> {
                if (currentUser.avatarResId != 0) currentUser.avatarResId else R.drawable.ic_avatar_placeholder
            }

            name.equals("jessica", ignoreCase = true) -> R.drawable.avatar_2
            name.equals("alex", ignoreCase = true) -> R.drawable.avatar_3
            name.equals("sophia", ignoreCase = true) -> R.drawable.avatar_3
            name.equals("david", ignoreCase = true) -> R.drawable.avatar_4
            name.equals("daniel", ignoreCase = true) -> R.drawable.avatar_4
            name.equals("emma", ignoreCase = true) -> R.drawable.avatar_2
            name.equals("michael", ignoreCase = true) -> R.drawable.avatar_2
            else -> R.drawable.ic_avatar_placeholder
        }
    }

    private fun formatCategoryName(category: String): String {
        return when (category.lowercase()) {
            "informatics" -> "Informatics"
            "mathematics", "math" -> "Mathematics"
            "english" -> "English"
            "history" -> "History"
            "logic" -> "Logic"
            "world", "worldview", "world_knowledge" -> "World Knowledge"
            else -> "Quiz"
        }
    }

    private fun setBackgroundByCategory(category: String) {
        val backgroundRes = when (category.lowercase()) {
            "informatics" -> R.drawable.bg_informatics_1
            "mathematics", "math" -> R.drawable.bg_math_1
            "english" -> R.drawable.bg_english_1
            "history" -> R.drawable.bg_history_1
            "logic" -> R.drawable.bg_logic_1
            "world", "worldview", "world_knowledge" -> R.drawable.bg_world_1
            else -> R.drawable.bg_informatics_1
        }

        binding.resultBackgroundImageView.setImageResource(backgroundRes)
    }

    private fun setupPressedEffects() = with(binding) {
        addPressedEffect(replayButton)
        addPressedEffect(leaderboardButton)
        addPressedEffect(homeButton)
        addPressedEffect(reviewAnswersButton)
    }

    private fun setupClickListeners() = with(binding) {
        replayButton.setOnClickListener {
            when {
                isMultiplayerTeamResult() -> {
                    findNavController().navigate(R.id.action_resultFragment_to_homeFragment)
                }

                isTeamBattleMode -> {
                    findNavController().navigate(R.id.action_resultFragment_to_homeFragment)
                }

                isOnlineDuel && gameMode.equals("challenge", ignoreCase = true) -> {
                    findNavController().navigate(R.id.action_resultFragment_to_profileFragment)
                }

                isOnlineDuel -> {
                    val bundle = Bundle().apply {
                        putString("category", categoryName.lowercase())
                        putString("difficulty", difficultyName)
                        putInt("questionCount", questionCount)
                    }

                    findNavController().navigate(
                        R.id.singlePlayerMatchmakingFragment,
                        bundle
                    )
                }

                else -> {
                    val bundle = Bundle().apply {
                        putString("category", categoryName.lowercase())
                        putString("difficulty", difficultyName)
                        putInt("questionCount", questionCount)
                    }

                    findNavController().navigate(
                        R.id.action_resultFragment_to_quizGameFragment,
                        bundle
                    )
                }
            }
        }

        leaderboardButton.setOnClickListener {
            findNavController().navigate(R.id.action_resultFragment_to_leaderFragment)
        }

        homeButton.setOnClickListener {
            findNavController().navigate(R.id.action_resultFragment_to_homeFragment)
        }

        reviewAnswersButton.setOnClickListener {
            findNavController().navigate(R.id.action_resultFragment_to_reviewAnswersFragment)
        }
    }

    private fun addPressedEffect(targetView: View) {
        targetView.isClickable = true
        targetView.isFocusable = true

        targetView.setOnTouchListener { view, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    if (view.isEnabled) {
                        view.animate()
                            .scaleX(0.98f)
                            .scaleY(0.98f)
                            .alpha(0.90f)
                            .setDuration(90)
                            .start()
                    }
                }

                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    view.animate()
                        .scaleX(1f)
                        .scaleY(1f)
                        .alpha(if (view.isEnabled) 1f else 0.55f)
                        .setDuration(90)
                        .start()
                }
            }
            false
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}