package com.example.shortsblocker.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.HourglassBottom
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.StayCurrentPortrait
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.shortsblocker.AppLimitConfig
import com.example.shortsblocker.CustomApp
import com.example.shortsblocker.ShortsBlockerUiState
import com.example.shortsblocker.ui.theme.EmeraldSuccess
import com.example.shortsblocker.ui.theme.IndigoPrimary
import com.example.shortsblocker.ui.theme.RoseError
import com.example.shortsblocker.ui.theme.VioletSecondary

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AppAndShortsDailyLimitCard(
    uiState: ShortsBlockerUiState,
    onSetAppSpecificLimits: (String, Int, Int) -> Unit, // appKey, appLimitMins, shortsLimitMins
    onResetAppUsage: (String) -> Unit,
    onResetShortsUsage: (String) -> Unit,
    onToggleCustomApp: (String, Boolean) -> Unit = { _, _ -> },
    onRemoveCustomApp: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    // List of selectable apps: built-in (YouTube, Facebook, Instagram) + Custom Apps
    val builtInKeys = listOf("youtube", "facebook", "instagram")
    var selectedAppKey by remember { mutableStateOf("youtube") }

    // Fallback if selected custom app was deleted
    val customAppKeys = uiState.customApps.map { it.packageName }
    val allAppKeys = builtInKeys + customAppKeys
    if (selectedAppKey !in allAppKeys) {
        selectedAppKey = "youtube"
    }

    val selectedCustomApp = uiState.customApps.firstOrNull { it.packageName == selectedAppKey }
    val isCustomSelected = selectedCustomApp != null

    val currentConfig = when (selectedAppKey) {
        "youtube" -> uiState.youtubeLimits
        "facebook" -> uiState.facebookLimits
        "instagram" -> uiState.instagramLimits
        else -> AppLimitConfig(
            appKey = selectedAppKey,
            displayName = selectedCustomApp?.name ?: "Custom App",
            packageName = selectedAppKey,
            appLimitMinutes = selectedCustomApp?.dailyLimitMinutes ?: 0,
            appTodayUsedSeconds = selectedCustomApp?.todayUsedSeconds ?: 0L,
            shortsLimitMinutes = 0,
            shortsTodayUsedSeconds = 0L,
            isEnabled = selectedCustomApp?.isEnabled ?: true
        )
    }

    val primaryThemeColor = when (selectedAppKey) {
        "youtube" -> RoseError
        "facebook" -> Color(0xFF1877F2)
        "instagram" -> Color(0xFFE1306C)
        else -> IndigoPrimary
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("app_and_shorts_daily_limit_card"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Card Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Timer,
                        contentDescription = null,
                        tint = IndigoPrimary,
                        modifier = Modifier.size(24.dp)
                    )
                    Column {
                        Text(
                            text = "Daily App & Shorts Limits",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "প্রতিটি অ্যাপের জন্য আলাদা App Limit ও Shorts Limit",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Scrollable App Selector Tabs (YouTube, Facebook, Instagram + Any newly added custom apps)
            ScrollableTabRow(
                selectedTabIndex = allAppKeys.indexOf(selectedAppKey).coerceAtLeast(0),
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                edgePadding = 8.dp,
                modifier = Modifier.clip(RoundedCornerShape(12.dp)),
                divider = {}
            ) {
                Tab(
                    selected = selectedAppKey == "youtube",
                    onClick = { selectedAppKey = "youtube" },
                    text = {
                        Text(
                            text = "YouTube",
                            fontWeight = if (selectedAppKey == "youtube") FontWeight.Bold else FontWeight.Normal,
                            color = if (selectedAppKey == "youtube") RoseError else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                )
                Tab(
                    selected = selectedAppKey == "facebook",
                    onClick = { selectedAppKey = "facebook" },
                    text = {
                        Text(
                            text = "Facebook",
                            fontWeight = if (selectedAppKey == "facebook") FontWeight.Bold else FontWeight.Normal,
                            color = if (selectedAppKey == "facebook") Color(0xFF1877F2) else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                )
                Tab(
                    selected = selectedAppKey == "instagram",
                    onClick = { selectedAppKey = "instagram" },
                    text = {
                        Text(
                            text = "Instagram",
                            fontWeight = if (selectedAppKey == "instagram") FontWeight.Bold else FontWeight.Normal,
                            color = if (selectedAppKey == "instagram") Color(0xFFE1306C) else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                )
                uiState.customApps.forEach { customApp ->
                    Tab(
                        selected = selectedAppKey == customApp.packageName,
                        onClick = { selectedAppKey = customApp.packageName },
                        text = {
                            Text(
                                text = customApp.name,
                                fontWeight = if (selectedAppKey == customApp.packageName) FontWeight.Bold else FontWeight.Normal,
                                color = if (selectedAppKey == customApp.packageName) IndigoPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    )
                }
            }

            // Selected App Config Container
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = primaryThemeColor.copy(alpha = 0.05f),
                border = androidx.compose.foundation.BorderStroke(1.dp, primaryThemeColor.copy(alpha = 0.2f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Title & Summary Row - Responsive on small screens
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "${currentConfig.displayName} লিমিট সেটিংস",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = primaryThemeColor,
                                modifier = Modifier.weight(1f, fill = false),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )

                            Spacer(modifier = Modifier.width(6.dp))

                            val shortsStatus = when {
                                currentConfig.shortsLimitMinutes == -1 -> "Unlimited"
                                currentConfig.shortsLimitMinutes == 0 -> "Block"
                                currentConfig.shortsLimitMinutes >= 60 && currentConfig.shortsLimitMinutes % 60 == 0 -> "${currentConfig.shortsLimitMinutes / 60}h"
                                currentConfig.shortsLimitMinutes >= 60 -> "${currentConfig.shortsLimitMinutes / 60}h ${currentConfig.shortsLimitMinutes % 60}m"
                                else -> "${currentConfig.shortsLimitMinutes}m"
                            }
                            val appStatus = when {
                                currentConfig.appLimitMinutes == 0 -> "Unlimited"
                                currentConfig.appLimitMinutes >= 60 && currentConfig.appLimitMinutes % 60 == 0 -> "${currentConfig.appLimitMinutes / 60}h"
                                currentConfig.appLimitMinutes >= 60 -> "${currentConfig.appLimitMinutes / 60}h ${currentConfig.appLimitMinutes % 60}m"
                                else -> "${currentConfig.appLimitMinutes}m"
                            }

                            Surface(
                                color = primaryThemeColor.copy(alpha = 0.12f),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = if (isCustomSelected) "Limit: $appStatus" else "App: $appStatus • Shorts: $shortsStatus",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = primaryThemeColor,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }

                    // 1. App Limit Section (with prominent Progress bar and Usage indicator)
                    AppLimitSubSection(
                        title = "1. App Limit",
                        description = if (currentConfig.appLimitMinutes == 0) {
                            "Unlimited: সারাদিনে যতক্ষণ ইচ্ছা অ্যাপ ব্যবহার করা যাবে।"
                        } else {
                            "দৈনিক ${currentConfig.appLimitMinutes} মিনিট পর্যন্ত সম্পূর্ণ ${currentConfig.displayName} ব্যবহার করা যাবে। সময় শেষ হলে অ্যাপ স্বয়ংক্রিয়ভাবে বন্ধ হবে।"
                        },
                        icon = Icons.Default.StayCurrentPortrait,
                        limitMinutes = currentConfig.appLimitMinutes,
                        usedSeconds = currentConfig.appTodayUsedSeconds,
                        presets = listOf(
                            0 to "Unlimited",
                            1 to "1 min",
                            5 to "5 min",
                            15 to "15 min",
                            30 to "30 min",
                            45 to "45 min",
                            60 to "1 hr",
                            120 to "2 hr"
                        ),
                        accentColor = primaryThemeColor,
                        onSelectLimit = { newAppLimit ->
                            onSetAppSpecificLimits(selectedAppKey, newAppLimit, currentConfig.shortsLimitMinutes)
                        },
                        onResetUsage = { onResetAppUsage(selectedAppKey) },
                        tagPrefix = "${selectedAppKey}_app"
                    )

                    // 2. Shorts / Reels Limit Section (Only for YouTube, Facebook, Instagram)
                    if (!isCustomSelected) {
                        HorizontalDivider(
                            color = primaryThemeColor.copy(alpha = 0.15f),
                            thickness = 1.dp
                        )

                        AppLimitSubSection(
                            title = "2. Shorts & Reels Limit",
                            description = when (currentConfig.shortsLimitMinutes) {
                                -1 -> "Unlimited: শর্টস বা রিলস ইচ্ছেমতো দেখা যাবে, কোনো ব্লকিং হবে না।"
                                0 -> "Block: শর্টস/রিলস সম্পূর্ণ বন্ধ থাকবে। ওপেন করলেই সাথে সাথে ব্লক হবে।"
                                else -> "দৈনিক ${currentConfig.shortsLimitMinutes} মিনিট শর্টস দেখতে পারবেন। সময় শেষ হলে শুধুমাত্র শর্টস ব্লক হবে (বড় ভিডিও চলবে)।"
                            },
                            icon = Icons.Default.PlayArrow,
                            limitMinutes = currentConfig.shortsLimitMinutes,
                            usedSeconds = currentConfig.shortsTodayUsedSeconds,
                            presets = listOf(
                                -1 to "Unlimited",
                                0 to "Block",
                                1 to "1 min",
                                3 to "3 min",
                                5 to "5 min",
                                10 to "10 min",
                                15 to "15 min"
                            ),
                            accentColor = RoseError,
                            onSelectLimit = { newShortsLimit ->
                                onSetAppSpecificLimits(selectedAppKey, currentConfig.appLimitMinutes, newShortsLimit)
                            },
                            onResetUsage = { onResetShortsUsage(selectedAppKey) },
                            tagPrefix = "${selectedAppKey}_shorts"
                        )
                    }
                }
            }

            // 3. New Added Custom Apps Overview List underneath Daily Limits
            if (uiState.customApps.isNotEmpty()) {
                HorizontalDivider(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    thickness = 1.dp
                )

                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "নতুন যোগ করা অ্যাপস (${uiState.customApps.size})",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "কাস্টম অ্যাপ লিমিট ও ব্যবহার",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    uiState.customApps.forEach { customApp ->
                        val usedMins = (customApp.todayUsedSeconds / 60L).toInt()
                        val usedSecs = (customApp.todayUsedSeconds % 60L).toInt()
                        val hasLimit = customApp.dailyLimitMinutes > 0
                        val isLimitExceeded = hasLimit && usedMins >= customApp.dailyLimitMinutes
                        val appProgress = if (hasLimit) {
                            (customApp.todayUsedSeconds.toFloat() / (customApp.dailyLimitMinutes * 60f)).coerceIn(0f, 1f)
                        } else 0f

                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { selectedAppKey = customApp.packageName },
                            color = if (selectedAppKey == customApp.packageName) {
                                IndigoPrimary.copy(alpha = 0.1f)
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                            },
                            shape = RoundedCornerShape(12.dp),
                            border = if (selectedAppKey == customApp.packageName) {
                                androidx.compose.foundation.BorderStroke(1.5.dp, IndigoPrimary)
                            } else null
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                                        modifier = Modifier.weight(1f, fill = false)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(36.dp)
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(IndigoPrimary.copy(alpha = 0.15f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Apps,
                                                contentDescription = null,
                                                tint = IndigoPrimary,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }

                                        Column(modifier = Modifier.weight(1f, fill = false)) {
                                            Text(
                                                text = customApp.name,
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurface,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = if (hasLimit) "ব্যবহার: ${usedMins}m ${usedSecs}s / ${customApp.dailyLimitMinutes}m" else "ব্যবহার: ${usedMins}m ${usedSecs}s (Unlimited)",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = if (isLimitExceeded) RoseError else MaterialTheme.colorScheme.onSurfaceVariant,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.width(6.dp))

                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                                    ) {
                                        // Reset button for this custom app
                                        IconButton(
                                            onClick = { onResetAppUsage(customApp.packageName) },
                                            modifier = Modifier.size(30.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.RestartAlt,
                                                contentDescription = "Reset usage",
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }

                                        // Switch for enabled
                                        Switch(
                                            checked = customApp.isEnabled,
                                            onCheckedChange = { onToggleCustomApp(customApp.packageName, it) },
                                            modifier = Modifier.height(28.dp)
                                        )

                                        // Delete button
                                        IconButton(
                                            onClick = { onRemoveCustomApp(customApp.packageName) },
                                            modifier = Modifier.size(30.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.DeleteOutline,
                                                contentDescription = "Delete app",
                                                tint = RoseError,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }

                                // Progress bar for custom app if limit is set
                                if (hasLimit) {
                                    LinearProgressIndicator(
                                        progress = { appProgress },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(4.dp)
                                            .clip(RoundedCornerShape(2.dp)),
                                        color = if (isLimitExceeded) RoseError else IndigoPrimary,
                                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AppLimitSubSection(
    title: String,
    description: String,
    icon: ImageVector,
    limitMinutes: Int,
    usedSeconds: Long,
    presets: List<Pair<Int, String>>,
    accentColor: Color,
    onSelectLimit: (Int) -> Unit,
    onResetUsage: () -> Unit,
    tagPrefix: String
) {
    val usedMinutes = (usedSeconds / 60L).toInt()
    val usedSecondsRemainder = (usedSeconds % 60L).toInt()
    val isExceeded = limitMinutes > 0 && usedMinutes >= limitMinutes
    val progress = if (limitMinutes > 0) {
        (usedSeconds.toFloat() / (limitMinutes * 60f)).coerceIn(0f, 1f)
    } else 0f

    val isCustomActive = limitMinutes > 0 && presets.none { it.first == limitMinutes }

    var showCustomDialog by remember { mutableStateOf(false) }
    var customInputText by remember { mutableStateOf("") }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        // Section Header Row: Left title (weighted) and Right status badge (non-breaking)
        val isShorts = tagPrefix.contains("shorts")
        val badgeText = when {
            isShorts && limitMinutes == -1 -> "Unlimited"
            isShorts && limitMinutes == 0 -> "Block"
            !isShorts && limitMinutes == 0 -> "Unlimited"
            isExceeded -> "Limit Reached"
            else -> "${(limitMinutes - usedMinutes).coerceAtLeast(0)}m left"
        }
        val badgeColor = when {
            isShorts && limitMinutes == -1 -> EmeraldSuccess
            isShorts && limitMinutes == 0 -> RoseError
            !isShorts && limitMinutes == 0 -> EmeraldSuccess
            isExceeded -> RoseError
            else -> EmeraldSuccess
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.weight(1f, fill = false),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.width(6.dp))

            Surface(
                color = badgeColor.copy(alpha = 0.15f),
                shape = RoundedCornerShape(6.dp)
            ) {
                Text(
                    text = badgeText,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = badgeColor,
                    maxLines = 1,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                )
            }
        }

        Text(
            text = description,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        // Always show live Usage Process Box & Progress Bar (how many minutes used today)
        Surface(
            shape = RoundedCornerShape(10.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.weight(1f, fill = false)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(
                                    when {
                                        isShorts && limitMinutes == 0 -> RoseError
                                        usedSeconds > 0 -> accentColor
                                        else -> MaterialTheme.colorScheme.outline
                                    }
                                )
                        )
                        Text(
                            text = when {
                                isShorts && limitMinutes == 0 -> "শর্টস স্ট্যাটাস"
                                isShorts -> "শর্টস ব্যবহার"
                                else -> "অ্যাপ ব্যবহার"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    val formattedLimit = when {
                        limitMinutes >= 60 && limitMinutes % 60 == 0 -> "${limitMinutes / 60}h"
                        limitMinutes >= 60 -> "${limitMinutes / 60}h ${limitMinutes % 60}m"
                        else -> "${limitMinutes}m"
                    }

                    Text(
                        text = when {
                            isShorts && limitMinutes == 0 -> "সম্পূর্ণ বন্ধ (Block)"
                            limitMinutes > 0 -> "${usedMinutes}m ${usedSecondsRemainder}s / $formattedLimit"
                            else -> "${usedMinutes}m ${usedSecondsRemainder}s"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = if (isExceeded) RoseError else MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Progress indicator bar (if limit > 0) or usage activity indicator
                if (limitMinutes > 0) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        LinearProgressIndicator(
                            progress = { progress },
                            modifier = Modifier
                                .weight(1f)
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = if (isExceeded) RoseError else accentColor,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                        Text(
                            text = "${(progress * 100).toInt()}%",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (isExceeded) RoseError else accentColor,
                            maxLines = 1
                        )
                    }
                }
            }
        }

        // Quick Preset Chips + Custom + Reset Option Chip
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            presets.forEach { (mins, label) ->
                val isSelected = limitMinutes == mins
                FilterChip(
                    selected = isSelected,
                    onClick = { onSelectLimit(mins) },
                    label = {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            maxLines = 1
                        )
                    },
                    leadingIcon = if (isSelected) {
                        {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    } else null,
                    modifier = Modifier.testTag("${tagPrefix}_chip_$mins")
                )
            }

            // Custom Chip with formatted hours and minutes
            val customFormatted = when {
                limitMinutes >= 60 && limitMinutes % 60 == 0 -> "${limitMinutes / 60}h"
                limitMinutes >= 60 -> "${limitMinutes / 60}h ${limitMinutes % 60}m"
                else -> "${limitMinutes}m"
            }

            FilterChip(
                selected = isCustomActive,
                onClick = {
                    showCustomDialog = true
                },
                label = {
                    Text(
                        text = if (isCustomActive) "Custom ($customFormatted)" else "Custom...",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = if (isCustomActive) FontWeight.Bold else FontWeight.Normal,
                        maxLines = 1
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = if (isCustomActive) Icons.Default.CheckCircle else Icons.Default.Edit,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp)
                    )
                },
                modifier = Modifier.testTag("${tagPrefix}_chip_custom")
            )

            // Reset Option Chip right after Custom Chip
            AssistChip(
                onClick = onResetUsage,
                label = {
                    Text(
                        text = "Reset",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = RoseError,
                        maxLines = 1
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.RestartAlt,
                        contentDescription = "Reset timer",
                        tint = RoseError,
                        modifier = Modifier.size(14.dp)
                    )
                },
                colors = AssistChipDefaults.assistChipColors(
                    containerColor = RoseError.copy(alpha = 0.08f)
                ),
                border = AssistChipDefaults.assistChipBorder(
                    enabled = true,
                    borderColor = RoseError.copy(alpha = 0.3f)
                ),
                modifier = Modifier.testTag("${tagPrefix}_chip_reset")
            )
        }
    }

    if (showCustomDialog) {
        var hourInput by remember {
            mutableStateOf(if (limitMinutes > 0) (limitMinutes / 60).let { if (it > 0) it.toString() else "" } else "")
        }
        var minInput by remember {
            mutableStateOf(if (limitMinutes > 0) (limitMinutes % 60).let { if (it > 0) it.toString() else "" } else "")
        }

        val parsedHours = hourInput.toIntOrNull() ?: 0
        val parsedMins = minInput.toIntOrNull() ?: 0
        val totalCalculatedMinutes = (parsedHours * 60) + parsedMins

        AlertDialog(
            onDismissRequest = { showCustomDialog = false },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Timer,
                        contentDescription = null,
                        tint = IndigoPrimary
                    )
                    Text(
                        text = "Custom Limit ($title)",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "ঘণ্টা (Hour) এবং মিনিট (Minute) নির্ধারণ করুন:",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    // Hours and Minutes Input Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Hour Field
                        OutlinedTextField(
                            value = hourInput,
                            onValueChange = { input ->
                                if (input.all { it.isDigit() } && input.length <= 2) {
                                    hourInput = input
                                }
                            },
                            label = { Text("ঘণ্টা (Hour)") },
                            placeholder = { Text("0") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("${tagPrefix}_input_hours")
                        )

                        // Minute Field
                        OutlinedTextField(
                            value = minInput,
                            onValueChange = { input ->
                                if (input.all { it.isDigit() } && input.length <= 3) {
                                    minInput = input
                                }
                            },
                            label = { Text("মিনিট (Minute)") },
                            placeholder = { Text("30") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("${tagPrefix}_input_minutes")
                        )
                    }

                    // Quick-Add Presets inside Dialog
                    Text(
                        text = "দ্রুত যোগ করুন (Quick Add):",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        SuggestionChip(
                            onClick = {
                                val currentTotal = totalCalculatedMinutes + 15
                                hourInput = (currentTotal / 60).let { if (it > 0) it.toString() else "" }
                                minInput = (currentTotal % 60).let { if (it > 0) it.toString() else "" }
                            },
                            label = { Text("+15 min") }
                        )
                        SuggestionChip(
                            onClick = {
                                val currentTotal = totalCalculatedMinutes + 30
                                hourInput = (currentTotal / 60).let { if (it > 0) it.toString() else "" }
                                minInput = (currentTotal % 60).let { if (it > 0) it.toString() else "" }
                            },
                            label = { Text("+30 min") }
                        )
                        SuggestionChip(
                            onClick = {
                                val currentTotal = totalCalculatedMinutes + 60
                                hourInput = (currentTotal / 60).toString()
                                minInput = (currentTotal % 60).let { if (it > 0) it.toString() else "" }
                            },
                            label = { Text("+1 hour") }
                        )
                        SuggestionChip(
                            onClick = {
                                val currentTotal = totalCalculatedMinutes + 120
                                hourInput = (currentTotal / 60).toString()
                                minInput = (currentTotal % 60).let { if (it > 0) it.toString() else "" }
                            },
                            label = { Text("+2 hour") }
                        )
                        SuggestionChip(
                            onClick = {
                                hourInput = ""
                                minInput = ""
                            },
                            label = { Text("Clear", color = RoseError) }
                        )
                    }

                    // Calculation & Summary Card
                    Surface(
                        color = IndigoPrimary.copy(alpha = 0.08f),
                        shape = RoundedCornerShape(10.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, IndigoPrimary.copy(alpha = 0.2f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "মোট দৈনিক সময়সীমা:",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            val summary = when {
                                totalCalculatedMinutes <= 0 -> "Unlimited (0 মিনিট)"
                                parsedHours > 0 && parsedMins > 0 -> "$parsedHours ঘণ্টা $parsedMins মিনিট ($totalCalculatedMinutes মিনিট)"
                                parsedHours > 0 -> "$parsedHours ঘণ্টা ($totalCalculatedMinutes মিনিট)"
                                else -> "$parsedMins মিনিট"
                            }
                            Text(
                                text = summary,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = IndigoPrimary
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onSelectLimit(totalCalculatedMinutes)
                        showCustomDialog = false
                    },
                    modifier = Modifier.testTag("${tagPrefix}_save_custom_btn")
                ) {
                    Text("সেভ করুন (Save)")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCustomDialog = false }) {
                    Text("বাতিল (Cancel)")
                }
            }
        )
    }
}
