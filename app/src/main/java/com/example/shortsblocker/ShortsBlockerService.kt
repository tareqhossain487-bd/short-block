package com.example.shortsblocker

import android.accessibilityservice.AccessibilityService
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.Toast
import androidx.core.app.NotificationCompat
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
    private var lastDateCheckTime: Long = 0L
    private var lastBrowserCheckTimestamp: Long = 0L
    private var lastAdSkipCheckTime: Long = 0L
    private var lastFlushTime: Long = 0L

    // In-memory usage buffers to eliminate continuous SharedPreferences disk I/O
    private val bufferedAppSeconds = mutableMapOf<String, Long>()
    private val bufferedShortsSeconds = mutableMapOf<String, Long>()

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

    // Intrusive Ad Networks, Popups and Malicious Trackers
    private val defaultAdNetworks = listOf(
        "doubleclick.net", "googleadservices", "pagead2.googlesyndication",
        "popads.net", "propellerads", "adsterra", "exoclick", "trafficjunky",
        "outbrain.com", "taboola.com", "mgid.com", "adnxs.com", "criteo.com",
        "adroll.com", "clickadu", "richpush", "onclickads", "bet365", "1xbet",
        "melbet", "mostbet", "adcolony", "applovin", "unityads", "ironsrc"
    )

    // Skip Ad button identifiers and texts across apps
    private val skipAdButtonKeywords = listOf(
        "skip_ad", "ad_skip", "skip_button", "skipbutton", "btn_skip",
        "ytp-ad-skip-button", "action_skip", "skip_ad_container",
        "skip ad", "skip ads", "skip", "বিজ্ঞাপন এড়িয়ে যান", "বিজ্ঞাপন এড়িয়ে যান", "স্কিপ"
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

    // Full-screen shorts/reels player identifiers and keywords
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
        "shorts_container",
        "shorts_root",
        "reel_root",
        "shorts_main_container",
        "shorts_player_surface",
        "reel_player_overlay",
        "reel_player_video_link",
        "reel_player_creator_avatar",
        "reel_player_like_button",
        "reel_player_dislike_button",
        "reel_player_comment_button",
        "reel_player_share_button",
        "reel_player_remix_button",
        "reel_player_sound_button",
        "shorts_sound_title",
        "shorts_pivot_button",
        "shorts_camera_button",
        "shorts_remix_button",
        "reel_item_player",
        "reel_watch_view",
        "reelwatchactivity",
        "shortsactivity"
    )

    private val youtubeShortsTextKeywords = listOf(
        "dislike this short",
        "like this short",
        "remix this short",
        "sound used in this short",
        "remix with this sound",
        "use this sound",
        "original sound - "
    )

    private val facebookReelsPlayerIndicators = listOf(
        "fb_shorts_viewer_fragment",
        "reels_viewer_fragment",
        "fb_shorts_full_screen",
        "reels_video_view_container",
        "reel_viewer_activity",
        "reel_viewer_page",
        "full_screen_video_player_reels",
        "fb_shorts",
        "reelsvieweractivity",
        "fbshortsactivity"
    )

    private val instagramReelsPlayerIndicators = listOf(
        "clips_viewer_fragment",
        "clips_video_player",
        "reel_viewer_fragment",
        "clips_viewer_container",
        "instagram_reel_viewer",
        "clips_video_container",
        "clips_action_bar",
        "clips_author_container",
        "clips_camera",
        "clipsvieweractivity"
    )

    // Dynamic foreground ticker to ensure time tracking always happens
    // while running at low power when idle or screen is off
    private val tickerRunnable = object : Runnable {
        override fun run() {
            var nextDelay = 1000L
            try {
                nextDelay = onTickerTick()
            } catch (_: Exception) {
            }
            mainHandler.postDelayed(this, nextDelay)
        }
    }

    override fun onCreate() {
        super.onCreate()
        prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        startForegroundNotification()
        checkAndResetDailyUsage(force = true)
        ensureTickerRunning()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (!::prefs.isInitialized) {
            prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        }
        startForegroundNotification()
        checkAndResetDailyUsage()
        ensureTickerRunning()
        return START_STICKY
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        flushPendingUsageToPrefs()
        startForegroundNotification()
        ensureTickerRunning()

        // Resurrect service if process is terminated by OS task cleanup
        try {
            val restartServiceIntent = Intent(applicationContext, ShortsBlockerService::class.java)
            val restartPendingIntent = PendingIntent.getService(
                applicationContext,
                1,
                restartServiceIntent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_ONE_SHOT
            )
            val alarmManager = getSystemService(Context.ALARM_SERVICE) as? AlarmManager
            alarmManager?.set(
                AlarmManager.ELAPSED_REALTIME,
                SystemClock.elapsedRealtime() + 1000L,
                restartPendingIntent
            )
        } catch (_: Exception) {}
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        startForegroundNotification()
        checkAndResetDailyUsage(force = true)
        ensureTickerRunning()
    }

    override fun onInterrupt() {
        flushPendingUsageToPrefs()
    }

    override fun onDestroy() {
        super.onDestroy()
        flushPendingUsageToPrefs()
        mainHandler.removeCallbacks(tickerRunnable)
    }

    private fun startForegroundNotification() {
        try {
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && notificationManager != null) {
                val channel = NotificationChannel(
                    NOTIFICATION_CHANNEL_ID,
                    "Shorts Blocker Service",
                    NotificationManager.IMPORTANCE_LOW
                ).apply {
                    description = "Shorts Blocker background protection"
                    setShowBadge(false)
                }
                notificationManager.createNotificationChannel(channel)
            }

            val launchIntent = Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val pendingIntent = PendingIntent.getActivity(
                this,
                0,
                launchIntent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )

            val notification = NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle("🛡️ Shorts Blocker সক্রিয় রয়েছে")
                .setContentText("ব্যাকগ্রাউন্ডে লিমিট ও শর্টস ট্র্যাকিং চলছে")
                .setOngoing(true)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setContentIntent(pendingIntent)
                .build()

            if (Build.VERSION.SDK_INT >= 34) {
                startForeground(
                    NOTIFICATION_ID,
                    notification,
                    android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
                )
            } else {
                startForeground(NOTIFICATION_ID, notification)
            }
        } catch (_: Exception) {}
    }

    private fun ensureTickerRunning() {
        mainHandler.removeCallbacks(tickerRunnable)
        mainHandler.post(tickerRunnable)
    }

    private fun flushPendingUsageToPrefs() {
        if (!::prefs.isInitialized) return
        if (bufferedAppSeconds.isEmpty() && bufferedShortsSeconds.isEmpty()) return
        val editor = prefs.edit()
        var hasChanges = false
        for ((key, added) in bufferedAppSeconds) {
            if (added > 0) {
                val current = prefs.getLong("app_used_sec_$key", 0L)
                editor.putLong("app_used_sec_$key", current + added)
                hasChanges = true
            }
        }
        for ((key, added) in bufferedShortsSeconds) {
            if (added > 0) {
                val current = prefs.getLong("shorts_used_sec_$key", 0L)
                editor.putLong("shorts_used_sec_$key", current + added)
                hasChanges = true
            }
        }
        bufferedAppSeconds.clear()
        bufferedShortsSeconds.clear()
        if (hasChanges) {
            editor.apply()
        }
    }

    private fun getEffectiveAppUsedSeconds(appKey: String): Long {
        val saved = prefs.getLong("app_used_sec_$appKey", 0L)
        val buffered = bufferedAppSeconds[appKey] ?: 0L
        return saved + buffered
    }

    private fun getEffectiveShortsUsedSeconds(appKey: String): Long {
        val saved = prefs.getLong("shorts_used_sec_$appKey", 0L)
        val buffered = bufferedShortsSeconds[appKey] ?: 0L
        return saved + buffered
    }

    private fun getTrackedAppInfo(pkg: String): TrackedAppInfo? {
        if (pkg.isBlank()) return null
        if (!::prefs.isInitialized) {
            prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        }

        val isMasterEnabled = prefs.getBoolean(PREF_ENABLED, true)
        if (!isMasterEnabled) return null

        return when {
            pkg.contains("youtube") && (prefs.getBoolean(PREF_BLOCK_YOUTUBE, true) || prefs.getInt("app_limit_youtube", 0) > 0 || prefs.getInt("shorts_limit_youtube", 0) >= 0) -> {
                TrackedAppInfo(appKey = "youtube", appLabel = "YouTube", packageName = pkg, isCustomApp = false)
            }
            (pkg.contains("facebook.katana") || pkg.contains("facebook.lite")) &&
                    (prefs.getBoolean(PREF_BLOCK_FACEBOOK, true) || prefs.getInt("app_limit_facebook", 0) > 0 || prefs.getInt("shorts_limit_facebook", 0) >= 0) -> {
                TrackedAppInfo(appKey = "facebook", appLabel = "Facebook", packageName = pkg, isCustomApp = false)
            }
            pkg.contains("instagram") && (prefs.getBoolean(PREF_BLOCK_INSTAGRAM, true) || prefs.getInt("app_limit_instagram", 0) > 0 || prefs.getInt("shorts_limit_instagram", 0) >= 0) -> {
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
     * Dynamic ticker: 1000ms when inside tracked app, 2500-3000ms when idle or screen off.
     * Buffers disk I/O and only inspects nodes when necessary.
     */
    private fun onTickerTick(): Long {
        if (!::prefs.isInitialized) {
            prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        }

        checkAndResetDailyUsage()

        val isMasterEnabled = prefs.getBoolean(PREF_ENABLED, true)
        if (!isMasterEnabled) {
            flushPendingUsageToPrefs()
            return 3000L
        }

        val powerManager = getSystemService(Context.POWER_SERVICE) as? android.os.PowerManager
        if (powerManager?.isInteractive == false) {
            flushPendingUsageToPrefs()
            return 3000L
        }

        var currentPkg = activeForegroundPackage
        if (currentPkg.isBlank() || isIgnoredSystemPackage(currentPkg)) {
            val root = try { rootInActiveWindow } catch (_: Exception) { null }
            val rootPkg = root?.packageName?.toString() ?: ""
            if (rootPkg.isNotBlank() && !isIgnoredSystemPackage(rootPkg)) {
                activeForegroundPackage = rootPkg
                currentPkg = rootPkg
            }
        }

        if (currentPkg.isBlank() || isIgnoredSystemPackage(currentPkg)) {
            flushPendingUsageToPrefs()
            return 2000L
        }

        val trackedApp = getTrackedAppInfo(currentPkg)
        if (trackedApp == null) {
            flushPendingUsageToPrefs()
            return 2000L
        }

        val appKey = trackedApp.appKey
        val appLabel = trackedApp.appLabel
        val now = SystemClock.elapsedRealtime()

        // 1. Increment this app's daily used seconds in memory buffer
        bufferedAppSeconds[appKey] = (bufferedAppSeconds[appKey] ?: 0L) + 1L
        val currentAppUsed = getEffectiveAppUsedSeconds(appKey)

        val appPrefLimitKey = "app_limit_$appKey"
        val appLimitMinutes = prefs.getInt(appPrefLimitKey, 0)

        // 2. Check if App Limit is exceeded
        if (appLimitMinutes > 0 && currentAppUsed >= (appLimitMinutes * 60L)) {
            flushPendingUsageToPrefs()
            performGlobalAction(GLOBAL_ACTION_HOME)
            performGlobalAction(GLOBAL_ACTION_BACK)
            activeForegroundPackage = ""
            if (now - lastHomeActionTimestamp >= 2000L) {
                lastHomeActionTimestamp = now
                showAppLimitToast(appLabel, appLimitMinutes)
                recordBlockEvent("$appLabel (App Limit: ${appLimitMinutes}m)", currentPkg)
            }
            return 2500L
        }

        // 3. If it's YouTube / Facebook / Instagram, track Shorts / Reels time
        // Only inspect if Shorts limit is not set to unlimited (-1)
        val shortsPrefLimitKey = "shorts_limit_$appKey"
        val shortsLimitMinutes = prefs.getInt(shortsPrefLimitKey, 0) // -1: Unlimited, 0: Block (Off), >0: Mins

        if (!trackedApp.isCustomApp && shortsLimitMinutes != -1) {
            val root = try { rootInActiveWindow } catch (_: Exception) { null }
            if (root != null) {
                val isShortsOpen = when (appKey) {
                    "youtube" -> isYouTubeShortsPlayerActive(root)
                    "facebook" -> isFacebookReelsPlayerActive(root)
                    "instagram" -> isInstagramReelsPlayerActive(root)
                    else -> false
                }

                if (isShortsOpen) {
                    bufferedShortsSeconds[appKey] = (bufferedShortsSeconds[appKey] ?: 0L) + 1L
                    val currentShortsUsed = getEffectiveShortsUsedSeconds(appKey)

                    if (shortsLimitMinutes == 0 || currentShortsUsed >= (shortsLimitMinutes * 60L)) {
                        flushPendingUsageToPrefs()
                        performGlobalAction(GLOBAL_ACTION_BACK)
                        if (now - lastBackActionTimestamp >= THROTTLE_INTERVAL_MS) {
                            lastBackActionTimestamp = now
                            val shortLabel = when (appKey) {
                                "youtube" -> "YouTube Shorts"
                                "facebook" -> "Facebook Reels"
                                "instagram" -> "Instagram Reels"
                                else -> appLabel
                            }
                            showShortsLimitToast(shortLabel, shortsLimitMinutes)
                            recordBlockEvent(shortLabel, currentPkg)
                        }
                        // Fail-safe: if still on Shorts after pressing BACK, kick to Home
                        mainHandler.postDelayed({
                            try {
                                val currentRoot = rootInActiveWindow
                                if (currentRoot != null && isYouTubeShortsPlayerActive(currentRoot)) {
                                    performGlobalAction(GLOBAL_ACTION_HOME)
                                }
                            } catch (_: Exception) {}
                        }, 250L)
                    }
                }
            }
        }

        // Periodically flush buffer to disk every 10 seconds to save battery
        if (now - lastFlushTime >= 10_000L) {
            lastFlushTime = now
            flushPendingUsageToPrefs()
        }

        return 1000L
    }

    private fun isIgnoredSystemPackage(pkg: String): Boolean {
        val lower = pkg.lowercase()
        return lower.contains("inputmethod") ||
                lower.contains("keyboard") ||
                lower.contains("latin") ||
                lower.contains("toast") ||
                lower.contains("volume")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
        if (!::prefs.isInitialized) {
            prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        }

        val isMasterEnabled = prefs.getBoolean(PREF_ENABLED, true)
        if (!isMasterEnabled) return

        val packageName = event.packageName?.toString() ?: return
        if (packageName.isBlank() || isIgnoredSystemPackage(packageName)) return

        val eventType = event.eventType
        val isWindowStateChange = (eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED)

        if (packageName != activeForegroundPackage) {
            flushPendingUsageToPrefs()
            activeForegroundPackage = packageName
            lastPackageEventTime = SystemClock.elapsedRealtime()
            if (getTrackedAppInfo(packageName) != null) {
                ensureTickerRunning()
            }
        }

        // Fast rejection: If not browser and not tracked app, ignore immediately! Zero tree inspection.
        val isBrowser = browserPackages.any { packageName.contains(it, ignoreCase = true) }
        val trackedApp = getTrackedAppInfo(packageName)
        if (!isBrowser && trackedApp == null) {
            return
        }

        checkAndResetDailyUsage()

        // 1. Check if event is in a Web Browser for Adult/Porn Sites or Custom Blocked Domains or Ads
        if (isBrowser) {
            val now = SystemClock.elapsedRealtime()
            if (isWindowStateChange || now - lastBrowserCheckTimestamp >= 800L) {
                lastBrowserCheckTimestamp = now
                handleBrowserEvent(packageName)
            }
            return
        }

        // 2. Try auto-skipping video ads if enabled (ONLY for YouTube and Facebook, throttled to 2 seconds)
        if (trackedApp != null && (packageName.contains("youtube") || packageName.contains("facebook"))) {
            val autoSkip = prefs.getBoolean(PREF_AUTO_SKIP_VIDEO_ADS, true)
            if (autoSkip) {
                val now = SystemClock.elapsedRealtime()
                if (now - lastAdSkipCheckTime >= 2000L) {
                    lastAdSkipCheckTime = now
                    val root: AccessibilityNodeInfo? = event.source ?: try { rootInActiveWindow } catch (_: Exception) { null }
                    if (root != null) {
                        tryAutoSkipAds(root, packageName)
                    }
                }
            }
        }

        // 3. Check for Tracked Apps
        if (trackedApp != null) {
            val appKey = trackedApp.appKey
            val appLabel = trackedApp.appLabel
            val isCustomApp = trackedApp.isCustomApp

            val now = SystemClock.elapsedRealtime()

            // Immediate check: If this app's App Limit is already exceeded, kick user out to Home
            val appLimitMinutes = prefs.getInt("app_limit_$appKey", 0)
            val todayAppUsedSeconds = getEffectiveAppUsedSeconds(appKey)
            if (appLimitMinutes > 0 && todayAppUsedSeconds >= (appLimitMinutes * 60L)) {
                flushPendingUsageToPrefs()
                performGlobalAction(GLOBAL_ACTION_HOME)
                performGlobalAction(GLOBAL_ACTION_BACK)
                activeForegroundPackage = ""
                if (now - lastHomeActionTimestamp >= 2000L) {
                    lastHomeActionTimestamp = now
                    showAppLimitToast(appLabel, appLimitMinutes)
                    recordBlockEvent("$appLabel (App Limit: ${appLimitMinutes}m)", packageName)
                }
                return
            }

            val shortsPrefLimitKey = "shorts_limit_$appKey"
            val shortsLimitMinutes = prefs.getInt(shortsPrefLimitKey, 0)

            // Only check shorts if shorts limit is not unlimited (-1)
            if (shortsLimitMinutes != -1) {
                val eventClassName = event.className?.toString() ?: ""
                val root: AccessibilityNodeInfo? = event.source ?: try { rootInActiveWindow } catch (_: Exception) { null }

                val isShortsOpen = when {
                    isCustomApp -> true
                    root != null && packageName.contains("youtube") -> isYouTubeShortsPlayerActive(root, eventClassName)
                    root != null && (packageName.contains("facebook.katana") || packageName.contains("facebook.lite")) -> isFacebookReelsPlayerActive(root, eventClassName)
                    root != null && packageName.contains("instagram") -> isInstagramReelsPlayerActive(root, eventClassName)
                    packageName.contains("youtube") && (eventClassName.contains("ReelWatchActivity", ignoreCase = true) || eventClassName.contains("ShortsActivity", ignoreCase = true)) -> true
                    packageName.contains("instagram") && eventClassName.contains("ClipsViewerActivity", ignoreCase = true) -> true
                    (packageName.contains("facebook.katana") || packageName.contains("facebook.lite")) && eventClassName.contains("ReelsViewerActivity", ignoreCase = true) -> true
                    else -> false
                }

                if (isShortsOpen) {
                    val todayShortsUsedSeconds = getEffectiveShortsUsedSeconds(appKey)

                    // -1 = Unlimited (no block), 0 = Block, > 0 = time limit
                    if (shortsLimitMinutes == 0 || todayShortsUsedSeconds >= (shortsLimitMinutes * 60L)) {
                        flushPendingUsageToPrefs()
                        performGlobalAction(GLOBAL_ACTION_BACK)
                        if (now - lastBackActionTimestamp >= THROTTLE_INTERVAL_MS) {
                            lastBackActionTimestamp = now
                            val shortLabel = when (appKey) {
                                "youtube" -> "YouTube Shorts"
                                "facebook" -> "Facebook Reels"
                                "instagram" -> "Instagram Reels"
                                else -> appLabel
                            }
                            showShortsLimitToast(shortLabel, shortsLimitMinutes)
                            recordBlockEvent(shortLabel, packageName)
                        }

                        // Fail-safe: if still on Shorts after pressing BACK, kick to Home
                        mainHandler.postDelayed({
                            try {
                                val currentRoot = rootInActiveWindow
                                if (currentRoot != null && isYouTubeShortsPlayerActive(currentRoot)) {
                                    performGlobalAction(GLOBAL_ACTION_HOME)
                                }
                            } catch (_: Exception) {}
                        }, 250L)
                    }
                }
            }
        }
    }

    private fun handleBrowserEvent(packageName: String) {
        val blockAdult = prefs.getBoolean(PREF_BLOCK_ADULT_WEBSITES, true)
        val blockAds = prefs.getBoolean(PREF_BLOCK_ADS, true)
        
        val customSitesStr = prefs.getString(PREF_CUSTOM_WEBSITES, "") ?: ""
        val customSites = customSitesStr.split(";")
            .filter { it.isNotBlank() }
            .mapNotNull {
                val parts = it.split("#")
                if (parts.size >= 2 && parts[1].toBoolean()) parts[0].lowercase().trim() else null
            }

        val customAdFiltersStr = prefs.getString(PREF_CUSTOM_AD_FILTERS, "") ?: ""
        val customAdFilters = customAdFiltersStr.split(";")
            .filter { it.isNotBlank() }
            .mapNotNull {
                val parts = it.split("#")
                if (parts.size >= 2 && parts[1].toBoolean()) parts[0].lowercase().trim() else null
            }

        val root = rootInActiveWindow ?: return
        val urlOrContent = findBrowserUrlOrKeywords(root) ?: return

        val containsAdult = blockAdult && adultKeywords.any { urlOrContent.contains(it) }
        val containsCustomSite = customSites.any { it.isNotEmpty() && urlOrContent.contains(it) }
        val containsAdNetwork = blockAds && (defaultAdNetworks.any { urlOrContent.contains(it) } || customAdFilters.any { it.isNotEmpty() && urlOrContent.contains(it) })

        if (containsAdult || containsCustomSite || containsAdNetwork) {
            val now = SystemClock.elapsedRealtime()
            if (now - lastBackActionTimestamp >= THROTTLE_INTERVAL_MS) {
                lastBackActionTimestamp = now
                val label = when {
                    containsAdult -> "Adult Website Blocked"
                    containsAdNetwork -> "Ad Popup / Network Blocked"
                    else -> "Custom Website Blocked"
                }
                recordBlockEvent(label, packageName)
                if (containsAdult || containsCustomSite) {
                    showReminderToast()
                }
                performGlobalAction(GLOBAL_ACTION_BACK)
            }
        }
    }

    private fun tryAutoSkipAds(root: AccessibilityNodeInfo, packageName: String) {
        val autoSkip = prefs.getBoolean(PREF_AUTO_SKIP_VIDEO_ADS, true)
        if (!autoSkip) return

        val skipButton = findClickableNodeMatchingKeywords(root, skipAdButtonKeywords) ?: return
        val clicked = skipButton.performAction(AccessibilityNodeInfo.ACTION_CLICK)
        if (clicked) {
            val now = SystemClock.elapsedRealtime()
            if (now - lastToastTimestamp >= 3000L) {
                lastToastTimestamp = now
                recordAdBlockEvent("Video Ad Skipped", packageName)
            }
        }
    }

    private fun findClickableNodeMatchingKeywords(
        node: AccessibilityNodeInfo,
        keywords: List<String>,
        depth: Int = 0
    ): AccessibilityNodeInfo? {
        if (depth > 14) return null

        val viewId = node.viewIdResourceName?.lowercase() ?: ""
        val text = node.text?.toString()?.lowercase() ?: ""
        val desc = node.contentDescription?.toString()?.lowercase() ?: ""

        val matches = keywords.any {
            viewId.contains(it) || (text.isNotBlank() && text.contains(it)) || (desc.isNotBlank() && desc.contains(it))
        }

        if (matches) {
            if (node.isClickable) return node
            val parent = node.parent
            if (parent != null && parent.isClickable) return parent
        }

        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            val found = findClickableNodeMatchingKeywords(child, keywords, depth + 1)
            if (found != null) return found
        }
        return null
    }

    private fun findBrowserUrlOrKeywords(node: AccessibilityNodeInfo, depth: Int = 0): String? {
        if (depth > 12) return null

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
        val showToast = prefs.getBoolean(PREF_SHOW_BLOCK_TOAST, false)
        if (!showToast) return

        val now = SystemClock.elapsedRealtime()
        if (now - lastToastTimestamp < 2500L) return
        lastToastTimestamp = now

        Handler(Looper.getMainLooper()).post {
            Toast.makeText(
                applicationContext,
                "⏳ $appName এর দৈনিক লিমিট ($limitMinutes মিনিট) শেষ! রাত ১২:০০ টার আগে আর খোলা যাবে না (অথবা Blocker অ্যাপে রিসেট করুন)।",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    private fun showShortsLimitToast(shortLabel: String, limitMinutes: Int) {
        val showToast = prefs.getBoolean(PREF_SHOW_BLOCK_TOAST, false)
        if (!showToast) return

        val now = SystemClock.elapsedRealtime()
        if (now - lastToastTimestamp < 2500L) return
        lastToastTimestamp = now

        val msg = if (limitMinutes == 0) {
            "🚫 $shortLabel সম্পূর্ণ ব্লক করা রয়েছে!"
        } else {
            "⏳ $shortLabel এর দৈনিক সময়সীমা ($limitMinutes মিনিট) শেষ! রাত ১২:০০ টার পর আবার দেখা যাবে।"
        }
        Handler(Looper.getMainLooper()).post {
            Toast.makeText(
                applicationContext,
                msg,
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun checkAndResetDailyUsage(force: Boolean = false) {
        val now = SystemClock.elapsedRealtime()
        if (!force && now - lastDateCheckTime < 60_000L) {
            return
        }
        lastDateCheckTime = now
        val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
        val savedDate = prefs.getString(PREF_TODAY_DATE, "") ?: ""
        if (savedDate.isBlank()) {
            // First run on device - initialize today's date without resetting existing counters
            prefs.edit().putString(PREF_TODAY_DATE, todayStr).apply()
            return
        }
        if (savedDate != todayStr) {
            flushPendingUsageToPrefs()
            // Midnight (12:00 AM) has passed! New calendar day reset
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

    private fun isYouTubeShortsPlayerActive(root: AccessibilityNodeInfo, className: String = ""): Boolean {
        if (className.contains("ReelWatchActivity", ignoreCase = true) || className.contains("ShortsActivity", ignoreCase = true)) {
            return true
        }
        return hasMatchingPlayerNode(root, youtubePlayerIndicators, youtubeShortsTextKeywords)
    }

    private fun isFacebookReelsPlayerActive(root: AccessibilityNodeInfo, className: String = ""): Boolean {
        if (className.contains("ReelsViewerActivity", ignoreCase = true) || className.contains("FbShortsViewerFragment", ignoreCase = true)) {
            return true
        }
        return hasMatchingPlayerNode(root, facebookReelsPlayerIndicators)
    }

    private fun isInstagramReelsPlayerActive(root: AccessibilityNodeInfo, className: String = ""): Boolean {
        if (className.contains("ClipsViewerActivity", ignoreCase = true) || className.contains("ClipsViewerFragment", ignoreCase = true)) {
            return true
        }
        return hasMatchingPlayerNode(root, instagramReelsPlayerIndicators)
    }

    private fun hasMatchingPlayerNode(
        node: AccessibilityNodeInfo,
        playerIndicators: List<String>,
        textKeywords: List<String> = emptyList(),
        depth: Int = 0
    ): Boolean {
        if (depth > 28) return false

        val viewId = node.viewIdResourceName?.lowercase() ?: ""
        val className = node.className?.toString()?.lowercase() ?: ""
        val contentDesc = node.contentDescription?.toString()?.lowercase() ?: ""
        val text = node.text?.toString()?.lowercase() ?: ""

        if (playerIndicators.any { viewId.contains(it) || className.contains(it) }) {
            return true
        }

        if (textKeywords.isNotEmpty() && textKeywords.any { contentDesc.contains(it) || text.contains(it) }) {
            return true
        }

        // Detect if Shorts navigation tab is selected in YouTube
        if ((contentDesc == "shorts" || text == "shorts" || contentDesc.contains("শর্টস") || text.contains("শর্টস")) &&
            (node.isSelected || className.contains("tab", ignoreCase = true) && !node.isClickable)) {
            return true
        }

        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            if (hasMatchingPlayerNode(child, playerIndicators, textKeywords, depth + 1)) {
                return true
            }
        }
        return false
    }

    private fun recordAdBlockEvent(appLabel: String, packageName: String) {
        val currentTotal = prefs.getInt(PREF_TOTAL_BLOCKED, 0) + 1
        val currentAdsBlocked = prefs.getInt(PREF_ADS_BLOCKED_COUNT, 0) + 1
        val editor = prefs.edit()
            .putInt(PREF_TOTAL_BLOCKED, currentTotal)
            .putInt(PREF_ADS_BLOCKED_COUNT, currentAdsBlocked)

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

        // Ad Blocker preferences
        const val PREF_BLOCK_ADS = "block_ads"
        const val PREF_AUTO_SKIP_VIDEO_ADS = "auto_skip_video_ads"
        const val PREF_BLOCK_POPUP_ADS = "block_popup_ads"
        const val PREF_CUSTOM_AD_FILTERS = "custom_ad_filters_list"
        const val PREF_ADS_BLOCKED_COUNT = "ads_blocked_count"

        const val PREF_TOTAL_BLOCKED = "total_blocked_count"
        const val PREF_YOUTUBE_BLOCKED = "youtube_blocked_count"
        const val PREF_FACEBOOK_BLOCKED = "facebook_blocked_count"
        const val PREF_INSTAGRAM_BLOCKED = "instagram_blocked_count"
        const val PREF_WEBSITES_BLOCKED = "websites_blocked_count"
        const val PREF_LAST_BLOCKED_TIME = "last_blocked_time"
        const val PREF_LAST_BLOCKED_APP = "last_blocked_app"
        const val PREF_RECENT_LOGS = "recent_logs"
        const val PREF_TODAY_DATE = "today_date"
        const val PREF_SHOW_BLOCK_TOAST = "show_block_toast"

        const val NOTIFICATION_ID = 1001
        const val NOTIFICATION_CHANNEL_ID = "shorts_blocker_service_channel"

        private const val THROTTLE_INTERVAL_MS = 800L
    }
}

data class TrackedAppInfo(
    val appKey: String,
    val appLabel: String,
    val packageName: String,
    val isCustomApp: Boolean
)
