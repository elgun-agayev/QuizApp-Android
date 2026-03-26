package com.example.quizapp.ui

import android.content.Context
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.addCallback
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.quizapp.R
import com.example.quizapp.databinding.FragmentMultiplayerModeBinding

class MultiPlayerModeFragment : Fragment(R.layout.fragment_multiplayer_mode) {

    private var _binding: FragmentMultiplayerModeBinding? = null
    private val binding get() = _binding!!

    private var isRandomSectionEnabled = false
    private var isFriendsSectionEnabled = false
    private var isRandomCategoryExpanded = false
    private var isRandomTeamFormatExpanded = false
    private var isFriendsCategoryExpanded = false

    private var selectedRandomCategory: String? = null
    private var selectedFriendsCategory: String? = null
    private var selectedTeamFormat: String? = null

    companion object {
        private const val DEFAULT_CATEGORY_HINT = "Select Category"
        private const val DEFAULT_TEAM_FORMAT_HINT = "Select Team Format"
        private const val DEFAULT_FRIENDS_TEAM_FORMAT = "4v4"

        private const val KEY_SELECTED_CATEGORY = "selectedCategory"
        private const val KEY_SELECTED_TEAM_FORMAT = "selectedTeamFormat"

        private const val KEY_CATEGORY = "category"
        private const val KEY_TEAM_FORMAT = "teamFormat"
        private const val KEY_ROOM_ID = "roomId"
        private const val KEY_IS_ROOM_CREATOR = "isRoomCreator"
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentMultiplayerModeBinding.bind(view)

        setupBackNavigation()
        setupInitialState()
        setupPressedEffects()
        setupClickListeners()
    }

    private fun setupBackNavigation() {
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner) {
            if (
                isFriendsSectionEnabled &&
                binding.friendsRoomIdInputContainer.isVisible &&
                binding.roomIdFriendsEditText.isEnabled &&
                binding.roomIdFriendsEditText.hasFocus()
            ) {
                binding.roomIdFriendsEditText.clearFocus()
                hideKeyboard(binding.roomIdFriendsEditText)
                return@addCallback
            }

            findNavController().navigateUp()
        }
    }

    private fun setupInitialState() = with(binding) {
        collapseRandomCategoryOptions()
        collapseRandomTeamFormatOptions()
        collapseFriendsCategoryOptions()

        randomCategorySelectorContainer.isVisible = false
        randomTeamFormatSelectorContainer.isVisible = false

        friendsCategorySelectorContainer.isVisible = false
        friendsRoomIdInputContainer.isVisible = false

        searchPlayerMaterialCardView.isVisible = false
        friendsActionButtonsRow.isVisible = false

        categoryLabelTextView.isVisible = false
        teamLabelTextView.isVisible = false
        withfriendscategoryLabelTextView.isVisible = false

        randomCategoryChipCardView.isVisible = false
        friendsCategoryChipCardView.isVisible = false

        setRandomCategoryHintState()
        setFriendsCategoryHintState()
        setTeamFormatHintState()

        disableRoomIdInput(clearText = true)

        updateSectionCheckIcons()
        rotateArrow(randomCategoryArrowImageView, false)
        rotateArrow(randomTeamFormatArrowImageView, false)
        rotateArrow(friendsCategoryArrowImageView, false)
    }

    private fun setupPressedEffects() = with(binding) {
        addPressedEffect(randomPlayerSelectorContainer)
        addPressedEffect(randomPlayerToggleImageView)

        addPressedEffect(friendsSelectorContainer)
        addPressedEffect(playWithFriendsToggleImageView)

        addPressedEffect(randomCategorySelectorContainer)
        addPressedEffect(randomCategoryArrowImageView)

        addPressedEffect(randomTeamFormatSelectorContainer)
        addPressedEffect(randomTeamFormatArrowImageView)

        addPressedEffect(friendsCategorySelectorContainer)
        addPressedEffect(friendsCategoryArrowImageView)

        addPressedEffect(randomInformaticsOptionContainer)
        addPressedEffect(randomMathematicsOptionContainer)
        addPressedEffect(randomEnglishOptionContainer)
        addPressedEffect(randomHistoryOptionContainer)
        addPressedEffect(randomWorldviewOptionContainer)
        addPressedEffect(randomLogicOptionContainer)

        addPressedEffect(friendsInformaticsOptionContainer)
        addPressedEffect(friendsMathematicsOptionContainer)
        addPressedEffect(friendsEnglishOptionContainer)
        addPressedEffect(friendsHistoryOptionContainer)
        addPressedEffect(friendsWorldviewOptionContainer)
        addPressedEffect(friendsLogicOptionContainer)

        addPressedEffect(randomTwoVsTwoOptionContainer)
        addPressedEffect(randomThreeVsThreeOptionContainer)
        addPressedEffect(randomFourVsFourOptionContainer)

        addPressedEffect(searchPlayerMaterialCardView)
        addPressedEffect(createRoomMaterialCardView)
        addPressedEffect(joinByIdRoomMaterialCardView)
        addPressedEffect(roomIdFriendsEditText)
    }

    private fun setupClickListeners() = with(binding) {

        val randomToggleAction = View.OnClickListener {
            isRandomSectionEnabled = !isRandomSectionEnabled

            if (isRandomSectionEnabled) {
                isFriendsSectionEnabled = false
                collapseFriendsSectionState()
            } else {
                collapseRandomInteractiveState()
            }

            updateAllUi()
        }

        val friendsToggleAction = View.OnClickListener {
            isFriendsSectionEnabled = !isFriendsSectionEnabled

            if (isFriendsSectionEnabled) {
                isRandomSectionEnabled = false
                collapseRandomInteractiveState()
            } else {
                collapseFriendsSectionState()
            }

            updateAllUi()
        }

        randomPlayerSelectorContainer.setOnClickListener(randomToggleAction)
        randomPlayerToggleImageView.setOnClickListener(randomToggleAction)

        friendsSelectorContainer.setOnClickListener(friendsToggleAction)
        playWithFriendsToggleImageView.setOnClickListener(friendsToggleAction)

        val randomCategoryToggleAction = View.OnClickListener {
            if (!isRandomSectionEnabled) return@OnClickListener
            isRandomCategoryExpanded = !isRandomCategoryExpanded
            updateRandomCategoryOptions()
        }

        randomCategorySelectorContainer.setOnClickListener(randomCategoryToggleAction)
        randomCategoryArrowImageView.setOnClickListener(randomCategoryToggleAction)

        val randomTeamFormatToggleAction = View.OnClickListener {
            if (!isRandomSectionEnabled) return@OnClickListener
            isRandomTeamFormatExpanded = !isRandomTeamFormatExpanded
            updateRandomTeamFormatOptions()
        }

        randomTeamFormatSelectorContainer.setOnClickListener(randomTeamFormatToggleAction)
        randomTeamFormatArrowImageView.setOnClickListener(randomTeamFormatToggleAction)

        val friendsCategoryToggleAction = View.OnClickListener {
            if (!isFriendsSectionEnabled) return@OnClickListener
            isFriendsCategoryExpanded = !isFriendsCategoryExpanded
            updateFriendsCategoryOptions()
        }

        friendsCategorySelectorContainer.setOnClickListener(friendsCategoryToggleAction)
        friendsCategoryArrowImageView.setOnClickListener(friendsCategoryToggleAction)

        randomInformaticsOptionContainer.setOnClickListener {
            selectRandomCategory("Informatics", R.drawable.ic_chip_informatics)
        }

        randomMathematicsOptionContainer.setOnClickListener {
            selectRandomCategory("Mathematics", R.drawable.ic_chip_math_pi)
        }

        randomEnglishOptionContainer.setOnClickListener {
            selectRandomCategory("English", R.drawable.ic_chip_english)
        }

        randomHistoryOptionContainer.setOnClickListener {
            selectRandomCategory("History", R.drawable.ic_chip_history)
        }

        randomWorldviewOptionContainer.setOnClickListener {
            selectRandomCategory("Worldview", R.drawable.ic_chip_knowledge)
        }

        randomLogicOptionContainer.setOnClickListener {
            selectRandomCategory("Logic", R.drawable.ic_chip_logic)
        }

        friendsInformaticsOptionContainer.setOnClickListener {
            selectFriendsCategory("Informatics", R.drawable.ic_chip_informatics)
        }

        friendsMathematicsOptionContainer.setOnClickListener {
            selectFriendsCategory("Mathematics", R.drawable.ic_chip_math_pi)
        }

        friendsEnglishOptionContainer.setOnClickListener {
            selectFriendsCategory("English", R.drawable.ic_chip_english)
        }

        friendsHistoryOptionContainer.setOnClickListener {
            selectFriendsCategory("History", R.drawable.ic_chip_history)
        }

        friendsWorldviewOptionContainer.setOnClickListener {
            selectFriendsCategory("Worldview", R.drawable.ic_chip_knowledge)
        }

        friendsLogicOptionContainer.setOnClickListener {
            selectFriendsCategory("Logic", R.drawable.ic_chip_logic)
        }

        randomTwoVsTwoOptionContainer.setOnClickListener { selectTeamFormat("2v2") }
        randomThreeVsThreeOptionContainer.setOnClickListener { selectTeamFormat("3v3") }
        randomFourVsFourOptionContainer.setOnClickListener { selectTeamFormat("4v4") }

        searchPlayerMaterialCardView.setOnClickListener {
            onSearchPlayerClicked()
        }

        createRoomMaterialCardView.setOnClickListener {
            disableRoomIdInput(clearText = true)
            onCreateRoomClicked()
        }

        joinByIdRoomMaterialCardView.setOnClickListener {
            if (selectedFriendsCategory == null) {
                showToast("Please select a category")
                return@setOnClickListener
            }

            if (!roomIdFriendsEditText.isEnabled) {
                enableRoomIdInput()
                return@setOnClickListener
            }

            onJoinByIdClicked()
        }

        root.setOnClickListener {
            if (
                isFriendsSectionEnabled &&
                friendsRoomIdInputContainer.isVisible &&
                roomIdFriendsEditText.isEnabled
            ) {
                roomIdFriendsEditText.clearFocus()
                hideKeyboard(roomIdFriendsEditText)
            }
        }

        roomIdFriendsEditText.setOnClickListener {
        }
    }

    private fun updateAllUi() {
        updateRandomSectionState()
        updateFriendsSectionState()
        updateRandomCategoryOptions()
        updateRandomTeamFormatOptions()
        updateFriendsCategoryOptions()
        updateSectionCheckIcons()
    }

    private fun updateRandomSectionState() = with(binding) {
        randomCategorySelectorContainer.isVisible = isRandomSectionEnabled
        randomTeamFormatSelectorContainer.isVisible = isRandomSectionEnabled
        searchPlayerMaterialCardView.isVisible = isRandomSectionEnabled

        categoryLabelTextView.isVisible = isRandomSectionEnabled
        teamLabelTextView.isVisible = isRandomSectionEnabled

        randomCategoryChipCardView.isVisible = false

        if (!isRandomSectionEnabled) {
            collapseRandomInteractiveState()
        }
    }

    private fun updateFriendsSectionState() = with(binding) {
        friendsCategorySelectorContainer.isVisible = isFriendsSectionEnabled
        friendsRoomIdInputContainer.isVisible = isFriendsSectionEnabled
        friendsActionButtonsRow.isVisible = isFriendsSectionEnabled

        withfriendscategoryLabelTextView.isVisible = isFriendsSectionEnabled
        friendsCategoryChipCardView.isVisible = false

        if (!isFriendsSectionEnabled) {
            collapseFriendsSectionState()
        }
    }

    private fun updateRandomCategoryOptions() = with(binding) {
        val show = isRandomSectionEnabled && isRandomCategoryExpanded

        randomInformaticsOptionContainer.isVisible = show
        randomMathematicsOptionContainer.isVisible = show
        randomEnglishOptionContainer.isVisible = show
        randomHistoryOptionContainer.isVisible = show
        randomWorldviewOptionContainer.isVisible = show
        randomLogicOptionContainer.isVisible = show

        rotateArrow(randomCategoryArrowImageView, show)
    }

    private fun updateFriendsCategoryOptions() = with(binding) {
        val show = isFriendsSectionEnabled && isFriendsCategoryExpanded

        friendsInformaticsOptionContainer.isVisible = show
        friendsMathematicsOptionContainer.isVisible = show
        friendsEnglishOptionContainer.isVisible = show
        friendsHistoryOptionContainer.isVisible = show
        friendsWorldviewOptionContainer.isVisible = show
        friendsLogicOptionContainer.isVisible = show

        rotateArrow(friendsCategoryArrowImageView, show)
    }

    private fun updateRandomTeamFormatOptions() = with(binding) {
        val show = isRandomSectionEnabled && isRandomTeamFormatExpanded

        randomTwoVsTwoOptionContainer.isVisible = show
        randomThreeVsThreeOptionContainer.isVisible = show
        randomFourVsFourOptionContainer.isVisible = show

        rotateArrow(randomTeamFormatArrowImageView, show)
    }

    private fun selectRandomCategory(category: String, chipIconRes: Int) = with(binding) {
        selectedRandomCategory = category
        randomCategoryTextView.text = category
        randomCategoryTextView.setTextColor(
            ContextCompat.getColor(requireContext(), R.color.color_navy_blue)
        )

        randomCategoryChipTextView.text = category
        randomCategoryChipTextView.setCompoundDrawablesWithIntrinsicBounds(
            chipIconRes,
            0,
            0,
            0
        )

        randomCategoryChipCardView.isVisible = false
        isRandomCategoryExpanded = false
        updateRandomCategoryOptions()
    }

    private fun selectFriendsCategory(category: String, chipIconRes: Int) = with(binding) {
        selectedFriendsCategory = category
        friendsCategoryTextView.text = category
        friendsCategoryTextView.setTextColor(
            ContextCompat.getColor(requireContext(), R.color.color_navy_blue)
        )

        friendsCategoryChipTextView.text = category
        friendsCategoryChipTextView.setCompoundDrawablesWithIntrinsicBounds(
            chipIconRes,
            0,
            0,
            0
        )

        friendsCategoryChipCardView.isVisible = false
        isFriendsCategoryExpanded = false
        updateFriendsCategoryOptions()
    }

    private fun selectTeamFormat(format: String) = with(binding) {
        selectedTeamFormat = format
        randomTeamFormatTextView.text = format
        randomTeamFormatTextView.setTextColor(
            ContextCompat.getColor(requireContext(), R.color.color_navy_blue)
        )
        isRandomTeamFormatExpanded = false
        updateRandomTeamFormatOptions()
    }

    private fun updateSectionCheckIcons() = with(binding) {
        updateArrow(randomPlayerToggleImageView, isRandomSectionEnabled)
        updateArrow(playWithFriendsToggleImageView, isFriendsSectionEnabled)
    }

    private fun updateArrow(imageView: ImageView, isExpanded: Boolean) {
        imageView.setImageResource(R.drawable.ic_arrow_down)
        imageView.animate()
            .rotation(if (isExpanded) 180f else 0f)
            .setDuration(200)
            .start()
    }

    private fun rotateArrow(arrowView: ImageView, isExpanded: Boolean) {
        arrowView.animate()
            .rotation(if (isExpanded) 180f else 0f)
            .setDuration(200)
            .start()
    }

    private fun enableRoomIdInput() = with(binding) {
        roomIdFriendsEditText.isEnabled = true
        roomIdFriendsEditText.isFocusable = true
        roomIdFriendsEditText.isFocusableInTouchMode = true
        roomIdFriendsEditText.isClickable = true
        roomIdFriendsEditText.alpha = 1f
        roomIdFriendsEditText.requestFocus()

        val imm = requireContext()
            .getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.showSoftInput(roomIdFriendsEditText, InputMethodManager.SHOW_IMPLICIT)
    }

    private fun disableRoomIdInput(clearText: Boolean) = with(binding) {
        hideKeyboard(roomIdFriendsEditText)
        roomIdFriendsEditText.clearFocus()
        roomIdFriendsEditText.isEnabled = false
        roomIdFriendsEditText.isFocusable = false
        roomIdFriendsEditText.isFocusableInTouchMode = false
        roomIdFriendsEditText.isClickable = false
        roomIdFriendsEditText.alpha = 0.6f

        if (clearText) {
            roomIdFriendsEditText.text?.clear()
        }
    }

    private fun hideKeyboard(targetView: View) {
        val imm = requireContext()
            .getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
        imm?.hideSoftInputFromWindow(targetView.windowToken, 0)
    }

    private fun isValidRoomId(roomId: String): Boolean {
        val regex = Regex("^AEL-[A-Z0-9]{4}$")
        return regex.matches(roomId)
    }

    private fun onSearchPlayerClicked() {
        if (!isRandomSectionEnabled) {
            showToast("Please enable Random Player mode")
            return
        }

        if (selectedRandomCategory == null) {
            showToast("Please select a category")
            return
        }

        if (selectedTeamFormat == null) {
            showToast("Please select a team format")
            return
        }

        val bundle = Bundle().apply {
            putString(KEY_SELECTED_CATEGORY, selectedRandomCategory)
            putString(KEY_SELECTED_TEAM_FORMAT, selectedTeamFormat)
        }

        findNavController().navigate(
            R.id.action_multiPlayerModeFragment_to_multiPlayer_RandomFragment,
            bundle
        )
    }

    private fun onCreateRoomClicked() {
        if (!isFriendsSectionEnabled) {
            showToast("Please enable Play with Friends mode")
            return
        }

        if (selectedFriendsCategory == null) {
            showToast("Please select a category")
            return
        }

        val bundle = Bundle().apply {
            putString(KEY_CATEGORY, selectedFriendsCategory)
            putString(KEY_TEAM_FORMAT, selectedTeamFormat ?: DEFAULT_FRIENDS_TEAM_FORMAT)
            putBoolean(KEY_IS_ROOM_CREATOR, true)
        }

        findNavController().navigate(
            R.id.action_multiPlayerModeFragment_to_multiPlayerFriendFragment,
            bundle
        )
    }

    private fun onJoinByIdClicked() {
        if (!isFriendsSectionEnabled) {
            showToast("Please enable Play with Friends mode")
            return
        }

        if (selectedFriendsCategory == null) {
            showToast("Please select a category")
            return
        }

        val roomId = binding.roomIdFriendsEditText.text
            ?.toString()
            ?.trim()
            ?.uppercase()
            .orEmpty()

        binding.roomIdFriendsEditText.setText(roomId)
        binding.roomIdFriendsEditText.setSelection(roomId.length)

        if (roomId.isBlank()) {
            showToast("Please enter room ID")
            return
        }

        if (!isValidRoomId(roomId)) {
            showToast("Room ID must be in AEL-XXXX format")
            return
        }

        val bundle = Bundle().apply {
            putString(KEY_CATEGORY, selectedFriendsCategory)
            putString(KEY_TEAM_FORMAT, selectedTeamFormat ?: DEFAULT_FRIENDS_TEAM_FORMAT)
            putString(KEY_ROOM_ID, roomId)
            putBoolean(KEY_IS_ROOM_CREATOR, false)
        }

        findNavController().navigate(
            R.id.action_multiPlayerModeFragment_to_multiPlayerFriendFragment,
            bundle
        )
    }

    private fun collapseRandomInteractiveState() {
        isRandomCategoryExpanded = false
        isRandomTeamFormatExpanded = false
        collapseRandomCategoryOptions()
        collapseRandomTeamFormatOptions()
    }

    private fun collapseFriendsSectionState() {
        isFriendsCategoryExpanded = false
        collapseFriendsCategoryOptions()
        disableRoomIdInput(clearText = true)
    }

    private fun collapseRandomCategoryOptions() = with(binding) {
        randomInformaticsOptionContainer.isVisible = false
        randomMathematicsOptionContainer.isVisible = false
        randomEnglishOptionContainer.isVisible = false
        randomHistoryOptionContainer.isVisible = false
        randomWorldviewOptionContainer.isVisible = false
        randomLogicOptionContainer.isVisible = false
    }

    private fun collapseRandomTeamFormatOptions() = with(binding) {
        randomTwoVsTwoOptionContainer.isVisible = false
        randomThreeVsThreeOptionContainer.isVisible = false
        randomFourVsFourOptionContainer.isVisible = false
    }

    private fun collapseFriendsCategoryOptions() = with(binding) {
        friendsInformaticsOptionContainer.isVisible = false
        friendsMathematicsOptionContainer.isVisible = false
        friendsEnglishOptionContainer.isVisible = false
        friendsHistoryOptionContainer.isVisible = false
        friendsWorldviewOptionContainer.isVisible = false
        friendsLogicOptionContainer.isVisible = false
    }

    private fun setRandomCategoryHintState() = with(binding) {
        randomCategoryTextView.text = DEFAULT_CATEGORY_HINT
        randomCategoryTextView.setTextColor(
            ContextCompat.getColor(requireContext(), R.color.color_text_hint)
        )
    }

    private fun setFriendsCategoryHintState() = with(binding) {
        friendsCategoryTextView.text = DEFAULT_CATEGORY_HINT
        friendsCategoryTextView.setTextColor(
            ContextCompat.getColor(requireContext(), R.color.color_text_hint)
        )
    }

    private fun setTeamFormatHintState() = with(binding) {
        randomTeamFormatTextView.text = DEFAULT_TEAM_FORMAT_HINT
        randomTeamFormatTextView.setTextColor(
            ContextCompat.getColor(requireContext(), R.color.color_text_hint)
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