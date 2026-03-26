package com.example.quizapp.ui

import android.os.Bundle
import android.os.CountDownTimer
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import androidx.activity.addCallback
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.quizapp.R
import com.example.quizapp.databinding.FragmentSinglePlayerMatchmakingBinding
import com.example.quizapp.manager.UserManager

class SinglePlayerMatchmakingFragment : Fragment() {

    private var _binding: FragmentSinglePlayerMatchmakingBinding? = null
    private val binding get() = _binding!!

    private var timer: CountDownTimer? = null
    private val searchTime = 45_000L
    private val interval = 1_000L

    private var selectedCategory = "informatics"
    private var selectedDifficulty = "easy"
    private var selectedQuestionCount = 10

    private val demoOpponentName = "Jessica"
    private val demoOpponentId = "AEL-54231"
    private val demoOpponentAvatar = R.drawable.avatar_4

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

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSinglePlayerMatchmakingBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        UserManager.loadUser(requireContext())

        setupBackNavigation()
        readBundleData()
        setupUI()
        bindCurrentUser()
        setupClickListeners()
        setupPressedEffects()
        startSearchingTimer()

        UserManager.userLiveData.observe(viewLifecycleOwner) {
            if (!isAdded || _binding == null) return@observe
            bindCurrentUser()
        }
    }

    override fun onResume() {
        super.onResume()
        bindCurrentUser()
    }

    private fun setupBackNavigation() {
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner) {
            timer?.cancel()
            findNavController().navigateUp()
        }
    }

    private fun readBundleData() {
        arguments?.let {
            selectedCategory = it.getString("category", "informatics")
                ?.trim()
                ?.ifBlank { "informatics" }
                ?: "informatics"

            selectedDifficulty = it.getString("difficulty", "easy")
                ?.trim()
                ?.ifBlank { "easy" }
                ?: "easy"

            selectedQuestionCount = it.getInt("questionCount", 10)
        }
    }

    private fun setupUI() = with(binding) {
        categoryValueTextView.text = formatCategoryName(selectedCategory)
        difficultyValueTextView.text = formatDifficultyName(selectedDifficulty)
        questionCountValueTextView.text = selectedQuestionCount.toString()

        noOpponentContainer.isVisible = false
        searchingStatusTextView.text = "Searching for opponent..."
        timerTextView.text = "45"
        matchmakingTimerProgressIndicator.max = 45
        matchmakingTimerProgressIndicator.progress = 45

        opponentStatusTextView.text = "Searching..."
        opponentAvatarImageView.setImageResource(R.drawable.ic_avatar_placeholder)
    }

    private fun bindCurrentUser() = with(binding) {
        youAvatarImageView.setImageResource(safeCurrentUserAvatar())
        youNameTextView.text = safeCurrentUserName()
    }

    private fun setupClickListeners() = with(binding) {
        cancelSearchButton.setOnClickListener {
            timer?.cancel()
            findNavController().navigateUp()
        }

        playVsComputerButton.setOnClickListener {
            timer?.cancel()
            openQuiz(isBot = true)
        }

        continueSearchingButton.setOnClickListener {
            resetSearchUi()
            startSearchingTimer()
        }
    }

    private fun setupPressedEffects() = with(binding) {
        addPressedEffect(cancelSearchButton)
        addPressedEffect(playVsComputerButton)
        addPressedEffect(continueSearchingButton)

        addPressedEffect(youAvatarImageView)
        addPressedEffect(opponentAvatarImageView)
    }

    private fun resetSearchUi() = with(binding) {
        timer?.cancel()
        noOpponentContainer.isVisible = false
        searchingStatusTextView.text = "Searching for opponent..."
        timerTextView.text = "45"
        matchmakingTimerProgressIndicator.progress = 45
        opponentStatusTextView.text = "Searching..."
        opponentAvatarImageView.setImageResource(R.drawable.ic_avatar_placeholder)
    }

    private fun startSearchingTimer() {
        timer?.cancel()

        timer = object : CountDownTimer(searchTime, interval) {

            override fun onTick(millisUntilFinished: Long) {
                val seconds = (millisUntilFinished / 1000L).toInt()

                binding.timerTextView.text = seconds.toString()
                binding.matchmakingTimerProgressIndicator.progress = seconds

                if (seconds == 35) {
                    opponentFound()
                    cancel()
                }
            }

            override fun onFinish() {
                binding.timerTextView.text = "0"
                binding.matchmakingTimerProgressIndicator.progress = 0
                searchFinished()
            }
        }.start()
    }

    private fun opponentFound() = with(binding) {
        searchingStatusTextView.text = "Opponent Found!"
        opponentStatusTextView.text = demoOpponentName
        opponentAvatarImageView.setImageResource(
            if (demoOpponentAvatar != 0) demoOpponentAvatar else R.drawable.ic_avatar_placeholder
        )

        timerTextView.text = "0"
        matchmakingTimerProgressIndicator.progress = 0

        root.postDelayed({
            if (isAdded && _binding != null) {
                openQuiz(isBot = false)
            }
        }, 2000)
    }

    private fun searchFinished() = with(binding) {
        searchingStatusTextView.text = "No opponent found"
        noOpponentContainer.isVisible = true
    }

    private fun openQuiz(isBot: Boolean) {
        val safePlayerName = safeCurrentUserName()
        val safePlayerId = safeCurrentUserId()
        val safePlayerAvatar = safeCurrentUserAvatar()

        val bundle = Bundle().apply {
            putString("gameMode", "online_duel")
            putString("category", selectedCategory)
            putString("difficulty", selectedDifficulty)
            putInt("questionCount", selectedQuestionCount)

            putBoolean("isBotMatch", isBot)
            putBoolean("isOnlineDuel", true)

            putString("playerName", safePlayerName)
            putString("playerId", safePlayerId)
            putInt("playerAvatar", safePlayerAvatar)

            putString("opponentName", if (isBot) "AI Player" else demoOpponentName)
            putString("opponentId", if (isBot) "BOT-0001" else demoOpponentId)
            putInt(
                "opponentAvatar",
                if (isBot) R.drawable.avatar_2 else demoOpponentAvatar
            )
        }

        findNavController().navigate(
            R.id.action_singlePlayerMatchmakingFragment_to_quizGameFragment,
            bundle
        )
    }

    private fun formatCategoryName(category: String): String {
        return when (category.trim().lowercase()) {
            "informatics" -> "Informatics"
            "mathematics", "math" -> "Mathematics"
            "english" -> "English"
            "history" -> "History"
            "logic" -> "Logic"
            "world", "worldview", "world_knowledge" -> "World Knowledge"
            else -> "Quiz"
        }
    }

    private fun formatDifficultyName(difficulty: String): String {
        return when (difficulty.trim().lowercase()) {
            "easy" -> "Easy"
            "medium" -> "Medium"
            "hard" -> "Hard"
            else -> "Easy"
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
        timer?.cancel()
        timer = null
        _binding = null
        super.onDestroyView()
    }
}