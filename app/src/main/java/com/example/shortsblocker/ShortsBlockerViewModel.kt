package com.example.shortsblocker

import android.app.Application
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.provider.Settings
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ShortsBlockerViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = application.getSharedPreferences(
        ShortsBlockerService.PREFS_NAME,
        Context.MODE_PRIVATE
    )

    private val _uiState = MutableStateFlow(ShortsBlockerUiState())
    val uiState: StateFlow<ShortsBlockerUiState> = _uiState.asStateFlow()

    init {
        loadStateFromPreferences()
        loadInstalledApps()
        startPeriodicSync()
    }

    fun refresh() {
        checkAccessibilityStatus()
        loadStateFromPreferences()
        loadInstalledApps()
    }

    private fun startPeriodicSync() {
        viewModelScope.launch {
            while (isActive) {
                checkAccessibilityStatus()
                syncStatistics()
                delay(1500)
            }
        }
    }

    fun checkAccessibilityStatus() {
        val isEnabled = isAccessibilityServiceEnabled(
            getApplication(),
            ShortsBlockerService::class.java
        )
        _uiState.update { it.copy(isAccessibilityServiceActive = isEnabled) }
    }

    fun loadInstalledApps() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingApps = true) }
            val appList = withContext(Dispatchers.IO) {
                val pm = getApplication<Application>().packageManager
                val packages = pm.getInstalledPackages(PackageManager.GET_META_DATA)
                val ourPkg = getApplication<Application>().packageName

                packages.mapNotNull { pkgInfo ->
                    val appInfo = pkgInfo.applicationInfo ?: return@mapNotNull null
                    val pkgName = pkgInfo.packageName
                    if (pkgName == ourPkg) return@mapNotNull null

                    val isSystem = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0
                    val appName = pm.getApplicationLabel(appInfo).toString()

                    val launchIntent = pm.getLaunchIntentForPackage(pkgName)
                    if (launchIntent != null || !isSystem) {
                        InstalledAppItem(
                            appName = appName,
                            packageName = pkgName,
                            isSystemApp = isSystem
                        )
                    } else null
                }.sortedBy { it.appName.lowercase() }
            }
            _uiState.update { it.copy(installedApps = appList, isLoadingApps = false) }
        }
    }

    private fun loadStateFromPreferences() {
        val masterEnabled = prefs.getBoolean(ShortsBlockerService.PREF_ENABLED, true)
        val blockYt = prefs.getBoolean(ShortsBlockerService.PREF_BLOCK_YOUTUBE, true)
        val blockFb = prefs.getBoolean(ShortsBlockerService.PREF_BLOCK_FACEBOOK, true)
        val blockIg = prefs.getBoolean(ShortsBlockerService.PREF_BLOCK_INSTAGRAM, true)
        val blockAdult = prefs.getBoolean(ShortsBlockerService.PREF_BLOCK_ADULT_WEBSITES, true)
        val reminderMsg = prefs.getString(
            ShortsBlockerService.PREF_REMINDER_MESSAGE,
            ShortsBlockerService.DEFAULT_REMINDER_MESSAGE
        ) ?: ShortsBlockerService.DEFAULT_REMINDER_MESSAGE

        // Per-app YouTube config
        val ytAppLimit = prefs.getInt("app_limit_youtube", 0)
        val ytAppUsed = prefs.getLong("app_used_sec_youtube", 0L)
        val ytShortsLimit = prefs.getInt("shorts_limit_youtube", 0)
        val ytShortsUsed = prefs.getLong("shorts_used_sec_youtube", 0L)

        // Per-app Facebook config
        val fbAppLimit = prefs.getInt("app_limit_facebook", 0)
        val fbAppUsed = prefs.getLong("app_used_sec_facebook", 0L)
        val fbShortsLimit = prefs.getInt("shorts_limit_facebook", 0)
        val fbShortsUsed = prefs.getLong("shorts_used_sec_facebook", 0L)

        // Per-app Instagram config
        val igAppLimit = prefs.getInt("app_limit_instagram", 0)
        val igAppUsed = prefs.getLong("app_used_sec_instagram", 0L)
        val igShortsLimit = prefs.getInt("shorts_limit_instagram", 0)
        val igShortsUsed = prefs.getLong("shorts_used_sec_instagram", 0L)

        val total = prefs.getInt(ShortsBlockerService.PREF_TOTAL_BLOCKED, 0)
        val ytCount = prefs.getInt(ShortsBlockerService.PREF_YOUTUBE_BLOCKED, 0)
        val fbCount = prefs.getInt(ShortsBlockerService.PREF_FACEBOOK_BLOCKED, 0)
        val igCount = prefs.getInt(ShortsBlockerService.PREF_INSTAGRAM_BLOCKED, 0)
        val webCount = prefs.getInt(ShortsBlockerService.PREF_WEBSITES_BLOCKED, 0)
        val lastTime = prefs.getLong(ShortsBlockerService.PREF_LAST_BLOCKED_TIME, 0L)
        val lastApp = prefs.getString(ShortsBlockerService.PREF_LAST_BLOCKED_APP, "") ?: ""

        val rawLogs = prefs.getString(ShortsBlockerService.PREF_RECENT_LOGS, "") ?: ""
        val events = rawLogs.split(";")
            .filter { it.isNotBlank() }
            .mapNotNull { entry ->
                val parts = entry.split(",")
                if (parts.size >= 3) {
                    BlockEvent(
                        timestamp = parts[0].toLongOrNull() ?: System.currentTimeMillis(),
                        appName = parts[1],
                        packageName = parts[2]
                    )
                } else null
            }

        val customAppsStr = prefs.getString(ShortsBlockerService.PREF_CUSTOM_APPS, "") ?: ""
        val customApps = customAppsStr.split(";")
            .filter { it.isNotBlank() }
            .mapNotNull { entry ->
                val parts = entry.split("#")
                if (parts.size >= 3) {
                    val pkg = parts[1]
                    val appLimit = prefs.getInt("app_limit_$pkg", if (parts.size >= 4) parts[3].toIntOrNull() ?: 0 else 0)
                    val appUsed = prefs.getLong("app_used_sec_$pkg", 0L)
                    CustomApp(
                        name = parts[0],
                        packageName = pkg,
                        isEnabled = parts[2].toBoolean(),
                        dailyLimitMinutes = appLimit,
                        todayUsedSeconds = appUsed
                    )
                } else null
            }

        val customWebsitesStr = prefs.getString(ShortsBlockerService.PREF_CUSTOM_WEBSITES, "") ?: ""
        val customWebsites = customWebsitesStr.split(";")
            .filter { it.isNotBlank() }
            .mapNotNull { entry ->
                val parts = entry.split("#")
                if (parts.size >= 2) {
                    BlockedDomain(
                        domain = parts[0],
                        isEnabled = parts[1].toBoolean()
                    )
                } else null
            }

        // Ad Blocker preferences
        val blockAds = prefs.getBoolean(ShortsBlockerService.PREF_BLOCK_ADS, true)
        val autoSkipAds = prefs.getBoolean(ShortsBlockerService.PREF_AUTO_SKIP_VIDEO_ADS, true)
        val blockPopupAds = prefs.getBoolean(ShortsBlockerService.PREF_BLOCK_POPUP_ADS, true)
        val adsBlocked = prefs.getInt(ShortsBlockerService.PREF_ADS_BLOCKED_COUNT, 0)
        val customAdFiltersStr = prefs.getString(ShortsBlockerService.PREF_CUSTOM_AD_FILTERS, "") ?: ""
        val customAdFilters = customAdFiltersStr.split(";")
            .filter { it.isNotBlank() }
            .mapNotNull { entry ->
                val parts = entry.split("#")
                if (parts.size >= 2) {
                    BlockedDomain(
                        domain = parts[0],
                        isEnabled = parts[1].toBoolean()
                    )
                } else null
            }

        _uiState.update {
            it.copy(
                isMasterEnabled = masterEnabled,
                blockYouTube = blockYt,
                blockFacebook = blockFb,
                blockInstagram = blockIg,
                youtubeLimits = AppLimitConfig(
                    appKey = "youtube",
                    displayName = "YouTube",
                    packageName = "com.google.android.youtube",
                    appLimitMinutes = ytAppLimit,
                    appTodayUsedSeconds = ytAppUsed,
                    shortsLimitMinutes = ytShortsLimit,
                    shortsTodayUsedSeconds = ytShortsUsed,
                    isEnabled = blockYt
                ),
                facebookLimits = AppLimitConfig(
                    appKey = "facebook",
                    displayName = "Facebook",
                    packageName = "com.facebook.katana",
                    appLimitMinutes = fbAppLimit,
                    appTodayUsedSeconds = fbAppUsed,
                    shortsLimitMinutes = fbShortsLimit,
                    shortsTodayUsedSeconds = fbShortsUsed,
                    isEnabled = blockFb
                ),
                instagramLimits = AppLimitConfig(
                    appKey = "instagram",
                    displayName = "Instagram",
                    packageName = "com.instagram.android",
                    appLimitMinutes = igAppLimit,
                    appTodayUsedSeconds = igAppUsed,
                    shortsLimitMinutes = igShortsLimit,
                    shortsTodayUsedSeconds = igShortsUsed,
                    isEnabled = blockIg
                ),
                blockAdultWebsites = blockAdult,
                customBlockedWebsites = customWebsites,
                reminderMessage = reminderMsg,
                blockAds = blockAds,
                autoSkipVideoAds = autoSkipAds,
                blockPopupAds = blockPopupAds,
                customAdFilters = customAdFilters,
                adsBlockedCount = adsBlocked,
                customApps = customApps,
                totalBlockedCount = total,
                youtubeBlockedCount = ytCount,
                facebookBlockedCount = fbCount,
                instagramBlockedCount = igCount,
                websiteBlockedCount = webCount,
                lastBlockedTimestamp = lastTime,
                lastBlockedApp = lastApp,
                recentEvents = events
            )
        }
    }

    // Per-app config setters
    fun setAppSpecificLimits(appKey: String, appLimitMinutes: Int, shortsLimitMinutes: Int) {
        prefs.edit()
            .putInt("app_limit_$appKey", appLimitMinutes)
            .putInt("shorts_limit_$appKey", shortsLimitMinutes)
            .apply()

        _uiState.update { state ->
            when (appKey) {
                "youtube" -> state.copy(
                    youtubeLimits = state.youtubeLimits.copy(
                        appLimitMinutes = appLimitMinutes,
                        shortsLimitMinutes = shortsLimitMinutes
                    )
                )
                "facebook" -> state.copy(
                    facebookLimits = state.facebookLimits.copy(
                        appLimitMinutes = appLimitMinutes,
                        shortsLimitMinutes = shortsLimitMinutes
                    )
                )
                "instagram" -> state.copy(
                    instagramLimits = state.instagramLimits.copy(
                        appLimitMinutes = appLimitMinutes,
                        shortsLimitMinutes = shortsLimitMinutes
                    )
                )
                else -> {
                    val updatedCustom = state.customApps.map {
                        if (it.packageName.equals(appKey, ignoreCase = true)) {
                            it.copy(dailyLimitMinutes = appLimitMinutes)
                        } else it
                    }
                    state.copy(customApps = updatedCustom)
                }
            }
        }
    }

    fun resetAppUsage(appKey: String) {
        prefs.edit().putLong("app_used_sec_$appKey", 0L).apply()
        _uiState.update { state ->
            when (appKey) {
                "youtube" -> state.copy(youtubeLimits = state.youtubeLimits.copy(appTodayUsedSeconds = 0L))
                "facebook" -> state.copy(facebookLimits = state.facebookLimits.copy(appTodayUsedSeconds = 0L))
                "instagram" -> state.copy(instagramLimits = state.instagramLimits.copy(appTodayUsedSeconds = 0L))
                else -> {
                    val updated = state.customApps.map {
                        if (it.packageName.equals(appKey, ignoreCase = true)) it.copy(todayUsedSeconds = 0L) else it
                    }
                    state.copy(customApps = updated)
                }
            }
        }
    }

    fun resetShortsUsage(appKey: String) {
        prefs.edit().putLong("shorts_used_sec_$appKey", 0L).apply()
        _uiState.update { state ->
            when (appKey) {
                "youtube" -> state.copy(youtubeLimits = state.youtubeLimits.copy(shortsTodayUsedSeconds = 0L))
                "facebook" -> state.copy(facebookLimits = state.facebookLimits.copy(shortsTodayUsedSeconds = 0L))
                "instagram" -> state.copy(instagramLimits = state.instagramLimits.copy(shortsTodayUsedSeconds = 0L))
                else -> state
            }
        }
    }

    fun addCustomApp(name: String, packageName: String, limitMinutes: Int = 0) {
        val currentList = _uiState.value.customApps.toMutableList()
        val existingIndex = currentList.indexOfFirst { it.packageName.equals(packageName, ignoreCase = true) }
        if (existingIndex >= 0) {
            currentList[existingIndex] = currentList[existingIndex].copy(
                name = name,
                isEnabled = true,
                dailyLimitMinutes = limitMinutes
            )
        } else {
            currentList.add(CustomApp(name = name, packageName = packageName, isEnabled = true, dailyLimitMinutes = limitMinutes))
        }
        saveCustomApps(currentList)
    }

    fun toggleCustomApp(packageName: String, enabled: Boolean) {
        val currentList = _uiState.value.customApps.map {
            if (it.packageName.equals(packageName, ignoreCase = true)) it.copy(isEnabled = enabled) else it
        }
        saveCustomApps(currentList)
    }

    fun removeCustomApp(packageName: String) {
        val currentList = _uiState.value.customApps.filterNot { it.packageName.equals(packageName, ignoreCase = true) }
        saveCustomApps(currentList)
    }

    private fun saveCustomApps(list: List<CustomApp>) {
        val serialized = list.joinToString(";") { "${it.name}#${it.packageName}#${it.isEnabled}#${it.dailyLimitMinutes}" }
        prefs.edit().putString(ShortsBlockerService.PREF_CUSTOM_APPS, serialized).apply()
        _uiState.update { it.copy(customApps = list) }
    }

    // Adult and Website Blocker methods
    fun setBlockAdultWebsites(enabled: Boolean) {
        prefs.edit().putBoolean(ShortsBlockerService.PREF_BLOCK_ADULT_WEBSITES, enabled).apply()
        _uiState.update { it.copy(blockAdultWebsites = enabled) }
    }

    fun addCustomWebsite(domain: String) {
        val cleanDomain = domain.trim().lowercase().removePrefix("https://").removePrefix("http://").removePrefix("www.")
        if (cleanDomain.isBlank()) return

        val current = _uiState.value.customBlockedWebsites.toMutableList()
        if (current.none { it.domain.equals(cleanDomain, ignoreCase = true) }) {
            current.add(BlockedDomain(domain = cleanDomain, isEnabled = true))
            saveCustomWebsites(current)
        }
    }

    fun toggleCustomWebsite(domain: String, enabled: Boolean) {
        val current = _uiState.value.customBlockedWebsites.map {
            if (it.domain.equals(domain, ignoreCase = true)) it.copy(isEnabled = enabled) else it
        }
        saveCustomWebsites(current)
    }

    fun removeCustomWebsite(domain: String) {
        val current = _uiState.value.customBlockedWebsites.filterNot { it.domain.equals(domain, ignoreCase = true) }
        saveCustomWebsites(current)
    }

    private fun saveCustomWebsites(list: List<BlockedDomain>) {
        val serialized = list.joinToString(";") { "${it.domain}#${it.isEnabled}" }
        prefs.edit().putString(ShortsBlockerService.PREF_CUSTOM_WEBSITES, serialized).apply()
        _uiState.update { it.copy(customBlockedWebsites = list) }
    }

    // Ad Blocker methods
    fun setBlockAds(enabled: Boolean) {
        prefs.edit().putBoolean(ShortsBlockerService.PREF_BLOCK_ADS, enabled).apply()
        _uiState.update { it.copy(blockAds = enabled) }
    }

    fun setAutoSkipVideoAds(enabled: Boolean) {
        prefs.edit().putBoolean(ShortsBlockerService.PREF_AUTO_SKIP_VIDEO_ADS, enabled).apply()
        _uiState.update { it.copy(autoSkipVideoAds = enabled) }
    }

    fun setBlockPopupAds(enabled: Boolean) {
        prefs.edit().putBoolean(ShortsBlockerService.PREF_BLOCK_POPUP_ADS, enabled).apply()
        _uiState.update { it.copy(blockPopupAds = enabled) }
    }

    fun addCustomAdFilter(domain: String) {
        val clean = domain.trim().lowercase().removePrefix("https://").removePrefix("http://").removePrefix("www.")
        if (clean.isBlank()) return

        val current = _uiState.value.customAdFilters.toMutableList()
        if (current.none { it.domain.equals(clean, ignoreCase = true) }) {
            current.add(BlockedDomain(domain = clean, isEnabled = true))
            saveCustomAdFilters(current)
        }
    }

    fun toggleCustomAdFilter(domain: String, enabled: Boolean) {
        val current = _uiState.value.customAdFilters.map {
            if (it.domain.equals(domain, ignoreCase = true)) it.copy(isEnabled = enabled) else it
        }
        saveCustomAdFilters(current)
    }

    fun removeCustomAdFilter(domain: String) {
        val current = _uiState.value.customAdFilters.filterNot { it.domain.equals(domain, ignoreCase = true) }
        saveCustomAdFilters(current)
    }

    private fun saveCustomAdFilters(list: List<BlockedDomain>) {
        val serialized = list.joinToString(";") { "${it.domain}#${it.isEnabled}" }
        prefs.edit().putString(ShortsBlockerService.PREF_CUSTOM_AD_FILTERS, serialized).apply()
        _uiState.update { it.copy(customAdFilters = list) }
    }

    fun setReminderMessage(message: String) {
        val trimmed = message.ifBlank { ShortsBlockerService.DEFAULT_REMINDER_MESSAGE }
        prefs.edit().putString(ShortsBlockerService.PREF_REMINDER_MESSAGE, trimmed).apply()
        _uiState.update { it.copy(reminderMessage = trimmed) }
    }

    fun setMasterEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(ShortsBlockerService.PREF_ENABLED, enabled).apply()
        _uiState.update { it.copy(isMasterEnabled = enabled) }
    }

    fun setBlockYouTube(enabled: Boolean) {
        prefs.edit().putBoolean(ShortsBlockerService.PREF_BLOCK_YOUTUBE, enabled).apply()
        _uiState.update {
            it.copy(
                blockYouTube = enabled,
                youtubeLimits = it.youtubeLimits.copy(isEnabled = enabled)
            )
        }
    }

    fun setBlockFacebook(enabled: Boolean) {
        prefs.edit().putBoolean(ShortsBlockerService.PREF_BLOCK_FACEBOOK, enabled).apply()
        _uiState.update {
            it.copy(
                blockFacebook = enabled,
                facebookLimits = it.facebookLimits.copy(isEnabled = enabled)
            )
        }
    }

    fun setBlockInstagram(enabled: Boolean) {
        prefs.edit().putBoolean(ShortsBlockerService.PREF_BLOCK_INSTAGRAM, enabled).apply()
        _uiState.update {
            it.copy(
                blockInstagram = enabled,
                instagramLimits = it.instagramLimits.copy(isEnabled = enabled)
            )
        }
    }

    fun resetStats() {
        val editor = prefs.edit()
            .putInt(ShortsBlockerService.PREF_TOTAL_BLOCKED, 0)
            .putInt(ShortsBlockerService.PREF_YOUTUBE_BLOCKED, 0)
            .putInt(ShortsBlockerService.PREF_FACEBOOK_BLOCKED, 0)
            .putInt(ShortsBlockerService.PREF_INSTAGRAM_BLOCKED, 0)
            .putInt(ShortsBlockerService.PREF_WEBSITES_BLOCKED, 0)
            .putInt(ShortsBlockerService.PREF_ADS_BLOCKED_COUNT, 0)
            .putLong(ShortsBlockerService.PREF_LAST_BLOCKED_TIME, 0L)
            .putString(ShortsBlockerService.PREF_LAST_BLOCKED_APP, "")
            .putString(ShortsBlockerService.PREF_RECENT_LOGS, "")

        val apps = listOf("youtube", "facebook", "instagram")
        apps.forEach {
            editor.putLong("app_used_sec_$it", 0L)
            editor.putLong("shorts_used_sec_$it", 0L)
        }
        editor.apply()

        _uiState.update {
            it.copy(
                totalBlockedCount = 0,
                youtubeBlockedCount = 0,
                facebookBlockedCount = 0,
                instagramBlockedCount = 0,
                websiteBlockedCount = 0,
                adsBlockedCount = 0,
                lastBlockedTimestamp = 0L,
                lastBlockedApp = "",
                youtubeLimits = it.youtubeLimits.copy(appTodayUsedSeconds = 0L, shortsTodayUsedSeconds = 0L),
                facebookLimits = it.facebookLimits.copy(appTodayUsedSeconds = 0L, shortsTodayUsedSeconds = 0L),
                instagramLimits = it.instagramLimits.copy(appTodayUsedSeconds = 0L, shortsTodayUsedSeconds = 0L),
                recentEvents = emptyList()
            )
        }
    }

    fun simulateTestBlock(appName: String) {
        val currentTotal = prefs.getInt(ShortsBlockerService.PREF_TOTAL_BLOCKED, 0) + 1
        val editor = prefs.edit().putInt(ShortsBlockerService.PREF_TOTAL_BLOCKED, currentTotal)

        when {
            appName.contains("YouTube", ignoreCase = true) -> {
                editor.putInt(ShortsBlockerService.PREF_YOUTUBE_BLOCKED, prefs.getInt(ShortsBlockerService.PREF_YOUTUBE_BLOCKED, 0) + 1)
            }
            appName.contains("Facebook", ignoreCase = true) -> {
                editor.putInt(ShortsBlockerService.PREF_FACEBOOK_BLOCKED, prefs.getInt(ShortsBlockerService.PREF_FACEBOOK_BLOCKED, 0) + 1)
            }
            appName.contains("Instagram", ignoreCase = true) -> {
                editor.putInt(ShortsBlockerService.PREF_INSTAGRAM_BLOCKED, prefs.getInt(ShortsBlockerService.PREF_INSTAGRAM_BLOCKED, 0) + 1)
            }
            appName.contains("Ad", ignoreCase = true) || appName.contains("বিজ্ঞাপন", ignoreCase = true) -> {
                editor.putInt(ShortsBlockerService.PREF_ADS_BLOCKED_COUNT, prefs.getInt(ShortsBlockerService.PREF_ADS_BLOCKED_COUNT, 0) + 1)
            }
            else -> {
                editor.putInt(ShortsBlockerService.PREF_WEBSITES_BLOCKED, prefs.getInt(ShortsBlockerService.PREF_WEBSITES_BLOCKED, 0) + 1)
            }
        }

        val now = System.currentTimeMillis()
        editor.putLong(ShortsBlockerService.PREF_LAST_BLOCKED_TIME, now)
        editor.putString(ShortsBlockerService.PREF_LAST_BLOCKED_APP, appName)

        val existingLogs = prefs.getString(ShortsBlockerService.PREF_RECENT_LOGS, "") ?: ""
        val newEntry = "$now,$appName,simulated.package"
        val updatedLogs = (listOf(newEntry) + existingLogs.split(";").filter { it.isNotBlank() })
            .take(20)
            .joinToString(";")

        editor.putString(ShortsBlockerService.PREF_RECENT_LOGS, updatedLogs)
        editor.apply()

        syncStatistics()
    }

    private fun syncStatistics() {
        val total = prefs.getInt(ShortsBlockerService.PREF_TOTAL_BLOCKED, 0)
        val ytCount = prefs.getInt(ShortsBlockerService.PREF_YOUTUBE_BLOCKED, 0)
        val fbCount = prefs.getInt(ShortsBlockerService.PREF_FACEBOOK_BLOCKED, 0)
        val igCount = prefs.getInt(ShortsBlockerService.PREF_INSTAGRAM_BLOCKED, 0)
        val webCount = prefs.getInt(ShortsBlockerService.PREF_WEBSITES_BLOCKED, 0)
        val adsCount = prefs.getInt(ShortsBlockerService.PREF_ADS_BLOCKED_COUNT, 0)
        val lastTime = prefs.getLong(ShortsBlockerService.PREF_LAST_BLOCKED_TIME, 0L)
        val lastApp = prefs.getString(ShortsBlockerService.PREF_LAST_BLOCKED_APP, "") ?: ""

        val ytAppUsed = prefs.getLong("app_used_sec_youtube", 0L)
        val ytShortsUsed = prefs.getLong("shorts_used_sec_youtube", 0L)
        val fbAppUsed = prefs.getLong("app_used_sec_facebook", 0L)
        val fbShortsUsed = prefs.getLong("shorts_used_sec_facebook", 0L)
        val igAppUsed = prefs.getLong("app_used_sec_instagram", 0L)
        val igShortsUsed = prefs.getLong("shorts_used_sec_instagram", 0L)

        val rawLogs = prefs.getString(ShortsBlockerService.PREF_RECENT_LOGS, "") ?: ""
        val events = rawLogs.split(";")
            .filter { it.isNotBlank() }
            .mapNotNull { entry ->
                val parts = entry.split(",")
                if (parts.size >= 3) {
                    BlockEvent(
                        timestamp = parts[0].toLongOrNull() ?: System.currentTimeMillis(),
                        appName = parts[1],
                        packageName = parts[2]
                    )
                } else null
            }

        val customAppsStr = prefs.getString(ShortsBlockerService.PREF_CUSTOM_APPS, "") ?: ""
        val customApps = customAppsStr.split(";")
            .filter { it.isNotBlank() }
            .mapNotNull { entry ->
                val parts = entry.split("#")
                if (parts.size >= 3) {
                    val pkg = parts[1]
                    val appLimit = prefs.getInt("app_limit_$pkg", if (parts.size >= 4) parts[3].toIntOrNull() ?: 0 else 0)
                    val appUsed = prefs.getLong("app_used_sec_$pkg", 0L)
                    CustomApp(
                        name = parts[0],
                        packageName = pkg,
                        isEnabled = parts[2].toBoolean(),
                        dailyLimitMinutes = appLimit,
                        todayUsedSeconds = appUsed
                    )
                } else null
            }

        _uiState.update {
            it.copy(
                totalBlockedCount = total,
                youtubeBlockedCount = ytCount,
                facebookBlockedCount = fbCount,
                instagramBlockedCount = igCount,
                websiteBlockedCount = webCount,
                adsBlockedCount = adsCount,
                lastBlockedTimestamp = lastTime,
                lastBlockedApp = lastApp,
                youtubeLimits = it.youtubeLimits.copy(appTodayUsedSeconds = ytAppUsed, shortsTodayUsedSeconds = ytShortsUsed),
                facebookLimits = it.facebookLimits.copy(appTodayUsedSeconds = fbAppUsed, shortsTodayUsedSeconds = fbShortsUsed),
                instagramLimits = it.instagramLimits.copy(appTodayUsedSeconds = igAppUsed, shortsTodayUsedSeconds = igShortsUsed),
                customApps = customApps,
                recentEvents = events
            )
        }
    }

    private fun isAccessibilityServiceEnabled(context: Context, service: Class<*>): Boolean {
        val expectedComponentName = "${context.packageName}/${service.name}"
        val enabledServices = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false

        return enabledServices.split(":")
            .any { it.equals(expectedComponentName, ignoreCase = true) }
    }
}
