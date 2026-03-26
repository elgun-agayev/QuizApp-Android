package com.example.quizapp.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.activity.addCallback
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import com.example.quizapp.R
import com.example.quizapp.adapter.AvatarAdapter
import com.example.quizapp.databinding.DialogAvatarPickerBinding
import com.example.quizapp.databinding.FragmentEditProfileBinding
import com.example.quizapp.manager.UserManager
import com.example.quizapp.model.UserStatus
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import java.util.Locale

class EditProfileFragment : Fragment(R.layout.fragment_edit_profile) {

    private var _binding: FragmentEditProfileBinding? = null
    private val binding get() = _binding!!

    private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }
    private val firestore: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }

    private var selectedAvatarResId: Int = 0
    private var isChangePasswordMode = false

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

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentEditProfileBinding.bind(view)

        UserManager.loadUser(requireContext())

        setupBackNavigation()
        bindCurrentUser()
        setupReadOnlyFields()
        setupPasswordSection()
        setupClickListeners()
        setupPressedEffects()
        enableUserIdCopy()
        configurePasswordAvailability()
        setupTapOutsideToHideKeyboard()
    }

    private fun setupBackNavigation() {
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner) {
            hideKeyboard()
            navigateToProfile()
        }
    }

    private fun navigateToProfile() {
        hideKeyboard()
        findNavController().navigate(R.id.action_editProfileFragment_to_profileFragment)
    }

    private fun bindCurrentUser() = with(binding) {
        val currentFirebaseUser = auth.currentUser
        val currentAppUser = UserManager.currentUser

        val displayName = currentFirebaseUser?.displayName ?: currentAppUser.name
        val email = currentFirebaseUser?.email ?: ""
        val userId = if (currentAppUser.userId.isNotEmpty()) {
            currentAppUser.userId
        } else {
            currentFirebaseUser?.uid.orEmpty()
        }

        selectedAvatarResId = if (currentAppUser.avatarResId != 0) {
            currentAppUser.avatarResId
        } else {
            R.drawable.ic_avatar_placeholder
        }

        nameTextInputEditText.setText(displayName)
        gmailTextInputEditText.setText(email)
        userIdTextInputEditText.setText(userId)
        passwordPreviewEditText.setText("........")

        avatarShapeableImageView.setImageResource(selectedAvatarResId)
    }

    private fun setupReadOnlyFields() = with(binding) {
        gmailTextInputEditText.isFocusable = false
        gmailTextInputEditText.isFocusableInTouchMode = false
        gmailTextInputEditText.isClickable = false
        gmailTextInputEditText.isLongClickable = false
        gmailTextInputEditText.keyListener = null

        userIdTextInputEditText.isFocusable = false
        userIdTextInputEditText.isFocusableInTouchMode = false
        userIdTextInputEditText.isClickable = true
        userIdTextInputEditText.isLongClickable = true
        userIdTextInputEditText.keyListener = null

        passwordPreviewEditText.isFocusable = false
        passwordPreviewEditText.isFocusableInTouchMode = false
        passwordPreviewEditText.isClickable = false
        passwordPreviewEditText.isLongClickable = false
        passwordPreviewEditText.keyListener = null
    }

    private fun setupPasswordSection() = with(binding) {
        changePasswordContainer.isVisible = false
        newPasswordTextInputLayout.isVisible = false
        confirmPasswordTextInputLayout.isVisible = false

        oldPasswordEditText.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                val oldPassword = s.toString().trim()

                oldPasswordTextInputLayout.error = null
                newPasswordTextInputLayout.error = null
                confirmPasswordTextInputLayout.error = null

                val shouldShowNewPasswordFields =
                    isChangePasswordMode && oldPassword.isNotEmpty()

                newPasswordTextInputLayout.isVisible = shouldShowNewPasswordFields
                confirmPasswordTextInputLayout.isVisible = shouldShowNewPasswordFields

                if (!shouldShowNewPasswordFields) {
                    newPasswordEditText.text?.clear()
                    confirmPasswordEditText.text?.clear()
                }
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) =
                Unit

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit
        })
    }

    private fun configurePasswordAvailability() = with(binding) {
        val firebaseUser = auth.currentUser
        val hasPasswordProvider =
            firebaseUser?.providerData?.any { it.providerId == "password" } == true

        val passwordAllowed = firebaseUser == null || hasPasswordProvider

        changePasswordTextView.isEnabled = passwordAllowed
        passwordPreviewTextInputLayout.isEnabled = passwordAllowed
        passwordPreviewEditText.isEnabled = passwordAllowed

        if (!passwordAllowed) {
            changePasswordTextView.alpha = 0.5f
            passwordPreviewTextInputLayout.alpha = 0.5f
            passwordPreviewEditText.setText("Google account")
        } else {
            changePasswordTextView.alpha = 1f
            passwordPreviewTextInputLayout.alpha = 1f
            passwordPreviewEditText.setText("........")
        }
    }

    private fun setupClickListeners() = with(binding) {
        backImageView.setOnClickListener {
            navigateToProfile()
        }

        saveProfileTextView.setOnClickListener {
            saveProfile()
        }

        saveProfileButton.setOnClickListener {
            saveProfile()
        }

        avatarShapeableImageView.setOnClickListener {
            showAvatarPicker()
        }

        editAvatarCardView.setOnClickListener {
            showAvatarPicker()
        }

        editAvatarImageView.setOnClickListener {
            showAvatarPicker()
        }

        changePasswordTextView.setOnClickListener {
            if (changePasswordTextView.isEnabled) {
                toggleChangePasswordSection()
            }
        }

        passwordPreviewTextInputLayout.setOnClickListener {
            if (changePasswordTextView.isEnabled) {
                toggleChangePasswordSection()
            }
        }

        passwordPreviewEditText.setOnClickListener {
            if (changePasswordTextView.isEnabled) {
                toggleChangePasswordSection()
            }
        }
    }

    private fun toggleChangePasswordSection() = with(binding) {
        isChangePasswordMode = !isChangePasswordMode
        changePasswordContainer.isVisible = isChangePasswordMode

        if (isChangePasswordMode) {
            changePasswordTextView.text = "Cancel Password Change"
            oldPasswordEditText.requestFocus()

            profileEditNestedScrollView.post {
                profileEditNestedScrollView.smoothScrollTo(0, confirmPasswordEditText.bottom)
            }
        } else {
            changePasswordTextView.text = "Change Password"

            oldPasswordEditText.text?.clear()
            newPasswordEditText.text?.clear()
            confirmPasswordEditText.text?.clear()

            oldPasswordTextInputLayout.error = null
            newPasswordTextInputLayout.error = null
            confirmPasswordTextInputLayout.error = null

            newPasswordTextInputLayout.isVisible = false
            confirmPasswordTextInputLayout.isVisible = false
        }
    }

    private fun setupPressedEffects() = with(binding) {
        addPressedEffect(backImageView)
        addPressedEffect(saveProfileTextView)
        addPressedEffect(saveProfileButton)
        addPressedEffect(avatarShapeableImageView)
        addPressedEffect(editAvatarCardView)
        addPressedEffect(editAvatarImageView)
        addPressedEffect(changePasswordTextView)
        addPressedEffect(passwordPreviewTextInputLayout)
    }

    private fun enableUserIdCopy() = with(binding) {
        val copyAction = {
            val userId = userIdTextInputEditText.text?.toString()?.trim().orEmpty()

            if (userId.isNotEmpty()) {
                val clipboard = requireContext()
                    .getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager

                val clip = ClipData.newPlainText("User ID", userId)
                clipboard.setPrimaryClip(clip)

                Toast.makeText(requireContext(), "Copied", Toast.LENGTH_SHORT).show()
            }
        }

        userIdTextInputEditText.setOnClickListener {
            copyAction()
        }

        userIdTextInputEditText.setOnLongClickListener {
            copyAction()
            true
        }
    }

    private fun saveProfile() {
        val currentFirebaseUser = auth.currentUser
        val newName = binding.nameTextInputEditText.text?.toString()?.trim().orEmpty()

        binding.nameTextInputLayout.error = null

        if (newName.isEmpty()) {
            binding.nameTextInputLayout.error = "Name cannot be empty"
            return
        }

        if (newName.length < 3) {
            binding.nameTextInputLayout.error = "Name must be at least 3 characters"
            return
        }

        if (currentFirebaseUser == null) {
            saveProfileLocallyOnly(newName)
            return
        }

        val currentDisplayName = currentFirebaseUser.displayName ?: ""
        val shouldUpdateNameOnFirebase = newName != currentDisplayName

        if (shouldUpdateNameOnFirebase) {
            val profileUpdates = UserProfileChangeRequest.Builder()
                .setDisplayName(newName)
                .build()

            currentFirebaseUser.updateProfile(profileUpdates)
                .addOnSuccessListener {
                    updateFirestoreProfile(
                        firebaseUid = currentFirebaseUser.uid,
                        newName = newName,
                        onSuccess = {
                            updateLocalUserAfterProfileSave(newName)
                            handlePasswordUpdateIfNeeded()
                        },
                        onFailure = {
                            showToast("Failed to update profile")
                        }
                    )
                }
                .addOnFailureListener {
                    showToast("Failed to update name")
                }
        } else {
            updateFirestoreProfile(
                firebaseUid = currentFirebaseUser.uid,
                newName = newName,
                onSuccess = {
                    updateLocalUserAfterProfileSave(newName)
                    handlePasswordUpdateIfNeeded()
                },
                onFailure = {
                    showToast("Failed to update profile")
                }
            )
        }
    }

    private fun saveProfileLocallyOnly(newName: String) {
        UserManager.updateUser(
            name = newName,
            avatarResId = selectedAvatarResId
        )
        UserManager.saveUser(requireContext())

        if (isChangePasswordMode) {
            if (validatePasswordFieldsForLocalOnly()) {
                UserManager.saveUser(requireContext())
            } else {
                return
            }
        }

        showToast("Profile updated successfully")
        navigateToProfile()
    }

    private fun updateFirestoreProfile(
        firebaseUid: String,
        newName: String,
        onSuccess: () -> Unit,
        onFailure: () -> Unit
    ) {
        val updates = hashMapOf<String, Any>(
            "name" to newName,
            "nameLowercase" to newName.lowercase(Locale.getDefault()),
            "avatarResId" to selectedAvatarResId
        )

        updates["avatarUrl"] = ""
        updates["status"] = getUserStatusText(UserManager.currentUser.status)

        firestore.collection("users")
            .document(firebaseUid)
            .update(updates)
            .addOnSuccessListener {
                if (!isAdded || _binding == null) return@addOnSuccessListener
                onSuccess()
                UserManager.updateUser(
                    userId = UserManager.currentUser.userId,
                    name = updates["name"] as? String ?: UserManager.currentUser.name,
                    avatarResId = updates["avatarResId"] as? Int
                        ?: UserManager.currentUser.avatarResId,
                    score = UserManager.currentUser.score,
                    status = UserManager.currentUser.status,
                    totalGames = UserManager.currentUser.totalGames,
                    correctAnswers = UserManager.currentUser.correctAnswers
                )

                UserManager.saveUser(requireContext())
            }
            .addOnFailureListener {
                if (!isAdded || _binding == null) return@addOnFailureListener
                onFailure()
            }
    }

    private fun updateLocalUserAfterProfileSave(newName: String) {
        UserManager.updateUser(
            name = newName,
            avatarResId = selectedAvatarResId
        )
        UserManager.saveUser(requireContext())
    }

    private fun handlePasswordUpdateIfNeeded() {
        if (!isChangePasswordMode) {
            showToast("Profile updated successfully")
            navigateToProfile()
            return
        }

        changePassword()
    }

    private fun validatePasswordFieldsForLocalOnly(): Boolean {
        val oldPassword = binding.oldPasswordEditText.text?.toString()?.trim().orEmpty()
        val newPassword = binding.newPasswordEditText.text?.toString()?.trim().orEmpty()
        val confirmPassword = binding.confirmPasswordEditText.text?.toString()?.trim().orEmpty()

        binding.oldPasswordTextInputLayout.error = null
        binding.newPasswordTextInputLayout.error = null
        binding.confirmPasswordTextInputLayout.error = null

        if (oldPassword.isEmpty()) {
            binding.oldPasswordTextInputLayout.error = "Enter your old password"
            return false
        }

        if (newPassword.isEmpty()) {
            binding.newPasswordTextInputLayout.error = "Enter a new password"
            return false
        }

        if (newPassword.length < 8) {
            binding.newPasswordTextInputLayout.error = "Password must be at least 8 characters"
            return false
        }

        if (confirmPassword.isEmpty()) {
            binding.confirmPasswordTextInputLayout.error = "Confirm your new password"
            return false
        }

        if (newPassword != confirmPassword) {
            binding.confirmPasswordTextInputLayout.error = "Passwords do not match"
            return false
        }

        if (oldPassword == newPassword) {
            binding.newPasswordTextInputLayout.error = "New password must be different"
            return false
        }

        return true
    }

    private fun changePassword() {
        val currentUser = auth.currentUser ?: run {
            showToast("Profile updated successfully")
            navigateToProfile()
            return
        }

        val hasPasswordProvider = currentUser.providerData.any { it.providerId == "password" }
        if (!hasPasswordProvider) {
            showToast("Password change is not available for this account")
            return
        }

        val email = currentUser.email ?: run {
            showToast("Email not found")
            return
        }

        val oldPassword = binding.oldPasswordEditText.text?.toString()?.trim().orEmpty()
        val newPassword = binding.newPasswordEditText.text?.toString()?.trim().orEmpty()
        val confirmPassword = binding.confirmPasswordEditText.text?.toString()?.trim().orEmpty()

        binding.oldPasswordTextInputLayout.error = null
        binding.newPasswordTextInputLayout.error = null
        binding.confirmPasswordTextInputLayout.error = null

        if (oldPassword.isEmpty()) {
            binding.oldPasswordTextInputLayout.error = "Enter your old password"
            return
        }

        if (newPassword.isEmpty()) {
            binding.newPasswordTextInputLayout.error = "Enter a new password"
            return
        }

        if (newPassword.length < 8) {
            binding.newPasswordTextInputLayout.error = "Password must be at least 8 characters"
            return
        }

        if (confirmPassword.isEmpty()) {
            binding.confirmPasswordTextInputLayout.error = "Confirm your new password"
            return
        }

        if (newPassword != confirmPassword) {
            binding.confirmPasswordTextInputLayout.error = "Passwords do not match"
            return
        }

        if (oldPassword == newPassword) {
            binding.newPasswordTextInputLayout.error = "New password must be different"
            return
        }

        val credential = EmailAuthProvider.getCredential(email, oldPassword)

        currentUser.reauthenticate(credential)
            .addOnSuccessListener {
                currentUser.updatePassword(newPassword)
                    .addOnSuccessListener {
                        UserManager.saveUser(requireContext())

                        resetPasswordSectionAfterSuccess()
                        showToast("Password updated successfully")
                        navigateToProfile()
                    }
                    .addOnFailureListener {
                        showToast("Failed to update password")
                    }
            }
            .addOnFailureListener {
                binding.oldPasswordTextInputLayout.error = "Old password is incorrect"
            }
    }

    private fun resetPasswordSectionAfterSuccess() = with(binding) {
        oldPasswordEditText.text?.clear()
        newPasswordEditText.text?.clear()
        confirmPasswordEditText.text?.clear()

        oldPasswordTextInputLayout.error = null
        newPasswordTextInputLayout.error = null
        confirmPasswordTextInputLayout.error = null

        isChangePasswordMode = false
        changePasswordContainer.isVisible = false
        newPasswordTextInputLayout.isVisible = false
        confirmPasswordTextInputLayout.isVisible = false
        changePasswordTextView.text = "Change Password"
    }

    private fun showAvatarPicker() {
        val dialogBinding = DialogAvatarPickerBinding.inflate(LayoutInflater.from(requireContext()))

        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setView(dialogBinding.root)
            .create()

        val avatarAdapter = AvatarAdapter(
            avatarList = avatarList,
            selectedAvatarResId = selectedAvatarResId
        ) { selectedAvatar ->
            selectedAvatarResId = selectedAvatar
            binding.avatarShapeableImageView.setImageResource(selectedAvatar)
            dialog.dismiss()
        }

        dialogBinding.avatarRecyclerView.apply {
            layoutManager = GridLayoutManager(requireContext(), 3)
            adapter = avatarAdapter
            setHasFixedSize(true)
        }

        dialog.show()
    }

    private fun addPressedEffect(targetView: View) {
        targetView.isClickable = true
        targetView.isFocusable = true

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

    private fun setupTapOutsideToHideKeyboard() {
        binding.root.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_DOWN) {
                binding.nameTextInputEditText.clearFocus()
                binding.oldPasswordEditText.clearFocus()
                binding.newPasswordEditText.clearFocus()
                binding.confirmPasswordEditText.clearFocus()
                hideKeyboard()
            }
            false
        }
    }

    private fun hideKeyboard() {
        if (!isAdded || _binding == null) return
        val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
        val currentFocus = requireActivity().currentFocus ?: binding.root
        imm?.hideSoftInputFromWindow(currentFocus.windowToken, 0)
    }

    private fun getUserStatusText(status: UserStatus): String {
        return when (status) {
            UserStatus.ONLINE -> "online"
            UserStatus.OFFLINE -> "offline"
            UserStatus.IN_GAME -> "in_game"
        }
    }

    private fun showToast(message: String) {
        if (isAdded) {
            Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroyView() {
        hideKeyboard()
        super.onDestroyView()
        _binding = null
    }
}