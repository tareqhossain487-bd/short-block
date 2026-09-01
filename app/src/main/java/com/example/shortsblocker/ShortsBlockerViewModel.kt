package com.example.shortsblocker

import android.app.Application
import android.content.ComponentName
import android.content.Context
import android.content.SharedPreferences
import android.provider.Settings
import android.text.TextUtils
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ShortsBlockerViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs: SharedPreferences =
        application.getSharedPreferences(ShortsBlockerService.PREFS_NAME, Context.MODE_PRIVATE)

    private val _uiState = MutableStateFlow(ShortsBlockerUiState())
    val uiState: StateFlow<ShortsBlockerUiState> = _uiState.asStateFlow()

    private val prefChangeListener = SharedPreferences.OnSharedPreferenceChangeListener { _, _ ->
        loadPreferences()
    }

    init {
        prefs.registerOnSharedPreferenceChangeListener(prefChangeListener)
        loadPreferences()
        checkAccessibilityStatus()
    }

    fun refresh() {
        checkAccessibilityStatus()
        loadPreferences()
    }

    fun checkAccessibilityStatus() {
        val isActive = isAccessibilityServiceEnabled(
            getApplication(),
            ShortsBlockerService::class.java
        )
        _uiState.update { it.copy(isAccessibilityServiceActive = isActive) }
    }

    private fun loadPreferences() {
        val masterEnabled = prefs.getBoolean(ShortsBlockerService.PREF_ENABLED, true)
        val blockYt = prefs.getBoolean(ShortsBlockerService.PREF_BLOCK_YOUTUBE, true)
        val blockFb = prefs.getBoolean(ShortsBlockerService.PREF_BLOCK_FACEBOOK, true)
        val blockIg = prefs.getBoolean(ShortsBlockerService.PREF_BLOCK_INSTAGRAM, true)

        val total = prefs.getInt(ShortsBlockerService.PREF_TOTAL_BLOCKED, 0)
        val ytCount = prefs.getInt(ShortsBlockerService.PREF_YOUTUBE_BLOCKED, 0)
        val fbCount = prefs.getInt(ShortsBlockerService.PREF_FACEBOOK_BLOCKED, 0)
        val igCount = prefs.getInt(ShortsBlockerService.PREF_INSTAGRAM_BLOCKED, 0)
        val lastTime = prefs.getLong(ShortsBlockerService.PREF_LAST_BLOCKED_TIME, 0L)
        val lastApp = prefs.getString(ShortsBlockerService.PREF_LAST_BLOCKED_APP, "") ?: ""

        val rawLogs = prefs.getString(ShortsBlockerService.PREF_RECENT_LOGS, "") ?: ""
        val events = rawLogs.split(";")
            .filter { it.isNotBlank() }
            .mapNotNull { entry ->
                val parts = entry.split("|")
                if (parts.size >= 3) {
                    BlockEvent(
                        appName = parts[0],
                        packageName = parts[1],
                        timestamp = parts[2].toLongOrNull() ?: System.currentTimeMillis()
                    )
                } else null
            }

        _uiState.update {
            it.copy(
                isMasterEnabled = masterEnabled,
                blockYouTube = blockYt,
                blockFacebook = blockFb,
                blockInstagram = blockIg,
                totalBlockedCount = total,
                youtubeBlockedCount = ytCount,
                facebookBlockedCount = fbCount,
                instagramBlockedCount = igCount,
                lastBlockedTimestamp = lastTime,
                lastBlockedApp = lastApp,
                recentEvents = events
            )
        }
    }

    fun setMasterEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(ShortsBlockerService.PREF_ENABLED, enabled).apply()
        _uiState.update { it.copy(isMasterEnabled = enabled) }
    }

    fun setBlockYouTube(enabled: Boolean) {
        prefs.edit().putBoolean(ShortsBlockerService.PREF_BLOCK_YOUTUBE, enabled).apply()
        _uiState.update { it.copy(blockYouTube = enabled) }
    }

    fun setBlockFacebook(enabled: Boolean) {
        prefs.edit().putBoolean(ShortsBlockerService.PREF_BLOCK_FACEBOOK, enabled).apply()
        _uiState.update { it.copy(blockFacebook = enabled) }
    }

    fun setBlockInstagram(enabled: Boolean) {
        prefs.edit().putBoolean(ShortsBlockerService.PREF_BLOCK_INSTAGRAM, enabled).apply()
        _uiState.update { it.copy(blockInstagram = enabled) }
    }

    fun resetStats() {
        prefs.edit()
            .putInt(ShortsBlockerService.PREF_TOTAL_BLOCKED, 0)
            .putInt(ShortsBlockerService.PREF_YOUTUBE_BLOCKED, 0)
            .putInt(ShortsBlockerService.PREF_FACEBOOK_BLOCKED, 0)
            .putInt(ShortsBlockerService.PREF_INSTAGRAM_BLOCKED, 0)
            .putLong(ShortsBlockerService.PREF_LAST_BLOCKED_TIME, 0L)
            .putString(ShortsBlockerService.PREF_LAST_BLOCKED_APP, "")
            .putString(ShortsBlockerService.PREF_RECENT_LOGS, "")
            .apply()
        loadPreferences()
    }

    fun simulateTestBlock(platform: String) {
        viewModelScope.launch {
            val total = _uiState.value.totalBlockedCount + 1
            val yt = _uiState.value.youtubeBlockedCount + if (platform.contains("YouTube")) 1 else 0
            val fb = _uiState.value.facebookBlockedCount + if (platform.contains("Facebook")) 1 else 0
            val ig = _uiState.value.instagramBlockedCount + if (platform.contains("Instagram")) 1 else 0
            val timestamp = System.currentTimeMillis()
            val pkg = when {
                platform.contains("YouTube") -> "com.google.android.youtube"
                platform.contains("Facebook") -> "com.facebook.katana"
                else -> "com.instagram.android"
            }

            val currentLog = prefs.getString(ShortsBlockerService.PREF_RECENT_LOGS, "") ?: ""
            val newEntry = "$platform|$pkg|$timestamp"
            val updatedLogs = (listOf(newEntry) + currentLog.split(";").filter { it.isNotBlank() })
                .take(15)
                .joinToString(";")

            prefs.edit()
                .putInt(ShortsBlockerService.PREF_TOTAL_BLOCKED, total)
                .putInt(ShortsBlockerService.PREF_YOUTUBE_BLOCKED, yt)
                .putInt(ShortsBlockerService.PREF_FACEBOOK_BLOCKED, fb)
                .putInt(ShortsBlockerService.PREF_INSTAGRAM_BLOCKED, ig)
                .putLong(ShortsBlockerService.PREF_LAST_BLOCKED_TIME, timestamp)
                .putString(ShortsBlockerService.PREF_LAST_BLOCKED_APP, platform)
                .putString(ShortsBlockerService.PREF_RECENT_LOGS, updatedLogs)
                .apply()
            loadPreferences()
        }
    }

    override fun onCleared() {
        super.onCleared()
        prefs.unregisterOnSharedPreferenceChangeListener(prefChangeListener)
    }

    companion object {
        fun isAccessibilityServiceEnabled(
            context: Context,
            service: Class<out android.accessibilityservice.AccessibilityService>
        ): Boolean {
            val expectedServiceName = ComponentName(context, service).flattenToString()
            val expectedSimpleName = ComponentName(context, service).flattenToShortString()
            val enabledServices = Settings.Secure.getString(
                context.contentResolver,
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            ) ?: return false

            val colonSplitter = TextUtils.SimpleStringSplitter(':')
            colonSplitter.setString(enabledServices)
            while (colonSplitter.hasNext()) {
                val componentName = colonSplitter.next()
                if (componentName.equals(expectedServiceName, ignoreCase = true) ||
                    componentName.equals(expectedSimpleName, ignoreCase = true) ||
                    componentName.contains("ShortsBlockerService", ignoreCase = true)
                ) {
                    return true
                }
            }
            return false
        }
    }
}
