package com.example.voice

import android.content.Context
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.speech.tts.Voice
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale
import java.util.UUID

private const val TAG = "ParmarTts"

data class VoiceOption(
    val name: String,
    val localeTag: String,
    val isFemaleHint: Boolean
)

class ParmarTtsManager(context: Context) : TextToSpeech.OnInitListener {

    private var tts: TextToSpeech? = TextToSpeech(context.applicationContext, this)
    private var isReady = false

    private val _isSpeaking = MutableStateFlow(false)
    val isSpeaking: StateFlow<Boolean> = _isSpeaking.asStateFlow()

    private val _availableVoices = MutableStateFlow<List<VoiceOption>>(emptyList())
    val availableVoices: StateFlow<List<VoiceOption>> = _availableVoices.asStateFlow()

    private var currentSpeed = 1.0f
    private var currentPitch = 1.08f
    private var currentLanguage = "gu"
    private var preferredVoiceName = ""

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            isReady = true
            tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {
                    _isSpeaking.value = true
                }

                override fun onDone(utteranceId: String?) {
                    _isSpeaking.value = false
                }

                @Deprecated("Deprecated in Java")
                override fun onError(utteranceId: String?) {
                    _isSpeaking.value = false
                }

                override fun onError(utteranceId: String?, errorCode: Int) {
                    _isSpeaking.value = false
                }
            })
            refreshVoices()
            applyVoiceConfiguration()
        } else {
            Log.e(TAG, "TextToSpeech init failed with status $status")
        }
    }

    fun updatePreferences(
        language: String,
        speed: Float,
        pitch: Float,
        selectedVoiceName: String
    ) {
        currentLanguage = language
        currentSpeed = speed
        currentPitch = pitch
        preferredVoiceName = selectedVoiceName
        if (isReady) {
            applyVoiceConfiguration()
        }
    }

    private fun refreshVoices() {
        val voices = try {
            tts?.voices?.filter { v ->
                val lang = v.locale.language.lowercase()
                lang == "gu" || lang == "en" || lang == "hi"
            }?.map { v ->
                VoiceOption(
                    name = v.name,
                    localeTag = v.locale.toLanguageTag(),
                    isFemaleHint = isLikelyFemaleVoice(v)
                )
            }?.sortedWith(compareByDescending<VoiceOption> { it.isFemaleHint }.thenBy { it.localeTag })
                ?: emptyList()
        } catch (_: Exception) {
            emptyList()
        }
        _availableVoices.value = voices
    }

    private fun applyVoiceConfiguration() {
        val engine = tts ?: return
        val targetLocale = if (currentLanguage == "gu") {
            Locale("gu", "IN")
        } else {
            Locale("en", "IN")
        }

        val langRes = engine.setLanguage(targetLocale)
        if (langRes == TextToSpeech.LANG_MISSING_DATA || langRes == TextToSpeech.LANG_NOT_SUPPORTED) {
            engine.setLanguage(Locale.US)
        }

        val allVoices = runCatching { engine.voices }.getOrNull().orEmpty()
        val chosen = if (preferredVoiceName.isNotBlank()) {
            allVoices.firstOrNull { it.name == preferredVoiceName }
        } else {
            selectBestFemaleVoice(allVoices, targetLocale)
        }
        if (chosen != null) {
            runCatching { engine.voice = chosen }
        }

        engine.setSpeechRate(currentSpeed)
        engine.setPitch(currentPitch)
    }

    private fun selectBestFemaleVoice(voices: Set<Voice>, targetLocale: Locale): Voice? {
        val langVoices = voices.filter { it.locale.language == targetLocale.language }
        return langVoices.firstOrNull { isLikelyFemaleVoice(it) }
            ?: langVoices.firstOrNull()
            ?: voices.firstOrNull { it.locale.language == "en" && isLikelyFemaleVoice(it) }
    }

    private fun isLikelyFemaleVoice(voice: Voice): Boolean {
        val lower = voice.name.lowercase()
        return lower.contains("female") ||
            lower.contains("guf") ||
            lower.contains("enf") ||
            lower.contains("ena") ||
            lower.contains("enc") ||
            lower.contains("sfg") ||
            lower.contains("iol") ||
            lower.contains("f00")
    }

    fun speak(text: String) {
        val clean = text.trim()
        if (clean.isEmpty() || !isReady) return
        applyVoiceConfiguration()
        val params = Bundle()
        val utteranceId = UUID.randomUUID().toString()
        tts?.speak(clean, TextToSpeech.QUEUE_FLUSH, params, utteranceId)
    }

    fun stop() {
        tts?.stop()
        _isSpeaking.value = false
    }

    fun shutdown() {
        tts?.stop()
        tts?.shutdown()
        tts = null
        isReady = false
        _isSpeaking.value = false
    }
}
