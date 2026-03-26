package com.example.quizapp.ui

import android.graphics.Color
import android.os.Bundle
import android.os.CountDownTimer
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.addCallback
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.quizapp.R
import com.example.quizapp.manager.FavoriteManager
import com.example.quizapp.manager.ReviewManager
import com.example.quizapp.manager.UserManager
import com.example.quizapp.model.AnswerReviewItem
import com.example.quizapp.model.FavoriteQuestion
import com.example.quizapp.model.RoomPlayer
import com.google.android.material.card.MaterialCardView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.imageview.ShapeableImageView
import com.google.android.material.progressindicator.LinearProgressIndicator

class QuizGameFragment : Fragment(R.layout.fragment_quiz_game) {

    private lateinit var quizBackgroundImageView: ImageView
    private lateinit var backImageView: ImageView
    private lateinit var bookmarkQuestionImageView: ImageView

    private lateinit var quizCategoryTextView: TextView
    private lateinit var questionCountTextView: TextView
    private lateinit var timerTextView: TextView
    private lateinit var questionTextView: TextView
    private lateinit var questionImageView: ImageView

    private lateinit var questionProgressIndicator: LinearProgressIndicator

    private lateinit var optionACardView: MaterialCardView
    private lateinit var optionBCardView: MaterialCardView
    private lateinit var optionCCardView: MaterialCardView
    private lateinit var optionDCardView: MaterialCardView

    private lateinit var optionATextView: TextView
    private lateinit var optionBTextView: TextView
    private lateinit var optionCTextView: TextView
    private lateinit var optionDTextView: TextView

    private lateinit var help5050CardView: MaterialCardView
    private lateinit var helpSkipCardView: MaterialCardView
    private lateinit var helpExtraTimeCardView: MaterialCardView
    private lateinit var helpersTitleTextView: TextView
    private lateinit var helpersContainer: LinearLayout
    private lateinit var timerContainer: LinearLayout

    private lateinit var teamHeaderCardView: MaterialCardView
    private lateinit var teamAQuizCardView: MaterialCardView
    private lateinit var teamAQuizTitleTextView: TextView
    private lateinit var teamAQuizCountTextView: TextView
    private lateinit var teamAStatusTextView: TextView
    private lateinit var teamATopPlayerAvatarImageView: ShapeableImageView
    private lateinit var teamATopPlayerNameTextView: TextView
    private lateinit var teamATopPlayerStatsTextView: TextView
    private lateinit var teamATopPlayerCorrectCountTextView: TextView
    private lateinit var teamAQuizScoreTextView: TextView
    private lateinit var teamAExpandImageView: ImageView

    private lateinit var teamBQuizCardView: MaterialCardView
    private lateinit var teamBQuizTitleTextView: TextView
    private lateinit var teamBQuizCountTextView: TextView
    private lateinit var teamBStatusTextView: TextView
    private lateinit var teamBTopPlayerAvatarImageView: ShapeableImageView
    private lateinit var teamBTopPlayerNameTextView: TextView
    private lateinit var teamBTopPlayerStatsTextView: TextView
    private lateinit var teamBTopPlayerCorrectCountTextView: TextView
    private lateinit var teamBQuizScoreTextView: TextView
    private lateinit var teamBExpandImageView: ImageView
    private lateinit var teamTimerTextView: TextView

    private lateinit var duelHeaderCardView: MaterialCardView
    private lateinit var duelModeLabelTextView: TextView
    private lateinit var playerAvatarImageView: ShapeableImageView
    private lateinit var playerNameTextView: TextView
    private lateinit var playerSubLabelTextView: TextView
    private lateinit var playerScoreMiniTextView: TextView
    private lateinit var opponentAvatarImageView: ShapeableImageView
    private lateinit var opponentNameTextView: TextView
    private lateinit var opponentSubLabelTextView: TextView
    private lateinit var opponentScoreMiniTextView: TextView
    private lateinit var duelTimerTextView: TextView

    private lateinit var selectedTeamDetailsCardView: MaterialCardView
    private lateinit var selectedTeamTitleTextView: TextView
    private lateinit var selectedTeamCloseImageView: ImageView
    private lateinit var selectedTeamStatusTextView: TextView
    private lateinit var selectedTeamCorrectTextView: TextView
    private lateinit var selectedTeamWrongTextView: TextView
    private lateinit var selectedTeamTopPerformerAvatarImageView: ShapeableImageView
    private lateinit var selectedTeamTopPerformerNameTextView: TextView
    private lateinit var selectedTeamTopPerformerStatsTextView: TextView
    private lateinit var selectedTeamTopPerformerBadgeTextView: TextView
    private lateinit var selectedTeamPlayersRecyclerView: RecyclerView
    private lateinit var selectedTeamPlayersAdapter: TeamDetailsPlayerAdapter

    private val selectedTeamPlayers = mutableListOf<RoomPlayer>()
    private val teamAPlayers = mutableListOf<RoomPlayer>()
    private val teamBPlayers = mutableListOf<RoomPlayer>()

    private var duelPlayerScore = 0
    private var duelOpponentScore = 0
    private var duelOpponentName = "Opponent"
    private var duelOpponentId = ""
    private var duelOpponentAvatarRes = R.drawable.ic_avatar_placeholder
    private var gameMode = "single"

    private var currentQuestionIndex = 0
    private var score = 0
    private var wrongCount = 0

    private var is5050Used = false
    private var isSkipUsed = false
    private var isExtraTimeUsed = false
    private var isAnswerLocked = false
    private var isQuizFinished = false

    private var countDownTimer: CountDownTimer? = null
    private var teamCountDownTimer: CountDownTimer? = null

    private var timeLeftInMillis: Long = 20_000L
    private var teamTimeLeftInMillis: Long = 60_000L

    private val answeredQuestions = mutableListOf<AnswerReviewItem>()

    private var roomId: String = ""
    private var selectedTeam: String = ""
    private var selectedTeamFormat: String = "4v4"
    private var isMultiplayer: Boolean = false
    private var isOnlineDuelMode: Boolean = false
    private var isTeamBattleMode: Boolean = false

    private var expandedTeamKey: String? = null

    private val currentCategory: String by lazy {
        arguments?.getString("selectedCategory")
            ?.trim()
            ?.ifBlank {
                arguments?.getString("category")
                    ?.trim()
                    ?.ifBlank { "informatics" }
                    ?: "informatics"
            }
            ?: arguments?.getString("category")
                ?.trim()
                ?.ifBlank { "informatics" }
            ?: "informatics"
    }

    private val currentDifficulty: String by lazy {
        arguments?.getString("difficulty")
            ?.trim()
            ?.ifBlank { "easy" }
            ?: "easy"
    }

    private val selectedQuestionCount: Int by lazy {
        arguments?.getInt("questionCount", 10) ?: 10
    }

    private val questions: MutableList<QuestionItem> by lazy {
        val allQuestions = getQuestionsByCategory(currentCategory).shuffled()
        allQuestions.take(minOf(selectedQuestionCount, allQuestions.size)).toMutableList()
    }

    private fun safeCurrentUserName(): String {
        val name = UserManager.currentUser.name.trim()
        return if (name.isNotBlank()) name else "Player"
    }

    private fun safeCurrentUserId(): String {
        val id = UserManager.currentUser.userId.trim()
        return if (id.isNotBlank()) id else "AEL-00000"
    }

    private fun safeCurrentUserAvatar(): Int {
        val avatar = UserManager.currentUser.avatarResId
        return if (avatar != 0) avatar else R.drawable.ic_avatar_placeholder
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        UserManager.loadUser(requireContext())

        readGameArguments()
        initViews(view)
        setupRecyclerView()
        setupBackNavigation()

        when {
            isTeamBattleMode -> setupDummyTeamData()
            isOnlineDuelMode -> setupDummyDuelData()
        }

        setupCategoryTitle()
        setRandomBackground()
        setupGameModeUI()
        setupPressedEffects()
        setupClickListeners()
        loadQuestion()

        if (isTeamBattleMode) {
            startTeamTimer()
        }

        UserManager.userLiveData.observe(viewLifecycleOwner) {
            if (!isAdded) return@observe
            if (isOnlineDuelMode) {
                updateDuelSummaryUi()
            }
            if (isTeamBattleMode) {
                updateTeamSummaryUi()
                if (expandedTeamKey == "A") {
                    showSelectedTeamDetails(
                        teamName = "Team A",
                        players = teamAPlayers,
                        accentColor = Color.parseColor("#FF8A34"),
                        teamKey = "A"
                    )
                } else if (expandedTeamKey == "B") {
                    showSelectedTeamDetails(
                        teamName = "Team B",
                        players = teamBPlayers,
                        accentColor = Color.parseColor("#7B61FF"),
                        teamKey = "B"
                    )
                }
            }
        }
    }

    private fun setupBackNavigation() {
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner) {
            when {
                shouldProtectMatchExit() && !isQuizFinished -> {
                    showLeaveMatchDialog()
                }

                !isQuizFinished -> {
                    findNavController().navigate(R.id.action_quizGameFragment_to_homeFragment)
                }

                else -> {
                    findNavController().navigateUp()
                }
            }
        }
    }

    private fun shouldProtectMatchExit(): Boolean {
        return isOnlineDuelMode
    }

    private fun showLeaveMatchDialog() {
        if (!isAdded) return

        val dialogTitle: String
        val dialogMessage: String

        when {
            isTeamBattleMode && isMultiplayer -> {
                dialogTitle = "Leave Team Match?"
                dialogMessage = "If you leave now, your team will lose this match."
            }

            isTeamBattleMode -> {
                dialogTitle = "Leave Team Match?"
                dialogMessage = "If you leave now, your team will forfeit this match."
            }

            isOnlineDuelMode -> {
                dialogTitle = "Leave Match?"
                dialogMessage =
                    "If you leave now, the match will be counted as a forfeit and your opponent will win."
            }

            isMultiplayer -> {
                dialogTitle = "Leave Match?"
                dialogMessage = "If you leave now, your opponent will win by forfeit."
            }

            else -> {
                dialogTitle = "Leave Match?"
                dialogMessage = "If you leave now, this match will be counted as a loss."
            }
        }

        MaterialAlertDialogBuilder(requireContext())
            .setTitle(dialogTitle)
            .setMessage(dialogMessage)
            .setCancelable(true)
            .setNegativeButton("Continue Match") { dialog, _ ->
                dialog.dismiss()
            }
            .setPositiveButton("Leave Match") { dialog, _ ->
                dialog.dismiss()
                navigateToForfeitResult()
            }
            .show()
    }

    private fun navigateToForfeitResult() {
        if (isQuizFinished) return
        isQuizFinished = true

        countDownTimer?.cancel()
        teamCountDownTimer?.cancel()

        ReviewManager.reviewList.clear()
        ReviewManager.reviewList.addAll(answeredQuestions)

        val safeUserName = safeCurrentUserName()
        val safeUserId = safeCurrentUserId()
        val safeUserAvatar = safeCurrentUserAvatar()

        val correctCount = score
        val finalScore = correctCount * 10
        val playerTeamName = if (selectedTeam == "B") "Team B" else "Team A"

        if (isTeamBattleMode) {
            val currentTeamCorrect = if (selectedTeam == "B") {
                teamBPlayers.sumOf { it.correctCount }
            } else {
                teamAPlayers.sumOf { it.correctCount }
            }

            val opponentTeamCorrect = if (selectedTeam == "B") {
                teamAPlayers.sumOf { it.correctCount }
            } else {
                teamBPlayers.sumOf { it.correctCount }
            }

            val losingTeamScore = currentTeamCorrect
            val winningTeamScore = maxOf(opponentTeamCorrect, currentTeamCorrect + 1)

            val teamAScoreValue = if (selectedTeam == "A") losingTeamScore else winningTeamScore
            val teamBScoreValue = if (selectedTeam == "B") losingTeamScore else winningTeamScore

            val allPlayers = teamAPlayers + teamBPlayers
            val topPerformer = allPlayers.maxWithOrNull(
                compareByDescending<RoomPlayer> { it.correctCount }
                    .thenBy { it.wrongCount }
            )

            val bundle = Bundle().apply {
                putInt("score", finalScore)
                putInt("correct", correctCount)
                putInt("wrong", wrongCount)
                putString("category", currentCategory)
                putString("difficulty", currentDifficulty)
                putInt("questionCount", questions.size)

                putString("gameMode", gameMode)

                putBoolean("isTeamBattleMode", true)
                putBoolean("isMultiplayer", isMultiplayer)
                putBoolean("isOnlineDuel", false)

                putBoolean("isForfeitResult", true)
                putBoolean("didCurrentPlayerForfeit", true)

                putString("roomId", roomId)
                putString("selectedTeam", selectedTeam)
                putString("teamFormat", selectedTeamFormat)

                putString("opponentName", duelOpponentName)
                putString("opponentId", duelOpponentId)
                putInt("opponentAvatar", duelOpponentAvatarRes)
                putInt("opponentCorrect", duelOpponentScore / 10)

                putString("playerName", safeUserName)
                putString("playerId", safeUserId)
                putInt("playerAvatar", safeUserAvatar)

                putInt("teamAScore", teamAScoreValue)
                putInt("teamBScore", teamBScoreValue)
                putString("playerTeamName", playerTeamName)
                putString("topPerformerName", topPerformer?.name ?: safeUserName)
                putInt("topPerformerCorrect", topPerformer?.correctCount ?: correctCount)
                putInt("topPerformerWrong", topPerformer?.wrongCount ?: wrongCount)

                putString("forfeitTitle", "Match Forfeited")
                putString("forfeitMessage", "You left the team match")
                putString("forfeitWinnerMessage", "The opposing team wins by forfeit")
            }

            findNavController().navigate(
                R.id.action_quizGameFragment_to_resultFragment,
                bundle
            )
            return
        }

        val opponentCorrectCount = duelOpponentScore / 10

        val bundle = Bundle().apply {
            putInt("score", finalScore)
            putInt("correct", correctCount)
            putInt("wrong", wrongCount)
            putString("category", currentCategory)
            putString("difficulty", currentDifficulty)
            putInt("questionCount", questions.size)

            putString("gameMode", gameMode)

            putBoolean("isTeamBattleMode", false)
            putBoolean("isMultiplayer", isMultiplayer)
            putBoolean("isOnlineDuel", true)

            putBoolean("isForfeitResult", true)
            putBoolean("didCurrentPlayerForfeit", true)

            putString("roomId", roomId)
            putString("selectedTeam", selectedTeam)
            putString("teamFormat", selectedTeamFormat)

            putString("opponentName", duelOpponentName)
            putString("opponentId", duelOpponentId)
            putInt("opponentAvatar", duelOpponentAvatarRes)
            putInt("opponentCorrect", opponentCorrectCount)

            putString("playerName", safeUserName)
            putString("playerId", safeUserId)
            putInt("playerAvatar", safeUserAvatar)

            putString("forfeitTitle", "Match Forfeited")
            putString("forfeitMessage", "You left the match")
            putString("forfeitWinnerMessage", "Opponent wins by forfeit")
        }

        findNavController().navigate(
            R.id.action_quizGameFragment_to_resultFragment,
            bundle
        )
    }

    private fun readGameArguments() {
        val bundle = arguments

        gameMode = bundle?.getString("gameMode")
            ?.trim()
            ?.ifBlank { "single" }
            ?: "single"

        roomId = bundle?.getString("roomId")
            ?.trim()
            .orEmpty()

        selectedTeam = bundle?.getString("selectedTeam")
            ?.trim()
            ?.uppercase()
            .orEmpty()

        if (selectedTeam.isBlank()) {
            selectedTeam = "A"
        }

        selectedTeamFormat = bundle?.getString("teamFormat", "4v4")
            ?.trim()
            ?.ifBlank { "4v4" }
            ?: "4v4"

        isMultiplayer = bundle?.getBoolean("isMultiplayer", false) ?: false
        isTeamBattleMode = bundle?.getBoolean("isTeamBattleMode", false) ?: false

        duelOpponentName = bundle?.getString("opponentName")
            ?.trim()
            ?.ifBlank { "Opponent" }
            ?: "Opponent"

        duelOpponentId = bundle?.getString("opponentId")
            ?.trim()
            .orEmpty()

        duelOpponentAvatarRes = bundle?.getInt(
            "opponentAvatar",
            R.drawable.ic_avatar_placeholder
        ) ?: R.drawable.ic_avatar_placeholder

        if (duelOpponentAvatarRes == 0) {
            duelOpponentAvatarRes = R.drawable.ic_avatar_placeholder
        }

        val explicitOnlineDuel = bundle?.getBoolean("isOnlineDuel", false) ?: false
        val isChallengeMode = gameMode.equals("challenge", ignoreCase = true)

        isOnlineDuelMode = (explicitOnlineDuel || isChallengeMode) && !isTeamBattleMode
    }

    private fun getRequiredPlayersPerTeam(): Int {
        return when (selectedTeamFormat.lowercase()) {
            "2v2" -> 2
            "3v3" -> 3
            "4v4" -> 4
            else -> 2
        }
    }

    private fun initViews(view: View) {
        quizBackgroundImageView = view.findViewById(R.id.quizBackgroundImageView)
        backImageView = view.findViewById(R.id.backImageView)
        bookmarkQuestionImageView = view.findViewById(R.id.bookmarkQuestionImageView)

        quizCategoryTextView = view.findViewById(R.id.quizCategoryTextView)
        questionCountTextView = view.findViewById(R.id.questionCountTextView)
        timerTextView = view.findViewById(R.id.timerTextView)
        questionTextView = view.findViewById(R.id.questionTextView)
        questionImageView = view.findViewById(R.id.questionImageView)

        questionProgressIndicator = view.findViewById(R.id.questionProgressIndicator)

        optionACardView = view.findViewById(R.id.optionACardView)
        optionBCardView = view.findViewById(R.id.optionBCardView)
        optionCCardView = view.findViewById(R.id.optionCCardView)
        optionDCardView = view.findViewById(R.id.optionDCardView)

        optionATextView = view.findViewById(R.id.optionATextView)
        optionBTextView = view.findViewById(R.id.optionBTextView)
        optionCTextView = view.findViewById(R.id.optionCTextView)
        optionDTextView = view.findViewById(R.id.optionDTextView)

        help5050CardView = view.findViewById(R.id.help5050CardView)
        helpSkipCardView = view.findViewById(R.id.helpSkipCardView)
        helpExtraTimeCardView = view.findViewById(R.id.helpExtraTimeCardView)
        helpersTitleTextView = view.findViewById(R.id.helpersTitleTextView)
        helpersContainer = view.findViewById(R.id.helpersContainer)
        timerContainer = view.findViewById(R.id.timerContainer)

        teamHeaderCardView = view.findViewById(R.id.teamHeaderCardView)
        teamAQuizCardView = view.findViewById(R.id.teamAQuizCardView)
        teamAQuizTitleTextView = view.findViewById(R.id.teamAQuizTitleTextView)
        teamAQuizCountTextView = view.findViewById(R.id.teamAQuizCountTextView)
        teamAStatusTextView = view.findViewById(R.id.teamAStatusTextView)
        teamATopPlayerAvatarImageView = view.findViewById(R.id.teamATopPlayerAvatarImageView)
        teamATopPlayerNameTextView = view.findViewById(R.id.teamATopPlayerNameTextView)
        teamATopPlayerStatsTextView = view.findViewById(R.id.teamATopPlayerStatsTextView)
        teamATopPlayerCorrectCountTextView = view.findViewById(R.id.teamATopPlayerCorrectCountTextView)
        teamAQuizScoreTextView = view.findViewById(R.id.teamAQuizScoreTextView)
        teamAExpandImageView = view.findViewById(R.id.teamAExpandImageView)

        teamBQuizCardView = view.findViewById(R.id.teamBQuizCardView)
        teamBQuizTitleTextView = view.findViewById(R.id.teamBQuizTitleTextView)
        teamBQuizCountTextView = view.findViewById(R.id.teamBQuizCountTextView)
        teamBStatusTextView = view.findViewById(R.id.teamBStatusTextView)
        teamBTopPlayerAvatarImageView = view.findViewById(R.id.teamBTopPlayerAvatarImageView)
        teamBTopPlayerNameTextView = view.findViewById(R.id.teamBTopPlayerNameTextView)
        teamBTopPlayerStatsTextView = view.findViewById(R.id.teamBTopPlayerStatsTextView)
        teamBTopPlayerCorrectCountTextView = view.findViewById(R.id.teamBTopPlayerCorrectCountTextView)
        teamBQuizScoreTextView = view.findViewById(R.id.teamBQuizScoreTextView)
        teamBExpandImageView = view.findViewById(R.id.teamBExpandImageView)
        teamTimerTextView = view.findViewById(R.id.teamTimerTextView)

        duelHeaderCardView = view.findViewById(R.id.duelHeaderCardView)
        duelModeLabelTextView = view.findViewById(R.id.duelModeLabelTextView)
        playerAvatarImageView = view.findViewById(R.id.playerAvatarImageView)
        playerNameTextView = view.findViewById(R.id.playerNameTextView)
        playerSubLabelTextView = view.findViewById(R.id.playerSubLabelTextView)
        playerScoreMiniTextView = view.findViewById(R.id.playerScoreMiniTextView)
        opponentAvatarImageView = view.findViewById(R.id.opponentAvatarImageView)
        opponentNameTextView = view.findViewById(R.id.opponentNameTextView)
        opponentSubLabelTextView = view.findViewById(R.id.opponentSubLabelTextView)
        opponentScoreMiniTextView = view.findViewById(R.id.opponentScoreMiniTextView)
        duelTimerTextView = view.findViewById(R.id.duelTimerTextView)

        selectedTeamDetailsCardView = view.findViewById(R.id.selectedTeamDetailsCardView)
        selectedTeamTitleTextView = view.findViewById(R.id.selectedTeamTitleTextView)
        selectedTeamCloseImageView = view.findViewById(R.id.selectedTeamCloseImageView)
        selectedTeamStatusTextView = view.findViewById(R.id.selectedTeamStatusTextView)
        selectedTeamCorrectTextView = view.findViewById(R.id.selectedTeamCorrectTextView)
        selectedTeamWrongTextView = view.findViewById(R.id.selectedTeamWrongTextView)
        selectedTeamTopPerformerAvatarImageView =
            view.findViewById(R.id.selectedTeamTopPerformerAvatarImageView)
        selectedTeamTopPerformerNameTextView =
            view.findViewById(R.id.selectedTeamTopPerformerNameTextView)
        selectedTeamTopPerformerStatsTextView =
            view.findViewById(R.id.selectedTeamTopPerformerStatsTextView)
        selectedTeamTopPerformerBadgeTextView =
            view.findViewById(R.id.selectedTeamTopPerformerBadgeTextView)
        selectedTeamPlayersRecyclerView = view.findViewById(R.id.selectedTeamPlayersRecyclerView)
    }

    private fun setupRecyclerView() {
        selectedTeamPlayersAdapter = TeamDetailsPlayerAdapter(selectedTeamPlayers)
        selectedTeamPlayersRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = selectedTeamPlayersAdapter
            isNestedScrollingEnabled = false
        }
    }

    private fun setupPressedEffects() {
        addPressedEffect(backImageView)
        addPressedEffect(bookmarkQuestionImageView)

        addPressedEffect(optionACardView)
        addPressedEffect(optionBCardView)
        addPressedEffect(optionCCardView)
        addPressedEffect(optionDCardView)

        addPressedEffect(help5050CardView)
        addPressedEffect(helpSkipCardView)
        addPressedEffect(helpExtraTimeCardView)

        addPressedEffect(teamAQuizCardView)
        addPressedEffect(teamBQuizCardView)
        addPressedEffect(teamAExpandImageView)
        addPressedEffect(teamBExpandImageView)
        addPressedEffect(selectedTeamCloseImageView)
    }

    private fun setupDummyDuelData() {
        val safeName = safeCurrentUserName()
        val safeAvatar = safeCurrentUserAvatar()

        duelPlayerScore = 0
        duelOpponentScore = 0

        playerNameTextView.text = safeName
        playerSubLabelTextView.text =
            if (gameMode.equals("challenge", true)) "Challenger" else "Online"
        playerScoreMiniTextView.text = "Score: 0"
        playerAvatarImageView.setImageResource(safeAvatar)

        opponentNameTextView.text = duelOpponentName
        opponentSubLabelTextView.text =
            if (duelOpponentId.isNotBlank()) duelOpponentId else "Opponent"
        opponentScoreMiniTextView.text = "Score: 0"
        opponentAvatarImageView.setImageResource(
            if (duelOpponentAvatarRes != 0) duelOpponentAvatarRes else R.drawable.ic_avatar_placeholder
        )

        duelModeLabelTextView.text =
            if (gameMode.equals("challenge", true)) "Challenge Duel" else "Online Duel"
        duelTimerTextView.text = "20s"
    }

    private fun setupDummyTeamData() {
        val safeUserId = safeCurrentUserId()
        val safeUserName = safeCurrentUserName()

        teamAPlayers.clear()
        teamBPlayers.clear()

        val requiredPlayers = getRequiredPlayersPerTeam()

        if (isMultiplayer) {
            if (selectedTeam == "A") {
                teamAPlayers.add(
                    RoomPlayer(
                        uid = safeUserId,
                        name = safeUserName,
                        avatarUrl = "",
                        team = "A",
                        isBot = false,
                        correctCount = 0,
                        wrongCount = 0,
                        score = 0,
                        joinedAt = System.currentTimeMillis(),
                        isReady = true
                    )
                )

                if (requiredPlayers >= 2) {
                    teamAPlayers.add(RoomPlayer("user_a_2", "Alex", "", "A", false, 0, 0, 0, System.currentTimeMillis(), true))
                }
                if (requiredPlayers >= 3) {
                    teamAPlayers.add(RoomPlayer("user_a_3", "Michael", "", "A", false, 0, 0, 0, System.currentTimeMillis(), true))
                }
                if (requiredPlayers >= 4) {
                    teamAPlayers.add(RoomPlayer("user_a_4", "Sophia", "", "A", false, 0, 0, 0, System.currentTimeMillis(), true))
                }

                teamBPlayers.add(RoomPlayer("user_b_1", "Jessica", "", "B", false, 0, 0, 0, System.currentTimeMillis(), true))
                if (requiredPlayers >= 2) {
                    teamBPlayers.add(RoomPlayer("user_b_2", "David", "", "B", false, 0, 0, 0, System.currentTimeMillis(), true))
                }
                if (requiredPlayers >= 3) {
                    teamBPlayers.add(RoomPlayer("user_b_3", "Emma", "", "B", false, 0, 0, 0, System.currentTimeMillis(), true))
                }
                if (requiredPlayers >= 4) {
                    teamBPlayers.add(RoomPlayer("user_b_4", "Daniel", "", "B", false, 0, 0, 0, System.currentTimeMillis(), true))
                }
            } else {
                teamBPlayers.add(
                    RoomPlayer(
                        uid = safeUserId,
                        name = safeUserName,
                        avatarUrl = "",
                        team = "B",
                        isBot = false,
                        correctCount = 0,
                        wrongCount = 0,
                        score = 0,
                        joinedAt = System.currentTimeMillis(),
                        isReady = true
                    )
                )

                if (requiredPlayers >= 2) {
                    teamBPlayers.add(RoomPlayer("user_b_2", "Jessica", "", "B", false, 0, 0, 0, System.currentTimeMillis(), true))
                }
                if (requiredPlayers >= 3) {
                    teamBPlayers.add(RoomPlayer("user_b_3", "Emma", "", "B", false, 0, 0, 0, System.currentTimeMillis(), true))
                }
                if (requiredPlayers >= 4) {
                    teamBPlayers.add(RoomPlayer("user_b_4", "Daniel", "", "B", false, 0, 0, 0, System.currentTimeMillis(), true))
                }

                teamAPlayers.add(RoomPlayer("user_a_1", "Alex", "", "A", false, 0, 0, 0, System.currentTimeMillis(), true))
                if (requiredPlayers >= 2) {
                    teamAPlayers.add(RoomPlayer("user_a_2", "David", "", "A", false, 0, 0, 0, System.currentTimeMillis(), true))
                }
                if (requiredPlayers >= 3) {
                    teamAPlayers.add(RoomPlayer("user_a_3", "Michael", "", "A", false, 0, 0, 0, System.currentTimeMillis(), true))
                }
                if (requiredPlayers >= 4) {
                    teamAPlayers.add(RoomPlayer("user_a_4", "Sophia", "", "A", false, 0, 0, 0, System.currentTimeMillis(), true))
                }
            }
            return
        }

        teamAPlayers.add(
            RoomPlayer(
                uid = safeUserId,
                name = safeUserName,
                avatarUrl = "",
                team = "A",
                isBot = false,
                correctCount = 4,
                wrongCount = 1,
                score = 1250,
                joinedAt = System.currentTimeMillis(),
                isReady = true
            )
        )
        if (requiredPlayers >= 2) {
            teamAPlayers.add(RoomPlayer("user_a_2", "Alex", "", "A", false, 2, 3, 1090, System.currentTimeMillis(), true))
        }
        if (requiredPlayers >= 3) {
            teamAPlayers.add(RoomPlayer("user_a_3", "Michael", "", "A", false, 3, 2, 1160, System.currentTimeMillis(), true))
        }
        if (requiredPlayers >= 4) {
            teamAPlayers.add(RoomPlayer("user_a_4", "Sophia", "", "A", false, 1, 1, 1010, System.currentTimeMillis(), true))
        }

        teamBPlayers.add(RoomPlayer("user_b_1", "Jessica", "", "B", false, 3, 2, 1180, System.currentTimeMillis(), true))
        if (requiredPlayers >= 2) {
            teamBPlayers.add(RoomPlayer("user_b_2", "David", "", "B", false, 1, 4, 1115, System.currentTimeMillis(), true))
        }
        if (requiredPlayers >= 3) {
            teamBPlayers.add(RoomPlayer("user_b_3", "Emma", "", "B", false, 2, 2, 1135, System.currentTimeMillis(), true))
        }
        if (requiredPlayers >= 4) {
            teamBPlayers.add(RoomPlayer("user_b_4", "Daniel", "", "B", false, 2, 3, 1175, System.currentTimeMillis(), true))
        }
    }

    private fun setupGameModeUI() {
        when {
            isTeamBattleMode -> {
                teamHeaderCardView.isVisible = true
                duelHeaderCardView.isVisible = false
                selectedTeamDetailsCardView.isVisible = false

                timerContainer.isVisible = false
                helpersTitleTextView.isVisible = false
                helpersContainer.isVisible = false
                bookmarkQuestionImageView.isVisible = false

                updateTeamSummaryUi()
                updateTeamExpandArrows(null)
            }

            isOnlineDuelMode -> {
                teamHeaderCardView.isVisible = false
                duelHeaderCardView.isVisible = true
                selectedTeamDetailsCardView.isVisible = false

                timerContainer.isVisible = false
                helpersTitleTextView.isVisible = false
                helpersContainer.isVisible = false
                bookmarkQuestionImageView.isVisible = false

                updateDuelSummaryUi()
            }

            else -> {
                teamHeaderCardView.isVisible = false
                duelHeaderCardView.isVisible = false
                selectedTeamDetailsCardView.isVisible = false

                timerContainer.isVisible = true
                helpersTitleTextView.isVisible = true
                helpersContainer.isVisible = true
                bookmarkQuestionImageView.isVisible = true
            }
        }
    }

    private fun updateDuelSummaryUi() {
        playerNameTextView.text = safeCurrentUserName()
        playerSubLabelTextView.text =
            if (gameMode.equals("challenge", true)) "Challenger" else "Online"
        playerAvatarImageView.setImageResource(safeCurrentUserAvatar())
        playerScoreMiniTextView.text = "Score: $duelPlayerScore"

        opponentNameTextView.text = duelOpponentName
        opponentSubLabelTextView.text =
            if (duelOpponentId.isNotBlank()) duelOpponentId else "Opponent"
        opponentAvatarImageView.setImageResource(
            if (duelOpponentAvatarRes != 0) duelOpponentAvatarRes else R.drawable.ic_avatar_placeholder
        )
        opponentScoreMiniTextView.text = "Score: $duelOpponentScore"
    }

    private fun updateTeamSummaryUi() {
        val teamACorrect = teamAPlayers.sumOf { it.correctCount }
        val teamBCorrect = teamBPlayers.sumOf { it.correctCount }
        val requiredPlayers = getRequiredPlayersPerTeam()

        teamAQuizTitleTextView.text = "Team A"
        teamBQuizTitleTextView.text = "Team B"

        teamAQuizCountTextView.text = "${teamAPlayers.size}/$requiredPlayers"
        teamBQuizCountTextView.text = "${teamBPlayers.size}/$requiredPlayers"

        teamAQuizScoreTextView.text = "Score: $teamACorrect"
        teamBQuizScoreTextView.text = "Score: $teamBCorrect"

        teamAStatusTextView.text = when {
            teamACorrect > teamBCorrect -> "Leading team"
            teamACorrect < teamBCorrect -> "Behind by ${teamBCorrect - teamACorrect}"
            else -> "Tie"
        }

        teamBStatusTextView.text = when {
            teamBCorrect > teamACorrect -> "Leading team"
            teamBCorrect < teamACorrect -> "Behind by ${teamACorrect - teamBCorrect}"
            else -> "Tie"
        }

        getTopPlayer(teamAPlayers)?.let {
            teamATopPlayerNameTextView.text = it.name
            teamATopPlayerStatsTextView.text = "Correct ${it.correctCount} • Wrong ${it.wrongCount}"
            teamATopPlayerCorrectCountTextView.text = it.correctCount.toString()
            teamATopPlayerAvatarImageView.setImageResource(getAvatarForPlayer(it))
        }

        getTopPlayer(teamBPlayers)?.let {
            teamBTopPlayerNameTextView.text = it.name
            teamBTopPlayerStatsTextView.text = "Correct ${it.correctCount} • Wrong ${it.wrongCount}"
            teamBTopPlayerCorrectCountTextView.text = it.correctCount.toString()
            teamBTopPlayerAvatarImageView.setImageResource(getAvatarForPlayer(it))
        }
    }

    private fun showSelectedTeamDetails(
        teamName: String,
        players: List<RoomPlayer>,
        accentColor: Int,
        teamKey: String
    ) {
        selectedTeamDetailsCardView.isVisible = true
        expandedTeamKey = teamKey
        updateTeamExpandArrows(teamKey)

        val totalCorrect = players.sumOf { it.correctCount }
        val totalWrong = players.sumOf { it.wrongCount }
        val topPlayer = getTopPlayer(players)

        selectedTeamTitleTextView.text = teamName
        selectedTeamTitleTextView.setTextColor(accentColor)
        selectedTeamStatusTextView.setTextColor(accentColor)

        val otherTeamCorrect = if (teamName == "Team A") {
            teamBPlayers.sumOf { it.correctCount }
        } else {
            teamAPlayers.sumOf { it.correctCount }
        }

        selectedTeamStatusTextView.text = when {
            totalCorrect > otherTeamCorrect -> "Leading by ${totalCorrect - otherTeamCorrect}"
            totalCorrect < otherTeamCorrect -> "Behind by ${otherTeamCorrect - totalCorrect}"
            else -> "Tie"
        }

        selectedTeamCorrectTextView.text = totalCorrect.toString()
        selectedTeamWrongTextView.text = totalWrong.toString()

        topPlayer?.let {
            selectedTeamTopPerformerNameTextView.text = it.name
            selectedTeamTopPerformerStatsTextView.text =
                "Correct ${it.correctCount} • Wrong ${it.wrongCount}"
            selectedTeamTopPerformerAvatarImageView.setImageResource(getAvatarForPlayer(it))
            selectedTeamTopPerformerBadgeTextView.text = "MVP"
        }

        selectedTeamPlayers.clear()
        selectedTeamPlayers.addAll(
            players.sortedWith(
                compareByDescending<RoomPlayer> { it.correctCount }
                    .thenBy { it.wrongCount }
            )
        )
        selectedTeamPlayersAdapter.notifyDataSetChanged()
    }

    private fun hideSelectedTeamDetails() {
        selectedTeamDetailsCardView.isVisible = false
        expandedTeamKey = null
        updateTeamExpandArrows(null)
    }

    private fun updateTeamExpandArrows(expandedKey: String?) {
        teamAExpandImageView.animate().rotation(if (expandedKey == "A") 180f else 0f).setDuration(180).start()
        teamBExpandImageView.animate().rotation(if (expandedKey == "B") 180f else 0f).setDuration(180).start()
    }

    private fun getTopPlayer(players: List<RoomPlayer>): RoomPlayer? {
        return players.sortedWith(
            compareByDescending<RoomPlayer> { it.correctCount }
                .thenBy { it.wrongCount }
                .thenBy { it.joinedAt }
        ).firstOrNull()
    }

    private fun isCurrentUserPlayer(player: RoomPlayer): Boolean {
        return player.uid == safeCurrentUserId() ||
                player.name.equals(safeCurrentUserName(), ignoreCase = true)
    }

    private fun getAvatarForPlayer(player: RoomPlayer): Int {
        return when {
            isCurrentUserPlayer(player) -> safeCurrentUserAvatar()
            player.name.equals("alex", ignoreCase = true) -> R.drawable.avatar_3
            player.name.equals("jessica", ignoreCase = true) -> R.drawable.avatar_2
            player.name.equals("david", ignoreCase = true) -> R.drawable.avatar_4
            player.name.equals("michael", ignoreCase = true) -> R.drawable.avatar_2
            player.name.equals("emma", ignoreCase = true) -> R.drawable.avatar_1
            player.name.equals("sophia", ignoreCase = true) -> R.drawable.avatar_3
            player.name.equals("daniel", ignoreCase = true) -> R.drawable.avatar_4
            else -> R.drawable.ic_avatar_placeholder
        }
    }

    private fun startTeamTimer() {
        teamCountDownTimer?.cancel()

        teamCountDownTimer = object : CountDownTimer(teamTimeLeftInMillis, 1000L) {
            override fun onTick(millisUntilFinished: Long) {
                teamTimeLeftInMillis = millisUntilFinished
                val seconds = millisUntilFinished / 1000L
                teamTimerTextView.text = "${seconds}s"
            }

            override fun onFinish() {
                teamTimerTextView.text = "0s"
                finishQuiz()
            }
        }.start()
    }

    private fun setupCategoryTitle() {
        val categoryTitle: String
        val categoryIconRes: Int

        when (currentCategory.trim().lowercase()) {
            "informatics" -> {
                categoryTitle = if (gameMode.equals("challenge", true)) {
                    "Challenge • Informatics"
                } else {
                    "Informatics"
                }
                categoryIconRes = R.drawable.ic_chip_informatics
            }
            "mathematics", "math" -> {
                categoryTitle = if (gameMode.equals("challenge", true)) {
                    "Challenge • Mathematics"
                } else {
                    "Mathematics"
                }
                categoryIconRes = R.drawable.ic_chip_math_pi
            }
            "english" -> {
                categoryTitle = if (gameMode.equals("challenge", true)) {
                    "Challenge • English"
                } else {
                    "English"
                }
                categoryIconRes = R.drawable.ic_chip_english
            }
            "history" -> {
                categoryTitle = if (gameMode.equals("challenge", true)) {
                    "Challenge • History"
                } else {
                    "History"
                }
                categoryIconRes = R.drawable.ic_chip_history
            }
            "world", "worldview", "world_knowledge" -> {
                categoryTitle = if (gameMode.equals("challenge", true)) {
                    "Challenge • World Knowledge"
                } else {
                    "World Knowledge"
                }
                categoryIconRes = R.drawable.ic_chip_knowledge
            }
            "logic" -> {
                categoryTitle = if (gameMode.equals("challenge", true)) {
                    "Challenge • Logic"
                } else {
                    "Logic"
                }
                categoryIconRes = R.drawable.ic_chip_logic
            }
            else -> {
                categoryTitle = if (gameMode.equals("challenge", true)) {
                    "Challenge • Quiz"
                } else {
                    "Quiz"
                }
                categoryIconRes = R.drawable.ic_chip_informatics
            }
        }

        quizCategoryTextView.text = categoryTitle
        quizCategoryTextView.setCompoundDrawablesRelativeWithIntrinsicBounds(
            categoryIconRes, 0, 0, 0
        )
        quizCategoryTextView.compoundDrawablePadding = 8
    }

    private fun setRandomBackground() {
        val backgroundList = when (currentCategory.lowercase()) {
            "informatics" -> listOf(
                R.drawable.bg_informatics_1,
                R.drawable.bg_informatics_2,
                R.drawable.bg_informatics_3
            )
            "mathematics", "math" -> listOf(
                R.drawable.bg_math_1,
                R.drawable.bg_math_2,
                R.drawable.bg_math_3
            )
            "english" -> listOf(
                R.drawable.bg_english_1,
                R.drawable.bg_english_2,
                R.drawable.bg_english_3
            )
            "history" -> listOf(
                R.drawable.bg_history_1,
                R.drawable.bg_history_2,
                R.drawable.bg_history_3
            )
            "world", "worldview", "world_knowledge" -> listOf(
                R.drawable.bg_world_1,
                R.drawable.bg_world_2,
                R.drawable.bg_world_3
            )
            "logic" -> listOf(
                R.drawable.bg_logic_1,
                R.drawable.bg_logic_2,
                R.drawable.bg_logic_3
            )
            else -> listOf(R.drawable.bg_informatics_1)
        }

        quizBackgroundImageView.setImageResource(backgroundList.random())
    }

    private fun setupClickListeners() {
        backImageView.setOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }

        bookmarkQuestionImageView.setOnClickListener {
            toggleFavoriteForCurrentQuestion()
        }

        optionACardView.setOnClickListener { checkAnswer(0, optionACardView) }
        optionBCardView.setOnClickListener { checkAnswer(1, optionBCardView) }
        optionCCardView.setOnClickListener { checkAnswer(2, optionCCardView) }
        optionDCardView.setOnClickListener { checkAnswer(3, optionDCardView) }

        if (!isTeamBattleMode && !isOnlineDuelMode) {
            help5050CardView.setOnClickListener { use5050Help() }
            helpSkipCardView.setOnClickListener { useSkipHelp() }
            helpExtraTimeCardView.setOnClickListener { useExtraTimeHelp() }
        }

        teamAQuizCardView.setOnClickListener {
            if (!isTeamBattleMode) return@setOnClickListener

            if (expandedTeamKey == "A") {
                hideSelectedTeamDetails()
            } else {
                showSelectedTeamDetails(
                    teamName = "Team A",
                    players = teamAPlayers,
                    accentColor = Color.parseColor("#FF8A34"),
                    teamKey = "A"
                )
            }
        }

        teamBQuizCardView.setOnClickListener {
            if (!isTeamBattleMode) return@setOnClickListener

            if (expandedTeamKey == "B") {
                hideSelectedTeamDetails()
            } else {
                showSelectedTeamDetails(
                    teamName = "Team B",
                    players = teamBPlayers,
                    accentColor = Color.parseColor("#7B61FF"),
                    teamKey = "B"
                )
            }
        }

        selectedTeamCloseImageView.setOnClickListener {
            hideSelectedTeamDetails()
        }
    }

    private fun loadQuestion() {
        if (questions.isEmpty()) {
            Toast.makeText(requireContext(), "No questions available", Toast.LENGTH_SHORT).show()
            findNavController().popBackStack()
            return
        }

        if (currentQuestionIndex >= questions.size) {
            finishQuiz()
            return
        }

        resetOptionViews()
        isAnswerLocked = false

        val currentQuestion = questions[currentQuestionIndex]

        if (bookmarkQuestionImageView.isVisible) {
            updateBookmarkState(currentQuestion)
        }

        questionCountTextView.text = "Question ${currentQuestionIndex + 1} / ${questions.size}"
        questionProgressIndicator.max = questions.size
        questionProgressIndicator.progress = currentQuestionIndex + 1

        questionTextView.text = currentQuestion.question
        optionATextView.text = currentQuestion.options[0]
        optionBTextView.text = currentQuestion.options[1]
        optionCTextView.text = currentQuestion.options[2]
        optionDTextView.text = currentQuestion.options[3]

        if (currentQuestion.imageResId != null) {
            questionImageView.isVisible = true
            questionImageView.setImageResource(currentQuestion.imageResId)
        } else {
            questionImageView.isVisible = false
        }

        if (!isTeamBattleMode) {
            startQuestionTimer(20_000L)
        }
    }

    private fun updateBookmarkState(questionItem: QuestionItem) {
        val isFavorite = FavoriteManager.isFavorite(questionItem.question)

        bookmarkQuestionImageView.setImageResource(
            if (isFavorite) R.drawable.ic_bookmark_selected else R.drawable.ic_bookmark
        )
    }

    private fun toggleFavoriteForCurrentQuestion() {
        val currentQuestion = questions[currentQuestionIndex]
        val isFavorite = FavoriteManager.isFavorite(currentQuestion.question)

        if (isFavorite) {
            FavoriteManager.removeFavorite(currentQuestion.question)
            bookmarkQuestionImageView.setImageResource(R.drawable.ic_bookmark)
            Toast.makeText(requireContext(), "Removed from favorites", Toast.LENGTH_SHORT).show()
        } else {
            val favoriteQuestion = FavoriteQuestion(
                category = quizCategoryTextView.text.toString(),
                question = currentQuestion.question,
                correctAnswer = currentQuestion.options[currentQuestion.correctAnswerIndex],
                categoryIconResId = getCategoryIconResId(currentCategory),
                isFavorite = true
            )

            FavoriteManager.addFavorite(favoriteQuestion)
            bookmarkQuestionImageView.setImageResource(R.drawable.ic_bookmark_selected)
            Toast.makeText(requireContext(), "Added to favorites", Toast.LENGTH_SHORT).show()
        }
    }

    private fun getCategoryIconResId(category: String): Int {
        return when (category.lowercase()) {
            "informatics" -> R.drawable.ic_category_informatics
            "mathematics", "math" -> R.drawable.ic_catagory_math_pi
            "english" -> R.drawable.ic_category_english
            "history" -> R.drawable.ic_category_history
            "world", "worldview", "world_knowledge" -> R.drawable.ic_category_knowledge
            "logic" -> R.drawable.ic_category_logic
            else -> R.drawable.ic_category_informatics
        }
    }

    private fun startQuestionTimer(duration: Long) {
        countDownTimer?.cancel()
        timeLeftInMillis = duration

        countDownTimer = object : CountDownTimer(timeLeftInMillis, 1000L) {
            override fun onTick(millisUntilFinished: Long) {
                timeLeftInMillis = millisUntilFinished
                val seconds = millisUntilFinished / 1000L

                if (isOnlineDuelMode) {
                    duelTimerTextView.text = "${seconds}s"
                } else {
                    timerTextView.text = "${seconds}s"
                }
            }

            override fun onFinish() {
                if (isOnlineDuelMode) {
                    duelTimerTextView.text = "0s"
                } else {
                    timerTextView.text = "0s"
                }

                val currentQuestion = questions[currentQuestionIndex]

                answeredQuestions.add(
                    AnswerReviewItem(
                        category = quizCategoryTextView.text.toString(),
                        question = currentQuestion.question,
                        options = currentQuestion.options,
                        correctAnswerIndex = currentQuestion.correctAnswerIndex,
                        selectedAnswerIndex = null
                    )
                )

                wrongCount++
                showCorrectAnswer()
                moveToNextQuestionDelayed()
            }
        }.start()
    }

    private fun checkAnswer(selectedIndex: Int, selectedCard: MaterialCardView) {
        if (isAnswerLocked || isQuizFinished) return

        isAnswerLocked = true

        if (!isTeamBattleMode) {
            countDownTimer?.cancel()
        }

        val currentQuestion = questions[currentQuestionIndex]
        val correctIndex = currentQuestion.correctAnswerIndex

        answeredQuestions.add(
            AnswerReviewItem(
                category = quizCategoryTextView.text.toString(),
                question = currentQuestion.question,
                options = currentQuestion.options,
                correctAnswerIndex = currentQuestion.correctAnswerIndex,
                selectedAnswerIndex = selectedIndex
            )
        )

        if (selectedIndex == correctIndex) {
            score++
            setCardCorrect(selectedCard)
        } else {
            wrongCount++
            setCardWrong(selectedCard)
            showCorrectAnswer()
        }

        if (isTeamBattleMode) {
            val safeUserId = safeCurrentUserId()
            val safeUserName = safeCurrentUserName()
            val teamList = if (selectedTeam == "B") teamBPlayers else teamAPlayers

            teamList.firstOrNull {
                it.uid == safeUserId || it.name.equals(safeUserName, ignoreCase = true)
            }?.let { player ->
                val index = teamList.indexOf(player)
                if (index != -1) {
                    teamList[index] = player.copy(
                        correctCount = score,
                        wrongCount = wrongCount,
                        score = score * 10
                    )
                }
            }

            updateTeamSummaryUi()

            if (expandedTeamKey == "A") {
                showSelectedTeamDetails(
                    teamName = "Team A",
                    players = teamAPlayers,
                    accentColor = Color.parseColor("#FF8A34"),
                    teamKey = "A"
                )
            } else if (expandedTeamKey == "B") {
                showSelectedTeamDetails(
                    teamName = "Team B",
                    players = teamBPlayers,
                    accentColor = Color.parseColor("#7B61FF"),
                    teamKey = "B"
                )
            }
        }

        if (isOnlineDuelMode) {
            duelPlayerScore = score * 10
            updateDuelSummaryUi()
        }

        moveToNextQuestionDelayed()
    }

    private fun showCorrectAnswer() {
        when (questions[currentQuestionIndex].correctAnswerIndex) {
            0 -> setCardCorrect(optionACardView)
            1 -> setCardCorrect(optionBCardView)
            2 -> setCardCorrect(optionCCardView)
            3 -> setCardCorrect(optionDCardView)
        }
    }

    private fun moveToNextQuestionDelayed() {
        view?.postDelayed({
            if (isQuizFinished) return@postDelayed
            currentQuestionIndex++
            loadQuestion()
        }, 900L)
    }

    private fun use5050Help() {
        if (is5050Used || isAnswerLocked) return

        is5050Used = true
        help5050CardView.alpha = 0.45f
        help5050CardView.isEnabled = false

        val correctIndex = questions[currentQuestionIndex].correctAnswerIndex
        val wrongIndexes = mutableListOf(0, 1, 2, 3).apply { remove(correctIndex) }.shuffled()

        hideOptionByIndex(wrongIndexes[0])
        hideOptionByIndex(wrongIndexes[1])
    }

    private fun useSkipHelp() {
        if (isSkipUsed || isAnswerLocked) return

        isSkipUsed = true
        helpSkipCardView.alpha = 0.45f
        helpSkipCardView.isEnabled = false

        val currentQuestion = questions[currentQuestionIndex]

        answeredQuestions.add(
            AnswerReviewItem(
                category = quizCategoryTextView.text.toString(),
                question = currentQuestion.question,
                options = currentQuestion.options,
                correctAnswerIndex = currentQuestion.correctAnswerIndex,
                selectedAnswerIndex = null
            )
        )

        countDownTimer?.cancel()
        currentQuestionIndex++
        loadQuestion()
    }

    private fun useExtraTimeHelp() {
        if (isExtraTimeUsed || isAnswerLocked) return

        isExtraTimeUsed = true
        helpExtraTimeCardView.alpha = 0.45f
        helpExtraTimeCardView.isEnabled = false

        startQuestionTimer(timeLeftInMillis + 10_000L)
        Toast.makeText(requireContext(), "+10 seconds added", Toast.LENGTH_SHORT).show()
    }

    private fun hideOptionByIndex(index: Int) {
        when (index) {
            0 -> optionACardView.visibility = View.INVISIBLE
            1 -> optionBCardView.visibility = View.INVISIBLE
            2 -> optionCCardView.visibility = View.INVISIBLE
            3 -> optionDCardView.visibility = View.INVISIBLE
        }
    }

    private fun resetOptionViews() {
        optionACardView.visibility = View.VISIBLE
        optionBCardView.visibility = View.VISIBLE
        optionCCardView.visibility = View.VISIBLE
        optionDCardView.visibility = View.VISIBLE

        resetCard(optionACardView)
        resetCard(optionBCardView)
        resetCard(optionCCardView)
        resetCard(optionDCardView)
    }

    private fun resetCard(cardView: MaterialCardView) {
        cardView.setCardBackgroundColor(Color.WHITE)
        cardView.strokeColor = Color.parseColor("#A3A3FF")
        cardView.strokeWidth = 1
    }

    private fun setCardCorrect(cardView: MaterialCardView) {
        cardView.setCardBackgroundColor(Color.parseColor("#DFF6E7"))
        cardView.strokeColor = Color.parseColor("#34D287")
        cardView.strokeWidth = 3
    }

    private fun setCardWrong(cardView: MaterialCardView) {
        cardView.setCardBackgroundColor(Color.parseColor("#FDE4E4"))
        cardView.strokeColor = Color.parseColor("#F55150")
        cardView.strokeWidth = 3
    }

    private fun finishQuiz() {
        if (isQuizFinished) return
        isQuizFinished = true

        countDownTimer?.cancel()
        teamCountDownTimer?.cancel()

        ReviewManager.reviewList.clear()
        ReviewManager.reviewList.addAll(answeredQuestions)

        val safeUserName = safeCurrentUserName()
        val safeUserId = safeCurrentUserId()
        val safeUserAvatar = safeCurrentUserAvatar()
        val correctCount = score
        val finalScore = correctCount * 10
        val playerTeamName = if (selectedTeam == "B") "Team B" else "Team A"

        val teamAScoreValue = teamAPlayers.sumOf { it.correctCount }
        val teamBScoreValue = teamBPlayers.sumOf { it.correctCount }

        val allPlayers = teamAPlayers + teamBPlayers
        val topPerformer = allPlayers.maxWithOrNull(
            compareByDescending<RoomPlayer> { it.correctCount }
                .thenBy { it.wrongCount }
        )

        val bundle = Bundle().apply {
            putInt("score", finalScore)
            putInt("correct", correctCount)
            putInt("wrong", wrongCount)
            putString("category", currentCategory)
            putString("difficulty", currentDifficulty)
            putInt("questionCount", questions.size)

            putString("gameMode", gameMode)

            putBoolean("isTeamBattleMode", isTeamBattleMode)
            putBoolean("isMultiplayer", isMultiplayer)
            putBoolean("isOnlineDuel", isOnlineDuelMode)

            putBoolean("isForfeitResult", false)
            putBoolean("didCurrentPlayerForfeit", false)

            putString("roomId", roomId)
            putString("selectedTeam", selectedTeam)
            putString("teamFormat", selectedTeamFormat)

            putString("opponentName", duelOpponentName)
            putString("opponentId", duelOpponentId)
            putInt("opponentAvatar", duelOpponentAvatarRes)
            putInt("opponentCorrect", duelOpponentScore / 10)

            putString("playerName", safeUserName)
            putString("playerId", safeUserId)
            putInt("playerAvatar", safeUserAvatar)

            putInt("teamAScore", teamAScoreValue)
            putInt("teamBScore", teamBScoreValue)
            putString("playerTeamName", playerTeamName)
            putString("topPerformerName", topPerformer?.name ?: safeUserName)
            putInt("topPerformerCorrect", topPerformer?.correctCount ?: correctCount)
            putInt("topPerformerWrong", topPerformer?.wrongCount ?: wrongCount)
        }

        findNavController().navigate(
            R.id.action_quizGameFragment_to_resultFragment,
            bundle
        )
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
                        .alpha(if (view.isEnabled) 1f else 0.45f)
                        .setDuration(90)
                        .start()
                }
            }
            false
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        countDownTimer?.cancel()
        teamCountDownTimer?.cancel()
    }

    private fun getQuestionsByCategory(category: String): List<QuestionItem> {
        return when (category.lowercase()) {
            "informatics" -> getInformaticsQuestions()
            "mathematics", "math" -> getMathematicsQuestions()
            "english" -> getEnglishQuestions()
            "history" -> getHistoryQuestions()
            "world", "worldview", "world_knowledge" -> getWorldQuestions()
            "logic" -> getLogicQuestions()
            else -> getInformaticsQuestions()
        }
    }

    private fun getInformaticsQuestions(): List<QuestionItem> {
        return listOf(
            QuestionItem(
                question = "What does CPU stand for in computer science?",
                options = listOf(
                    "Central Processing Unit",
                    "Computer Power Unit",
                    "Control Program Utility",
                    "Central Program Utility"
                ),
                correctAnswerIndex = 0
            ),
            QuestionItem(
                question = "Which device is used to input text into a computer?",
                options = listOf("Monitor", "Keyboard", "Printer", "Speaker"),
                correctAnswerIndex = 1
            ),
            QuestionItem(
                question = "What does RAM stand for?",
                options = listOf(
                    "Read Access Memory",
                    "Random Access Memory",
                    "Run Access Memory",
                    "Rapid Action Memory"
                ),
                correctAnswerIndex = 1
            ),
            QuestionItem(
                question = "Which one is an operating system?",
                options = listOf("Windows", "Google", "Intel", "YouTube"),
                correctAnswerIndex = 0
            ),
            QuestionItem(
                question = "Which of these is a programming language?",
                options = listOf("HTML", "Kotlin", "Wi-Fi", "SSD"),
                correctAnswerIndex = 1
            ),
            QuestionItem(
                question = "What does USB stand for?",
                options = listOf(
                    "Universal Serial Bus",
                    "United System Board",
                    "Universal System Base",
                    "User Storage Box"
                ),
                correctAnswerIndex = 0
            ),
            QuestionItem(
                question = "Which part of a computer shows visual output?",
                options = listOf("Mouse", "Monitor", "Keyboard", "Microphone"),
                correctAnswerIndex = 1
            ),
            QuestionItem(
                question = "What is used to browse websites?",
                options = listOf("Browser", "Printer", "Scanner", "Folder"),
                correctAnswerIndex = 0
            ),
            QuestionItem(
                question = "Which storage is temporary?",
                options = listOf("Hard Disk", "SSD", "RAM", "USB Flash"),
                correctAnswerIndex = 2
            ),
            QuestionItem(
                question = "What does WWW stand for?",
                options = listOf(
                    "World Wide Web",
                    "Web World Window",
                    "Wide Web World",
                    "World Web Wire"
                ),
                correctAnswerIndex = 0
            )
        )
    }

    private fun getMathematicsQuestions(): List<QuestionItem> {
        return listOf(
            QuestionItem("What is 12 × 8?", listOf("96", "88", "108", "92"), 0),
            QuestionItem("What is the square root of 81?", listOf("7", "8", "9", "10"), 2),
            QuestionItem("What is 15 + 27?", listOf("41", "42", "43", "44"), 1),
            QuestionItem("What is 100 ÷ 4?", listOf("20", "25", "30", "40"), 1),
            QuestionItem("How many sides does a triangle have?", listOf("3", "4", "5", "6"), 0),
            QuestionItem("What is 9²?", listOf("18", "72", "81", "99"), 2),
            QuestionItem("What is 50 - 19?", listOf("29", "30", "31", "32"), 2),
            QuestionItem("Which is an even number?", listOf("13", "17", "22", "29"), 2),
            QuestionItem("What is 7 + 6 × 2?", listOf("19", "26", "24", "20"), 0),
            QuestionItem("What is the value of π approximately?", listOf("2.14", "3.14", "4.13", "3.41"), 1)
        )
    }

    private fun getEnglishQuestions(): List<QuestionItem> {
        return listOf(
            QuestionItem("Choose the correct plural form of 'child'.", listOf("Childs", "Children", "Childes", "Childrens"), 1),
            QuestionItem("Which word is a verb?", listOf("Beautiful", "Quickly", "Run", "Blue"), 2),
            QuestionItem("Choose the correct sentence.", listOf("She go to school.", "She goes to school.", "She going to school.", "She gone to school."), 1),
            QuestionItem("What is the opposite of 'happy'?", listOf("Angry", "Sad", "Funny", "Kind"), 1),
            QuestionItem("Choose the correct article: ___ apple", listOf("a", "an", "the", "no article"), 1),
            QuestionItem("Which word is a noun?", listOf("Table", "Quickly", "Happy", "Run"), 0),
            QuestionItem("What is the past form of 'go'?", listOf("Goed", "Went", "Gone", "Goes"), 1),
            QuestionItem("Choose the synonym of 'big'.", listOf("Tiny", "Large", "Short", "Light"), 1),
            QuestionItem("Which one is correct?", listOf("He don't like tea.", "He doesn't like tea.", "He not like tea.", "He isn't like tea."), 1),
            QuestionItem("What is the opposite of 'early'?", listOf("Late", "Fast", "Soon", "Before"), 0)
        )
    }

    private fun getHistoryQuestions(): List<QuestionItem> {
        return listOf(
            QuestionItem("Who was the first President of the United States?", listOf("Abraham Lincoln", "George Washington", "Thomas Jefferson", "John Adams"), 1),
            QuestionItem("In which year did World War II end?", listOf("1942", "1945", "1950", "1939"), 1),
            QuestionItem("Which ancient civilization built the pyramids?", listOf("Romans", "Greeks", "Egyptians", "Persians"), 2),
            QuestionItem("Who discovered America in 1492?", listOf("Christopher Columbus", "Marco Polo", "Ferdinand Magellan", "James Cook"), 0),
            QuestionItem("Which wall divided Berlin?", listOf("China Wall", "Berlin Wall", "Roman Wall", "Stone Wall"), 1),
            QuestionItem("Who was known as the 'Maid of Orleans'?", listOf("Cleopatra", "Joan of Arc", "Queen Elizabeth", "Marie Curie"), 1),
            QuestionItem("Which empire was ruled by Julius Caesar?", listOf("Greek Empire", "Roman Empire", "Ottoman Empire", "Persian Empire"), 1),
            QuestionItem("The Titanic sank in which year?", listOf("1905", "1912", "1920", "1898"), 1),
            QuestionItem("Which country started the Renaissance?", listOf("France", "Italy", "Germany", "England"), 1),
            QuestionItem("Who was the famous leader of Nazi Germany?", listOf("Hitler", "Napoleon", "Stalin", "Churchill"), 0)
        )
    }

    private fun getWorldQuestions(): List<QuestionItem> {
        return listOf(
            QuestionItem("What is the largest ocean on Earth?", listOf("Atlantic Ocean", "Indian Ocean", "Pacific Ocean", "Arctic Ocean"), 2),
            QuestionItem("Which country has the largest population?", listOf("India", "China", "USA", "Brazil"), 1),
            QuestionItem("What is the capital of Japan?", listOf("Seoul", "Kyoto", "Tokyo", "Osaka"), 2),
            QuestionItem("Which continent is Egypt in?", listOf("Asia", "Europe", "Africa", "South America"), 2),
            QuestionItem("What is the longest river in the world?", listOf("Amazon", "Nile", "Mississippi", "Yangtze"), 1),
            QuestionItem("Which country is known as the Land of the Rising Sun?", listOf("China", "Thailand", "Japan", "Korea"), 2),
            QuestionItem("Which desert is the largest hot desert?", listOf("Gobi", "Sahara", "Kalahari", "Arabian"), 1),
            QuestionItem("What is the capital of France?", listOf("Berlin", "Madrid", "Rome", "Paris"), 3),
            QuestionItem("Which country has the city of Sydney?", listOf("Canada", "Australia", "New Zealand", "USA"), 1),
            QuestionItem("Mount Everest is located in which mountain range?", listOf("Andes", "Himalayas", "Alps", "Rockies"), 1)
        )
    }

    private fun getLogicQuestions(): List<QuestionItem> {
        return listOf(
            QuestionItem("What comes next in the sequence: 2, 4, 8, 16, ?", listOf("18", "24", "32", "30"), 2),
            QuestionItem("If all roses are flowers and some flowers fade, which statement is true?", listOf("All roses fade", "Some roses may fade", "No roses fade", "Only roses fade"), 1),
            QuestionItem("Which number does not belong: 3, 5, 7, 10, 11?", listOf("3", "5", "7", "10"), 3),
            QuestionItem("If TODAY is coded as UPEBZ, how is CODE coded?", listOf("DPEF", "DPDF", "CPDE", "DODE"), 0),
            QuestionItem("What comes next: A, C, E, G, ?", listOf("H", "I", "J", "K"), 1),
            QuestionItem("Which shape completes the pattern?", listOf("Circle", "Triangle", "Square", "Star"), 2),
            QuestionItem("A clock shows 3:15. What is the angle between the hands approximately?", listOf("0°", "7.5°", "15°", "30°"), 1),
            QuestionItem("Which word is the odd one out?", listOf("Apple", "Banana", "Carrot", "Orange"), 2),
            QuestionItem("If 5 machines make 5 items in 5 minutes, how long do 100 machines take to make 100 items?", listOf("5 minutes", "20 minutes", "100 minutes", "1 minute"), 0),
            QuestionItem("Which number completes the pattern: 1, 1, 2, 3, 5, 8, ?", listOf("11", "12", "13", "14"), 2)
        )
    }

    data class QuestionItem(
        val question: String,
        val options: List<String>,
        val correctAnswerIndex: Int,
        val imageResId: Int? = null
    )

    class TeamDetailsPlayerAdapter(
        private val players: List<RoomPlayer>
    ) : RecyclerView.Adapter<TeamDetailsPlayerAdapter.TeamDetailsPlayerViewHolder>() {

        inner class TeamDetailsPlayerViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            private val avatarImageView: ShapeableImageView =
                itemView.findViewById(R.id.playerAvatarImageView)
            private val nameTextView: TextView =
                itemView.findViewById(R.id.playerNameTextView)
            private val idTextView: TextView =
                itemView.findViewById(R.id.playerIdTextView)
            private val scoreTextView: TextView =
                itemView.findViewById(R.id.playerScoreTextView)
            private val onlineStatusView: View =
                itemView.findViewById(R.id.playerOnlineStatusView)

            fun bind(player: RoomPlayer) {
                val currentUser = UserManager.currentUser

                nameTextView.text = player.name
                idTextView.text = "Correct ${player.correctCount} • Wrong ${player.wrongCount}"
                scoreTextView.text = player.score.toString()
                onlineStatusView.isVisible = !player.isBot

                val safeCurrentUserId = currentUser.userId.trim()
                val safeCurrentUserName = currentUser.name.trim()
                val safeCurrentUserAvatar =
                    if (currentUser.avatarResId != 0) currentUser.avatarResId else R.drawable.ic_avatar_placeholder

                val avatarRes = when {
                    player.uid == safeCurrentUserId ||
                            player.name.equals(safeCurrentUserName, ignoreCase = true) ->
                        safeCurrentUserAvatar

                    player.name.equals("alex", ignoreCase = true) -> R.drawable.avatar_3
                    player.name.equals("jessica", ignoreCase = true) -> R.drawable.avatar_2
                    player.name.equals("david", ignoreCase = true) -> R.drawable.avatar_4
                    player.name.equals("michael", ignoreCase = true) -> R.drawable.avatar_2
                    player.name.equals("emma", ignoreCase = true) -> R.drawable.avatar_1
                    player.name.equals("sophia", ignoreCase = true) -> R.drawable.avatar_3
                    player.name.equals("daniel", ignoreCase = true) -> R.drawable.avatar_4
                    else -> R.drawable.ic_avatar_placeholder
                }

                avatarImageView.setImageResource(avatarRes)
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TeamDetailsPlayerViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_team_player, parent, false)
            return TeamDetailsPlayerViewHolder(view)
        }

        override fun onBindViewHolder(holder: TeamDetailsPlayerViewHolder, position: Int) {
            holder.bind(players[position])
        }

        override fun getItemCount(): Int = players.size
    }

}