package com.example.shortsblocker

import android.accessibilityservice.AccessibilityService
import android.content.SharedPreferences
import android.preference.PreferenceManager
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

/**
 * Watches YouTube and Facebook for Shorts / Reels screens and presses "back"
 * as soon as one is detected.
 *
 * NOTE: This works by reading resource-ids / content-descriptions in the
 * screen's view tree. YouTube and Facebook change these fairly often when
 * they update their apps, so if blocking stops working after an app update,
 * the keyword lists below (SHORTS_KEYWORDS) are the first place to check —
 * you may need to add new id/description strings.
 */
class ShortsBlockerService : AccessibilityService() {

    private lateinit var prefs: SharedPreferences

    // Resource-id fragments and content-description keywords that indicate
    // a Shorts/Reels screen is currently showing.
    private val shortsKeywords = listOf(
        "shorts",
        "reel_recycler",
        "reel_player",
        "reels_viewer",
        "reels_tray"
    )

    override fun onServiceConnected() {
        super.onServiceConnected()
        prefs = PreferenceManager.getDefaultSharedPreferences(this)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return

        val isEnabled = prefs.getBoolean(PREF_ENABLED, true)
        if (!isEnabled) return

        val root: AccessibilityNodeInfo = rootInActiveWindow ?: return
        if (containsShorts(root)) {
            performGlobalAction(GLOBAL_ACTION_BACK)
        }
    }

    private fun containsShorts(node: AccessibilityNodeInfo, depth: Int = 0): Boolean {
        // Limit recursion depth so we never hang on a huge tree.
        if (depth > 40) return false

        val viewId = node.viewIdResourceName?.lowercase() ?: ""
        val desc = node.contentDescription?.toString()?.lowercase() ?: ""

        if (shortsKeywords.any { viewId.contains(it) || desc.contains(it) }) {
            return true
        }

        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            if (containsShorts(child, depth + 1)) return true
        }
        return false
    }

    override fun onInterrupt() {
        // Required override — nothing to clean up.
    }

    companion object {
        const val PREF_ENABLED = "is_enabled"
    }
}
