package com.example.quizapp.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import android.widget.Toast
import androidx.activity.addCallback
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.quizapp.R
import com.example.quizapp.databinding.FragmentOtherPlayerProfileBinding
import com.example.quizapp.manager.UserManager
import com.example.quizapp.model.ChallengeRequest
import com.example.quizapp.model.User
import com.example.quizapp.model.UserStatus
import com.google.firebase.firestore.FirebaseFirestore
import java.util.UUID

class OtherPlayerProfileFragment : Fragment() {

    private var _binding: FragmentOtherPlayerProfileBinding? = null
    private val binding get() = _binding!!

    private val firestore = FirebaseFirestore.getInstance()

    private var playerName: String = ""
    private var playerId: String = ""
    private var playerScore: Int = 0
    private var totalGames: Int = 0
    private var correctAnswers: Int = 0
    private var winRate: String = ""
    private var status: String = ""
    private var playerStatus: UserStatus = UserStatus.OFFLINE
    private var playerAvatarResId: Int = R.drawable.avatar_1

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

    private fun safeOtherPlayerAvatar(): Int {
        return if (playerAvatarResId != 0) playerAvatarResId else R.drawable.ic_avatar_placeholder
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentOtherPlayerProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        UserManager.loadUser(requireContext())
        getPlayerDataFromArguments()
        setupBackNavigation()
        setupClicks()
        setupPressedEffects()
        bindPlayerData()
    }

    private fun setupBackNavigation() {
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner) {
            findNavController().navigateUp()
        }
    }

    private fun getPlayerDataFromArguments() {
        playerName = arguments?.getString(ARG_PLAYER_NAME).orEmpty().ifBlank { "Unknown Player" }
        playerId = arguments?.getString(ARG_PLAYER_ID).orEmpty().ifBlank { "AEL-0000" }
        playerScore = arguments?.getInt(ARG_PLAYER_SCORE, 0) ?: 0
        totalGames = arguments?.getInt(ARG_TOTAL_GAMES, 0) ?: 0
        correctAnswers = arguments?.getInt(ARG_CORRECT_ANSWERS, 0) ?: 0
        winRate = arguments?.getString(ARG_WIN_RATE).orEmpty().ifBlank { "0%" }
        status = arguments?.getString(ARG_STATUS).orEmpty().ifBlank { "offline" }
        playerStatus = when (status.trim().lowercase()) {
            "online" -> UserStatus.ONLINE
            "offline" -> UserStatus.OFFLINE
            "in_game", "in game" -> UserStatus.IN_GAME
            else -> UserStatus.OFFLINE
        }
        playerAvatarResId = arguments?.getInt(ARG_PLAYER_AVATAR, R.drawable.avatar_1)
            ?: R.drawable.avatar_1

        if (playerAvatarResId == 0) {
            playerAvatarResId = R.drawable.ic_avatar_placeholder
        }
    }

    private fun setupClicks() = with(binding) {
        backImageView.setOnClickListener {
            findNavController().navigateUp()
        }

        moreOptionsImageView.setOnClickListener { anchorView ->
            showMoreOptionsMenu(anchorView)
        }

        addFriendButtonCardView.setOnClickListener {
            showToast("Friend request sent to $playerName")
        }

        challengePlayerButtonCardView.setOnClickListener {
            sendChallengeRequest()
        }

        otherPlayerIdTextView.setOnClickListener {
            copyPlayerIdToClipboard()
        }
    }

    private fun setupPressedEffects() = with(binding) {
        addPressedEffect(backImageView)
        addPressedEffect(moreOptionsImageView)
        addPressedEffect(addFriendButtonCardView)
        addPressedEffect(challengePlayerButtonCardView)
        addPressedEffect(otherPlayerIdTextView)
        addPressedEffect(otherPlayerAvatarImageView)
    }

    private fun bindPlayerData() = with(binding) {
        otherPlayerNameTextView.text = playerName
        otherPlayerIdTextView.text = "ID: $playerId"
        otherPlayerScoreTextView.text = "$playerScore XP"
        totalGamesValueTextView.text = totalGames.toString()
        correctAnswersValueTextView.text = correctAnswers.toString()
        winRateValueTextView.text = winRate
        otherPlayerAvatarImageView.setImageResource(safeOtherPlayerAvatar())

        applyStatus(playerStatus)
        challengePlayerButtonCardView.visibility = View.VISIBLE
        setChallengeButtonEnabled(true)
    }

    private fun sendChallengeRequest() {
        val currentUserId = safeCurrentUserId()

        if (currentUserId == playerId) {
            showToast("You cannot challenge yourself")
            return
        }

        setChallengeButtonEnabled(false)

        firestore.collection("challenge_requests")
            .whereEqualTo("fromPlayerId", currentUserId)
            .whereEqualTo("toPlayerId", playerId)
            .whereEqualTo("status", "pending")
            .get()
            .addOnSuccessListener { result ->
                if (!isAdded || _binding == null) return@addOnSuccessListener

                if (!result.isEmpty) {
                    setChallengeButtonEnabled(true)
                    showToast("Challenge already sent to $playerName")
                    return@addOnSuccessListener
                }

                checkOpponentStatusAndProceed(UserManager.currentUser)
            }
            .addOnFailureListener { exception ->
                if (!isAdded || _binding == null) return@addOnFailureListener
                setChallengeButtonEnabled(true)
                showToast("Failed to check existing challenge: ${exception.message}")
            }
    }

    private fun checkOpponentStatusAndProceed(currentUser: User) {
        val opponentStatus = status.trim().lowercase()

        when (opponentStatus) {
            "online" -> {
                createNewChallengeRequest(currentUser)
            }

            "in game", "in_game" -> {
                setChallengeButtonEnabled(true)
                showToast("$playerName is currently in a game. Send the request after the match ends.")
            }

            "in challenge room", "in_challenge_room" -> {
                setChallengeButtonEnabled(true)
                showToast("$playerName is currently in a challenge room.")
            }

            "searching", "searching_match", "matching" -> {
                setChallengeButtonEnabled(true)
                showToast("$playerName is currently searching for a match.")
            }

            "offline" -> {
                setChallengeButtonEnabled(true)
                showToast("$playerName is offline right now.")
            }

            else -> {
                setChallengeButtonEnabled(true)
                showToast("$playerName is not available for challenge right now.")
            }
        }
    }

    private fun createNewChallengeRequest(currentUser: User) {
        val requestId = UUID.randomUUID().toString()

        val challengeRequest = ChallengeRequest(
            requestId = requestId,
            fromPlayerId = safeCurrentUserId(),
            fromPlayerName = safeCurrentUserName(),
            fromPlayerAvatar = safeCurrentUserAvatar(),
            toPlayerId = playerId,
            toPlayerName = playerName,
            toPlayerAvatar = safeOtherPlayerAvatar(),
            category = "Random",
            mode = "1 vs 1",
            status = "pending"
        )

        firestore.collection("challenge_requests")
            .document(requestId)
            .set(challengeRequest)
            .addOnSuccessListener {
                if (!isAdded || _binding == null) return@addOnSuccessListener
                setChallengeButtonEnabled(true)
                showToast("Challenge sent to $playerName")
            }
            .addOnFailureListener { exception ->
                if (!isAdded || _binding == null) return@addOnFailureListener
                setChallengeButtonEnabled(true)
                showToast("Failed to send challenge: ${exception.message}")
            }
    }

    private fun setChallengeButtonEnabled(enabled: Boolean) = with(binding) {
        challengePlayerButtonCardView.isEnabled = enabled
        challengePlayerButtonCardView.alpha = if (enabled) 1f else 0.6f
        challengePlayerTextView.alpha = if (enabled) 1f else 0.7f
        challengePlayerIconImageView.alpha = if (enabled) 1f else 0.7f
    }

    private fun applyStatus(status: UserStatus) {
        binding.otherPlayerStatusTextView.text = when (status) {
            UserStatus.ONLINE -> "Online"
            UserStatus.OFFLINE -> "Offline"
            UserStatus.IN_GAME -> "In Game"
        }

        val statusColor = when (status) {
            UserStatus.ONLINE -> R.color.color_green
            UserStatus.OFFLINE -> R.color.color_text_hint
            UserStatus.IN_GAME -> R.color.color_orange_primary
        }

        binding.otherPlayerStatusTextView.setTextColor(
            ContextCompat.getColor(requireContext(), statusColor)
        )
    }

    private fun showMoreOptionsMenu(anchorView: View) {
        val popupMenu = PopupMenu(requireContext(), anchorView)
        popupMenu.menuInflater.inflate(R.menu.menu_other_player_profile_options, popupMenu.menu)

        popupMenu.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.menu_copy_player_id -> {
                    copyPlayerIdToClipboard()
                    true
                }

                R.id.menu_share_profile -> {
                    sharePlayerProfile()
                    true
                }

                else -> false
            }
        }

        popupMenu.show()
    }

    private fun copyPlayerIdToClipboard() {
        val clipboardManager =
            requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager

        val clipData = ClipData.newPlainText("Player ID", playerId)
        clipboardManager.setPrimaryClip(clipData)

        showToast("Player ID copied")
    }

    private fun sharePlayerProfile() {
        val shareText = """
            Check out this QuizApp player profile!
            
            Name: $playerName
            ID: $playerId
            Score: $playerScore XP
        """.trimIndent()

        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, shareText)
        }

        startActivity(Intent.createChooser(shareIntent, "Share Profile"))
    }

    private fun addPressedEffect(targetView: View) {
        targetView.isClickable = true
        targetView.isFocusable = true

        targetView.setOnTouchListener { v, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    if (v.isEnabled) {
                        v.animate()
                            .alpha(0.86f)
                            .scaleX(0.97f)
                            .scaleY(0.97f)
                            .setDuration(90)
                            .start()
                    }
                }

                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    v.animate()
                        .alpha(if (v.isEnabled) 1f else 0.6f)
                        .scaleX(1f)
                        .scaleY(1f)
                        .setDuration(90)
                        .start()
                }
            }
            false
        }
    }

    private fun showToast(message: String) {
        if (isAdded) {
            Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val ARG_PLAYER_NAME = "playerName"
        const val ARG_PLAYER_ID = "playerId"
        const val ARG_PLAYER_SCORE = "playerScore"
        const val ARG_TOTAL_GAMES = "totalGames"
        const val ARG_CORRECT_ANSWERS = "correctAnswers"
        const val ARG_WIN_RATE = "winRate"
        const val ARG_STATUS = "status"
        const val ARG_PLAYER_AVATAR = "playerAvatar"
    }
}