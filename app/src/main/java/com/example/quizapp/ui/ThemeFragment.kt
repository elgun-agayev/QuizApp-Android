package com.example.quizapp.ui

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.view.View
import android.widget.RadioButton
import androidx.activity.addCallback
import androidx.appcompat.app.AppCompatDelegate
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.quizapp.R
import com.google.android.material.card.MaterialCardView

class ThemeFragment : Fragment(R.layout.fragment_theme) {

    private lateinit var systemDefaultRadioButton: RadioButton
    private lateinit var lightThemeRadioButton: RadioButton
    private lateinit var darkThemeRadioButton: RadioButton

    private lateinit var systemDefaultThemeCardView: MaterialCardView
    private lateinit var lightThemeCardView: MaterialCardView
    private lateinit var darkThemeCardView: MaterialCardView

    private lateinit var sharedPreferences: SharedPreferences

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        sharedPreferences =
            requireContext().getSharedPreferences("theme_prefs", Context.MODE_PRIVATE)

        initViews(view)
        setupBackNavigation()
        loadSavedTheme()
        setupClickListeners()
    }

    private fun initViews(view: View) {
        systemDefaultRadioButton = view.findViewById(R.id.systemDefaultRadioButton)
        lightThemeRadioButton = view.findViewById(R.id.lightThemeRadioButton)
        darkThemeRadioButton = view.findViewById(R.id.darkThemeRadioButton)

        systemDefaultThemeCardView = view.findViewById(R.id.systemDefaultThemeCardView)
        lightThemeCardView = view.findViewById(R.id.lightThemeCardView)
        darkThemeCardView = view.findViewById(R.id.darkThemeCardView)
    }

    private fun setupBackNavigation() {
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner) {
            navigateToProfile()
        }
    }

    private fun navigateToProfile() {
        findNavController().navigate(R.id.action_themeFragment_to_profileFragment)
    }

    private fun setupClickListeners() {
        systemDefaultThemeCardView.setOnClickListener {
            selectTheme(ThemeMode.SYSTEM)
        }

        lightThemeCardView.setOnClickListener {
            selectTheme(ThemeMode.LIGHT)
        }

        darkThemeCardView.setOnClickListener {
            selectTheme(ThemeMode.DARK)
        }

        systemDefaultRadioButton.setOnClickListener {
            selectTheme(ThemeMode.SYSTEM)
        }

        lightThemeRadioButton.setOnClickListener {
            selectTheme(ThemeMode.LIGHT)
        }

        darkThemeRadioButton.setOnClickListener {
            selectTheme(ThemeMode.DARK)
        }
    }

    private fun selectTheme(themeMode: ThemeMode) {
        val savedTheme = sharedPreferences.getString("app_theme", "system") ?: "system"
        val newTheme = when (themeMode) {
            ThemeMode.SYSTEM -> "system"
            ThemeMode.LIGHT -> "light"
            ThemeMode.DARK -> "dark"
        }

        selectOnlyRadio(themeMode)

        if (savedTheme == newTheme) return

        saveTheme(newTheme)

        when (themeMode) {
            ThemeMode.SYSTEM -> {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
            }

            ThemeMode.LIGHT -> {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            }

            ThemeMode.DARK -> {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            }
        }

        requireActivity().recreate()
    }

    private fun loadSavedTheme() {
        when (sharedPreferences.getString("app_theme", "system")) {
            "light" -> selectOnlyRadio(ThemeMode.LIGHT)
            "dark" -> selectOnlyRadio(ThemeMode.DARK)
            else -> selectOnlyRadio(ThemeMode.SYSTEM)
        }
    }

    private fun selectOnlyRadio(themeMode: ThemeMode) {
        systemDefaultRadioButton.isChecked = themeMode == ThemeMode.SYSTEM
        lightThemeRadioButton.isChecked = themeMode == ThemeMode.LIGHT
        darkThemeRadioButton.isChecked = themeMode == ThemeMode.DARK
    }

    private fun saveTheme(theme: String) {
        sharedPreferences.edit().putString("app_theme", theme).apply()
    }

    enum class ThemeMode {
        SYSTEM, LIGHT, DARK
    }
}