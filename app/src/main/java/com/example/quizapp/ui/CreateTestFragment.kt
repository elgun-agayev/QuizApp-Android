package com.example.quizapp.ui

import android.content.Context
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.quizapp.R
import com.example.quizapp.databinding.FragmentCreateTestBinding

class CreateTestFragment : Fragment() {

    private var _binding: FragmentCreateTestBinding? = null
    private val binding get() = _binding!!

    private var isCategoryExpanded = false
    private var isDifficultyExpanded = false
    private var isQuestionCountExpanded = false

    private val prefsName = "created_quizzes_prefs"
    private val savedTestsKey = "saved_test_signatures"

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCreateTestBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupInitialState()
        setupTopicSpinner()
        setupCategorySection()
        setupDifficultySection()
        setupQuestionCountSection()
        setupTitleField()
        setupSaveButton()
    }

    private fun setupInitialState() = with(binding) {
        informaticsOptionContainer.visibility = View.GONE
        mathematicsOptionContainer.visibility = View.GONE
        englishOptionContainer.visibility = View.GONE
        historyOptionContainer.visibility = View.GONE
        worldKnowledgeOptionContainer.visibility = View.GONE
        logicOptionContainer.visibility = View.GONE

        easyOptionContainer.visibility = View.GONE
        mediumOptionContainer.visibility = View.GONE
        hardOptionContainer.visibility = View.GONE

        tenQuestionsOptionContainer.visibility = View.GONE
        twentyQuestionsOptionContainer.visibility = View.GONE
        thirtyQuestionsOptionContainer.visibility = View.GONE

        categoryArrowImageView.rotation = 0f
        difficultyArrowImageView.rotation = 0f
        questionCountArrowImageView.rotation = 0f

        categorySelectedIconImageView.visibility = View.GONE

        categoryLabelTextView.visibility = View.GONE
        difficultyLabelTextView.visibility = View.GONE
        questionCountLabelTextView.visibility = View.GONE
        topicLabelTextView.visibility = View.GONE
        quizTitleLabelTextView.visibility = View.GONE

        categoryTextView.text = "Select Category"
        difficultyTextView.text = "Select Difficulty"
        questionCountTextView.text = "Question Count"
        topicTextView.text = "Topic"

        categoryTextView.setTextColor(
            ContextCompat.getColor(requireContext(), R.color.color_text_hint)
        )
        difficultyTextView.setTextColor(
            ContextCompat.getColor(requireContext(), R.color.color_text_hint)
        )
        questionCountTextView.setTextColor(
            ContextCompat.getColor(requireContext(), R.color.color_text_hint)
        )
        topicTextView.setTextColor(
            ContextCompat.getColor(requireContext(), R.color.color_text_hint)
        )
    }

    private fun setupCategorySection() = with(binding) {
        val toggleCategoryAction = View.OnClickListener {
            closeOtherMenus(menuToKeepOpen = "category")
            isCategoryExpanded = !isCategoryExpanded
            updateCategoryVisibility()
        }

        categoryTextView.setOnClickListener(toggleCategoryAction)
        categoryArrowImageView.setOnClickListener(toggleCategoryAction)

        informaticsOptionContainer.setOnClickListener {
            selectCategory("Informatics", R.drawable.ic_category_informatics)
        }

        mathematicsOptionContainer.setOnClickListener {
            selectCategory("Mathematics", R.drawable.ic_catagory_math_pi)
        }

        englishOptionContainer.setOnClickListener {
            selectCategory("English", R.drawable.ic_category_english)
        }

        historyOptionContainer.setOnClickListener {
            selectCategory("History", R.drawable.ic_category_history)
        }

        worldKnowledgeOptionContainer.setOnClickListener {
            selectCategory("World Knowledge", R.drawable.ic_category_knowledge)
        }

        logicOptionContainer.setOnClickListener {
            selectCategory("Logic", R.drawable.ic_category_logic)
        }
    }

    private fun selectCategory(category: String, iconResId: Int) = with(binding) {
        categoryTextView.text = category
        categoryTextView.setTextColor(
            ContextCompat.getColor(requireContext(), R.color.color_black)
        )
        categorySelectedIconImageView.setImageResource(iconResId)
        categorySelectedIconImageView.visibility = View.VISIBLE
        showFloatingLabel(categoryLabelTextView)
        collapseCategoryMenu()
    }

    private fun setupDifficultySection() = with(binding) {
        val toggleDifficultyAction = View.OnClickListener {
            closeOtherMenus(menuToKeepOpen = "difficulty")
            isDifficultyExpanded = !isDifficultyExpanded
            updateDifficultyVisibility()
        }

        difficultyTextView.setOnClickListener(toggleDifficultyAction)
        difficultyArrowImageView.setOnClickListener(toggleDifficultyAction)

        easyOptionContainer.setOnClickListener {
            difficultyTextView.text = "Easy"
            difficultyTextView.setTextColor(
                ContextCompat.getColor(requireContext(), R.color.color_black)
            )
            showFloatingLabel(difficultyLabelTextView)
            collapseDifficultyMenu()
        }

        mediumOptionContainer.setOnClickListener {
            difficultyTextView.text = "Medium"
            difficultyTextView.setTextColor(
                ContextCompat.getColor(requireContext(), R.color.color_black)
            )
            showFloatingLabel(difficultyLabelTextView)
            collapseDifficultyMenu()
        }

        hardOptionContainer.setOnClickListener {
            difficultyTextView.text = "Hard"
            difficultyTextView.setTextColor(
                ContextCompat.getColor(requireContext(), R.color.color_black)
            )
            showFloatingLabel(difficultyLabelTextView)
            collapseDifficultyMenu()
        }
    }

    private fun setupQuestionCountSection() = with(binding) {
        val toggleQuestionCountAction = View.OnClickListener {
            closeOtherMenus(menuToKeepOpen = "questionCount")
            isQuestionCountExpanded = !isQuestionCountExpanded
            updateQuestionCountVisibility()
        }

        questionCountTextView.setOnClickListener(toggleQuestionCountAction)
        questionCountArrowImageView.setOnClickListener(toggleQuestionCountAction)

        tenQuestionsOptionContainer.setOnClickListener {
            questionCountTextView.text = "10"
            questionCountTextView.setTextColor(
                ContextCompat.getColor(requireContext(), R.color.color_black)
            )
            showFloatingLabel(questionCountLabelTextView)
            collapseQuestionCountMenu()
        }

        twentyQuestionsOptionContainer.setOnClickListener {
            questionCountTextView.text = "20"
            questionCountTextView.setTextColor(
                ContextCompat.getColor(requireContext(), R.color.color_black)
            )
            showFloatingLabel(questionCountLabelTextView)
            collapseQuestionCountMenu()
        }

        thirtyQuestionsOptionContainer.setOnClickListener {
            questionCountTextView.text = "30"
            questionCountTextView.setTextColor(
                ContextCompat.getColor(requireContext(), R.color.color_black)
            )
            showFloatingLabel(questionCountLabelTextView)
            collapseQuestionCountMenu()
        }
    }

    private fun setupTopicSpinner() = with(binding) {
        val topicList = listOf(
            "Topic",
            "Variables",
            "Loops",
            "Conditions",
            "Functions",
            "Arrays",
            "Algorithms"
        )

        val adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_dropdown_item,
            topicList
        )

        topicSpinner.adapter = adapter

        topicArrowImageView.setOnClickListener {
            topicSpinner.performClick()
        }

        topicTextView.setOnClickListener {
            topicSpinner.performClick()
        }

        topicSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                topicTextView.text = topicList[position]

                if (position == 0) {
                    topicTextView.setTextColor(
                        ContextCompat.getColor(requireContext(), R.color.color_text_hint)
                    )
                    hideFloatingLabel(topicLabelTextView)
                } else {
                    topicTextView.setTextColor(
                        ContextCompat.getColor(requireContext(), R.color.color_black)
                    )
                    showFloatingLabel(topicLabelTextView)
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) = Unit
        }
    }

    private fun setupTitleField() = with(binding) {
        quizTitleEditText.setOnFocusChangeListener { _, hasFocus ->
            val currentText = quizTitleEditText.text.toString().trim()

            if (hasFocus || currentText.isNotEmpty()) {
                showFloatingLabel(quizTitleLabelTextView)
            } else {
                hideFloatingLabel(quizTitleLabelTextView)
            }
        }

        quizTitleEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val currentText = s.toString().trim()

                if (currentText.isNotEmpty()) {
                    showFloatingLabel(quizTitleLabelTextView)
                } else if (!quizTitleEditText.hasFocus()) {
                    hideFloatingLabel(quizTitleLabelTextView)
                }
            }

            override fun afterTextChanged(s: Editable?) = Unit
        })
    }

    private fun setupSaveButton() = with(binding) {
        saveQuizButtonCard.setOnClickListener {
            val category = categoryTextView.text.toString().trim()
            val difficulty = difficultyTextView.text.toString().trim()
            val topic = topicTextView.text.toString().trim()
            val questionCount = questionCountTextView.text.toString().trim()
            val title = quizTitleEditText.text.toString().trim()

            if (category.isEmpty() || category == "Select Category") {
                showToast("Please select a category")
                return@setOnClickListener
            }

            if (difficulty.isEmpty() || difficulty == "Select Difficulty") {
                showToast("Please select a difficulty")
                return@setOnClickListener
            }

            if (topic.isEmpty() || topic == "Topic") {
                showToast("Please select a topic")
                return@setOnClickListener
            }

            if (questionCount.isEmpty() || questionCount == "Question Count") {
                showToast("Please select question count")
                return@setOnClickListener
            }

            if (title.isEmpty()) {
                showToast("Please enter a title")
                return@setOnClickListener
            }

            val testSignature = buildTestSignature(
                category = category,
                difficulty = difficulty,
                topic = topic,
                questionCount = questionCount,
                title = title
            )

            if (isTestAlreadySaved(testSignature)) {
                showToast("This quiz has already been saved")
                return@setOnClickListener
            }

            saveTestSignature(testSignature)
            showToast("Quiz saved successfully")
            findNavController().navigate(R.id.action_createTestFragment_to_homeFragment)
        }
    }

    private fun buildTestSignature(
        category: String,
        difficulty: String,
        topic: String,
        questionCount: String,
        title: String
    ): String {
        return listOf(
            category.lowercase(),
            difficulty.lowercase(),
            topic.lowercase(),
            questionCount.lowercase(),
            title.lowercase()
        ).joinToString("|")
    }

    private fun isTestAlreadySaved(signature: String): Boolean {
        val prefs = requireContext().getSharedPreferences(prefsName, Context.MODE_PRIVATE)
        val savedSet = prefs.getStringSet(savedTestsKey, emptySet()) ?: emptySet()
        return savedSet.contains(signature)
    }

    private fun saveTestSignature(signature: String) {
        val prefs = requireContext().getSharedPreferences(prefsName, Context.MODE_PRIVATE)
        val currentSet = prefs.getStringSet(savedTestsKey, emptySet()) ?: emptySet()
        val updatedSet = currentSet.toMutableSet().apply {
            add(signature)
        }

        prefs.edit()
            .putStringSet(savedTestsKey, updatedSet)
            .apply()
    }

    private fun updateCategoryVisibility() = with(binding) {
        val visibility = if (isCategoryExpanded) View.VISIBLE else View.GONE

        informaticsOptionContainer.visibility = visibility
        mathematicsOptionContainer.visibility = visibility
        englishOptionContainer.visibility = visibility
        historyOptionContainer.visibility = visibility
        worldKnowledgeOptionContainer.visibility = visibility
        logicOptionContainer.visibility = visibility

        rotateArrow(categoryArrowImageView, isCategoryExpanded)
    }

    private fun updateDifficultyVisibility() = with(binding) {
        val visibility = if (isDifficultyExpanded) View.VISIBLE else View.GONE

        easyOptionContainer.visibility = visibility
        mediumOptionContainer.visibility = visibility
        hardOptionContainer.visibility = visibility

        rotateArrow(difficultyArrowImageView, isDifficultyExpanded)
    }

    private fun updateQuestionCountVisibility() = with(binding) {
        val visibility = if (isQuestionCountExpanded) View.VISIBLE else View.GONE

        tenQuestionsOptionContainer.visibility = visibility
        twentyQuestionsOptionContainer.visibility = visibility
        thirtyQuestionsOptionContainer.visibility = visibility

        rotateArrow(questionCountArrowImageView, isQuestionCountExpanded)
    }

    private fun collapseCategoryMenu() {
        isCategoryExpanded = false
        updateCategoryVisibility()
    }

    private fun collapseDifficultyMenu() {
        isDifficultyExpanded = false
        updateDifficultyVisibility()
    }

    private fun collapseQuestionCountMenu() {
        isQuestionCountExpanded = false
        updateQuestionCountVisibility()
    }

    private fun closeOtherMenus(menuToKeepOpen: String) {
        if (menuToKeepOpen != "category" && isCategoryExpanded) {
            collapseCategoryMenu()
        }

        if (menuToKeepOpen != "difficulty" && isDifficultyExpanded) {
            collapseDifficultyMenu()
        }

        if (menuToKeepOpen != "questionCount" && isQuestionCountExpanded) {
            collapseQuestionCountMenu()
        }
    }

    private fun rotateArrow(arrowView: ImageView, isExpanded: Boolean) {
        arrowView.animate()
            .rotation(if (isExpanded) 180f else 0f)
            .setDuration(200)
            .start()
    }

    private fun showFloatingLabel(label: TextView) {
        if (label.visibility == View.VISIBLE) return

        label.visibility = View.VISIBLE
        label.alpha = 0f
        label.translationY = 10f

        label.animate()
            .alpha(1f)
            .translationY(0f)
            .setDuration(180)
            .start()
    }

    private fun hideFloatingLabel(label: TextView) {
        if (label.visibility != View.VISIBLE) return

        label.animate()
            .alpha(0f)
            .translationY(10f)
            .setDuration(150)
            .withEndAction {
                label.visibility = View.GONE
                label.alpha = 1f
                label.translationY = 0f
            }
            .start()
    }

    private fun showToast(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}