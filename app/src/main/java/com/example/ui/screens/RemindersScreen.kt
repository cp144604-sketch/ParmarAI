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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Alarm
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.example.data.ReminderEntity
import com.example.ui.LocalizedCopy
import com.example.ui.components.FuturisticCard
import com.example.ui.theme.CoolSlate
import com.example.ui.theme.CrispIce
import com.example.ui.theme.CrimsonAlert
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.ElevatedCard
import com.example.ui.theme.EmeraldLive
import com.example.ui.theme.ObsidianBlack
import com.example.ui.theme.SubtleBorder
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun RemindersScreen(
    reminders: List<ReminderEntity>,
    language: String,
    strings: LocalizedCopy,
    onCreateReminder: (title: String, triggerAtMillis: Long, repeatType: String) -> Unit,
    onToggleComplete: (ReminderEntity) -> Unit,
    onDeleteReminder: (Long) -> Unit
) {
    var showCreateDialog by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ObsidianBlack)
            .testTag("reminders_screen")
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 600.dp)
                .fillMaxSize()
                .align(Alignment.TopCenter)
                .padding(horizontal = 16.dp)
        ) {
            Text(
                text = strings.remindersTitle,
                style = MaterialTheme.typography.titleLarge,
                color = CrispIce,
                modifier = Modifier.padding(vertical = 8.dp)
            )

            if (reminders.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(24.dp)) {
                        Icon(
                            Icons.Rounded.Alarm,
                            contentDescription = null,
                            tint = ElectricBlue.copy(alpha = 0.6f),
                            modifier = Modifier.size(56.dp)
                        )
                        Spacer(Modifier.height(12.dp))
                        Text(
                            text = strings.noRemindersHint,
                            style = MaterialTheme.typography.bodyMedium,
                            color = CoolSlate,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 88.dp)
                ) {
                    items(reminders, key = { it.id }) { item ->
                        val formattedTime = remember(item.triggerAtMillis) {
                            SimpleDateFormat("dd MMM • hh:mm a", Locale.getDefault())
                                .format(Date(item.triggerAtMillis))
                        }
                        FuturisticCard(highlightBorder = !item.isCompleted) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(onClick = { onToggleComplete(item) }) {
                                    Icon(
                                        imageVector = if (item.isCompleted) Icons.Rounded.CheckCircle else Icons.Rounded.RadioButtonUnchecked,
                                        contentDescription = "Toggle Reminder",
                                        tint = if (item.isCompleted) EmeraldLive else ElectricBlue
                                    )
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = item.title,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = if (item.isCompleted) CoolSlate else CrispIce,
                                        textDecoration = if (item.isCompleted) TextDecoration.LineThrough else null
                                    )
                                    Text(
                                        text = "$formattedTime • ${item.repeatType}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = if (item.isCompleted) CoolSlate else ElectricBlue
                                    )
                                }
                                IconButton(onClick = { onDeleteReminder(item.id) }) {
                                    Icon(Icons.Rounded.Delete, contentDescription = "Delete Reminder", tint = CrimsonAlert)
                                }
                            }
                        }
                    }
                }
            }
        }

        FloatingActionButton(
            onClick = { showCreateDialog = true },
            containerColor = ElectricBlue,
            contentColor = ObsidianBlack,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(20.dp)
                .testTag("add_reminder_fab")
        ) {
            Icon(Icons.Rounded.Add, contentDescription = strings.addReminder)
        }
    }

    if (showCreateDialog) {
        CreateReminderDialog(
            language = language,
            onDismiss = { showCreateDialog = false },
            onSave = { title, delayMinutes, repeat ->
                val triggerAt = System.currentTimeMillis() + delayMinutes * 60_000L
                onCreateReminder(title, triggerAt, repeat)
                showCreateDialog = false
            }
        )
    }
}

@Composable
private fun CreateReminderDialog(
    language: String,
    onDismiss: () -> Unit,
    onSave: (title: String, delayMinutes: Int, repeatType: String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var selectedMinutes by remember { mutableIntStateOf(15) }
    var repeatType by remember { mutableStateOf("ONCE") }

    val presets = listOf(5 to "5m", 15 to "15m", 60 to "1h", 180 to "3h", 1440 to "1d")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(if (language == "gu") "નવું લોકલ રિમાઇન્ડર" else "New Local Reminder")
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text(if (language == "gu") "શું યાદ અપાવવું?" else "Reminder title") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("reminder_title_input")
                )

                Text(
                    text = if (language == "gu") "સમય પસંદ કરો:" else "Remind me in:",
                    style = MaterialTheme.typography.labelMedium,
                    color = ElectricBlue
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    presets.forEach { (mins, label) ->
                        val active = selectedMinutes == mins
                        Surface(
                            color = if (active) ElectricBlue else ElevatedCard,
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, if (active) ElectricBlue else SubtleBorder),
                            modifier = Modifier
                                .weight(1f)
                                .clickable { selectedMinutes = mins }
                        ) {
                            Text(
                                text = label,
                                color = if (active) ObsidianBlack else CrispIce,
                                style = MaterialTheme.typography.labelMedium,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        }
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("ONCE", "DAILY").forEach { mode ->
                        val active = repeatType == mode
                        Surface(
                            color = if (active) ElectricBlue.copy(alpha = 0.2f) else ElevatedCard,
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, if (active) ElectricBlue else SubtleBorder),
                            modifier = Modifier
                                .weight(1f)
                                .clickable { repeatType = mode }
                        ) {
                            Text(
                                text = mode,
                                color = CrispIce,
                                style = MaterialTheme.typography.labelMedium,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { if (title.isNotBlank()) onSave(title, selectedMinutes, repeatType) },
                colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue, contentColor = ObsidianBlack),
                modifier = Modifier.testTag("save_reminder_dialog_btn")
            ) {
                Text(if (language == "gu") "સેટ કરો" else "Schedule")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(if (language == "gu") "રદ કરો" else "Cancel")
            }
        }
    )
}
