package com.example.quizapp.ui

import android.os.Bundle
import android.view.View
import androidx.activity.addCallback
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.quizapp.R
import com.example.quizapp.adapter.FavoriteAdapter
import com.example.quizapp.databinding.FragmentFavoritesBinding
import com.example.quizapp.manager.FavoriteManager

class FavoritesFragment : Fragment(R.layout.fragment_favorites) {

    private var _binding: FragmentFavoritesBinding? = null
    private val binding get() = _binding!!

    private lateinit var favoriteAdapter: FavoriteAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        _binding = FragmentFavoritesBinding.bind(view)

        setupBackNavigation()
        setupRecyclerView()
        loadFavorites()
    }

    override fun onResume() {
        super.onResume()
        loadFavorites()
    }

    private fun setupBackNavigation() {
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner) {
            navigateToHome()
        }
    }

    private fun navigateToHome() {
        findNavController().navigate(R.id.action_favoritesFragment_to_homeFragment)
    }

    private fun setupRecyclerView() {
        favoriteAdapter = FavoriteAdapter(mutableListOf()) {
            loadFavorites()
        }

        binding.favoritesRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = favoriteAdapter
            setHasFixedSize(false)
            isNestedScrollingEnabled = false
        }
    }

    private fun loadFavorites() {
        val favoriteQuestions = FavoriteManager.getFavorites().toMutableList()

        binding.favoritesEmptyStateCardView.isVisible = favoriteQuestions.isEmpty()
        binding.favoritesRecyclerView.isVisible = favoriteQuestions.isNotEmpty()

        if (::favoriteAdapter.isInitialized) {
            // Adapter yenidən yaradılmır → performans yaxşı
            favoriteAdapter.updateData(favoriteQuestions)
        }
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}