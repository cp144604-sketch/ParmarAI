package com.example.voice

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.sqrt

private const val TAG = "WakeWordDetector"

enum class VoiceEngineMode {
    IDLE,
    WAKE_STANDBY,
    ACTIVE_LISTENING,
    ERROR
}

object WakeWordMatcher {
    fun matches(transcript: String, wakeWord: String): Boolean {
        val target = wakeWord.trim().lowercase()
        if (target.isEmpty()) return false
        val normalized = transcript.trim().lowercase()
        if (normalized.isEmpty()) return false

        if (normalized.contains(target)) return true

        if (target == "diamond") {
            val gujaratiAndPhoneticVariants = listOf(
                "ડાયમંડ", "ડાયમન્ડ", "ડાઈમંડ", "ડાયમંટ",
                "daimond", "dimond", "diamon", "dayamand", "diamand", "dimon", "dime on"
            )
            if (gujaratiAndPhoneticVariants.any { normalized.contains(it) }) return true
        }

        val words = normalized.split(Regex("[\\s,।.!?]+")).filter { it.isNotBlank() }
        return words.any { word ->
            if (abs(word.length - target.length) > 2) false
            else levenshtein(word, target) <= 2
        }
    }

    private fun levenshtein(a: String, b: String): Int {
        val dp = Array(a.length + 1) { IntArray(b.length + 1) }
        for (i in 0..a.length) dp[i][0] = i
        for (j in 0..b.length) dp[0][j] = j
        for (i in 1..a.length) {
            for (j in 1..b.length) {
                val cost = if (a[i - 1] == b[j - 1]) 0 else 1
                dp[i][j] = minOf(
                    dp[i - 1][j] + 1,
                    dp[i][j - 1] + 1,
                    dp[i - 1][j - 1] + cost
                )
            }
        }
        return dp[a.length][b.length]
    }
}

class WakeWordDetector(
    private val context: Context,
    private val onWakeWordDetected: () -> Unit,
    private val onStopWordDetected: () -> Unit,
    private val onCommandTranscript: (String) -> Unit,
    private val onErrorMessage: (String) -> Unit
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val mainHandler = Handler(Looper.getMainLooper())

    private var speechRecognizer: SpeechRecognizer? = null
    private var vadJob: Job? = null
    private var audioRecord: AudioRecord? = null

    private val _mode = MutableStateFlow(VoiceEngineMode.IDLE)
    val mode: StateFlow<VoiceEngineMode> = _mode.asStateFlow()

    private val _audioLevel = MutableStateFlow(0f)
    val audioLevel: StateFlow<Float> = _audioLevel.asStateFlow()

    private val _partialTranscript = MutableStateFlow("")
    val partialTranscript: StateFlow<String> = _partialTranscript.asStateFlow()

    @Volatile
    private var wakeWord = "DIAMOND"

    @Volatile
    private var stopWord = "DIAMOND"

    @Volatile
    private var languageCode = "gu"

    @Volatile
    private var preferOffline = false

    @Volatile
    private var isRecognizerBusy = false

    fun updateConfig(
        wakeWord: String,
        stopWord: String,
        languageCode: String,
        preferOffline: Boolean
    ) {
        this.wakeWord = wakeWord.ifBlank { "DIAMOND" }
        this.stopWord = stopWord.ifBlank { "DIAMOND" }
        this.languageCode = languageCode
        this.preferOffline = preferOffline
    }

    fun hasRecordPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun startStandbyWakeMonitoring() {
        if (!hasRecordPermission()) {
            _mode.value = VoiceEngineMode.ERROR
            onErrorMessage("Microphone permission required for wake-word detection.")
            return
        }
        stopAllInternal()
        _mode.value = VoiceEngineMode.WAKE_STANDBY
        startOnDeviceVadLoop()
    }

    fun startActiveCommandListening() {
        if (!hasRecordPermission()) {
            _mode.value = VoiceEngineMode.ERROR
            onErrorMessage("Microphone permission required.")
            return
        }
        stopVadLoopOnly()
        _mode.value = VoiceEngineMode.ACTIVE_LISTENING
        _partialTranscript.value = ""
        launchSpeechRecognizer(singleShotCommand = true)
    }

    fun stopAll() {
        stopAllInternal()
        _mode.value = VoiceEngineMode.IDLE
        _audioLevel.value = 0f
        _partialTranscript.value = ""
    }

    fun sampleVoiceEnergyOnce(onSampled: (Float) -> Unit) {
        if (!hasRecordPermission()) {
            onSampled(0.08f)
            return
        }
        scope.launch {
            val sampleRate = 16000
            val minBuf = AudioRecord.getMinBufferSize(
                sampleRate,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            ).coerceAtLeast(2048)

            var recorder: AudioRecord? = null
            var peakRms = 0f
            try {
                recorder = AudioRecord(
                    MediaRecorder.AudioSource.VOICE_RECOGNITION,
                    sampleRate,
                    AudioFormat.CHANNEL_IN_MONO,
                    AudioFormat.ENCODING_PCM_16BIT,
                    minBuf
                )
                if (recorder.state == AudioRecord.STATE_INITIALIZED) {
                    recorder.startRecording()
                    val buf = ShortArray(minBuf / 2)
                    repeat(20) {
                        val read = recorder.read(buf, 0, buf.size)
                        if (read > 0) {
                            val rms = computeNormalizedRms(buf, read)
                            _audioLevel.value = rms
                            if (rms > peakRms) peakRms = rms
                        }
                        delay(50)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Calibration sample failed", e)
            } finally {
                runCatching { recorder?.stop() }
                runCatching { recorder?.release() }
                _audioLevel.value = 0f
            }
            mainHandler.post {
                onSampled(peakRms.coerceIn(0.04f, 0.35f))
            }
        }
    }

    private fun startOnDeviceVadLoop() {
        vadJob = scope.launch {
            val sampleRate = 16000
            val minBuf = AudioRecord.getMinBufferSize(
                sampleRate,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            ).coerceAtLeast(2048)

            try {
                audioRecord = AudioRecord(
                    MediaRecorder.AudioSource.VOICE_RECOGNITION,
                    sampleRate,
                    AudioFormat.CHANNEL_IN_MONO,
                    AudioFormat.ENCODING_PCM_16BIT,
                    minBuf
                )
                if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
                    Log.w(TAG, "AudioRecord not initialized; using SpeechRecognizer standby fallback")
                    return@launch
                }
                audioRecord?.startRecording()
                val buffer = ShortArray(minBuf / 2)
                var lastTriggerTime = 0L

                while (isActive && _mode.value == VoiceEngineMode.WAKE_STANDBY) {
                    if (isRecognizerBusy) {
                        delay(120)
                        continue
                    }
                    val read = audioRecord?.read(buffer, 0, buffer.size) ?: -1
                    if (read > 0) {
                        val rms = computeNormalizedRms(buffer, read)
                        _audioLevel.value = rms
                        val now = System.currentTimeMillis()
                        if (rms > 0.065f && now - lastTriggerTime > 2200L) {
                            lastTriggerTime = now
                            stopVadLoopOnly()
                            launchSpeechRecognizer(singleShotCommand = false)
                        }
                    }
                    delay(40)
                }
            } catch (e: SecurityException) {
                Log.e(TAG, "Missing RECORD_AUDIO permission", e)
            } catch (e: Exception) {
                Log.e(TAG, "VAD loop error", e)
            }
        }
    }

    private fun launchSpeechRecognizer(singleShotCommand: Boolean) {
        mainHandler.post {
            if (!SpeechRecognizer.isRecognitionAvailable(context)) {
                isRecognizerBusy = false
                onErrorMessage("Android SpeechRecognizer service is unavailable on this device.")
                return@post
            }

            try {
                isRecognizerBusy = true
                speechRecognizer?.destroy()
                speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                    setRecognitionListener(object : RecognitionListener {
                        override fun onReadyForSpeech(params: Bundle?) {}
                        override fun onBeginningOfSpeech() {}
                        override fun onRmsChanged(rmsdB: Float) {
                            val normalized = ((rmsdB + 2f) / 12f).coerceIn(0f, 1f)
                            _audioLevel.value = normalized
                        }

                        override fun onBufferReceived(buffer: ByteArray?) {}
                        override fun onEndOfSpeech() {
                            _audioLevel.value = 0.05f
                        }

                        override fun onError(error: Int) {
                            isRecognizerBusy = false
                            _audioLevel.value = 0f
                            Log.d(TAG, "SpeechRecognizer error code=$error, singleShot=$singleShotCommand")
                            if (_mode.value == VoiceEngineMode.WAKE_STANDBY) {
                                startOnDeviceVadLoop()
                            } else if (_mode.value == VoiceEngineMode.ACTIVE_LISTENING) {
                                _mode.value = VoiceEngineMode.IDLE
                            }
                        }

                        override fun onResults(results: Bundle?) {
                            isRecognizerBusy = false
                            val matches = results
                                ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                                .orEmpty()
                            val best = matches.firstOrNull().orEmpty()
                            _partialTranscript.value = best
                            _audioLevel.value = 0f

                            if (singleShotCommand) {
                                if (matches.any { WakeWordMatcher.matches(it, stopWord) }) {
                                    onStopWordDetected()
                                } else if (best.isNotBlank()) {
                                    onCommandTranscript(best)
                                }
                            } else {
                                if (matches.any { WakeWordMatcher.matches(it, wakeWord) }) {
                                    onWakeWordDetected()
                                } else if (_mode.value == VoiceEngineMode.WAKE_STANDBY) {
                                    startOnDeviceVadLoop()
                                }
                            }
                        }

                        override fun onPartialResults(partialResults: Bundle?) {
                            val partial = partialResults
                                ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                                ?.firstOrNull()
                                .orEmpty()
                            if (partial.isNotBlank()) {
                                _partialTranscript.value = partial
                                if (!singleShotCommand && WakeWordMatcher.matches(partial, wakeWord)) {
                                    isRecognizerBusy = false
                                    runCatching { speechRecognizer?.cancel() }
                                    onWakeWordDetected()
                                } else if (singleShotCommand && WakeWordMatcher.matches(partial, stopWord)) {
                                    isRecognizerBusy = false
                                    runCatching { speechRecognizer?.cancel() }
                                    onStopWordDetected()
                                }
                            }
                        }

                        override fun onEvent(eventType: Int, params: Bundle?) {}
                    })
                }

                val localeTag = if (languageCode == "gu") "gu-IN" else "en-IN"
                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, localeTag)
                    putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                    putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
                    if (preferOffline) {
                        putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
                    }
                }
                speechRecognizer?.startListening(intent)
            } catch (e: Exception) {
                isRecognizerBusy = false
                Log.e(TAG, "Failed to start SpeechRecognizer", e)
                onErrorMessage(e.localizedMessage ?: "Speech recognition failed.")
            }
        }
    }

    private fun computeNormalizedRms(buffer: ShortArray, readSize: Int): Float {
        var sumSq = 0.0
        for (i in 0 until readSize) {
            val sample = buffer[i] / 32768.0
            sumSq += sample * sample
        }
        val rms = sqrt(sumSq / readSize.coerceAtLeast(1))
        return (rms * 4.0).toFloat().coerceIn(0f, 1f)
    }

    private fun stopVadLoopOnly() {
        vadJob?.cancel()
        vadJob = null
        runCatching { audioRecord?.stop() }
        runCatching { audioRecord?.release() }
        audioRecord = null
    }

    private fun stopAllInternal() {
        stopVadLoopOnly()
        isRecognizerBusy = false
        mainHandler.post {
            runCatching { speechRecognizer?.cancel() }
            runCatching { speechRecognizer?.destroy() }
            speechRecognizer = null
        }
    }
}
