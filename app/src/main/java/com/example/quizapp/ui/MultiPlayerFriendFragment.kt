package com.example.quizapp.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.os.CountDownTimer
import android.view.GestureDetector
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.addCallback
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.quizapp.R
import com.example.quizapp.adapter.RoomChatAdapter
import com.example.quizapp.databinding.FragmentMultiplayerFriendBinding
import com.example.quizapp.manager.UserManager
import com.example.quizapp.model.RoomMessage
import com.example.quizapp.model.RoomPlayer
import com.google.android.material.imageview.ShapeableImageView
import java.util.Locale
import kotlin.math.absoluteValue

class MultiPlayerFriendFragment : Fragment() {

    private var _binding: FragmentMultiplayerFriendBinding? = null
    private val binding get() = _binding!!

    private lateinit var roomId: String
    private var countDownTimer: CountDownTimer? = null

    private var isCountdownRunning = false
    private var hasGameStarted = false

    private var selectedCategory: String = DEFAULT_CATEGORY
    private var selectedTeamFormat: String = DEFAULT_TEAM_FORMAT

    private var currentJoinedTeam: String? = null
    private var isRoomCreator = false

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

    private val roomCreatorName: String
        get() = if (isRoomCreator) currentUserName else "Host"

    private val maxPlayersPerTeam: Int
        get() = getMaxPlayersPerTeam(selectedTeamFormat)

    private val roomMessages = mutableListOf<RoomMessage>()
    private lateinit var roomChatAdapter: RoomChatAdapter

    private val teamAPlayers = mutableListOf<RoomPlayer>()
    private val teamBPlayers = mutableListOf<RoomPlayer>()

    companion object {
        private const val DEFAULT_CATEGORY = "Informatics"
        private const val DEFAULT_TEAM_FORMAT = "4v4"
        private const val DEFAULT_QUESTION_COUNT = 10

        private const val KEY_CATEGORY = "category"
        private const val KEY_TEAM_FORMAT = "teamFormat"
        private const val KEY_ROOM_ID = "roomId"
        private const val KEY_SELECTED_TEAM = "selectedTeam"
        private const val KEY_IS_ROOM_CREATOR = "isRoomCreator"
        private const val KEY_IS_MULTIPLAYER = "isMultiplayer"
        private const val KEY_IS_TEAM_BATTLE_MODE = "isTeamBattleMode"
        private const val KEY_QUESTION_COUNT = "questionCount"
        private const val KEY_PLAYER_NAME = "playerName"
        private const val KEY_PLAYER_ID = "playerId"
        private const val KEY_PLAYER_AVATAR = "playerAvatar"

        private const val TEAM_A = "A"
        private const val TEAM_B = "B"

        private const val COUNTDOWN_MILLIS = 15_000L
        private const val COUNTDOWN_INTERVAL = 1_000L
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMultiplayerFriendBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        UserManager.loadUser(requireContext())

        getArgumentsData()
        setupBackPressedHandler()
        setupViews()
        setupRoomChat()
        setupSlotJoinActions()
        setupClickListeners()
        setupPressedEffects()
        loadMockRoomState()
    }

    private fun getArgumentsData() {
        val bundle = arguments

        selectedCategory = bundle?.getString(KEY_CATEGORY)
            ?.trim()
            ?.ifBlank { DEFAULT_CATEGORY }
            ?: DEFAULT_CATEGORY

        selectedTeamFormat = bundle?.getString(KEY_TEAM_FORMAT)
            ?.trim()
            ?.ifBlank { DEFAULT_TEAM_FORMAT }
            ?: DEFAULT_TEAM_FORMAT

        roomId = bundle?.getString(KEY_ROOM_ID)
            ?.trim()
            ?.ifBlank { generateRoomId() }
            ?: generateRoomId()

        isRoomCreator = bundle?.getBoolean(KEY_IS_ROOM_CREATOR, true) ?: true
    }

    private fun setupBackPressedHandler() {
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner) {
            handleLeaveRoom()
        }
    }

    private fun setupViews() = with(binding) {
        roomIdTextView.text = roomId
        friendsCategoryChipTextView.text = formatCategoryName(selectedCategory)
        friendsFormatChipTextView.text = selectedTeamFormat

        friendsSubtitleTextView.text = if (isRoomCreator) {
            "You are the room creator"
        } else {
            "Waiting for host to start"
        }

        friendsStartInfoTextView.text = if (isRoomCreator) {
            "Start the match when both teams are ready"
        } else {
            "Only the room creator can start the match"
        }

        startFriendsMatchButton.text = "Start Match"
        startFriendsMatchButton.alpha = if (isRoomCreator) 1f else 0.55f
        startFriendsMatchButton.isEnabled = isRoomCreator

        setupCategoryChipIcon()
        setupCategoryBackground()
        setupRoomChatRecycler()
        updateAllUi()
    }

    private fun setupRoomChatRecycler() = with(binding) {
        roomChatAdapter = RoomChatAdapter(roomMessages)

        roomChatRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = roomChatAdapter
            setHasFixedSize(false)
            isNestedScrollingEnabled = false
        }
    }

    private fun setupRoomChat() {
        addSystemMessage("Room created: $roomId")
        addSystemMessage("Category: ${formatCategoryName(selectedCategory)}")
        addSystemMessage("Format: $selectedTeamFormat")
        addSystemMessage(
            if (isRoomCreator) {
                "You are the room creator"
            } else {
                "Waiting for room creator to start the match"
            }
        )
    }

    private fun setupSlotJoinActions() = with(binding) {
        val teamASlots = listOf(
            teamAAvatarSlot1,
            teamAAvatarSlot2,
            teamAAvatarSlot3,
            teamAAvatarSlot4
        )

        val teamBSlots = listOf(
            teamBAvatarSlot1,
            teamBAvatarSlot2,
            teamBAvatarSlot3,
            teamBAvatarSlot4
        )

        teamASlots.forEach { setupDoubleTapJoin(it, TEAM_A) }
        teamBSlots.forEach { setupDoubleTapJoin(it, TEAM_B) }
    }

    private fun setupDoubleTapJoin(view: View, team: String) {
        val gestureDetector = GestureDetector(
            requireContext(),
            object : GestureDetector.SimpleOnGestureListener() {
                override fun onDoubleTap(e: MotionEvent): Boolean {
                    if (isTeamChangeLocked()) {
                        showToast("Team change is locked now")
                        return true
                    }

                    joinTeam(team)
                    return true
                }
            }
        )

        view.setOnTouchListener { _, event ->
            gestureDetector.onTouchEvent(event)
            true
        }
    }

    private fun setupClickListeners() = with(binding) {
        copyRoomIdCardView.setOnClickListener { copyRoomIdToClipboard() }
        copyRoomIdContainer.setOnClickListener { copyRoomIdToClipboard() }

        startFriendsMatchButton.setOnClickListener {
            handleStartGameClick()
        }

        sendMessageImageView.setOnClickListener {
            sendTypedMessage()
        }

        joinTeamAQuickButton.setOnClickListener {
            if (isTeamChangeLocked()) {
                showToast("Team change is locked now")
                return@setOnClickListener
            }
            joinTeam(TEAM_A)
        }

        joinTeamBQuickButton.setOnClickListener {
            if (isTeamChangeLocked()) {
                showToast("Team change is locked now")
                return@setOnClickListener
            }
            joinTeam(TEAM_B)
        }

        readyQuickButton.setOnClickListener {
            sendQuickMessage("I am ready")
        }

        waitQuickButton.setOnClickListener {
            sendQuickMessage("Wait for me")
        }
    }

    private fun setupPressedEffects() = with(binding) {
        addPressedEffect(copyRoomIdCardView)
        addPressedEffect(copyRoomIdContainer)
        addPressedEffect(startFriendsMatchButton)
        addPressedEffect(sendMessageImageView)

        addPressedEffect(joinTeamAQuickButton)
        addPressedEffect(joinTeamBQuickButton)
        addPressedEffect(readyQuickButton)
        addPressedEffect(waitQuickButton)

        addPressedEffect(teamAFriendsCardView)
        addPressedEffect(teamBFriendsCardView)

        addPressedEffect(teamAAvatarSlot1)
        addPressedEffect(teamAAvatarSlot2)
        addPressedEffect(teamAAvatarSlot3)
        addPressedEffect(teamAAvatarSlot4)

        addPressedEffect(teamBAvatarSlot1)
        addPressedEffect(teamBAvatarSlot2)
        addPressedEffect(teamBAvatarSlot3)
        addPressedEffect(teamBAvatarSlot4)

        addPressedEffect(friendsCategoryChipCardView)
        addPressedEffect(friendsFormatChipCardView)
    }

    private fun joinTeam(team: String) {
        if (team == currentJoinedTeam) {
            showToast("You are already in Team $team")
            return
        }

        if (isTeamFull(team)) {
            showToast("Team $team is full")
            return
        }

        removeCurrentUserFromTeams()

        val currentUserPlayer = RoomPlayer(
            uid = currentUserId,
            name = currentUserName,
            avatarUrl = "",
            team = team,
            isBot = false,
            correctCount = 0,
            wrongCount = 0,
            score = UserManager.currentUser.score,
            joinedAt = System.currentTimeMillis(),
            isReady = false
        )

        if (team == TEAM_A) {
            teamAPlayers.add(currentUserPlayer)
        } else {
            teamBPlayers.add(currentUserPlayer)
        }

        currentJoinedTeam = team

        updateAllUi()
        addUserMessage("I joined Team $team")
        binding.friendsSubtitleTextView.text =
            if (canStartGame()) "Teams are ready" else "Waiting for players to join"
    }

    private fun isTeamFull(team: String): Boolean {
        return when (team) {
            TEAM_A -> teamAPlayers.size >= maxPlayersPerTeam
            TEAM_B -> teamBPlayers.size >= maxPlayersPerTeam
            else -> true
        }
    }

    private fun removeCurrentUserFromTeams() {
        teamAPlayers.removeAll { it.uid == currentUserId }
        teamBPlayers.removeAll { it.uid == currentUserId }
    }

    private fun updateAllUi() {
        updatePlayerCounts()
        updateAvatarSlots()
        updatePlayersSummary()
        updateTeamUiState()
    }

    private fun sendTypedMessage() {
        val message = binding.roomMessageEditText.text
            ?.toString()
            ?.trim()
            .orEmpty()

        if (message.isBlank()) {
            showToast("Please write a message")
            return
        }

        addUserMessage(message)
        binding.roomMessageEditText.text?.clear()
    }

    private fun sendQuickMessage(message: String) {
        addUserMessage(message)
    }

    private fun addUserMessage(message: String) {
        roomMessages.add(
            RoomMessage(
                senderName = currentUserName,
                message = message,
                isSystemMessage = false,
                timestamp = System.currentTimeMillis()
            )
        )

        if (::roomChatAdapter.isInitialized && _binding != null) {
            roomChatAdapter.notifyItemInserted(roomMessages.lastIndex)
            binding.roomChatRecyclerView.scrollToPosition(roomMessages.lastIndex)
        }
    }

    private fun addSystemMessage(message: String) {
        roomMessages.add(
            RoomMessage(
                senderName = "System",
                message = message,
                isSystemMessage = true,
                timestamp = System.currentTimeMillis()
            )
        )

        if (::roomChatAdapter.isInitialized && _binding != null) {
            roomChatAdapter.notifyItemInserted(roomMessages.lastIndex)
            binding.roomChatRecyclerView.scrollToPosition(roomMessages.lastIndex)
        }
    }

    private fun handleStartGameClick() {
        if (!isRoomCreator) {
            showToast("Only the room creator can start or stop the match")
            return
        }

        if (isCountdownRunning) {
            cancelCountdown()
            return
        }

        if (hasGameStarted) {
            showToast("Game has already started")
            return
        }

        if (!canStartGame()) {
            showToast("The match can start only when both teams have at least 1 player")
            binding.friendsSubtitleTextView.text = "Both Team A and Team B must have players"
            return
        }

        if (currentJoinedTeam == null) {
            showToast("Please join a team first")
            binding.friendsSubtitleTextView.text = "Join Team A or Team B first"
            return
        }

        startCountdown()
    }

    private fun canStartGame(): Boolean {
        return teamAPlayers.isNotEmpty() && teamBPlayers.isNotEmpty()
    }

    private fun startCountdown() {
        if (!isRoomCreator) return

        isCountdownRunning = true

        binding.friendsSubtitleTextView.text = "Countdown started by room creator"
        binding.friendsStartInfoTextView.text = "Match starts in 15 sec"
        binding.startFriendsMatchButton.text = "Cancel Start"
        binding.startFriendsMatchButton.isEnabled = true
        binding.startFriendsMatchButton.alpha = 1f

        addSystemMessage("Room creator started the countdown")

        countDownTimer?.cancel()
        countDownTimer = object : CountDownTimer(COUNTDOWN_MILLIS, COUNTDOWN_INTERVAL) {
            override fun onTick(millisUntilFinished: Long) {
                if (_binding == null) return
                val secondsLeft = (millisUntilFinished / 1000L).toInt()
                binding.friendsStartInfoTextView.text = "Match starts in $secondsLeft sec"
            }

            override fun onFinish() {
                if (_binding == null) return

                isCountdownRunning = false
                hasGameStarted = true

                binding.friendsStartInfoTextView.text = "Match is starting now"
                binding.friendsSubtitleTextView.text = "Game started by room creator"
                binding.startFriendsMatchButton.text = "Start Match"
                binding.startFriendsMatchButton.isEnabled = false
                binding.startFriendsMatchButton.alpha = 0.55f

                addSystemMessage("Game started")
                showToast("Game Started")

                val bundle = Bundle().apply {
                    putString(KEY_CATEGORY, selectedCategory)
                    putString(KEY_TEAM_FORMAT, selectedTeamFormat)
                    putString(KEY_ROOM_ID, roomId)
                    putString(KEY_SELECTED_TEAM, currentJoinedTeam ?: TEAM_A)
                    putBoolean(KEY_IS_MULTIPLAYER, true)
                    putBoolean(KEY_IS_TEAM_BATTLE_MODE, true)
                    putInt(KEY_QUESTION_COUNT, DEFAULT_QUESTION_COUNT)

                    putString(KEY_PLAYER_NAME, currentUserName)
                    putString(KEY_PLAYER_ID, currentUserId)
                    putInt(KEY_PLAYER_AVATAR, currentUserAvatarRes)
                }

                val navController = findNavController()
                if (navController.currentDestination?.id == R.id.multiPlayerFriendFragment) {
                    navController.navigate(
                        R.id.action_multiPlayerFriendFragment_to_quizGameFragment,
                        bundle
                    )
                }
            }
        }.start()
    }

    private fun cancelCountdown() {
        if (!isRoomCreator) {
            showToast("Only the room creator can stop the countdown")
            return
        }

        countDownTimer?.cancel()
        isCountdownRunning = false

        binding.startFriendsMatchButton.text = "Start Match"
        binding.startFriendsMatchButton.isEnabled = true
        binding.startFriendsMatchButton.alpha = 1f

        binding.friendsSubtitleTextView.text =
            if (canStartGame()) "Teams are ready"
            else "Waiting for players to join"

        binding.friendsStartInfoTextView.text = "Start the match when both teams are ready"

        addSystemMessage("Room creator cancelled the countdown")
        showToast("Countdown cancelled")
    }

    private fun handleLeaveRoom() {
        if (isRoomCreator && (isCountdownRunning || hasGameStarted)) {
            showToast("Room creator cannot leave after starting the match")
            return
        }

        if (!isRoomCreator) {
            addSystemMessage("$currentUserName left the room")
        } else {
            addSystemMessage("Room creator left the room")
        }

        findNavController().popBackStack()
    }

    private fun updatePlayerCounts() = with(binding) {
        val teamACount = teamAPlayers.size
        val teamBCount = teamBPlayers.size

        teamAFriendsCountTextView.text = "$teamACount/$maxPlayersPerTeam"
        teamBFriendsCountTextView.text = "$teamBCount/$maxPlayersPerTeam"

        teamAFriendsPlayerCountTextView.text = "Player Count: $teamACount/$maxPlayersPerTeam"
        teamBFriendsPlayerCountTextView.text = "Player Count: $teamBCount/$maxPlayersPerTeam"
    }

    private fun updateAvatarSlots() = with(binding) {
        val teamASlots = listOf(
            teamAAvatarSlot1,
            teamAAvatarSlot2,
            teamAAvatarSlot3,
            teamAAvatarSlot4
        )

        val teamBSlots = listOf(
            teamBAvatarSlot1,
            teamBAvatarSlot2,
            teamBAvatarSlot3,
            teamBAvatarSlot4
        )

        updateTeamSlots(
            slots = teamASlots,
            players = teamAPlayers,
            teamColor = R.color.color_orange_primary
        )

        updateTeamSlots(
            slots = teamBSlots,
            players = teamBPlayers,
            teamColor = R.color.color_purple
        )
    }

    private fun updateTeamSlots(
        slots: List<ShapeableImageView>,
        players: List<RoomPlayer>,
        teamColor: Int
    ) {
        slots.forEachIndexed { index, imageView ->
            if (index >= maxPlayersPerTeam) {
                imageView.isVisible = false
                return@forEachIndexed
            }

            imageView.isVisible = true

            if (index < players.size) {
                val player = players[index]

                imageView.alpha = 1f
                imageView.setPadding(0, 0, 0, 0)
                imageView.scaleType = ImageView.ScaleType.CENTER_CROP
                imageView.background = null
                imageView.imageTintList = null
                imageView.strokeColor =
                    ContextCompat.getColorStateList(requireContext(), teamColor)
                imageView.strokeWidth = 2f

                val avatarRes = if (player.uid == currentUserId) {
                    currentUserAvatarRes
                } else {
                    getStableAvatarRes(player.uid)
                }

                imageView.setImageResource(avatarRes)
            } else {
                imageView.alpha = 0.60f
                imageView.scaleType = ImageView.ScaleType.CENTER_INSIDE
                imageView.setImageResource(R.drawable.ic_add)
                imageView.setPadding(10, 10, 10, 10)
                imageView.background =
                    ContextCompat.getDrawable(requireContext(), R.drawable.bg_white)
                imageView.imageTintList =
                    ContextCompat.getColorStateList(requireContext(), R.color.color_text_hint)
                imageView.strokeColor =
                    ContextCompat.getColorStateList(requireContext(), R.color.color_lavender)
                imageView.strokeWidth = 1f
            }
        }
    }

    private fun getStableAvatarRes(uid: String): Int {
        val avatars = listOf(
            R.drawable.avatar_1,
            R.drawable.avatar_2,
            R.drawable.avatar_3,
            R.drawable.avatar_4
        )
        val index = uid.hashCode().absoluteValue % avatars.size
        return avatars[index]
    }

    private fun updatePlayersSummary() = with(binding) {
        teamAPlayersSummaryTextView.text = if (teamAPlayers.isEmpty()) {
            "Waiting for players..."
        } else {
            teamAPlayers.joinToString(", ") { it.name.ifBlank { "Unknown" } }
        }

        teamBPlayersSummaryTextView.text = if (teamBPlayers.isEmpty()) {
            "Waiting for players..."
        } else {
            teamBPlayers.joinToString(", ") { it.name.ifBlank { "Unknown" } }
        }
    }

    private fun updateTeamUiState() = with(binding) {
        teamAYouBadgeCardView.isVisible = currentJoinedTeam == TEAM_A
        teamBYouBadgeCardView.isVisible = currentJoinedTeam == TEAM_B

        teamAStatusTextView.text = when {
            teamAPlayers.size >= maxPlayersPerTeam -> "Full"
            currentJoinedTeam == TEAM_A -> "Joined"
            else -> "Open"
        }

        teamBStatusTextView.text = when {
            teamBPlayers.size >= maxPlayersPerTeam -> "Full"
            currentJoinedTeam == TEAM_B -> "Joined"
            else -> "Open"
        }

        teamAFriendsCardView.strokeWidth = if (currentJoinedTeam == TEAM_A) 3 else 1
        teamBFriendsCardView.strokeWidth = if (currentJoinedTeam == TEAM_B) 3 else 1

        teamAJoinHintTextView.isVisible = currentJoinedTeam != TEAM_A
        teamBJoinHintTextView.isVisible = currentJoinedTeam != TEAM_B

        startFriendsMatchButton.isEnabled = isRoomCreator && !hasGameStarted
        startFriendsMatchButton.alpha = if (isRoomCreator && !hasGameStarted) 1f else 0.55f

        friendsTeamInstructionTextView.text = when {
            hasGameStarted -> "Game already started"
            isCountdownRunning -> "Countdown active"
            isRoomCreator -> "You are the room creator"
            else -> "Only room creator can start or stop the match"
        }
    }

    private fun setupCategoryChipIcon() {
        val iconRes = getCategoryIconRes(selectedCategory)

        binding.friendsCategoryChipTextView.apply {
            setCompoundDrawablesRelativeWithIntrinsicBounds(iconRes, 0, 0, 0)
            compoundDrawablePadding = 8
        }
    }

    private fun setupCategoryBackground() {
        val backgroundRes = getCategoryBackgroundRes(selectedCategory)
        binding.friendsBackgroundImageView.setImageResource(backgroundRes)
    }

    private fun getCategoryIconRes(category: String): Int {
        return when (category.trim().lowercase(Locale.ROOT)) {
            "informatics" -> R.drawable.ic_chip_informatics
            "mathematics", "math" -> R.drawable.ic_chip_math_pi
            "english" -> R.drawable.ic_chip_english
            "history" -> R.drawable.ic_chip_history
            "world", "worldview", "world_knowledge" -> R.drawable.ic_chip_knowledge
            "logic" -> R.drawable.ic_chip_logic
            else -> R.drawable.ic_chip_informatics
        }
    }

    private fun getCategoryBackgroundRes(category: String): Int {
        return when (category.trim().lowercase(Locale.ROOT)) {
            "informatics" -> R.drawable.bg_informatics_1
            "mathematics", "math" -> R.drawable.bg_math_1
            "english" -> R.drawable.bg_english_1
            "history" -> R.drawable.bg_history_1
            "world", "worldview", "world_knowledge" -> R.drawable.bg_world_1
            "logic" -> R.drawable.bg_logic_1
            else -> R.drawable.bg_informatics_1
        }
    }

    private fun copyRoomIdToClipboard() {
        val clipboard =
            requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager

        val clip = ClipData.newPlainText("Room ID", roomId)
        clipboard.setPrimaryClip(clip)

        showToast("Room ID copied")
    }

    private fun generateRoomId(): String {
        val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
        val randomPart = (1..4).map { chars.random() }.joinToString("")
        return "AEL-$randomPart"
    }

    private fun loadMockRoomState() {
        teamAPlayers.clear()
        teamBPlayers.clear()

        teamAPlayers.add(
            RoomPlayer(
                uid = if (isRoomCreator) currentUserId else "host_uid",
                name = roomCreatorName,
                avatarUrl = "",
                team = TEAM_A,
                isBot = false,
                correctCount = 0,
                wrongCount = 0,
                score = if (isRoomCreator) UserManager.currentUser.score else 1250,
                joinedAt = System.currentTimeMillis(),
                isReady = true
            )
        )

        teamBPlayers.add(
            RoomPlayer(
                uid = "player_b_1",
                name = "Jessica",
                avatarUrl = "",
                team = TEAM_B,
                isBot = false,
                correctCount = 0,
                wrongCount = 0,
                score = 1180,
                joinedAt = System.currentTimeMillis(),
                isReady = true
            )
        )

        if (isRoomCreator) {
            currentJoinedTeam = TEAM_A
        }

        updateAllUi()
        binding.friendsSubtitleTextView.text =
            if (canStartGame()) "Teams are ready" else "Waiting for players to join"

        addSystemMessage("1 player is currently in Team A")
        addSystemMessage("1 player is currently in Team B")
    }

    private fun getMaxPlayersPerTeam(format: String): Int {
        return when (format.trim().lowercase(Locale.ROOT)) {
            "2v2" -> 2
            "3v3" -> 3
            "4v4" -> 4
            else -> 4
        }
    }

    private fun formatCategoryName(category: String): String {
        return when (category.trim().lowercase(Locale.ROOT)) {
            "informatics" -> "Informatics"
            "mathematics", "math" -> "Mathematics"
            "english" -> "English"
            "history" -> "History"
            "world", "worldview", "world_knowledge" -> "Worldview"
            "logic" -> "Logic"
            else -> "Informatics"
        }
    }

    private fun isTeamChangeLocked(): Boolean {
        return isCountdownRunning || hasGameStarted
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

    private fun showToast(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }

    override fun onDestroyView() {
        countDownTimer?.cancel()
        countDownTimer = null
        _binding = null
        super.onDestroyView()
    }
}