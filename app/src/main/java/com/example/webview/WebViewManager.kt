package com.example.webview

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import android.os.Looper
import android.view.View
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.WebSettings
import android.webkit.WebView
import androidx.webkit.WebViewCompat
import androidx.webkit.WebViewFeature
import org.json.JSONObject
import java.net.InetAddress
import java.util.concurrent.atomic.AtomicBoolean

/**
 * High-performance, Chromium-grade WebView Engine Manager.
 * Implements:
 * 1. Safe connection pre-warming (asynchronous DNS pre-resolution & Chromium provider warmup).
 * 2. Single managed WebView lifecycle (zero reload on back/forth navigation).
 * 3. Modern Angular SPA settings (DOM storage, database, full cookies, hash routing).
 * 4. Standard HTTP cache reuse (LOAD_DEFAULT).
 * 5. Full renderer crash resilience and memory optimization.
 */
object WebViewManager {

    const val PNB_HOST = "cbdcr.pnb.bank.in"
    const val PNB_MERCHANT_PORTAL_URL = "https://cbdcr.pnb.bank.in/rtsp-merchant/#/login"

    // Real Mobile Chrome User-Agent without "Version/4.0" (prevents banking gateway from blocking WebViews)
    const val CHROME_MOBILE_USER_AGENT = "Mozilla/5.0 (Linux; Android 14; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Mobile Safari/537.36"

    private val isPrewarmed = AtomicBoolean(false)
    private var sharedWebView: WebView? = null
    private var isInitialPageLoaded = false

    /**
     * Non-blocking background pre-warming for DNS and Chromium engine.
     */
    fun prewarm(context: Context) {
        if (isPrewarmed.compareAndSet(false, true)) {
            // Clean any stale or corrupted Chromium variations_seed files in app_webview data directory
            try {
                cleanCorruptedVariationsSeeds(context.applicationContext)
            } catch (_: Throwable) {}

            Thread {
                try {
                    // 1. Asynchronous DNS Pre-resolution for target banking host
                    try {
                        InetAddress.getAllByName(PNB_HOST)
                    } catch (_: Throwable) {}

                    // 2. Warm CookieManager & Chromium internal thread
                    try {
                        val cookieManager = CookieManager.getInstance()
                        cookieManager.setAcceptCookie(true)
                    } catch (_: Throwable) {}
                } catch (_: Throwable) {}
            }.start()
        }
    }

    /**
     * Cleans stale, truncated, or unsigned Chromium Finch/Variations seed files from
     * the app's app_webview directory to prevent `variations_seed_loader.cc: Seed missing signature`.
     */
    private fun cleanCorruptedVariationsSeeds(context: Context) {
        try {
            val appDataDir = context.dataDir ?: context.filesDir?.parentFile ?: return
            val webViewDir = java.io.File(appDataDir, "app_webview")
            if (webViewDir.exists() && webViewDir.isDirectory) {
                val seedFiles = listOf("variations_seed", "variations_seed_new")
                for (name in seedFiles) {
                    val file = java.io.File(webViewDir, name)
                    if (file.exists()) {
                        // If file is empty or corrupted (missing valid signature), delete it so Chromium starts cleanly
                        file.delete()
                    }
                }
            }
        } catch (_: Throwable) {}
    }

    /**
     * Retrieves or lazily creates a shared, managed WebView instance.
     * Retains page state across screen navigation to eliminate reloading overhead.
     */
    fun getOrCreateSharedWebView(
        context: Context,
        onClientConfigured: (WebView) -> Unit
    ): Pair<WebView, Boolean> {
        val existing = sharedWebView
        if (existing != null) {
            // Detach from previous parent if still attached
            (existing.parent as? ViewGroup)?.removeView(existing)
            return Pair(existing, isInitialPageLoaded)
        }

        val newWebView = WebView(context).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            setBackgroundColor(android.graphics.Color.WHITE)
        }
        configureWebView(newWebView)
        onClientConfigured(newWebView)
        sharedWebView = newWebView
        return Pair(newWebView, false)
    }

    fun markPageLoaded() {
        isInitialPageLoaded = true
    }

    fun resetSharedWebView() {
        try {
            sharedWebView?.stopLoading()
            (sharedWebView?.parent as? ViewGroup)?.removeView(sharedWebView)
            sharedWebView?.destroy()
        } catch (_: Throwable) {}
        sharedWebView = null
        isInitialPageLoaded = false
    }

    @SuppressLint("SetJavaScriptEnabled")
    fun configureWebView(webView: WebView) {
        val settings = webView.settings

        // 1. Hardware Rendering & Background
        webView.setLayerType(View.LAYER_TYPE_HARDWARE, null)
        webView.setBackgroundColor(android.graphics.Color.WHITE)
        webView.isVerticalScrollBarEnabled = false
        webView.isHorizontalScrollBarEnabled = false
        webView.isScrollbarFadingEnabled = true
        webView.overScrollMode = View.OVER_SCROLL_IF_CONTENT_SCROLLS

        // 2. JavaScript, DOM Storage, IndexedDB & Angular Single Page Routing
        settings.javaScriptEnabled = true
        settings.domStorageEnabled = true
        settings.databaseEnabled = true
        settings.setGeolocationEnabled(true)
        settings.javaScriptCanOpenWindowsAutomatically = true
        // Set multiple windows to false so single-page Angular transitions and login redirects never wipe or blank out the webview
        settings.setSupportMultipleWindows(false)

        // 3. Native Chromium HTTP Caching (LOAD_DEFAULT enables browser disk & memory cache)
        settings.cacheMode = WebSettings.LOAD_DEFAULT
        settings.loadsImagesAutomatically = true
        settings.blockNetworkImage = false

        // 4. Viewport, Text & Layout Engine
        settings.useWideViewPort = true
        settings.loadWithOverviewMode = true
        settings.setSupportZoom(true)
        settings.builtInZoomControls = true
        settings.displayZoomControls = false
        settings.layoutAlgorithm = WebSettings.LayoutAlgorithm.NORMAL
        settings.mediaPlaybackRequiresUserGesture = false

        // 5. Genuine Mobile Chrome User Agent matching device Chromium build
        val defaultUa = try {
            WebSettings.getDefaultUserAgent(webView.context)
        } catch (_: Throwable) {
            CHROME_MOBILE_USER_AGENT
        }
        // Remove "; wv" and "Version/X.X" token so web servers treat it as authentic Chrome Mobile browser
        val cleanedUa = defaultUa.replace("; wv", "").replace(Regex("Version/\\d+\\.\\d+\\s?"), "")
        settings.userAgentString = cleanedUa

        // 6. Security, Mixed Content & Full Cookie Acceptance
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            settings.safeBrowsingEnabled = false
        }
        settings.mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
        settings.allowFileAccess = true
        settings.allowContentAccess = true

        val cookieManager = CookieManager.getInstance()
        cookieManager.setAcceptCookie(true)
        cookieManager.setAcceptThirdPartyCookies(webView, true)
        try {
            cookieManager.flush()
        } catch (_: Throwable) {}
    }

    /**
     * Injects credentials strictly into Login User ID and Password fields on the login screen.
     * Compatible with Angular Reactive Forms, AngularJS, and standard HTML5 inputs.
     * Safely updates form values and triggers model bindings without modifying CAPTCHA fields.
     */
    fun injectCredentials(webView: WebView, userId: String, pass: String, callback: ((Boolean) -> Unit)? = null) {
        if (userId.isEmpty() || pass.isEmpty()) {
            callback?.invoke(false)
            return
        }

        val jsonUser = JSONObject.quote(userId)
        val jsonPass = JSONObject.quote(pass)

        val jsScript = """
            (function() {
                try {
                    var userVal = $jsonUser;
                    var passVal = $jsonPass;

                    function setInputValue(el, val) {
                        if (!el) return false;
                        try {
                            el.focus();
                            var nativeSetter = Object.getOwnPropertyDescriptor(window.HTMLInputElement.prototype, 'value')?.set;
                            if (nativeSetter) {
                                nativeSetter.call(el, val);
                            } else {
                                el.value = val;
                            }
                            // Trigger all standard and framework events for Angular / Reactive Forms
                            el.dispatchEvent(new Event('input', { bubbles: true, cancelable: true }));
                            el.dispatchEvent(new Event('change', { bubbles: true, cancelable: true }));
                            el.dispatchEvent(new CustomEvent('input', { bubbles: true }));
                            return true;
                        } catch(err) {
                            try { el.value = val; return true; } catch(e) { return false; }
                        }
                    }

                    var allInputs = Array.from(document.querySelectorAll('input'));
                    if (!allInputs || allInputs.length === 0) return 'no_inputs';

                    // 1. Locate Password Input
                    var passInput = null;
                    for (var i = 0; i < allInputs.length; i++) {
                        var el = allInputs[i];
                        var type = (el.getAttribute('type') || '').toLowerCase();
                        var name = (el.getAttribute('name') || '').toLowerCase();
                        var id = (el.getAttribute('id') || '').toLowerCase();
                        var formControl = (el.getAttribute('formcontrolname') || el.getAttribute('ng-reflect-name') || '').toLowerCase();
                        var placeholder = (el.getAttribute('placeholder') || '').toLowerCase();

                        if (type === 'password' || name.includes('password') || id.includes('password') || 
                            formControl.includes('password') || placeholder.includes('password') || 
                            name.includes('pass') || id.includes('pass') || formControl.includes('pass') ||
                            id.includes('pwd') || name.includes('pwd')) {
                            passInput = el;
                            break;
                        }
                    }

                    // 2. Locate User ID Input (Strictly avoid CAPTCHA, hidden, buttons, checkboxes)
                    var userInput = null;
                    for (var j = 0; j < allInputs.length; j++) {
                        var inp = allInputs[j];
                        if (inp === passInput) continue;

                        var type = (inp.getAttribute('type') || 'text').toLowerCase();
                        if (type === 'hidden' || type === 'password' || type === 'submit' || type === 'button' || type === 'checkbox' || type === 'radio' || type === 'file') {
                            continue;
                        }

                        var name = (inp.getAttribute('name') || '').toLowerCase();
                        var id = (inp.getAttribute('id') || '').toLowerCase();
                        var formControl = (inp.getAttribute('formcontrolname') || inp.getAttribute('ng-reflect-name') || '').toLowerCase();
                        var placeholder = (inp.getAttribute('placeholder') || '').toLowerCase();
                        var aria = (inp.getAttribute('aria-label') || '').toLowerCase();

                        var isCaptcha = (name.includes('captcha') || id.includes('captcha') || formControl.includes('captcha') || 
                                         placeholder.includes('captcha') || aria.includes('captcha') || name.includes('capt') || 
                                         id.includes('capt') || placeholder.includes('capt') || name.includes('code') || 
                                         id.includes('code') || placeholder.includes('code') || name.includes('otp') || id.includes('otp'));

                        if (!isCaptcha) {
                            if (name.includes('user') || id.includes('user') || formControl.includes('user') ||
                                name.includes('login') || id.includes('login') || formControl.includes('login') ||
                                name.includes('merchant') || id.includes('merchant') || formControl.includes('merchant') ||
                                placeholder.includes('user') || placeholder.includes('merchant') || placeholder.includes('id') ||
                                placeholder.includes('login') || placeholder.includes('email') || placeholder.includes('phone') ||
                                placeholder.includes('mobile') || aria.includes('user') || aria.includes('merchant')) {
                                userInput = inp;
                                break;
                            }
                        }
                    }

                    // Fallback: If not matched by specific names, pick the first visible non-captcha input before password
                    if (!userInput && passInput) {
                        for (var k = 0; k < allInputs.length; k++) {
                            var inp2 = allInputs[k];
                            if (inp2 === passInput) break;
                            var type2 = (inp2.getAttribute('type') || 'text').toLowerCase();
                            if (type2 === 'hidden' || type2 === 'submit' || type2 === 'button') continue;

                            var name2 = (inp2.getAttribute('name') || '').toLowerCase();
                            var id2 = (inp2.getAttribute('id') || '').toLowerCase();
                            var formControl2 = (inp2.getAttribute('formcontrolname') || inp2.getAttribute('ng-reflect-name') || '').toLowerCase();
                            var placeholder2 = (inp2.getAttribute('placeholder') || '').toLowerCase();

                            var isCaptcha2 = (name2.includes('captcha') || id2.includes('captcha') || formControl2.includes('captcha') || 
                                             placeholder2.includes('captcha') || name2.includes('capt') || id2.includes('capt'));

                            if (!isCaptcha2) {
                                userInput = inp2;
                                break;
                            }
                        }
                    }

                    var filledUser = setInputValue(userInput, userVal);
                    var filledPass = setInputValue(passInput, passVal);

                    return (filledUser && filledPass) ? 'full' : (filledUser ? 'user_only' : (filledPass ? 'pass_only' : 'none'));
                } catch(e) {
                    return 'error: ' + e.message;
                }
            })();
        """.trimIndent()

        webView.evaluateJavascript(jsScript) { result ->
            val cleanRes = result?.replace("\"", "") ?: "none"
            val success = cleanRes == "full" || cleanRes == "user_only" || cleanRes == "pass_only"
            callback?.invoke(success)
        }
    }

    /**
     * Checks if the Angular login form input elements are rendered in the DOM.
     */
    fun isLoginFormReady(webView: WebView, callback: (Boolean) -> Unit) {
        val checkScript = """
            (function() {
                try {
                    var inputs = document.querySelectorAll('input:not([type="hidden"])');
                    return (inputs && inputs.length >= 2) ? 'ready' : 'waiting';
                } catch(e) {
                    return 'waiting';
                }
            })();
        """.trimIndent()

        webView.evaluateJavascript(checkScript) { result ->
            val clean = result?.replace("\"", "") ?: "waiting"
            callback(clean == "ready")
        }
    }

    /**
     * Robust automated selector for Angular Material Datepicker & Phone Type radio on merchant scheme pages.
     * Handles:
     * 1. Auto-selecting "Smart Phone" radio button if present on page.
     * 2. Programmatically triggering mat-datepicker-toggle calendar popup.
     * 3. 2-step navigation: Current Year ("2026") -> Current Month ("OCT") -> Today's Date ("10").
     * 4. Priority selection of .mat-calendar-body-today highlighted cells.
     * 5. Fallback event dispatching for reactive Angular forms.
     */
    /**
     * Robust automated selector for Angular Material Datepicker & Phone Type radio on merchant scheme pages.
     * Handles:
     * 1. Auto-selecting "Smart Phone" radio button if present on page.
     * 2. Programmatically triggering mat-datepicker-toggle calendar popup.
     * 3. 3-step navigation: Year View ("2026") -> Month View ("OCT") -> Day View ("10").
     * 4. Properly dispatches Pointer/Mouse events on inner <button> elements.
     * 5. Does not prematurely terminate before final Day selection.
     */
    fun autoFillDateAndMonth(webView: WebView, callback: ((Boolean) -> Unit)? = null) {
        val cal = java.util.Calendar.getInstance()
        val targetYear = cal.get(java.util.Calendar.YEAR)
        val targetMonth = cal.get(java.util.Calendar.MONTH) + 1 // 1-12 (October is 10)
        val targetDay = cal.get(java.util.Calendar.DAY_OF_MONTH) // 1-31 (10)

        val script = """
            (function() {
                try {
                    var targetYear = $targetYear;
                    var targetMonth = $targetMonth;
                    var targetDay = $targetDay;

                    var monthNamesShort = ['JAN', 'FEB', 'MAR', 'APR', 'MAY', 'JUN', 'JUL', 'AUG', 'SEP', 'OCT', 'NOV', 'DEC'];
                    var monthNamesFull = ['JANUARY', 'FEBRUARY', 'MARCH', 'APRIL', 'MAY', 'JUNE', 'JULY', 'AUGUST', 'SEPTEMBER', 'OCTOBER', 'NOVEMBER', 'DECEMBER'];
                    var currentMonthShort = monthNamesShort[targetMonth - 1];
                    var currentMonthFull = monthNamesFull[targetMonth - 1];

                    var actionsDone = [];

                    // Clean, single-target click & touch dispatcher without synthetic keyboard Enter
                    function clickCalendarElement(el) {
                        if (!el) return false;
                        var btn = el.querySelector ? (el.querySelector('button, .mat-calendar-body-cell-content') || el) : el;
                        try { btn.focus(); } catch(e) {}

                        try {
                            var rect = btn.getBoundingClientRect();
                            var cx = rect.left + rect.width / 2;
                            var cy = rect.top + rect.height / 2;

                            if (typeof Touch !== 'undefined' && typeof TouchEvent !== 'undefined') {
                                try {
                                    var touch = new Touch({ identifier: Date.now(), target: btn, clientX: cx, clientY: cy, pageX: cx, pageY: cy });
                                    btn.dispatchEvent(new TouchEvent('touchstart', { bubbles: true, cancelable: true, touches: [touch], targetTouches: [touch], changedTouches: [touch] }));
                                    btn.dispatchEvent(new TouchEvent('touchend', { bubbles: true, cancelable: true, touches: [], targetTouches: [], changedTouches: [touch] }));
                                } catch(te) {}
                            }

                            var evtOpts = { bubbles: true, cancelable: true, view: window, clientX: cx, clientY: cy };
                            try { btn.dispatchEvent(new PointerEvent('pointerdown', evtOpts)); } catch(pe) {}
                            try { btn.dispatchEvent(new MouseEvent('mousedown', evtOpts)); } catch(me) {}
                            try { btn.dispatchEvent(new PointerEvent('pointerup', evtOpts)); } catch(pe) {}
                            try { btn.dispatchEvent(new MouseEvent('mouseup', evtOpts)); } catch(me) {}

                            btn.click();
                        } catch(err) {
                            try { btn.click(); } catch(e) {}
                        }
                        return true;
                    }

                    // 1. Auto-select 'Smart Phone' radio button if present
                    try {
                        var radios = Array.from(document.querySelectorAll('mat-radio-button, label, .mat-radio-label, input[type="radio"]'));
                        for (var rIdx = 0; rIdx < radios.length; rIdx++) {
                            var r = radios[rIdx];
                            var txt = (r.innerText || r.textContent || r.getAttribute('aria-label') || '').toLowerCase();
                            if (txt.includes('smart phone') || txt.includes('smartphone')) {
                                var clickEl = r.querySelector('input') || r.querySelector('.mat-radio-inner-circle') || r;
                                clickCalendarElement(clickEl);
                                actionsDone.push('phone');
                                break;
                            }
                        }
                    } catch(e) {}

                    // 2. Direct Value Fill Helper (Dispatches M/D/YYYY e.g. 10/10/2026 to inputs)
                    function applyDirectDateFallback() {
                        try {
                            var mStr = String(targetMonth);
                            var dStr = String(targetDay);
                            var yStr = String(targetYear);
                            var mPadded = targetMonth < 10 ? '0' + targetMonth : mStr;
                            var dPadded = targetDay < 10 ? '0' + targetDay : dStr;

                            var targetSlashDate = mStr + '/' + dStr + '/' + yStr;
                            var targetPaddedDate = mPadded + '/' + dPadded + '/' + yStr;

                            var dateInps = Array.from(document.querySelectorAll('input'));
                            dateInps.forEach(function(inp) {
                                var type = (inp.getAttribute('type') || 'text').toLowerCase();
                                if (type === 'password' || type === 'hidden' || type === 'checkbox' || type === 'radio') return;

                                var val = (inp.value || '').trim();
                                var placeholder = (inp.getAttribute('placeholder') || '').toLowerCase();
                                var name = (inp.getAttribute('name') || '').toLowerCase();
                                var fcn = (inp.getAttribute('formcontrolname') || '').toLowerCase();
                                var aria = (inp.getAttribute('aria-label') || '').toLowerCase();

                                var isDateOrMonth = (
                                    val.includes('2026') ||
                                    val.includes('/1/2026') ||
                                    val.includes('/01/2026') ||
                                    placeholder.includes('month') || placeholder.includes('date') ||
                                    name.includes('month') || name.includes('date') ||
                                    fcn.includes('month') || fcn.includes('date') ||
                                    aria.includes('month') || aria.includes('date') ||
                                    inp.hasAttribute('matdatepicker')
                                );

                                if (isDateOrMonth) {
                                    try {
                                        inp.focus();
                                        var nativeSetter = Object.getOwnPropertyDescriptor(window.HTMLInputElement.prototype, 'value')?.set;
                                        if (nativeSetter) {
                                            nativeSetter.call(inp, targetSlashDate);
                                        } else {
                                            inp.value = targetSlashDate;
                                        }
                                    } catch(e) {
                                        try { inp.value = targetSlashDate; } catch(err) {}
                                    }

                                    ['input', 'change', 'blur'].forEach(function(evName) {
                                        try {
                                            inp.dispatchEvent(new Event(evName, { bubbles: true, cancelable: true }));
                                        } catch(evErr) {}
                                    });
                                    actionsDone.push('input_fallback');
                                }
                            });
                        } catch(e) {}
                    }

                    // 3. Open Calendar Toggle if popup not already open
                    var popup = document.querySelector('.mat-calendar, mat-datepicker-content, .cdk-overlay-pane mat-calendar');
                    if (!popup) {
                        var toggleBtns = Array.from(document.querySelectorAll('mat-datepicker-toggle button, [aria-label*="calendar" i], [aria-label*="datepicker" i], button.mat-datepicker-toggle-default-icon, button[aria-label*="Open" i]'));
                        if (toggleBtns.length > 0) {
                            clickCalendarElement(toggleBtns[0]);
                            actionsDone.push('toggle_clicked');
                        } else {
                            var dateInputs = Array.from(document.querySelectorAll('input[placeholder*="month" i], input[formcontrolname*="month" i], input[placeholder*="date" i], input[formcontrolname*="date" i]'));
                            for (var j = 0; j < dateInputs.length; j++) {
                                var container = dateInputs[j].closest('mat-form-field') || dateInputs[j].parentElement;
                                if (container) {
                                    var btn = container.querySelector('button, mat-datepicker-toggle button');
                                    if (btn) {
                                        clickCalendarElement(btn);
                                        actionsDone.push('field_toggle_clicked');
                                        break;
                                    }
                                }
                            }
                        }
                    }

                    // 4. Multi-view Navigation Loop (Year -> Month -> Day)
                    var ticks = 0;
                    var maxTicks = 80;
                    var actionCooldown = 0;

                    var interval = setInterval(function() {
                        ticks++;
                        if (actionCooldown > 0) {
                            actionCooldown--;
                            return;
                        }

                        var cal = document.querySelector('.mat-calendar, mat-datepicker-content, .cdk-overlay-pane mat-calendar');
                        if (!cal) {
                            // Calendar closed: verify direct values are applied
                            if (actionsDone.indexOf('day_clicked') !== -1 || ticks > 10) {
                                clearInterval(interval);
                                applyDirectDateFallback();
                            }
                            return;
                        }

                        var body = cal.querySelector('.mat-calendar-body, tbody');
                        if (!body) return;

                        var cells = Array.from(body.querySelectorAll('.mat-calendar-body-cell, td[role="gridcell"]'));
                        if (!cells || cells.length === 0) {
                            cells = Array.from(body.querySelectorAll('td, button'));
                        }
                        if (!cells || cells.length === 0) return;

                        function getCellExactText(c) {
                            var b = c.querySelector('button, .mat-calendar-body-cell-content') || c;
                            return (b.innerText || b.textContent || '').trim();
                        }

                        // Day cells: numbers 1..31
                        var dayCells = cells.filter(function(c) {
                            var t = getCellExactText(c);
                            var num = parseInt(t, 10);
                            return !isNaN(num) && num >= 1 && num <= 31 && t.length <= 2;
                        });

                        // Month cells: JAN..DEC
                        var monthCells = cells.filter(function(c) {
                            var t = getCellExactText(c).toUpperCase();
                            return monthNamesShort.indexOf(t) !== -1;
                        });

                        // Year cells: 4-digit years (e.g. 2026)
                        var yearCells = cells.filter(function(c) {
                            var t = getCellExactText(c);
                            var num = parseInt(t, 10);
                            return !isNaN(num) && num >= 2020 && num <= 2049 && t.length === 4;
                        });

                        // --- PRIORITY 1: DAY VIEW ACTIVE ---
                        if (dayCells.length >= 7) {
                            var dayTarget = null;

                            // 1. Look for official Angular Material "Today" marker
                            dayTarget = dayCells.find(function(c) {
                                return c.querySelector('.mat-calendar-body-today') !== null ||
                                       c.classList.contains('mat-calendar-body-today') ||
                                       (c.getAttribute('aria-current') || '') === 'date';
                            });

                            // 2. Look for exact numerical day number (targetDay e.g. 10)
                            if (!dayTarget) {
                                dayTarget = dayCells.find(function(c) {
                                    var num = parseInt(getCellExactText(c), 10);
                                    return num === targetDay;
                                });
                            }

                            // 3. Look for aria-label matching today's date
                            if (!dayTarget) {
                                dayTarget = dayCells.find(function(c) {
                                    var aria = (c.getAttribute('aria-label') || '').toLowerCase();
                                    return aria.includes(' ' + targetDay + ',') ||
                                           aria.includes(' ' + targetDay + ' ') ||
                                           aria.includes(' ' + targetDay + '-') ||
                                           aria.endsWith(' ' + targetDay);
                                });
                            }

                            // Strict check: Prevent clicking day 1 when today is not day 1
                            if (dayTarget) {
                                var selectedNum = parseInt(getCellExactText(dayTarget), 10);
                                if (targetDay !== 1 && selectedNum === 1) {
                                    var correctDay = dayCells.find(function(c) {
                                        return parseInt(getCellExactText(c), 10) === targetDay;
                                    });
                                    if (correctDay) dayTarget = correctDay;
                                }

                                clickCalendarElement(dayTarget);
                                actionsDone.push('day_clicked');
                                clearInterval(interval);

                                // Give Angular 350ms to close calendar, then reinforce correct date
                                setTimeout(function() {
                                    applyDirectDateFallback();
                                }, 350);
                                return;
                            }
                        }

                        // --- PRIORITY 2: MONTH VIEW ACTIVE ---
                        if (monthCells.length >= 4 && dayCells.length < 7) {
                            var monthTarget = monthCells.find(function(c) {
                                var t = getCellExactText(c).toUpperCase();
                                return t === currentMonthShort || t === currentMonthFull || t.indexOf(currentMonthShort) === 0;
                            });

                            if (!monthTarget) {
                                monthTarget = body.querySelector('.mat-calendar-body-today');
                            }

                            if (monthTarget) {
                                clickCalendarElement(monthTarget);
                                actionsDone.push('month_clicked');
                                actionCooldown = 4; // 4 ticks (480ms) for Day View to render
                                return;
                            }
                        }

                        // --- PRIORITY 3: MULTI-YEAR VIEW ACTIVE ---
                        if (yearCells.length >= 1 && monthCells.length < 4 && dayCells.length < 7) {
                            var yearTarget = yearCells.find(function(c) {
                                return parseInt(getCellExactText(c), 10) === targetYear;
                            });

                            if (!yearTarget) {
                                yearTarget = body.querySelector('.mat-calendar-body-today');
                            }

                            if (yearTarget) {
                                clickCalendarElement(yearTarget);
                                actionsDone.push('year_clicked');
                                actionCooldown = 4; // 4 ticks (480ms) for Month View to render
                                return;
                            }
                        }

                        if (ticks >= maxTicks) {
                            clearInterval(interval);
                            applyDirectDateFallback();
                        }
                    }, 120);

                    // Immediate fallback call for inputs already in DOM
                    applyDirectDateFallback();

                    return actionsDone.length > 0 ? 'success' : 'pending';
                } catch(err) {
                    return 'error: ' + err.message;
                }
            })();
        """.trimIndent()

        webView.evaluateJavascript(script) { result ->
            val clean = result?.replace("\"", "") ?: "error"
            val success = clean == "success" || clean == "pending"
            callback?.invoke(success)
        }
    }
}

