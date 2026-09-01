package com.example.shortsblocker

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.content.SharedPreferences
import android.os.SystemClock
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

/**
 * Watches YouTube, Facebook, and Instagram specifically for FULL-SCREEN Shorts / Reels players
 * and automatically triggers GLOBAL_ACTION_BACK only when an active short-form player is open.
 * Home feeds, normal videos, and search results remain fully accessible.
 */
class ShortsBlockerService : AccessibilityService() {

    private lateinit var prefs: SharedPreferences
    private var lastBackActionTimestamp: Long = 0L

    // Target full-screen shorts/reels player identifiers (excluding home shelves / navigation tabs)
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

    // Elements on home feeds / tabs that should NEVER trigger a block
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

        val (shouldCheck, appLabel) = when {
            packageName.contains("youtube") && prefs.getBoolean(PREF_BLOCK_YOUTUBE, true) -> {
                true to "YouTube Shorts"
            }
            (packageName.contains("facebook.katana") || packageName.contains("facebook.lite")) &&
                    prefs.getBoolean(PREF_BLOCK_FACEBOOK, true) -> {
                true to "Facebook Reels"
            }
            packageName.contains("instagram") && prefs.getBoolean(PREF_BLOCK_INSTAGRAM, true) -> {
                true to "Instagram Reels"
            }
            else -> false to ""
        }

        if (!shouldCheck) return

        val now = SystemClock.elapsedRealtime()
        if (now - lastBackActionTimestamp < THROTTLE_INTERVAL_MS) {
            return
        }

        val root: AccessibilityNodeInfo = rootInActiveWindow ?: return

        val isShortsOpen = when {
            packageName.contains("youtube") -> isYouTubeShortsPlayerActive(root)
            packageName.contains("facebook") -> isFacebookReelsPlayerActive(root)
            packageName.contains("instagram") -> isInstagramReelsPlayerActive(root)
            else -> false
        }

        if (isShortsOpen) {
            lastBackActionTimestamp = now
            recordBlockEvent(appLabel, packageName)
            performGlobalAction(GLOBAL_ACTION_BACK)
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

        // Discard items that belong to bottom navigation or feed carousels
        if (ignoredElementKeywords.any { viewId.contains(it) || contentDesc.contains(it) }) {
            return false
        }

        // Check if this node is part of the fullscreen player
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
