package com.example.ui.theme

import android.content.Context
import android.content.SharedPreferences

class ThemeManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("app_theme_prefs", Context.MODE_PRIVATE)

    fun isDarkMode(isSystemDark: Boolean): Boolean {
        // Default to system dark mode if not explicitly saved
        return prefs.getBoolean("is_dark_mode", isSystemDark)
    }

    fun setDarkMode(enabled: Boolean) {
        prefs.edit().putBoolean("is_dark_mode", enabled).apply()
    }
}
