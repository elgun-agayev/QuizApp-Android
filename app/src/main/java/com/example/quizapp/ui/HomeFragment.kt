package com.example.quizapp.ui

import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.core.widget.NestedScrollView
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.quizapp.R
import com.example.quizapp.databinding.FragmentHomeBinding
import com.example.quizapp.manager.UserManager
import com.example.quizapp.model.UserStatus
import com.google.android.material.card.MaterialCardView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private var coinAnimator: ObjectAnimator? = null
    private var isSearchMode = false

    private lateinit var searchAdapter: SearchPlayerAdapter

    private var acceptedChallengeListener: ListenerRegistration? = null
    private var acceptedChallengeDialog: AlertDialog? = null
    private var lastHandledAcceptedRequestId: String? = null
    private var lastShownCancelledRoomId: String? = null

    private var currentPlayerId: String = ""
    private var currentPlayerName: String = ""
    private var currentPlayerAvatarResId: Int = R.drawable.ic_avatar_placeholder
    private var currentPlayerAvatarUrl: String = ""

    private val allPlayers = listOf(
        SearchPlayer("Joseph", "AEL-29481", R.drawable.avatar_1),
        SearchPlayer("Jessica", "AEL-12045", R.drawable.avatar_2),
        SearchPlayer("Jefry", "AEL-88771", R.drawable.avatar_3),
        SearchPlayer("Alice", "AEL-44592", R.drawable.avatar_4),
        SearchPlayer("Captan", "AEL-77310", R.drawable.avatar_5),
        SearchPlayer("Crown", "AEL-55500", R.drawable.avatar_6),
        SearchPlayer("Jorg", "AEL-22991", R.drawable.avatar_7),
        SearchPlayer("Elite", "AEL-90909", R.drawable.avatar_8)
    )

    private val ROOM_JOIN_TIMEOUT_MILLIS = 600_000L
    private val HOME_ROOM_CLOSE_DELAY_MILLIS = 120_000L

    private val filteredPlayers = mutableListOf<SearchPlayer>()

    override fun onResume() {
        super.onResume()
        UserManager.loadUser(requireContext())
        bindCurrentUserLocal()
        loadHomeUserProfile()
    }

    override fun onPause() {
        super.onPause()
        acceptedChallengeListener?.remove()
        acceptedChallengeListener = null
        acceptedChallengeDialog?.dismiss()
        acceptedChallengeDialog = null
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        UserManager.loadUser(requireContext())

        setupCoinAnimation()
        setupSearchRecyclerView()
        setupSearchInitialState()
        setupPressedEffects()
        setupSearchTextWatcher()
        setupClickListeners()
        setupBackPressHandler()
        bindCurrentUserLocal()
        loadHomeUserProfile()

        UserManager.userLiveData.observe(viewLifecycleOwner) { user ->
            currentPlayerName = user.name
            currentPlayerId = user.userId
            currentPlayerAvatarResId = user.avatarResId

            binding.greetingText.text = "Hello, ${
                user.name.split("\\s+".toRegex()).firstOrNull().orEmpty().ifBlank { "Player" }
            }"
            binding.gemAmountText.text = user.score.toString()
            binding.profileImage.setImageResource(
                user.avatarResId.takeIf { it != 0 } ?: R.drawable.ic_avatar_placeholder
            )
        }
    }

    private fun bindCurrentUserLocal() = with(binding) {
        val user = UserManager.currentUser

        currentPlayerId = user.userId
        currentPlayerName = user.name
        currentPlayerAvatarResId = user.avatarResId

        greetingText.text = "Hello, ${
            user.name.split("\\s+".toRegex()).firstOrNull().orEmpty().ifBlank { "Player" }
        }"
        gemAmountText.text = user.score.toString()
        profileImage.setImageResource(
            user.avatarResId.takeIf { it != 0 } ?: R.drawable.ic_avatar_placeholder
        )
    }

    private fun loadHomeUserProfile() {
        val firebaseUser = auth.currentUser ?: run {
            listenForAcceptedChallenges()
            checkActiveChallengeRoom()
            return
        }

        firestore.collection("users")
            .document(firebaseUser.uid)
            .get()
            .addOnSuccessListener { document ->
                if (!isAdded || _binding == null) return@addOnSuccessListener

                val localUser = UserManager.currentUser

                val firestoreName = document.getString("name").orEmpty().trim()
                val firestoreFirstName = document.getString("firstName").orEmpty().trim()
                val firestorePlayerId = document.getString("playerId").orEmpty().trim()
                val firestoreScore = document.getLong("score")?.toInt() ?: localUser.score
                val firestoreStatus = document.getString("status").toUserStatusOrDefault(localUser.status)
                val firestoreTotalGames = document.getLong("totalGames")?.toInt() ?: localUser.totalGames
                val firestoreCorrectAnswers = document.getLong("correctAnswers")?.toInt() ?: localUser.correctAnswers

                val authDisplayName = firebaseUser.displayName.orEmpty().trim()
                val resolvedName = when {
                    firestoreFirstName.isNotBlank() -> firestoreFirstName
                    firestoreName.isNotBlank() -> firestoreName.split("\\s+".toRegex()).firstOrNull()
                        .orEmpty().ifBlank { firestoreName }
                    authDisplayName.isNotBlank() -> authDisplayName.split("\\s+".toRegex()).firstOrNull()
                        .orEmpty().ifBlank { authDisplayName }
                    localUser.name.isNotBlank() -> localUser.name
                    else -> "Player"
                }

                val resolvedPlayerId = when {
                    firestorePlayerId.isNotBlank() -> firestorePlayerId
                    localUser.userId.isNotBlank() -> localUser.userId
                    else -> ""
                }

                currentPlayerName = resolvedName
                currentPlayerId = resolvedPlayerId
                currentPlayerAvatarResId =
                    localUser.avatarResId.takeIf { it != 0 } ?: R.drawable.ic_avatar_placeholder

                binding.greetingText.text = "Hello, ${
                    resolvedName.split("\\s+".toRegex()).firstOrNull().orEmpty().ifBlank { "Player" }
                }"
                binding.gemAmountText.text = firestoreScore.toString()
                binding.profileImage.setImageResource(currentPlayerAvatarResId)

                UserManager.updateUser(
                    userId = resolvedPlayerId,
                    name = resolvedName,
                    avatarResId = currentPlayerAvatarResId,
                    score = firestoreScore,
                    status = firestoreStatus,
                    totalGames = firestoreTotalGames,
                    correctAnswers = firestoreCorrectAnswers
                )
                UserManager.saveUser(requireContext())

                listenForAcceptedChallenges()
                checkActiveChallengeRoom()
            }
            .addOnFailureListener {
                if (!isAdded || _binding == null) return@addOnFailureListener
                bindCurrentUserLocal()
                listenForAcceptedChallenges()
                checkActiveChallengeRoom()
            }
    }

    private fun listenForAcceptedChallenges() {
        acceptedChallengeListener?.remove()
        acceptedChallengeListener = null

        if (currentPlayerId.isBlank()) return

        acceptedChallengeListener = firestore.collection("challenge_requests")
            .whereEqualTo("fromPlayerId", currentPlayerId)
            .whereEqualTo("status", "accepted")
            .addSnapshotListener { snapshots, exception ->
                if (exception != null) {
                    showToast("Listener error: ${exception.message}")
                    return@addSnapshotListener
                }

                if (!isAdded || _binding == null) return@addSnapshotListener
                if (findNavController().currentDestination?.id != R.id.homeFragment) return@addSnapshotListener

                val documents = snapshots?.documents ?: return@addSnapshotListener
                if (documents.isEmpty()) return@addSnapshotListener

                val latestAcceptedRequest = documents
                    .sortedByDescending { it.getLong("timestamp") ?: 0L }
                    .firstOrNull() ?: return@addSnapshotListener

                val requestId = latestAcceptedRequest.getString("requestId").orEmpty()
                val opponentName = latestAcceptedRequest.getString("toPlayerName").orEmpty()
                val opponentId = latestAcceptedRequest.getString("toPlayerId").orEmpty()
                val opponentAvatar = latestAcceptedRequest.getLong("toPlayerAvatar")?.toInt()
                    ?: R.drawable.ic_avatar_placeholder

                if (requestId.isBlank()) return@addSnapshotListener
                if (lastHandledAcceptedRequestId == requestId) return@addSnapshotListener
                if (acceptedChallengeDialog?.isShowing == true) return@addSnapshotListener

                lastHandledAcceptedRequestId = requestId

                findChallengeRoomAndShowDialog(
                    requestId = requestId,
                    opponentName = opponentName,
                    opponentId = opponentId,
                    opponentAvatar = opponentAvatar
                )
            }
    }

    private fun checkActiveChallengeRoom() {
        if (currentPlayerId.isBlank()) return

        val now = System.currentTimeMillis()

        firestore.collection("challenge_rooms")
            .get()
            .addOnSuccessListener { result ->
                val activeRoom = result.documents.firstOrNull { document ->
                    val hostPlayerId = document.getString("hostPlayerId").orEmpty()
                    val guestPlayerId = document.getString("guestPlayerId").orEmpty()
                    val status = document.getString("status").orEmpty()
                    val closeAt = document.getLong("closeAt") ?: 0L

                    val isCurrentUserInRoom =
                        currentPlayerId == hostPlayerId || currentPlayerId == guestPlayerId

                    if (!isCurrentUserInRoom) return@firstOrNull false

                    when (status.lowercase()) {
                        "waiting", "ready", "started" -> true
                        "cancelled" -> closeAt > now
                        else -> false
                    }
                }

                if (activeRoom == null) return@addOnSuccessListener

                val roomId = activeRoom.getString("roomId").orEmpty()
                val hostPlayerId = activeRoom.getString("hostPlayerId").orEmpty()
                val guestPlayerId = activeRoom.getString("guestPlayerId").orEmpty()
                val hostPlayerName = activeRoom.getString("hostPlayerName").orEmpty()
                val guestPlayerName = activeRoom.getString("guestPlayerName").orEmpty()
                val hostPlayerAvatar =
                    activeRoom.getLong("hostPlayerAvatar")?.toInt()
                        ?: R.drawable.ic_avatar_placeholder
                val guestPlayerAvatar =
                    activeRoom.getLong("guestPlayerAvatar")?.toInt()
                        ?: R.drawable.ic_avatar_placeholder
                val status = activeRoom.getString("status").orEmpty()
                val cancelReason = activeRoom.getString("cancelReason").orEmpty()
                val cancelledBy = activeRoom.getString("cancelledBy").orEmpty()
                val mode = activeRoom.getString("mode").orEmpty().ifBlank { "1 vs 1" }
                val category = activeRoom.getString("category").orEmpty().ifBlank { "Random" }

                val isHost = currentPlayerId == hostPlayerId

                val opponentName = if (isHost) guestPlayerName else hostPlayerName
                val opponentId = if (isHost) guestPlayerId else hostPlayerId
                val opponentAvatar = if (isHost) guestPlayerAvatar else hostPlayerAvatar

                if (roomId.isBlank()) return@addOnSuccessListener

                if (status.equals("cancelled", ignoreCase = true)) {
                    if (lastShownCancelledRoomId == roomId) return@addOnSuccessListener
                    lastShownCancelledRoomId = roomId

                    if (cancelledBy == currentPlayerId) {
                        firestore.collection("challenge_rooms")
                            .document(roomId)
                            .update("closeAt", 0L)

                        return@addOnSuccessListener
                    }

                    val message = when (cancelReason) {
                        "host_cancelled", "guest_cancelled", "cancelled" ->
                            "$opponentName cancelled the challenge."

                        "host_left_room", "guest_left_room", "left_room" ->
                            "$opponentName left the challenge room."

                        "host_started_game", "guest_started_game" ->
                            "$opponentName started another game."

                        else ->
                            "Challenge was cancelled."
                    }

                    showChallengeRoomInfoDialog(
                        roomId = roomId,
                        title = "Challenge Ended",
                        message = message
                    )

                    return@addOnSuccessListener
                }

                if (findNavController().currentDestination?.id == R.id.homeFragment) {
                    val bundle = Bundle().apply {
                        putString("roomId", roomId)
                        putString("opponentName", opponentName)
                        putString("opponentId", opponentId)
                        putString("selectedMode", mode)
                        putString("selectedCategory", category)
                        putInt("opponentAvatar", opponentAvatar)
                    }

                    findNavController().navigate(
                        R.id.action_homeFragment_to_challengeRoomFragment,
                        bundle
                    )
                }
            }
            .addOnFailureListener { exception ->
                showToast("Failed to check active room: ${exception.message}")
            }
    }

    private fun findChallengeRoomAndShowDialog(
        requestId: String,
        opponentName: String,
        opponentId: String,
        opponentAvatar: Int
    ) {
        firestore.collection("challenge_rooms")
            .whereEqualTo("requestId", requestId)
            .limit(1)
            .get()
            .addOnSuccessListener { result ->
                if (result.isEmpty) {
                    lastHandledAcceptedRequestId = null
                    showToast("Challenge room not found")
                    return@addOnSuccessListener
                }

                val roomDocument = result.documents.first()
                val roomId = roomDocument.getString("roomId").orEmpty()
                val roomStatus = roomDocument.getString("status").orEmpty()
                val cancelReason = roomDocument.getString("cancelReason").orEmpty()
                val category = roomDocument.getString("category").orEmpty().ifBlank { "Random" }
                val mode = roomDocument.getString("mode").orEmpty().ifBlank { "1 vs 1" }
                val createdAt = roomDocument.getLong("createdAt") ?: System.currentTimeMillis()

                if (roomId.isBlank()) {
                    lastHandledAcceptedRequestId = null
                    showToast("Room ID not found")
                    return@addOnSuccessListener
                }

                if (roomStatus.equals("cancelled", ignoreCase = true)) {
                    lastHandledAcceptedRequestId = null

                    val message = when (cancelReason) {
                        "host_cancelled", "guest_cancelled", "cancelled" ->
                            "$opponentName cancelled the challenge."

                        "host_left_room", "guest_left_room", "left_room" ->
                            "$opponentName left the challenge room."

                        "host_started_game", "guest_started_game" ->
                            "$opponentName started another game."

                        else ->
                            "$opponentName cancelled the challenge."
                    }

                    showChallengeRoomInfoDialog(
                        roomId = roomId,
                        title = "Challenge Ended",
                        message = message
                    )

                    return@addOnSuccessListener
                }

                if (roomStatus.equals("expired", ignoreCase = true)) {
                    lastHandledAcceptedRequestId = null

                    showChallengeRoomInfoDialog(
                        roomId = roomId,
                        title = "Challenge Expired",
                        message = "Challenge room expired."
                    )

                    return@addOnSuccessListener
                }

                val currentTime = System.currentTimeMillis()
                val elapsedTime = currentTime - createdAt

                if (elapsedTime > ROOM_JOIN_TIMEOUT_MILLIS) {
                    firestore.collection("challenge_requests")
                        .document(requestId)
                        .update("status", "expired")

                    firestore.collection("challenge_rooms")
                        .document(roomId)
                        .update(
                            mapOf(
                                "status" to "expired",
                                "cancelReason" to "timeout",
                                "lastActionAt" to System.currentTimeMillis()
                            )
                        )

                    lastHandledAcceptedRequestId = null

                    showChallengeRoomInfoDialog(
                        roomId = roomId,
                        title = "Challenge Expired",
                        message = "Challenge room expired."
                    )

                    return@addOnSuccessListener
                }

                showAcceptedChallengeDialog(
                    requestId = requestId,
                    roomId = roomId,
                    opponentName = opponentName,
                    opponentId = opponentId,
                    opponentAvatar = opponentAvatar,
                    selectedCategory = category,
                    selectedMode = mode
                )
            }
            .addOnFailureListener { exception ->
                lastHandledAcceptedRequestId = null
                showToast("Failed to find room: ${exception.message}")
            }
    }

    private fun showAcceptedChallengeDialog(
        requestId: String,
        roomId: String,
        opponentName: String,
        opponentId: String,
        opponentAvatar: Int,
        selectedCategory: String,
        selectedMode: String
    ) {
        if (!isAdded) return
        if (acceptedChallengeDialog?.isShowing == true) return

        val dialogView =
            layoutInflater.inflate(R.layout.dialog_challenge_accepted, null, false)

        val avatarImageView = dialogView.findViewById<ImageView>(R.id.playerAvatar)
        val playerNameTextView = dialogView.findViewById<TextView>(R.id.playerName)
        val playerIdTextView = dialogView.findViewById<TextView>(R.id.playerId)
        val dialogMessageTextView = dialogView.findViewById<TextView>(R.id.dialogMessage)
        val modeTextView = dialogView.findViewById<TextView>(R.id.modeTextView)
        val categoryTextView = dialogView.findViewById<TextView>(R.id.categoryTextView)
        val cancelButton = dialogView.findViewById<Button>(R.id.cancelButton)
        val joinButton = dialogView.findViewById<Button>(R.id.joinButton)

        avatarImageView.setImageResource(opponentAvatar)
        playerNameTextView.text = opponentName
        playerIdTextView.text = "ID: $opponentId"
        dialogMessageTextView.text =
            "$opponentName accepted your challenge and is waiting in the room.\nJoin now to start the duel."
        modeTextView.text = selectedMode
        categoryTextView.text = selectedCategory

        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setView(dialogView)
            .setCancelable(false)
            .create()

        acceptedChallengeDialog = dialog
        dialog.setOnDismissListener {
            acceptedChallengeDialog = null
        }

        cancelButton.setOnClickListener {
            cancelAcceptedChallenge(requestId, roomId, opponentName)
            dialog.dismiss()
        }

        joinButton.setOnClickListener {
            joinAcceptedChallengeRoom(
                requestId = requestId,
                roomId = roomId,
                opponentName = opponentName,
                opponentId = opponentId,
                opponentAvatar = opponentAvatar,
                selectedCategory = selectedCategory,
                selectedMode = selectedMode
            )
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun showChallengeRoomInfoDialog(
        roomId: String,
        title: String,
        message: String
    ) {
        if (!isAdded) return

        val dialogView =
            layoutInflater.inflate(R.layout.dialog_challenge_info, null, false)

        val titleTextView = dialogView.findViewById<TextView>(R.id.infoTitleTextView)
        val messageTextView = dialogView.findViewById<TextView>(R.id.infoMessageTextView)
        val closeButton = dialogView.findViewById<Button>(R.id.closeInfoButton)

        titleTextView.text = title
        messageTextView.text = message

        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setView(dialogView)
            .setCancelable(false)
            .create()

        closeButton.setOnClickListener {
            dialog.dismiss()

            firestore.collection("challenge_rooms")
                .document(roomId)
                .update("closeAt", 0L)

            lastShownCancelledRoomId = roomId
        }

        dialog.show()
    }

    private fun joinAcceptedChallengeRoom(
        requestId: String,
        roomId: String,
        opponentName: String,
        opponentId: String,
        opponentAvatar: Int,
        selectedCategory: String,
        selectedMode: String
    ) {
        if (currentPlayerId.isBlank()) {
            showToast("Current player ID not found")
            return
        }

        firestore.collection("challenge_rooms")
            .document(roomId)
            .get()
            .addOnSuccessListener { document ->
                if (!document.exists()) {
                    showToast("Room not found")
                    return@addOnSuccessListener
                }

                val hostPlayerId = document.getString("hostPlayerId").orEmpty()
                val guestPlayerId = document.getString("guestPlayerId").orEmpty()

                val joinedField: String
                val stateField: String

                when (currentPlayerId) {
                    hostPlayerId -> {
                        joinedField = "hostJoined"
                        stateField = "hostState"
                    }

                    guestPlayerId -> {
                        joinedField = "guestJoined"
                        stateField = "guestState"
                    }

                    else -> {
                        showToast("You are not part of this room")
                        return@addOnSuccessListener
                    }
                }

                firestore.collection("challenge_rooms")
                    .document(roomId)
                    .update(
                        mapOf(
                            joinedField to true,
                            stateField to "in_room",
                            "lastActionAt" to System.currentTimeMillis()
                        )
                    )
                    .addOnSuccessListener {
                        firestore.collection("challenge_requests")
                            .document(requestId)
                            .update("status", "joined")
                            .addOnSuccessListener {
                                val bundle = Bundle().apply {
                                    putString("roomId", roomId)
                                    putString("opponentName", opponentName)
                                    putString("opponentId", opponentId)
                                    putString("selectedMode", selectedMode)
                                    putString("selectedCategory", selectedCategory)
                                    putInt("opponentAvatar", opponentAvatar)
                                }

                                findNavController().navigate(
                                    R.id.action_homeFragment_to_challengeRoomFragment,
                                    bundle
                                )
                            }
                            .addOnFailureListener { exception ->
                                showToast("Failed to update request status: ${exception.message}")
                            }
                    }
                    .addOnFailureListener { exception ->
                        showToast("Failed to join room: ${exception.message}")
                    }
            }
            .addOnFailureListener { exception ->
                showToast("Failed to load room: ${exception.message}")
            }
    }

    private fun cancelAcceptedChallenge(
        requestId: String,
        roomId: String,
        opponentName: String
    ) {
        if (currentPlayerId.isBlank()) {
            showToast("Current player ID not found")
            return
        }

        firestore.collection("challenge_rooms")
            .document(roomId)
            .get()
            .addOnSuccessListener { document ->
                if (!document.exists()) {
                    showToast("Room not found")
                    return@addOnSuccessListener
                }

                val hostPlayerId = document.getString("hostPlayerId").orEmpty()
                val guestPlayerId = document.getString("guestPlayerId").orEmpty()

                val cancelReason: String
                val stateField: String

                when (currentPlayerId) {
                    hostPlayerId -> {
                        cancelReason = "host_cancelled"
                        stateField = "hostState"
                    }

                    guestPlayerId -> {
                        cancelReason = "guest_cancelled"
                        stateField = "guestState"
                    }

                    else -> {
                        showToast("You are not part of this room")
                        return@addOnSuccessListener
                    }
                }

                firestore.collection("challenge_requests")
                    .document(requestId)
                    .update("status", "cancelled")
                    .addOnSuccessListener {
                        firestore.collection("challenge_rooms")
                            .document(roomId)
                            .update(
                                mapOf(
                                    "status" to "cancelled",
                                    "cancelledBy" to currentPlayerId,
                                    "cancelReason" to cancelReason,
                                    stateField to "cancelled",
                                    "lastActionAt" to System.currentTimeMillis(),
                                    "closeAt" to (System.currentTimeMillis() + HOME_ROOM_CLOSE_DELAY_MILLIS)
                                )
                            )
                            .addOnSuccessListener {
                                showToast("Challenge with $opponentName was cancelled")
                            }
                            .addOnFailureListener { exception ->
                                showToast("Failed to cancel room: ${exception.message}")
                            }
                    }
                    .addOnFailureListener { exception ->
                        showToast("Failed to cancel request: ${exception.message}")
                    }
            }
            .addOnFailureListener { exception ->
                showToast("Failed to load room: ${exception.message}")
            }
    }

    private fun setupCoinAnimation() = with(binding) {
        coinAnimator = ObjectAnimator.ofFloat(coinImage, "translationY", 0f, -24f).apply {
            duration = 1000
            repeatCount = ValueAnimator.INFINITE
            repeatMode = ValueAnimator.REVERSE
            interpolator = AccelerateDecelerateInterpolator()
            start()
        }
    }

    private fun setupSearchRecyclerView() = with(binding) {
        searchAdapter = SearchPlayerAdapter(filteredPlayers) { selectedPlayer ->
            showToast("${selectedPlayer.name} selected")
        }

        searchResultsRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = searchAdapter
            setHasFixedSize(true)
        }
    }

    private fun setupSearchInitialState() = with(binding) {
       homeNestedScrollView.isVisible = true
        playerSearchContainer.isVisible = false
        noSearchResultImageView.isVisible = false
        noSearchResultTextView.isVisible = false
        searchResultsRecyclerView.isVisible = false
    }

    private fun setupSearchTextWatcher() = with(binding) {
        searchPlayerEditText.doAfterTextChanged {
            searchPlayerTextInputLayout.error = null

            if (it.isNullOrBlank()) {
                noSearchResultImageView.isVisible = false
                noSearchResultTextView.isVisible = false
                filteredPlayers.clear()
                searchAdapter.notifyDataSetChanged()
                searchResultsRecyclerView.isVisible = false
            }
        }
    }

    private fun setupPressedEffects() = with(binding) {
        val createTestContainer = createTestIcon.parent as? View
        val singlePlayerContainer = singlePlayerIcon.parent as? View
        val multiPlayerContainer = multiPlayerIcon.parent as? View

        addPressedEffect(profileImage)
        addIconButtonEffect(searchHomeImageView)
        addIconButtonEffect(searchBackImageView)
        addPrimaryButtonEffect(searchPlayerButton)

        createTestContainer?.let { addPressedEffect(it) }
        singlePlayerContainer?.let { addPressedEffect(it) }
        multiPlayerContainer?.let { addPressedEffect(it) }

        addPressedEffect(startNowButton)
        addPressedEffect(viewAllText)

        addPressedEffect(informaticsIcon.parent as View)
        addPressedEffect(mathIcon.parent as View)
        addPressedEffect(englishIcon.parent as View)
        addPressedEffect(historyIcon.parent as View)
        addPressedEffect(worldViewIcon.parent as View)
        addPressedEffect(logicIcon.parent as View)
    }

    private fun setupClickListeners() = with(binding) {
        val createTestContainer = createTestIcon.parent as? View
        val singlePlayerContainer = singlePlayerIcon.parent as? View
        val multiPlayerContainer = multiPlayerIcon.parent as? View

        val informaticsContainer = informaticsIcon.parent as View
        val mathematicsContainer = mathIcon.parent as View
        val englishContainer = englishIcon.parent as View
        val historyContainer = historyIcon.parent as View
        val worldViewContainer = worldViewIcon.parent as View
        val logicContainer = logicIcon.parent as View

        profileImage.setOnClickListener {
            findNavController().navigate(R.id.action_homeFragment_to_profileFragment)
        }

        searchHomeImageView.setOnClickListener {
            openSearchMode()
        }

        searchBackImageView.setOnClickListener {
            closeSearchMode()
        }

        searchPlayerButton.setOnClickListener {
            performPlayerSearch()
        }

        searchPlayerEditText.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH || actionId == EditorInfo.IME_ACTION_DONE) {
                performPlayerSearch()
                true
            } else {
                false
            }
        }

        createTestContainer?.setOnClickListener {
            findNavController().navigate(R.id.action_homeFragment_to_createTestFragment)
        }

        singlePlayerContainer?.setOnClickListener {
            findNavController().navigate(R.id.action_homeFragment_to_singlePlayerSetupFragment)
        }

        multiPlayerContainer?.setOnClickListener {
            findNavController().navigate(R.id.action_homeFragment_to_multiPlayerModeFragment)
        }

        startNowButton.setOnClickListener {
            findNavController().navigate(R.id.action_homeFragment_to_createTestFragment)
        }

        viewAllText.setOnClickListener {
            toggleCategoriesVisibility()
        }

        informaticsContainer.setOnClickListener {
            openQuizGame("informatics")
        }

        mathematicsContainer.setOnClickListener {
            openQuizGame("mathematics")
        }

        englishContainer.setOnClickListener {
            openQuizGame("english")
        }

        historyContainer.setOnClickListener {
            openQuizGame("history")
        }

        worldViewContainer.setOnClickListener {
            openQuizGame("world")
        }

        logicContainer.setOnClickListener {
            openQuizGame("logic")
        }

        root.setOnClickListener {
            if (hiddenCategoriesLayout.isVisible) {
                hiddenCategoriesLayout.isVisible = false
            }
            clearSearchFocusAndHideKeyboard()
        }

        playerSearchContainer.setOnClickListener {
            clearSearchFocusAndHideKeyboard()
        }

        hiddenCategoriesLayout.setOnClickListener {
        }
    }

    private fun setupBackPressHandler() {
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                clearSearchFocusAndHideKeyboard()
                if (isSearchMode) {
                    closeSearchMode()
                } else {
                    isEnabled = false
                    requireActivity().onBackPressedDispatcher.onBackPressed()
                    isEnabled = true
                }
            }
        })
    }

    private fun openSearchMode() = with(binding) {
        isSearchMode = true
        homeNestedScrollView.isVisible = false
        playerSearchContainer.isVisible = true
        searchPlayerTextInputLayout.error = null
        noSearchResultImageView.isVisible = false
        noSearchResultTextView.isVisible = false
        searchResultsRecyclerView.isVisible = false
        searchPlayerEditText.requestFocus()
    }

    private fun closeSearchMode() = with(binding) {
        clearSearchFocusAndHideKeyboard()
        isSearchMode = false
        playerSearchContainer.isVisible = false
        homeNestedScrollView.isVisible = true
        searchPlayerEditText.text?.clear()
        searchPlayerTextInputLayout.error = null
        filteredPlayers.clear()
        searchAdapter.notifyDataSetChanged()
        noSearchResultImageView.isVisible = false
        noSearchResultTextView.isVisible = false
        searchResultsRecyclerView.isVisible = false
    }

    private fun performPlayerSearch() = with(binding) {
        val query = searchPlayerEditText.text?.toString()?.trim().orEmpty()

        if (query.isEmpty()) {
            searchPlayerTextInputLayout.error = "Enter player name or ID"
            noSearchResultImageView.isVisible = false
            noSearchResultTextView.isVisible = false
            searchResultsRecyclerView.isVisible = false
            return@with
        } else {
            searchPlayerTextInputLayout.error = null
        }

        val results = allPlayers.filter { player ->
            player.name.contains(query, ignoreCase = true) ||
                    player.playerId.contains(query, ignoreCase = true)
        }

        filteredPlayers.clear()
        filteredPlayers.addAll(results)
        searchAdapter.notifyDataSetChanged()

        if (results.isEmpty()) {
            noSearchResultImageView.isVisible = true
            noSearchResultTextView.isVisible = true
            searchResultsRecyclerView.isVisible = false
        } else {
            noSearchResultImageView.isVisible = false
            noSearchResultTextView.isVisible = false
            searchResultsRecyclerView.isVisible = true
        }
    }

    private fun openQuizGame(category: String) {
        val bundle = Bundle().apply {
            putString("category", category)
        }
        findNavController().navigate(R.id.action_homeFragment_to_quizGameFragment, bundle)
    }

    private fun toggleCategoriesVisibility() = with(binding) {
        hiddenCategoriesLayout.isVisible = !hiddenCategoriesLayout.isVisible
    }

    private fun clearSearchFocusAndHideKeyboard() {
        if (_binding == null) return
        binding.searchPlayerEditText.clearFocus()
        hideKeyboard(binding.searchPlayerEditText)
    }

    private fun hideKeyboard(view: View?) {
        if (!isAdded) return
        val targetView = view ?: activity?.currentFocus ?: return
        val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
        imm?.hideSoftInputFromWindow(targetView.windowToken, 0)
    }

    private fun addPressedEffect(targetView: View) {
        targetView.setOnTouchListener { v, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    v.animate()
                        .alpha(0.86f)
                        .scaleX(0.97f)
                        .scaleY(0.97f)
                        .setDuration(90)
                        .start()
                }

                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    v.animate()
                        .alpha(1f)
                        .scaleX(1f)
                        .scaleY(1f)
                        .setDuration(90)
                        .start()
                }
            }
            false
        }
    }

    private fun addIconButtonEffect(targetView: View) {
        targetView.isClickable = true
        targetView.isFocusable = true

        targetView.setOnTouchListener { v, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    v.animate()
                        .alpha(0.78f)
                        .scaleX(0.90f)
                        .scaleY(0.90f)
                        .translationY(dpToPx(1))
                        .setDuration(80)
                        .start()
                }

                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    v.animate()
                        .alpha(1f)
                        .scaleX(1f)
                        .scaleY(1f)
                        .translationY(0f)
                        .setDuration(100)
                        .start()
                }
            }
            false
        }
    }

    private fun addPrimaryButtonEffect(targetView: View) {
        targetView.isClickable = true
        targetView.isFocusable = true

        targetView.setOnTouchListener { v, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    v.animate()
                        .alpha(0.88f)
                        .scaleX(0.98f)
                        .scaleY(0.98f)
                        .translationY(dpToPx(1))
                        .setDuration(80)
                        .start()
                }

                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    v.animate()
                        .alpha(1f)
                        .scaleX(1f)
                        .scaleY(1f)
                        .translationY(0f)
                        .setDuration(100)
                        .start()
                }
            }
            false
        }
    }

    private fun String?.toUserStatusOrDefault(defaultStatus: UserStatus): UserStatus {
        return when (this?.trim()?.lowercase()) {
            "online" -> UserStatus.ONLINE
            "offline" -> UserStatus.OFFLINE
            "in_game" -> UserStatus.IN_GAME
            else -> defaultStatus
        }
    }

    private fun dpToPx(dp: Int): Float {
        return dp * resources.displayMetrics.density
    }

    private fun showToast(message: String) {
        if (isAdded) {
            Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroyView() {
        acceptedChallengeListener?.remove()
        acceptedChallengeListener = null
        acceptedChallengeDialog?.dismiss()
        acceptedChallengeDialog = null
        coinAnimator?.cancel()
        coinAnimator = null
        _binding = null
        super.onDestroyView()
    }

    data class SearchPlayer(
        val name: String,
        val playerId: String,
        val avatarResId: Int
    )

    class SearchPlayerAdapter(
        private val playerList: List<SearchPlayer>,
        private val onItemClick: (SearchPlayer) -> Unit
    ) : RecyclerView.Adapter<SearchPlayerAdapter.SearchPlayerViewHolder>() {

        inner class SearchPlayerViewHolder(
            itemView: View,
            private val avatarImageView: ImageView,
            private val playerNameTextView: TextView,
            private val playerIdTextView: TextView,
            private val addFriendTextView: TextView
        ) : RecyclerView.ViewHolder(itemView) {

            fun bind(player: SearchPlayer) {
                avatarImageView.setImageResource(player.avatarResId)
                playerNameTextView.text = player.name
                playerIdTextView.text = player.playerId

                itemView.setOnClickListener {
                    onItemClick(player)
                }

                addFriendTextView.setOnClickListener {
                    Toast.makeText(
                        itemView.context,
                        "Friend request sent to ${player.name}",
                        Toast.LENGTH_SHORT
                    ).show()
                }

                addFriendTextView.setOnTouchListener { v, event ->
                    when (event.action) {
                        MotionEvent.ACTION_DOWN -> {
                            v.animate()
                                .alpha(0.85f)
                                .scaleX(0.96f)
                                .scaleY(0.96f)
                                .setDuration(80)
                                .start()
                        }

                        MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                            v.animate()
                                .alpha(1f)
                                .scaleX(1f)
                                .scaleY(1f)
                                .setDuration(100)
                                .start()
                        }
                    }
                    false
                }
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SearchPlayerViewHolder {
            val context = parent.context

            val cardView = MaterialCardView(context).apply {
                layoutParams = RecyclerView.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                ).apply {
                    bottomMargin = dp(context, 12)
                }
                radius = dp(context, 16).toFloat()
                cardElevation = dp(context, 2).toFloat()
                setCardBackgroundColor(
                    ContextCompat.getColor(context, android.R.color.white)
                )
            }

            val rootLayout = LinearLayout(context).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(dp(context, 14), dp(context, 14), dp(context, 14), dp(context, 14))
            }

            val avatarImageView = ImageView(context).apply {
                layoutParams = LinearLayout.LayoutParams(dp(context, 52), dp(context, 52))
                scaleType = ImageView.ScaleType.CENTER_CROP
                background = ContextCompat.getDrawable(context, R.drawable.bg_avatar_edit)
                clipToOutline = true
            }

            val textContainer = LinearLayout(context).apply {
                layoutParams = LinearLayout.LayoutParams(
                    0,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    1f
                ).apply {
                    marginStart = dp(context, 12)
                    marginEnd = dp(context, 12)
                }
                orientation = LinearLayout.VERTICAL
            }

            val playerNameTextView = TextView(context).apply {
                setTextColor(ContextCompat.getColor(context, R.color.color_navy_blue))
                textSize = 16f
                setTypeface(typeface, Typeface.BOLD)
            }

            val playerIdTextView = TextView(context).apply {
                setTextColor(ContextCompat.getColor(context, android.R.color.darker_gray))
                textSize = 13f
            }

            val addFriendTextView = TextView(context).apply {
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    dp(context, 36)
                )
                minWidth = dp(context, 72)
                gravity = Gravity.CENTER
                text = "Add"
                setTextColor(ContextCompat.getColor(context, android.R.color.white))
                setTypeface(typeface, Typeface.BOLD)
                background = ContextCompat.getDrawable(context, R.drawable.bg_orange)
                setPadding(dp(context, 14), 0, dp(context, 14), 0)
            }

            textContainer.addView(playerNameTextView)
            textContainer.addView(playerIdTextView)

            rootLayout.addView(avatarImageView)
            rootLayout.addView(textContainer)
            rootLayout.addView(addFriendTextView)

            cardView.addView(rootLayout)

            return SearchPlayerViewHolder(
                cardView,
                avatarImageView,
                playerNameTextView,
                playerIdTextView,
                addFriendTextView
            )
        }

        override fun onBindViewHolder(holder: SearchPlayerViewHolder, position: Int) {
            holder.bind(playerList[position])
        }

        override fun getItemCount(): Int = playerList.size

        private fun dp(context: Context, value: Int): Int {
            return (value * context.resources.displayMetrics.density).toInt()
        }
    }

}