package com.example.quizapp.ui

import android.os.Bundle
import android.os.CountDownTimer
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.TextView
import android.widget.Toast
import androidx.activity.addCallback
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.quizapp.R
import com.example.quizapp.databinding.FragmentMultiplayerRandomBinding
import com.example.quizapp.manager.TeamBalancer
import com.example.quizapp.manager.UserManager
import com.example.quizapp.model.RoomPlayer
import com.google.android.material.imageview.ShapeableImageView
import java.util.Locale
import kotlin.math.absoluteValue

class MultiPlayerRandomFragment : Fragment() {

    private var _binding: FragmentMultiplayerRandomBinding? = null
    private val binding get() = _binding!!

    private var searchTimer: CountDownTimer? = null
    private var isSearching = false
    private var hasNavigatedToGame = false

    private val searchDuration = 45_000L
    private val totalSeconds = 45

    private var selectedCategory: String = "Mathematics"
    private var selectedTeamFormat: String = "2v2"
    private var requiredPlayersPerTeam: Int = 2

    private val teamAPlayers = mutableListOf<RoomPlayer>()
    private val teamBPlayers = mutableListOf<RoomPlayer>()

    private lateinit var teamAAdapter: TeamPlayerAdapter
    private lateinit var teamBAdapter: TeamPlayerAdapter

    private val currentUserName: String
        get() = UserManager.currentUser.name.ifBlank { "You" }

    private val currentUserId: String
        get() = UserManager.currentUser.userId.ifBlank { "AEL-0000" }

    private val currentUserAvatarRes: Int
        get() = if (UserManager.currentUser.avatarResId != 0) {
            UserManager.currentUser.avatarResId
        } else {
            R.drawable.ic_avatar_placeholder
        }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMultiplayerRandomBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        UserManager.loadUser(requireContext())

        setupBackNavigation()
        readArguments()
        setupRecyclerViews()
        setupViews()
        setupClickListeners()
        setupPressedEffects()
        startSearching()
    }

    private fun setupBackNavigation() {
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner) {
            searchTimer?.cancel()
            if (isSearching) {
                isSearching = false
            }
            findNavController().navigateUp()
        }
    }

    private fun readArguments() {
        selectedCategory = arguments?.getString("selectedCategory")
            ?.trim()
            ?.ifBlank {
                arguments?.getString("category")?.trim()?.ifBlank { "Mathematics" }
                    ?: "Mathematics"
            }
            ?: arguments?.getString("category")
                ?.trim()
                ?.ifBlank { "Mathematics" }
                    ?: "Mathematics"

        selectedTeamFormat = arguments?.getString("selectedTeamFormat")
            ?.trim()
            ?.ifBlank {
                arguments?.getString("teamFormat")?.trim()?.ifBlank { "2v2" }
                    ?: "2v2"
            }
            ?: arguments?.getString("teamFormat")
                ?.trim()
                ?.ifBlank { "2v2" }
                    ?: "2v2"

        requiredPlayersPerTeam = TeamBalancer.getRequiredPlayersPerTeam(selectedTeamFormat)
    }

    private fun setupRecyclerViews() {
        teamAAdapter = TeamPlayerAdapter(players = teamAPlayers)
        teamBAdapter = TeamPlayerAdapter(players = teamBPlayers)

        binding.teamARecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = teamAAdapter
            setHasFixedSize(false)
            isNestedScrollingEnabled = false
        }

        binding.teamBRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = teamBAdapter
            setHasFixedSize(false)
            isNestedScrollingEnabled = false
        }
    }

    private fun setupViews() {
        updateCategoryChip(selectedCategory)
        binding.randomFormatChipTextView.text = selectedTeamFormat
        binding.randomTimerTextView.text = totalSeconds.toString()
        binding.searchingForPlayerTextView.text = "Searching for players..."
        binding.stopSearchButton.text = "Stop Searching"
        binding.stopSearchButton.isEnabled = true
        binding.stopSearchButton.alpha = 1f

        binding.randomMatchmakingTimerProgressIndicator.max = totalSeconds
        binding.randomMatchmakingTimerProgressIndicator.setProgressCompat(totalSeconds, false)

        updateTimerColor(totalSeconds)
        updateTeamLists()
    }

    private fun setupPressedEffects() = with(binding) {
        addPressedEffect(stopSearchButton)
        addPressedEffect(randomCategoryChipCardView)
        addPressedEffect(randomFormatChipCardView)
    }

    private fun updateCategoryChip(category: String) {
        binding.randomCategoryChipTextView.text = formatCategoryName(category)

        val iconRes = when (category.trim().lowercase(Locale.ROOT)) {
            "informatics" -> R.drawable.ic_chip_informatics
            "mathematics", "math" -> R.drawable.ic_chip_math_pi
            "english" -> R.drawable.ic_chip_english
            "history" -> R.drawable.ic_chip_history
            "world", "worldview", "world_knowledge" -> R.drawable.ic_chip_knowledge
            "logic" -> R.drawable.ic_chip_logic
            else -> R.drawable.ic_chip_informatics
        }

        binding.randomCategoryChipTextView.setCompoundDrawablesWithIntrinsicBounds(
            iconRes,
            0,
            0,
            0
        )
    }

    private fun setupClickListeners() {
        binding.stopSearchButton.setOnClickListener {
            if (isSearching) {
                stopSearching()
                Toast.makeText(requireContext(), "Search stopped", Toast.LENGTH_SHORT).show()
            } else {
                restartSearching()
                Toast.makeText(requireContext(), "Searching again", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun startSearching() {
        isSearching = true
        hasNavigatedToGame = false

        binding.stopSearchButton.text = "Stop Searching"
        binding.stopSearchButton.isEnabled = true
        binding.stopSearchButton.alpha = 1f
        binding.searchingForPlayerTextView.text = "Searching for players..."
        binding.randomTimerTextView.text = totalSeconds.toString()
        binding.randomMatchmakingTimerProgressIndicator.max = totalSeconds
        binding.randomMatchmakingTimerProgressIndicator.setProgressCompat(totalSeconds, false)

        teamAPlayers.clear()
        teamBPlayers.clear()
        updateTeamLists()
        updateTimerColor(totalSeconds)

        searchTimer?.cancel()
        searchTimer = object : CountDownTimer(searchDuration, 1000L) {

            override fun onTick(millisUntilFinished: Long) {
                if (!isAdded || _binding == null || hasNavigatedToGame) return

                val secondsLeft = (millisUntilFinished / 1000L).toInt()

                binding.randomTimerTextView.text = secondsLeft.toString()
                binding.randomMatchmakingTimerProgressIndicator.setProgressCompat(secondsLeft, true)

                updateTimerColor(secondsLeft)
                animateTimerPulse()
                simulatePlayers(secondsLeft)
            }

            override fun onFinish() {
                if (!isAdded || _binding == null || hasNavigatedToGame) return

                isSearching = false

                binding.randomTimerTextView.text = "0"
                binding.randomMatchmakingTimerProgressIndicator.setProgressCompat(0, true)
                updateTimerColor(0)
                binding.stopSearchButton.text = "Search Again"
                binding.stopSearchButton.isEnabled = true
                binding.stopSearchButton.alpha = 1f

                val canStartMatch = TeamBalancer.fillTeamsWithBotsIfNeeded(
                    teamAPlayers = teamAPlayers,
                    teamBPlayers = teamBPlayers,
                    teamFormat = selectedTeamFormat
                )

                updateTeamLists()

                if (canStartMatch) {
                    val realPlayerCount = (teamAPlayers + teamBPlayers).count { !it.isBot }
                    val botCount = (teamAPlayers + teamBPlayers).count { it.isBot }

                    binding.searchingForPlayerTextView.text = "Match Found!"
                    Toast.makeText(
                        requireContext(),
                        "Match found • Real players: $realPlayerCount • Bots: $botCount",
                        Toast.LENGTH_SHORT
                    ).show()

                    binding.root.postDelayed({
                        if (!hasNavigatedToGame && _binding != null) {
                            openGame()
                        }
                    }, 1200L)
                } else {
                    binding.searchingForPlayerTextView.text = "No players found"
                    Toast.makeText(
                        requireContext(),
                        "No players found. Try again.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }.start()
    }

    private fun stopSearching() {
        isSearching = false
        searchTimer?.cancel()
        binding.searchingForPlayerTextView.text = "Search stopped"
        binding.stopSearchButton.text = "Search Again"
        binding.stopSearchButton.isEnabled = true
        binding.stopSearchButton.alpha = 1f
    }

    private fun restartSearching() {
        startSearching()
    }

    private fun updateTimerColor(secondsLeft: Int) {
        val percent = secondsLeft.toFloat() / totalSeconds.toFloat()

        val colorRes = when {
            percent <= 0.2f -> R.color.color_error
            percent <= 0.5f -> R.color.color_orange_primary
            else -> R.color.color_green
        }

        val color = ContextCompat.getColor(requireContext(), colorRes)
        binding.randomTimerTextView.setTextColor(color)
        binding.randomMatchmakingTimerProgressIndicator.setIndicatorColor(color)
    }

    private fun animateTimerPulse() {
        binding.randomTimerContainer.animate()
            .scaleX(1.03f)
            .scaleY(1.03f)
            .setDuration(220)
            .setInterpolator(AccelerateDecelerateInterpolator())
            .withEndAction {
                if (!isAdded || _binding == null) return@withEndAction

                binding.randomTimerContainer.animate()
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(220)
                    .setInterpolator(AccelerateDecelerateInterpolator())
                    .start()
            }
            .start()
    }

    private fun simulatePlayers(seconds: Int) {
        when (seconds) {
            40 -> {
                if (teamAPlayers.none { it.uid == currentUserId }) {
                    teamAPlayers.add(
                        RoomPlayer(
                            uid = currentUserId,
                            name = currentUserName,
                            avatarUrl = "",
                            team = "A",
                            isBot = false,
                            correctCount = 0,
                            wrongCount = 0,
                            score = UserManager.currentUser.score,
                            joinedAt = System.currentTimeMillis(),
                            isReady = true
                        )
                    )
                    onPlayerListChanged()
                }
            }

            35 -> {
                if (teamBPlayers.none { it.uid == "user_b_1" }) {
                    teamBPlayers.add(
                        RoomPlayer(
                            uid = "user_b_1",
                            name = "Jessica",
                            avatarUrl = "",
                            team = "B",
                            isBot = false,
                            correctCount = 0,
                            wrongCount = 0,
                            score = 1180,
                            joinedAt = System.currentTimeMillis(),
                            isReady = true
                        )
                    )
                    onPlayerListChanged()
                }
            }

            25 -> {
                if (requiredPlayersPerTeam >= 2 && teamAPlayers.none { it.uid == "user_a_2" }) {
                    teamAPlayers.add(
                        RoomPlayer(
                            uid = "user_a_2",
                            name = "Alex",
                            avatarUrl = "",
                            team = "A",
                            isBot = false,
                            correctCount = 0,
                            wrongCount = 0,
                            score = 1090,
                            joinedAt = System.currentTimeMillis(),
                            isReady = true
                        )
                    )
                    onPlayerListChanged()
                }
            }

            15 -> {
                if (requiredPlayersPerTeam >= 2 && teamBPlayers.none { it.uid == "user_b_2" }) {
                    teamBPlayers.add(
                        RoomPlayer(
                            uid = "user_b_2",
                            name = "David",
                            avatarUrl = "",
                            team = "B",
                            isBot = false,
                            correctCount = 0,
                            wrongCount = 0,
                            score = 1115,
                            joinedAt = System.currentTimeMillis(),
                            isReady = true
                        )
                    )
                    onPlayerListChanged()
                }
            }

            12 -> {
                if (requiredPlayersPerTeam >= 3 && teamAPlayers.none { it.uid == "user_a_3" }) {
                    teamAPlayers.add(
                        RoomPlayer(
                            uid = "user_a_3",
                            name = "Michael",
                            avatarUrl = "",
                            team = "A",
                            isBot = false,
                            correctCount = 0,
                            wrongCount = 0,
                            score = 1160,
                            joinedAt = System.currentTimeMillis(),
                            isReady = true
                        )
                    )
                    onPlayerListChanged()
                }
            }

            10 -> {
                if (requiredPlayersPerTeam >= 3 && teamBPlayers.none { it.uid == "user_b_3" }) {
                    teamBPlayers.add(
                        RoomPlayer(
                            uid = "user_b_3",
                            name = "Emma",
                            avatarUrl = "",
                            team = "B",
                            isBot = false,
                            correctCount = 0,
                            wrongCount = 0,
                            score = 1135,
                            joinedAt = System.currentTimeMillis(),
                            isReady = true
                        )
                    )
                    onPlayerListChanged()
                }
            }

            8 -> {
                if (requiredPlayersPerTeam >= 4 && teamAPlayers.none { it.uid == "user_a_4" }) {
                    teamAPlayers.add(
                        RoomPlayer(
                            uid = "user_a_4",
                            name = "Sophia",
                            avatarUrl = "",
                            team = "A",
                            isBot = false,
                            correctCount = 0,
                            wrongCount = 0,
                            score = 1210,
                            joinedAt = System.currentTimeMillis(),
                            isReady = true
                        )
                    )
                    onPlayerListChanged()
                }
            }

            6 -> {
                if (requiredPlayersPerTeam >= 4 && teamBPlayers.none { it.uid == "user_b_4" }) {
                    teamBPlayers.add(
                        RoomPlayer(
                            uid = "user_b_4",
                            name = "Daniel",
                            avatarUrl = "",
                            team = "B",
                            isBot = false,
                            correctCount = 0,
                            wrongCount = 0,
                            score = 1175,
                            joinedAt = System.currentTimeMillis(),
                            isReady = true
                        )
                    )
                    onPlayerListChanged()
                }
            }
        }
    }

    private fun onPlayerListChanged() {
        updateTeamLists()
        checkIfMatchReady()
    }

    private fun checkIfMatchReady() {
        if (!isSearching || hasNavigatedToGame) return

        val realTeamACount = teamAPlayers.count { !it.isBot }
        val realTeamBCount = teamBPlayers.count { !it.isBot }

        val isReady =
            realTeamACount >= requiredPlayersPerTeam &&
                    realTeamBCount >= requiredPlayersPerTeam

        if (isReady) {
            searchTimer?.cancel()
            isSearching = false

            binding.searchingForPlayerTextView.text = "Match Found!"
            binding.stopSearchButton.text = "Starting..."
            binding.stopSearchButton.isEnabled = false
            binding.stopSearchButton.alpha = 0.6f

            Toast.makeText(
                requireContext(),
                "All players joined. Starting match...",
                Toast.LENGTH_SHORT
            ).show()

            binding.root.postDelayed({
                if (!hasNavigatedToGame && _binding != null) {
                    openGame()
                }
            }, 800L)
        }
    }

    private fun updateTeamLists() {
        teamAAdapter.notifyDataSetChanged()
        teamBAdapter.notifyDataSetChanged()

        binding.teamAPlayerCountTextView.text = "${teamAPlayers.size}/$requiredPlayersPerTeam"
        binding.teamBPlayerCountTextView.text = "${teamBPlayers.size}/$requiredPlayersPerTeam"

        binding.teamARecyclerView.isVisible = teamAPlayers.isNotEmpty()
        binding.teamBRecyclerView.isVisible = teamBPlayers.isNotEmpty()
    }

    private fun openGame() {
        if (hasNavigatedToGame || _binding == null) return
        hasNavigatedToGame = true

        val currentUserTeam = when {
            teamAPlayers.any { it.uid == currentUserId } -> "A"
            teamBPlayers.any { it.uid == currentUserId } -> "B"
            else -> "A"
        }

        val bundle = Bundle().apply {
            putString("category", selectedCategory)
            putString("teamFormat", selectedTeamFormat)
            putString("roomId", "RANDOM-${System.currentTimeMillis()}")
            putString("selectedTeam", currentUserTeam)
            putBoolean("isMultiplayer", true)
            putBoolean("isTeamBattleMode", true)
            putInt("questionCount", 10)

            putString("playerName", currentUserName)
            putString("playerId", currentUserId)
            putInt("playerAvatar", currentUserAvatarRes)
        }

        val navController = findNavController()
        if (navController.currentDestination?.id == R.id.multiPlayer_RandomFragment) {
            navController.navigate(
                R.id.action_multiPlayer_RandomFragment_to_quizGameFragment,
                bundle
            )
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
                        .alpha(if (view.isEnabled) 1f else 0.6f)
                        .setDuration(90)
                        .start()
                }
            }
            false
        }
    }

    override fun onDestroyView() {
        searchTimer?.cancel()
        searchTimer = null
        _binding = null
        super.onDestroyView()
    }

    class TeamPlayerAdapter(
        private val players: List<RoomPlayer>
    ) : RecyclerView.Adapter<TeamPlayerAdapter.TeamPlayerViewHolder>() {

        inner class TeamPlayerViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
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
                val avatarRes = getAvatarRes(player)

                avatarImageView.setImageResource(avatarRes)
                nameTextView.text = if (player.isBot) {
                    "${player.name.ifBlank { "Unknown Bot" }} (BOT)"
                } else {
                    player.name.ifBlank { "Unknown Player" }
                }
                idTextView.text = "ID: ${player.uid}"
                scoreTextView.text = player.score.toString()
                onlineStatusView.visibility = if (!player.isBot) View.VISIBLE else View.INVISIBLE
            }

            private fun getAvatarRes(player: RoomPlayer): Int {
                return when {
                    player.uid == UserManager.currentUser.userId -> {
                        if (UserManager.currentUser.avatarResId != 0) {
                            UserManager.currentUser.avatarResId
                        } else {
                            R.drawable.ic_avatar_placeholder
                        }
                    }

                    player.isBot && player.team == "A" -> R.drawable.avatar_3
                    player.isBot && player.team == "B" -> R.drawable.avatar_4
                    else -> {
                        val avatarList = listOf(
                            R.drawable.avatar_1,
                            R.drawable.avatar_2,
                            R.drawable.avatar_3,
                            R.drawable.avatar_4
                        )
                        val index = player.uid.hashCode().absoluteValue % avatarList.size
                        avatarList[index]
                    }
                }
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TeamPlayerViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_team_player, parent, false)
            return TeamPlayerViewHolder(view)
        }

        override fun onBindViewHolder(holder: TeamPlayerViewHolder, position: Int) {
            holder.bind(players[position])
        }

        override fun getItemCount(): Int = players.size
    }

    private fun formatCategoryName(category: String): String {
        return when (category.trim().lowercase(Locale.ROOT)) {
            "informatics" -> "Informatics"
            "mathematics", "math" -> "Mathematics"
            "english" -> "English"
            "history" -> "History"
            "world", "worldview", "world_knowledge" -> "Worldview"
            "logic" -> "Logic"
            else -> "Mathematics"
        }
    }
}