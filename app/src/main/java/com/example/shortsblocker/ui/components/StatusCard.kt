package com.example.shortsblocker.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.shortsblocker.ShortsBlockerUiState
import com.example.shortsblocker.ui.theme.*

@Composable
fun StatusCard(
    uiState: ShortsBlockerUiState,
    onOpenSettings: () -> Unit,
    onToggleMaster: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val isFullyActive = uiState.isAccessibilityServiceActive && uiState.isMasterEnabled

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("status_card"),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = when {
                !uiState.isAccessibilityServiceActive -> AmberWarningContainer
                isFullyActive -> IndigoContainer
                else -> MaterialTheme.colorScheme.surfaceVariant
            }
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(
                                when {
                                    !uiState.isAccessibilityServiceActive -> AmberWarning
                                    isFullyActive -> IndigoPrimary
                                    else -> MaterialTheme.colorScheme.outline
                                }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = when {
                                !uiState.isAccessibilityServiceActive -> Icons.Default.Warning
                                isFullyActive -> Icons.Default.CheckCircle
                                else -> Icons.Default.PowerSettingsNew
                            },
                            contentDescription = "Status Icon",
                            tint = ColorSchemeUtils.contentColorFor(
                                when {
                                    !uiState.isAccessibilityServiceActive -> AmberWarning
                                    isFullyActive -> IndigoPrimary
                                    else -> MaterialTheme.colorScheme.outline
                                }
                            ),
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    Column {
                        Text(
                            text = when {
                                !uiState.isAccessibilityServiceActive -> "Permission Required"
                                isFullyActive -> "Shorts Blocking Active"
                                else -> "Shorts Blocking Paused"
                            },
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = when {
                                !uiState.isAccessibilityServiceActive -> AmberWarning
                                isFullyActive -> IndigoOnContainer
                                else -> MaterialTheme.colorScheme.onSurface
                            }
                        )
                        Text(
                            text = when {
                                !uiState.isAccessibilityServiceActive -> "Accessibility service is OFF"
                                isFullyActive -> "Auto-closing Shorts & Reels"
                                else -> "Master switch is turned OFF"
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Master Toggle Switch
                Switch(
                    checked = uiState.isMasterEnabled,
                    onCheckedChange = onToggleMaster,
                    modifier = Modifier.testTag("master_toggle_switch")
                )
            }

            AnimatedVisibility(visible = !uiState.isAccessibilityServiceActive) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Android requires the Accessibility Service to be turned ON in system settings for ShortsBlocker to detect and close short-form video screens.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Button(
                        onClick = onOpenSettings,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("enable_accessibility_button"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AmberWarning,
                            contentColor = IndigoOnPrimary
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Enable in Accessibility Settings",
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

object ColorSchemeUtils {
    fun contentColorFor(background: androidx.compose.ui.graphics.Color): androidx.compose.ui.graphics.Color {
        return androidx.compose.ui.graphics.Color.White
    }
}
