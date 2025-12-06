package com.example.Smart_Chat.fragment

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import com.example.Smart_Chat.R
import com.example.Smart_Chat.activities.MainActivity
import com.example.Smart_Chat.utils.LanguageManager
import com.example.Smart_Chat.utils.ThemeManager

class SettingsFragment : Fragment() {

    private lateinit var themeOption: View
    private lateinit var languageOption: View
    private lateinit var themeValue: TextView
    private lateinit var languageValue: TextView

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_settings, container, false)

        themeOption = view.findViewById(R.id.theme_option)
        languageOption = view.findViewById(R.id.language_option)
        themeValue = view.findViewById(R.id.theme_value)
        languageValue = view.findViewById(R.id.language_value)

        // Display current settings
        updateThemeValue()
        updateLanguageValue()

        // Click listeners
        themeOption.setOnClickListener {
            showThemeDialog()
        }

        languageOption.setOnClickListener {
            showLanguageDialog()
        }

        return view
    }

    private fun updateThemeValue() {
        val currentTheme = ThemeManager.getThemeMode(requireContext())
        themeValue.text = when (currentTheme) {
            ThemeManager.THEME_LIGHT -> getString(R.string.theme_light)
            ThemeManager.THEME_DARK -> getString(R.string.theme_dark)
            ThemeManager.THEME_SYSTEM -> getString(R.string.theme_system)
            else -> getString(R.string.theme_system)
        }
    }

    private fun updateLanguageValue() {
        val currentLang = LanguageManager.getLanguage(requireContext())
        languageValue.text = when (currentLang) {
            "en" -> "English"
            "vi" -> "Tiếng Việt"
            else -> "English"
        }
    }

    private fun showThemeDialog() {
        val themes = arrayOf(
            getString(R.string.theme_light),
            getString(R.string.theme_dark),
            getString(R.string.theme_system)
        )

        val currentTheme = ThemeManager.getThemeMode(requireContext())
        val checkedItem = when (currentTheme) {
            ThemeManager.THEME_LIGHT -> 0
            ThemeManager.THEME_DARK -> 1
            ThemeManager.THEME_SYSTEM -> 2
            else -> 2
        }

        AlertDialog.Builder(requireContext())
            .setTitle(getString(R.string.choose_theme))
            .setSingleChoiceItems(themes, checkedItem) { dialog, which ->
                val selectedTheme = when (which) {
                    0 -> ThemeManager.THEME_LIGHT
                    1 -> ThemeManager.THEME_DARK
                    2 -> ThemeManager.THEME_SYSTEM
                    else -> ThemeManager.THEME_SYSTEM
                }

                ThemeManager.saveThemeMode(requireContext(), selectedTheme)
                updateThemeValue()
                dialog.dismiss() // ✅ Closes immediately after selection
            }
            .setNegativeButton(getString(R.string.cancel), null) // Optional cancel button
            .show()
    }

    private fun showLanguageDialog() {
        val languages = arrayOf("English", "Tiếng Việt")
        val languageCodes = arrayOf("en", "vi")

        val currentLang = LanguageManager.getLanguage(requireContext())
        val checkedItem = languageCodes.indexOf(currentLang)

        AlertDialog.Builder(requireContext())
            .setTitle(getString(R.string.choose_language))
            .setSingleChoiceItems(languages, checkedItem) { dialog, which ->
                val selectedLang = languageCodes[which]

                LanguageManager.setLanguage(requireContext(), selectedLang)

                // Restart app to apply language
                val intent = Intent(requireContext(), MainActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK
                startActivity(intent)
                requireActivity().finish()

                dialog.dismiss()
            }
            .show()
    }
}