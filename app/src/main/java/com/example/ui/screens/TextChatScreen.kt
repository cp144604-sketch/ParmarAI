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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.AssistantSettings
import com.example.data.ChatMessageEntity
import com.example.ui.LocalizedCopy
import com.example.ui.ParmarUiState
import com.example.ui.components.MemoryApprovalBanner
import com.example.ui.theme.AmberOffline
import com.example.ui.theme.CobaltGlow
import com.example.ui.theme.CoolSlate
import com.example.ui.theme.CrispIce
import com.example.ui.theme.DeepAzure
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.ElevatedCard
import com.example.ui.theme.MidnightSurface
import com.example.ui.theme.ObsidianBlack
import com.example.ui.theme.SubtleBorder

@Composable
fun TextChatScreen(
    messages: List<ChatMessageEntity>,
    uiState: ParmarUiState,
    settings: AssistantSettings,
    strings: LocalizedCopy,
    onSendMessage: (String) -> Unit,
    onVoiceInput: () -> Unit,
    onClearChat: () -> Unit,
    onApproveMemory: () -> Unit,
    onDismissMemory: () -> Unit
) {
    var inputText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    val samplePrompts = if (settings.language == "gu") {
        listOf(
            "આજે શું કામ કરવાનું છે?",
            "ફ્લેશલાઇટ ચાલુ કર",
            "સમય કેટલો થયો?",
            "Speak in English"
        )
    } else {
        listOf(
            "What do I have today?",
            "Turn flashlight on",
            "What time is it?",
            "ગુજરાતીમાં બોલો"
        )
    }

    LaunchedEffect(messages.size, uiState.streamingDraft) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ObsidianBlack)
            .testTag("text_chat_screen"),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier
                .widthIn(max = 600.dp)
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = strings.chatTitle,
                style = MaterialTheme.typography.titleLarge,
                color = CrispIce
            )
            TextButton(
                onClick = onClearChat,
                modifier = Modifier.testTag("clear_chat_button")
            ) {
                Icon(Icons.Rounded.DeleteSweep, contentDescription = null, tint = CoolSlate, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(4.dp))
                Text(strings.clearChat, color = CoolSlate)
            }
        }

        MemoryApprovalBanner(
            visible = uiState.pendingMemory != null,
            key = uiState.pendingMemory?.key.orEmpty(),
            value = uiState.pendingMemory?.value.orEmpty(),
            language = settings.language,
            onApprove = onApproveMemory,
            onDismiss = onDismissMemory
        )

        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.widthIn(max = 600.dp).fillMaxWidth()
        ) {
            items(samplePrompts) { prompt ->
                Surface(
                    color = ElevatedCard,
                    shape = RoundedCornerShape(50),
                    border = BorderStroke(1.dp, SubtleBorder),
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .clickable { onSendMessage(prompt) }
                ) {
                    Text(
                        text = prompt,
                        style = MaterialTheme.typography.labelMedium,
                        color = ElectricBlue,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                    )
                }
            }
        }

        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .widthIn(max = 600.dp)
                .fillMaxWidth(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (messages.isEmpty() && uiState.streamingDraft.isBlank()) {
                item {
                    Surface(
                        color = MidnightSurface,
                        shape = RoundedCornerShape(18.dp),
                        border = BorderStroke(1.dp, ElectricBlue.copy(alpha = 0.4f))
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("PARMAR", style = MaterialTheme.typography.labelMedium, color = ElectricBlue)
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = strings.greetingTitle + " " +
                                    if (settings.language == "gu") "તમે અહીં ગુજરાતી અથવા English માં કંઈ પણ પૂછી શકો છો."
                                    else "Ask me anything in Gujarati or English, Sir.",
                                style = MaterialTheme.typography.bodyLarge,
                                color = CrispIce
                            )
                        }
                    }
                }
            }

            items(messages, key = { it.id }) { msg ->
                val isUser = msg.role == "user"
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = if (isUser) Alignment.CenterEnd else Alignment.CenterStart
                ) {
                    Surface(
                        color = if (isUser) DeepAzure else MidnightSurface,
                        shape = RoundedCornerShape(
                            topStart = 18.dp,
                            topEnd = 18.dp,
                            bottomStart = if (isUser) 18.dp else 4.dp,
                            bottomEnd = if (isUser) 4.dp else 18.dp
                        ),
                        border = BorderStroke(
                            1.dp,
                            if (isUser) CobaltGlow else SubtleBorder
                        ),
                        modifier = Modifier.widthIn(max = 320.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (isUser) "Sir" else "PARMAR",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (isUser) CrispIce else ElectricBlue,
                                    fontWeight = FontWeight.Bold
                                )
                                if (msg.isOfflineResponse) {
                                    Text(
                                        text = "• OFFLINE",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = AmberOffline
                                    )
                                }
                                if (!msg.actionTaken.isNullOrBlank()) {
                                    Text(
                                        text = "• ${msg.actionTaken}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = ElectricBlue
                                    )
                                }
                            }
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = msg.text,
                                style = MaterialTheme.typography.bodyLarge,
                                color = CrispIce
                            )
                        }
                    }
                }
            }

            if (uiState.streamingDraft.isNotBlank()) {
                item {
                    Surface(
                        color = MidnightSurface,
                        shape = RoundedCornerShape(18.dp),
                        border = BorderStroke(1.dp, ElectricBlue.copy(alpha = 0.6f))
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("PARMAR • ...", style = MaterialTheme.typography.labelSmall, color = ElectricBlue)
                            Spacer(Modifier.height(4.dp))
                            Text(uiState.streamingDraft, style = MaterialTheme.typography.bodyLarge, color = CrispIce)
                        }
                    }
                }
            }
        }

        // Bottom input bar
        Row(
            modifier = Modifier
                .widthIn(max = 600.dp)
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            IconButton(
                onClick = onVoiceInput,
                modifier = Modifier
                    .size(48.dp)
                    .background(ElevatedCard, RoundedCornerShape(14.dp))
                    .testTag("chat_mic_button")
            ) {
                Icon(Icons.Rounded.Mic, contentDescription = "Voice Input", tint = ElectricBlue)
            }

            OutlinedTextField(
                value = inputText,
                onValueChange = { inputText = it },
                placeholder = { Text(strings.chatInputPlaceholder, color = CoolSlate) },
                maxLines = 3,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = ElectricBlue,
                    unfocusedBorderColor = SubtleBorder,
                    focusedTextColor = CrispIce,
                    unfocusedTextColor = CrispIce
                ),
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier
                    .weight(1f)
                    .testTag("chat_text_input")
            )

            IconButton(
                onClick = {
                    val toSend = inputText.trim()
                    if (toSend.isNotEmpty() && !uiState.isAiBusy) {
                        inputText = ""
                        onSendMessage(toSend)
                    }
                },
                enabled = inputText.isNotBlank() && !uiState.isAiBusy,
                modifier = Modifier
                    .size(48.dp)
                    .background(ElectricBlue, RoundedCornerShape(14.dp))
                    .testTag("chat_send_button")
            ) {
                if (uiState.isAiBusy) {
                    CircularProgressIndicator(
                        color = ObsidianBlack,
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(20.dp)
                    )
                } else {
                    Icon(
                        Icons.AutoMirrored.Rounded.Send,
                        contentDescription = "Send",
                        tint = ObsidianBlack
                    )
                }
            }
        }
    }
}
