package com.example.shortsblocker

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.example.shortsblocker.ui.components.*
import com.example.shortsblocker.ui.theme.EmeraldSuccess
import com.example.shortsblocker.ui.theme.IndigoPrimary
import com.example.shortsblocker.ui.theme.RoseError
import com.example.shortsblocker.ui.theme.ShortsBlockerTheme

class MainActivity : ComponentActivity() {

    private val viewModel: ShortsBlockerViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        setContent {
            ShortsBlockerTheme {
                val uiState by viewModel.uiState.collectAsState()
                val lifecycleOwner = LocalLifecycleOwner.current

                // Automatically refresh status whenever app is resumed (e.g. returning from Settings)
                DisposableEffect(lifecycleOwner) {
                    val observer = LifecycleEventObserver { _, event ->
                        if (event == Lifecycle.Event.ON_RESUME) {
                            viewModel.refresh()
                        }
                    }
                    lifecycleOwner.lifecycle.addObserver(observer)
                    onDispose {
                        lifecycleOwner.lifecycle.removeObserver(observer)
                    }
                }

                ShortsBlockerApp(
                    uiState = uiState,
                    onOpenAccessibilitySettings = {
                        startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                    },
                    onToggleMaster = { viewModel.setMasterEnabled(it) },
                    onToggleYouTube = { viewModel.setBlockYouTube(it) },
                    onToggleFacebook = { viewModel.setBlockFacebook(it) },
                    onToggleInstagram = { viewModel.setBlockInstagram(it) },
                    onResetStats = { viewModel.resetStats() },
                    onSimulateTest = { viewModel.simulateTestBlock(it) }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShortsBlockerApp(
    uiState: ShortsBlockerUiState,
    onOpenAccessibilitySettings: () -> Unit,
    onToggleMaster: (Boolean) -> Unit,
    onToggleYouTube: (Boolean) -> Unit,
    onToggleFacebook: (Boolean) -> Unit,
    onToggleInstagram: (Boolean) -> Unit,
    onResetStats: () -> Unit,
    onSimulateTest: (String) -> Unit
) {
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(IndigoPrimary),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Shield,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        Text(
                            text = "ShortsBlocker",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(innerPadding),
            contentAlignment = Alignment.TopCenter
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 600.dp)
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(top = 8.dp, bottom = 32.dp)
            ) {
                item {
                    StatusCard(
                        uiState = uiState,
                        onOpenSettings = onOpenAccessibilitySettings,
                        onToggleMaster = onToggleMaster
                    )
                }

                item {
                    StatsDashboard(
                        uiState = uiState,
                        onResetStats = onResetStats
                    )
                }

                item {
                    PlatformToggles(
                        uiState = uiState,
                        onToggleYouTube = onToggleYouTube,
                        onToggleFacebook = onToggleFacebook,
                        onToggleInstagram = onToggleInstagram
                    )
                }

                item {
                    RecentActivityCard(
                        events = uiState.recentEvents,
                        onSimulateTest = onSimulateTest
                    )
                }

                item {
                    SetupGuideCard(
                        onOpenSettings = onOpenAccessibilitySettings
                    )
                }
            }
        }
    }
}
