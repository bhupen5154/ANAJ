package com.example

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.example.ads.AdManager
import com.example.localization.AppLanguage
import com.example.localization.LocalizationManager
import com.example.security.SecureCredentialManager
import com.example.ui.HomeScreen
import com.example.ui.MerchantPortalScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.ThemeManager
import com.example.update.AppUpdateChecker
import com.example.webview.WebViewManager

enum class ScreenState {
    HOME,
    MERCHANT_PORTAL
}

class MainActivity : ComponentActivity() {

    private lateinit var credentialManager: SecureCredentialManager
    private lateinit var themeManager: ThemeManager
    private lateinit var localizationManager: LocalizationManager
    private lateinit var updateChecker: AppUpdateChecker

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Initialize managers
        credentialManager = SecureCredentialManager(this)
        themeManager = ThemeManager(this)
        localizationManager = LocalizationManager(this)
        updateChecker = AppUpdateChecker(this)

        // Pre-warm WebView engine and AdMob immediately for instant launch speed
        WebViewManager.prewarm(this)
        AdManager.initialize(this)

        setContent {
            val systemDark = isSystemInDarkTheme()
            var isDarkMode by remember { mutableStateOf(themeManager.isDarkMode(systemDark)) }
            var currentLanguage by remember { mutableStateOf(localizationManager.getSavedLanguage()) }

            MyApplicationTheme(darkTheme = isDarkMode) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    var currentScreen by remember { mutableStateOf(ScreenState.HOME) }

                    when (currentScreen) {
                        ScreenState.HOME -> {
                            HomeScreen(
                                credentialManager = credentialManager,
                                updateChecker = updateChecker,
                                activity = this@MainActivity,
                                isDarkMode = isDarkMode,
                                onToggleDarkMode = {
                                    val newMode = !isDarkMode
                                    isDarkMode = newMode
                                    themeManager.setDarkMode(newMode)
                                },
                                currentLanguage = currentLanguage,
                                onLanguageChange = { newLang ->
                                    currentLanguage = newLang
                                    localizationManager.saveLanguage(newLang)
                                },
                                onOpenMerchantPortal = {
                                    currentScreen = ScreenState.MERCHANT_PORTAL
                                }
                            )
                        }

                        ScreenState.MERCHANT_PORTAL -> {
                            MerchantPortalScreen(
                                credentialManager = credentialManager,
                                language = currentLanguage,
                                onNavigateBackToHome = {
                                    currentScreen = ScreenState.HOME
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Check for in-progress or pending updates when resuming
        if (::updateChecker.isInitialized) {
            updateChecker.checkForPlayStoreUpdate(
                activity = this,
                onUpdateAvailable = { updateInfo, isForced ->
                    if (isForced) {
                        updateChecker.startImmediateUpdate(this, updateInfo)
                    }
                },
                onNoUpdate = {}
            )
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == AppUpdateChecker.UPDATE_REQUEST_CODE) {
            if (resultCode != RESULT_OK) {
                // Update was cancelled or failed - re-prompt if required
                updateChecker.openPlayStorePage(this)
            }
        }
    }
}
