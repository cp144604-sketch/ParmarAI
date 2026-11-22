package com.example.control

import com.example.ai.AiStructuredReply
import com.example.ai.AssistantActionType
import com.example.data.NoteEntity
import com.example.data.ReminderEntity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class LocalCommandEngine {

    fun tryHandleLocally(
        input: String,
        language: String,
        stopWord: String,
        notes: List<NoteEntity>,
        reminders: List<ReminderEntity>,
        isOffline: Boolean
    ): AiStructuredReply? {
        val trimmed = input.trim()
        val lower = trimmed.lowercase()

        // 1. Stop word detection inside active session
        if (lower == stopWord.lowercase() || lower.contains("stop listening") || lower.contains("બંધ કરો")) {
            val reply = if (language == "gu") {
                "ઠીક છે Sir, PARMAR સ્ટેન્ડબાય મોડમાં જાય છે."
            } else {
                "Understood Sir, PARMAR entering standby mode."
            }
            return AiStructuredReply(reply = reply, action = AssistantActionType.NONE)
        }

        // 2. Language switch requests
        if (lower.contains("speak in english") || lower.contains("english mode") ||
            lower.contains("અંગ્રેજીમાં બોલ") || lower.contains("અંગ્રેજી ભાષા")
        ) {
            return AiStructuredReply(
                reply = "Certainly Sir! I have switched to English. How may I assist you?",
                action = AssistantActionType.SWITCH_LANGUAGE,
                actionParam = "en"
            )
        }
        if (lower.contains("speak in gujarati") || lower.contains("gujarati mode") ||
            lower.contains("ગુજરાતીમાં બોલ") || lower.contains("ગુજરાતી ભાષા")
        ) {
            return AiStructuredReply(
                reply = "ચોક્કસ Sir! મેં ગુજરાતી ભાષા પસંદ કરી છે. હવે હું ગુજરાતીમાં વાત કરીશ.",
                action = AssistantActionType.SWITCH_LANGUAGE,
                actionParam = "gu"
            )
        }

        // 3. Date and time
        if (lower.contains("time") || lower.contains("date") ||
            lower.contains("સમય") || lower.contains("કેટલા વાગ્યા") || lower.contains("તારીખ")
        ) {
            return AiStructuredReply(
                reply = "",
                action = AssistantActionType.READ_TIME
            )
        }

        // 4. Flashlight on/off
        if (lower.contains("flashlight") || lower.contains("torch") ||
            lower.contains("ફ્લેશલાઇટ") || lower.contains("ટૉર્ચ") || lower.contains("ટોર્ચ")
        ) {
            val turnOff = lower.contains("off") || lower.contains("બંધ")
            val turnOn = lower.contains("on") || lower.contains("ચાલુ")
            val param = when {
                turnOff -> "off"
                turnOn -> "on"
                else -> "toggle"
            }
            return AiStructuredReply(
                reply = "",
                action = AssistantActionType.TOGGLE_FLASHLIGHT,
                actionParam = param
            )
        }

        // 5. Agenda / Today's tasks ("આજે શું કામ કરવાનું છે?")
        if (lower.contains("આજે શું કામ") || lower.contains("મારા કામ") ||
            lower.contains("today's tasks") || lower.contains("what do i have today") ||
            lower.contains("my agenda") || lower.contains("check reminders")
        ) {
            val pending = reminders.filter { !it.isCompleted }
            val reply = if (language == "gu") {
                if (pending.isEmpty() && notes.isEmpty()) {
                    "Sir, મેં તમારા રિમાઇન્ડર અને નોટ્સ તપાસ્યા. હાલ કોઈ બાકી કામ નોંધાયેલું નથી."
                } else {
                    val remSummary = pending.take(3).joinToString(", ") {
                        val t = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(it.triggerAtMillis))
                        "${it.title} ($t)"
                    }
                    "Sir, હું તમારા રિમાઇન્ડર અને નોટ્સ તપાસી આપું છું. તમારી પાસે ${pending.size} રિમાઇન્ડર અને ${notes.size} નોટ્સ છે" +
                        if (remSummary.isNotBlank()) ": $remSummary." else "."
                }
            } else {
                if (pending.isEmpty() && notes.isEmpty()) {
                    "Sir, I checked your reminders and notes. There are no pending tasks right now."
                } else {
                    val remSummary = pending.take(3).joinToString(", ") { it.title }
                    "Sir, I checked your schedule. You have ${pending.size} pending reminders and ${notes.size} notes" +
                        if (remSummary.isNotBlank()) ": $remSummary." else "."
                }
            }
            return AiStructuredReply(
                reply = reply,
                action = AssistantActionType.CHECK_AGENDA
            )
        }

        // 6. Bluetooth & Network shortcuts
        if (lower.contains("bluetooth") || lower.contains("બ્લુટુથ")) {
            return AiStructuredReply(
                reply = "",
                action = AssistantActionType.OPEN_BLUETOOTH_SETTINGS
            )
        }
        if (lower.contains("wifi") || lower.contains("wi-fi") || lower.contains("network") ||
            lower.contains("વાઇફાઇ") || lower.contains("ઇન્ટરનેટ")
        ) {
            return AiStructuredReply(
                reply = "",
                action = AssistantActionType.OPEN_WIFI_SETTINGS
            )
        }

        // 7. Volume controls
        if (lower.contains("volume") || lower.contains("વોલ્યુમ") || lower.contains("અવાજ")) {
            val param = when {
                lower.contains("up") || lower.contains("વધાર") -> "up"
                lower.contains("down") || lower.contains("ઓછ") -> "down"
                lower.contains("mute") || lower.contains("બંધ") -> "mute"
                lower.contains("max") || lower.contains("ફૂલ") -> "max"
                else -> "up"
            }
            return AiStructuredReply(
                reply = "",
                action = AssistantActionType.SET_VOLUME,
                actionParam = param
            )
        }

        // 8. Lock device
        if (lower.contains("lock phone") || lower.contains("lock screen") ||
            lower.contains("ફોન લોક") || lower.contains("સ્ક્રીન લોક")
        ) {
            return AiStructuredReply(
                reply = "",
                action = AssistantActionType.LOCK_DEVICE
            )
        }

        // 9. Open app
        if (lower.startsWith("open ") || lower.startsWith("launch ") ||
            lower.endsWith("ખોલો") || lower.endsWith("ખોલ")
        ) {
            val target = trimmed
                .replace(Regex("^(open|launch)\\s+", RegexOption.IGNORE_CASE), "")
                .replace(Regex("\\s*(ખોલો|ખોલ)$"), "")
                .trim()
            if (target.isNotBlank()) {
                return AiStructuredReply(
                    reply = "",
                    action = AssistantActionType.OPEN_APP,
                    actionParam = target
                )
            }
        }

        // 10. Quick local note creation (especially useful offline)
        if (lower.startsWith("note ") || lower.startsWith("create note ") ||
            lower.startsWith("નોટ ") || lower.startsWith("નોંધ ")
        ) {
            val content = trimmed
                .replace(Regex("^(create note|note|નોટ|નોંધ)\\s*[:\\-]?\\s*", RegexOption.IGNORE_CASE), "")
                .trim()
            if (content.isNotBlank()) {
                val reply = if (language == "gu") {
                    "Sir, મેં તમારી નોટ લોકલ મેમરીમાં સેવ કરી દીધી છે: \"$content\"."
                } else {
                    "Sir, I have saved your local note: \"$content\"."
                }
                return AiStructuredReply(
                    reply = reply,
                    action = AssistantActionType.CREATE_NOTE,
                    actionParam = content
                )
            }
        }

        // 11. Quick local reminder creation
        if (lower.startsWith("remind me ") || lower.startsWith("set reminder ") ||
            lower.startsWith("રિમાઇન્ડર ") || lower.startsWith("યાદ અપાવ")
        ) {
            val title = trimmed
                .replace(
                    Regex("^(remind me to|remind me|set reminder|રિમાઇન્ડર|યાદ અપાવજો|યાદ અપાવ)\\s*[:\\-]?\\s*", RegexOption.IGNORE_CASE),
                    ""
                )
                .trim()
            if (title.isNotBlank()) {
                val reply = if (language == "gu") {
                    "Sir, મેં 15 મિનિટ પછીનું લોકલ રિમાઇન્ડર સેટ કર્યું છે: \"$title\"."
                } else {
                    "Sir, I have set a local reminder for 15 minutes from now: \"$title\"."
                }
                return AiStructuredReply(
                    reply = reply,
                    action = AssistantActionType.CREATE_REMINDER,
                    actionParam = title,
                    timeHint = "in 15 minutes"
                )
            }
        }

        // 12. Offline fallback message — never claim full AI offline
        if (isOffline) {
            val reply = if (language == "gu") {
                "Sir, હાલ ઇન્ટરનેટ ઉપલબ્ધ નથી તેથી ઓનલાઇન AI મોડલ બંધ છે (ઓફલાઇન મોડ). તમે લોકલ નોટ્સ, રિમાઇન્ડર, સમય, ફ્લેશલાઇટ, વોલ્યુમ, એપ ઓપન અથવા સેટિંગ્સ કમાન્ડ વાપરી શકો છો."
            } else {
                "Sir, internet is unavailable so cloud AI is offline. No local LLM is installed, but offline commands (time, flashlight, open apps, local notes, reminders, volume, settings) are ready."
            }
            return AiStructuredReply(reply = reply, action = AssistantActionType.NONE)
        }

        return null
    }
}
