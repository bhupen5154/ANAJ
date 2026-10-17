package com.example.ads

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import java.util.concurrent.atomic.AtomicBoolean

/**
 * AdManager handles AdMob test ads initialization and rendering.
 * Uses official Google sample test ad units for MREC (300x250) and Standard Banner (320x50).
 */
object AdManager {
    // Production Google AdMob IDs
    const val BANNER_AD_UNIT_ID = "ca-app-pub-7573834929363761/4544544306"
    const val MREC_AD_UNIT_ID = "ca-app-pub-7573834929363761/8088028484"

    // Official Google AdMob sample test unit ID fallback
    const val TEST_BANNER_AD_UNIT_ID = "ca-app-pub-3940256099942544/6300978111"
    const val TEST_MREC_AD_UNIT_ID = "ca-app-pub-3940256099942544/6300978111"

    private val isInitialized = AtomicBoolean(false)

    fun initialize(context: Context) {
        if (isInitialized.compareAndSet(false, true)) {
            try {
                // Initialize in background to ensure zero cold start latency
                Thread {
                    try {
                        MobileAds.initialize(context.applicationContext) {}
                    } catch (_: Throwable) {
                        // Fail silently without blocking UI
                    }
                }.start()
            } catch (_: Throwable) {
                // Safe handling
            }
        }
    }
}

/**
 * Standard Small Banner (320x50) Composable for Merchant Portal.
 * Compact, lightweight, occupies minimal screen space outside WebView.
 */
@Composable
fun AdMobBannerAdView(
    modifier: Modifier = Modifier,
    adUnitId: String = AdManager.BANNER_AD_UNIT_ID
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(Unit) {
        AdManager.initialize(context)
        onDispose { }
    }

    var adFailed by remember { mutableStateOf(false) }
    var adLoaded by remember { mutableStateOf(false) }

    val adView = remember {
        try {
            AdView(context).apply {
                setAdSize(AdSize.BANNER) // 320 x 50 dp
                this.adUnitId = adUnitId
                adListener = object : AdListener() {
                    override fun onAdLoaded() {
                        adLoaded = true
                        adFailed = false
                    }

                    override fun onAdFailedToLoad(error: LoadAdError) {
                        adFailed = true
                        adLoaded = false
                    }
                }
            }
        } catch (_: Throwable) {
            null
        }
    }

    DisposableEffect(lifecycleOwner, adView) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE -> adView?.pause()
                Lifecycle.Event.ON_RESUME -> adView?.resume()
                Lifecycle.Event.ON_DESTROY -> adView?.destroy()
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            adView?.destroy()
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .testTag("admob_banner_container"),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(width = 320.dp, height = 50.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFFF1F1E8))
                .border(
                    1.dp,
                    Color(0xFFC5C7B9),
                    RoundedCornerShape(8.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            if (adView != null && !adFailed) {
                AndroidView(
                    modifier = Modifier.size(320.dp, 50.dp),
                    factory = {
                        try {
                            val adRequest = AdRequest.Builder().build()
                            adView.loadAd(adRequest)
                        } catch (_: Throwable) {
                            adFailed = true
                        }
                        adView
                    }
                )
            }

            if (!adLoaded) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 12.dp)
                ) {
                    Text(
                        text = "Advertisement",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF44493F).copy(alpha = 0.7f)
                    )
                }
            }
        }
    }
}

/**
 * Material 3 / Clean Minimalist AdMob Test MREC (300x250) Composable for Home Screen.
 * Renders cleanly outside content, fails gracefully if offline or load fails.
 */
@Composable
fun AdMobMrecAdView(
    modifier: Modifier = Modifier,
    adUnitId: String = AdManager.MREC_AD_UNIT_ID
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(Unit) {
        AdManager.initialize(context)
        onDispose { }
    }

    var adFailed by remember { mutableStateOf(false) }
    var adLoaded by remember { mutableStateOf(false) }

    val adView = remember {
        try {
            AdView(context).apply {
                setAdSize(AdSize.MEDIUM_RECTANGLE)
                this.adUnitId = adUnitId
                adListener = object : AdListener() {
                    override fun onAdLoaded() {
                        adLoaded = true
                        adFailed = false
                    }

                    override fun onAdFailedToLoad(error: LoadAdError) {
                        adFailed = true
                        adLoaded = false
                    }
                }
            }
        } catch (_: Throwable) {
            null
        }
    }

    DisposableEffect(lifecycleOwner, adView) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE -> adView?.pause()
                Lifecycle.Event.ON_RESUME -> adView?.resume()
                Lifecycle.Event.ON_DESTROY -> adView?.destroy()
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            adView?.destroy()
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .testTag("admob_mrec_container"),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "ADVERTISEMENT",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
            letterSpacing = 2.sp,
            fontSize = 10.sp
        )
        Spacer(modifier = Modifier.height(6.dp))

        Box(
            modifier = Modifier
                .size(width = 300.dp, height = 250.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFFF1F1E8))
                .border(
                    1.5.dp,
                    Color(0xFFC5C7B9),
                    RoundedCornerShape(12.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            if (adView != null && !adFailed) {
                AndroidView(
                    modifier = Modifier.size(300.dp, 250.dp),
                    factory = {
                        try {
                            val adRequest = AdRequest.Builder().build()
                            adView.loadAd(adRequest)
                        } catch (_: Throwable) {
                            adFailed = true
                        }
                        adView
                    }
                )
            }

            if (!adLoaded) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Advertisement",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF44493F).copy(alpha = 0.8f)
                    )
                }
            }
        }
    }
}

@Composable
fun CleanMinimalAdView(
    modifier: Modifier = Modifier,
    adUnitId: String = AdManager.MREC_AD_UNIT_ID
) {
    AdMobMrecAdView(modifier = modifier, adUnitId = adUnitId)
}
