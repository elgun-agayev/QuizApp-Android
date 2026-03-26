package com.example.quizapp.ui

import android.content.Context
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Patterns
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.activity.addCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.quizapp.R
import com.example.quizapp.databinding.FragmentRegisterBinding
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
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import java.util.Locale
import kotlin.random.Random

class RegisterFragment : Fragment(R.layout.fragment_register) {

    private var _binding: FragmentRegisterBinding? = null
    private val binding get() = _binding!!

    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    private lateinit var googleSignInClient: GoogleSignInClient

    private var isNavigating = false
    private var isGoogleLoading = false
    private var isEmailRegisterLoading = false

    private val googleSignUpLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            val data = result.data
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)

            try {
                val account = task.getResult(ApiException::class.java)
                if (account == null) {
                    setGoogleLoading(false)
                    showToast("Google sign up failed")
                    return@registerForActivityResult
                }
                handleGoogleSignUp(account)
            } catch (_: ApiException) {
                setGoogleLoading(false)
                showToast("Google sign up failed")
            }
        }

    companion object {
        private const val USERS_COLLECTION = "users"
        private const val GOOGLE_PROVIDER = "google"
        private const val PASSWORD_PROVIDER = "password"
        private const val PLAYER_ID_PREFIX = "AEL-"
        private const val MAX_PLAYER_ID_ATTEMPTS = 20
        private const val MIN_NAME_LENGTH = 3
        private const val MAX_NAME_LENGTH = 20

        private val NAME_REGEX = Regex("^[A-Za-z0-9._ ]{3,20}$")
        private val PASSWORD_REGEX =
            Regex("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^A-Za-z\\d]).{8,}$")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentRegisterBinding.bind(view)

        setupGoogleSignIn()
        setupInputValidationUI()
        setupClickListeners()

        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner) {
            if (binding.nameEditText.hasFocus() ||
                binding.emailEditText.hasFocus() ||
                binding.passwordEditText.hasFocus()
            ) {
                binding.nameEditText.clearFocus()
                binding.emailEditText.clearFocus()
                binding.passwordEditText.clearFocus()
                hideKeyboard()
            } else {
                findNavController().navigateUp()
            }
        }

        binding.root.setOnClickListener {
            binding.nameEditText.clearFocus()
            binding.emailEditText.clearFocus()
            binding.passwordEditText.clearFocus()
            hideKeyboard()
        }
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

    private fun setupClickListeners() = with(binding) {
        loginTextView.setOnClickListener {
            nameEditText.clearFocus()
            emailEditText.clearFocus()
            passwordEditText.clearFocus()
            hideKeyboard()
            navigateToLogin()
        }

        registerButton.setOnClickListener {
            nameEditText.clearFocus()
            emailEditText.clearFocus()
            passwordEditText.clearFocus()
            hideKeyboard()
            registerUser()
        }

        googleSignInButton.setOnClickListener {
            nameEditText.clearFocus()
            emailEditText.clearFocus()
            passwordEditText.clearFocus()
            hideKeyboard()
            signUpWithGoogle()
        }
    }

    private fun setEmailEndIconVisible(visible: Boolean) {
        if (!isAdded || _binding == null) return

        val targetMode = if (visible) {
            TextInputLayout.END_ICON_CUSTOM
        } else {
            TextInputLayout.END_ICON_NONE
        }

        if (binding.emailInputLayout.endIconMode != targetMode) {
            binding.emailInputLayout.endIconMode = targetMode
            if (visible) {
                binding.emailInputLayout.setEndIconDrawable(R.drawable.ic_email)
            }
        }
    }

    private fun setPasswordEndIconVisible(visible: Boolean) {
        if (!isAdded || _binding == null) return

        val targetMode = if (visible) {
            TextInputLayout.END_ICON_PASSWORD_TOGGLE
        } else {
            TextInputLayout.END_ICON_NONE
        }

        if (binding.passwordInputLayout.endIconMode != targetMode) {
            binding.passwordInputLayout.endIconMode = targetMode
        }
    }

    private fun updateEmailVisualState(rawEmail: String, showError: Boolean) {
        if (!isAdded || _binding == null) return

        val email = rawEmail.trim()
        val isValid = email.isNotEmpty() && Patterns.EMAIL_ADDRESS.matcher(email).matches()

        setEmailEndIconVisible(isValid)

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
        val isValid = PASSWORD_REGEX.matches(password)

        setPasswordEndIconVisible(isValid)

        binding.passwordInputLayout.error = when {
            !showError -> null
            password.isEmpty() -> null
            !isValid -> "Password must be at least 8 characters and include uppercase, lowercase, number and special character."
            else -> null
        }
    }

    private fun registerUser() {
        val rawName = binding.nameEditText.text?.toString().orEmpty()
        val rawEmail = binding.emailEditText.text?.toString().orEmpty().trim()
        val rawPassword = binding.passwordEditText.text?.toString().orEmpty()

        val normalizedName = normalizeName(rawName)
        val email = rawEmail.lowercase(Locale.getDefault())
        val password = rawPassword.trim()

        clearInputErrors()
        updateEmailVisualState(email, showError = true)
        updatePasswordVisualState(password, showError = true)

        val nameValidation = validateName(normalizedName)
        if (nameValidation != null) {
            binding.nameInputLayout.error = nameValidation
            binding.nameEditText.requestFocus()
            showToast(nameValidation)
            return
        }

        when {
            email.isEmpty() -> {
                binding.emailInputLayout.error = "Enter your Gmail"
                binding.emailEditText.requestFocus()
                showToast("Enter your Gmail")
                return
            }

            !Patterns.EMAIL_ADDRESS.matcher(email).matches() -> {
                binding.emailInputLayout.error = "Invalid Gmail format"
                binding.emailEditText.requestFocus()
                showToast("Invalid Gmail format")
                return
            }
        }

        val passwordValidation = validatePassword(password)
        if (passwordValidation != null) {
            binding.passwordInputLayout.error = passwordValidation
            binding.passwordEditText.requestFocus()
            showToast(passwordValidation)
            return
        }

        setEmailRegisterLoading(true)

        checkIfEmailOrNameExists(
            email = email,
            name = normalizedName,
            onEmailExists = {
                if (!isAdded || _binding == null) return@checkIfEmailOrNameExists
                setEmailRegisterLoading(false)
                binding.emailInputLayout.error = "This email is already in use"
                binding.emailEditText.requestFocus()
                showToast("This email is already in use")
            },
            onNameExists = {
                if (!isAdded || _binding == null) return@checkIfEmailOrNameExists
                setEmailRegisterLoading(false)
                val suggestions = generateNameSuggestions(normalizedName)
                binding.nameInputLayout.error = "This name is already taken"
                binding.nameEditText.requestFocus()
                showToast("This name is already taken. Try: ${suggestions.joinToString(", ")}")
            },
            onAvailable = {
                createFirebaseUser(
                    name = normalizedName,
                    email = email,
                    password = password
                )
            }
        )
    }

    private fun checkIfEmailOrNameExists(
        email: String,
        name: String,
        onEmailExists: () -> Unit,
        onNameExists: () -> Unit,
        onAvailable: () -> Unit
    ) {
        firestore.collection(USERS_COLLECTION)
            .whereEqualTo("email", email)
            .limit(1)
            .get()
            .addOnSuccessListener { emailDocuments ->
                if (!isAdded || _binding == null) return@addOnSuccessListener

                if (!emailDocuments.isEmpty) {
                    onEmailExists()
                } else {
                    firestore.collection(USERS_COLLECTION)
                        .whereEqualTo("nameLowercase", name.lowercase(Locale.getDefault()))
                        .limit(1)
                        .get()
                        .addOnSuccessListener { nameDocuments ->
                            if (!isAdded || _binding == null) return@addOnSuccessListener

                            if (!nameDocuments.isEmpty) {
                                onNameExists()
                            } else {
                                onAvailable()
                            }
                        }
                        .addOnFailureListener {
                            if (!isAdded || _binding == null) return@addOnFailureListener
                            setEmailRegisterLoading(false)
                            showToast("Failed to verify name")
                        }
                }
            }
            .addOnFailureListener {
                if (!isAdded || _binding == null) return@addOnFailureListener
                setEmailRegisterLoading(false)
                showToast("Failed to verify email")
            }
    }

    private fun createFirebaseUser(name: String, email: String, password: String) {
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (!isAdded || _binding == null) return@addOnCompleteListener

                if (task.isSuccessful) {
                    val currentUser = auth.currentUser

                    if (currentUser == null) {
                        setEmailRegisterLoading(false)
                        showToast("User data not found")
                        return@addOnCompleteListener
                    }

                    val profileUpdates = UserProfileChangeRequest.Builder()
                        .setDisplayName(name)
                        .build()

                    currentUser.updateProfile(profileUpdates)
                        .addOnCompleteListener { profileTask ->
                            if (!isAdded || _binding == null) return@addOnCompleteListener

                            if (profileTask.isSuccessful) {
                                createPasswordUserDocument(
                                    uid = currentUser.uid,
                                    name = name,
                                    email = email
                                )
                            } else {
                                currentUser.delete()
                                setEmailRegisterLoading(false)
                                showToast("Profile could not be updated")
                            }
                        }
                } else {
                    setEmailRegisterLoading(false)

                    val message = when ((task.exception as? FirebaseAuthException)?.errorCode) {
                        "ERROR_EMAIL_ALREADY_IN_USE" -> "This email is already in use"
                        "ERROR_INVALID_EMAIL" -> "Invalid email format"
                        "ERROR_WEAK_PASSWORD" -> "Password is too weak"
                        "ERROR_NETWORK_REQUEST_FAILED" -> "Network error occurred"
                        else -> task.exception?.localizedMessage ?: "Registration failed"
                    }

                    showToast(message)
                }
            }
    }

    private fun createPasswordUserDocument(uid: String, name: String, email: String) {
        generateUniquePlayerId(
            onReady = { playerId ->
                val firstName = name.split("\\s+".toRegex()).firstOrNull().orEmpty().ifBlank { "Player" }

                val userMap = hashMapOf(
                    "uid" to uid,
                    "playerId" to playerId,
                    "name" to name,
                    "firstName" to firstName,
                    "nameLowercase" to name.lowercase(Locale.getDefault()),
                    "email" to email,
                    "avatarUrl" to "",
                    "coins" to 0,
                    "wins" to 0,
                    "losses" to 0,
                    "score" to 0,
                    "status" to "online",
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
                        sendVerificationEmailOnly()
                    }
                    .addOnFailureListener {
                        if (!isAdded || _binding == null) return@addOnFailureListener
                        auth.currentUser?.delete()
                        setEmailRegisterLoading(false)
                        showToast("User data could not be saved")
                    }
            },
            onFailure = {
                if (!isAdded || _binding == null) return@generateUniquePlayerId
                auth.currentUser?.delete()
                setEmailRegisterLoading(false)
                showToast("Failed to generate player ID")
            }
        )
    }

    private fun sendVerificationEmailOnly() {
        val currentUser = auth.currentUser

        if (currentUser == null) {
            setEmailRegisterLoading(false)
            showToast("User not found")
            return
        }

        currentUser.sendEmailVerification()
            .addOnCompleteListener { verifyTask ->
                if (!isAdded || _binding == null) return@addOnCompleteListener

                setEmailRegisterLoading(false)

                if (verifyTask.isSuccessful) {
                    showToast("Verification email sent. Please verify your email before logging in.")
                    auth.signOut()
                    navigateToLogin()
                } else {
                    val errorMessage = verifyTask.exception?.localizedMessage ?: "Unknown error"
                    auth.signOut()
                    showToast("Failed to send verification email: $errorMessage")
                    navigateToLogin()
                }
            }
    }

    private fun signUpWithGoogle() {
        if (isGoogleLoading) return

        setGoogleLoading(true)

        val signInIntent = googleSignInClient.signInIntent
        googleSignUpLauncher.launch(signInIntent)
    }

    private fun handleGoogleSignUp(account: GoogleSignInAccount) {
        val email = account.email?.trim()?.lowercase(Locale.getDefault())
        val idToken = account.idToken
        val rawGoogleName = account.displayName ?: "Player"

        if (email.isNullOrBlank()) {
            setGoogleLoading(false)
            googleSignInClient.signOut()
            showToast("Google email not found")
            return
        }

        if (idToken.isNullOrBlank()) {
            setGoogleLoading(false)
            googleSignInClient.signOut()
            showToast("Google token is null")
            return
        }

        firestore.collection(USERS_COLLECTION)
            .whereEqualTo("email", email)
            .limit(1)
            .get()
            .addOnSuccessListener { documents ->
                if (!isAdded || _binding == null) return@addOnSuccessListener

                if (!documents.isEmpty) {
                    setGoogleLoading(false)
                    signOutGoogleAndFirebase()
                    showToast("This Google account is already registered. Please sign in.")
                    navigateToLogin()
                } else {
                    createGoogleUser(
                        account = account,
                        idToken = idToken,
                        rawGoogleName = rawGoogleName,
                        email = email
                    )
                }
            }
            .addOnFailureListener {
                if (!isAdded || _binding == null) return@addOnFailureListener
                setGoogleLoading(false)
                auth.signOut()
                googleSignInClient.signOut()
                showToast("Failed to verify Google account")
            }
    }

    private fun createGoogleUser(
        account: GoogleSignInAccount,
        idToken: String,
        rawGoogleName: String,
        email: String
    ) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)

        auth.signInWithCredential(credential)
            .addOnCompleteListener(requireActivity()) { task ->
                if (!isAdded || _binding == null) return@addOnCompleteListener

                if (task.isSuccessful) {
                    val firebaseUser = auth.currentUser
                    if (firebaseUser == null) {
                        setGoogleLoading(false)
                        signOutGoogleAndFirebase()
                        showToast("Google registration failed")
                        return@addOnCompleteListener
                    }

                    val normalizedName = normalizeName(rawGoogleName)
                    val safeBaseName = when {
                        normalizedName.isBlank() -> email.substringBefore("@")
                        validateName(normalizedName) == null -> normalizedName
                        else -> normalizeName(email.substringBefore("@"))
                    }.ifBlank { "Player" }

                    generateUniqueName(
                        baseName = safeBaseName,
                        onReady = { uniqueName ->
                            val profileUpdates = UserProfileChangeRequest.Builder()
                                .setDisplayName(uniqueName)
                                .build()

                            firebaseUser.updateProfile(profileUpdates)
                                .addOnCompleteListener { profileTask ->
                                    if (!isAdded || _binding == null) return@addOnCompleteListener

                                    if (profileTask.isSuccessful) {
                                        saveGoogleUserToFirestore(
                                            uid = firebaseUser.uid,
                                            name = uniqueName,
                                            email = email,
                                            avatarUrl = account.photoUrl?.toString().orEmpty()
                                        )
                                    } else {
                                        firebaseUser.delete().addOnCompleteListener {
                                            setGoogleLoading(false)
                                            signOutGoogleAndFirebase()
                                            showToast("Profile could not be updated")
                                        }
                                    }
                                }
                        },
                        onFailure = {
                            firebaseUser.delete().addOnCompleteListener {
                                setGoogleLoading(false)
                                signOutGoogleAndFirebase()
                                showToast("Failed to generate unique name")
                            }
                        }
                    )
                } else {
                    setGoogleLoading(false)
                    signOutGoogleAndFirebase()
                    showToast(task.exception?.localizedMessage ?: "Google authentication failed")
                }
            }
    }

    private fun saveGoogleUserToFirestore(
        uid: String,
        name: String,
        email: String,
        avatarUrl: String
    ) {
        generateUniquePlayerId(
            onReady = { playerId ->
                val firstName = name.split("\\s+".toRegex()).firstOrNull().orEmpty().ifBlank { "Player" }

                val userMap = hashMapOf(
                    "uid" to uid,
                    "playerId" to playerId,
                    "name" to name,
                    "firstName" to firstName,
                    "nameLowercase" to name.lowercase(Locale.getDefault()),
                    "email" to email,
                    "avatarUrl" to avatarUrl,
                    "coins" to 0,
                    "wins" to 0,
                    "losses" to 0,
                    "score" to 0,
                    "status" to "online",
                    "totalGames" to 0,
                    "correctAnswers" to 0,
                    "provider" to GOOGLE_PROVIDER,
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
                        setGoogleLoading(false)
                        showToast("Registration successful")
                        navigateToHome()
                    }
                    .addOnFailureListener {
                        if (!isAdded || _binding == null) return@addOnFailureListener
                        auth.currentUser?.delete()?.addOnCompleteListener {
                            setGoogleLoading(false)
                            signOutGoogleAndFirebase()
                            showToast("User data could not be saved")
                        }
                    }
            },
            onFailure = {
                if (!isAdded || _binding == null) return@generateUniquePlayerId
                auth.currentUser?.delete()?.addOnCompleteListener {
                    setGoogleLoading(false)
                    signOutGoogleAndFirebase()
                    showToast("Failed to generate player ID")
                }
            }
        )
    }

    private fun generateUniquePlayerId(
        onReady: (String) -> Unit,
        onFailure: () -> Unit
    ) {
        tryGeneratePlayerId(0, onReady, onFailure)
    }

    private fun tryGeneratePlayerId(
        attempt: Int,
        onReady: (String) -> Unit,
        onFailure: () -> Unit
    ) {
        if (attempt >= MAX_PLAYER_ID_ATTEMPTS) {
            onFailure()
            return
        }

        val candidate = "$PLAYER_ID_PREFIX${Random.nextInt(10000, 99999)}"

        firestore.collection(USERS_COLLECTION)
            .whereEqualTo("playerId", candidate)
            .limit(1)
            .get()
            .addOnSuccessListener { documents ->
                if (!isAdded || _binding == null) return@addOnSuccessListener

                if (documents.isEmpty) {
                    onReady(candidate)
                } else {
                    tryGeneratePlayerId(attempt + 1, onReady, onFailure)
                }
            }
            .addOnFailureListener {
                if (!isAdded || _binding == null) return@addOnFailureListener
                onFailure()
            }
    }

    private fun generateUniqueName(
        baseName: String,
        onReady: (String) -> Unit,
        onFailure: () -> Unit
    ) {
        val cleanedBaseName = normalizeName(baseName).ifBlank { "Player" }
        val safeBaseName = cleanedBaseName.take(MAX_NAME_LENGTH).trim()

        if (safeBaseName.length < MIN_NAME_LENGTH) {
            tryUniqueNameWithSuffix("Player", 0, onReady, onFailure)
            return
        }

        checkNameAvailability(
            name = safeBaseName,
            onAvailable = {
                onReady(safeBaseName)
            },
            onTaken = {
                tryUniqueNameWithSuffix(safeBaseName, 0, onReady, onFailure)
            },
            onFailure = onFailure
        )
    }

    private fun tryUniqueNameWithSuffix(
        baseName: String,
        attempt: Int,
        onReady: (String) -> Unit,
        onFailure: () -> Unit
    ) {
        if (attempt >= 15) {
            onFailure()
            return
        }

        val suffix = Random.nextInt(10, 9999).toString()
        val allowedBaseLength = (MAX_NAME_LENGTH - suffix.length).coerceAtLeast(MIN_NAME_LENGTH)
        val shortBase = baseName.take(allowedBaseLength).trim().ifBlank { "Player" }
        val candidate = "$shortBase$suffix"

        checkNameAvailability(
            name = candidate,
            onAvailable = {
                onReady(candidate)
            },
            onTaken = {
                tryUniqueNameWithSuffix(baseName, attempt + 1, onReady, onFailure)
            },
            onFailure = onFailure
        )
    }

    private fun checkNameAvailability(
        name: String,
        onAvailable: () -> Unit,
        onTaken: () -> Unit,
        onFailure: () -> Unit
    ) {
        firestore.collection(USERS_COLLECTION)
            .whereEqualTo("nameLowercase", name.lowercase(Locale.getDefault()))
            .limit(1)
            .get()
            .addOnSuccessListener { documents ->
                if (!isAdded || _binding == null) return@addOnSuccessListener

                if (documents.isEmpty) {
                    onAvailable()
                } else {
                    onTaken()
                }
            }
            .addOnFailureListener {
                if (!isAdded || _binding == null) return@addOnFailureListener
                onFailure()
            }
    }

    private fun generateNameSuggestions(name: String): List<String> {
        val base = normalizeName(name)
            .replace("\\s+".toRegex(), "")
            .replace("[^A-Za-z0-9._]".toRegex(), "")
            .ifBlank { "Player" }
            .take(12)

        return listOf(
            "${base}${Random.nextInt(10, 99)}",
            "${base}_${Random.nextInt(10, 99)}",
            "${base}.${Random.nextInt(10, 99)}"
        ).distinct()
    }

    private fun normalizeName(name: String): String {
        return name
            .trim()
            .replace("\\s+".toRegex(), " ")
    }

    private fun validateName(name: String): String? {
        if (name.isBlank()) return "Enter your name"
        if (name.length < MIN_NAME_LENGTH) return "Name must be at least 3 characters"
        if (name.length > MAX_NAME_LENGTH) return "Name must be at most 20 characters"
        if (!NAME_REGEX.matches(name)) {
            return "Name can contain only letters, numbers, spaces, _ and ."
        }
        return null
    }

    private fun validatePassword(password: String): String? {
        if (password.isBlank()) return "Enter your password"
        if (!PASSWORD_REGEX.matches(password)) {
            return "Password must be at least 8 characters and include uppercase, lowercase, number and special character"
        }
        return null
    }

    private fun clearInputErrors() {
        binding.nameInputLayout.error = null
        binding.emailInputLayout.error = null
        binding.passwordInputLayout.error = null
    }

    private fun hideKeyboard() {
        val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
        val focusedView = requireActivity().currentFocus ?: binding.root
        imm?.hideSoftInputFromWindow(focusedView.windowToken, 0)
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
        if (navController.currentDestination?.id == R.id.registerFragment) {
            isNavigating = true
            navController.navigate(R.id.action_registerFragment_to_homeFragment)
        }
    }

    private fun navigateToLogin() {
        if (!isAdded || _binding == null || isNavigating) return

        val navController = findNavController()
        if (navController.currentDestination?.id == R.id.registerFragment) {
            isNavigating = true
            navController.navigate(R.id.action_registerFragment_to_loginFragment)
        }
    }

    private fun setGoogleLoading(isLoading: Boolean) {
        isGoogleLoading = isLoading
        if (!isAdded || _binding == null) return
        binding.googleSignInButton.isEnabled = !isLoading
        binding.registerButton.isEnabled = !isLoading && !isEmailRegisterLoading
    }

    private fun setEmailRegisterLoading(isLoading: Boolean) {
        isEmailRegisterLoading = isLoading
        if (!isAdded || _binding == null) return
        binding.registerButton.isEnabled = !isLoading
        binding.googleSignInButton.isEnabled = !isLoading && !isGoogleLoading
    }

    private fun showToast(message: String) {
        if (isAdded) {
            Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show()
        }
    }

    override fun onDestroyView() {
        isNavigating = false
        _binding = null
        super.onDestroyView()
    }

}