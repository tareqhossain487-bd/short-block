package com.example.shortsblocker

data class InstalledAppItem(
    val appName: String,
    val packageName: String,
    val isSystemApp: Boolean = false,
    val isSelected: Boolean = false
)

data class BlockedDomain(
    val id: String = java.util.UUID.randomUUID().toString(),
    val domain: String,
    val isEnabled: Boolean = true
)

data class AppLimitConfig(
    val appKey: String, // "youtube", "facebook", "instagram", or package name
    val displayName: String,
    val packageName: String,
    val appLimitMinutes: Int = 0, // 0 = Unlimited overall app screen time
    val appTodayUsedSeconds: Long = 0L,
    val shortsLimitMinutes: Int = 0, // 0 = Strict block, > 0 = allowed shorts/reels minutes
    val shortsTodayUsedSeconds: Long = 0L,
    val isEnabled: Boolean = true
)

data class CustomApp(
    val id: String = java.util.UUID.randomUUID().toString(),
    val name: String,
    val packageName: String,
    val isEnabled: Boolean = true,
    val dailyLimitMinutes: Int = 0,
    val todayUsedSeconds: Long = 0L
)

data class BlockEvent(
    val id: Long = System.currentTimeMillis(),
    val appName: String,
    val packageName: String,
    val timestamp: Long = System.currentTimeMillis()
)

data class ShortsBlockerUiState(
    val isAccessibilityServiceActive: Boolean = false,
    val isMasterEnabled: Boolean = true,
    val blockYouTube: Boolean = true,
    val blockFacebook: Boolean = true,
    val blockInstagram: Boolean = true,
    // Per-app dedicated Limits (YouTube, Facebook, Instagram, + Custom Apps)
    val youtubeLimits: AppLimitConfig = AppLimitConfig(
        appKey = "youtube",
        displayName = "YouTube",
        packageName = "com.google.android.youtube"
    ),
    val facebookLimits: AppLimitConfig = AppLimitConfig(
        appKey = "facebook",
        displayName = "Facebook",
        packageName = "com.facebook.katana"
    ),
    val instagramLimits: AppLimitConfig = AppLimitConfig(
        appKey = "instagram",
        displayName = "Instagram",
        packageName = "com.instagram.android"
    ),
    val customApps: List<CustomApp> = emptyList(),
    // Adult & Porn site blocker
    val blockAdultWebsites: Boolean = true,
    val customBlockedWebsites: List<BlockedDomain> = emptyList(),
    val reminderMessage: String = "আল্লাহর দিকে ফিরে আসো",
    // Ad Blocker & Auto-Skip
    val blockAds: Boolean = true,
    val autoSkipVideoAds: Boolean = true,
    val blockPopupAds: Boolean = true,
    val customAdFilters: List<BlockedDomain> = emptyList(),
    val adsBlockedCount: Int = 0,
    val installedApps: List<InstalledAppItem> = emptyList(),
    val isLoadingApps: Boolean = false,
    val totalBlockedCount: Int = 0,
    val youtubeBlockedCount: Int = 0,
    val facebookBlockedCount: Int = 0,
    val instagramBlockedCount: Int = 0,
    val websiteBlockedCount: Int = 0,
    val lastBlockedTimestamp: Long = 0L,
    val lastBlockedApp: String = "",
    val recentEvents: List<BlockEvent> = emptyList()
)
