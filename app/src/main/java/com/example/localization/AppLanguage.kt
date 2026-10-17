package com.example.localization

import android.content.Context
import android.content.SharedPreferences

enum class AppLanguage(val code: String, val displayName: String, val nativeName: String) {
    ENGLISH("en", "English", "English"),
    HINDI("hi", "Hindi", "हिन्दी"),
    PUNJABI("pa", "Punjabi", "ਪੰਜਾਬੀ"),
    GUJARATI("gu", "Gujarati", "ગુજરાતી"),
    MARATHI("mr", "Marathi", "मराठी"),
    BENGALI("bn", "Bengali", "বাংলা")
}

class LocalizationManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("app_localization_prefs", Context.MODE_PRIVATE)

    fun getSavedLanguage(): AppLanguage {
        val code = prefs.getString("selected_language_code", AppLanguage.ENGLISH.code) ?: AppLanguage.ENGLISH.code
        return AppLanguage.entries.find { it.code == code } ?: AppLanguage.ENGLISH
    }

    fun saveLanguage(language: AppLanguage) {
        prefs.edit().putString("selected_language_code", language.code).apply()
    }
}
