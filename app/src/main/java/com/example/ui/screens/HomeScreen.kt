package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccessTime
import androidx.compose.material.icons.rounded.Alarm
import androidx.compose.material.icons.rounded.Bluetooth
import androidx.compose.material.icons.rounded.FlashlightOff
import androidx.compose.material.icons.rounded.FlashlightOn
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.Psychology
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.TaskAlt
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.AssistantSettings
import com.example.ui.AssistantPhase
import com.example.ui.LocalizedCopy
import com.example.ui.ParmarUiState
import com.example.ui.components.FuturisticCard
import com.example.ui.components.GlowingAiOrb
import com.example.ui.components.MemoryApprovalBanner
import com.example.ui.components.OrbVisualState
import com.example.ui.theme.CobaltGlow
import com.example.ui.theme.CoolSlate
import com.example.ui.theme.CrispIce
import com.example.ui.theme.DeepSpace
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.ElevatedCard
import com.example.ui.theme.MidnightSurface
import com.example.ui.theme.ObsidianBlack
import com.example.ui.theme.SubtleBorder

private data class QuickActionChip(
    val tag: String,
    val label: String,
    val icon: ImageVector,
    val active: Boolean = false,
    val onClick: () -> Unit
)

@Composable
fun HomeScreen(
    uiState: ParmarUiState,
    settings: AssistantSettings,
    strings: LocalizedCopy,
    pendingRemindersCount: Int,
    notesCount: Int,
    memoriesCount: Int,
    onOrbClick: () -> Unit,
    onSimulateDiamond: () -> Unit,
    onToggleStandbyMic: () -> Unit,
    onQuickAgenda: () -> Unit,
    onQuickTorch: () -> Unit,
    onQuickTime: () -> Unit,
    onQuickBluetooth: () -> Unit,
    onQuickWifi: () -> Unit,
    onQuickLock: () -> Unit,
    onNavigateToReminders: () -> Unit,
    onNavigateToMemory: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToPermissions: () -> Unit,
    onApproveMemory: () -> Unit,
    onDismissMemory: () -> Unit
) {
    val orbState = when {
        uiState.phase == AssistantPhase.LISTENING -> OrbVisualState.LISTENING
        uiState.phase == AssistantPhase.THINKING -> OrbVisualState.THINKING
        uiState.phase == AssistantPhase.SPEAKING || uiState.phase == AssistantPhase.GREETING -> OrbVisualState.SPEAKING
        uiState.isEffectiveOffline -> OrbVisualState.OFFLINE_IDLE
        else -> OrbVisualState.STANDBY
    }

    val quickChips = listOf(
        QuickActionChip("quick_agenda_btn", strings.actionAgenda, Icons.Rounded.TaskAlt, false, onQuickAgenda),
        QuickActionChip(
            "quick_torch_btn",
            strings.actionTorch,
            if (uiState.isFlashlightOn) Icons.Rounded.FlashlightOn else Icons.Rounded.FlashlightOff,
            uiState.isFlashlightOn,
            onQuickTorch
        ),
        QuickActionChip("quick_time_btn", strings.actionTime, Icons.Rounded.AccessTime, false, onQuickTime),
        QuickActionChip("quick_bt_btn", strings.actionBluetooth, Icons.Rounded.Bluetooth, false, onQuickBluetooth),
        QuickActionChip("quick_wifi_btn", strings.actionWifi, Icons.Rounded.Wifi, false, onQuickWifi),
        QuickActionChip("quick_lock_btn", strings.actionLock, Icons.Rounded.Lock, false, onQuickLock)
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(DeepSpace, ObsidianBlack, ObsidianBlack)
                )
            )
            .testTag("home_screen"),
        horizontalAlignment = Alignment.CenterHorizontally,
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        item {
            Column(
                modifier = Modifier
                    .widthIn(max = 600.dp)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "PARMAR AI",
                    style = MaterialTheme.typography.headlineMedium,
                    color = CrispIce,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = strings.wakePromptHint,
                    style = MaterialTheme.typography.bodyMedium,
                    color = CoolSlate,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(14.dp))

                GlowingAiOrb(
                    state = orbState,
                    audioLevel = uiState.audioLevel,
                    size = 216.dp,
                    testTag = "home_ai_orb",
                    onClick = onOrbClick
                )

                Spacer(Modifier.height(8.dp))
                Text(
                    text = when (uiState.phase) {
                        AssistantPhase.LISTENING -> if (settings.language == "gu") "સાંભળી રહી છું Sir..." else "Listening, Sir..."
                        AssistantPhase.THINKING -> if (settings.language == "gu") "વિચારી રહી છું Sir..." else "Processing, Sir..."
                        AssistantPhase.SPEAKING, AssistantPhase.GREETING -> "PARMAR Speaking..."
                        else -> strings.tapOrbHint
                    },
                    style = MaterialTheme.typography.labelMedium,
                    color = ElectricBlue
                )

                Spacer(Modifier.height(14.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Button(
                        onClick = onSimulateDiamond,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ElectricBlue,
                            contentColor = ObsidianBlack
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("home_wake_diamond_button")
                    ) {
                        Text(
                            text = strings.simulateDiamond,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    OutlinedButton(
                        onClick = onToggleStandbyMic,
                        border = BorderStroke(1.dp, ElectricBlue.copy(alpha = 0.6f)),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("home_standby_toggle_button")
                    ) {
                        Text(
                            text = if (uiState.isWakeStandbyActive || uiState.isServiceRunning) {
                                strings.stopListening
                            } else {
                                strings.micStandby
                            },
                            color = CrispIce,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }

        item {
            Spacer(Modifier.height(14.dp))
            MemoryApprovalBanner(
                visible = uiState.pendingMemory != null,
                key = uiState.pendingMemory?.key.orEmpty(),
                value = uiState.pendingMemory?.value.orEmpty(),
                language = settings.language,
                onApprove = onApproveMemory,
                onDismiss = onDismissMemory
            )
        }

        item {
            Box(
                modifier = Modifier
                    .widthIn(max = 600.dp)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                FuturisticCard(highlightBorder = true) {
                    Text(
                        text = if (uiState.lastActionBadge.isNotBlank()) "PARMAR • ${uiState.lastActionBadge}" else "PARMAR",
                        style = MaterialTheme.typography.labelMedium,
                        color = ElectricBlue
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = uiState.lastParmarReply,
                        style = MaterialTheme.typography.bodyLarge,
                        color = CrispIce
                    )
                }
            }
        }

        item {
            Spacer(Modifier.height(16.dp))
            Column(
                modifier = Modifier
                    .widthIn(max = 600.dp)
                    .fillMaxWidth()
            ) {
                Text(
                    text = strings.quickActionsTitle,
                    style = MaterialTheme.typography.titleMedium,
                    color = CrispIce,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
                Spacer(Modifier.height(10.dp))
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(quickChips, key = { it.tag }) { chip ->
                        Surface(
                            color = if (chip.active) ElectricBlue.copy(alpha = 0.2f) else ElevatedCard,
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(
                                1.dp,
                                if (chip.active) ElectricBlue else SubtleBorder
                            ),
                            modifier = Modifier
                                .testTag(chip.tag)
                                .clip(RoundedCornerShape(16.dp))
                                .clickable(onClick = chip.onClick)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = chip.icon,
                                    contentDescription = chip.label,
                                    tint = ElectricBlue,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = chip.label,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = CrispIce
                                )
                            }
                        }
                    }
                }
            }
        }

        item {
            Spacer(Modifier.height(16.dp))
            Column(
                modifier = Modifier
                    .widthIn(max = 600.dp)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    DashboardTile(
                        title = strings.remindersTitle,
                        subtitle = "$pendingRemindersCount active",
                        icon = Icons.Rounded.Alarm,
                        testTag = "tile_reminders",
                        onClick = onNavigateToReminders,
                        modifier = Modifier.weight(1f)
                    )
                    DashboardTile(
                        title = strings.memoryTitle,
                        subtitle = "$memoriesCount saved • $notesCount notes",
                        icon = Icons.Rounded.Psychology,
                        testTag = "tile_memory",
                        onClick = onNavigateToMemory,
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    DashboardTile(
                        title = strings.settingsTitle,
                        subtitle = "${settings.wakeWord} • ${if (settings.language == "gu") "ગુજરાતી" else "English"}",
                        icon = Icons.Rounded.Settings,
                        testTag = "tile_settings",
                        onClick = onNavigateToSettings,
                        modifier = Modifier.weight(1f)
                    )
                    DashboardTile(
                        title = strings.permissionsTitle,
                        subtitle = if (uiState.permissions.micGranted) "Mic Ready" else "Mic Needed",
                        icon = if (uiState.permissions.micGranted) Icons.Rounded.Security else Icons.Rounded.NotificationsActive,
                        testTag = "tile_permissions",
                        onClick = onNavigateToPermissions,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun DashboardTile(
    title: String,
    subtitle: String,
    icon: ImageVector,
    testTag: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = MidnightSurface,
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, SubtleBorder),
        modifier = modifier
            .testTag(testTag)
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = CobaltGlow,
                modifier = Modifier.size(22.dp)
            )
            Spacer(Modifier.height(10.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = CrispIce,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = CoolSlate,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
