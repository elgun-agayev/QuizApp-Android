package com.example.quizapp.ui

import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import androidx.activity.addCallback
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.quizapp.R
import com.example.quizapp.adapter.ReviewAnswerAdapter
import com.example.quizapp.databinding.FragmentReviewAnswersBinding
import com.example.quizapp.manager.ReviewManager

class ReviewAnswersFragment : Fragment(R.layout.fragment_review_answers) {

    private var _binding: FragmentReviewAnswersBinding? = null
    private val binding get() = _binding!!

    private lateinit var reviewAnswerAdapter: ReviewAnswerAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        _binding = FragmentReviewAnswersBinding.bind(view)

        setupBackNavigation()
        setupPressedEffects()
        setupClickListeners()
        setupRecyclerView()
        loadReviewAnswers()
    }

    private fun setupBackNavigation() {
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner) {
            findNavController().navigateUp()
        }
    }

    private fun setupPressedEffects() {
        addPressedEffect(binding.reviewAnswersBackImageView)
    }

    private fun setupClickListeners() {
        binding.reviewAnswersBackImageView.setOnClickListener {
            findNavController().navigateUp()
        }
    }

    private fun setupRecyclerView() {
        binding.reviewAnswersRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            setHasFixedSize(false)
            isNestedScrollingEnabled = false
        }
    }

    private fun loadReviewAnswers() {
        val reviewList = ReviewManager.reviewList

        binding.reviewAnswersEmptyStateCardView.isVisible = reviewList.isEmpty()
        binding.reviewAnswersRecyclerView.isVisible = reviewList.isNotEmpty()

        if (reviewList.isNotEmpty()) {
            reviewAnswerAdapter = ReviewAnswerAdapter(reviewList)
            binding.reviewAnswersRecyclerView.adapter = reviewAnswerAdapter
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
                            .scaleX(0.92f)
                            .scaleY(0.92f)
                            .alpha(0.85f)
                            .setDuration(90)
                            .start()
                    }
                }

                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    view.animate()
                        .scaleX(1f)
                        .scaleY(1f)
                        .alpha(1f)
                        .setDuration(90)
                        .start()
                }
            }
            false
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}