package com.example.shortsblocker.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FormatQuote
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.shortsblocker.BlockedDomain
import com.example.shortsblocker.ShortsBlockerUiState
import com.example.shortsblocker.ui.theme.EmeraldSuccess
import com.example.shortsblocker.ui.theme.IndigoPrimary
import com.example.shortsblocker.ui.theme.RoseError

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AdultAndWebsiteBlockerCard(
    uiState: ShortsBlockerUiState,
    onToggleBlockAdult: (Boolean) -> Unit,
    onAddCustomWebsite: (String) -> Unit,
    onToggleCustomWebsite: (String, Boolean) -> Unit,
    onRemoveCustomWebsite: (String) -> Unit,
    onUpdateReminderMessage: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var showAddWebsiteDialog by remember { mutableStateOf(false) }
    var showEditMessageDialog by remember { mutableStateOf(false) }
    var websiteInputText by remember { mutableStateOf("") }
    var messageInputText by remember { mutableStateOf("") }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("adult_and_website_blocker_card"),
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
                        tint = RoseError,
                        modifier = Modifier.size(24.dp)
                    )
                    Column {
                        Text(
                            text = "Adult Site & Web Blocker",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "ব্রাউজার পর্ন ও কাস্টম ওয়েবসাইট ব্লকার",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Surface(
                    color = if (uiState.blockAdultWebsites) EmeraldSuccess.copy(alpha = 0.15f) else RoseError.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = if (uiState.blockAdultWebsites) "Active" else "Disabled",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (uiState.blockAdultWebsites) EmeraldSuccess else RoseError,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            // 1. Master Adult / Porn Site Block Toggle
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
                            tint = RoseError,
                            modifier = Modifier.size(22.dp)
                        )
                        Column {
                            Text(
                                text = "Block All Adult & Porn Sites",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "সকল প্রাপ্তবয়স্ক ও পর্নো সাইট ব্রাউজারে স্বয়ংক্রিয়ভাবে বন্ধ করবে",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Switch(
                        checked = uiState.blockAdultWebsites,
                        onCheckedChange = onToggleBlockAdult,
                        modifier = Modifier.testTag("toggle_block_adult_websites")
                    )
                }
            }

            // 2. Custom Warning / Islamic Reminder Message Card
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = EmeraldSuccess.copy(alpha = 0.08f),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        messageInputText = uiState.reminderMessage
                        showEditMessageDialog = true
                    }
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
                            imageVector = Icons.Default.FormatQuote,
                            contentDescription = null,
                            tint = EmeraldSuccess,
                            modifier = Modifier.size(22.dp)
                        )
                        Column {
                            Text(
                                text = "সাইট ব্লক হলে যে মেসেজ দেখাবে:",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "\"${uiState.reminderMessage}\"",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    IconButton(
                        onClick = {
                            messageInputText = uiState.reminderMessage
                            showEditMessageDialog = true
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit Message",
                            tint = IndigoPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            HorizontalDivider(
                color = MaterialTheme.colorScheme.surfaceVariant,
                thickness = 1.dp
            )

            // 3. Custom Website Block Add & Manage
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Custom Blocked Websites",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )

                OutlinedButton(
                    onClick = {
                        websiteInputText = ""
                        showAddWebsiteDialog = true
                    },
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.testTag("add_custom_website_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Add Website",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            if (uiState.customBlockedWebsites.isEmpty()) {
                Text(
                    text = "কোনো নির্দিষ্ট ওয়েবসাইট ব্লক করতে চাইলে উপরে 'Add Website' বাটনে ক্লিক করে ডোমেইন যোগ করুন (যেমন: example.com, gambling.com)।",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                uiState.customBlockedWebsites.forEach { site ->
                    CustomWebsiteRow(
                        site = site,
                        onToggle = { onToggleCustomWebsite(site.domain, it) },
                        onRemove = { onRemoveCustomWebsite(site.domain) }
                    )
                }
            }
        }
    }

    // Dialog 1: Add Custom Website Dialog
    if (showAddWebsiteDialog) {
        AlertDialog(
            onDismissRequest = { showAddWebsiteDialog = false },
            title = {
                Text(text = "Add Website to Block", fontWeight = FontWeight.Bold)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Enter website URL or domain name to block:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    OutlinedTextField(
                        value = websiteInputText,
                        onValueChange = { websiteInputText = it },
                        label = { Text("Website Domain") },
                        placeholder = { Text("e.g. gambling.com, badsite.org") },
                        singleLine = true,
                        leadingIcon = {
                            Icon(imageVector = Icons.Default.Language, contentDescription = null)
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onAddCustomWebsite(websiteInputText)
                        showAddWebsiteDialog = false
                    },
                    enabled = websiteInputText.isNotBlank()
                ) {
                    Text("Add Block")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddWebsiteDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Dialog 2: Edit Custom Reminder Message Dialog
    if (showEditMessageDialog) {
        val messagePresets = listOf(
            "আল্লাহর দিকে ফিরে আসো",
            "আল্লাহ আপনাকে দেখছেন, পাপ পরিহার করুন",
            "পাপ ছেড়ে দিন, তওবা করুন",
            "নিজের সময় ও চোখকে হেফাজত করুন"
        )

        AlertDialog(
            onDismissRequest = { showEditMessageDialog = false },
            title = {
                Text(text = "Custom Reminder Message", fontWeight = FontWeight.Bold)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "ব্লক হওয়া ওয়েবসাইট খোলার সময় যে সতর্কতামূলক বার্তা স্ক্রিনে ভেসে উঠবে:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        messagePresets.forEach { preset ->
                            SuggestionChip(
                                onClick = { messageInputText = preset },
                                label = { Text(preset, style = MaterialTheme.typography.labelSmall) }
                            )
                        }
                    }

                    OutlinedTextField(
                        value = messageInputText,
                        onValueChange = { messageInputText = it },
                        label = { Text("Warning Message") },
                        placeholder = { Text("আল্লাহর দিকে ফিরে আসো") },
                        singleLine = false,
                        maxLines = 3,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onUpdateReminderMessage(messageInputText)
                        showEditMessageDialog = false
                    },
                    enabled = messageInputText.isNotBlank()
                ) {
                    Text("Save Message")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditMessageDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun CustomWebsiteRow(
    site: BlockedDomain,
    onToggle: (Boolean) -> Unit,
    onRemove: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("custom_website_row_${site.domain}"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Language,
                contentDescription = null,
                tint = RoseError,
                modifier = Modifier.size(20.dp)
            )

            Text(
                text = site.domain,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            IconButton(
                onClick = onRemove,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.DeleteOutline,
                    contentDescription = "Remove",
                    tint = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.size(18.dp)
                )
            }

            Switch(
                checked = site.isEnabled,
                onCheckedChange = onToggle,
                modifier = Modifier.testTag("toggle_custom_site_${site.domain}")
            )
        }
    }
}
