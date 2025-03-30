package com.example.tp1application2

import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import android.os.Build
import java.util.Locale

object LocaleHelper {

    fun setLocale(context: Context, language: String) {
        val locale = Locale(language)
        Locale.setDefault(locale)

        val resources: Resources = context.resources
        val config = Configuration(resources.configuration)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            config.setLocale(locale)
        } else {
            config.locale = locale
        }

        context.resources.updateConfiguration(config, resources.displayMetrics)

        // Sauvegarde de la langue sélectionnée
        saveLanguagePreference(context, language)
    }

    fun getSavedLanguage(context: Context): String {
        val prefs = context.getSharedPreferences("Settings", Context.MODE_PRIVATE)
        return prefs.getString("My_Lang", "fr") ?: "fr" // Langue par défaut : Français
    }

    private fun saveLanguagePreference(context: Context, lang: String) {
        val prefs = context.getSharedPreferences("Settings", Context.MODE_PRIVATE)
        val editor = prefs.edit()
        editor.putString("My_Lang", lang)
        editor.apply()
    }
}