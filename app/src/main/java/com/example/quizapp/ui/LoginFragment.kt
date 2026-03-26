package com.example.quizapp.ui

import android.content.Context
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Patterns
import android.view.MotionEvent
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.quizapp.R
import com.example.quizapp.databinding.FragmentLoginBinding
import com.example.quizapp.manager.UserManager
import com.example.quizapp.model.UserStatus
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import java.util.Locale
import kotlin.random.Random

class LoginFragment : Fragment(R.layout.fragment_login) {

    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!

    private lateinit var auth: FirebaseAuth
    private lateinit var googleSignInClient: GoogleSignInClient
    private lateinit var firestore: FirebaseFirestore

    private var isNavigating = false
    private var isGoogleLoading = false
    private var isEmailLoading = false

    private val googleSignInLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            val data = result.data
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)

            try {
                val account = task.getResult(ApiException::class.java)
                if (account == null) {
                    setGoogleLoading(false)
                    showToast("Google sign-in failed.")
                    return@registerForActivityResult
                }

                handleGoogleAccountSelection(account)

            } catch (_: ApiException) {
                setGoogleLoading(false)
                showToast("Google sign-in failed.")
            }
        }

    companion object {
        private const val USERS_COLLECTION = "users"
        private const val PASSWORD_PROVIDER = "password"
        private const val GOOGLE_PROVIDER = "google"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()
    }

    override fun onStart() {
        super.onStart()
        checkExistingLoggedInUser()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentLoginBinding.bind(view)

        setupGoogleSignIn()
        setupInputValidationUI()
        setupClickListeners()
        setupKeyboardDismissOnBack()
        setupTapOutsideToHideKeyboard()
    }

    private fun setupGoogleSignIn() {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()

        googleSignInClient = GoogleSignIn.getClient(requireActivity(), gso)
    }

    private fun setupInputValidationUI() = with(binding) {
        updateEmailVisualState(emailEditText.text?.toString().orEmpty(), showError = false)
        updatePasswordVisualState(passwordEditText.text?.toString().orEmpty(), showError = false)

        emailEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit
            override fun afterTextChanged(s: Editable?) {
                if (!isAdded || _binding == null) return
                updateEmailVisualState(s?.toString().orEmpty(), showError = true)
            }
        })

        passwordEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit
            override fun afterTextChanged(s: Editable?) {
                if (!isAdded || _binding == null) return
                updatePasswordVisualState(s?.toString().orEmpty(), showError = true)
            }
        })
    }

    private fun updateEmailVisualState(rawEmail: String, showError: Boolean) {
        if (!isAdded || _binding == null) return

        val email = rawEmail.trim()
        val isValid = email.isNotEmpty() && Patterns.EMAIL_ADDRESS.matcher(email).matches()

        if (isValid) {
            binding.emailInputLayout.endIconMode = TextInputLayout.END_ICON_CUSTOM
            binding.emailInputLayout.setEndIconDrawable(R.drawable.ic_email)
        } else {
            binding.emailInputLayout.endIconMode = TextInputLayout.END_ICON_NONE
        }

        binding.emailInputLayout.error = when {
            !showError -> null
            email.isEmpty() -> null
            !isValid -> "Invalid email format."
            else -> null
        }
    }

    private fun updatePasswordVisualState(rawPassword: String, showError: Boolean) {
        if (!isAdded || _binding == null) return

        val password = rawPassword.trim()
        val isValid = password.isNotEmpty()

        if (isValid) {
            binding.passwordInputLayout.endIconMode = TextInputLayout.END_ICON_PASSWORD_TOGGLE
        } else {
            binding.passwordInputLayout.endIconMode = TextInputLayout.END_ICON_NONE
        }

        binding.passwordInputLayout.error = when {
            !showError -> null
            password.isEmpty() -> null
            else -> null
        }
    }

    private fun setupClickListeners() = with(binding) {
        registerTextView.setOnClickListener {
            if (findNavController().currentDestination?.id == R.id.loginFragment) {
                findNavController().navigate(R.id.action_loginFragment_to_registerFragment)
            }
        }

        loginButton.setOnClickListener {
            loginUser()
        }

        googleSignInButton.setOnClickListener {
            signInWithGoogle()
        }

        forgotPasswordTextView.setOnClickListener {
            sendPasswordResetLink()
        }
    }

    private fun checkExistingLoggedInUser() {
        val currentUser = auth.currentUser ?: return

        val isGoogleUser = currentUser.providerData.any { it.providerId == "google.com" }
        val isPasswordUser = currentUser.providerData.any { it.providerId == "password" }

        when {
            isGoogleUser -> {
                val email = currentUser.email
                if (email.isNullOrBlank()) {
                    signOutGoogleAndFirebase()
                    return
                }

                checkGoogleAccountExistsByEmail(
                    email = email,
                    onExists = {
                        syncGoogleUserDocumentByEmail(
                            firebaseUser = currentUser,
                            onSuccess = { navigateToHome() },
                            onFailure = { signOutGoogleAndFirebase() }
                        )
                    },
                    onNotExists = {
                        signOutGoogleAndFirebase()
                    }
                )
            }

            isPasswordUser -> {
                if (!currentUser.isEmailVerified) {
                    auth.signOut()
                    return
                }

                ensurePasswordUserDocument(
                    firebaseUser = currentUser,
                    onSuccess = { navigateToHome() },
                    onFailure = { auth.signOut() }
                )
            }

            else -> {
                auth.signOut()
            }
        }
    }

    private fun loginUser() {
        val email = binding.emailEditText.text?.toString().orEmpty().trim()
        val password = binding.passwordEditText.text?.toString().orEmpty().trim()

        updateEmailVisualState(email, showError = true)
        updatePasswordVisualState(password, showError = true)

        when {
            email.isEmpty() -> {
                binding.emailEditText.requestFocus()
                showToast("Enter your email.")
                return
            }

            !Patterns.EMAIL_ADDRESS.matcher(email).matches() -> {
                binding.emailEditText.requestFocus()
                showToast("Invalid email format.")
                return
            }

            password.isEmpty() -> {
                binding.passwordEditText.requestFocus()
                showToast("Enter your password.")
                return
            }
        }

        setEmailLoading(true)

        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (!isAdded || _binding == null) return@addOnCompleteListener

                setEmailLoading(false)

                if (task.isSuccessful) {
                    val user = auth.currentUser

                    if (user == null) {
                        auth.signOut()
                        showToast("Login failed.")
                        return@addOnCompleteListener
                    }

                    if (!user.isEmailVerified) {
                        auth.signOut()
                        showToast("Please verify your email before logging in.")
                        return@addOnCompleteListener
                    }

                    ensurePasswordUserDocument(
                        firebaseUser = user,
                        onSuccess = {
                            showToast("Login successful.")
                            navigateToHome()
                        },
                        onFailure = {
                            auth.signOut()
                            showToast("Failed to complete account setup.")
                        }
                    )
                } else {
                    val message = when ((task.exception as? FirebaseAuthException)?.errorCode) {
                        "ERROR_WRONG_PASSWORD" -> "Incorrect password."
                        "ERROR_INVALID_EMAIL" -> "Invalid email format."
                        "ERROR_USER_DISABLED" -> "This account has been disabled."
                        "ERROR_USER_NOT_FOUND" -> "User not found."
                        "ERROR_NETWORK_REQUEST_FAILED" -> "Network error occurred."
                        else -> "Login failed."
                    }

                    showToast(message)
                }
            }
    }

    private fun sendPasswordResetLink() {
        val email = binding.emailEditText.text?.toString().orEmpty().trim()

        updateEmailVisualState(email, showError = true)

        when {
            email.isEmpty() -> {
                binding.emailInputLayout.error = "Email is required."
                binding.emailEditText.requestFocus()
                showToast("Please enter your email.")
                return
            }

            !Patterns.EMAIL_ADDRESS.matcher(email).matches() -> {
                binding.emailInputLayout.error = "Invalid email format."
                binding.emailEditText.requestFocus()
                showToast("Invalid email format.")
                return
            }
        }

        auth.sendPasswordResetEmail(email)
            .addOnSuccessListener {
                if (!isAdded || _binding == null) return@addOnSuccessListener
                showToast("Password reset link sent to your email.")
            }
            .addOnFailureListener { exception ->
                if (!isAdded || _binding == null) return@addOnFailureListener

                val message = when ((exception as? FirebaseAuthException)?.errorCode) {
                    "ERROR_USER_NOT_FOUND" -> "User not found."
                    "ERROR_INVALID_EMAIL" -> "Invalid email format."
                    "ERROR_NETWORK_REQUEST_FAILED" -> "Network error occurred."
                    else -> exception.localizedMessage ?: "Failed to send reset link."
                }

                showToast(message)
            }
    }

    private fun ensurePasswordUserDocument(
        firebaseUser: FirebaseUser,
        onSuccess: () -> Unit,
        onFailure: () -> Unit
    ) {
        val uid = firebaseUser.uid

        firestore.collection(USERS_COLLECTION)
            .document(uid)
            .get()
            .addOnSuccessListener { document ->
                if (!isAdded || _binding == null) return@addOnSuccessListener

                if (document.exists()) {
                    updatePasswordUserBasicInfo(
                        firebaseUser = firebaseUser,
                        onSuccess = onSuccess,
                        onFailure = onFailure
                    )
                } else {
                    createFirestoreUserForVerifiedEmailUser(
                        firebaseUser = firebaseUser,
                        onSuccess = onSuccess,
                        onFailure = onFailure
                    )
                }
            }
            .addOnFailureListener {
                if (!isAdded || _binding == null) return@addOnFailureListener
                onFailure()
            }
    }

    private fun createFirestoreUserForVerifiedEmailUser(
        firebaseUser: FirebaseUser,
        onSuccess: () -> Unit,
        onFailure: () -> Unit
    ) {
        val uid = firebaseUser.uid
        val email = firebaseUser.email

        if (email.isNullOrBlank()) {
            onFailure()
            return
        }

        val displayName = firebaseUser.displayName?.trim()
        val finalName = if (!displayName.isNullOrBlank()) {
            displayName
        } else {
            email.substringBefore("@")
        }
        val firstName = finalName.split("\\s+".toRegex()).firstOrNull().orEmpty().ifBlank { "Player" }
        val playerId = generatePlayerId()

        val userMap = hashMapOf(
            "uid" to uid,
            "playerId" to playerId,
            "name" to finalName,
            "firstName" to firstName,
            "nameLowercase" to finalName.lowercase(Locale.getDefault()),
            "email" to email,
            "avatarUrl" to "",
            "coins" to 0,
            "wins" to 0,
            "losses" to 0,
            "score" to 0,
            "status" to "online" ,
            "totalGames" to 0,
            "correctAnswers" to 0,
            "provider" to PASSWORD_PROVIDER,
            "createdAt" to Timestamp.now(),
            "lastLoginAt" to Timestamp.now()
        )

        firestore.collection(USERS_COLLECTION)
            .document(uid)
            .set(userMap)
            .addOnSuccessListener {
                if (!isAdded || _binding == null) return@addOnSuccessListener
                UserManager.updateUser(
                    userId = playerId,
                    name = firstName,
                    avatarResId = R.drawable.ic_avatar_placeholder,
                    score = 0,
                    status = UserStatus.ONLINE,
                    totalGames = 0,
                    correctAnswers = 0
                )
                UserManager.saveUser(requireContext())
                onSuccess()
            }
            .addOnFailureListener {
                if (!isAdded || _binding == null) return@addOnFailureListener
                onFailure()
            }
    }

    private fun updatePasswordUserBasicInfo(
        firebaseUser: FirebaseUser,
        onSuccess: () -> Unit,
        onFailure: () -> Unit
    ) {
        val uid = firebaseUser.uid
        val email = firebaseUser.email

        if (email.isNullOrBlank()) {
            onFailure()
            return
        }

        firestore.collection(USERS_COLLECTION)
            .document(uid)
            .get()
            .addOnSuccessListener { document ->
                if (!isAdded || _binding == null) return@addOnSuccessListener

                val existingPlayerId = document.getString("playerId").orEmpty()
                val existingScore = document.getLong("score")?.toInt() ?: 0
                val existingStatus = when (document.getString("status").orEmpty().ifBlank { "online" }) {
                    "online" -> UserStatus.ONLINE
                    "offline" -> UserStatus.OFFLINE
                    "in_game" -> UserStatus.IN_GAME
                    else -> UserStatus.OFFLINE
                }
                val existingTotalGames = document.getLong("totalGames")?.toInt() ?: 0
                val existingCorrectAnswers = document.getLong("correctAnswers")?.toInt() ?: 0

                val displayName = firebaseUser.displayName?.trim()
                val finalName = if (!displayName.isNullOrBlank()) {
                    displayName
                } else {
                    email.substringBefore("@")
                }
                val firstName = finalName.split("\\s+".toRegex()).firstOrNull().orEmpty().ifBlank { "Player" }

                val updates = hashMapOf<String, Any>(
                    "uid" to uid,
                    "name" to finalName,
                    "firstName" to firstName,
                    "nameLowercase" to finalName.lowercase(Locale.getDefault()),
                    "email" to email,
                    "provider" to PASSWORD_PROVIDER,
                    "status" to "online" ,
                    "lastLoginAt" to Timestamp.now()
                )

                firestore.collection(USERS_COLLECTION)
                    .document(uid)
                    .update(updates)
                    .addOnSuccessListener {
                        if (!isAdded || _binding == null) return@addOnSuccessListener
                        UserManager.updateUser(
                            userId = existingPlayerId,
                            name = firstName,
                            avatarResId = UserManager.currentUser.avatarResId.takeIf { it != 0 }
                                ?: R.drawable.ic_avatar_placeholder,
                            score = existingScore,
                            status = existingStatus,
                            totalGames = existingTotalGames,
                            correctAnswers = existingCorrectAnswers
                        )
                        UserManager.saveUser(requireContext())
                        onSuccess()
                    }
                    .addOnFailureListener {
                        if (!isAdded || _binding == null) return@addOnFailureListener
                        onFailure()
                    }
            }
            .addOnFailureListener {
                if (!isAdded || _binding == null) return@addOnFailureListener
                onFailure()
            }
    }

    private fun signInWithGoogle() {
        if (isGoogleLoading) return

        setGoogleLoading(true)

        val signInIntent = googleSignInClient.signInIntent
        googleSignInLauncher.launch(signInIntent)
    }

    private fun handleGoogleAccountSelection(account: GoogleSignInAccount) {
        val email = account.email?.trim()
        val idToken = account.idToken

        if (email.isNullOrBlank()) {
            setGoogleLoading(false)
            googleSignInClient.signOut()
            showToast("Google email not found.")
            return
        }

        if (idToken.isNullOrBlank()) {
            setGoogleLoading(false)
            googleSignInClient.signOut()
            showToast("Google token is null.")
            return
        }

        checkGoogleAccountExistsByEmail(
            email = email,
            onExists = {
                firebaseAuthWithGoogle(idToken)
            },
            onNotExists = {
                setGoogleLoading(false)
                signOutGoogleAndFirebase()
                showToast("No account found with this Google account.")
            }
        )
    }

    private fun checkGoogleAccountExistsByEmail(
        email: String,
        onExists: () -> Unit,
        onNotExists: () -> Unit
    ) {
        firestore.collection(USERS_COLLECTION)
            .whereEqualTo("email", email)
            .limit(1)
            .get()
            .addOnSuccessListener { documents ->
                if (!isAdded || _binding == null) return@addOnSuccessListener

                if (documents.isEmpty) {
                    onNotExists()
                } else {
                    onExists()
                }
            }
            .addOnFailureListener {
                if (!isAdded || _binding == null) return@addOnFailureListener
                setGoogleLoading(false)
                showToast("Failed to check Google account.")
            }
    }

    private fun firebaseAuthWithGoogle(idToken: String) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)

        auth.signInWithCredential(credential)
            .addOnCompleteListener(requireActivity()) { task ->
                if (!isAdded || _binding == null) return@addOnCompleteListener

                if (task.isSuccessful) {
                    val firebaseUser = auth.currentUser

                    if (firebaseUser == null) {
                        setGoogleLoading(false)
                        signOutGoogleAndFirebase()
                        showToast("Google login failed.")
                        return@addOnCompleteListener
                    }

                    syncGoogleUserDocumentByEmail(
                        firebaseUser = firebaseUser,
                        onSuccess = {
                            setGoogleLoading(false)
                            showToast("Google login successful.")
                            navigateToHome()
                        },
                        onFailure = {
                            setGoogleLoading(false)
                            signOutGoogleAndFirebase()
                            showToast("No account found with this Google account.")
                        }
                    )
                } else {
                    setGoogleLoading(false)

                    val exception = task.exception
                    val message = when {
                        exception is FirebaseAuthInvalidCredentialsException -> {
                            "Google authentication failed."
                        }

                        exception is FirebaseAuthException &&
                                exception.errorCode == "ERROR_ACCOUNT_EXISTS_WITH_DIFFERENT_CREDENTIAL" -> {
                            "This email is already registered with a different sign-in method."
                        }

                        else -> {
                            "Google authentication failed."
                        }
                    }

                    signOutGoogleAndFirebase()
                    showToast(message)
                }
            }
    }

    private fun syncGoogleUserDocumentByEmail(
        firebaseUser: FirebaseUser,
        onSuccess: () -> Unit,
        onFailure: () -> Unit
    ) {
        val email = firebaseUser.email?.trim()

        if (email.isNullOrBlank()) {
            onFailure()
            return
        }

        firestore.collection(USERS_COLLECTION)
            .whereEqualTo("email", email)
            .limit(1)
            .get()
            .addOnSuccessListener { documents ->
                if (!isAdded || _binding == null) return@addOnSuccessListener

                if (documents.isEmpty) {
                    onFailure()
                    return@addOnSuccessListener
                }

                val document = documents.documents.first()
                val docRef = document.reference

                val existingPlayerId = document.getString("playerId").orEmpty()
                val existingName = document.getString("name")?.trim()
                val existingFirstName = document.getString("firstName")?.trim()
                val existingAvatarUrl = document.getString("avatarUrl").orEmpty()
                val existingScore = document.getLong("score")?.toInt() ?: 0
                val existingStatus = when (document.getString("status").orEmpty().ifBlank { "online" }) {
                    "online" -> UserStatus.ONLINE
                    "offline" -> UserStatus.OFFLINE
                    "in_game" -> UserStatus.IN_GAME
                    else -> UserStatus.OFFLINE
                }
                val existingTotalGames = document.getLong("totalGames")?.toInt() ?: 0
                val existingCorrectAnswers = document.getLong("correctAnswers")?.toInt() ?: 0
                val displayName = firebaseUser.displayName?.trim()
                val fullName = when {
                    !existingName.isNullOrBlank() -> existingName
                    !displayName.isNullOrBlank() -> displayName
                    else -> email.substringBefore("@")
                }
                val firstName = when {
                    !existingFirstName.isNullOrBlank() -> existingFirstName
                    else -> fullName.split("\\s+".toRegex()).firstOrNull().orEmpty().ifBlank { "Player" }
                }

                val updates = hashMapOf<String, Any>(
                    "uid" to firebaseUser.uid,
                    "name" to fullName,
                    "firstName" to firstName,
                    "nameLowercase" to fullName.lowercase(Locale.getDefault()),
                    "email" to email,
                    "provider" to GOOGLE_PROVIDER,
                    "status" to "online",
                    "lastLoginAt" to Timestamp.now()
                )

                if (existingAvatarUrl.isBlank() && firebaseUser.photoUrl != null) {
                    updates["avatarUrl"] = firebaseUser.photoUrl.toString()
                }

                docRef.update(updates)
                    .addOnSuccessListener {
                        if (!isAdded || _binding == null) return@addOnSuccessListener
                        UserManager.updateUser(
                            userId = existingPlayerId,
                            name = firstName,
                            avatarResId = R.drawable.ic_avatar_placeholder,
                            score = existingScore,
                            status = existingStatus,
                            totalGames = existingTotalGames,
                            correctAnswers = existingCorrectAnswers
                        )
                        UserManager.saveUser(requireContext())
                        onSuccess()
                    }
                    .addOnFailureListener {
                        if (!isAdded || _binding == null) return@addOnFailureListener
                        onFailure()
                    }
            }
            .addOnFailureListener {
                if (!isAdded || _binding == null) return@addOnFailureListener
                onFailure()
            }
    }

    private fun generatePlayerId(): String {
        return "AEL-${Random.nextInt(10000, 99999)}"
    }

    private fun isPasswordValid(password: String): Boolean {
        return password.isNotEmpty()
    }

    private fun signOutGoogleAndFirebase() {
        auth.signOut()
        if (::googleSignInClient.isInitialized) {
            googleSignInClient.signOut()
        }
    }

    private fun navigateToHome() {
        if (!isAdded || _binding == null || isNavigating) return

        val navController = findNavController()
        if (navController.currentDestination?.id == R.id.loginFragment) {
            isNavigating = true
            navController.navigate(R.id.action_loginFragment_to_homeFragment)
        }
    }

    private fun setEmailLoading(isLoading: Boolean) {
        isEmailLoading = isLoading
        if (!isAdded || _binding == null) return

        binding.loginButton.isEnabled = !isLoading && !isGoogleLoading
        binding.googleSignInButton.isEnabled = !isLoading && !isGoogleLoading
        binding.forgotPasswordTextView.isEnabled = !isLoading && !isGoogleLoading
    }

    private fun setGoogleLoading(isLoading: Boolean) {
        isGoogleLoading = isLoading
        if (!isAdded || _binding == null) return

        binding.googleSignInButton.isEnabled = !isLoading && !isEmailLoading
        binding.loginButton.isEnabled = !isLoading && !isEmailLoading
        binding.forgotPasswordTextView.isEnabled = !isLoading && !isEmailLoading
    }

    private fun setupKeyboardDismissOnBack() {
        requireActivity().onBackPressedDispatcher.addCallback(
            viewLifecycleOwner,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    hideKeyboard()
                    isEnabled = false
                    requireActivity().onBackPressedDispatcher.onBackPressed()
                }
            }
        )
    }

    private fun setupTapOutsideToHideKeyboard() {
        binding.root.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_DOWN) {
                binding.emailEditText.clearFocus()
                binding.passwordEditText.clearFocus()
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

    private fun showToast(message: String) {
        if (isAdded) {
            Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroyView() {
        hideKeyboard()
        isNavigating = false
        _binding = null
        super.onDestroyView()
    }
}