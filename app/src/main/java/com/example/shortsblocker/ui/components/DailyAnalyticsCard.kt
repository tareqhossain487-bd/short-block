package com.example.shortsblocker.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.DonutLarge
import androidx.compose.material.icons.filled.HourglassBottom
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.shortsblocker.ShortsBlockerUiState
import com.example.shortsblocker.ui.theme.AmberWarning
import com.example.shortsblocker.ui.theme.EmeraldSuccess
import com.example.shortsblocker.ui.theme.IndigoPrimary
import com.example.shortsblocker.ui.theme.RoseError
import com.example.shortsblocker.ui.theme.VioletSecondary
import kotlin.math.max

enum class ChartDisplayMode(val label: String) {
    BAR_COMPARISON("Bar Chart"),
    DONUT_BALANCE("Donut Split"),
    PLATFORM_LIST("Breakdown")
}

data class PlatformDataPoint(
    val platformName: String,
    val spentMinutes: Float,
    val savedMinutes: Float,
    val blockCount: Int,
    val color: Color
)

@Composable
fun DailyAnalyticsCard(
    uiState: ShortsBlockerUiState,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableStateOf(ChartDisplayMode.BAR_COMPARISON) }
    var selectedPlatformIndex by remember { mutableStateOf<Int?>(null) }

    // Calculate Spent Minutes from recorded app usage seconds
    val ytSpentMinutes = (uiState.youtubeLimits.appTodayUsedSeconds / 60f)
    val fbSpentMinutes = (uiState.facebookLimits.appTodayUsedSeconds / 60f)
    val igSpentMinutes = (uiState.instagramLimits.appTodayUsedSeconds / 60f)
    val customSpentMinutes = (uiState.customApps.sumOf { it.todayUsedSeconds } / 60f)
    val totalSpentMinutes = ytSpentMinutes + fbSpentMinutes + igSpentMinutes + customSpentMinutes

    // Calculate Saved Minutes:
    // YouTube Shorts blocked: ~2.5 mins avoided per block
    val ytSavedMinutes = (uiState.youtubeBlockedCount * 2.5f)
    // Facebook Reels blocked: ~2.5 mins avoided per block
    val fbSavedMinutes = (uiState.facebookBlockedCount * 2.5f)
    // Instagram Reels blocked: ~2.5 mins avoided per block
    val igSavedMinutes = (uiState.instagramBlockedCount * 2.5f)
    // Adult & custom websites blocked: ~4.0 mins avoided per block
    val webSavedMinutes = (uiState.websiteBlockedCount * 4.0f)
    // Ads skipped / blocked: ~0.5 mins (30s) avoided per ad
    val adsSavedMinutes = (uiState.adsBlockedCount * 0.5f)

    val totalSavedMinutes = ytSavedMinutes + fbSavedMinutes + igSavedMinutes + webSavedMinutes + adsSavedMinutes

    // Focus Efficiency percentage
    val totalActivityTime = totalSpentMinutes + totalSavedMinutes
    val efficiencyPercent = if (totalActivityTime > 0) {
        ((totalSavedMinutes / totalActivityTime) * 100).toInt()
    } else {
        100
    }

    val dataPoints = remember(
        ytSpentMinutes, ytSavedMinutes, fbSpentMinutes, fbSavedMinutes,
        igSpentMinutes, igSavedMinutes, webSavedMinutes, adsSavedMinutes, customSpentMinutes,
        uiState.youtubeBlockedCount, uiState.facebookBlockedCount, uiState.instagramBlockedCount,
        uiState.websiteBlockedCount, uiState.adsBlockedCount
    ) {
        listOf(
            PlatformDataPoint(
                platformName = "YouTube",
                spentMinutes = ytSpentMinutes,
                savedMinutes = ytSavedMinutes,
                blockCount = uiState.youtubeBlockedCount,
                color = Color(0xFFEF4444)
            ),
            PlatformDataPoint(
                platformName = "Facebook",
                spentMinutes = fbSpentMinutes,
                savedMinutes = fbSavedMinutes,
                blockCount = uiState.facebookBlockedCount,
                color = Color(0xFF3B82F6)
            ),
            PlatformDataPoint(
                platformName = "Instagram",
                spentMinutes = igSpentMinutes,
                savedMinutes = igSavedMinutes,
                blockCount = uiState.instagramBlockedCount,
                color = Color(0xFFEC4899)
            ),
            PlatformDataPoint(
                platformName = "Websites",
                spentMinutes = 0f,
                savedMinutes = webSavedMinutes,
                blockCount = uiState.websiteBlockedCount,
                color = VioletSecondary
            ),
            PlatformDataPoint(
                platformName = "Ads",
                spentMinutes = 0f,
                savedMinutes = adsSavedMinutes,
                blockCount = uiState.adsBlockedCount,
                color = EmeraldSuccess
            )
        )
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("daily_analytics_card"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(IndigoPrimary.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Insights,
                            contentDescription = null,
                            tint = IndigoPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Column(modifier = Modifier.weight(1f, fill = false)) {
                        Text(
                            text = "Daily Time Analytics",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                        Text(
                            text = "Time Spent vs. Time Saved",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Efficiency Ratio Badge
                Surface(
                    color = EmeraldSuccess.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.TrendingUp,
                            contentDescription = null,
                            tint = EmeraldSuccess,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "$efficiencyPercent% Focus",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = EmeraldSuccess,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }
            }

            // High-Level Dual Metrics (Time Spent vs Time Saved)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Time Spent Box
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(16.dp))
                        .background(AmberWarning.copy(alpha = 0.08f))
                        .border(1.dp, AmberWarning.copy(alpha = 0.2f), RoundedCornerShape(16.dp))
                        .padding(12.dp)
                ) {
                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.HourglassBottom,
                                contentDescription = null,
                                tint = AmberWarning,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "Time Spent",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = formatMinutes(totalSpentMinutes),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Apps Screen Time",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            fontSize = 10.sp
                        )
                    }
                }

                // Time Saved Box
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(16.dp))
                        .background(EmeraldSuccess.copy(alpha = 0.08f))
                        .border(1.dp, EmeraldSuccess.copy(alpha = 0.2f), RoundedCornerShape(16.dp))
                        .padding(12.dp)
                ) {
                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.HourglassTop,
                                contentDescription = null,
                                tint = EmeraldSuccess,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "Time Saved",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = formatMinutes(totalSavedMinutes),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = EmeraldSuccess
                        )
                        Text(
                            text = "Doomscroll Prevented",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            fontSize = 10.sp
                        )
                    }
                }
            }

            // Mode Selector Tabs (Recharts style visualization switcher)
            SingleChoiceSegmentedButtonRow(
                modifier = Modifier.fillMaxWidth()
            ) {
                ChartDisplayMode.values().forEachIndexed { index, mode ->
                    SegmentedButton(
                        shape = SegmentedButtonDefaults.itemShape(index = index, count = ChartDisplayMode.values().size),
                        onClick = {
                            selectedTab = mode
                            selectedPlatformIndex = null
                        },
                        selected = selectedTab == mode,
                        icon = {
                            when (mode) {
                                ChartDisplayMode.BAR_COMPARISON -> Icon(Icons.Default.BarChart, contentDescription = null, modifier = Modifier.size(16.dp))
                                ChartDisplayMode.DONUT_BALANCE -> Icon(Icons.Default.DonutLarge, contentDescription = null, modifier = Modifier.size(16.dp))
                                ChartDisplayMode.PLATFORM_LIST -> Icon(Icons.Default.Timeline, contentDescription = null, modifier = Modifier.size(16.dp))
                            }
                        }
                    ) {
                        Text(text = mode.label, style = MaterialTheme.typography.labelSmall)
                    }
                }
            }

            // Legend indicators
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(AmberWarning)
                    )
                    Text(
                        text = "Time Spent",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.width(20.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(EmeraldSuccess)
                    )
                    Text(
                        text = "Time Saved",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Main Visualization Container
            AnimatedContent(
                targetState = selectedTab,
                label = "chartViewTransition"
            ) { mode ->
                when (mode) {
                    ChartDisplayMode.BAR_COMPARISON -> {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            RechartsStyleBarChart(
                                dataPoints = dataPoints,
                                selectedIndex = selectedPlatformIndex,
                                onSelectIndex = { selectedPlatformIndex = it }
                            )

                            // Tooltip / Selection detail box
                            if (selectedPlatformIndex != null && selectedPlatformIndex!! in dataPoints.indices) {
                                val item = dataPoints[selectedPlatformIndex!!]
                                PlatformDetailBanner(item)
                            } else {
                                Text(
                                    text = "💡 বারগুলোতে ট্যাপ করে বিস্তারিত সময় ও ব্লকিং বিশ্লেষণ দেখুন",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.fillMaxWidth(),
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }

                    ChartDisplayMode.DONUT_BALANCE -> {
                        DonutBalanceView(
                            spentMinutes = totalSpentMinutes,
                            savedMinutes = totalSavedMinutes,
                            efficiencyPercent = efficiencyPercent
                        )
                    }

                    ChartDisplayMode.PLATFORM_LIST -> {
                        PlatformBreakdownList(dataPoints = dataPoints)
                    }
                }
            }
        }
    }
}

@Composable
private fun RechartsStyleBarChart(
    dataPoints: List<PlatformDataPoint>,
    selectedIndex: Int?,
    onSelectIndex: (Int?) -> Unit,
    modifier: Modifier = Modifier
) {
    val maxMinuteValue = remember(dataPoints) {
        val maxVal = dataPoints.maxOfOrNull { max(it.spentMinutes, it.savedMinutes) } ?: 30f
        max(maxVal, 20f)
    }

    val animatedMax by animateFloatAsState(
        targetValue = maxMinuteValue,
        animationSpec = tween(durationMillis = 600),
        label = "maxValAnim"
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
            .padding(14.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(dataPoints) {
                        detectTapGestures { offset ->
                            val slotWidth = size.width / dataPoints.size
                            val clickedIndex = (offset.x / slotWidth).toInt()
                            if (clickedIndex in dataPoints.indices) {
                                if (selectedIndex == clickedIndex) {
                                    onSelectIndex(null)
                                } else {
                                    onSelectIndex(clickedIndex)
                                }
                            } else {
                                onSelectIndex(null)
                            }
                        }
                    }
            ) {
                val width = size.width
                val height = size.height
                val bottomAxisY = height - 24.dp.toPx()
                val chartAreaHeight = bottomAxisY - 10.dp.toPx()

                // Draw Horizontal Grid lines (Recharts style)
                val gridLines = 4
                for (i in 0..gridLines) {
                    val y = bottomAxisY - (chartAreaHeight * (i.toFloat() / gridLines))
                    drawLine(
                        color = Color.LightGray.copy(alpha = 0.3f),
                        start = Offset(0f, y),
                        end = Offset(width, y),
                        strokeWidth = 1.dp.toPx()
                    )
                }

                val slotWidth = width / dataPoints.size
                val barWidth = 14.dp.toPx()
                val barSpacing = 4.dp.toPx()

                dataPoints.forEachIndexed { index, dp ->
                    val centerX = (slotWidth * index) + (slotWidth / 2f)
                    val spentBarX = centerX - barWidth - (barSpacing / 2f)
                    val savedBarX = centerX + (barSpacing / 2f)

                    val spentHeight = (dp.spentMinutes / animatedMax) * chartAreaHeight
                    val savedHeight = (dp.savedMinutes / animatedMax) * chartAreaHeight

                    val isSelected = selectedIndex == index

                    // Highlight background for selected column
                    if (isSelected) {
                        drawRoundRect(
                            color = IndigoPrimary.copy(alpha = 0.1f),
                            topLeft = Offset(slotWidth * index + 2.dp.toPx(), 4.dp.toPx()),
                            size = Size(slotWidth - 4.dp.toPx(), bottomAxisY - 4.dp.toPx()),
                            cornerRadius = CornerRadius(8.dp.toPx())
                        )
                    }

                    // Draw Spent Bar (Amber)
                    if (spentHeight > 0f) {
                        drawRoundRect(
                            brush = Brush.verticalGradient(
                                colors = listOf(AmberWarning, AmberWarning.copy(alpha = 0.7f))
                            ),
                            topLeft = Offset(spentBarX, bottomAxisY - spentHeight),
                            size = Size(barWidth, spentHeight),
                            cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
                        )
                    } else {
                        // Empty tick
                        drawRoundRect(
                            color = Color.Gray.copy(alpha = 0.2f),
                            topLeft = Offset(spentBarX, bottomAxisY - 3.dp.toPx()),
                            size = Size(barWidth, 3.dp.toPx()),
                            cornerRadius = CornerRadius(2.dp.toPx())
                        )
                    }

                    // Draw Saved Bar (Emerald)
                    if (savedHeight > 0f) {
                        drawRoundRect(
                            brush = Brush.verticalGradient(
                                colors = listOf(EmeraldSuccess, EmeraldSuccess.copy(alpha = 0.7f))
                            ),
                            topLeft = Offset(savedBarX, bottomAxisY - savedHeight),
                            size = Size(barWidth, savedHeight),
                            cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
                        )
                    } else {
                        // Empty tick
                        drawRoundRect(
                            color = Color.Gray.copy(alpha = 0.2f),
                            topLeft = Offset(savedBarX, bottomAxisY - 3.dp.toPx()),
                            size = Size(barWidth, 3.dp.toPx()),
                            cornerRadius = CornerRadius(2.dp.toPx())
                        )
                    }
                }
            }

            // X-Axis Labels positioned underneath
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 2.dp),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                dataPoints.forEachIndexed { index, dp ->
                    val isSelected = selectedIndex == index
                    Text(
                        text = dp.platformName,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSelected) IndigoPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

@Composable
private fun PlatformDetailBanner(item: PlatformDataPoint) {
    Surface(
        color = item.color.copy(alpha = 0.08f),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, item.color.copy(alpha = 0.25f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = item.platformName,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = item.color
                )
                Text(
                    text = "Blocks: ${item.blockCount} বার প্রতিরোধ করা হয়েছে",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "ব্যবহৃত: ${formatMinutes(item.spentMinutes)}",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = AmberWarning
                    )
                    Text(
                        text = "বাঁচানো: ${formatMinutes(item.savedMinutes)}",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = EmeraldSuccess
                    )
                }
            }
        }
    }
}

@Composable
private fun DonutBalanceView(
    spentMinutes: Float,
    savedMinutes: Float,
    efficiencyPercent: Int
) {
    val total = spentMinutes + savedMinutes
    val sweepAngleSaved by animateFloatAsState(
        targetValue = if (total > 0) (savedMinutes / total) * 360f else 360f,
        animationSpec = tween(durationMillis = 800),
        label = "donutSweep"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Box(
            modifier = Modifier.size(160.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize().padding(12.dp)) {
                val strokeWidth = 18.dp.toPx()
                val radius = (size.minDimension - strokeWidth) / 2f
                val centerOffset = Offset(size.width / 2f, size.height / 2f)

                // Background track (Spent time)
                drawCircle(
                    color = AmberWarning.copy(alpha = 0.8f),
                    radius = radius,
                    center = centerOffset,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )

                // Foreground track (Saved time)
                if (sweepAngleSaved > 0f) {
                    drawArc(
                        brush = Brush.sweepGradient(
                            colors = listOf(EmeraldSuccess, IndigoPrimary, EmeraldSuccess)
                        ),
                        startAngle = -90f,
                        sweepAngle = sweepAngleSaved,
                        useCenter = false,
                        topLeft = Offset(centerOffset.x - radius, centerOffset.y - radius),
                        size = Size(radius * 2, radius * 2),
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                    )
                }
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "$efficiencyPercent%",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = EmeraldSuccess
                )
                Text(
                    text = "Time Saved",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Summary details row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = formatMinutes(spentMinutes),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = AmberWarning
                )
                Text(
                    text = "Total Screen Time",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            VerticalDivider(modifier = Modifier.height(32.dp))

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = formatMinutes(savedMinutes),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = EmeraldSuccess
                )
                Text(
                    text = "Total Saved Time",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun PlatformBreakdownList(dataPoints: List<PlatformDataPoint>) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        dataPoints.forEach { item ->
            val total = item.spentMinutes + item.savedMinutes
            val savedRatio = if (total > 0) item.savedMinutes / total else 1f

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(item.color)
                        )
                        Text(
                            text = item.platformName,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "Spent: ${formatMinutes(item.spentMinutes)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = AmberWarning
                        )
                        Text(
                            text = "Saved: ${formatMinutes(item.savedMinutes)}",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = EmeraldSuccess
                        )
                    }
                }

                // Dual progress visual bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color.LightGray.copy(alpha = 0.3f))
                ) {
                    if (item.spentMinutes > 0) {
                        Box(
                            modifier = Modifier
                                .weight(item.spentMinutes.coerceAtLeast(0.1f))
                                .fillMaxHeight()
                                .background(AmberWarning)
                        )
                    }
                    if (item.savedMinutes > 0) {
                        Box(
                            modifier = Modifier
                                .weight(item.savedMinutes.coerceAtLeast(0.1f))
                                .fillMaxHeight()
                                .background(EmeraldSuccess)
                        )
                    }
                }
            }
        }
    }
}

private fun formatMinutes(minutesFloat: Float): String {
    val totalMins = minutesFloat.toInt()
    val hours = totalMins / 60
    val mins = totalMins % 60
    return when {
        hours > 0 && mins > 0 -> "${hours}h ${mins}m"
        hours > 0 -> "${hours}h"
        mins > 0 -> "${mins}m"
        minutesFloat > 0 -> "< 1m"
        else -> "0m"
    }
}
