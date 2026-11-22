package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.AssistantSettings
import com.example.ui.AiConnectionState
import com.example.ui.LocalizedCopy
import com.example.ui.ParmarUiState
import com.example.ui.components.FuturisticCard
import com.example.ui.theme.AmberOffline
import com.example.ui.theme.CoolSlate
import com.example.ui.theme.CrispIce
import com.example.ui.theme.CrimsonAlert
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.ElevatedCard
import com.example.ui.theme.EmeraldLive
import com.example.ui.theme.ObsidianBlack
import com.example.ui.theme.SubtleBorder
import com.example.voice.VoiceOption

@Composable
fun SettingsScreen(
    settings: AssistantSettings,
    uiState: ParmarUiState,
    availableVoices: List<VoiceOption>,
    strings: LocalizedCopy,
    onUpdateSettings: ((AssistantSettings) -> AssistantSettings) -> Unit,
    onToggleBackgroundListening: (Boolean) -> Unit,
    onTestVoice: () -> Unit,
    onCalibrateVoiceProfile: () -> Unit,
    onTestAiConnection: () -> Unit,
    onClearAllMemories: () -> Unit,
    onAdjustVolume: (Int) -> Unit
) {
    var wakeWordInput by remember(settings.wakeWord) { mutableStateOf(settings.wakeWord) }
    var stopWordInput by remember(settings.stopWord) { mutableStateOf(settings.stopWord) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(ObsidianBlack)
            .testTag("settings_screen"),
        horizontalAlignment = Alignment.CenterHorizontally,
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Column(modifier = Modifier.widthIn(max = 600.dp).fillMaxWidth()) {
                Text(
                    text = strings.settingsTitle,
                    style = MaterialTheme.typography.titleLarge,
                    color = CrispIce
                )
            }
        }

        // 1. Wake Word & Stop Word
        item {
            Column(modifier = Modifier.widthIn(max = 600.dp).fillMaxWidth()) {
                FuturisticCard(highlightBorder = true) {
                    Text(
                        text = if (settings.language == "gu") "વેક વર્ડ અને સ્ટોપ વર્ડ" else "Wake Word & Stop Word",
                        style = MaterialTheme.typography.titleMedium,
                        color = ElectricBlue
                    )
                    Spacer(Modifier.height(10.dp))
                    OutlinedTextField(
                        value = wakeWordInput,
                        onValueChange = {
                            wakeWordInput = it
                            onUpdateSettings { s -> s.copy(wakeWord = it) }
                        },
                        label = { Text("Wake Word (Default: DIAMOND)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("wake_word_input")
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = stopWordInput,
                        onValueChange = {
                            stopWordInput = it
                            onUpdateSettings { s -> s.copy(stopWord = it) }
                        },
                        label = { Text("Stop Word (Default: DIAMOND)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("stop_word_input")
                    )
                }
            }
        }

        // 2. Language & Female Voice Settings
        item {
            Column(modifier = Modifier.widthIn(max = 600.dp).fillMaxWidth()) {
                FuturisticCard {
                    Text(
                        text = if (settings.language == "gu") "ભાષા અને સ્ત્રી અવાજ સેટિંગ્સ" else "Language & Female Voice",
                        style = MaterialTheme.typography.titleMedium,
                        color = ElectricBlue
                    )
                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        listOf("gu" to "ગુજરાતી (Default)", "en" to "English").forEach { (code, label) ->
                            val selected = settings.language == code
                            Surface(
                                color = if (selected) ElectricBlue else ElevatedCard,
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, if (selected) ElectricBlue else SubtleBorder),
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { onUpdateSettings { it.copy(language = code) } }
                                    .testTag("settings_lang_$code")
                            ) {
                                Text(
                                    text = label,
                                    color = if (selected) ObsidianBlack else CrispIce,
                                    style = MaterialTheme.typography.labelMedium,
                                    modifier = Modifier.padding(12.dp)
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = "Voice Speed: ${String.format("%.2fx", settings.voiceSpeed)}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = CrispIce
                    )
                    Slider(
                        value = settings.voiceSpeed,
                        onValueChange = { v -> onUpdateSettings { it.copy(voiceSpeed = v) } },
                        valueRange = 0.6f..1.5f,
                        colors = SliderDefaults.colors(thumbColor = ElectricBlue, activeTrackColor = ElectricBlue)
                    )

                    Text(
                        text = "Female Voice Pitch: ${String.format("%.2f", settings.voicePitch)}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = CrispIce
                    )
                    Slider(
                        value = settings.voicePitch,
                        onValueChange = { v -> onUpdateSettings { it.copy(voicePitch = v) } },
                        valueRange = 0.8f..1.35f,
                        colors = SliderDefaults.colors(thumbColor = ElectricBlue, activeTrackColor = ElectricBlue)
                    )

                    Text(
                        text = "Assistant Volume: ${uiState.currentVolumePercent}%",
                        style = MaterialTheme.typography.bodyMedium,
                        color = CrispIce
                    )
                    Slider(
                        value = uiState.currentVolumePercent.toFloat(),
                        onValueChange = { onAdjustVolume(it.toInt()) },
                        valueRange = 0f..100f,
                        colors = SliderDefaults.colors(thumbColor = ElectricBlue, activeTrackColor = ElectricBlue)
                    )

                    if (availableVoices.isNotEmpty()) {
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = if (settings.language == "gu") "ઉપલબ્ધ સ્ત્રી / પ્રાદેશિક અવાજો:" else "Installed Device Voices:",
                            style = MaterialTheme.typography.labelMedium,
                            color = CoolSlate
                        )
                        Spacer(Modifier.height(6.dp))
                        availableVoices.take(4).forEach { voice ->
                            val isChosen = settings.selectedVoiceName == voice.name
                            Surface(
                                color = if (isChosen) ElectricBlue.copy(alpha = 0.16f) else ElevatedCard,
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(1.dp, if (isChosen) ElectricBlue else SubtleBorder),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 3.dp)
                                    .clickable { onUpdateSettings { it.copy(selectedVoiceName = voice.name) } }
                            ) {
                                Text(
                                    text = "${voice.localeTag} • ${voice.name}" + if (voice.isFemaleHint) " (Female)" else "",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = CrispIce,
                                    modifier = Modifier.padding(10.dp)
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(10.dp))
                    Button(
                        onClick = onTestVoice,
                        colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue, contentColor = ObsidianBlack),
                        modifier = Modifier.fillMaxWidth().testTag("test_voice_button")
                    ) {
                        Icon(Icons.Rounded.PlayArrow, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text(if (settings.language == "gu") "અવાજ ચકાસો (Test Voice)" else "Test PARMAR Female Voice")
                    }
                }
            }
        }

        // 3. AI Service, Offline Mode & Background Listening
        item {
            Column(modifier = Modifier.widthIn(max = 600.dp).fillMaxWidth()) {
                FuturisticCard {
                    Text(
                        text = if (settings.language == "gu") "AI કનેક્શન, ઓફલાઇન અને બેકગ્રાઉન્ડ" else "AI Service, Offline & Background",
                        style = MaterialTheme.typography.titleMedium,
                        color = ElectricBlue
                    )
                    Spacer(Modifier.height(8.dp))

                    SettingSwitchRow(
                        title = if (settings.language == "gu") "ફોર્સ ઓફલાઇન મોડ" else "Force Offline Mode",
                        subtitle = if (settings.language == "gu") "માત્ર લોકલ કમાન્ડ, નોટ્સ અને રિમાઇન્ડર વાપરો" else "Disable cloud Gemini calls and use local engine only",
                        checked = settings.offlineModeForced,
                        testTag = "switch_offline_mode",
                        onCheckedChange = { v -> onUpdateSettings { it.copy(offlineModeForced = v) } }
                    )

                    SettingSwitchRow(
                        title = if (settings.language == "gu") "બેકગ્રાઉન્ડ લિસનિંગ (Foreground Service)" else "Background Wake Listening",
                        subtitle = if (settings.language == "gu") "ઓનગોઇંગ નોટિફિકેશન સાથે \"DIAMOND\" સાંભળે છે" else "Runs transparent microphone foreground service",
                        checked = settings.backgroundListeningEnabled,
                        testTag = "switch_bg_listening",
                        onCheckedChange = onToggleBackgroundListening
                    )

                    SettingSwitchRow(
                        title = if (settings.language == "gu") "સ્ક્રીન બંધ હોય ત્યારે વેક સપોર્ટ" else "Screen-Off Wake Lock",
                        subtitle = if (settings.language == "gu") "બેટરી ઓપ્ટિમાઇઝેશન છૂટ હોય ત્યારે સ્ક્રીન-ઓફ સપોર્ટ" else "Keep partial wake lock when permitted on OPPO A3 5G",
                        checked = settings.screenOffWakeEnabled,
                        testTag = "switch_screen_off_wake",
                        onCheckedChange = { v -> onUpdateSettings { it.copy(screenOffWakeEnabled = v) } }
                    )

                    Spacer(Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = onTestAiConnection,
                        border = BorderStroke(1.dp, ElectricBlue),
                        modifier = Modifier.fillMaxWidth().testTag("test_ai_connection_btn")
                    ) {
                        Text(
                            text = when (uiState.aiConnectionState) {
                                AiConnectionState.TESTING -> "Testing Gemini 3.8 Flash..."
                                AiConnectionState.CONNECTED -> uiState.aiConnectionMessage
                                AiConnectionState.ERROR -> "Error: ${uiState.aiConnectionMessage}"
                                AiConnectionState.IDLE -> if (settings.language == "gu") "AI કનેક્શન ટેસ્ટ કરો" else "Test Firebase AI Connection"
                            },
                            color = when (uiState.aiConnectionState) {
                                AiConnectionState.CONNECTED -> EmeraldLive
                                AiConnectionState.ERROR -> CrimsonAlert
                                else -> CrispIce
                            }
                        )
                    }
                }
            }
        }

        // 4. Optional Voice Profile & Privacy Controls
        item {
            Column(modifier = Modifier.widthIn(max = 600.dp).fillMaxWidth()) {
                FuturisticCard {
                    Text(
                        text = if (settings.language == "gu") "વોઇસ પ્રોફાઇલ અને પ્રાઇવસી કંટ્રોલ્સ" else "Voice Profile & Privacy Controls",
                        style = MaterialTheme.typography.titleMedium,
                        color = ElectricBlue
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = if (settings.language == "gu") {
                            "નોંધ: વોઇસ પ્રોફાઇલ માત્ર પર્સનલાઇઝેશન માટે છે, સુરક્ષા ચકાસણી માટે નહીં. સંવેદનશીલ કાર્યો માટે ડિવાઇસ PIN/બાયોમેટ્રિક જરૂરી છે."
                        } else {
                            "Voice profile is for personalization only, NOT identity verification. Sensitive actions require device PIN/biometric."
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = AmberOffline
                    )
                    Spacer(Modifier.height(8.dp))

                    OutlinedButton(
                        onClick = onCalibrateVoiceProfile,
                        border = BorderStroke(1.dp, ElectricBlue),
                        modifier = Modifier.fillMaxWidth().testTag("calibrate_voice_profile_btn")
                    ) {
                        Icon(Icons.Rounded.GraphicEq, contentDescription = null, tint = ElectricBlue)
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = if (settings.voiceProfileEnabled) {
                                "Recalibrate Voice Profile (${String.format("%.2f", settings.voiceProfileEnergyBaseline)})"
                            } else {
                                "Calibrate Optional Voice Profile"
                            },
                            color = CrispIce
                        )
                    }

                    Spacer(Modifier.height(8.dp))
                    SettingSwitchRow(
                        title = if (settings.language == "gu") "ખાનગી મેમરી માટે ડિવાઇસ ઓથ" else "Require Device Auth for Private Data",
                        subtitle = if (settings.language == "gu") "બાયોમેટ્રિક / PIN ચકાસણી" else "Enforce Biometric/PIN for private memories",
                        checked = settings.requireAuthForPrivateActions,
                        testTag = "switch_require_auth",
                        onCheckedChange = { v -> onUpdateSettings { it.copy(requireAuthForPrivateActions = v) } }
                    )

                    Spacer(Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = onClearAllMemories,
                        border = BorderStroke(1.dp, CrimsonAlert),
                        modifier = Modifier.fillMaxWidth().testTag("clear_all_memories_btn")
                    ) {
                        Text(
                            text = if (settings.language == "gu") "બધી મેમરી ડિલીટ કરો" else "Erase All Saved Memories",
                            color = CrimsonAlert
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingSwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    testTag: String,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
            Text(text = title, style = MaterialTheme.typography.titleMedium, color = CrispIce)
            Text(text = subtitle, style = MaterialTheme.typography.bodyMedium, color = CoolSlate)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = ObsidianBlack,
                checkedTrackColor = ElectricBlue
            ),
            modifier = Modifier.testTag(testTag)
        )
    }
}
