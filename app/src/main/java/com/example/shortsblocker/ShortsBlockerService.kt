package com.example.shortsblocker

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.content.SharedPreferences
import android.os.SystemClock
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

/**
 * Watches YouTube, Facebook, and Instagram for Shorts / Reels screens
 * and automatically triggers GLOBAL_ACTION_BACK when detected.
 */
class ShortsBlockerService : AccessibilityService() {

    private lateinit var prefs: SharedPreferences
    private var lastBackActionTimestamp: Long = 0L

    private val shortsKeywords = listOf(
        "shorts",
        "reel_recycler",
        "reel_player",
        "reels_viewer",
        "reels_tray",
        "clips_viewer",
        "reel_watch",
        "shorts_player",
        "shorts_container",
        "reel_video",
        "reels_tab",
        "reel_viewer"
    )

    override fun onServiceConnected() {
        super.onServiceConnected()
        prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return

        if (!::prefs.isInitialized) {
            prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        }

        val isMasterEnabled = prefs.getBoolean(PREF_ENABLED, true)
        if (!isMasterEnabled) return

        val packageName = event.packageName?.toString() ?: return

        // Platform-specific toggle checks
        val shouldCheck = when {
            packageName.contains("youtube") -> prefs.getBoolean(PREF_BLOCK_YOUTUBE, true)
            packageName.contains("facebook") -> prefs.getBoolean(PREF_BLOCK_FACEBOOK, true)
            packageName.contains("instagram") -> prefs.getBoolean(PREF_BLOCK_INSTAGRAM, true)
            else -> false
        }

        if (!shouldCheck) return

        // Throttle back action to prevent infinite back-loops
        val now = SystemClock.elapsedRealtime()
        if (now - lastBackActionTimestamp < THROTTLE_INTERVAL_MS) {
            return
        }

        val root: AccessibilityNodeInfo = rootInActiveWindow ?: return
        if (containsShorts(root)) {
            val appLabel = when {
                packageName.contains("youtube") -> "YouTube Shorts"
                packageName.contains("facebook") -> "Facebook Reels"
                packageName.contains("instagram") -> "Instagram Reels"
                else -> "Short Video"
            }

            lastBackActionTimestamp = now
            recordBlockEvent(appLabel, packageName)
            performGlobalAction(GLOBAL_ACTION_BACK)
        }
    }

    private fun containsShorts(node: AccessibilityNodeInfo, depth: Int = 0): Boolean {
        // Limit recursion depth to prevent any ANR or excessive UI traversal
        if (depth > 40) return false

        val viewId = node.viewIdResourceName?.lowercase() ?: ""
        val desc = node.contentDescription?.toString()?.lowercase() ?: ""
        val text = node.text?.toString()?.lowercase() ?: ""

        if (shortsKeywords.any { viewId.contains(it) || desc.contains(it) || (text.contains(it) && text.length < 30) }) {
            return true
        }

        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            if (containsShorts(child, depth + 1)) return true
        }
        return false
    }

    private fun recordBlockEvent(appName: String, packageName: String) {
        val currentTotal = prefs.getInt(PREF_TOTAL_BLOCKED, 0)
        val platformKey = when {
            packageName.contains("youtube") -> PREF_YOUTUBE_BLOCKED
            packageName.contains("facebook") -> PREF_FACEBOOK_BLOCKED
            packageName.contains("instagram") -> PREF_INSTAGRAM_BLOCKED
            else -> PREF_YOUTUBE_BLOCKED
        }
        val currentPlatform = prefs.getInt(platformKey, 0)

        val timestamp = System.currentTimeMillis()
        val currentLog = prefs.getString(PREF_RECENT_LOGS, "") ?: ""
        val newEntry = "$appName|$packageName|$timestamp"
        val updatedLogs = (listOf(newEntry) + currentLog.split(";").filter { it.isNotBlank() })
            .take(15)
            .joinToString(";")

        prefs.edit()
            .putInt(PREF_TOTAL_BLOCKED, currentTotal + 1)
            .putInt(platformKey, currentPlatform + 1)
            .putLong(PREF_LAST_BLOCKED_TIME, timestamp)
            .putString(PREF_LAST_BLOCKED_APP, appName)
            .putString(PREF_RECENT_LOGS, updatedLogs)
            .apply()
    }

    override fun onInterrupt() {
        // Accessibility cleanup if necessary
    }

    companion object {
        const val PREFS_NAME = "shorts_blocker_prefs"
        const val PREF_ENABLED = "is_enabled"
        const val PREF_BLOCK_YOUTUBE = "block_youtube"
        const val PREF_BLOCK_FACEBOOK = "block_facebook"
        const val PREF_BLOCK_INSTAGRAM = "block_instagram"

        const val PREF_TOTAL_BLOCKED = "total_blocked_count"
        const val PREF_YOUTUBE_BLOCKED = "youtube_blocked_count"
        const val PREF_FACEBOOK_BLOCKED = "facebook_blocked_count"
        const val PREF_INSTAGRAM_BLOCKED = "instagram_blocked_count"
        const val PREF_LAST_BLOCKED_TIME = "last_blocked_time"
        const val PREF_LAST_BLOCKED_APP = "last_blocked_app"
        const val PREF_RECENT_LOGS = "recent_logs"

        private const val THROTTLE_INTERVAL_MS = 800L
    }
}
