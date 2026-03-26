package com.example.quizapp

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.navigation.NavController
import androidx.navigation.NavGraph
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.navOptions
import com.example.quizapp.databinding.ActivityMainBinding
import com.example.quizapp.manager.UserManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import android.graphics.Color

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var navController: NavController
    private lateinit var auth: FirebaseAuth

    // Test üçün bunu true saxla
    private val SHOW_TEST_USER_PICKER = false

    // Bottom navigation seçimi programmatically dəyişəndə loop düşməsin
    private var isUpdatingBottomNav = false

    override fun onCreate(savedInstanceState: Bundle?) {
        applySavedTheme()

        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        window.statusBarColor = Color.TRANSPARENT
        ViewCompat.getWindowInsetsController(window.decorView)?.isAppearanceLightStatusBars = true

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // user məlumatını yüklə
        UserManager.loadUser(this)

        auth = FirebaseAuth.getInstance()

        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())

            v.setPadding(
                systemBars.left,
                0,
                systemBars.right,
                0
            )
            insets
        }

        setupNavigation()
        setupBackPressBehavior()

        if (SHOW_TEST_USER_PICKER && savedInstanceState == null) {
            showTestUserPicker()
        }
    }

    private fun setupNavigation() {
        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.nav_host) as? NavHostFragment ?: return

        navController = navHostFragment.navController

        val navInflater = navController.navInflater
        val navGraph: NavGraph = navInflater.inflate(R.navigation.nav_graph)

        val currentUser = auth.currentUser

        val shouldOpenHome = when {
            currentUser == null -> false
            isGoogleUser(currentUser) -> true
            isPasswordUser(currentUser) && currentUser.isEmailVerified -> true
            isPasswordUser(currentUser) && !currentUser.isEmailVerified -> {
                auth.signOut()
                false
            }
            else -> {
                auth.signOut()
                false
            }
        }

        navGraph.setStartDestination(
            if (shouldOpenHome) R.id.homeFragment else R.id.loginFragment
        )

        // Lokal düzəliş:
        // setGraph(navGraph, null) bəzi hallarda əlavə trigger yarada bilir.
        // graph property ilə vermək daha stabildir.
        navController.graph = navGraph

        setupBottomNavigation()
    }

    /**
     * Lokal əlavə:
     * Back button və sağ/sol swipe back eyni callback-ə düşür.
     * Əgər HomeFragment açıqdırsa app-dən çıxır,
     * yoxsa normal geri qayıdır.
     */
    private fun setupBackPressBehavior() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                when (navController.currentDestination?.id) {
                    R.id.homeFragment -> {
                        finish()
                    }

                    else -> {
                        val navigatedUp = navController.navigateUp()
                        if (!navigatedUp) {
                            finish()
                        }
                    }
                }
            }
        })
    }

    private fun isGoogleUser(user: FirebaseUser): Boolean {
        return user.providerData.any { it.providerId == "google.com" }
    }

    private fun isPasswordUser(user: FirebaseUser): Boolean {
        return user.providerData.any { it.providerId == "password" }
    }

    // USER SEÇİM DİALOQU
    private fun showTestUserPicker() {
        val options = arrayOf(
            "Alex ilə aç",
            "Joseph ilə aç",
            "Hazırkı user ilə davam et"
        )

        AlertDialog.Builder(this)
            .setTitle("Test User Seç")
            .setItems(options) { dialog, which ->
                when (which) {

                    // Alex
                    0 -> {
                        UserManager.clearUser(this)
                        UserManager.switchToAlex(this)
                        restartAppFresh()
                    }

                    // Joseph
                    1 -> {
                        UserManager.clearUser(this)
                        UserManager.switchToJoseph(this)
                        restartAppFresh()
                    }

                    // hazırkı user
                    2 -> {
                        UserManager.loadUser(this)
                    }
                }
                dialog.dismiss()
            }
            .setCancelable(false)
            .show()
    }

    // APP-I TAM RESTART EDİR
    private fun restartAppFresh() {
        val restartIntent = intent
        restartIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        startActivity(restartIntent)
        finish()
    }

    private fun setupBottomNavigation() {
        binding.bottomNav.setOnItemSelectedListener { item ->
            if (isUpdatingBottomNav) return@setOnItemSelectedListener true

            when (item.itemId) {

                R.id.home -> {
                    navController.navigate(
                        R.id.homeFragment,
                        null,
                        navOptions {
                            launchSingleTop = true
                            restoreState = true
                            popUpTo(navController.graph.startDestinationId) {
                                saveState = true
                            }
                        }
                    )
                    true
                }

                R.id.leader -> {
                    navController.navigate(
                        R.id.leaderFragment,
                        null,
                        navOptions {
                            launchSingleTop = true
                            restoreState = true
                            popUpTo(navController.graph.startDestinationId) {
                                saveState = true
                            }
                        }
                    )
                    true
                }

                R.id.favorites -> {
                    navController.navigate(
                        R.id.favoritesFragment,
                        null,
                        navOptions {
                            launchSingleTop = true
                            restoreState = true
                            popUpTo(navController.graph.startDestinationId) {
                                saveState = true
                            }
                        }
                    )
                    true
                }

                R.id.profile -> {
                    navController.navigate(
                        R.id.profileFragment,
                        null,
                        navOptions {
                            launchSingleTop = true
                            restoreState = true
                            popUpTo(navController.graph.startDestinationId) {
                                saveState = true
                            }
                        }
                    )
                    true
                }

                else -> false
            }
        }

        navController.addOnDestinationChangedListener { _, destination, _ ->
            when (destination.id) {

                R.id.homeFragment -> {
                    binding.bottomNav.visibility = View.VISIBLE
                    if (binding.bottomNav.selectedItemId != R.id.home) {
                        isUpdatingBottomNav = true
                        binding.bottomNav.selectedItemId = R.id.home
                        isUpdatingBottomNav = false
                    }
                }

                R.id.leaderFragment -> {
                    binding.bottomNav.visibility = View.VISIBLE
                    if (binding.bottomNav.selectedItemId != R.id.leader) {
                        isUpdatingBottomNav = true
                        binding.bottomNav.selectedItemId = R.id.leader
                        isUpdatingBottomNav = false
                    }
                }

                R.id.favoritesFragment -> {
                    binding.bottomNav.visibility = View.VISIBLE
                    if (binding.bottomNav.selectedItemId != R.id.favorites) {
                        isUpdatingBottomNav = true
                        binding.bottomNav.selectedItemId = R.id.favorites
                        isUpdatingBottomNav = false
                    }
                }

                R.id.profileFragment -> {
                    binding.bottomNav.visibility = View.VISIBLE
                    if (binding.bottomNav.selectedItemId != R.id.profile) {
                        isUpdatingBottomNav = true
                        binding.bottomNav.selectedItemId = R.id.profile
                        isUpdatingBottomNav = false
                    }
                }

                else -> {
                    binding.bottomNav.visibility = View.GONE
                }
            }
        }
    }

    private fun applySavedTheme() {
        val sharedPreferences = getSharedPreferences("theme_prefs", Context.MODE_PRIVATE)

        when (sharedPreferences.getString("app_theme", "system")) {
            "light" -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            "dark" -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            else -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        }
    }
}