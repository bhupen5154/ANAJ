package com.example.webview

import android.app.AlertDialog
import android.graphics.Bitmap
import android.net.Uri
import android.os.Message
import android.util.Log
import android.webkit.ConsoleMessage
import android.webkit.JsPromptResult
import android.webkit.JsResult
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast

/**
 * High-performance WebChromeClient with:
 * - Loading progress & T3 response telemetry
 * - Native Android modal alerts & prompts for bank notifications
 * - Smooth multi-window routing for popup auth callbacks
 */
class MerchantWebChromeClient(
    private val onProgressCallback: (progress: Int) -> Unit,
    private val onTitleCallback: (title: String) -> Unit,
    private val onFileChooserCallback: ((ValueCallback<Array<Uri>>?, FileChooserParams?) -> Boolean)? = null
) : WebChromeClient() {

    override fun onProgressChanged(view: WebView?, newProgress: Int) {
        super.onProgressChanged(view, newProgress)
        PerformanceTracker.markT3FirstResponse(newProgress)
        onProgressCallback(newProgress)
    }

    override fun onReceivedTitle(view: WebView?, title: String?) {
        super.onReceivedTitle(view, title)
        title?.let { onTitleCallback(it) }
    }

    override fun onConsoleMessage(consoleMessage: ConsoleMessage?): Boolean {
        consoleMessage?.let {
            Log.d("PNB_PORTAL_JS", "${it.message()} [Line: ${it.lineNumber()}]")
        }
        return true
    }

    override fun onShowFileChooser(
        webView: WebView?,
        filePathCallback: ValueCallback<Array<Uri>>?,
        fileChooserParams: FileChooserParams?
    ): Boolean {
        return onFileChooserCallback?.invoke(filePathCallback, fileChooserParams)
            ?: super.onShowFileChooser(webView, filePathCallback, fileChooserParams)
    }

    override fun onJsAlert(view: WebView?, url: String?, message: String?, result: JsResult?): Boolean {
        val ctx = view?.context
        if (ctx != null && !message.isNullOrEmpty()) {
            try {
                AlertDialog.Builder(ctx)
                    .setTitle("PNB Notification")
                    .setMessage(message)
                    .setPositiveButton("OK") { dialog, _ ->
                        dialog.dismiss()
                        result?.confirm()
                    }
                    .setOnDismissListener {
                        try { result?.confirm() } catch (_: Throwable) {}
                    }
                    .show()
                return true
            } catch (_: Exception) {
                Toast.makeText(ctx, message, Toast.LENGTH_LONG).show()
                result?.confirm()
                return true
            }
        }
        result?.confirm()
        return true
    }

    override fun onJsConfirm(view: WebView?, url: String?, message: String?, result: JsResult?): Boolean {
        val ctx = view?.context
        if (ctx != null && !message.isNullOrEmpty()) {
            try {
                AlertDialog.Builder(ctx)
                    .setTitle("PNB Portal")
                    .setMessage(message)
                    .setPositiveButton("OK") { dialog, _ ->
                        dialog.dismiss()
                        result?.confirm()
                    }
                    .setNegativeButton("Cancel") { dialog, _ ->
                        dialog.dismiss()
                        result?.cancel()
                    }
                    .setOnDismissListener {
                        try { result?.cancel() } catch (_: Throwable) {}
                    }
                    .show()
                return true
            } catch (_: Exception) {
                result?.confirm()
                return true
            }
        }
        result?.confirm()
        return true
    }

    override fun onJsPrompt(view: WebView?, url: String?, message: String?, defaultValue: String?, result: JsPromptResult?): Boolean {
        result?.confirm(defaultValue)
        return true
    }

    override fun onCreateWindow(view: WebView?, isDialog: Boolean, isUserGesture: Boolean, resultMsg: Message?): Boolean {
        // Return false so all link clicks, form actions, and script navigations process in-place without blanking out the view
        return false
    }
}
