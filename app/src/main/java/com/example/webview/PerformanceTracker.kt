package com.example.webview

import android.util.Log

/**
 * Performance measurement diagnostics for WebView startup and page loading lifecycle.
 * Tracks T0 -> T6 milestones as required by performance engineering specifications.
 * Strictly avoids logging any user credentials, CAPTCHA, tokens, or banking data.
 */
object PerformanceTracker {
    private const val TAG = "PNB_PERF_TELEMETRY"

    private var t0_portalTapped: Long = 0L
    private var t1_webViewInitComplete: Long = 0L
    private var t2_navigationStarted: Long = 0L
    private var t3_firstResponse: Long = 0L
    private var t4_firstVisibleContent: Long = 0L
    private var t5_domContentLoaded: Long = 0L
    private var t6_angularUsable: Long = 0L

    var isWarmLaunch: Boolean = false
        private set

    fun markT0PortalTapped(isWarm: Boolean = false) {
        t0_portalTapped = System.currentTimeMillis()
        t1_webViewInitComplete = 0L
        t2_navigationStarted = 0L
        t3_firstResponse = 0L
        t4_firstVisibleContent = 0L
        t5_domContentLoaded = 0L
        t6_angularUsable = 0L
        isWarmLaunch = isWarm
        Log.d(TAG, "T0: Merchant Portal Tapped [warm=$isWarm]")
    }

    fun markT1InitComplete() {
        if (t1_webViewInitComplete == 0L) {
            t1_webViewInitComplete = System.currentTimeMillis()
            val delta = t1_webViewInitComplete - t0_portalTapped
            Log.d(TAG, "T1: WebView Init Complete (+${delta}ms from T0)")
        }
    }

    fun markT2NavigationStarted() {
        if (t2_navigationStarted == 0L) {
            t2_navigationStarted = System.currentTimeMillis()
            val delta = t2_navigationStarted - t0_portalTapped
            Log.d(TAG, "T2: Navigation Started (+${delta}ms from T0)")
        }
    }

    fun markT3FirstResponse(progress: Int) {
        if (t3_firstResponse == 0L && progress > 10) {
            t3_firstResponse = System.currentTimeMillis()
            val delta = t3_firstResponse - t0_portalTapped
            Log.d(TAG, "T3: First Network Response / Progress ($progress%) (+${delta}ms from T0)")
        }
    }

    fun markT4FirstVisibleContent() {
        if (t4_firstVisibleContent == 0L) {
            t4_firstVisibleContent = System.currentTimeMillis()
            val deltaFromT0 = t4_firstVisibleContent - t0_portalTapped
            Log.i(TAG, "T4: First Visible Content Rendered! (T4 - T0 = ${deltaFromT0}ms)")
        }
    }

    fun markT5DomContentLoaded() {
        if (t5_domContentLoaded == 0L) {
            t5_domContentLoaded = System.currentTimeMillis()
            val deltaFromT0 = t5_domContentLoaded - t0_portalTapped
            Log.i(TAG, "T5: DOM / Page Load Finished (T5 - T0 = ${deltaFromT0}ms)")
        }
    }

    fun markT6AngularUsable() {
        if (t6_angularUsable == 0L) {
            t6_angularUsable = System.currentTimeMillis()
            val deltaFromT0 = t6_angularUsable - t0_portalTapped
            Log.i(TAG, "T6: Angular SPA Usable & Ready (T6 - T0 = ${deltaFromT0}ms)")
        }
    }

    fun getLastSummary(): String {
        if (t0_portalTapped == 0L) return "Awaiting navigation"
        val t4Delta = if (t4_firstVisibleContent > 0) "${t4_firstVisibleContent - t0_portalTapped}ms" else "Measuring..."
        val t5Delta = if (t5_domContentLoaded > 0) "${t5_domContentLoaded - t0_portalTapped}ms" else "Measuring..."
        return "Visible (T4-T0): $t4Delta | Loaded (T5-T0): $t5Delta"
    }
}
