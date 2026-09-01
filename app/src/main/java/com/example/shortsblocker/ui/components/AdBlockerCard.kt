package com.example.shortsblocker.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.shortsblocker.BlockedDomain
import com.example.shortsblocker.ShortsBlockerUiState
import com.example.shortsblocker.ui.theme.AmberWarning
import com.example.shortsblocker.ui.theme.EmeraldSuccess
import com.example.shortsblocker.ui.theme.IndigoPrimary
import com.example.shortsblocker.ui.theme.RoseError

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AdBlockerCard(
    uiState: ShortsBlockerUiState,
    onToggleBlockAds: (Boolean) -> Unit,
    onToggleAutoSkipAds: (Boolean) -> Unit,
    onToggleBlockPopupAds: (Boolean) -> Unit,
    onAddCustomAdFilter: (String) -> Unit,
    onToggleCustomAdFilter: (String, Boolean) -> Unit,
    onRemoveCustomAdFilter: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var showAddDialog by remember { mutableStateOf(false) }
    var domainInputText by remember { mutableStateOf("") }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("ad_blocker_card"),
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
            // Header
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
                        imageVector = Icons.Default.Shield,
                        contentDescription = null,
                        tint = IndigoPrimary,
                        modifier = Modifier.size(24.dp)
                    )
                    Column {
                        Text(
                            text = "AdBlocker (বিজ্ঞাপন ব্লকার)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "পপ-আপ, ভিডিও অ্যাড ও ব্রাউজার বিজ্ঞাপন নিয়ন্ত্রণ",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (uiState.adsBlockedCount > 0) {
                        Surface(
                            color = IndigoPrimary.copy(alpha = 0.12f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = "${uiState.adsBlockedCount} Blocked",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = IndigoPrimary,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Surface(
                        color = if (uiState.blockAds) EmeraldSuccess.copy(alpha = 0.15f) else RoseError.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = if (uiState.blockAds) "Active" else "Disabled",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (uiState.blockAds) EmeraldSuccess else RoseError,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            // 1. Master AdBlock Switch
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Block,
                            contentDescription = null,
                            tint = IndigoPrimary,
                            modifier = Modifier.size(22.dp)
                        )
                        Column {
                            Text(
                                text = "Block Intrusive & Network Ads",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "ব্রাউজার ও পপ-আপ অ্যাড নেটওয়ার্ক স্বয়ংক্রিয়ভাবে ব্লক করবে",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Switch(
                        checked = uiState.blockAds,
                        onCheckedChange = onToggleBlockAds,
                        modifier = Modifier.testTag("toggle_master_block_ads")
                    )
                }
            }

            // 2. Auto-Skip Video Ads Switch
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = EmeraldSuccess.copy(alpha = 0.08f)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.FastForward,
                            contentDescription = null,
                            tint = EmeraldSuccess,
                            modifier = Modifier.size(22.dp)
                        )
                        Column {
                            Text(
                                text = "Auto-Skip Video Ads (অটো স্কিপ)",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "ইউটিউব ও অন্যান্য ভিডিওতে 'Skip Ad' বোতাম এলে সঙ্গে সঙ্গে স্কিপ করবে",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Switch(
                        checked = uiState.autoSkipVideoAds,
                        onCheckedChange = onToggleAutoSkipAds,
                        modifier = Modifier.testTag("toggle_auto_skip_video_ads")
                    )
                }
            }

            // 3. Block Browser Popups & Redirects Switch
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = AmberWarning.copy(alpha = 0.08f)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Public,
                            contentDescription = null,
                            tint = AmberWarning,
                            modifier = Modifier.size(22.dp)
                        )
                        Column {
                            Text(
                                text = "Block Popups & Betting Ads",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "অনাকাঙ্ক্ষিত পপ-আপ ও জুয়া/বিজ্ঞাপনের রিডাইরেক্ট আটকাবে",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Switch(
                        checked = uiState.blockPopupAds,
                        onCheckedChange = onToggleBlockPopupAds,
                        modifier = Modifier.testTag("toggle_block_popup_ads")
                    )
                }
            }

            HorizontalDivider(
                color = MaterialTheme.colorScheme.surfaceVariant,
                thickness = 1.dp
            )

            // 4. Custom Ad Filters
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Custom Ad Filters & Domains",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "কাস্টম বিজ্ঞাপনী ডোমেইন ফিল্টার",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                OutlinedButton(
                    onClick = {
                        domainInputText = ""
                        showAddDialog = true
                    },
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.testTag("add_custom_ad_filter_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Add Filter",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            if (uiState.customAdFilters.isEmpty()) {
                Text(
                    text = "কোনো নির্দিষ্ট অ্যাড নেটওয়ার্ক বা সাইট ফিল্টার করতে উপরে 'Add Filter' এ ক্লিক করুন (যেমন: adnetwork.com, popads.net)।",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    uiState.customAdFilters.forEach { item ->
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    modifier = Modifier.weight(1f),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Language,
                                        contentDescription = null,
                                        tint = if (item.isEnabled) IndigoPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(
                                        text = item.domain,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (item.isEnabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Switch(
                                        checked = item.isEnabled,
                                        onCheckedChange = { enabled ->
                                            onToggleCustomAdFilter(item.domain, enabled)
                                        },
                                        modifier = Modifier.testTag("toggle_ad_domain_${item.domain}")
                                    )

                                    IconButton(
                                        onClick = { onRemoveCustomAdFilter(item.domain) }
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.DeleteOutline,
                                            contentDescription = "Delete Ad Filter",
                                            tint = RoseError,
                                            modifier = Modifier.size(18.dp)
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

    // Add Ad Filter Dialog
    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = {
                Text(
                    text = "Add Custom Ad Filter",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "যে বিজ্ঞাপনী ডোমেইন বা কিওয়ার্ড ব্লক করতে চান তা লিখুন:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = domainInputText,
                        onValueChange = { domainInputText = it },
                        placeholder = { Text("e.g. popads.net or adserver") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("custom_ad_filter_input_field")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (domainInputText.isNotBlank()) {
                            onAddCustomAdFilter(domainInputText)
                            showAddDialog = false
                        }
                    },
                    modifier = Modifier.testTag("confirm_add_ad_filter_btn")
                ) {
                    Text("Add")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
