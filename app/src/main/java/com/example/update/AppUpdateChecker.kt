package com.example.update

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.util.Log
import com.example.BuildConfig
import com.google.android.play.core.appupdate.AppUpdateInfo
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.appupdate.AppUpdateOptions
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.UpdateAvailability
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

data class AppUpdateState(
    val isUpdateAvailable: Boolean = false,
    val isForced: Boolean = false,
    val currentVersionCode: Int = BuildConfig.VERSION_CODE,
    val currentVersionName: String = BuildConfig.VERSION_NAME,
    val latestVersionCode: Int = BuildConfig.VERSION_CODE,
    val latestVersionName: String = BuildConfig.VERSION_NAME,
    val releaseNotes: String = "",
    val playStoreUrl: String = "https://play.google.com/store/apps/details?id=bgs.com.ration"
)

class AppUpdateChecker(private val context: Context) {

    private val appUpdateManager: AppUpdateManager by lazy {
        AppUpdateManagerFactory.create(context)
    }

    private val prefs: SharedPreferences =
        context.getSharedPreferences("app_update_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val TAG = "AppUpdateChecker"
        const val UPDATE_REQUEST_CODE = 9001
        const val PLAY_STORE_PACKAGE_NAME = "bgs.com.ration"

        // Default remote config URL fallback or version endpoint
        private const val PREF_LATEST_VERSION_CODE = "pref_latest_version_code"
        private const val PREF_LATEST_VERSION_NAME = "pref_latest_version_name"
        private const val PREF_IS_FORCED = "pref_is_forced"
        private const val PREF_RELEASE_NOTES = "pref_release_notes"
    }

    /**
     * Check Play Store AppUpdateManager for Google Play In-App Updates.
     */
    fun checkForPlayStoreUpdate(
        activity: Activity,
        onUpdateAvailable: (AppUpdateInfo, Boolean) -> Unit,
        onNoUpdate: () -> Unit
    ) {
        val appUpdateInfoTask = appUpdateManager.appUpdateInfo
        appUpdateInfoTask.addOnSuccessListener { appUpdateInfo ->
            if (appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE) {
                val isImmediateAllowed = appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE)
                val isFlexibleAllowed = appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE)
                val isForced = isImmediateAllowed || appUpdateInfo.updatePriority() >= 4

                Log.d(TAG, "Play Store Update Available: priority=${appUpdateInfo.updatePriority()}, isForced=$isForced")
                onUpdateAvailable(appUpdateInfo, isForced)
            } else if (appUpdateInfo.updateAvailability() == UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS) {
                // Resume in-progress update
                try {
                    appUpdateManager.startUpdateFlowForResult(
                        appUpdateInfo,
                        activity,
                        AppUpdateOptions.newBuilder(AppUpdateType.IMMEDIATE).build(),
                        UPDATE_REQUEST_CODE
                    )
                } catch (e: Exception) {
                    Log.e(TAG, "Error resuming update flow: ${e.message}")
                }
            } else {
                onNoUpdate()
            }
        }.addOnFailureListener { e ->
            Log.w(TAG, "Play Store in-app update check failed or not installed via Play Store: ${e.message}")
            onNoUpdate()
        }
    }

    /**
     * Start Google Play Immediate Update Flow
     */
    fun startImmediateUpdate(activity: Activity, appUpdateInfo: AppUpdateInfo) {
        try {
            appUpdateManager.startUpdateFlowForResult(
                appUpdateInfo,
                activity,
                AppUpdateOptions.newBuilder(AppUpdateType.IMMEDIATE).build(),
                UPDATE_REQUEST_CODE
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start immediate update flow: ${e.message}")
            openPlayStorePage(activity)
        }
    }

    /**
     * Check if a mandatory update is required via version comparison.
     */
    suspend fun checkVersionPolicy(remoteConfigUrl: String? = null): AppUpdateState {
        return withContext(Dispatchers.IO) {
            val currentCode = BuildConfig.VERSION_CODE
            val currentName = BuildConfig.VERSION_NAME

            var latestCode = prefs.getInt(PREF_LATEST_VERSION_CODE, currentCode)
            var latestName = prefs.getString(PREF_LATEST_VERSION_NAME, currentName) ?: currentName
            var isForced = prefs.getBoolean(PREF_IS_FORCED, true)
            var notes = prefs.getString(PREF_RELEASE_NOTES, "") ?: ""

            // If a remote URL is provided, query it for real-time version info
            if (!remoteConfigUrl.isNullOrBlank()) {
                try {
                    val url = URL(remoteConfigUrl)
                    val conn = url.openConnection() as HttpURLConnection
                    conn.connectTimeout = 5000
                    conn.readTimeout = 5000
                    conn.requestMethod = "GET"

                    if (conn.responseCode == 200) {
                        val response = conn.inputStream.bufferedReader().use { it.readText() }
                        val json = JSONObject(response)
                        latestCode = json.optInt("latest_version_code", currentCode)
                        latestName = json.optString("latest_version_name", currentName)
                        isForced = json.optBoolean("is_forced_update", true)
                        notes = json.optString("release_notes", "")

                        // Cache in prefs
                        prefs.edit()
                            .putInt(PREF_LATEST_VERSION_CODE, latestCode)
                            .putString(PREF_LATEST_VERSION_NAME, latestName)
                            .putBoolean(PREF_IS_FORCED, isForced)
                            .putString(PREF_RELEASE_NOTES, notes)
                            .apply()
                    }
                    conn.disconnect()
                } catch (e: Exception) {
                    Log.w(TAG, "Remote version check error: ${e.message}")
                }
            }

            val isUpdateAvailable = latestCode > currentCode
            AppUpdateState(
                isUpdateAvailable = isUpdateAvailable,
                isForced = isForced,
                currentVersionCode = currentCode,
                currentVersionName = currentName,
                latestVersionCode = latestCode,
                latestVersionName = latestName,
                releaseNotes = notes,
                playStoreUrl = "https://play.google.com/store/apps/details?id=$PLAY_STORE_PACKAGE_NAME"
            )
        }
    }

    /**
     * Opens Google Play Store app page or web browser directly
     */
    fun openPlayStorePage(ctx: Context) {
        val packageName = PLAY_STORE_PACKAGE_NAME
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$packageName")).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            ctx.startActivity(intent)
        } catch (e: Exception) {
            val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=$packageName")).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            ctx.startActivity(webIntent)
        }
    }

    /**
     * Helper to set simulated or new remote minimum version (useful for testing or remote push)
     */
    fun setRemoteVersionRequirement(targetVersionCode: Int, targetVersionName: String, isForced: Boolean = true, notes: String = "") {
        prefs.edit()
            .putInt(PREF_LATEST_VERSION_CODE, targetVersionCode)
            .putString(PREF_LATEST_VERSION_NAME, targetVersionName)
            .putBoolean(PREF_IS_FORCED, isForced)
            .putString(PREF_RELEASE_NOTES, notes)
            .apply()
    }
}
