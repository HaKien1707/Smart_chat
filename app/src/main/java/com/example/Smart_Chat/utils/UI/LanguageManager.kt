package com.example.Smart_Chat.utils.UI

import android.content.Context
import android.content.SharedPreferences
import android.content.res.Configuration
import java.util.Locale

object LanguageManager {
    private const val PREFS_NAME = "app_preferences"
    private const val KEY_LANGUAGE = "language"

    private fun getPreferences(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun setLanguage(context: Context, languageCode: String) {
        getPreferences(context).edit().putString(KEY_LANGUAGE, languageCode).apply()
        updateResources(context, languageCode)
    }

    fun getLanguage(context: Context): String {
        return getPreferences(context).getString(KEY_LANGUAGE, "en") ?: "en"
    }

    fun updateResources(context: Context, languageCode: String) {
        val locale = Locale(languageCode)
        Locale.setDefault(locale)

        val config = Configuration(context.resources.configuration)
        config.setLocale(locale)

        context.resources.updateConfiguration(config, context.resources.displayMetrics)
    }

    fun applySavedLanguage(context: Context) {
        val savedLanguage = getLanguage(context)
        updateResources(context, savedLanguage)
    }
}