package com.example.data

import android.net.Uri
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

class ParmarRepository(
    private val database: ParmarDatabase,
    private val settingsStore: SettingsDataStore,
    private val backupManager: BackupManager
) {
    private val noteDao = database.noteDao()
    private val reminderDao = database.reminderDao()
    private val memoryDao = database.memoryDao()
    private val chatDao = database.chatMessageDao()

    val notes: Flow<List<NoteEntity>> = noteDao.observeAllNotes()
    val reminders: Flow<List<ReminderEntity>> = reminderDao.observeAllReminders()
    val memories: Flow<List<MemoryEntity>> = memoryDao.observeAllMemories()
    val chatMessages: Flow<List<ChatMessageEntity>> = chatDao.observeAllMessages()
    val settings: Flow<AssistantSettings> = settingsStore.settingsFlow

    suspend fun addNote(title: String, content: String, category: String = "General"): Long {
        return noteDao.insertNote(
            NoteEntity(
                title = title.trim().ifBlank { "Note" },
                content = content.trim(),
                category = category
            )
        )
    }

    suspend fun updateNote(note: NoteEntity) = noteDao.updateNote(
        note.copy(updatedAt = System.currentTimeMillis())
    )

    suspend fun deleteNote(id: Long) = noteDao.deleteNoteById(id)

    suspend fun addReminder(title: String, triggerAtMillis: Long, repeatType: String = "ONCE"): Long {
        return reminderDao.insertReminder(
            ReminderEntity(
                title = title.trim(),
                triggerAtMillis = triggerAtMillis,
                repeatType = repeatType
            )
        )
    }

    suspend fun updateReminder(reminder: ReminderEntity) = reminderDao.updateReminder(reminder)

    suspend fun deleteReminder(id: Long) = reminderDao.deleteReminderById(id)

    suspend fun addMemory(
        key: String,
        value: String,
        category: String = "Preference",
        isPrivate: Boolean = false
    ): Long {
        return memoryDao.insertMemory(
            MemoryEntity(
                key = key.trim(),
                value = value.trim(),
                category = category,
                isPrivate = isPrivate,
                userApproved = true
            )
        )
    }

    suspend fun updateMemory(memory: MemoryEntity) = memoryDao.updateMemory(memory)

    suspend fun deleteMemory(id: Long) = memoryDao.deleteMemoryById(id)

    suspend fun addChatMessage(
        role: String,
        text: String,
        isOfflineResponse: Boolean = false,
        actionTaken: String? = null
    ): Long {
        return chatDao.insertMessage(
            ChatMessageEntity(
                role = role,
                text = text,
                isOfflineResponse = isOfflineResponse,
                actionTaken = actionTaken
            )
        )
    }

    suspend fun clearChatHistory() = chatDao.clearAllMessages()

    suspend fun clearAllMemories() = memoryDao.clearAllMemories()

    suspend fun updateSettings(transform: (AssistantSettings) -> AssistantSettings) {
        settingsStore.updateSettings(transform)
    }

    suspend fun getSnapshotForAi(): AiContextSnapshot {
        return AiContextSnapshot(
            memories = memoryDao.getAllMemoriesSnapshot(),
            notes = noteDao.getAllNotesSnapshot(),
            reminders = reminderDao.getAllRemindersSnapshot(),
            recentMessages = chatDao.getRecentMessages(10).reversed()
        )
    }

    suspend fun exportBackupToUri(uri: Uri): Result<Int> {
        return try {
            val notesList = noteDao.getAllNotesSnapshot()
            val remindersList = reminderDao.getAllRemindersSnapshot()
            val memoriesList = memoryDao.getAllMemoriesSnapshot()
            val currentSettings = settingsStore.settingsFlow.first()
            val json = backupManager.encodeBackupJson(notesList, remindersList, memoriesList, currentSettings)
            backupManager.writeToUri(uri, json).map {
                notesList.size + remindersList.size + memoriesList.size
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun importBackupFromUri(uri: Uri): Result<Int> {
        return try {
            val raw = backupManager.readFromUri(uri).getOrThrow()
            val payload = backupManager.decodeBackupJson(raw)
            payload.notes.forEach { noteDao.insertNote(it) }
            payload.reminders.forEach { reminderDao.insertReminder(it) }
            payload.memories.forEach { memoryDao.insertMemory(it) }
            payload.settings?.let { imported ->
                settingsStore.updateSettings { current ->
                    current.copy(
                        wakeWord = imported.wakeWord,
                        stopWord = imported.stopWord,
                        language = imported.language,
                        voiceSpeed = imported.voiceSpeed,
                        voicePitch = imported.voicePitch,
                        selectedVoiceName = imported.selectedVoiceName,
                        offlineModeForced = imported.offlineModeForced,
                        requireAuthForPrivateActions = imported.requireAuthForPrivateActions,
                        voiceProfileName = imported.voiceProfileName
                    )
                }
            }
            Result.success(payload.notes.size + payload.reminders.size + payload.memories.size)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

data class AiContextSnapshot(
    val memories: List<MemoryEntity>,
    val notes: List<NoteEntity>,
    val reminders: List<ReminderEntity>,
    val recentMessages: List<ChatMessageEntity>
)
