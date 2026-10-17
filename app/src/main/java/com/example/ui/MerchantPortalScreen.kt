package com.example.ui

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.net.http.SslError
import android.view.ViewGroup
import android.webkit.WebView
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.ads.AdMobBannerAdView
import com.example.ads.AdMobMrecAdView
import com.example.localization.AppLanguage
import com.example.localization.AppStrings
import com.example.security.SecureCredentialManager
import com.example.ui.theme.PnbCardStroke
import com.example.ui.theme.PnbGoldLight
import com.example.ui.theme.PnbPrimary
import com.example.ui.theme.PnbPrimaryDark
import com.example.ui.theme.PnbSecondary
import com.example.ui.theme.PnbSuccess
import com.example.ui.theme.PnbTertiary
import com.example.webview.MerchantWebChromeClient
import com.example.webview.MerchantWebViewClient
import com.example.webview.PerformanceTracker
import com.example.webview.WebViewManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MerchantPortalScreen(
    credentialManager: SecureCredentialManager,
    language: AppLanguage = AppLanguage.ENGLISH,
    onNavigateBackToHome: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val density = LocalDensity.current

    var webViewInstance by remember { mutableStateOf<WebView?>(null) }
    var currentUrl by remember { mutableStateOf(WebViewManager.PNB_MERCHANT_PORTAL_URL) }
    var pageTitle by remember { mutableStateOf("PNB Merchant Portal") }
    var loadingProgress by remember { mutableIntStateOf(0) }
    var isLoading by remember { mutableStateOf(false) }
    var hasNetworkError by remember { mutableStateOf(false) }
    var errorDetails by remember { mutableStateOf("") }
    var sslErrorDetected by remember { mutableStateOf(false) }

    var showCredentialDialog by remember { mutableStateOf(false) }
    var hasSavedCreds by remember { mutableStateOf(credentialManager.hasCredentials()) }
    var autoFillBannerVisible by remember { mutableStateOf(false) }

    // Check if soft keyboard is currently visible to hide bottom ad
    val imeInsets = WindowInsets.ime
    val isKeyboardOpen = imeInsets.getBottom(density) > 100

    fun openInChromeBrowser() {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(WebViewManager.PNB_MERCHANT_PORTAL_URL)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                setPackage("com.android.chrome")
            }
            context.startActivity(intent)
        } catch (_: Exception) {
            val fallbackIntent = Intent(Intent.ACTION_VIEW, Uri.parse(WebViewManager.PNB_MERCHANT_PORTAL_URL))
            context.startActivity(fallbackIntent)
        }
    }

    // Intercept back navigation: navigate within WebView history if possible, else return to Home
    BackHandler {
        if (webViewInstance?.canGoBack() == true) {
            webViewInstance?.goBack()
        } else {
            onNavigateBackToHome()
        }
    }

    val triggerAutoFill = {
        val creds = credentialManager.getCredentials()
        if (creds != null && webViewInstance != null) {
            WebViewManager.injectCredentials(webViewInstance!!, creds.first, creds.second) { success ->
                if (success) {
                    Toast.makeText(context, "User ID & Password Auto-filled! Enter CAPTCHA to login.", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "Ready! Auto-filling when login form appears.", Toast.LENGTH_SHORT).show()
                }
            }
        } else if (creds == null) {
            showCredentialDialog = true
        }
    }

    if (showCredentialDialog) {
        CredentialManagementDialog(
            hasExistingCredentials = hasSavedCreds,
            initialUserId = if (hasSavedCreds) (credentialManager.getCredentials()?.first ?: "") else "",
            language = language,
            onSave = { user, pass ->
                credentialManager.saveCredentials(user, pass)
                hasSavedCreds = true
                showCredentialDialog = false
                triggerAutoFill()
            },
            onClear = {
                credentialManager.clearCredentials()
                hasSavedCreds = false
                showCredentialDialog = false
                Toast.makeText(context, "Saved credentials cleared", Toast.LENGTH_SHORT).show()
            },
            onDismiss = {
                showCredentialDialog = false
            }
        )
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .imePadding(),
        topBar = {
            Column(modifier = Modifier.fillMaxWidth()) {
                TopAppBar(
                    title = {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = "HTTPS Secure",
                                    tint = PnbGoldLight,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "cbdcr.pnb.bank.in",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Text(
                                text = "PNB Merchant Portal (High-Speed Engine)",
                                style = MaterialTheme.typography.labelSmall,
                                color = PnbGoldLight.copy(alpha = 0.9f),
                                fontSize = 10.sp
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(
                            onClick = {
                                if (webViewInstance?.canGoBack() == true) {
                                    webViewInstance?.goBack()
                                } else {
                                    onNavigateBackToHome()
                                }
                            },
                            modifier = Modifier.testTag("portal_back_btn")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = Color.White
                            )
                        }
                    },
                    actions = {
                        IconButton(
                            onClick = {
                                webViewInstance?.let { wv ->
                                    Toast.makeText(context, "📅 Aaj ki Date & Month Auto-select ho rahi hai...", Toast.LENGTH_SHORT).show()
                                    WebViewManager.autoFillDateAndMonth(wv)
                                }
                            },
                            modifier = Modifier.testTag("portal_auto_date_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.DateRange,
                                contentDescription = "Auto Fill Date & Month",
                                tint = PnbGoldLight
                            )
                        }

                        if (hasSavedCreds) {
                            IconButton(
                                onClick = { triggerAutoFill() },
                                modifier = Modifier.testTag("portal_autofill_btn")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AutoFixHigh,
                                    contentDescription = "Auto-fill credentials",
                                    tint = PnbGoldLight
                                )
                            }
                        }

                        IconButton(
                            onClick = { showCredentialDialog = true },
                            modifier = Modifier.testTag("portal_creds_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Key,
                                contentDescription = "Credentials settings",
                                tint = Color.White
                            )
                        }

                        IconButton(
                            onClick = { openInChromeBrowser() },
                            modifier = Modifier.testTag("portal_open_chrome_btn")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                                contentDescription = "Open in Chrome App",
                                tint = Color.White
                            )
                        }

                        IconButton(
                            onClick = {
                                hasNetworkError = false
                                sslErrorDetected = false
                                isLoading = true
                                PerformanceTracker.markT0PortalTapped(isWarm = true)
                                webViewInstance?.reload()
                            },
                            modifier = Modifier.testTag("portal_refresh_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Refresh page",
                                tint = Color.White
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = PnbPrimary
                    ),
                    modifier = Modifier.testTag("portal_top_bar")
                )

                // Page Loading Indicator
                if (isLoading) {
                    LinearProgressIndicator(
                        progress = { loadingProgress / 100f },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(3.dp)
                            .testTag("portal_progress_bar"),
                        color = PnbTertiary,
                        trackColor = PnbPrimaryDark
                    )
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Quick Auto-fill banner when on Login page (only when soft keyboard is closed)
            if (autoFillBannerVisible && !isKeyboardOpen) {
                Surface(
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        if (hasSavedCreds) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = PnbSuccess,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Credentials saved (Auto-fill ready)",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 12.sp
                                )
                            }

                            Button(
                                onClick = { triggerAutoFill() },
                                colors = ButtonDefaults.buttonColors(containerColor = PnbPrimary),
                                shape = RoundedCornerShape(6.dp),
                                modifier = Modifier.height(32.dp)
                            ) {
                                Text("Auto-fill", fontSize = 11.sp)
                            }
                        } else {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Key,
                                    contentDescription = null,
                                    tint = PnbPrimary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Save credentials for 1-Tap Login",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 12.sp
                                )
                            }

                            Button(
                                onClick = { showCredentialDialog = true },
                                colors = ButtonDefaults.buttonColors(containerColor = PnbPrimary),
                                shape = RoundedCornerShape(6.dp),
                                modifier = Modifier.height(32.dp)
                            ) {
                                Text("Save Now", fontSize = 11.sp)
                            }
                        }
                    }
                }
            }

            // Main Content Area: Reusable High-Speed WebView & Error Overlay (Flex 1f)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                AndroidView(
                    factory = { ctx ->
                        val (wv, isAlreadyLoaded) = WebViewManager.getOrCreateSharedWebView(ctx) { webView ->
                            webView.webViewClient = MerchantWebViewClient(
                                onPageStartedCallback = { url ->
                                    currentUrl = url
                                    isLoading = true
                                    hasNetworkError = false
                                },
                                onPageFinishedCallback = { url ->
                                    currentUrl = url
                                    hasNetworkError = false

                                    val isLoginPage = url.contains("login", ignoreCase = true) || 
                                            (url.endsWith("rtsp-merchant/#/") || url.endsWith("rtsp-merchant/"))
                                    val isDashboardOrInnerPage = url.contains("dashboard", ignoreCase = true) || 
                                            url.contains("report", ignoreCase = true) || 
                                            url.contains("transaction", ignoreCase = true) || 
                                            url.contains("settlement", ignoreCase = true) || 
                                            url.contains("profile", ignoreCase = true)

                                    val showBanner = isLoginPage && !isDashboardOrInnerPage
                                    autoFillBannerVisible = showBanner

                                    if (showBanner) {
                                        // Keep loading spinner active over the transparent webview until login form renders and auto-fills
                                        coroutineScope.launch {
                                            val creds = credentialManager.getCredentials()
                                            var isAutoFilledOrReady = false
                                            val startTime = System.currentTimeMillis()

                                            // Fast polling loop every 150ms for up to 4.5 seconds to auto-detect DOM elements
                                            while (!isAutoFilledOrReady && (System.currentTimeMillis() - startTime) < 4500) {
                                                delay(150)
                                                if (webViewInstance != null) {
                                                    if (creds != null) {
                                                        WebViewManager.injectCredentials(webViewInstance!!, creds.first, creds.second) { success ->
                                                            if (success) {
                                                                isAutoFilledOrReady = true
                                                                isLoading = false
                                                            }
                                                        }
                                                    } else {
                                                        WebViewManager.isLoginFormReady(webViewInstance!!) { ready ->
                                                            if (ready) {
                                                                isAutoFilledOrReady = true
                                                                isLoading = false
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                            // Fallback safety timeout to ensure spinner is dismissed
                                            delay(200)
                                            isLoading = false
                                        }
                                    } else {
                                        isLoading = false
                                        // Auto-trigger Date & Month selection on inner pages (schemes, reports, transactions)
                                        coroutineScope.launch {
                                            delay(1000)
                                            webViewInstance?.let { wv ->
                                                WebViewManager.autoFillDateAndMonth(wv)
                                            }
                                        }
                                    }
                                },
                                onPageVisibleCallback = {
                                    val isLoginPage = currentUrl.contains("login", ignoreCase = true) || 
                                            (currentUrl.endsWith("rtsp-merchant/#/") || currentUrl.endsWith("rtsp-merchant/"))
                                    if (!isLoginPage) {
                                        isLoading = false
                                    }
                                },
                                onErrorCallback = { _, desc ->
                                    isLoading = false
                                    hasNetworkError = true
                                    errorDetails = desc
                                },
                                onSslErrorCallback = { _ ->
                                    isLoading = false
                                    sslErrorDetected = true
                                    hasNetworkError = true
                                    errorDetails = "SSL Certificate Verification Failed. Connection securely terminated."
                                },
                                onRendererCrashCallback = {
                                    isLoading = false
                                    hasNetworkError = true
                                    errorDetails = "Renderer refreshed due to system memory optimization."
                                }
                            )

                            webView.webChromeClient = MerchantWebChromeClient(
                                onProgressCallback = { progress ->
                                    loadingProgress = progress
                                    val isLoginPage = currentUrl.contains("login", ignoreCase = true) || 
                                            (currentUrl.endsWith("rtsp-merchant/#/") || currentUrl.endsWith("rtsp-merchant/"))
                                    if (progress >= 100 && !isLoginPage) {
                                        isLoading = false
                                    }
                                },
                                onTitleCallback = { title ->
                                    pageTitle = title
                                }
                            )
                        }

                        if (!isAlreadyLoaded) {
                            isLoading = true
                            PerformanceTracker.markT0PortalTapped(isWarm = false)
                            PerformanceTracker.markT1InitComplete()
                            wv.loadUrl(WebViewManager.PNB_MERCHANT_PORTAL_URL)
                        } else {
                            isLoading = false
                            PerformanceTracker.markT0PortalTapped(isWarm = true)
                            PerformanceTracker.markT4FirstVisibleContent()
                            PerformanceTracker.markT5DomContentLoaded()
                        }

                        webViewInstance = wv
                        wv
                    },
                    update = { view ->
                        webViewInstance = view
                    },
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("merchant_webview")
                )

                // Floating "⚡ Auto Date" button on inner portal pages (scheme, reports, etc.)
                if (!autoFillBannerVisible && !isLoading && !hasNetworkError && !isKeyboardOpen) {
                    Surface(
                        onClick = {
                            webViewInstance?.let { wv ->
                                Toast.makeText(context, "📅 Aaj ki Date & Month Auto-select ho rahi hai...", Toast.LENGTH_SHORT).show()
                                WebViewManager.autoFillDateAndMonth(wv)
                            }
                        },
                        shape = RoundedCornerShape(24.dp),
                        color = PnbPrimary,
                        shadowElevation = 8.dp,
                        tonalElevation = 4.dp,
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(end = 16.dp, bottom = 16.dp)
                            .testTag("portal_floating_autodate_btn")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.DateRange,
                                contentDescription = null,
                                tint = PnbGoldLight,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "⚡ Auto Date",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }

                // Semi-Transparent Floating Loading Indicator Overlay with MREC Ad
                if (isLoading && !hasNetworkError) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.35f))
                            .testTag("portal_loading_overlay"),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .padding(horizontal = 16.dp)
                                .verticalScroll(rememberScrollState())
                        ) {
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
                                tonalElevation = 6.dp,
                                shadowElevation = 8.dp,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .padding(horizontal = 16.dp, vertical = 14.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    CircularProgressIndicator(
                                        modifier = Modifier
                                            .size(28.dp)
                                            .testTag("portal_circular_spinner"),
                                        color = PnbPrimary,
                                        strokeWidth = 3.dp,
                                        trackColor = PnbPrimary.copy(alpha = 0.15f)
                                    )

                                    Spacer(modifier = Modifier.width(14.dp))

                                    Column {
                                        Text(
                                            text = AppStrings.get("loading_portal", language),
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = if (hasSavedCreds) "Detecting login & auto-filling..." else AppStrings.get("loading_subtext", language),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            fontSize = 11.5.sp
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // MREC Test Ad (300x250) shown while loading is active
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surface,
                                shadowElevation = 6.dp,
                                modifier = Modifier.padding(bottom = 6.dp)
                            ) {
                                AdMobMrecAdView()
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            // Normal Banner Ad (320x50) shown right below MREC ad
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.surface,
                                shadowElevation = 4.dp,
                                modifier = Modifier.padding(bottom = 6.dp)
                            ) {
                                AdMobBannerAdView()
                            }
                        }
                    }
                }

                // Network Failure / SSL Error Overlay
                if (hasNetworkError) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.background)
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            ),
                            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Surface(
                                    color = if (sslErrorDetected) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.error.copy(alpha = 0.12f),
                                    shape = CircleShape,
                                    modifier = Modifier.size(56.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = if (sslErrorDetected) Icons.Default.Warning else Icons.Default.WifiOff,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.error,
                                            modifier = Modifier.size(28.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(16.dp))

                                Text(
                                    text = if (sslErrorDetected) "Security Certificate Alert" else "Unable to Reach Portal",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                Text(
                                    text = if (sslErrorDetected) {
                                        "The portal SSL certificate could not be validated. To protect your banking data, the connection was cancelled."
                                    } else {
                                        "Please check your internet connection or mobile network data. The PNB merchant portal requires an active connection."
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center,
                                    lineHeight = 18.sp
                                )

                                if (errorDetails.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "Status: $errorDetails",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.error
                                    )
                                }

                                Spacer(modifier = Modifier.height(20.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Button(
                                        onClick = {
                                            hasNetworkError = false
                                            sslErrorDetected = false
                                            isLoading = true
                                            PerformanceTracker.markT0PortalTapped(isWarm = true)
                                            webViewInstance?.loadUrl(WebViewManager.PNB_MERCHANT_PORTAL_URL)
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = PnbPrimary),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("portal_retry_btn")
                                    ) {
                                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Retry")
                                    }

                                    OutlinedButton(
                                        onClick = { openInChromeBrowser() },
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("portal_chrome_open_btn")
                                    ) {
                                        Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Chrome")
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Bottom Ad Area (Automatically hidden when keyboard is open)
            if (!isKeyboardOpen) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("portal_bottom_ad_wrapper"),
                    color = MaterialTheme.colorScheme.surface,
                    shadowElevation = 2.dp
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        AdMobBannerAdView()
                    }
                }
            }
        }
    }
}
