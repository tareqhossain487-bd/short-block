package com.example.shortsblocker

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
    val totalBlockedCount: Int = 0,
    val youtubeBlockedCount: Int = 0,
    val facebookBlockedCount: Int = 0,
    val instagramBlockedCount: Int = 0,
    val lastBlockedTimestamp: Long = 0L,
    val lastBlockedApp: String = "",
    val recentEvents: List<BlockEvent> = emptyList()
)
