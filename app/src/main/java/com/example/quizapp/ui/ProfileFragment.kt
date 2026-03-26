package com.example.quizapp.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.Toast
import androidx.activity.addCallback
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.navOptions
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.example.quizapp.R
import com.example.quizapp.adapter.AvatarAdapter
import com.example.quizapp.adapter.GameRequestAdapter
import com.example.quizapp.databinding.DialogAvatarPickerBinding
import com.example.quizapp.databinding.FragmentProfileBinding
import com.example.quizapp.manager.UserManager
import com.example.quizapp.model.ChallengeRoom
import com.example.quizapp.model.GameRequest
import com.example.quizapp.model.UserStatus
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.util.UUID

class ProfileFragment : Fragment(R.layout.fragment_profile) {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!

    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private var isStatusExpanded = false
    private var isFriendsExpanded = false
    private var isFriendRequestsExpanded = false
    private var isGameRequestsExpanded = false
    private var isMenuExpanded = false

    private var friendsCount = 0
    private var friendRequestsCount = 0
    private var gameRequestsCount = 0

    private lateinit var gameRequestAdapter: GameRequestAdapter
    private val gameRequestList = mutableListOf<GameRequest>()

    private val avatarList = listOf(
        R.drawable.ic_avatar_placeholder,
        R.drawable.avatar_1,
        R.drawable.avatar_2,
        R.drawable.avatar_3,
        R.drawable.avatar_4,
        R.drawable.avatar_5,
        R.drawable.avatar_6,
        R.drawable.avatar_7,
        R.drawable.avatar_8,
        R.drawable.avatar_9
    )

    override fun onResume() {
        super.onResume()
        UserManager.loadUser(requireContext())
        bindCurrentUserLocal()
        loadUserProfileFromFirestore()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentProfileBinding.bind(view)

        UserManager.loadUser(requireContext())

        setupBackNavigation()
        setupGameRequestsRecyclerView()
        setupInitialState()
        bindCurrentUserLocal()
        setupProfileData()
        setupClickListeners()
        loadUserProfileFromFirestore()
    }

    private fun setupBackNavigation() {
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner) {
            navigateToHome()
        }
    }

    private fun navigateToHome() {
        findNavController().navigate(R.id.homeFragment)
    }

    private fun bindCurrentUserLocal() = with(binding) {
        val user = UserManager.currentUser

        playerNameTextView.text = user.name
        playerIdTextView.text = "ID: ${user.userId}"
        scoreTextView.text = user.score.toString()
        onlineStatusTextView.text = user.status.toReadableText()
        gameCountValueTextView.text = user.totalGames.toString()
        correctAnswerValueTextView.text = user.correctAnswers.toString()

        if (user.avatarResId != 0) {
            avatarImageView.setImageResource(user.avatarResId)
        } else {
            avatarImageView.setImageResource(R.drawable.ic_avatar_placeholder)
        }
    }

    private fun loadUserProfileFromFirestore() {
        val firebaseUser = auth.currentUser ?: run {
            loadGameRequests()
            return
        }

        val user = UserManager.currentUser

        binding.playerNameTextView.text = user.name
        binding.playerIdTextView.text = "ID: ${user.userId}"
        binding.scoreTextView.text = user.score.toString()
        binding.onlineStatusTextView.text = user.status.toReadableText()
        binding.gameCountValueTextView.text = user.totalGames.toString()
        binding.correctAnswerValueTextView.text = user.correctAnswers.toString()

        if (user.avatarResId != 0) {
            binding.avatarImageView.setImageResource(user.avatarResId)
        } else {
            binding.avatarImageView.setImageResource(R.drawable.ic_avatar_placeholder)
        }

        loadGameRequests()
    }

    private fun setupGameRequestsRecyclerView() {
        gameRequestAdapter = GameRequestAdapter(
            onAcceptClick = { request ->
                acceptGameRequest(request)
            },
            onDeclineClick = { request ->
                declineGameRequest(request)
            }
        )

        binding.gameRequestsRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = gameRequestAdapter
            setHasFixedSize(true)
        }
    }

    private fun loadGameRequests() {
        val currentUserId = UserManager.currentUser.userId
        if (currentUserId.isBlank()) return

        firestore.collection("challenge_requests")
            .whereEqualTo("toPlayerId", currentUserId)
            .whereEqualTo("status", "pending")
            .get()
            .addOnSuccessListener { result ->
                if (!isAdded || _binding == null) return@addOnSuccessListener

                gameRequestList.clear()

                for (document in result.documents) {
                    val request = GameRequest(
                        requestId = document.getString("requestId").orEmpty(),
                        fromPlayerId = document.getString("fromPlayerId").orEmpty(),
                        fromPlayerName = document.getString("fromPlayerName").orEmpty(),
                        fromPlayerAvatar = (document.getLong("fromPlayerAvatar")
                            ?: R.drawable.ic_avatar_placeholder.toLong()).toInt(),
                        status = document.getString("status").orEmpty(),
                        timestamp = document.getLong("timestamp") ?: 0L
                    )
                    gameRequestList.add(request)
                }

                refreshGameRequests()
            }
            .addOnFailureListener { exception ->
                showToast("Failed to load requests: ${exception.message}")
            }
    }

    private fun acceptGameRequest(request: GameRequest) {
        val currentUser = UserManager.currentUser
        val roomId = UUID.randomUUID().toString()
        val createdAt = System.currentTimeMillis()

        val challengeRoom = ChallengeRoom(
            roomId = roomId,
            requestId = request.requestId,
            hostPlayerId = request.fromPlayerId,
            hostPlayerName = request.fromPlayerName,
            hostPlayerAvatar = request.fromPlayerAvatar,
            guestPlayerId = currentUser.userId,
            guestPlayerName = currentUser.name,
            guestPlayerAvatar = currentUser.avatarResId,
            category = "Random",
            mode = "1 vs 1",
            status = "waiting",
            createdAt = createdAt,
            hostJoined = false,
            guestJoined = true,
            hostState = "waiting",
            guestState = "in_room",
            cancelledBy = "",
            cancelReason = "",
            lastActionAt = createdAt,
            closeAt = 0L
        )

        firestore.collection("challenge_requests")
            .document(request.requestId)
            .update(
                mapOf(
                    "status" to "accepted",
                    "timestamp" to System.currentTimeMillis()
                )
            )
            .addOnSuccessListener {
                firestore.collection("challenge_rooms")
                    .document(roomId)
                    .set(challengeRoom)
                    .addOnSuccessListener {
                        showToast("${request.fromPlayerName} request accepted")

                        gameRequestList.removeAll { it.requestId == request.requestId }
                        refreshGameRequests()

                        val bundle = Bundle().apply {
                            putString("roomId", roomId)
                            putString("opponentName", request.fromPlayerName)
                            putString("opponentId", request.fromPlayerId)
                            putString("selectedMode", "1v1 Duel")
                            putString("selectedCategory", "Random")
                            putInt("opponentAvatar", request.fromPlayerAvatar)
                        }

                        findNavController().navigate(
                            R.id.action_profileFragment_to_challengeRoomFragment,
                            bundle
                        )
                    }
                    .addOnFailureListener { exception ->
                        showToast("Failed to create room: ${exception.message}")
                    }
            }
            .addOnFailureListener { exception ->
                showToast("Failed to accept request: ${exception.message}")
            }
    }

    private fun declineGameRequest(request: GameRequest) {
        firestore.collection("challenge_requests")
            .document(request.requestId)
            .update("status", "declined")
            .addOnSuccessListener {
                showToast("${request.fromPlayerName} request declined")

                gameRequestList.removeAll { it.requestId == request.requestId }
                refreshGameRequests()
            }
            .addOnFailureListener { exception ->
                showToast("Failed to decline request: ${exception.message}")
            }
    }

    private fun setupInitialState() = with(binding) {
        statusDetailsContainer.isVisible = false

        friendsDividerView.isVisible = false
        noFriendsTextView.isVisible = false
        friendsRecyclerView.isVisible = false
        friendsMoreOptionsImageView.isVisible = false

        friendRequestsDividerView.isVisible = false
        noFriendRequestsTextView.isVisible = false
        friendRequestsRecyclerView.isVisible = false

        gameRequestsDividerView.isVisible = false
        noGameRequestsTextView.isVisible = false
        gameRequestsRecyclerView.isVisible = false

        menuDividerView.isVisible = false
        editProfileSectionContainer.isVisible = false
        themeSectionContainer.isVisible = false
        helpSectionContainer.isVisible = false
        aboutSectionContainer.isVisible = false
        logoutSectionContainer.isVisible = false

        rotateArrow(statusArrowImageView, false)
        rotateArrow(friendsArrowImageView, false)
        rotateArrow(friendRequestsArrowImageView, false)
        rotateArrow(gameRequestsArrowImageView, false)
        rotateArrow(menuArrowImageView, false)
    }

    private fun setupProfileData() = with(binding) {
        friendsCount = 0
        friendRequestsCount = 0
        gameRequestsCount = gameRequestList.size

        friendRequestsCountTextView.text = friendRequestsCount.toString()
        gameRequestsCountTextView.text = gameRequestsCount.toString()

        gameRequestAdapter.submitList(gameRequestList.toList())

        updateFriendsSection()
        updateFriendRequestsSection()
        updateGameRequestsSection()
    }

    private fun setupClickListeners() = with(binding) {
        statusHeaderContainer.setOnClickListener {
            isStatusExpanded = !isStatusExpanded
            updateStatusSection()
        }

        friendsHeaderContainer.setOnClickListener {
            isFriendsExpanded = !isFriendsExpanded
            updateFriendsSection()
        }

        friendRequestsHeaderContainer.setOnClickListener {
            isFriendRequestsExpanded = !isFriendRequestsExpanded
            updateFriendRequestsSection()
        }

        gameRequestsHeaderContainer.setOnClickListener {
            isGameRequestsExpanded = !isGameRequestsExpanded
            updateGameRequestsSection()
        }

        menuHeaderContainer.setOnClickListener {
            isMenuExpanded = !isMenuExpanded
            updateMenuSection()
        }

        editProfileMenuItemContainer.setOnClickListener {
            findNavController().navigate(R.id.action_profileFragment_to_editProfileFragment)
        }

        themeMenuItemContainer.setOnClickListener {
            findNavController().navigate(R.id.action_profileFragment_to_themeFragment)
        }

        helpMenuItemContainer.setOnClickListener {
            showToast("Help clicked")
        }

        aboutMenuItemContainer.setOnClickListener {
            showToast("About clicked")
        }

        logoutMenuItemContainer.setOnClickListener {
            logoutUser()
        }

        friendsMoreOptionsImageView.setOnClickListener {
            showToast("More options clicked")
        }

        playerIdTextView.setOnClickListener {
            copyPlayerIdToClipboard()
        }

        avatarImageView.setOnClickListener {
            showAvatarPicker()
        }

        editAvatarImageView.setOnClickListener {
            showAvatarPicker()
        }
    }

    private fun logoutUser() {
        val googleSignInClient = GoogleSignIn.getClient(
            requireActivity(),
            GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN).build()
        )

        auth.signOut()

        googleSignInClient.signOut().addOnCompleteListener {
            UserManager.clearUser(requireContext())
            showToast("Logout successful")

            val options = navOptions {
                popUpTo(R.id.nav_graph) {
                    inclusive = true
                }
            }

            findNavController().navigate(R.id.loginFragment, null, options)
        }
    }

    private fun refreshGameRequests() {
        gameRequestsCount = gameRequestList.size
        binding.gameRequestsCountTextView.text = gameRequestsCount.toString()
        gameRequestAdapter.submitList(gameRequestList.toList())
        updateGameRequestsSection()
    }

    private fun updateStatusSection() = with(binding) {
        statusDetailsContainer.isVisible = isStatusExpanded
        rotateArrow(statusArrowImageView, isStatusExpanded)
    }

    private fun updateFriendsSection() = with(binding) {
        friendsDividerView.isVisible = isFriendsExpanded
        friendsMoreOptionsImageView.isVisible = isFriendsExpanded && friendsCount > 0

        if (!isFriendsExpanded) {
            noFriendsTextView.isVisible = false
            friendsRecyclerView.isVisible = false
            rotateArrow(friendsArrowImageView, false)
            return@with
        }

        if (friendsCount == 0) {
            noFriendsTextView.isVisible = true
            friendsRecyclerView.isVisible = false
        } else {
            noFriendsTextView.isVisible = false
            friendsRecyclerView.isVisible = true
        }

        rotateArrow(friendsArrowImageView, true)
    }

    private fun updateFriendRequestsSection() = with(binding) {
        friendRequestsCountTextView.text = friendRequestsCount.toString()
        friendRequestsDividerView.isVisible = isFriendRequestsExpanded

        if (!isFriendRequestsExpanded) {
            noFriendRequestsTextView.isVisible = false
            friendRequestsRecyclerView.isVisible = false
            rotateArrow(friendRequestsArrowImageView, false)
            return@with
        }

        if (friendRequestsCount == 0) {
            noFriendRequestsTextView.isVisible = true
            friendRequestsRecyclerView.isVisible = false
        } else {
            noFriendRequestsTextView.isVisible = false
            friendRequestsRecyclerView.isVisible = true
        }

        rotateArrow(friendRequestsArrowImageView, true)
    }

    private fun updateGameRequestsSection() = with(binding) {
        gameRequestsCountTextView.text = gameRequestsCount.toString()
        gameRequestsDividerView.isVisible = isGameRequestsExpanded

        if (!isGameRequestsExpanded) {
            noGameRequestsTextView.isVisible = false
            gameRequestsRecyclerView.isVisible = false
            rotateArrow(gameRequestsArrowImageView, false)
            return@with
        }

        if (gameRequestsCount == 0) {
            noGameRequestsTextView.isVisible = true
            gameRequestsRecyclerView.isVisible = false
        } else {
            gameRequestsRecyclerView.isVisible = true
            noGameRequestsTextView.isVisible = false
        }

        rotateArrow(gameRequestsArrowImageView, true)
    }

    private fun updateMenuSection() = with(binding) {
        menuDividerView.isVisible = isMenuExpanded
        editProfileSectionContainer.isVisible = isMenuExpanded
        themeSectionContainer.isVisible = isMenuExpanded
        helpSectionContainer.isVisible = isMenuExpanded
        aboutSectionContainer.isVisible = isMenuExpanded
        logoutSectionContainer.isVisible = isMenuExpanded

        rotateArrow(menuArrowImageView, isMenuExpanded)
    }

    private fun rotateArrow(view: View, isExpanded: Boolean) {
        view.animate()
            .rotation(if (isExpanded) 90f else 0f)
            .setDuration(200)
            .start()
    }

    private fun copyPlayerIdToClipboard() {
        val playerIdText = binding.playerIdTextView.text.toString()
            .removePrefix("ID: ")
            .trim()

        val clipboard = requireContext()
            .getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager

        val clip = ClipData.newPlainText("Player ID", playerIdText)
        clipboard.setPrimaryClip(clip)

        showToast("Player ID copied")
    }

    private fun showAvatarPicker() {
        val dialogBinding = DialogAvatarPickerBinding.inflate(LayoutInflater.from(requireContext()))
        val currentAvatar = UserManager.currentUser.avatarResId

        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setView(dialogBinding.root)
            .create()

        val avatarAdapter = AvatarAdapter(
            avatarList = avatarList,
            selectedAvatarResId = currentAvatar
        ) { selectedAvatar ->
            val firebaseUser = auth.currentUser

            UserManager.updateProfile(
                name = UserManager.currentUser.name,
                avatarResId = selectedAvatar
            )
            UserManager.saveUser(requireContext())

            binding.avatarImageView.setImageResource(selectedAvatar)

            UserManager.updateProfile(
                name = UserManager.currentUser.name,
                avatarResId = selectedAvatar
            )

            UserManager.saveUser(requireContext())

            showToast("Avatar updated")
            dialog.dismiss()
        }

        dialogBinding.avatarRecyclerView.apply {
            layoutManager = GridLayoutManager(requireContext(), 3)
            adapter = avatarAdapter
            setHasFixedSize(true)
        }

        dialog.show()
    }

    private fun UserStatus.toReadableText(): String {
        return when (this) {
            UserStatus.ONLINE -> "Online"
            UserStatus.OFFLINE -> "Offline"
            UserStatus.IN_GAME -> "In Game"
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

}