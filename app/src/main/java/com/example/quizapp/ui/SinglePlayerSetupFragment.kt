package com.example.quizapp.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.addCallback
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.quizapp.R
import com.example.quizapp.databinding.FragmentSinglePlayerSetupBinding

class SinglePlayerSetupFragment : Fragment() {

    private var _binding: FragmentSinglePlayerSetupBinding? = null
    private val binding get() = _binding!!

    private var selectedCategory: String? = null
    private var selectedDifficulty: String? = null

    private var isCategoryExpanded = false
    private var isDifficultyExpanded = false

    companion object {
        private const val DEFAULT_QUESTION_COUNT = 10
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSinglePlayerSetupBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupBackNavigation()
        setupInitialState()
        setupPressedEffects()
        setupClickListeners()
        refreshUi()
    }

    private fun setupBackNavigation() {
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner) {
            findNavController().navigateUp()
        }
    }

    private fun setupInitialState() = with(binding) {
        informaticsOptionContainer.isVisible = false
        mathematicsOptionContainer.isVisible = false
        englishOptionContainer.isVisible = false
        historyOptionContainer.isVisible = false
        worldviewOptionContainer.isVisible = false
        logicOptionContainer.isVisible = false

        easyOptionContainer.isVisible = false
        mediumOptionContainer.isVisible = false
        hardOptionContainer.isVisible = false

        categoryChipCardView.isVisible = false

        categoryLabelTextView.isVisible = false
        difficultyLabelTextView.isVisible = false

        categoryTextView.text = "Select Category"
        difficultyTextView.text = "Select Difficulty"

        categoryArrowImageView.rotation = 0f
        difficultyArrowImageView.rotation = 0f
    }

    private fun setupPressedEffects() = with(binding) {
        addPressedEffect(categorySelectorContainer)
        addPressedEffect(difficultySelectorContainer)

        addPressedEffect(categoryArrowImageView)
        addPressedEffect(difficultyArrowImageView)

        addPressedEffect(informaticsOptionContainer)
        addPressedEffect(mathematicsOptionContainer)
        addPressedEffect(englishOptionContainer)
        addPressedEffect(historyOptionContainer)
        addPressedEffect(worldviewOptionContainer)
        addPressedEffect(logicOptionContainer)

        addPressedEffect(easyOptionContainer)
        addPressedEffect(mediumOptionContainer)
        addPressedEffect(hardOptionContainer)

        addPressedEffect(searchPlayerButtonCard)
        addPressedEffect(searchPlayerButtonText)
    }

    private fun setupClickListeners() = with(binding) {
        categorySelectorContainer.setOnClickListener {
            toggleCategoryDropdown()
        }

        categoryArrowImageView.setOnClickListener {
            toggleCategoryDropdown()
        }

        difficultySelectorContainer.setOnClickListener {
            toggleDifficultyDropdown()
        }

        difficultyArrowImageView.setOnClickListener {
            toggleDifficultyDropdown()
        }

        informaticsOptionContainer.setOnClickListener {
            selectCategory("informatics", "Informatics")
        }

        mathematicsOptionContainer.setOnClickListener {
            selectCategory("mathematics", "Mathematics")
        }

        englishOptionContainer.setOnClickListener {
            selectCategory("english", "English")
        }

        historyOptionContainer.setOnClickListener {
            selectCategory("history", "History")
        }

        worldviewOptionContainer.setOnClickListener {
            selectCategory("world", "Worldview")
        }

        logicOptionContainer.setOnClickListener {
            selectCategory("logic", "Logic")
        }

        easyOptionContainer.setOnClickListener {
            selectDifficulty("easy", "Easy")
        }

        mediumOptionContainer.setOnClickListener {
            selectDifficulty("medium", "Medium")
        }

        hardOptionContainer.setOnClickListener {
            selectDifficulty("hard", "Hard")
        }

        searchPlayerButtonCard.setOnClickListener {
            openSinglePlayerMatchmaking()
        }

        searchPlayerButtonText.setOnClickListener {
            openSinglePlayerMatchmaking()
        }
    }

    private fun toggleCategoryDropdown() {
        isCategoryExpanded = !isCategoryExpanded
        if (isCategoryExpanded) {
            isDifficultyExpanded = false
        }
        refreshUi()
    }

    private fun toggleDifficultyDropdown() {
        isDifficultyExpanded = !isDifficultyExpanded
        if (isDifficultyExpanded) {
            isCategoryExpanded = false
        }
        refreshUi()
    }

    private fun selectCategory(categoryValue: String, displayText: String) {
        selectedCategory = categoryValue
        binding.categoryTextView.text = displayText
        isCategoryExpanded = false
        refreshUi()
    }

    private fun selectDifficulty(difficultyValue: String, displayText: String) {
        selectedDifficulty = difficultyValue
        binding.difficultyTextView.text = displayText
        isDifficultyExpanded = false
        refreshUi()
    }

    private fun refreshUi() {
        updateDropdownVisibility()
        updateArrowStates()
        updateSelectedStates()
        updateSelectorTextColors()
        updateCategoryChip()
        updateTopLabels()
    }

    private fun updateDropdownVisibility() = with(binding) {
        informaticsOptionContainer.isVisible = isCategoryExpanded
        mathematicsOptionContainer.isVisible = isCategoryExpanded
        englishOptionContainer.isVisible = isCategoryExpanded
        historyOptionContainer.isVisible = isCategoryExpanded
        worldviewOptionContainer.isVisible = isCategoryExpanded
        logicOptionContainer.isVisible = isCategoryExpanded

        easyOptionContainer.isVisible = isDifficultyExpanded
        mediumOptionContainer.isVisible = isDifficultyExpanded
        hardOptionContainer.isVisible = isDifficultyExpanded
    }

    private fun updateArrowStates() = with(binding) {
        rotateArrow(categoryArrowImageView, isCategoryExpanded)
        rotateArrow(difficultyArrowImageView, isDifficultyExpanded)
    }

    private fun updateSelectedStates() = with(binding) {
        setOptionSelected(informaticsOptionContainer, selectedCategory == "informatics")
        setOptionSelected(mathematicsOptionContainer, selectedCategory == "mathematics")
        setOptionSelected(englishOptionContainer, selectedCategory == "english")
        setOptionSelected(historyOptionContainer, selectedCategory == "history")
        setOptionSelected(worldviewOptionContainer, selectedCategory == "world")
        setOptionSelected(logicOptionContainer, selectedCategory == "logic")

        setOptionSelected(easyOptionContainer, selectedDifficulty == "easy")
        setOptionSelected(mediumOptionContainer, selectedDifficulty == "medium")
        setOptionSelected(hardOptionContainer, selectedDifficulty == "hard")
    }

    private fun setOptionSelected(view: View, isSelected: Boolean) {
        view.alpha = if (isSelected) 1f else 0.82f
    }

    private fun updateSelectorTextColors() = with(binding) {
        categoryTextView.setTextColor(
            ContextCompat.getColor(
                requireContext(),
                if (selectedCategory == null) R.color.color_text_hint else R.color.color_navy_blue
            )
        )

        difficultyTextView.setTextColor(
            ContextCompat.getColor(
                requireContext(),
                if (selectedDifficulty == null) R.color.color_text_hint else R.color.color_navy_blue
            )
        )
    }

    private fun updateCategoryChip() = with(binding) {
        categoryChipCardView.isVisible = false
    }

    private fun updateTopLabels() = with(binding) {
        categoryLabelTextView.isVisible = selectedCategory != null
        difficultyLabelTextView.isVisible = selectedDifficulty != null
    }

    private fun rotateArrow(arrowView: View, isExpanded: Boolean) {
        arrowView.animate()
            .rotation(if (isExpanded) 180f else 0f)
            .setDuration(180)
            .start()
    }

    private fun openSinglePlayerMatchmaking() {
        if (selectedCategory == null) {
            showToast("Please select a category")
            if (!isCategoryExpanded) {
                isCategoryExpanded = true
                isDifficultyExpanded = false
                refreshUi()
            }
            return
        }

        if (selectedDifficulty == null) {
            showToast("Please select a difficulty")
            if (!isDifficultyExpanded) {
                isDifficultyExpanded = true
                isCategoryExpanded = false
                refreshUi()
            }
            return
        }

        val bundle = Bundle().apply {
            putString("category", selectedCategory)
            putString("difficulty", selectedDifficulty)
            putInt("questionCount", DEFAULT_QUESTION_COUNT)
        }

        findNavController().navigate(
            R.id.action_singlePlayerSetupFragment_to_singlePlayerMatchmakingFragment,
            bundle
        )
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

    private fun showToast(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}