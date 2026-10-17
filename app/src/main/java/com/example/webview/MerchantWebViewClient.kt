package com.example.webview

import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.net.http.SslError
import android.os.Build
import android.util.Log
import android.webkit.RenderProcessGoneDetail
import android.webkit.SslErrorHandler
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient

/**
 * High-performance WebViewClient with:
 * - Direct Chromium network pass-through (no shouldInterceptRequest interference)
 * - T0 -> T6 performance telemetry tracking
 * - Safe SSL certificate validation
 * - Native renderer crash resilience (onRenderProcessGone)
 * - Lightweight auto-scroll on input focus
 */
class MerchantWebViewClient(
    private val onPageStartedCallback: (url: String) -> Unit,
    private val onPageFinishedCallback: (url: String) -> Unit,
    private val onPageVisibleCallback: (() -> Unit)? = null,
    private val onErrorCallback: (errorCode: Int, description: String) -> Unit,
    private val onSslErrorCallback: (error: SslError) -> Unit,
    private val onRendererCrashCallback: (() -> Unit)? = null
) : WebViewClient() {

    override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
        val url = request?.url?.toString() ?: return false
        return handleUrl(view, url)
    }

    @Deprecated("Deprecated in Java")
    override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
        if (url == null) return false
        return handleUrl(view, url)
    }

    private fun handleUrl(view: WebView?, url: String): Boolean {
        // Allow internal and script schemes to process natively
        if (url.startsWith("about:", ignoreCase = true) ||
            url.startsWith("javascript:", ignoreCase = true) ||
            url.startsWith("data:", ignoreCase = true) ||
            url.startsWith("blob:", ignoreCase = true)
        ) {
            return false
        }

        // Keep all banking http/https URLs strictly within the in-app WebView
        if (url.startsWith("http://", ignoreCase = true) || url.startsWith("https://", ignoreCase = true)) {
            return false
        }

        // Handle external intent schemes (e.g. tel:, mailto:, upi:)
        return try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            view?.context?.startActivity(intent)
            true
        } catch (_: Exception) {
            true
        }
    }

    override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
        super.onPageStarted(view, url, favicon)
        PerformanceTracker.markT2NavigationStarted()
        url?.let { onPageStartedCallback(it) }
    }

    override fun onPageCommitVisible(view: WebView?, url: String?) {
        super.onPageCommitVisible(view, url)
        PerformanceTracker.markT4FirstVisibleContent()
        onPageVisibleCallback?.invoke()
    }

    override fun onPageFinished(view: WebView?, url: String?) {
        super.onPageFinished(view, url)
        PerformanceTracker.markT5DomContentLoaded()
        WebViewManager.markPageLoaded()
        url?.let { onPageFinishedCallback(it) }
    }

    override fun onReceivedError(
        view: WebView?,
        request: WebResourceRequest?,
        error: WebResourceError?
    ) {
        super.onReceivedError(view, request, error)
        // Only trigger major error for true fatal main frame failures, ignoring normal redirect aborts
        if (request?.isForMainFrame == true) {
            val description = error?.description?.toString() ?: ""
            val errorCode = error?.errorCode ?: 0
            
            // Ignore normal navigation aborts and cache misses during login redirects
            if (description.contains("ERR_ABORTED", ignoreCase = true) ||
                description.contains("ERR_CACHE_MISS", ignoreCase = true) ||
                errorCode == -3 // net::ERR_ABORTED
            ) {
                return
            }

            onErrorCallback(errorCode, if (description.isEmpty()) "Connection error" else description)
        }
    }

    override fun onReceivedSslError(view: WebView?, handler: SslErrorHandler?, error: SslError?) {
        // STRICT SECURITY REQUIREMENT: Never bypass SSL certificate validation!
        handler?.cancel()
        if (error != null) {
            onSslErrorCallback(error)
        }
    }

    override fun onRenderProcessGone(view: WebView?, detail: RenderProcessGoneDetail?): Boolean {
        val didCrash = detail?.didCrash() ?: false
        Log.e("PNB_WEBVIEW", "Renderer process gone. Did crash: $didCrash")
        WebViewManager.resetSharedWebView()
        onRendererCrashCallback?.invoke()
        return true // Returning true informs the OS that the app handled the event safely
    }
}
