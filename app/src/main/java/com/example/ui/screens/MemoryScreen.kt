package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Backup
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.LockOpen
import androidx.compose.material.icons.rounded.Psychology
import androidx.compose.material.icons.rounded.Restore
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.data.MemoryEntity
import com.example.ui.LocalizedCopy
import com.example.ui.components.FuturisticCard
import com.example.ui.theme.AmberOffline
import com.example.ui.theme.CoolSlate
import com.example.ui.theme.CrispIce
import com.example.ui.theme.CrimsonAlert
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.ObsidianBlack

@Composable
fun MemoryScreen(
    memories: List<MemoryEntity>,
    privateUnlocked: Boolean,
    language: String,
    strings: LocalizedCopy,
    onAddMemory: (key: String, value: String, category: String, isPrivate: Boolean) -> Unit,
    onDeleteMemory: (Long) -> Unit,
    onUnlockPrivate: () -> Unit,
    onLockPrivate: () -> Unit,
    onExportBackup: () -> Unit,
    onImportBackup: () -> Unit
) {
    var showAddDialog by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ObsidianBlack)
            .testTag("memory_screen")
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 600.dp)
                .fillMaxSize()
                .align(Alignment.TopCenter)
                .padding(horizontal = 16.dp)
        ) {
            Text(
                text = strings.memoryTitle,
                style = MaterialTheme.typography.titleLarge,
                color = CrispIce,
                modifier = Modifier.padding(vertical = 8.dp)
            )

            // Backup & Restore Card
            FuturisticCard(highlightBorder = true) {
                Text(
                    text = if (language == "gu") "લોકલ બેકઅપ અને રિસ્ટોર" else "Local Backup & Restore",
                    style = MaterialTheme.typography.titleMedium,
                    color = ElectricBlue
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = if (language == "gu") {
                        "તમારી બધી નોટ્સ, રિમાઇન્ડર, મેમરી અને સેટિંગ્સ સુરક્ષિત JSON ફાઇલમાં સેવ કરો."
                    } else {
                        "Export or restore all Notes, Reminders, Memories, and Settings as a local JSON file."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = CoolSlate
                )
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(
                        onClick = onExportBackup,
                        colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue, contentColor = ObsidianBlack),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("export_backup_btn")
                    ) {
                        Icon(Icons.Rounded.Backup, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(strings.backupJson)
                    }
                    OutlinedButton(
                        onClick = onImportBackup,
                        border = BorderStroke(1.dp, ElectricBlue),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("import_backup_btn")
                    ) {
                        Icon(Icons.Rounded.Restore, contentDescription = null, tint = ElectricBlue, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(strings.restoreJson, color = CrispIce)
                    }
                }
            }

            Spacer(Modifier.height(10.dp))

            // Private memories lock toggle bar
            val hasPrivate = memories.any { it.isPrivate }
            if (hasPrivate) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    OutlinedButton(
                        onClick = if (privateUnlocked) onLockPrivate else onUnlockPrivate,
                        border = BorderStroke(1.dp, AmberOffline),
                        modifier = Modifier.testTag("toggle_private_memory_lock_btn")
                    ) {
                        Icon(
                            imageVector = if (privateUnlocked) Icons.Rounded.LockOpen else Icons.Rounded.Lock,
                            contentDescription = null,
                            tint = AmberOffline,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = if (privateUnlocked) {
                                if (language == "gu") "ખાનગી મેમરી લોક કરો" else "Lock Private"
                            } else {
                                strings.unlockPrivateMemories
                            },
                            color = AmberOffline
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
            }

            if (memories.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(24.dp)) {
                        Icon(
                            Icons.Rounded.Psychology,
                            contentDescription = null,
                            tint = ElectricBlue.copy(alpha = 0.6f),
                            modifier = Modifier.size(56.dp)
                        )
                        Spacer(Modifier.height(12.dp))
                        Text(
                            text = strings.noMemoryHint,
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
                    items(memories, key = { it.id }) { mem ->
                        val masked = mem.isPrivate && !privateUnlocked
                        FuturisticCard(highlightBorder = mem.isPrivate) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = mem.category.uppercase(),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = if (mem.isPrivate) AmberOffline else ElectricBlue
                                        )
                                        if (mem.isPrivate) {
                                            Spacer(Modifier.width(6.dp))
                                            Icon(
                                                Icons.Rounded.Lock,
                                                contentDescription = "Private",
                                                tint = AmberOffline,
                                                modifier = Modifier.size(13.dp)
                                            )
                                        }
                                    }
                                    Text(
                                        text = mem.key,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = CrispIce
                                    )
                                    Spacer(Modifier.height(2.dp))
                                    Text(
                                        text = if (masked) "•••••••••••• (Device Auth Required)" else mem.value,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = CoolSlate
                                    )
                                }
                                IconButton(
                                    onClick = { onDeleteMemory(mem.id) },
                                    enabled = !masked
                                ) {
                                    Icon(
                                        Icons.Rounded.Delete,
                                        contentDescription = "Delete Memory",
                                        tint = if (masked) CoolSlate.copy(alpha = 0.3f) else CrimsonAlert
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        FloatingActionButton(
            onClick = { showAddDialog = true },
            containerColor = ElectricBlue,
            contentColor = ObsidianBlack,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(20.dp)
                .testTag("add_memory_fab")
        ) {
            Icon(Icons.Rounded.Add, contentDescription = strings.addMemory)
        }
    }

    if (showAddDialog) {
        AddMemoryDialog(
            language = language,
            onDismiss = { showAddDialog = false },
            onSave = { k, v, c, priv ->
                onAddMemory(k, v, c, priv)
                showAddDialog = false
            }
        )
    }
}

@Composable
private fun AddMemoryDialog(
    language: String,
    onDismiss: () -> Unit,
    onSave: (String, String, String, Boolean) -> Unit
) {
    var key by remember { mutableStateOf("") }
    var value by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("Preference") }
    var isPrivate by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (language == "gu") "નવી મેમરી ઉમેરો" else "Add Approved Memory") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = key,
                    onValueChange = { key = it },
                    label = { Text(if (language == "gu") "વિષય (દા.ત. ચા પસંદ)" else "Topic / Key") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("memory_key_input")
                )
                OutlinedTextField(
                    value = value,
                    onValueChange = { value = it },
                    label = { Text(if (language == "gu") "માહિતી" else "Detail / Value") },
                    modifier = Modifier.fillMaxWidth().testTag("memory_value_input")
                )
                OutlinedTextField(
                    value = category,
                    onValueChange = { category = it },
                    label = { Text(if (language == "gu") "કેટેગરી" else "Category") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = isPrivate, onCheckedChange = { isPrivate = it })
                    Text(
                        text = if (language == "gu") "ખાનગી મેમરી (ડિવાઇસ લોકથી સુરક્ષિત)" else "Private memory (requires device auth)",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { if (key.isNotBlank() && value.isNotBlank()) onSave(key, value, category, isPrivate) },
                colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue, contentColor = ObsidianBlack),
                modifier = Modifier.testTag("save_memory_dialog_btn")
            ) {
                Text(if (language == "gu") "સેવ કરો" else "Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(if (language == "gu") "રદ કરો" else "Cancel")
            }
        }
    )
}
