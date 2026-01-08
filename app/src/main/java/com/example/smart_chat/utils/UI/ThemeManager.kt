package com.example.smart_chat.utils.UI

import android.content.Context
import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatDelegate

object ThemeManager {
    private const val PREFS_NAME = "app_preferences"
    private const val KEY_THEME_MODE = "theme_mode"

    const val THEME_LIGHT = "light"
    const val THEME_DARK = "dark"
    const val THEME_SYSTEM = "system"

    private fun getPreferences(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun saveThemeMode(context: Context, mode: String) {
        getPreferences(context).edit().putString(KEY_THEME_MODE, mode).apply()
        applyTheme(mode)
    }

    fun getThemeMode(context: Context): String {
        return getPreferences(context).getString(KEY_THEME_MODE, THEME_SYSTEM) ?: THEME_SYSTEM
    }

    fun applyTheme(mode: String) {
        when (mode) {
            THEME_LIGHT -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            THEME_DARK -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            THEME_SYSTEM -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        }
    }

    fun applySavedTheme(context: Context) {
        val savedMode = getThemeMode(context)
        applyTheme(savedMode)
    }
}