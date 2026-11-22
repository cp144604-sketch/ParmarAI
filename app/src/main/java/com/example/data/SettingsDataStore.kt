package com.example.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.parmarSettingsStore: DataStore<Preferences> by preferencesDataStore(
    name = "parmar_ai_settings"
)

data class AssistantSettings(
    val wakeWord: String = "DIAMOND",
    val stopWord: String = "DIAMOND",
    val language: String = "gu", // "gu" default, "en" when requested
    val voiceSpeed: Float = 1.0f,
    val voicePitch: Float = 1.08f,
    val selectedVoiceName: String = "",
    val offlineModeForced: Boolean = false,
    val backgroundListeningEnabled: Boolean = false,
    val screenOffWakeEnabled: Boolean = false,
    val requireAuthForPrivateActions: Boolean = true,
    val voiceProfileName: String = "Sir",
    val voiceProfileEnabled: Boolean = false,
    val voiceProfileEnergyBaseline: Float = 0.08f
)

class SettingsDataStore(private val context: Context) {

    private object Keys {
        val WAKE_WORD = stringPreferencesKey("wake_word")
        val STOP_WORD = stringPreferencesKey("stop_word")
        val LANGUAGE = stringPreferencesKey("language")
        val VOICE_SPEED = floatPreferencesKey("voice_speed")
        val VOICE_PITCH = floatPreferencesKey("voice_pitch")
        val SELECTED_VOICE_NAME = stringPreferencesKey("selected_voice_name")
        val OFFLINE_MODE_FORCED = booleanPreferencesKey("offline_mode_forced")
        val BACKGROUND_LISTENING = booleanPreferencesKey("background_listening")
        val SCREEN_OFF_WAKE = booleanPreferencesKey("screen_off_wake")
        val REQUIRE_AUTH_PRIVATE = booleanPreferencesKey("require_auth_private")
        val VOICE_PROFILE_NAME = stringPreferencesKey("voice_profile_name")
        val VOICE_PROFILE_ENABLED = booleanPreferencesKey("voice_profile_enabled")
        val VOICE_PROFILE_ENERGY = floatPreferencesKey("voice_profile_energy")
    }

    val settingsFlow: Flow<AssistantSettings> = context.parmarSettingsStore.data.map { prefs ->
        AssistantSettings(
            wakeWord = prefs[Keys.WAKE_WORD] ?: "DIAMOND",
            stopWord = prefs[Keys.STOP_WORD] ?: "DIAMOND",
            language = prefs[Keys.LANGUAGE] ?: "gu",
            voiceSpeed = prefs[Keys.VOICE_SPEED] ?: 1.0f,
            voicePitch = prefs[Keys.VOICE_PITCH] ?: 1.08f,
            selectedVoiceName = prefs[Keys.SELECTED_VOICE_NAME] ?: "",
            offlineModeForced = prefs[Keys.OFFLINE_MODE_FORCED] ?: false,
            backgroundListeningEnabled = prefs[Keys.BACKGROUND_LISTENING] ?: false,
            screenOffWakeEnabled = prefs[Keys.SCREEN_OFF_WAKE] ?: false,
            requireAuthForPrivateActions = prefs[Keys.REQUIRE_AUTH_PRIVATE] ?: true,
            voiceProfileName = prefs[Keys.VOICE_PROFILE_NAME] ?: "Sir",
            voiceProfileEnabled = prefs[Keys.VOICE_PROFILE_ENABLED] ?: false,
            voiceProfileEnergyBaseline = prefs[Keys.VOICE_PROFILE_ENERGY] ?: 0.08f
        )
    }

    suspend fun updateSettings(transform: (AssistantSettings) -> AssistantSettings) {
        context.parmarSettingsStore.edit { prefs ->
            val current = AssistantSettings(
                wakeWord = prefs[Keys.WAKE_WORD] ?: "DIAMOND",
                stopWord = prefs[Keys.STOP_WORD] ?: "DIAMOND",
                language = prefs[Keys.LANGUAGE] ?: "gu",
                voiceSpeed = prefs[Keys.VOICE_SPEED] ?: 1.0f,
                voicePitch = prefs[Keys.VOICE_PITCH] ?: 1.08f,
                selectedVoiceName = prefs[Keys.SELECTED_VOICE_NAME] ?: "",
                offlineModeForced = prefs[Keys.OFFLINE_MODE_FORCED] ?: false,
                backgroundListeningEnabled = prefs[Keys.BACKGROUND_LISTENING] ?: false,
                screenOffWakeEnabled = prefs[Keys.SCREEN_OFF_WAKE] ?: false,
                requireAuthForPrivateActions = prefs[Keys.REQUIRE_AUTH_PRIVATE] ?: true,
                voiceProfileName = prefs[Keys.VOICE_PROFILE_NAME] ?: "Sir",
                voiceProfileEnabled = prefs[Keys.VOICE_PROFILE_ENABLED] ?: false,
                voiceProfileEnergyBaseline = prefs[Keys.VOICE_PROFILE_ENERGY] ?: 0.08f
            )
            val updated = transform(current)
            prefs[Keys.WAKE_WORD] = updated.wakeWord.trim().ifBlank { "DIAMOND" }
            prefs[Keys.STOP_WORD] = updated.stopWord.trim().ifBlank { "DIAMOND" }
            prefs[Keys.LANGUAGE] = updated.language
            prefs[Keys.VOICE_SPEED] = updated.voiceSpeed.coerceIn(0.6f, 1.6f)
            prefs[Keys.VOICE_PITCH] = updated.voicePitch.coerceIn(0.7f, 1.4f)
            prefs[Keys.SELECTED_VOICE_NAME] = updated.selectedVoiceName
            prefs[Keys.OFFLINE_MODE_FORCED] = updated.offlineModeForced
            prefs[Keys.BACKGROUND_LISTENING] = updated.backgroundListeningEnabled
            prefs[Keys.SCREEN_OFF_WAKE] = updated.screenOffWakeEnabled
            prefs[Keys.REQUIRE_AUTH_PRIVATE] = updated.requireAuthForPrivateActions
            prefs[Keys.VOICE_PROFILE_NAME] = updated.voiceProfileName
            prefs[Keys.VOICE_PROFILE_ENABLED] = updated.voiceProfileEnabled
            prefs[Keys.VOICE_PROFILE_ENERGY] = updated.voiceProfileEnergyBaseline
        }
    }
}
