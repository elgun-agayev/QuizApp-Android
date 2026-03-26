package com.example.quizapp.ui

import android.os.Bundle
import android.view.View
import android.widget.PopupMenu
import android.widget.Toast
import androidx.activity.addCallback
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.quizapp.R
import com.example.quizapp.databinding.FragmentLeaderboardBinding

class LeaderFragment : Fragment(R.layout.fragment_leaderboard) {

    private var _binding: FragmentLeaderboardBinding? = null
    private val binding get() = _binding!!

    private lateinit var leaderboardAdapter: LeaderboardAdapter

    private val weeklyList = listOf(
        LeaderboardPlayer("AEL-1003", "Michael", 1250, R.drawable.avatar_1, "online"),
        LeaderboardPlayer("AEL-1002", "Emma", 1180, R.drawable.avatar_2, "in_game"),
        LeaderboardPlayer("AEL-1005", "Daniel", 1100, R.drawable.avatar_3, "offline"),
        LeaderboardPlayer("AEL-1001", "Alex", 980, R.drawable.avatar_8, "online"),
        LeaderboardPlayer("AEL-1006", "David", 920, R.drawable.avatar_6, "offline"),
        LeaderboardPlayer("AEL-1007", "Lucas", 890, R.drawable.avatar_3, "online")
    )

    private val monthlyList = listOf(
        LeaderboardPlayer("AEL-1002", "Emma", 4200, R.drawable.avatar_2, "in_game"),
        LeaderboardPlayer("AEL-1003", "Michael", 4100, R.drawable.avatar_1, "online"),
        LeaderboardPlayer("AEL-1005", "Daniel", 3950, R.drawable.avatar_3, "offline"),
        LeaderboardPlayer("AEL-1001", "Alex", 3600, R.drawable.avatar_8, "online"),
        LeaderboardPlayer("AEL-1006", "David", 3400, R.drawable.avatar_6, "offline"),
        LeaderboardPlayer("AEL-1007", "Lucas", 3200, R.drawable.avatar_3, "offline")
    )

    private val overallList = listOf(
        LeaderboardPlayer("AEL-1003", "Michael", 15200, R.drawable.avatar_1, "online"),
        LeaderboardPlayer("AEL-1002", "Emma", 14950, R.drawable.avatar_2, "in_game"),
        LeaderboardPlayer("AEL-1005", "Daniel", 14300, R.drawable.avatar_3, "offline"),
        LeaderboardPlayer("AEL-1001", "Alex", 13820, R.drawable.avatar_8, "online"),
        LeaderboardPlayer("AEL-1006", "David", 12990, R.drawable.avatar_6, "offline"),
        LeaderboardPlayer("AEL-1007", "Lucas", 12050, R.drawable.avatar_3, "online")
    )

    private fun safeAvatar(avatarRes: Int): Int {
        return if (avatarRes != 0) avatarRes else R.drawable.ic_avatar_placeholder
    }

    private fun safeName(name: String): String {
        return name.trim().ifBlank { "Unknown Player" }
    }

    private fun safeId(id: String): String {
        return id.trim().ifBlank { "AEL-0000" }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        _binding = FragmentLeaderboardBinding.bind(view)

        setupBackNavigation()
        setupRecyclerView()
        setupClickListeners()

        showLeaderboard(weeklyList)
        selectWeeklyTab()
    }

    private fun setupBackNavigation() {
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner) {
            navigateToHome()
        }
    }

    private fun navigateToHome() {
        findNavController().navigate(R.id.action_leaderFragment_to_homeFragment)
    }

    private fun setupRecyclerView() {
        leaderboardAdapter = LeaderboardAdapter(
            onItemClick = { player ->
                openOtherPlayerProfile(player)
            },
            onMoreOptionsClick = { anchorView, player ->
                showPlayerOptionsMenu(anchorView, player)
            }
        )

        binding.leaderboardRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = leaderboardAdapter
            isNestedScrollingEnabled = false
        }
    }

    private fun setupClickListeners() {
        binding.weeklyButton.setOnClickListener {
            showLeaderboard(weeklyList)
            selectWeeklyTab()
        }

        binding.monthlyButton.setOnClickListener {
            showLeaderboard(monthlyList)
            selectMonthlyTab()
        }

        binding.overallButton.setOnClickListener {
            showLeaderboard(overallList)
            selectOverallTab()
        }
    }

    private fun showLeaderboard(fullList: List<LeaderboardPlayer>) {
        if (fullList.isEmpty()) {
            leaderboardAdapter.submitList(emptyList())
            return
        }

        val sortedList = fullList.sortedByDescending { it.playerScore }

        if (sortedList.size >= 3) {
            val firstPlayer = sortedList[0]
            val secondPlayer = sortedList[1]
            val thirdPlayer = sortedList[2]

            bindTopThreePlayers(firstPlayer, secondPlayer, thirdPlayer)

            val remainingPlayers = if (sortedList.size > 3) {
                sortedList.drop(3)
            } else {
                emptyList()
            }

            leaderboardAdapter.submitList(remainingPlayers)
        } else {
            leaderboardAdapter.submitList(sortedList)
        }
    }

    private fun bindTopThreePlayers(
        firstPlayer: LeaderboardPlayer,
        secondPlayer: LeaderboardPlayer,
        thirdPlayer: LeaderboardPlayer
    ) {
        binding.firstPlaceImageView.setImageResource(safeAvatar(firstPlayer.playerAvatar))
        binding.firstPlaceNameTextView.text = safeName(firstPlayer.playerName)
        binding.firstPlaceScoreTextView.text = "${firstPlayer.playerScore} XP"

        binding.secondPlaceImageView.setImageResource(safeAvatar(secondPlayer.playerAvatar))
        binding.secondPlaceNameTextView.text = safeName(secondPlayer.playerName)
        binding.secondPlaceScoreTextView.text = "${secondPlayer.playerScore} XP"

        binding.thirdPlaceImageView.setImageResource(safeAvatar(thirdPlayer.playerAvatar))
        binding.thirdPlaceNameTextView.text = safeName(thirdPlayer.playerName)
        binding.thirdPlaceScoreTextView.text = "${thirdPlayer.playerScore} XP"

        binding.firstPlaceImageView.setOnClickListener {
            openOtherPlayerProfile(firstPlayer)
        }

        binding.secondPlaceImageView.setOnClickListener {
            openOtherPlayerProfile(secondPlayer)
        }

        binding.thirdPlaceImageView.setOnClickListener {
            openOtherPlayerProfile(thirdPlayer)
        }
    }

    private fun openOtherPlayerProfile(player: LeaderboardPlayer) {
        Toast.makeText(
            requireContext(),
            "${player.playerName} profile opening...",
            Toast.LENGTH_SHORT
        ).show()

        val bundle = Bundle().apply {
            putString(OtherPlayerProfileFragment.ARG_PLAYER_NAME, safeName(player.playerName))
            putString(OtherPlayerProfileFragment.ARG_PLAYER_ID, safeId(player.id))
            putInt(OtherPlayerProfileFragment.ARG_PLAYER_SCORE, player.playerScore)
            putInt(OtherPlayerProfileFragment.ARG_PLAYER_AVATAR, safeAvatar(player.playerAvatar))
            putString(OtherPlayerProfileFragment.ARG_STATUS, formatStatus(player.playerStatus))
            putInt(OtherPlayerProfileFragment.ARG_TOTAL_GAMES, getMockTotalGames(player))
            putInt(OtherPlayerProfileFragment.ARG_CORRECT_ANSWERS, getMockCorrectAnswers(player))
            putString(OtherPlayerProfileFragment.ARG_WIN_RATE, getMockWinRate(player))
        }

        findNavController().navigate(
            R.id.otherPlayerProfileFragment,
            bundle
        )
    }

    private fun showPlayerOptionsMenu(anchorView: View, player: LeaderboardPlayer) {
        val popupMenu = PopupMenu(requireContext(), anchorView)
        popupMenu.menuInflater.inflate(R.menu.menu_leaderboard_player_options, popupMenu.menu)

        popupMenu.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.menu_view_profile -> {
                    openOtherPlayerProfile(player)
                    true
                }

                R.id.menu_add_friend -> {
                    Toast.makeText(
                        requireContext(),
                        "Friend request sent to ${player.playerName}",
                        Toast.LENGTH_SHORT
                    ).show()
                    true
                }

                else -> false
            }
        }

        popupMenu.show()
    }

    private fun formatStatus(status: String): String {
        return when (status.lowercase()) {
            "online" -> "Online"
            "offline" -> "Offline"
            "in_game" -> "In Game"
            "in game" -> "In Game"
            "searching" -> "Searching"
            "searching_match" -> "Searching"
            "in_challenge_room" -> "In Challenge Room"
            "in challenge room" -> "In Challenge Room"
            else -> "Offline"
        }
    }

    private fun getMockTotalGames(player: LeaderboardPlayer): Int {
        return when (player.id) {
            "AEL-1001" -> 97
            "AEL-1002" -> 119
            "AEL-1003" -> 128
            "AEL-1005" -> 111
            "AEL-1006" -> 88
            "AEL-1007" -> 80
            else -> 0
        }
    }

    private fun getMockCorrectAnswers(player: LeaderboardPlayer): Int {
        return when (player.id) {
            "AEL-1001" -> 720
            "AEL-1002" -> 876
            "AEL-1003" -> 934
            "AEL-1005" -> 801
            "AEL-1006" -> 654
            "AEL-1007" -> 610
            else -> 0
        }
    }

    private fun getMockWinRate(player: LeaderboardPlayer): String {
        return when (player.id) {
            "AEL-1001" -> "68%"
            "AEL-1002" -> "74%"
            "AEL-1003" -> "76%"
            "AEL-1005" -> "71%"
            "AEL-1006" -> "64%"
            "AEL-1007" -> "61%"
            else -> "0%"
        }
    }

    private fun selectWeeklyTab() {
        binding.weeklyButton.setBackgroundResource(R.drawable.bg_orange)
        binding.monthlyButton.setBackgroundResource(R.drawable.bg_white)
        binding.overallButton.setBackgroundResource(R.drawable.bg_white)
    }

    private fun selectMonthlyTab() {
        binding.weeklyButton.setBackgroundResource(R.drawable.bg_white)
        binding.monthlyButton.setBackgroundResource(R.drawable.bg_orange)
        binding.overallButton.setBackgroundResource(R.drawable.bg_white)
    }

    private fun selectOverallTab() {
        binding.weeklyButton.setBackgroundResource(R.drawable.bg_white)
        binding.monthlyButton.setBackgroundResource(R.drawable.bg_white)
        binding.overallButton.setBackgroundResource(R.drawable.bg_orange)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}