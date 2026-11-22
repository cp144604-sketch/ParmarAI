package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Diamond
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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
import com.example.ui.theme.CrimsonAlert
import com.example.ui.theme.CrispIce
import com.example.ui.theme.DeepSpace
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.ObsidianBlack

@Composable
fun VoiceAssistantScreen(
    uiState: ParmarUiState,
    settings: AssistantSettings,
    strings: LocalizedCopy,
    onOrbClick: () -> Unit,
    onWakeDiamond: () -> Unit,
    onStopWordDiamond: () -> Unit,
    onStartListening: () -> Unit,
    onApproveMemory: () -> Unit,
    onDismissMemory: () -> Unit
) {
    val orbState = when (uiState.phase) {
        AssistantPhase.LISTENING -> OrbVisualState.LISTENING
        AssistantPhase.THINKING -> OrbVisualState.THINKING
        AssistantPhase.SPEAKING, AssistantPhase.GREETING -> OrbVisualState.SPEAKING
        else -> if (uiState.isEffectiveOffline) OrbVisualState.OFFLINE_IDLE else OrbVisualState.STANDBY
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(listOf(DeepSpace, ObsidianBlack))
            )
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .testTag("voice_assistant_screen"),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 600.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = strings.voiceTitle,
                style = MaterialTheme.typography.titleLarge,
                color = CrispIce
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "Wake: \"${settings.wakeWord}\"  •  Stop: \"${settings.stopWord}\"",
                style = MaterialTheme.typography.labelMedium,
                color = ElectricBlue
            )
            Spacer(Modifier.height(18.dp))

            GlowingAiOrb(
                state = orbState,
                audioLevel = uiState.audioLevel,
                size = 248.dp,
                testTag = "voice_screen_orb",
                onClick = onOrbClick
            )

            Spacer(Modifier.height(12.dp))

            // Audio Waveform Level Bar Strip
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.height(32.dp)
            ) {
                val multipliers = listOf(0.35f, 0.65f, 0.9f, 1.0f, 0.85f, 0.6f, 0.4f)
                multipliers.forEach { mult ->
                    val barHeight = (6f + (uiState.audioLevel * 24f * mult)).dp
                    Box(
                        modifier = Modifier
                            .width(5.dp)
                            .height(barHeight)
                            .background(
                                color = if (uiState.phase == AssistantPhase.LISTENING || uiState.phase == AssistantPhase.SPEAKING) {
                                    NeonCyan
                                } else {
                                    CobaltGlow.copy(alpha = 0.4f)
                                },
                                shape = RoundedCornerShape(50)
                            )
                    )
                }
            }

            Spacer(Modifier.height(12.dp))
            Text(
                text = strings.voiceStopWordHint,
                style = MaterialTheme.typography.bodyMedium,
                color = CoolSlate,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(16.dp))

            if (uiState.liveTranscript.isNotBlank()) {
                FuturisticCard {
                    Text(
                        text = if (settings.language == "gu") "તમે કહ્યું (Sir):" else "You said (Sir):",
                        style = MaterialTheme.typography.labelMedium,
                        color = CoolSlate
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = uiState.liveTranscript,
                        style = MaterialTheme.typography.bodyLarge,
                        color = CrispIce
                    )
                }
                Spacer(Modifier.height(10.dp))
            }

            FuturisticCard(highlightBorder = true) {
                Text(
                    text = "PARMAR",
                    style = MaterialTheme.typography.labelMedium,
                    color = ElectricBlue,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = uiState.lastParmarReply,
                    style = MaterialTheme.typography.bodyLarge,
                    color = CrispIce
                )
            }

            Spacer(Modifier.height(10.dp))
            MemoryApprovalBanner(
                visible = uiState.pendingMemory != null,
                key = uiState.pendingMemory?.key.orEmpty(),
                value = uiState.pendingMemory?.value.orEmpty(),
                language = settings.language,
                onApprove = onApproveMemory,
                onDismiss = onDismissMemory
            )

            Spacer(Modifier.height(16.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Button(
                    onClick = onStartListening,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ElectricBlue,
                        contentColor = ObsidianBlack
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp)
                        .testTag("voice_listen_button")
                ) {
                    Icon(Icons.Rounded.Mic, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text(strings.speakCommandBtn, fontWeight = FontWeight.Bold)
                }

                OutlinedButton(
                    onClick = onWakeDiamond,
                    border = BorderStroke(1.dp, ElectricBlue),
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp)
                        .testTag("voice_wake_diamond_btn")
                ) {
                    Icon(Icons.Rounded.Diamond, contentDescription = null, tint = ElectricBlue)
                    Spacer(Modifier.width(6.dp))
                    Text(settings.wakeWord, color = CrispIce)
                }
            }

            Spacer(Modifier.height(10.dp))
            OutlinedButton(
                onClick = onStopWordDiamond,
                border = BorderStroke(1.dp, CrimsonAlert.copy(alpha = 0.7f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("voice_stop_session_btn")
            ) {
                Icon(Icons.Rounded.Stop, contentDescription = null, tint = CrimsonAlert)
                Spacer(Modifier.width(6.dp))
                Text(
                    text = "${strings.stopListening} (\"${settings.stopWord}\")",
                    color = CrimsonAlert
                )
            }
        }
    }
}
