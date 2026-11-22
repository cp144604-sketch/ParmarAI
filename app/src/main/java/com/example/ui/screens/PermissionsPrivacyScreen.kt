package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BatteryChargingFull
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.StopCircle
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.AssistantSettings
import com.example.ui.LocalizedCopy
import com.example.ui.ParmarUiState
import com.example.ui.components.FuturisticCard
import com.example.ui.theme.AmberOffline
import com.example.ui.theme.CoolSlate
import com.example.ui.theme.CrispIce
import com.example.ui.theme.CrimsonAlert
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.EmeraldLive
import com.example.ui.theme.ObsidianBlack

@Composable
fun PermissionsPrivacyScreen(
    uiState: ParmarUiState,
    settings: AssistantSettings,
    strings: LocalizedCopy,
    onRequestMicPermission: () -> Unit,
    onRequestNotificationPermission: () -> Unit,
    onRequestExactAlarm: () -> Unit,
    onRequestDeviceAdmin: () -> Unit,
    onOpenBatterySettings: () -> Unit,
    onOpenAppSettings: () -> Unit,
    onStopListeningKillSwitch: () -> Unit
) {
    val perms = uiState.permissions
    val isGu = settings.language == "gu"

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(ObsidianBlack)
            .testTag("permissions_privacy_screen"),
        horizontalAlignment = Alignment.CenterHorizontally,
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Column(modifier = Modifier.widthIn(max = 600.dp).fillMaxWidth()) {
                Text(
                    text = strings.permissionsTitle,
                    style = MaterialTheme.typography.titleLarge,
                    color = CrispIce
                )
            }
        }

        // Privacy Transparency Guarantee
        item {
            Column(modifier = Modifier.widthIn(max = 600.dp).fillMaxWidth()) {
                FuturisticCard(highlightBorder = true) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.Security, contentDescription = null, tint = ElectricBlue)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = if (isGu) "પ્રાઇવસી અને માઇક્રોફોન ખાતરી" else "Zero-Secret-Recording Guarantee",
                            style = MaterialTheme.typography.titleMedium,
                            color = ElectricBlue
                        )
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = if (isGu) {
                            "• PARMAR AI ગુપ્ત રીતે ઓડિયો રેકોર્ડ કે ઇન્ટરનેટ પર અપલોડ કરતું નથી.\n" +
                                "• વેક-વર્ડ \"DIAMOND\" ઓન-ડિવાઇસ Voice Activity Detector થી જ ચાલે છે.\n" +
                                "• બેકગ્રાઉન્ડ લિસનિંગ ચાલુ હોય ત્યારે હંમેશા સ્થાયી નોટિફિકેશન દેખાશે.\n" +
                                "• API ઓળખપત્ર Firebase App Check દ્વારા સુરક્ષિત છે."
                        } else {
                            "• PARMAR AI never secretly records or continuously uploads microphone audio.\n" +
                                "• \"DIAMOND\" wake-word detection runs via on-device VAD & speech matching.\n" +
                                "• Background microphone use displays a persistent Foreground Service notification.\n" +
                                "• AI requests are attested via Firebase App Check (no embedded API key)."
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = CrispIce
                    )
                    Spacer(Modifier.height(10.dp))
                    Button(
                        onClick = onStopListeningKillSwitch,
                        colors = ButtonDefaults.buttonColors(containerColor = CrimsonAlert, contentColor = CrispIce),
                        modifier = Modifier.fillMaxWidth().testTag("kill_switch_stop_mic_btn")
                    ) {
                        Icon(Icons.Rounded.StopCircle, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text(if (isGu) "તરત જ માઇક અને સર્વિસ બંધ કરો" else "Stop All Listening Immediately")
                    }
                }
            }
        }

        // Runtime & System Permissions
        item {
            Column(
                modifier = Modifier.widthIn(max = 600.dp).fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                PermissionRowCard(
                    title = if (isGu) "માઇક્રોફોન (RECORD_AUDIO)" else "Microphone Permission",
                    description = if (isGu) "વેક વર્ડ \"DIAMOND\" અને વોઇસ કમાન્ડ માટે જરૂરી" else "Required for wake word \"DIAMOND\" and voice commands",
                    granted = perms.micGranted,
                    icon = Icons.Rounded.Mic,
                    actionLabel = if (perms.micGranted) "Granted" else "Grant Mic",
                    testTag = "perm_mic_btn",
                    onAction = onRequestMicPermission
                )

                PermissionRowCard(
                    title = if (isGu) "નોટિફિકેશન (POST_NOTIFICATIONS)" else "Notifications Permission",
                    description = if (isGu) "માઇક ફોરગ્રાઉન્ડ સર્વિસ અને રિમાઇન્ડર એલર્ટ માટે" else "Required for foreground mic badge and reminder alerts",
                    granted = perms.notificationsGranted,
                    icon = Icons.Rounded.Notifications,
                    actionLabel = if (perms.notificationsGranted) "Granted" else "Allow",
                    testTag = "perm_notif_btn",
                    onAction = onRequestNotificationPermission
                )

                PermissionRowCard(
                    title = if (isGu) "ચોક્કસ રિમાઇન્ડર એલાર્મ (SCHEDULE_EXACT_ALARM)" else "Exact Reminder Alarms",
                    description = if (isGu) "સમયસર લોકલ રિમાઇન્ડર વગાડવા માટે" else "Allows exact timing for scheduled local reminders",
                    granted = perms.exactAlarmAllowed,
                    icon = Icons.Rounded.Notifications,
                    actionLabel = if (perms.exactAlarmAllowed) "Allowed" else "Configure",
                    testTag = "perm_exact_alarm_btn",
                    onAction = onRequestExactAlarm
                )

                PermissionRowCard(
                    title = if (isGu) "સ્ક્રીન લોક એડમિન (Device Admin)" else "Screen Lock (Device Admin)",
                    description = if (isGu) "માત્ર તમારા કમાન્ડ પર ફોન લોક કરવા માટે (PIN બાયપાસ વિના)" else "Optional official Android API to lock screen on command",
                    granted = perms.deviceAdminActive,
                    icon = Icons.Rounded.Lock,
                    actionLabel = if (perms.deviceAdminActive) "Active" else "Enable Admin",
                    testTag = "perm_device_admin_btn",
                    onAction = onRequestDeviceAdmin
                )
            }
        }

        // OPPO A3 5G / ColorOS Guidance Card
        item {
            Column(modifier = Modifier.widthIn(max = 600.dp).fillMaxWidth()) {
                FuturisticCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.BatteryChargingFull, contentDescription = null, tint = ElectricBlue)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = if (isGu) "OPPO A3 5G / ColorOS બેટરી ગાઇડ" else "OPPO A3 5G / ColorOS Background Setup",
                            style = MaterialTheme.typography.titleMedium,
                            color = ElectricBlue
                        )
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = if (isGu) {
                            "OPPO A3 5G (ColorOS / Android 14-15) પર સ્ક્રીન બંધ હોય ત્યારે પણ વિશ્વસનીય કામગીરી માટે:\n" +
                                "1. Battery Optimization માં PARMAR AI માટે 'Allow background activity / Don't optimize' પસંદ કરો.\n" +
                                "2. App Info માં 'Auto-launch' મંજૂર કરો.\n" +
                                "3. Android નીચેની સુરક્ષા નીતિ મુજબ સ્ક્રીન લોક કે બાયોમેટ્રિક ક્યારેય બાયપાસ થતું નથી."
                        } else {
                            "For reliable background & screen-off wake-word operation on OPPO A3 5G (ColorOS):\n" +
                                "1. Set Battery Usage to Unrestricted / Don't Optimize for PARMAR AI.\n" +
                                "2. Enable Auto-Launch in App Info if desired.\n" +
                                "3. Android OS policies prevent bypassing lockscreen PIN/biometrics."
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = CoolSlate
                    )
                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedButton(
                            onClick = onOpenBatterySettings,
                            border = BorderStroke(1.dp, ElectricBlue),
                            modifier = Modifier.weight(1f).testTag("open_battery_settings_btn")
                        ) {
                            Text("Battery Settings", color = CrispIce)
                        }
                        OutlinedButton(
                            onClick = onOpenAppSettings,
                            border = BorderStroke(1.dp, ElectricBlue),
                            modifier = Modifier.weight(1f).testTag("open_app_settings_btn")
                        ) {
                            Text("App Info", color = CrispIce)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PermissionRowCard(
    title: String,
    description: String,
    granted: Boolean,
    icon: ImageVector,
    actionLabel: String,
    testTag: String,
    onAction: () -> Unit
) {
    FuturisticCard(highlightBorder = !granted) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (granted) EmeraldLive else AmberOffline
                )
                Spacer(Modifier.width(10.dp))
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = title, style = MaterialTheme.typography.titleMedium, color = CrispIce)
                        Spacer(Modifier.width(6.dp))
                        Icon(
                            imageVector = if (granted) Icons.Rounded.CheckCircle else Icons.Rounded.WarningAmber,
                            contentDescription = null,
                            tint = if (granted) EmeraldLive else AmberOffline
                        )
                    }
                    Text(text = description, style = MaterialTheme.typography.bodyMedium, color = CoolSlate)
                }
            }
            Spacer(Modifier.width(8.dp))
            OutlinedButton(
                onClick = onAction,
                border = BorderStroke(1.dp, if (granted) EmeraldLive else ElectricBlue),
                modifier = Modifier.testTag(testTag)
            ) {
                Text(actionLabel, color = if (granted) EmeraldLive else ElectricBlue)
            }
        }
    }
}
