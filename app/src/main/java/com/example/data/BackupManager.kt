package com.example.data

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

data class BackupPayload(
    val notes: List<NoteEntity>,
    val reminders: List<ReminderEntity>,
    val memories: List<MemoryEntity>,
    val settings: AssistantSettings?
)

class BackupManager(private val context: Context) {

    fun encodeBackupJson(
        notes: List<NoteEntity>,
        reminders: List<ReminderEntity>,
        memories: List<MemoryEntity>,
        settings: AssistantSettings
    ): String {
        val root = JSONObject().apply {
            put("app", "PARMAR AI")
            put("schemaVersion", 1)
            put("exportedAt", System.currentTimeMillis())

            put("notes", JSONArray().apply {
                notes.forEach { n ->
                    put(JSONObject().apply {
                        put("title", n.title)
                        put("content", n.content)
                        put("category", n.category)
                        put("isPinned", n.isPinned)
                        put("updatedAt", n.updatedAt)
                    })
                }
            })

            put("reminders", JSONArray().apply {
                reminders.forEach { r ->
                    put(JSONObject().apply {
                        put("title", r.title)
                        put("triggerAtMillis", r.triggerAtMillis)
                        put("repeatType", r.repeatType)
                        put("isCompleted", r.isCompleted)
                        put("createdAt", r.createdAt)
                    })
                }
            })

            put("memories", JSONArray().apply {
                memories.forEach { m ->
                    put(JSONObject().apply {
                        put("key", m.key)
                        put("value", m.value)
                        put("category", m.category)
                        put("isPrivate", m.isPrivate)
                        put("userApproved", m.userApproved)
                        put("createdAt", m.createdAt)
                    })
                }
            })

            put("settings", JSONObject().apply {
                put("wakeWord", settings.wakeWord)
                put("stopWord", settings.stopWord)
                put("language", settings.language)
                put("voiceSpeed", settings.voiceSpeed.toDouble())
                put("voicePitch", settings.voicePitch.toDouble())
                put("selectedVoiceName", settings.selectedVoiceName)
                put("offlineModeForced", settings.offlineModeForced)
                put("requireAuthForPrivateActions", settings.requireAuthForPrivateActions)
                put("voiceProfileName", settings.voiceProfileName)
            })
        }
        return root.toString(2)
    }

    fun decodeBackupJson(rawJson: String): BackupPayload {
        val root = JSONObject(rawJson)
        val notesArr = root.optJSONArray("notes") ?: JSONArray()
        val notes = (0 until notesArr.length()).map { i ->
            val obj = notesArr.getJSONObject(i)
            NoteEntity(
                title = obj.optString("title", ""),
                content = obj.optString("content", ""),
                category = obj.optString("category", "General"),
                isPinned = obj.optBoolean("isPinned", false),
                updatedAt = obj.optLong("updatedAt", System.currentTimeMillis())
            )
        }

        val remindersArr = root.optJSONArray("reminders") ?: JSONArray()
        val reminders = (0 until remindersArr.length()).map { i ->
            val obj = remindersArr.getJSONObject(i)
            ReminderEntity(
                title = obj.optString("title", ""),
                triggerAtMillis = obj.optLong("triggerAtMillis", System.currentTimeMillis()),
                repeatType = obj.optString("repeatType", "ONCE"),
                isCompleted = obj.optBoolean("isCompleted", false),
                createdAt = obj.optLong("createdAt", System.currentTimeMillis())
            )
        }

        val memoriesArr = root.optJSONArray("memories") ?: JSONArray()
        val memories = (0 until memoriesArr.length()).map { i ->
            val obj = memoriesArr.getJSONObject(i)
            MemoryEntity(
                key = obj.optString("key", ""),
                value = obj.optString("value", ""),
                category = obj.optString("category", "Preference"),
                isPrivate = obj.optBoolean("isPrivate", false),
                userApproved = obj.optBoolean("userApproved", true),
                createdAt = obj.optLong("createdAt", System.currentTimeMillis())
            )
        }

        val settingsObj = root.optJSONObject("settings")
        val settings = settingsObj?.let { s ->
            AssistantSettings(
                wakeWord = s.optString("wakeWord", "DIAMOND"),
                stopWord = s.optString("stopWord", "DIAMOND"),
                language = s.optString("language", "gu"),
                voiceSpeed = s.optDouble("voiceSpeed", 1.0).toFloat(),
                voicePitch = s.optDouble("voicePitch", 1.08).toFloat(),
                selectedVoiceName = s.optString("selectedVoiceName", ""),
                offlineModeForced = s.optBoolean("offlineModeForced", false),
                requireAuthForPrivateActions = s.optBoolean("requireAuthForPrivateActions", true),
                voiceProfileName = s.optString("voiceProfileName", "Sir")
            )
        }

        return BackupPayload(
            notes = notes,
            reminders = reminders,
            memories = memories,
            settings = settings
        )
    }

    suspend fun writeToUri(uri: Uri, content: String): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            context.contentResolver.openOutputStream(uri)?.use { stream ->
                stream.write(content.toByteArray(Charsets.UTF_8))
                stream.flush()
            } ?: throw IllegalStateException("Unable to open destination file.")
        }
    }

    suspend fun readFromUri(uri: Uri): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                stream.bufferedReader(Charsets.UTF_8).readText()
            } ?: throw IllegalStateException("Unable to read backup file.")
        }
    }
}
