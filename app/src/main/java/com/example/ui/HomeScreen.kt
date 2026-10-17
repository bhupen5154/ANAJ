package com.example.ui

import android.app.Activity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.SyncLock
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ads.AdMobBannerAdView
import com.example.ads.AdMobMrecAdView
import com.example.localization.AppLanguage
import com.example.localization.AppStrings
import com.example.security.SecureCredentialManager
import com.example.ui.theme.PnbGoldLight
import com.example.ui.theme.PnbPrimary
import com.example.ui.theme.PnbPrimaryDark
import com.example.ui.theme.PnbSuccess
import com.example.ui.theme.PnbTertiary
import com.example.update.AppUpdateChecker
import com.example.update.AppUpdateState
import com.google.android.play.core.appupdate.AppUpdateInfo
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    credentialManager: SecureCredentialManager,
    updateChecker: AppUpdateChecker? = null,
    activity: Activity? = null,
    isDarkMode: Boolean = false,
    onToggleDarkMode: () -> Unit = {},
    currentLanguage: AppLanguage = AppLanguage.ENGLISH,
    onLanguageChange: (AppLanguage) -> Unit = {},
    onOpenMerchantPortal: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    var showCredentialDialog by remember { mutableStateOf(false) }
    var showLanguageDialog by remember { mutableStateOf(false) }
    var showPrivacyPolicyDialog by remember { mutableStateOf(false) }

    // App Update State & Dialog
    var showUpdateDialog by remember { mutableStateOf(false) }
    var currentUpdateState by remember { mutableStateOf(AppUpdateState()) }
    var playStoreAppUpdateInfo by remember { mutableStateOf<AppUpdateInfo?>(null) }

    var hasSavedCreds by remember { mutableStateOf(credentialManager.hasCredentials()) }
    var maskedUser by remember { mutableStateOf(credentialManager.getMaskedUserId()) }

    val refreshCredState = {
        hasSavedCreds = credentialManager.hasCredentials()
        maskedUser = credentialManager.getMaskedUserId()
    }

    // Check for App Updates automatically when Home screen opens
    LaunchedEffect(Unit) {
        val currentActivity = activity
        val checker = updateChecker
        if (currentActivity != null && checker != null) {
            // 1. Check Google Play Store In-App Updates API
            checker.checkForPlayStoreUpdate(
                activity = currentActivity,
                onUpdateAvailable = { updateInfo, isForced ->
                    playStoreAppUpdateInfo = updateInfo
                    currentUpdateState = currentUpdateState.copy(
                        isUpdateAvailable = true,
                        isForced = isForced,
                        latestVersionCode = updateInfo.availableVersionCode()
                    )
                    showUpdateDialog = true
                },
                onNoUpdate = {
                    // 2. Check Version Policy
                    coroutineScope.launch {
                        val policyState = checker.checkVersionPolicy()
                        if (policyState.isUpdateAvailable) {
                            currentUpdateState = policyState
                            showUpdateDialog = true
                        }
                    }
                }
            )
        }
    }

    // Force/Mandatory App Update Dialog Popup
    if (showUpdateDialog && currentUpdateState.isUpdateAvailable) {
        AppUpdateDialog(
            updateState = currentUpdateState,
            language = currentLanguage,
            onUpdateClick = {
                val act = activity
                val checker = updateChecker
                if (act != null && checker != null) {
                    val info = playStoreAppUpdateInfo
                    if (info != null) {
                        checker.startImmediateUpdate(act, info)
                    } else {
                        checker.openPlayStorePage(act)
                    }
                }
            },
            onDismiss = {
                showUpdateDialog = false
            }
        )
    }

    if (showCredentialDialog) {
        CredentialManagementDialog(
            hasExistingCredentials = hasSavedCreds,
            initialUserId = if (hasSavedCreds) (credentialManager.getCredentials()?.first ?: "") else "",
            language = currentLanguage,
            onSave = { user, pass ->
                credentialManager.saveCredentials(user, pass)
                refreshCredState()
                showCredentialDialog = false
            },
            onClear = {
                credentialManager.clearCredentials()
                refreshCredState()
                showCredentialDialog = false
            },
            onDismiss = {
                showCredentialDialog = false
            }
        )
    }

    if (showLanguageDialog) {
        LanguageSelectionDialog(
            currentLanguage = currentLanguage,
            onLanguageSelected = onLanguageChange,
            onDismiss = { showLanguageDialog = false }
        )
    }

    if (showPrivacyPolicyDialog) {
        PrivacyPolicyDialog(
            language = currentLanguage,
            onDismiss = { showPrivacyPolicyDialog = false }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = Color.Transparent,
                            modifier = Modifier.size(34.dp)
                        ) {
                            androidx.compose.foundation.Image(
                                painter = androidx.compose.ui.res.painterResource(id = com.example.R.drawable.ic_anaj_pay_logo),
                                contentDescription = "AnajPay Logo",
                                modifier = Modifier.size(34.dp),
                                contentScale = androidx.compose.ui.layout.ContentScale.Crop
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = AppStrings.get("app_title", currentLanguage),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                fontSize = 16.sp
                            )
                            Text(
                                text = AppStrings.get("app_subtitle", currentLanguage),
                                style = MaterialTheme.typography.labelSmall,
                                color = PnbGoldLight,
                                fontSize = 10.sp
                            )
                        }
                    }
                },
                actions = {
                    // Check for Updates Action
                    IconButton(
                        onClick = {
                            coroutineScope.launch {
                                val currentActivity = activity
                                val checker = updateChecker
                                if (currentActivity != null && checker != null) {
                                    checker.checkForPlayStoreUpdate(
                                        activity = currentActivity,
                                        onUpdateAvailable = { updateInfo, isForced ->
                                            playStoreAppUpdateInfo = updateInfo
                                            currentUpdateState = currentUpdateState.copy(
                                                isUpdateAvailable = true,
                                                isForced = isForced,
                                                latestVersionCode = updateInfo.availableVersionCode()
                                            )
                                            showUpdateDialog = true
                                        },
                                        onNoUpdate = {
                                            coroutineScope.launch {
                                                val state = checker.checkVersionPolicy()
                                                if (state.isUpdateAvailable) {
                                                    currentUpdateState = state
                                                    showUpdateDialog = true
                                                } else {
                                                    // If already on latest version, prompt Play Store page to ensure user has fresh version
                                                    checker.openPlayStorePage(currentActivity)
                                                }
                                            }
                                        }
                                    )
                                }
                            }
                        },
                        modifier = Modifier.testTag("check_update_top_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.SystemUpdate,
                            contentDescription = AppStrings.get("update_check_btn", currentLanguage),
                            tint = PnbGoldLight
                        )
                    }

                    IconButton(
                        onClick = onToggleDarkMode,
                        modifier = Modifier.testTag("toggle_dark_mode_btn")
                    ) {
                        Icon(
                            imageVector = if (isDarkMode) Icons.Default.LightMode else Icons.Default.DarkMode,
                            contentDescription = if (isDarkMode) AppStrings.get("light_mode", currentLanguage) else AppStrings.get("dark_mode", currentLanguage),
                            tint = if (isDarkMode) PnbGoldLight else Color.White
                        )
                    }

                    IconButton(
                        onClick = { showLanguageDialog = true },
                        modifier = Modifier.testTag("select_language_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Translate,
                            contentDescription = AppStrings.get("language_select", currentLanguage),
                            tint = Color.White
                        )
                    }

                    IconButton(
                        onClick = { showPrivacyPolicyDialog = true },
                        modifier = Modifier.testTag("open_privacy_policy_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Shield,
                            contentDescription = AppStrings.get("privacy_policy", currentLanguage),
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = PnbPrimary
                ),
                modifier = Modifier.testTag("home_top_bar")
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Compact Hero Card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(PnbPrimary, PnbPrimaryDark)
                        )
                    )
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = AppStrings.get("hero_title", currentLanguage),
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = AppStrings.get("hero_desc", currentLanguage),
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.9f),
                        textAlign = TextAlign.Center,
                        fontSize = 11.5.sp,
                        lineHeight = 15.sp
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Primary Main Action Button
                    Button(
                        onClick = onOpenMerchantPortal,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = PnbTertiary,
                            contentColor = Color.Black
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp)
                            .testTag("open_merchant_portal_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.OpenInBrowser,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = AppStrings.get("open_portal_button", currentLanguage),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Credentials & Security Status Section (Compact)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp)
            ) {
                ElevatedCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("security_status_card"),
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.elevatedCardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    elevation = CardDefaults.elevatedCardElevation(defaultElevation = 1.dp)
                ) {
                    Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.SyncLock,
                                    contentDescription = null,
                                    tint = PnbPrimary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = AppStrings.get("feature_3_title", currentLanguage),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            if (hasSavedCreds) {
                                Surface(
                                    color = PnbSuccess.copy(alpha = 0.15f),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = null,
                                            tint = PnbSuccess,
                                            modifier = Modifier.size(12.dp)
                                        )
                                        Spacer(modifier = Modifier.width(3.dp))
                                        Text(
                                            text = AppStrings.get("creds_saved_badge", currentLanguage),
                                            color = PnbSuccess,
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 10.sp
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        if (hasSavedCreds && maskedUser != null) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${AppStrings.get("saved_user_label", currentLanguage)}: $maskedUser",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                OutlinedButton(
                                    onClick = { showCredentialDialog = true },
                                    modifier = Modifier
                                        .height(32.dp)
                                        .testTag("manage_creds_btn"),
                                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                    Icon(Icons.Default.Key, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(AppStrings.get("edit_creds_button", currentLanguage), fontSize = 11.sp)
                                }
                            }
                        } else {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = AppStrings.get("cred_dialog_desc", currentLanguage),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 10.5.sp,
                                    modifier = Modifier.weight(1f)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                OutlinedButton(
                                    onClick = { showCredentialDialog = true },
                                    modifier = Modifier
                                        .height(32.dp)
                                        .testTag("setup_creds_btn"),
                                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                    Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(AppStrings.get("save_creds_button", currentLanguage), fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Compact Disclaimer & Privacy Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "Disclaimer",
                            tint = MaterialTheme.colorScheme.outline,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = AppStrings.get("disclaimer_footer", currentLanguage),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 9.5.sp,
                            maxLines = 1
                        )
                    }

                    TextButton(
                        onClick = { showPrivacyPolicyDialog = true },
                        modifier = Modifier
                            .height(28.dp)
                            .testTag("footer_privacy_policy_btn"),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 4.dp, vertical = 0.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Shield,
                            contentDescription = null,
                            modifier = Modifier.size(12.dp),
                            tint = PnbPrimary
                        )
                        Spacer(modifier = Modifier.width(2.dp))
                        Text(
                            text = AppStrings.get("privacy_policy", currentLanguage),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = PnbPrimary,
                            fontSize = 10.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Google AdMob MREC & Standard Banner ads container
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                AdMobMrecAdView()
                Spacer(modifier = Modifier.height(6.dp))
                AdMobBannerAdView()
            }
        }
    }
}
