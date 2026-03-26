package com.example.quizapp.ui

import android.os.Bundle
import android.os.CountDownTimer
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.addCallback
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.quizapp.R
import com.example.quizapp.databinding.FragmentChallengeRoomBinding
import com.example.quizapp.manager.UserManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlin.math.abs

class ChallengeRoomFragment : Fragment() {

    private var _binding: FragmentChallengeRoomBinding? = null
    private val binding get() = _binding!!

    private val firestore = FirebaseFirestore.getInstance()
    private var roomListener: ListenerRegistration? = null

    private var challengeCountDownTimer: CountDownTimer? = null
    private var roomCloseCountDownTimer: CountDownTimer? = null

    private var remainingTimeMillis: Long = 600_000L

    private var isCurrentPlayerReady = false
    private var isOpponentReady = false
    private var hasMatchStarted = false
    private var isLeavingIntentionally = false

    private var roomId: String = ""
    private var opponentName: String = "Unknown Player"
    private var opponentId: String = ""
    private var selectedMode: String = "1 vs 1"
    private var selectedCategory: String = "random"
    private var opponentAvatarRes: Int = R.drawable.ic_avatar_placeholder

    private var lastShownRoomMessage: String? = null
    private var hasScheduledExit = false
    private var hasShownTerminalDialog = false
    private var currentRoomStatus: String = "waiting"

    private val ROOM_CLOSE_DELAY_MILLIS = 120_000L
    private val ROOM_JOIN_TIMEOUT_MILLIS = 600_000L

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
        _binding = FragmentChallengeRoomBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        UserManager.loadUser(requireContext())
        getChallengeArguments()
        setupBackNavigation()
        setupInitialUI()
        setupClickListeners()
        setupPressedEffects()
        markCurrentPlayerAsJoined()
        listenToChallengeRoom()
    }

    private fun setupBackNavigation() {
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner) {
            if (!hasMatchStarted) {
                leaveRoom("left_room")
            }
        }
    }

    private fun getChallengeArguments() {
        arguments?.let { bundle ->
            roomId = bundle.getString("roomId").orEmpty()

            opponentName = bundle.getString("opponentName", opponentName)
                ?.trim()
                ?.ifBlank { opponentName }
                ?: opponentName

            opponentId = bundle.getString("opponentId", opponentId)
                ?.trim()
                ?.ifBlank { opponentId }
                ?: opponentId

            selectedMode = bundle.getString("selectedMode", selectedMode)
                ?.trim()
                ?.ifBlank { selectedMode }
                ?: selectedMode

            selectedCategory = bundle.getString("selectedCategory", selectedCategory)
                ?.trim()
                ?.ifBlank { selectedCategory }
                ?: selectedCategory

            opponentAvatarRes = bundle.getInt(
                "opponentAvatar",
                R.drawable.ic_avatar_placeholder
            )

            if (opponentAvatarRes == 0) {
                opponentAvatarRes = R.drawable.ic_avatar_placeholder
            }
        }
    }

    private fun setupInitialUI() = with(binding) {
        opponentNameTextView.text = opponentName
        opponentIdTextView.text = "ID: $opponentId"
        opponentAvatarImageView.setImageResource(
            if (opponentAvatarRes != 0) opponentAvatarRes else R.drawable.ic_avatar_placeholder
        )

        challengeModeValueTextView.text = selectedMode
        challengeCategoryValueTextView.text = formatCategoryName(selectedCategory)

        updateChallengeStatus()
        updatePlayerStatuses()
        updateTimerText(remainingTimeMillis)
        setRoomActionButtonsEnabled(
            startEnabled = false,
            cancelEnabled = true
        )
    }

    private fun setupClickListeners() = with(binding) {
        backImageView.setOnClickListener {
            if (!hasMatchStarted) {
                leaveRoom("left_room")
            }
        }

        cancelChallengeButton.setOnClickListener {
            if (!hasMatchStarted) {
                cancelChallengeRoom()
            }
        }

        startChallengeMatchButton.setOnClickListener {
            onStartMatchClicked()
        }
    }

    private fun setupPressedEffects() = with(binding) {
        addPressedEffect(backImageView)
        addPressedEffect(cancelChallengeButton)
        addPressedEffect(startChallengeMatchButton)
        addPressedEffect(opponentAvatarImageView)
    }

    private fun markCurrentPlayerAsJoined() {
        if (roomId.isBlank()) return

        val currentUserId = safeCurrentUserId()

        firestore.collection("challenge_rooms")
            .document(roomId)
            .get()
            .addOnSuccessListener { document ->
                if (!document.exists()) return@addOnSuccessListener

                val roomStatus = document.getString("status").orEmpty()
                if (isTerminalRoomStatus(roomStatus) || roomStatus.equals("started", ignoreCase = true)) {
                    return@addOnSuccessListener
                }

                val hostPlayerId = document.getString("hostPlayerId").orEmpty()
                val guestPlayerId = document.getString("guestPlayerId").orEmpty()

                when (currentUserId) {
                    hostPlayerId -> {
                        firestore.collection("challenge_rooms")
                            .document(roomId)
                            .update(
                                mapOf(
                                    "hostJoined" to true,
                                    "hostState" to "in_room",
                                    "lastActionAt" to System.currentTimeMillis()
                                )
                            )
                    }

                    guestPlayerId -> {
                        firestore.collection("challenge_rooms")
                            .document(roomId)
                            .update(
                                mapOf(
                                    "guestJoined" to true,
                                    "guestState" to "in_room",
                                    "lastActionAt" to System.currentTimeMillis()
                                )
                            )
                    }
                }
            }
    }

    private fun listenToChallengeRoom() {
        if (roomId.isBlank()) {
            showToast("Room ID not found")
            return
        }

        roomListener?.remove()
        roomListener = firestore.collection("challenge_rooms")
            .document(roomId)
            .addSnapshotListener { snapshot, exception ->
                if (exception != null) {
                    showToast("Failed to listen room: ${exception.message}")
                    return@addSnapshotListener
                }

                if (snapshot == null || !snapshot.exists()) {
                    if (!isLeavingIntentionally && !hasShownTerminalDialog) {
                        hasShownTerminalDialog = true
                        showInfoDialog(
                            title = "Room Closed",
                            message = "Challenge room no longer exists."
                        ) {
                            safeExitToHome()
                        }
                    }
                    return@addSnapshotListener
                }

                val currentUserId = safeCurrentUserId()

                val hostPlayerId = snapshot.getString("hostPlayerId").orEmpty()
                val guestPlayerId = snapshot.getString("guestPlayerId").orEmpty()

                val hostJoined = snapshot.getBoolean("hostJoined") ?: false
                val guestJoined = snapshot.getBoolean("guestJoined") ?: false
                val roomStatus = snapshot.getString("status").orEmpty()
                val createdAt = snapshot.getLong("createdAt") ?: System.currentTimeMillis()

                currentRoomStatus = roomStatus

                val hostState = snapshot.getString("hostState").orEmpty().ifBlank { "waiting" }
                val guestState = snapshot.getString("guestState").orEmpty().ifBlank { "waiting" }
                val cancelReason = snapshot.getString("cancelReason").orEmpty()
                val cancelledBy = snapshot.getString("cancelledBy").orEmpty()
                val closeAt = snapshot.getLong("closeAt") ?: 0L

                selectedMode = snapshot.getString("mode").orEmpty().ifBlank { selectedMode }
                selectedCategory =
                    snapshot.getString("category").orEmpty().ifBlank { selectedCategory }

                if (!hasMatchStarted && !isTerminalRoomStatus(roomStatus) && !roomStatus.equals("started", ignoreCase = true)) {
                    val syncedRemainingTime = (ROOM_JOIN_TIMEOUT_MILLIS - (System.currentTimeMillis() - createdAt))
                        .coerceAtLeast(0L)

                    if (challengeCountDownTimer == null || abs(remainingTimeMillis - syncedRemainingTime) > 1500L) {
                        remainingTimeMillis = syncedRemainingTime
                        updateTimerText(remainingTimeMillis)
                        startChallengeTimer()
                    }
                }

                if (currentUserId == hostPlayerId) {
                    isCurrentPlayerReady = hostJoined
                    isOpponentReady = guestJoined
                } else if (currentUserId == guestPlayerId) {
                    isCurrentPlayerReady = guestJoined
                    isOpponentReady = hostJoined
                }

                updatePlayerStatuses()
                updateChallengeStatus()

                if (hostJoined && guestJoined && roomStatus.equals("waiting", ignoreCase = true)) {
                    firestore.collection("challenge_rooms")
                        .document(roomId)
                        .update(
                            mapOf(
                                "status" to "ready",
                                "lastActionAt" to System.currentTimeMillis()
                            )
                        )
                }

                val opponentState = if (currentUserId == hostPlayerId) guestState else hostState

                when {
                    roomStatus.equals("started", ignoreCase = true) -> {
                        disableRoomActions()
                        if (!hasMatchStarted) {
                            startMatch()
                        }
                    }

                    roomStatus.equals("cancelled", ignoreCase = true) -> {
                        disableRoomActions()

                        if (isLeavingIntentionally) return@addSnapshotListener
                        if (hasShownTerminalDialog) return@addSnapshotListener

                        hasShownTerminalDialog = true

                        val cancelMessage = when (cancelReason) {
                            "host_cancelled", "guest_cancelled", "cancelled" ->
                                "$opponentName cancelled the challenge."

                            "host_left_room", "guest_left_room", "left_room" ->
                                "$opponentName left the challenge room."

                            "host_started_game", "guest_started_game" ->
                                "$opponentName started another game."

                            else ->
                                "Challenge was cancelled."
                        }

                        if (cancelledBy == currentUserId) {
                            showInfoDialog(
                                title = "Challenge Cancelled",
                                message = "You cancelled the challenge."
                            ) {
                                safeExitToHome()
                            }
                        } else {
                            showInfoDialog(
                                title = "Challenge Ended",
                                message = cancelMessage
                            ) {
                                safeExitToHome()
                            }
                        }

                        scheduleRoomExit(closeAt)
                    }

                    roomStatus.equals("expired", ignoreCase = true) -> {
                        disableRoomActions()

                        if (hasShownTerminalDialog) return@addSnapshotListener

                        hasShownTerminalDialog = true
                        showInfoDialog(
                            title = "Challenge Expired",
                            message = "Challenge time expired before both players were ready."
                        ) {
                            safeExitToHome()
                        }
                        scheduleRoomExit(System.currentTimeMillis() + 3000L)
                    }

                    opponentState == "left_room" -> {
                        showSingleRoomMessage(
                            title = "Opponent Left",
                            message = "$opponentName left the room."
                        )
                    }

                    opponentState == "in_game" -> {
                        disableRoomActions()

                        if (hasShownTerminalDialog) return@addSnapshotListener

                        hasShownTerminalDialog = true
                        showInfoDialog(
                            title = "Challenge Cancelled",
                            message = "$opponentName started another game."
                        ) {
                            safeExitToHome()
                        }
                        scheduleRoomExit(closeAt)
                    }

                    roomStatus.equals("waiting", ignoreCase = true) -> {
                        binding.challengeStatusTextView.text = "Waiting for players"
                    }

                    roomStatus.equals("ready", ignoreCase = true) -> {
                        binding.challengeStatusTextView.text = "Both players ready"
                        setRoomActionButtonsEnabled(
                            startEnabled = true,
                            cancelEnabled = true
                        )
                    }
                }
            }
    }

    private fun startChallengeTimer() {
        challengeCountDownTimer?.cancel()

        challengeCountDownTimer = object : CountDownTimer(remainingTimeMillis, 1000L) {
            override fun onTick(millisUntilFinished: Long) {
                remainingTimeMillis = millisUntilFinished
                updateTimerText(millisUntilFinished)
            }

            override fun onFinish() {
                remainingTimeMillis = 0L
                updateTimerText(0L)

                if (hasMatchStarted) return

                if (isCurrentPlayerReady && isOpponentReady && currentRoomStatus.equals("ready", ignoreCase = true)) {
                    updateRoomStatusToStarted()
                } else {
                    binding.challengeStatusTextView.text = "Challenge expired"
                    binding.challengeTimerTitleTextView.text = "Time is over"
                    binding.challengeTimerSubtitleTextView.text =
                        "Both players were not ready in time"
                    setRoomActionButtonsEnabled(
                        startEnabled = false,
                        cancelEnabled = true
                    )

                    if (roomId.isNotBlank()) {
                        firestore.collection("challenge_rooms")
                            .document(roomId)
                            .update(
                                mapOf(
                                    "status" to "expired",
                                    "cancelReason" to "timeout",
                                    "lastActionAt" to System.currentTimeMillis()
                                )
                            )
                    }
                }
            }
        }.start()
    }

    private fun updateTimerText(timeMillis: Long) {
        val totalSeconds = timeMillis / 1000L
        val minutes = totalSeconds / 60L
        val seconds = totalSeconds % 60L
        binding.challengeTimerTextView.text = String.format("%02d:%02d", minutes, seconds)
    }

    private fun updateChallengeStatus() = with(binding) {
        when {
            currentRoomStatus.equals("ready", ignoreCase = true) && isCurrentPlayerReady && isOpponentReady -> {
                challengeStatusTextView.text = "Both players ready"
                challengeTimerTitleTextView.text = "Match starts soon"
                challengeTimerSubtitleTextView.text = "Both players are ready to begin"
                setRoomActionButtonsEnabled(
                    startEnabled = true,
                    cancelEnabled = true
                )
            }

            isCurrentPlayerReady && !isOpponentReady -> {
                challengeStatusTextView.text = "Waiting for player"
                challengeTimerTitleTextView.text = "Match starts soon"
                challengeTimerSubtitleTextView.text = "Waiting for opponent to be ready"
                setRoomActionButtonsEnabled(
                    startEnabled = false,
                    cancelEnabled = true
                )
            }

            else -> {
                challengeStatusTextView.text = "Get ready"
                challengeTimerTitleTextView.text = "Waiting for readiness"
                challengeTimerSubtitleTextView.text = "Confirm your readiness to continue"
                setRoomActionButtonsEnabled(
                    startEnabled = false,
                    cancelEnabled = true
                )
            }
        }
    }

    private fun updatePlayerStatuses() {
        if (isCurrentPlayerReady) {
            binding.currentPlayerStatusValueTextView.text = "Ready"
            binding.currentPlayerStatusValueTextView.setTextColor(
                requireContext().getColor(R.color.color_success)
            )
        } else {
            binding.currentPlayerStatusValueTextView.text = "Not Ready"
            binding.currentPlayerStatusValueTextView.setTextColor(
                requireContext().getColor(R.color.color_error)
            )
        }

        if (isOpponentReady) {
            binding.opponentPlayerStatusValueTextView.text = "Ready"
            binding.opponentPlayerStatusValueTextView.setTextColor(
                requireContext().getColor(R.color.color_success)
            )
        } else {
            binding.opponentPlayerStatusValueTextView.text = "Waiting"
            binding.opponentPlayerStatusValueTextView.setTextColor(
                requireContext().getColor(R.color.color_orange_primary)
            )
        }
    }

    private fun onStartMatchClicked() {
        if (hasMatchStarted) return

        if (!isCurrentPlayerReady || !isOpponentReady || !currentRoomStatus.equals("ready", ignoreCase = true)) {
            showToast("Both players must be ready first")
            return
        }

        updateRoomStatusToStarted()
    }

    private fun updateRoomStatusToStarted() {
        if (roomId.isBlank()) return

        firestore.collection("challenge_rooms")
            .document(roomId)
            .update(
                mapOf(
                    "status" to "started",
                    "lastActionAt" to System.currentTimeMillis()
                )
            )
    }

    private fun startMatch() {
        if (hasMatchStarted) return
        hasMatchStarted = true

        challengeCountDownTimer?.cancel()
        roomCloseCountDownTimer?.cancel()

        showToast("Starting challenge match...")

        val bundle = Bundle().apply {
            putString("gameMode", "challenge")
            putString("selectedCategory", selectedCategory.lowercase())
            putString("selectedMode", selectedMode)
            putString("opponentName", opponentName)
            putString("opponentId", opponentId)
            putInt("opponentAvatar", opponentAvatarRes)
            putBoolean("isOnlineDuel", true)

            putString("playerName", safeCurrentUserName())
            putString("playerId", safeCurrentUserId())
            putInt("playerAvatar", safeCurrentUserAvatar())

            putString("roomId", roomId)
        }

        findNavController().navigate(
            R.id.action_challengeRoomFragment_to_quizGameFragment,
            bundle
        )
    }

    private fun cancelChallengeRoom() {
        challengeCountDownTimer?.cancel()

        if (roomId.isBlank()) {
            safeExitToHome()
            return
        }

        isLeavingIntentionally = true
        val currentUserId = safeCurrentUserId()

        firestore.collection("challenge_rooms")
            .document(roomId)
            .get()
            .addOnSuccessListener { document ->
                if (!document.exists()) {
                    safeExitToHome()
                    return@addOnSuccessListener
                }

                val hostPlayerId = document.getString("hostPlayerId").orEmpty()
                val guestPlayerId = document.getString("guestPlayerId").orEmpty()

                val reason = when (currentUserId) {
                    hostPlayerId -> "host_cancelled"
                    guestPlayerId -> "guest_cancelled"
                    else -> "cancelled"
                }

                val closeAt = System.currentTimeMillis() + ROOM_CLOSE_DELAY_MILLIS

                val updates = mutableMapOf<String, Any>(
                    "status" to "cancelled",
                    "cancelledBy" to currentUserId,
                    "cancelReason" to reason,
                    "lastActionAt" to System.currentTimeMillis(),
                    "closeAt" to closeAt
                )

                if (currentUserId == hostPlayerId) {
                    updates["hostState"] = "cancelled"
                } else if (currentUserId == guestPlayerId) {
                    updates["guestState"] = "cancelled"
                }

                firestore.collection("challenge_rooms")
                    .document(roomId)
                    .update(updates)
                    .addOnSuccessListener {
                        safeExitToHome()
                    }
                    .addOnFailureListener { exception ->
                        showToast("Failed to cancel challenge: ${exception.message}")
                        safeExitToHome()
                    }
            }
            .addOnFailureListener { exception ->
                showToast("Failed to load room: ${exception.message}")
                safeExitToHome()
            }
    }

    private fun leaveRoom(leaveState: String) {
        if (roomId.isBlank()) {
            safeExitToHome()
            return
        }

        isLeavingIntentionally = true
        challengeCountDownTimer?.cancel()

        val currentUserId = safeCurrentUserId()

        firestore.collection("challenge_rooms")
            .document(roomId)
            .get()
            .addOnSuccessListener { document ->
                if (!document.exists()) {
                    safeExitToHome()
                    return@addOnSuccessListener
                }

                val hostPlayerId = document.getString("hostPlayerId").orEmpty()
                val guestPlayerId = document.getString("guestPlayerId").orEmpty()
                val closeAt = System.currentTimeMillis() + ROOM_CLOSE_DELAY_MILLIS

                val updates = mutableMapOf<String, Any>(
                    "status" to "cancelled",
                    "cancelledBy" to currentUserId,
                    "lastActionAt" to System.currentTimeMillis(),
                    "closeAt" to closeAt
                )

                when (currentUserId) {
                    hostPlayerId -> {
                        updates["hostState"] = leaveState
                        updates["cancelReason"] =
                            if (leaveState == "in_game") "host_started_game" else "host_left_room"
                    }

                    guestPlayerId -> {
                        updates["guestState"] = leaveState
                        updates["cancelReason"] =
                            if (leaveState == "in_game") "guest_started_game" else "guest_left_room"
                    }
                }

                firestore.collection("challenge_rooms")
                    .document(roomId)
                    .update(updates)
                    .addOnSuccessListener {
                        safeExitToHome()
                    }
                    .addOnFailureListener {
                        safeExitToHome()
                    }
            }
            .addOnFailureListener {
                safeExitToHome()
            }
    }

    private fun setRoomActionButtonsEnabled(
        startEnabled: Boolean,
        cancelEnabled: Boolean
    ) = with(binding) {
        startChallengeMatchButton.isEnabled = startEnabled
        startChallengeMatchButton.alpha = if (startEnabled) 1f else 0.6f

        cancelChallengeButton.isEnabled = cancelEnabled
        cancelChallengeButton.alpha = if (cancelEnabled) 1f else 0.6f
    }

    private fun disableRoomActions() {
        setRoomActionButtonsEnabled(
            startEnabled = false,
            cancelEnabled = false
        )
    }

    private fun scheduleRoomExit(closeAt: Long) {
        if (hasScheduledExit) return

        val delay = if (closeAt > 0L) {
            closeAt - System.currentTimeMillis()
        } else {
            3000L
        }

        val safeDelay = if (delay <= 0L) 1000L else delay
        hasScheduledExit = true

        roomCloseCountDownTimer?.cancel()
        roomCloseCountDownTimer = object : CountDownTimer(safeDelay, 1000L) {
            override fun onTick(millisUntilFinished: Long) = Unit

            override fun onFinish() {
                if (!isLeavingIntentionally) {
                    safeExitToHome()
                }
            }
        }.start()
    }

    private fun showSingleRoomMessage(title: String, message: String) {
        if (!isAdded) return

        val uniqueKey = "$title|$message"
        if (lastShownRoomMessage == uniqueKey) return
        lastShownRoomMessage = uniqueKey

        showInfoDialog(title, message, null)
    }

    private fun showInfoDialog(
        title: String,
        message: String,
        onDismissAction: (() -> Unit)?
    ) {
        if (!isAdded) return

        val dialogView = layoutInflater.inflate(R.layout.dialog_challenge_info, null, false)

        val titleTextView = dialogView.findViewById<TextView>(R.id.infoTitleTextView)
        val messageTextView = dialogView.findViewById<TextView>(R.id.infoMessageTextView)
        val closeButton = dialogView.findViewById<Button>(R.id.closeInfoButton)

        titleTextView.text = title
        messageTextView.text = message

        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setView(dialogView)
            .setCancelable(false)
            .create()

        addPressedEffect(closeButton)

        closeButton.setOnClickListener {
            dialog.dismiss()
            onDismissAction?.invoke()
        }

        dialog.show()
    }

    private fun safeExitToHome() {
        if (!isAdded) return
        if (findNavController().currentDestination?.id == R.id.challengeRoomFragment) {
            findNavController().navigate(R.id.action_challengeRoomFragment_to_homeFragment)
        }
    }

    private fun formatCategoryName(category: String): String {
        return when (category.trim().lowercase()) {
            "informatics" -> "Informatics"
            "mathematics", "math" -> "Mathematics"
            "english" -> "English"
            "history" -> "History"
            "logic" -> "Logic"
            "world", "worldview", "world_knowledge", "random" -> "Random"
            else -> "Quiz"
        }
    }

    private fun isTerminalRoomStatus(status: String): Boolean {
        return status.equals("cancelled", ignoreCase = true) ||
                status.equals("expired", ignoreCase = true)
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
        if (!isAdded) return
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }

    override fun onDestroyView() {
        challengeCountDownTimer?.cancel()
        challengeCountDownTimer = null

        roomCloseCountDownTimer?.cancel()
        roomCloseCountDownTimer = null

        roomListener?.remove()
        roomListener = null

        _binding = null
        super.onDestroyView()
    }
}