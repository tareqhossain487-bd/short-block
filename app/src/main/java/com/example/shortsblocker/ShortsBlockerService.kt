package com.example.shortsblocker

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.content.SharedPreferences
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.Toast
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Accessibility service that:
 * 1. Tracks exact per-second usage of monitored apps (YouTube, Facebook, Instagram, Custom apps)
 *    using a reliable 1-second ticker even when full-screen videos are playing passively.
 * 2. Immediately closes (GLOBAL_ACTION_HOME) the app when its App Limit is reached.
 * 3. Detects and blocks Shorts/Reels when the dedicated Short Limit is reached, or instantly if Block (0m/Off).
 *    If Short Limit is -1 (Unlimited), shorts are allowed without blocking.
 * 4. Detects and blocks adult/porn websites and custom added domains inside web browsers.
 */
class ShortsBlockerService : AccessibilityService() {

    private lateinit var prefs: SharedPreferences
    private val mainHandler = Handler(Looper.getMainLooper())

    private var lastBackActionTimestamp: Long = 0L
    private var lastHomeActionTimestamp: Long = 0L
    private var lastToastTimestamp: Long = 0L

    @Volatile
    private var activeForegroundPackage: String = ""
    @Volatile
    private var lastPackageEventTime: Long = 0L

    // Adult keywords and popular porn domain substrings
    private val adultKeywords = listOf(
        "porn", "xxx", "xvideos", "pornhub", "xnxx", "xhamster", "redtube",
        "youporn", "brazzers", "sex", "nude", "erotic", "nsfw", "cam4",
        "chaturbate", "onlyfans", "bangbros", "adultdvd", "eporner", "beeg",
        "hqporner", "tnaflix", "tube8", "spankwire", "daftsex", "vporn", "leakgirls"
    )

    // Browsers package names to inspect for URL / Web address bars
    private val browserPackages = listOf(
        "com.android.chrome",
        "org.mozilla.firefox",
        "com.opera.browser",
        "com.opera.mini.native",
        "com.microsoft.emmx",
        "com.brave.browser",
        "com.sec.android.app.sbrowser",
        "com.duckduckgo.mobile.android",
        "com.kiwibrowser.browser",
        "com.UCMobile.intl",
        "com.uc.browser.en",
        "com.mi.globalbrowser"
    )

    // Full-screen shorts/reels player identifiers
    private val youtubePlayerIndicators = listOf(
        "reel_watch_fragment",
        "reel_player_fragment",
        "shorts_player_fragment",
        "reel_recycler",
        "shorts_player_view",
        "reel_player_page",
        "shorts_video_surface_view",
        "reel_video_view",
        "reel_watch_container",
        "com.google.android.apps.youtube.app.extensions.reel.watch.activity.ReelWatchActivity"
    )

    private val facebookReelsPlayerIndicators = listOf(
        "fb_shorts_viewer_fragment",
        "reels_viewer_fragment",
        "fb_shorts_full_screen",
        "reels_video_view_container",
        "reel_viewer_activity",
        "reel_viewer_page",
        "full_screen_video_player_reels"
    )

    private val instagramReelsPlayerIndicators = listOf(
        "clips_viewer_fragment",
        "clips_video_player",
        "reel_viewer_fragment",
        "clips_viewer_container",
        "instagram_reel_viewer"
    )

    private val ignoredElementKeywords = listOf(
        "pivot_bar",
        "bottom_navigation",
        "tab_bar",
        "navigation_bar",
        "shelf_header",
        "reel_shelf",
        "shorts_shelf",
        "feed_unit",
        "feed_reels_card",
        "tray_header"
    )

    // 1-second continuous foreground ticker to ensure time tracking always happens
    // even during long video playback where no accessibility events are triggered
    private val tickerRunnable = object : Runnable {
        override fun run() {
            try {
                onTickerTick()
            } catch (_: Exception) {
            }
            mainHandler.postDelayed(this, 1000L)
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        checkAndResetDailyUsage()
        mainHandler.removeCallbacks(tickerRunnable)
        mainHandler.post(tickerRunnable)
    }

    override fun onInterrupt() {
        mainHandler.removeCallbacks(tickerRunnable)
    }

    override fun onDestroy() {
        super.onDestroy()
        mainHandler.removeCallbacks(tickerRunnable)
    }

    private fun getTrackedAppInfo(pkg: String): TrackedAppInfo? {
        if (pkg.isBlank()) return null
        if (!::prefs.isInitialized) {
            prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        }

        val isMasterEnabled = prefs.getBoolean(PREF_ENABLED, true)
        if (!isMasterEnabled) return null

        return when {
            pkg.contains("youtube") && prefs.getBoolean(PREF_BLOCK_YOUTUBE, true) -> {
                TrackedAppInfo(appKey = "youtube", appLabel = "YouTube", packageName = pkg, isCustomApp = false)
            }
            (pkg.contains("facebook.katana") || pkg.contains("facebook.lite")) &&
                    prefs.getBoolean(PREF_BLOCK_FACEBOOK, true) -> {
                TrackedAppInfo(appKey = "facebook", appLabel = "Facebook", packageName = pkg, isCustomApp = false)
            }
            pkg.contains("instagram") && prefs.getBoolean(PREF_BLOCK_INSTAGRAM, true) -> {
                TrackedAppInfo(appKey = "instagram", appLabel = "Instagram", packageName = pkg, isCustomApp = false)
            }
            else -> {
                val customAppsStr = prefs.getString(PREF_CUSTOM_APPS, "") ?: ""
                val matchingCustom = customAppsStr.split(";")
                    .filter { it.isNotBlank() }
                    .mapNotNull {
                        val parts = it.split("#")
                        if (parts.size >= 3) Triple(parts[0], parts[1], parts[2].toBoolean()) else null
                    }
                    .firstOrNull { (_, customPkg, isEnabled) ->
                        isEnabled && (pkg.equals(customPkg, ignoreCase = true) || pkg.contains(customPkg, ignoreCase = true))
                    }

                if (matchingCustom != null) {
                    TrackedAppInfo(
                        appKey = matchingCustom.second,
                        appLabel = matchingCustom.first,
                        packageName = matchingCustom.second,
                        isCustomApp = true
                    )
                } else null
            }
        }
    }

    /**
     * Executes every 1 second to reliably record seconds used and enforce limits
     */
    private fun onTickerTick() {
        if (!::prefs.isInitialized) {
            prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        }

        checkAndResetDailyUsage()

        val isMasterEnabled = prefs.getBoolean(PREF_ENABLED, true)
        if (!isMasterEnabled) return

        val root = try { rootInActiveWindow } catch (_: Exception) { null }
        val rootPkg = root?.packageName?.toString() ?: ""
        val now = SystemClock.elapsedRealtime()

        val currentPkg = when {
            rootPkg.isNotBlank() -> rootPkg
            (now - lastPackageEventTime) < 4000L -> activeForegroundPackage
            else -> ""
        }

        if (currentPkg.isBlank()) return

        val trackedApp = getTrackedAppInfo(currentPkg) ?: return
        val appKey = trackedApp.appKey
        val appLabel = trackedApp.appLabel

        // 1. Increment this app's daily used seconds
        val appPrefLimitKey = "app_limit_$appKey"
        val appPrefUsedKey = "app_used_sec_$appKey"
        val appLimitMinutes = prefs.getInt(appPrefLimitKey, 0)
        val currentAppUsed = prefs.getLong(appPrefUsedKey, 0L) + 1L
        prefs.edit().putLong(appPrefUsedKey, currentAppUsed).apply()

        // 2. Check if App Limit is exceeded
        if (appLimitMinutes > 0 && currentAppUsed >= (appLimitMinutes * 60L)) {
            if (now - lastHomeActionTimestamp >= 1500L) {
                lastHomeActionTimestamp = now
                showAppLimitToast(appLabel, appLimitMinutes)
                recordBlockEvent("$appLabel (App Limit: ${appLimitMinutes}m)", currentPkg)
                // Kick out of app to Home screen immediately
                performGlobalAction(GLOBAL_ACTION_HOME)
                performGlobalAction(GLOBAL_ACTION_BACK)
            }
            return
        }

        // 3. If it's YouTube / Facebook / Instagram, track Shorts / Reels time
        if (!trackedApp.isCustomApp && root != null) {
            val isShortsOpen = when (appKey) {
                "youtube" -> isYouTubeShortsPlayerActive(root)
                "facebook" -> isFacebookReelsPlayerActive(root)
                "instagram" -> isInstagramReelsPlayerActive(root)
                else -> false
            }

            if (isShortsOpen) {
                val shortsPrefLimitKey = "shorts_limit_$appKey"
                val shortsPrefUsedKey = "shorts_used_sec_$appKey"
                val shortsLimitMinutes = prefs.getInt(shortsPrefLimitKey, 0) // -1: Unlimited, 0: Block (Off), >0: Mins

                val currentShortsUsed = prefs.getLong(shortsPrefUsedKey, 0L) + 1L
                prefs.edit().putLong(shortsPrefUsedKey, currentShortsUsed).apply()

                // If shortsLimitMinutes == -1 (Unlimited), do NOT block.
                // If shortsLimitMinutes == 0 (Block/Off), block immediately.
                // If shortsLimitMinutes > 0, block when used >= limit.
                if (shortsLimitMinutes != -1 && (shortsLimitMinutes == 0 || currentShortsUsed >= (shortsLimitMinutes * 60L))) {
                    if (now - lastBackActionTimestamp >= THROTTLE_INTERVAL_MS) {
                        lastBackActionTimestamp = now
                        val shortLabel = when (appKey) {
                            "youtube" -> "YouTube Shorts"
                            "facebook" -> "Facebook Reels"
                            "instagram" -> "Instagram Reels"
                            else -> appLabel
                        }
                        if (shortsLimitMinutes > 0) {
                            showShortsLimitToast(shortLabel, shortsLimitMinutes)
                        } else {
                            showShortsStrictBlockToast(shortLabel)
                        }
                        recordBlockEvent(shortLabel, currentPkg)
                        performGlobalAction(GLOBAL_ACTION_BACK)
                    }
                }
            }
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
        if (!::prefs.isInitialized) {
            prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        }

        val isMasterEnabled = prefs.getBoolean(PREF_ENABLED, true)
        if (!isMasterEnabled) return

        checkAndResetDailyUsage()

        val packageName = event.packageName?.toString() ?: return

        if (event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED ||
            event.eventType == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED) {
            activeForegroundPackage = packageName
            lastPackageEventTime = SystemClock.elapsedRealtime()
        }

        // 1. Check if event is in a Web Browser for Adult/Porn Sites or Custom Blocked Domains
        val isBrowser = browserPackages.any { packageName.contains(it, ignoreCase = true) }
        if (isBrowser) {
            handleBrowserEvent(packageName)
            return
        }

        // 2. Check for Tracked Apps
        val trackedApp = getTrackedAppInfo(packageName) ?: return
        val appKey = trackedApp.appKey
        val appLabel = trackedApp.appLabel
        val isCustomApp = trackedApp.isCustomApp

        val now = SystemClock.elapsedRealtime()

        // Immediate check: If this app's App Limit is already exceeded, kick user out to Home
        val appLimitMinutes = prefs.getInt("app_limit_$appKey", 0)
        val todayAppUsedSeconds = prefs.getLong("app_used_sec_$appKey", 0L)
        if (appLimitMinutes > 0 && todayAppUsedSeconds >= (appLimitMinutes * 60L)) {
            if (now - lastHomeActionTimestamp >= 1200L) {
                lastHomeActionTimestamp = now
                showAppLimitToast(appLabel, appLimitMinutes)
                recordBlockEvent("$appLabel (App Limit: ${appLimitMinutes}m)", packageName)
                performGlobalAction(GLOBAL_ACTION_HOME)
                performGlobalAction(GLOBAL_ACTION_BACK)
            }
            return
        }

        val root: AccessibilityNodeInfo = rootInActiveWindow ?: return

        val isShortsOpen = when {
            isCustomApp -> true
            packageName.contains("youtube") -> isYouTubeShortsPlayerActive(root)
            packageName.contains("facebook") -> isFacebookReelsPlayerActive(root)
            packageName.contains("instagram") -> isInstagramReelsPlayerActive(root)
            else -> false
        }

        if (isShortsOpen) {
            val shortsPrefLimitKey = "shorts_limit_$appKey"
            val shortsPrefUsedKey = "shorts_used_sec_$appKey"
            val shortsLimitMinutes = prefs.getInt(shortsPrefLimitKey, 0)
            val todayShortsUsedSeconds = prefs.getLong(shortsPrefUsedKey, 0L)

            // -1 = Unlimited (no block), 0 = Block (Off), > 0 = time limit
            if (shortsLimitMinutes != -1 && (shortsLimitMinutes == 0 || todayShortsUsedSeconds >= (shortsLimitMinutes * 60L))) {
                if (now - lastBackActionTimestamp >= THROTTLE_INTERVAL_MS) {
                    lastBackActionTimestamp = now
                    val shortLabel = when (appKey) {
                        "youtube" -> "YouTube Shorts"
                        "facebook" -> "Facebook Reels"
                        "instagram" -> "Instagram Reels"
                        else -> appLabel
                    }
                    if (shortsLimitMinutes > 0) {
                        showShortsLimitToast(shortLabel, shortsLimitMinutes)
                    } else {
                        showShortsStrictBlockToast(shortLabel)
                    }
                    recordBlockEvent(shortLabel, packageName)
                    performGlobalAction(GLOBAL_ACTION_BACK)
                }
            }
        }
    }

    private fun handleBrowserEvent(packageName: String) {
        val blockAdult = prefs.getBoolean(PREF_BLOCK_ADULT_WEBSITES, true)
        val customSitesStr = prefs.getString(PREF_CUSTOM_WEBSITES, "") ?: ""
        val customSites = customSitesStr.split(";")
            .filter { it.isNotBlank() }
            .mapNotNull {
                val parts = it.split("#")
                if (parts.size >= 2 && parts[1].toBoolean()) parts[0].lowercase().trim() else null
            }

        val root = rootInActiveWindow ?: return
        val urlOrContent = findBrowserUrlOrKeywords(root) ?: return

        val containsAdult = blockAdult && adultKeywords.any { urlOrContent.contains(it) }
        val containsCustom = customSites.any { it.isNotEmpty() && urlOrContent.contains(it) }

        if (containsAdult || containsCustom) {
            val now = SystemClock.elapsedRealtime()
            if (now - lastBackActionTimestamp >= THROTTLE_INTERVAL_MS) {
                lastBackActionTimestamp = now
                val label = if (containsAdult) "Adult Website Blocked" else "Custom Website Blocked"
                recordBlockEvent(label, packageName)
                showReminderToast()
                performGlobalAction(GLOBAL_ACTION_BACK)
            }
        }
    }

    private fun findBrowserUrlOrKeywords(node: AccessibilityNodeInfo, depth: Int = 0): String? {
        if (depth > 25) return null

        val viewId = node.viewIdResourceName?.lowercase() ?: ""
        val text = node.text?.toString()?.lowercase() ?: ""
        val desc = node.contentDescription?.toString()?.lowercase() ?: ""

        if (viewId.contains("url_bar") || viewId.contains("location_bar") || viewId.contains("search_box") ||
            viewId.contains("address") || viewId.contains("omnibox") || viewId.contains("search_src_text")
        ) {
            if (text.isNotBlank()) return text
            if (desc.isNotBlank()) return desc
        }

        // Also check if text matches adult keywords directly
        if (adultKeywords.any { text.contains(it) || desc.contains(it) }) {
            return text.ifBlank { desc }
        }

        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            val found = findBrowserUrlOrKeywords(child, depth + 1)
            if (found != null) return found
        }
        return null
    }

    private fun showReminderToast() {
        val now = SystemClock.elapsedRealtime()
        if (now - lastToastTimestamp < 2000L) return
        lastToastTimestamp = now

        val customMsg = prefs.getString(PREF_REMINDER_MESSAGE, DEFAULT_REMINDER_MESSAGE) ?: DEFAULT_REMINDER_MESSAGE
        Handler(Looper.getMainLooper()).post {
            Toast.makeText(applicationContext, "⚠️ $customMsg", Toast.LENGTH_LONG).show()
        }
    }

    private fun showAppLimitToast(appName: String, limitMinutes: Int) {
        val now = SystemClock.elapsedRealtime()
        if (now - lastToastTimestamp < 2500L) return
        lastToastTimestamp = now

        Handler(Looper.getMainLooper()).post {
            Toast.makeText(
                applicationContext,
                "⏳ $appName এর আজকের সময়সীমা ($limitMinutes মিনিট) শেষ হয়েছে! অ্যাপ বন্ধ করা হয়েছে।",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    private fun showShortsLimitToast(shortLabel: String, limitMinutes: Int) {
        val now = SystemClock.elapsedRealtime()
        if (now - lastToastTimestamp < 2500L) return
        lastToastTimestamp = now

        Handler(Looper.getMainLooper()).post {
            Toast.makeText(
                applicationContext,
                "⏳ $shortLabel এর দৈনিক সময়সীমা ($limitMinutes মিনিট) শেষ হয়েছে!",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun showShortsStrictBlockToast(shortLabel: String) {
        val now = SystemClock.elapsedRealtime()
        if (now - lastToastTimestamp < 2500L) return
        lastToastTimestamp = now

        val customMsg = prefs.getString(PREF_REMINDER_MESSAGE, DEFAULT_REMINDER_MESSAGE) ?: DEFAULT_REMINDER_MESSAGE
        Handler(Looper.getMainLooper()).post {
            Toast.makeText(
                applicationContext,
                "🚫 $shortLabel সম্পূর্ণ বন্ধ (Block / Off)! $customMsg",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun checkAndResetDailyUsage() {
        val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val savedDate = prefs.getString(PREF_TODAY_DATE, "") ?: ""
        if (savedDate != todayStr) {
            val editor = prefs.edit().putString(PREF_TODAY_DATE, todayStr)
            // Reset built-in apps usage
            val apps = listOf("youtube", "facebook", "instagram")
            apps.forEach { appKey ->
                editor.putLong("app_used_sec_$appKey", 0L)
                editor.putLong("shorts_used_sec_$appKey", 0L)
            }
            // Reset custom apps usage
            val customAppsStr = prefs.getString(PREF_CUSTOM_APPS, "") ?: ""
            customAppsStr.split(";").filter { it.isNotBlank() }.forEach {
                val parts = it.split("#")
                if (parts.size >= 2) {
                    editor.putLong("app_used_sec_${parts[1]}", 0L)
                }
            }
            editor.apply()
        }
    }

    private fun isYouTubeShortsPlayerActive(root: AccessibilityNodeInfo): Boolean {
        return hasMatchingPlayerNode(root, youtubePlayerIndicators)
    }

    private fun isFacebookReelsPlayerActive(root: AccessibilityNodeInfo): Boolean {
        return hasMatchingPlayerNode(root, facebookReelsPlayerIndicators)
    }

    private fun isInstagramReelsPlayerActive(root: AccessibilityNodeInfo): Boolean {
        return hasMatchingPlayerNode(root, instagramReelsPlayerIndicators)
    }

    private fun hasMatchingPlayerNode(
        node: AccessibilityNodeInfo,
        playerIndicators: List<String>,
        depth: Int = 0
    ): Boolean {
        if (depth > 35) return false

        val viewId = node.viewIdResourceName?.lowercase() ?: ""
        val className = node.className?.toString()?.lowercase() ?: ""
        val contentDesc = node.contentDescription?.toString()?.lowercase() ?: ""

        if (ignoredElementKeywords.any { viewId.contains(it) || contentDesc.contains(it) }) {
            return false
        }

        if (playerIndicators.any { viewId.contains(it) || className.contains(it) }) {
            return true
        }

        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            if (hasMatchingPlayerNode(child, playerIndicators, depth + 1)) {
                return true
            }
        }
        return false
    }

    private fun recordBlockEvent(appLabel: String, packageName: String) {
        val currentTotal = prefs.getInt(PREF_TOTAL_BLOCKED, 0) + 1
        val editor = prefs.edit().putInt(PREF_TOTAL_BLOCKED, currentTotal)

        when {
            packageName.contains("youtube") -> {
                editor.putInt(PREF_YOUTUBE_BLOCKED, prefs.getInt(PREF_YOUTUBE_BLOCKED, 0) + 1)
            }
            packageName.contains("facebook") -> {
                editor.putInt(PREF_FACEBOOK_BLOCKED, prefs.getInt(PREF_FACEBOOK_BLOCKED, 0) + 1)
            }
            packageName.contains("instagram") -> {
                editor.putInt(PREF_INSTAGRAM_BLOCKED, prefs.getInt(PREF_INSTAGRAM_BLOCKED, 0) + 1)
            }
            else -> {
                editor.putInt(PREF_WEBSITES_BLOCKED, prefs.getInt(PREF_WEBSITES_BLOCKED, 0) + 1)
            }
        }

        val now = System.currentTimeMillis()
        editor.putLong(PREF_LAST_BLOCKED_TIME, now)
        editor.putString(PREF_LAST_BLOCKED_APP, appLabel)

        val existingLogs = prefs.getString(PREF_RECENT_LOGS, "") ?: ""
        val newEntry = "$now,$appLabel,$packageName"
        val updatedLogs = (listOf(newEntry) + existingLogs.split(";").filter { it.isNotBlank() })
            .take(20)
            .joinToString(";")

        editor.putString(PREF_RECENT_LOGS, updatedLogs)
        editor.apply()
    }

    companion object {
        const val PREFS_NAME = "shorts_blocker_prefs"
        const val PREF_ENABLED = "master_service_enabled"
        const val PREF_BLOCK_YOUTUBE = "block_youtube"
        const val PREF_BLOCK_FACEBOOK = "block_facebook"
        const val PREF_BLOCK_INSTAGRAM = "block_instagram"
        const val PREF_CUSTOM_APPS = "custom_apps_list"

        const val PREF_BLOCK_ADULT_WEBSITES = "block_adult_websites"
        const val PREF_CUSTOM_WEBSITES = "custom_websites_list"
        const val PREF_REMINDER_MESSAGE = "block_reminder_message"
        const val DEFAULT_REMINDER_MESSAGE = "আল্লাহর দিকে ফিরে আসো"

        const val PREF_TOTAL_BLOCKED = "total_blocked_count"
        const val PREF_YOUTUBE_BLOCKED = "youtube_blocked_count"
        const val PREF_FACEBOOK_BLOCKED = "facebook_blocked_count"
        const val PREF_INSTAGRAM_BLOCKED = "instagram_blocked_count"
        const val PREF_WEBSITES_BLOCKED = "websites_blocked_count"
        const val PREF_LAST_BLOCKED_TIME = "last_blocked_time"
        const val PREF_LAST_BLOCKED_APP = "last_blocked_app"
        const val PREF_RECENT_LOGS = "recent_logs"
        const val PREF_TODAY_DATE = "today_date"

        private const val THROTTLE_INTERVAL_MS = 800L
    }
}

data class TrackedAppInfo(
    val appKey: String,
    val appLabel: String,
    val packageName: String,
    val isCustomApp: Boolean
)
