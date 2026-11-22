package com.example

import com.example.ai.AssistantActionType
import com.example.control.LocalCommandEngine
import com.example.data.NoteEntity
import com.example.data.ReminderEntity
import com.example.voice.WakeWordMatcher
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ParmarEngineUnitTest {

    @Test
    fun `wake word matcher detects DIAMOND across English phonetic and Gujarati variants`() {
        assertTrue(WakeWordMatcher.matches("DIAMOND", "DIAMOND"))
        assertTrue(WakeWordMatcher.matches("Hey diamond wake up", "DIAMOND"))
        assertTrue(WakeWordMatcher.matches("ડાયમંડ", "DIAMOND"))
        assertTrue(WakeWordMatcher.matches("daimond", "DIAMOND"))
        assertFalse(WakeWordMatcher.matches("good morning parmar", "DIAMOND"))
    }

    @Test
    fun `local command engine handles Gujarati agenda query and offline actions`() {
        val engine = LocalCommandEngine()
        val notes = listOf(NoteEntity(title = "Work", content = "Review APK"))
        val reminders = listOf(ReminderEntity(title = "Office Call", triggerAtMillis = System.currentTimeMillis() + 60_000L))

        val agendaReply = engine.tryHandleLocally(
            input = "આજે શું કામ કરવાનું છે?",
            language = "gu",
            stopWord = "DIAMOND",
            notes = notes,
            reminders = reminders,
            isOffline = false
        )
        assertNotNull(agendaReply)
        assertEquals(AssistantActionType.CHECK_AGENDA, agendaReply?.action)
        assertTrue(agendaReply?.reply?.contains("Sir") == true)

        val torchReply = engine.tryHandleLocally(
            input = "ફ્લેશલાઇટ ચાલુ કર",
            language = "gu",
            stopWord = "DIAMOND",
            notes = notes,
            reminders = reminders,
            isOffline = true
        )
        assertEquals(AssistantActionType.TOGGLE_FLASHLIGHT, torchReply?.action)
        assertEquals("on", torchReply?.actionParam)

        val langSwitch = engine.tryHandleLocally(
            input = "Speak in English",
            language = "gu",
            stopWord = "DIAMOND",
            notes = notes,
            reminders = reminders,
            isOffline = true
        )
        assertEquals(AssistantActionType.SWITCH_LANGUAGE, langSwitch?.action)
        assertEquals("en", langSwitch?.actionParam)

        val offlineUnknown = engine.tryHandleLocally(
            input = "What is quantum computing?",
            language = "en",
            stopWord = "DIAMOND",
            notes = notes,
            reminders = reminders,
            isOffline = true
        )
        assertNotNull(offlineUnknown)
        assertTrue(offlineUnknown?.reply?.contains("offline", ignoreCase = true) == true)
    }
}
