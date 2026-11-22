package com.example.ai

import android.util.Log
import com.example.data.ChatMessageEntity
import com.example.data.MemoryEntity
import com.example.data.NoteEntity
import com.example.data.ReminderEntity
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withTimeout
import org.json.JSONObject

private const val TAG = "ParmarAiEngine"
private const val MODEL_NAME = "gemini-3.8-flash"

enum class AssistantActionType {
    NONE,
    CHECK_AGENDA,
    CREATE_NOTE,
    CREATE_REMINDER,
    PROPOSE_MEMORY,
    TOGGLE_FLASHLIGHT,
    OPEN_APP,
    READ_TIME,
    OPEN_BLUETOOTH_SETTINGS,
    OPEN_WIFI_SETTINGS,
    SET_VOLUME,
    LOCK_DEVICE,
    SWITCH_LANGUAGE,
    SET_ALARM
}

data class AiStructuredReply(
    val reply: String,
    val action: AssistantActionType = AssistantActionType.NONE,
    val actionParam: String = "",
    val timeHint: String = "",
    val memoryKey: String = "",
    val memoryValue: String = "",
    val memoryCategory: String = "Preference"
)

class ParmarAiEngine {

    private val structuredSchema = Schema.obj(
        properties = mapOf(
            "reply" to Schema.string(
                description = "Natural, concise response as PARMAR addressing the user as Sir in Gujarati by default, or English if language is en or user asked for English."
            ),
            "action" to Schema.enumeration(
                values = AssistantActionType.entries.map { it.name },
                description = "Optional authorized device or assistant action to execute."
            ),
            "actionParam" to Schema.string(
                description = "Action argument: note text, reminder title, app name, 'on'/'off' for flashlight, 'up'/'down'/'mute'/'50' for volume, 'en'/'gu' for language.",
                nullable = true
            ),
            "timeHint" to Schema.string(
                description = "Time or schedule hint for reminders/alarms like '18:30', 'in 15 minutes', 'tomorrow 09:00'.",
                nullable = true
            ),
            "memoryKey" to Schema.string(
                description = "Short label if user shared a personal preference/fact to remember.",
                nullable = true
            ),
            "memoryValue" to Schema.string(
                description = "Fact or preference value to propose saving to Memory.",
                nullable = true
            ),
            "memoryCategory" to Schema.string(
                description = "Category: Preference, Personal, Work, or Fact.",
                nullable = true
            )
        ),
        optionalProperties = listOf(
            "actionParam",
            "timeHint",
            "memoryKey",
            "memoryValue",
            "memoryCategory"
        )
    )

    private val structuredModel by lazy {
        Firebase.ai.generativeModel(
            modelName = MODEL_NAME,
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = structuredSchema
                temperature = 0.5f
            }
        )
    }

    private val streamingModel by lazy {
        Firebase.ai.generativeModel(
            modelName = MODEL_NAME,
            generationConfig = generationConfig {
                temperature = 0.6f
            }
        )
    }

    suspend fun generateStructuredResponse(
        userInput: String,
        language: String,
        memories: List<MemoryEntity>,
        notes: List<NoteEntity>,
        reminders: List<ReminderEntity>,
        recentHistory: List<ChatMessageEntity>
    ): Result<AiStructuredReply> {
        return try {
            val prompt = buildContextPrompt(
                userInput = userInput,
                language = language,
                memories = memories,
                notes = notes,
                reminders = reminders,
                recentHistory = recentHistory,
                structured = true
            )
            val response = withTimeout(60_000L) {
                structuredModel.generateContent(prompt)
            }
            val rawJson = response.text
                ?: return Result.failure(IllegalStateException("Empty response from AI engine."))

            val json = JSONObject(rawJson)
            val replyText = json.optString("reply").trim()
            if (replyText.isEmpty()) {
                return Result.failure(IllegalStateException("AI returned an empty reply."))
            }
            val actionName = json.optString("action", AssistantActionType.NONE.name)
            val action = runCatching { AssistantActionType.valueOf(actionName) }
                .getOrDefault(AssistantActionType.NONE)

            Result.success(
                AiStructuredReply(
                    reply = replyText,
                    action = action,
                    actionParam = json.optString("actionParam", "").takeIf { it != "null" }.orEmpty(),
                    timeHint = json.optString("timeHint", "").takeIf { it != "null" }.orEmpty(),
                    memoryKey = json.optString("memoryKey", "").takeIf { it != "null" }.orEmpty(),
                    memoryValue = json.optString("memoryValue", "").takeIf { it != "null" }.orEmpty(),
                    memoryCategory = json.optString("memoryCategory", "Preference")
                        .takeIf { it != "null" && it.isNotBlank() } ?: "Preference"
                )
            )
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "AI generation timed out", e)
            Result.failure(
                IllegalStateException(
                    if (language == "gu") "AI જવાબ સમયમર્યાદા પૂર્ણ થઈ ગઈ (60 સેકન્ડ). કૃપા કરીને ફરી પ્રયાસ કરો."
                    else "AI request timed out after 60 seconds. Please try again."
                )
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "AI generation failed", e)
            Result.failure(e)
        }
    }

    fun streamConversationalReply(
        userInput: String,
        language: String,
        memories: List<MemoryEntity>,
        notes: List<NoteEntity>,
        reminders: List<ReminderEntity>,
        recentHistory: List<ChatMessageEntity>
    ): Flow<String> = flow {
        val prompt = buildContextPrompt(
            userInput = userInput,
            language = language,
            memories = memories,
            notes = notes,
            reminders = reminders,
            recentHistory = recentHistory,
            structured = false
        )
        val buffer = StringBuilder()
        withTimeout(60_000L) {
            streamingModel.generateContentStream(
                content { text(prompt) }
            ).collect { chunk ->
                val piece = chunk.text.orEmpty()
                if (piece.isNotEmpty()) {
                    buffer.append(piece)
                    emit(buffer.toString())
                }
            }
        }
        if (buffer.isBlank()) {
            throw IllegalStateException("AI stream returned empty content.")
        }
    }

    suspend fun testConnection(): Result<String> {
        return try {
            val response = withTimeout(30_000L) {
                streamingModel.generateContent("Reply with strictly: PARMAR ONLINE")
            }
            val text = response.text?.trim().orEmpty()
            if (text.isNotEmpty()) Result.success(text)
            else Result.failure(IllegalStateException("Empty test response"))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Connection test failed", e)
            Result.failure(e)
        }
    }

    private fun buildContextPrompt(
        userInput: String,
        language: String,
        memories: List<MemoryEntity>,
        notes: List<NoteEntity>,
        reminders: List<ReminderEntity>,
        recentHistory: List<ChatMessageEntity>,
        structured: Boolean
    ): String {
        val langInstruction = if (language == "en") {
            "Respond in clear, natural English."
        } else {
            "Respond in natural, polite Gujarati (ગુજરાતી). Use English ONLY if the user explicitly asks you to speak/reply in English."
        }

        val approvedMemories = memories.filter { it.userApproved }.take(12).joinToString("\n") {
            "- [${it.category}] ${it.key}: ${it.value}"
        }.ifBlank { "None" }

        val activeReminders = reminders.filter { !it.isCompleted }.take(8).joinToString("\n") {
            "- ${it.title} (at ${java.text.SimpleDateFormat("dd MMM HH:mm", java.util.Locale.getDefault()).format(java.util.Date(it.triggerAtMillis))})"
        }.ifBlank { "None" }

        val recentNotes = notes.take(6).joinToString("\n") {
            "- ${it.title}: ${it.content.take(80)}"
        }.ifBlank { "None" }

        val historyText = recentHistory.takeLast(6).joinToString("\n") {
            "${it.role.uppercase()}: ${it.text}"
        }.ifBlank { "None" }

        return buildString {
            appendLine("You are PARMAR, a personal futuristic Android AI voice assistant.")
            appendLine("Personality Rules:")
            appendLine("1. Always address the user respectfully as 'Sir'.")
            appendLine("2. Female persona, calm, helpful, concise, and intelligent.")
            appendLine("3. $langInstruction")
            appendLine("4. Never claim you can bypass Android security, PINs, passwords, or biometrics.")
            appendLine("5. If the user states a personal preference or fact to remember, populate memoryKey and memoryValue and set action to PROPOSE_MEMORY (do not pretend it is saved without user approval).")
            if (structured) {
                appendLine("6. Set 'action' accurately when the user wants to check agenda/tasks (CHECK_AGENDA), create a note (CREATE_NOTE), set a reminder (CREATE_REMINDER), toggle flashlight (TOGGLE_FLASHLIGHT), open an app (OPEN_APP), check time/date (READ_TIME), open Bluetooth/Wi-Fi settings, adjust volume (SET_VOLUME), set system alarm (SET_ALARM), lock phone (LOCK_DEVICE), or switch language (SWITCH_LANGUAGE).")
            }
            appendLine()
            appendLine("User-Approved Saved Memories:")
            appendLine(approvedMemories)
            appendLine()
            appendLine("Pending Local Reminders:")
            appendLine(activeReminders)
            appendLine()
            appendLine("Recent Local Notes:")
            appendLine(recentNotes)
            appendLine()
            appendLine("Recent Conversation:")
            appendLine(historyText)
            appendLine()
            appendLine("Current User Message: $userInput")
        }
    }
}
