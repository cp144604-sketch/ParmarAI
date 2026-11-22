package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.AssistantSettings
import com.example.data.BackupManager
import com.example.data.MemoryEntity
import com.example.data.NoteEntity
import com.example.data.ParmarDatabase
import com.example.data.ReminderEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `read app name from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("PARMAR AI", appName)
    }

    @Test
    fun `room database stores and observes notes reminders and memories`() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = Room.inMemoryDatabaseBuilder(context, ParmarDatabase::class.java)
            .allowMainThreadQueries()
            .build()

        try {
            db.noteDao().insertNote(NoteEntity(title = "Meeting", content = "Client call at 5 PM"))
            db.reminderDao().insertReminder(ReminderEntity(title = "Take medicine", triggerAtMillis = 9999999999999L))
            db.memoryDao().insertMemory(MemoryEntity(key = "Tea", value = "Less sugar", userApproved = true))

            assertEquals(1, db.noteDao().observeAllNotes().first().size)
            assertEquals(1, db.reminderDao().observeAllReminders().first().size)
            val memories = db.memoryDao().observeAllMemories().first()
            assertEquals(1, memories.size)
            assertEquals("Less sugar", memories.first().value)
        } finally {
            db.close()
        }
    }

    @Test
    fun `backup manager round trips notes reminders memories and settings`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val manager = BackupManager(context)
        val notes = listOf(NoteEntity(title = "Shopping", content = "Milk, Tea"))
        val reminders = listOf(ReminderEntity(title = "Call Mom", triggerAtMillis = 1800000000000L))
        val memories = listOf(MemoryEntity(key = "Language", value = "Gujarati", isPrivate = false))
        val settings = AssistantSettings(wakeWord = "DIAMOND", language = "gu", voiceSpeed = 1.1f)

        val json = manager.encodeBackupJson(notes, reminders, memories, settings)
        val decoded = manager.decodeBackupJson(json)

        assertEquals(1, decoded.notes.size)
        assertEquals("Shopping", decoded.notes.first().title)
        assertEquals(1, decoded.reminders.size)
        assertEquals("Call Mom", decoded.reminders.first().title)
        assertEquals(1, decoded.memories.size)
        assertEquals("Gujarati", decoded.memories.first().value)
        assertTrue(decoded.settings?.language == "gu")
    }
}
