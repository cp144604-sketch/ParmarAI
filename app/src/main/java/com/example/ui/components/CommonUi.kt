package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BookmarkAdd
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.CloudDone
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.MicOff
import androidx.compose.material.icons.rounded.StopCircle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.ui.LocalizedCopy
import com.example.ui.theme.AmberOffline
import com.example.ui.theme.CobaltGlow
import com.example.ui.theme.CrispIce
import com.example.ui.theme.DeepSpace
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.ElevatedCard
import com.example.ui.theme.EmeraldLive
import com.example.ui.theme.MidnightSurface
import com.example.ui.theme.ObsidianBlack
import com.example.ui.theme.SubtleBorder

@Composable
fun ParmarStatusHeader(
    strings: LocalizedCopy,
    language: String,
    isOffline: Boolean,
    isMicListening: Boolean,
    onToggleLanguage: () -> Unit,
    onStopListening: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Network status pill
        Surface(
            color = if (isOffline) AmberOffline.copy(alpha = 0.14f) else CobaltGlow.copy(alpha = 0.16f),
            shape = RoundedCornerShape(50),
            border = BorderStroke(
                1.dp,
                if (isOffline) AmberOffline.copy(alpha = 0.6f) else ElectricBlue.copy(alpha = 0.45f)
            )
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = if (isOffline) Icons.Rounded.CloudOff else Icons.Rounded.CloudDone,
                    contentDescription = null,
                    tint = if (isOffline) AmberOffline else ElectricBlue,
                    modifier = Modifier.size(15.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = if (isOffline) strings.offlineBadge else strings.onlineBadge,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isOffline) AmberOffline else CrispIce,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Mic live status indicator + emergency stop button
            Surface(
                color = if (isMicListening) EmeraldLive.copy(alpha = 0.16f) else MidnightSurface,
                shape = RoundedCornerShape(50),
                border = BorderStroke(
                    1.dp,
                    if (isMicListening) EmeraldLive.copy(alpha = 0.65f) else SubtleBorder
                ),
                modifier = Modifier
                    .testTag("mic_status_pill")
                    .clickable(enabled = isMicListening, onClick = onStopListening)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (isMicListening) Icons.Rounded.Mic else Icons.Rounded.MicOff,
                        contentDescription = if (isMicListening) "Microphone active" else "Microphone off",
                        tint = if (isMicListening) EmeraldLive else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(15.dp)
                    )
                    if (isMicListening) {
                        Spacer(Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.Rounded.StopCircle,
                            contentDescription = strings.stopListening,
                            tint = EmeraldLive,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }

            // Language toggle pill GU / EN
            Surface(
                color = ElevatedCard,
                shape = RoundedCornerShape(50),
                border = BorderStroke(1.dp, ElectricBlue.copy(alpha = 0.5f)),
                modifier = Modifier
                    .testTag("language_toggle_pill")
                    .clip(RoundedCornerShape(50))
                    .clickable(onClick = onToggleLanguage)
            ) {
                Text(
                    text = if (language == "gu") "ગુજ" else "ENG",
                    style = MaterialTheme.typography.labelMedium,
                    color = ElectricBlue,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }
        }
    }
}

@Composable
fun FuturisticCard(
    modifier: Modifier = Modifier,
    highlightBorder: Boolean = false,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MidnightSurface),
        border = BorderStroke(
            width = 1.dp,
            brush = Brush.linearGradient(
                if (highlightBorder) {
                    listOf(ElectricBlue.copy(alpha = 0.7f), CobaltGlow.copy(alpha = 0.35f))
                } else {
                    listOf(SubtleBorder, DeepSpace)
                }
            )
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            content = content
        )
    }
}

@Composable
fun MemoryApprovalBanner(
    visible: Boolean,
    key: String,
    value: String,
    language: String,
    onApprove: () -> Unit,
    onDismiss: () -> Unit
) {
    AnimatedVisibility(visible = visible) {
        FuturisticCard(
            highlightBorder = true,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .background(ElectricBlue.copy(alpha = 0.16f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.BookmarkAdd,
                            contentDescription = null,
                            tint = ElectricBlue,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text(
                            text = if (language == "gu") "મેમરીમાં સેવ કરવું છે Sir?" else "Save to PARMAR Memory, Sir?",
                            style = MaterialTheme.typography.labelMedium,
                            color = ElectricBlue
                        )
                        Text(
                            text = "$key: $value",
                            style = MaterialTheme.typography.bodyMedium,
                            color = CrispIce,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Rounded.Close, contentDescription = "Dismiss", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(if (language == "gu") "રદ કરો" else "Ignore")
                }
                Button(
                    onClick = onApprove,
                    colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue, contentColor = ObsidianBlack),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("approve_memory_button")
                ) {
                    Text(if (language == "gu") "મંજૂર અને સેવ" else "Approve & Save")
                }
            }
        }
    }
}
