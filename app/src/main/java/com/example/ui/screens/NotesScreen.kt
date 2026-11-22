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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.NoteAlt
import androidx.compose.material.icons.rounded.PushPin
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import com.example.data.NoteEntity
import com.example.ui.LocalizedCopy
import com.example.ui.components.FuturisticCard
import com.example.ui.theme.CoolSlate
import com.example.ui.theme.CrispIce
import com.example.ui.theme.CrimsonAlert
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.ObsidianBlack

@Composable
fun NotesScreen(
    notes: List<NoteEntity>,
    language: String,
    strings: LocalizedCopy,
    onSaveNote: (id: Long?, title: String, content: String, category: String, isPinned: Boolean) -> Unit,
    onTogglePin: (NoteEntity) -> Unit,
    onDeleteNote: (Long) -> Unit
) {
    var query by remember { mutableStateOf("") }
    var editingNote by remember { mutableStateOf<NoteEntity?>(null) }
    var showDialog by remember { mutableStateOf(false) }

    val filtered = remember(notes, query) {
        if (query.isBlank()) notes
        else notes.filter {
            it.title.contains(query, ignoreCase = true) ||
                it.content.contains(query, ignoreCase = true) ||
                it.category.contains(query, ignoreCase = true)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ObsidianBlack)
            .testTag("notes_screen")
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 600.dp)
                .fillMaxSize()
                .align(Alignment.TopCenter)
                .padding(horizontal = 16.dp)
        ) {
            Text(
                text = strings.notesTitle,
                style = MaterialTheme.typography.titleLarge,
                color = CrispIce,
                modifier = Modifier.padding(vertical = 8.dp)
            )

            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null, tint = ElectricBlue) },
                placeholder = { Text(if (language == "gu") "નોટ્સ શોધો..." else "Search notes...", color = CoolSlate) },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("notes_search_input")
            )

            Spacer(Modifier.height(12.dp))

            if (filtered.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(24.dp)) {
                        Icon(
                            Icons.Rounded.NoteAlt,
                            contentDescription = null,
                            tint = ElectricBlue.copy(alpha = 0.6f),
                            modifier = Modifier.size(56.dp)
                        )
                        Spacer(Modifier.height(12.dp))
                        Text(
                            text = strings.noNotesHint,
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
                    items(filtered, key = { it.id }) { note ->
                        FuturisticCard(highlightBorder = note.isPinned) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = note.category.uppercase(),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = ElectricBlue
                                    )
                                    Text(
                                        text = note.title,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = CrispIce
                                    )
                                }
                                Row {
                                    IconButton(onClick = { onTogglePin(note) }) {
                                        Icon(
                                            Icons.Rounded.PushPin,
                                            contentDescription = "Pin Note",
                                            tint = if (note.isPinned) ElectricBlue else CoolSlate
                                        )
                                    }
                                    IconButton(onClick = {
                                        editingNote = note
                                        showDialog = true
                                    }) {
                                        Icon(Icons.Rounded.Edit, contentDescription = "Edit Note", tint = CrispIce)
                                    }
                                    IconButton(onClick = { onDeleteNote(note.id) }) {
                                        Icon(Icons.Rounded.Delete, contentDescription = "Delete Note", tint = CrimsonAlert)
                                    }
                                }
                            }
                            if (note.content.isNotBlank()) {
                                Spacer(Modifier.height(6.dp))
                                Text(
                                    text = note.content,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = CoolSlate
                                )
                            }
                        }
                    }
                }
            }
        }

        FloatingActionButton(
            onClick = {
                editingNote = null
                showDialog = true
            },
            containerColor = ElectricBlue,
            contentColor = ObsidianBlack,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(20.dp)
                .testTag("add_note_fab")
        ) {
            Icon(Icons.Rounded.Add, contentDescription = strings.addNote)
        }
    }

    if (showDialog) {
        NoteEditorDialog(
            initial = editingNote,
            language = language,
            onDismiss = { showDialog = false },
            onSave = { title, content, category ->
                onSaveNote(editingNote?.id, title, content, category, editingNote?.isPinned ?: false)
                showDialog = false
            }
        )
    }
}

@Composable
private fun NoteEditorDialog(
    initial: NoteEntity?,
    language: String,
    onDismiss: () -> Unit,
    onSave: (String, String, String) -> Unit
) {
    var title by remember { mutableStateOf(initial?.title ?: "") }
    var content by remember { mutableStateOf(initial?.content ?: "") }
    var category by remember { mutableStateOf(initial?.category ?: "Personal") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (initial == null) {
                    if (language == "gu") "નવી લોકલ નોટ" else "New Local Note"
                } else {
                    if (language == "gu") "નોટ સુધારો" else "Edit Note"
                }
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text(if (language == "gu") "શીર્ષક" else "Title") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("note_title_input")
                )
                OutlinedTextField(
                    value = content,
                    onValueChange = { content = it },
                    label = { Text(if (language == "gu") "વિગત" else "Content") },
                    minLines = 3,
                    modifier = Modifier.fillMaxWidth().testTag("note_content_input")
                )
                OutlinedTextField(
                    value = category,
                    onValueChange = { category = it },
                    label = { Text(if (language == "gu") "કેટેગરી" else "Category") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { if (title.isNotBlank() || content.isNotBlank()) onSave(title.ifBlank { content.take(24) }, content, category) },
                colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue, contentColor = ObsidianBlack),
                modifier = Modifier.testTag("save_note_dialog_btn")
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
