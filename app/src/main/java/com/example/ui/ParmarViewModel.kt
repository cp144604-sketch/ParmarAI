package com.example.ui

import android.Manifest
import android.app.Application
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.ai.AiStructuredReply
import com.example.ai.AssistantActionType
import com.example.ai.ParmarAiEngine
import com.example.control.ConnectivityMonitor
import com.example.control.DeviceAuthHelper
import com.example.control.InstalledAppInfo
import com.example.control.LocalCommandEngine
import com.example.control.PhoneActionResult
import com.example.control.PhoneController
import com.example.data.AssistantSettings
import com.example.data.BackupManager
import com.example.data.ChatMessageEntity
import com.example.data.MemoryEntity
import com.example.data.NoteEntity
import com.example.data.ParmarDatabase
import com.example.data.ParmarRepository
import com.example.data.ReminderEntity
import com.example.data.SettingsDataStore
import com.example.service.ParmarVoiceService
import com.example.service.ServiceStateBus
import com.example.service.VoiceServiceEvent
import com.example.voice.ParmarTtsManager
import com.example.voice.VoiceEngineMode
import com.example.voice.VoiceOption
import com.example.voice.WakeWordDetector
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Calendar

private const val TAG = "ParmarViewModel"

enum class AssistantPhase {
    STANDBY,
    GREETING,
    LISTENING,
    THINKING,
    SPEAKING,
    ERROR
}

enum class AiConnectionState {
    IDLE,
    TESTING,
    CONNECTED,
    ERROR
}

data class PendingMemoryProposal(
    val key: String,
    val value: String,
    val category: String
)

data class PermissionStatusSnapshot(
    val micGranted: Boolean = false,
    val notificationsGranted: Boolean = true,
    val exactAlarmAllowed: Boolean = true,
    val deviceAdminActive: Boolean = false,
    val deviceSecure: Boolean = false
)

data class ParmarUiState(
    val phase: AssistantPhase = AssistantPhase.STANDBY,
    val isNetworkAvailable: Boolean = true,
    val isEffectiveOffline: Boolean = false,
    val isWakeStandbyActive: Boolean = false,
    val isServiceRunning: Boolean = false,
    val audioLevel: Float = 0f,
    val liveTranscript: String = "",
    val lastParmarReply: String = "Hello Sir! હું PARMAR છું. હું તમારી કેવી રીતે મદદ કરી શકું?",
    val lastActionBadge: String = "",
    val isFlashlightOn: Boolean = false,
    val streamingDraft: String = "",
    val isAiBusy: Boolean = false,
    val errorMessage: String? = null,
    val infoToast: String? = null,
    val pendingMemory: PendingMemoryProposal? = null,
    val pendingAdminSetupIntent: Intent? = null,
    val privateMemoriesUnlocked: Boolean = false,
    val aiConnectionState: AiConnectionState = AiConnectionState.IDLE,
    val aiConnectionMessage: String = "",
    val installedAppsCount: Int = 0,
    val currentVolumePercent: Int = 50,
    val permissions: PermissionStatusSnapshot = PermissionStatusSnapshot()
)

class ParmarViewModel(application: Application) : AndroidViewModel(application) {

    private val context = application.applicationContext
    private val database = ParmarDatabase.getInstance(context)
    private val settingsStore = SettingsDataStore(context)
    private val backupManager = BackupManager(context)
    private val repository = ParmarRepository(database, settingsStore, backupManager)

    private val aiEngine = ParmarAiEngine()
    private val localEngine = LocalCommandEngine()
    private val phoneController = PhoneController(context)
    private val deviceAuthHelper = DeviceAuthHelper(context)
    private val connectivityMonitor = ConnectivityMonitor(context)
    private val ttsManager = ParmarTtsManager(context)

    private val wakeWordDetector = WakeWordDetector(
        context = context,
        onWakeWordDetected = { onWakeWordTriggered() },
        onStopWordDetected = { onStopWordTriggered() },
        onCommandTranscript = { transcript -> submitUserMessage(transcript, viaVoice = true) },
        onErrorMessage = { msg ->
            _uiState.update { it.copy(errorMessage = msg, phase = AssistantPhase.ERROR) }
        }
    )

    private val _uiState = MutableStateFlow(ParmarUiState())
    val uiState: StateFlow<ParmarUiState> = _uiState.asStateFlow()

    val settings: StateFlow<AssistantSettings> = repository.settings.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        AssistantSettings()
    )

    val notes: StateFlow<List<NoteEntity>> = repository.notes.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )

    val reminders: StateFlow<List<ReminderEntity>> = repository.reminders.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )

    val memories: StateFlow<List<MemoryEntity>> = repository.memories.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )

    val chatMessages: StateFlow<List<ChatMessageEntity>> = repository.chatMessages.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )

    val availableVoices: StateFlow<List<VoiceOption>> = ttsManager.availableVoices

    private var installedAppsCache: List<InstalledAppInfo> = emptyList()

    init {
        refreshSystemSnapshot()
        observeSettingsAndConnectivity()
        observeVoiceTelemetry()
    }

    private fun observeSettingsAndConnectivity() {
        viewModelScope.launch {
            combine(
                repository.settings,
                connectivityMonitor.isNetworkAvailable
            ) { s, net -> s to net }.collect { (s, net) ->
                val effectiveOffline = s.offlineModeForced || !net
                ttsManager.updatePreferences(
                    language = s.language,
                    speed = s.voiceSpeed,
                    pitch = s.voicePitch,
                    selectedVoiceName = s.selectedVoiceName
                )
                wakeWordDetector.updateConfig(
                    wakeWord = s.wakeWord,
                    stopWord = s.stopWord,
                    languageCode = s.language,
                    preferOffline = effectiveOffline
                )
                _uiState.update {
                    it.copy(
                        isNetworkAvailable = net,
                        isEffectiveOffline = effectiveOffline
                    )
                }
            }
        }
    }

    private fun observeVoiceTelemetry() {
        viewModelScope.launch {
            wakeWordDetector.mode.collect { mode ->
                val phase = when (mode) {
                    VoiceEngineMode.ACTIVE_LISTENING -> AssistantPhase.LISTENING
                    VoiceEngineMode.WAKE_STANDBY -> AssistantPhase.STANDBY
                    VoiceEngineMode.ERROR -> AssistantPhase.ERROR
                    VoiceEngineMode.IDLE -> if (_uiState.value.phase == AssistantPhase.LISTENING) {
                        AssistantPhase.STANDBY
                    } else _uiState.value.phase
                }
                _uiState.update {
                    it.copy(
                        phase = phase,
                        isWakeStandbyActive = mode == VoiceEngineMode.WAKE_STANDBY
                    )
                }
            }
        }

        viewModelScope.launch {
            combine(
                wakeWordDetector.audioLevel,
                ServiceStateBus.serviceAudioLevel
            ) { localLvl, svcLvl -> maxOf(localLvl, svcLvl) }.collect { lvl ->
                _uiState.update { it.copy(audioLevel = lvl) }
            }
        }

        viewModelScope.launch {
            wakeWordDetector.partialTranscript.collect { transcript ->
                if (transcript.isNotBlank()) {
                    _uiState.update { it.copy(liveTranscript = transcript) }
                }
            }
        }

        viewModelScope.launch {
            ttsManager.isSpeaking.collect { speaking ->
                _uiState.update { state ->
                    if (speaking) state.copy(phase = AssistantPhase.SPEAKING)
                    else if (state.phase == AssistantPhase.SPEAKING) state.copy(phase = AssistantPhase.STANDBY)
                    else state
                }
            }
        }

        viewModelScope.launch {
            ServiceStateBus.isForegroundListening.collect { running ->
                _uiState.update { it.copy(isServiceRunning = running) }
            }
        }

        viewModelScope.launch {
            ServiceStateBus.events.collect { event ->
                when (event) {
                    VoiceServiceEvent.WakeWordTriggered -> onWakeWordTriggered()
                    VoiceServiceEvent.StopWordTriggered -> onStopWordTriggered()
                    VoiceServiceEvent.ServiceStoppedByUser -> {
                        stopListeningAll()
                        updateSettings { it.copy(backgroundListeningEnabled = false) }
                    }
                }
            }
        }
    }

    fun refreshSystemSnapshot() {
        val micOk = ContextCompat.checkSelfPermission(
            context, Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
        val notifOk = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context, Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else true

        installedAppsCache = phoneController.getInstalledLauncherApps()
        _uiState.update {
            it.copy(
                installedAppsCount = installedAppsCache.size,
                currentVolumePercent = phoneController.getCurrentVolumePercent(),
                permissions = PermissionStatusSnapshot(
                    micGranted = micOk,
                    notificationsGranted = notifOk,
                    exactAlarmAllowed = phoneController.canScheduleExactAlarms(),
                    deviceAdminActive = phoneController.isDeviceAdminActive(),
                    deviceSecure = deviceAuthHelper.isDeviceSecured()
                )
            )
        }
    }

    fun toggleLanguage() {
        val next = if (settings.value.language == "gu") "en" else "gu"
        updateSettings { it.copy(language = next) }
    }

    fun onOrbTapped() {
        refreshSystemSnapshot()
        if (!_uiState.value.permissions.micGranted) {
            val lang = settings.value.language
            _uiState.update {
                it.copy(
                    errorMessage = if (lang == "gu") {
                        "માઇક્રોફોન પરમિશન જરૂરી છે. કૃપા કરીને Permissions સ્ક્રીનમાં મંજૂરી આપો."
                    } else {
                        "Microphone permission is required. Please grant it in Permissions & Privacy."
                    }
                )
            }
            return
        }
        when (_uiState.value.phase) {
            AssistantPhase.LISTENING, AssistantPhase.SPEAKING -> onStopWordTriggered()
            else -> startActiveVoiceCapture()
        }
    }

    fun onWakeWordTriggered() {
        val lang = settings.value.language
        val greeting = if (lang == "gu") {
            "Hello Sir! હું PARMAR છું. હું તમારી કેવી રીતે મદદ કરી શકું?"
        } else {
            "Hello Sir! I am PARMAR. How may I assist you?"
        }
        _uiState.update {
            it.copy(
                phase = AssistantPhase.GREETING,
                lastParmarReply = greeting,
                liveTranscript = "${settings.value.wakeWord} detected!",
                errorMessage = null
            )
        }
        ttsManager.speak(greeting)
        if (_uiState.value.permissions.micGranted) {
            wakeWordDetector.startActiveCommandListening()
        }
    }

    fun startActiveVoiceCapture() {
        ttsManager.stop()
        _uiState.update { it.copy(errorMessage = null, liveTranscript = "") }
        wakeWordDetector.startActiveCommandListening()
    }

    fun startStandbyWakeListening() {
        if (!_uiState.value.permissions.micGranted) return
        if (settings.value.backgroundListeningEnabled) {
            ParmarVoiceService.start(context)
        } else {
            wakeWordDetector.startStandbyWakeMonitoring()
        }
    }

    fun onStopWordTriggered() {
        ttsManager.stop()
        wakeWordDetector.stopAll()
        val lang = settings.value.language
        val bye = if (lang == "gu") {
            "ઠીક છે Sir, PARMAR સ્ટેન્ડબાય મોડમાં જાય છે."
        } else {
            "Understood Sir, PARMAR is now on standby."
        }
        _uiState.update {
            it.copy(
                phase = AssistantPhase.STANDBY,
                lastParmarReply = bye,
                isWakeStandbyActive = false
            )
        }
        if (settings.value.backgroundListeningEnabled && _uiState.value.permissions.micGranted) {
            ParmarVoiceService.start(context)
        }
    }

    fun stopListeningAll() {
        ttsManager.stop()
        wakeWordDetector.stopAll()
        ParmarVoiceService.stop(context)
        _uiState.update {
            it.copy(
                phase = AssistantPhase.STANDBY,
                isWakeStandbyActive = false,
                isServiceRunning = false,
                audioLevel = 0f
            )
        }
    }

    fun setBackgroundListening(enabled: Boolean) {
        updateSettings { it.copy(backgroundListeningEnabled = enabled) }
        if (enabled && _uiState.value.permissions.micGranted) {
            wakeWordDetector.stopAll()
            ParmarVoiceService.start(context)
        } else {
            ParmarVoiceService.stop(context)
        }
    }

    fun submitUserMessage(text: String, viaVoice: Boolean = false) {
        val clean = text.trim()
        if (clean.isEmpty()) return

        viewModelScope.launch {
            val currentSettings = settings.value
            val isOffline = _uiState.value.isEffectiveOffline
            val lang = currentSettings.language

            repository.addChatMessage(role = "user", text = clean)
            _uiState.update {
                it.copy(
                    phase = AssistantPhase.THINKING,
                    isAiBusy = true,
                    errorMessage = null,
                    liveTranscript = clean,
                    streamingDraft = ""
                )
            }

            // 1. Check stop word immediately
            if (com.example.voice.WakeWordMatcher.matches(clean, currentSettings.stopWord) && viaVoice) {
                _uiState.update { it.copy(isAiBusy = false) }
                onStopWordTriggered()
                return@launch
            }

            // 2. Try deterministic local command engine (works 100% offline & ultra fast online)
            val snapshot = repository.getSnapshotForAi()
            val localResult = localEngine.tryHandleLocally(
                input = clean,
                language = lang,
                stopWord = currentSettings.stopWord,
                notes = snapshot.notes,
                reminders = snapshot.reminders,
                isOffline = isOffline
            )

            if (localResult != null) {
                finalizeStructuredReply(localResult, isOffline = isOffline, speakOut = viaVoice)
                return@launch
            }

            // 3. Online Gemini 3.8 Flash via Firebase AI Logic
            try {
                val structuredResult = aiEngine.generateStructuredResponse(
                    userInput = clean,
                    language = lang,
                    memories = snapshot.memories,
                    notes = snapshot.notes,
                    reminders = snapshot.reminders,
                    recentHistory = snapshot.recentMessages
                )
                structuredResult.onSuccess { reply ->
                    finalizeStructuredReply(reply, isOffline = false, speakOut = viaVoice)
                }.onFailure { err ->
                    Log.e(TAG, "AI failure", err)
                    val msg = err.localizedMessage ?: "AI request failed."
                    _uiState.update {
                        it.copy(
                            isAiBusy = false,
                            phase = AssistantPhase.ERROR,
                            errorMessage = msg
                        )
                    }
                    repository.addChatMessage(
                        role = "parmar",
                        text = if (lang == "gu") "Sir, AI કનેક્શનમાં ભૂલ આવી: $msg" else "Sir, AI error: $msg",
                        isOfflineResponse = false
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Unexpected AI error", e)
                _uiState.update {
                    it.copy(
                        isAiBusy = false,
                        phase = AssistantPhase.ERROR,
                        errorMessage = e.localizedMessage ?: "Unexpected AI error"
                    )
                }
            }
        }
    }

    fun sendStreamingChatQuestion(question: String) {
        val clean = question.trim()
        if (clean.isEmpty()) return
        if (_uiState.value.isEffectiveOffline) {
            submitUserMessage(clean, viaVoice = false)
            return
        }

        // First see if it's a device control command
        viewModelScope.launch {
            val s = settings.value
            val snapshot = repository.getSnapshotForAi()
            val local = localEngine.tryHandleLocally(
                input = clean,
                language = s.language,
                stopWord = s.stopWord,
                notes = snapshot.notes,
                reminders = snapshot.reminders,
                isOffline = false
            )
            if (local != null) {
                repository.addChatMessage(role = "user", text = clean)
                finalizeStructuredReply(local, isOffline = false, speakOut = false)
                return@launch
            }

            // If message likely requires a device action (e.g., open app, note, reminder, torch, lock),
            // use structured tool engine; otherwise stream smoothly!
            val actionHints = listOf("open", "ખોલ", "note", "નોટ", "remind", "રિમાઇન્ડર", "torch", "ફ્લેશ", "alarm", "એલાર્મ", "remember", "યાદ રાખ", "lock", "લોક")
            if (actionHints.any { clean.lowercase().contains(it) }) {
                submitUserMessage(clean, viaVoice = false)
                return@launch
            }

            repository.addChatMessage(role = "user", text = clean)
            _uiState.update {
                it.copy(isAiBusy = true, phase = AssistantPhase.THINKING, streamingDraft = "", errorMessage = null)
            }
            var finalText = ""
            try {
                aiEngine.streamConversationalReply(
                    userInput = clean,
                    language = s.language,
                    memories = snapshot.memories,
                    notes = snapshot.notes,
                    reminders = snapshot.reminders,
                    recentHistory = snapshot.recentMessages
                ).collect { accumulated ->
                    finalText = accumulated
                    _uiState.update { it.copy(streamingDraft = accumulated) }
                }
                if (finalText.isNotBlank()) {
                    repository.addChatMessage(role = "parmar", text = finalText, isOfflineResponse = false)
                    _uiState.update {
                        it.copy(
                            isAiBusy = false,
                            streamingDraft = "",
                            lastParmarReply = finalText,
                            phase = AssistantPhase.STANDBY
                        )
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Streaming chat failed", e)
                _uiState.update {
                    it.copy(
                        isAiBusy = false,
                        streamingDraft = "",
                        phase = AssistantPhase.ERROR,
                        errorMessage = e.localizedMessage ?: "Streaming chat failed."
                    )
                }
            }
        }
    }

    private suspend fun finalizeStructuredReply(
        reply: AiStructuredReply,
        isOffline: Boolean,
        speakOut: Boolean
    ) {
        val lang = settings.value.language
        var finalText = reply.reply
        var actionLabel = ""
        var setupIntent: Intent? = null

        when (reply.action) {
            AssistantActionType.READ_TIME -> {
                finalText = phoneController.readDateAndTime(lang)
                actionLabel = "TIME"
            }

            AssistantActionType.TOGGLE_FLASHLIGHT -> {
                val turnOn = when (reply.actionParam.lowercase()) {
                    "on", "true" -> true
                    "off", "false" -> false
                    else -> null
                }
                val res = phoneController.toggleFlashlight(turnOn)
                finalText = extractPhoneResultText(res, lang) { setupIntent = it }
                actionLabel = "FLASHLIGHT"
                _uiState.update { it.copy(isFlashlightOn = phoneController.isTorchOn) }
            }

            AssistantActionType.OPEN_APP -> {
                val res = phoneController.openAppByName(reply.actionParam)
                finalText = extractPhoneResultText(res, lang) { setupIntent = it }
                actionLabel = "OPEN APP"
            }

            AssistantActionType.CREATE_NOTE -> {
                val content = reply.actionParam.ifBlank { _uiState.value.liveTranscript }
                if (content.isNotBlank()) {
                    repository.addNote(
                        title = content.take(36),
                        content = content,
                        category = "Voice"
                    )
                    actionLabel = "NOTE SAVED"
                }
            }

            AssistantActionType.CREATE_REMINDER -> {
                val title = reply.actionParam.ifBlank { "Reminder" }
                val triggerMillis = parseTimeHintToMillis(reply.timeHint)
                val id = repository.addReminder(title = title, triggerAtMillis = triggerMillis)
                phoneController.scheduleLocalReminderAlarm(
                    ReminderEntity(id = id, title = title, triggerAtMillis = triggerMillis)
                )
                actionLabel = "REMINDER SET"
            }

            AssistantActionType.SET_ALARM -> {
                val cal = Calendar.getInstance().apply { add(Calendar.HOUR_OF_DAY, 1) }
                val res = phoneController.setSystemAlarm(
                    hour = cal.get(Calendar.HOUR_OF_DAY),
                    minute = 0,
                    message = reply.actionParam.ifBlank { "PARMAR AI" }
                )
                finalText = extractPhoneResultText(res, lang) { setupIntent = it }
                actionLabel = "ALARM"
            }

            AssistantActionType.OPEN_BLUETOOTH_SETTINGS -> {
                val res = phoneController.openBluetoothSettings()
                finalText = extractPhoneResultText(res, lang) { setupIntent = it }
                actionLabel = "BLUETOOTH"
            }

            AssistantActionType.OPEN_WIFI_SETTINGS -> {
                val res = phoneController.openNetworkSettings()
                finalText = extractPhoneResultText(res, lang) { setupIntent = it }
                actionLabel = "NETWORK"
            }

            AssistantActionType.SET_VOLUME -> {
                val pct = reply.actionParam.toIntOrNull()
                val res = phoneController.adjustAssistantVolume(reply.actionParam, pct)
                finalText = extractPhoneResultText(res, lang) { setupIntent = it }
                _uiState.update { it.copy(currentVolumePercent = phoneController.getCurrentVolumePercent()) }
                actionLabel = "VOLUME"
            }

            AssistantActionType.LOCK_DEVICE -> {
                val res = phoneController.lockPhone()
                finalText = extractPhoneResultText(res, lang) { setupIntent = it }
                actionLabel = "LOCK"
            }

            AssistantActionType.SWITCH_LANGUAGE -> {
                val nextLang = if (reply.actionParam.lowercase().startsWith("en")) "en" else "gu"
                updateSettings { it.copy(language = nextLang) }
                actionLabel = "LANGUAGE: ${nextLang.uppercase()}"
            }

            AssistantActionType.PROPOSE_MEMORY -> {
                if (reply.memoryKey.isNotBlank() && reply.memoryValue.isNotBlank()) {
                    _uiState.update {
                        it.copy(
                            pendingMemory = PendingMemoryProposal(
                                key = reply.memoryKey,
                                value = reply.memoryValue,
                                category = reply.memoryCategory
                            )
                        )
                    }
                    actionLabel = "MEMORY PROPOSAL"
                }
            }

            AssistantActionType.CHECK_AGENDA -> {
                actionLabel = "AGENDA"
            }

            AssistantActionType.NONE -> Unit
        }

        if (finalText.isBlank()) {
            finalText = if (lang == "gu") "Sir, કાર્ય પૂર્ણ કર્યું છે." else "Sir, done."
        }

        repository.addChatMessage(
            role = "parmar",
            text = finalText,
            isOfflineResponse = isOffline,
            actionTaken = actionLabel.ifBlank { null }
        )

        _uiState.update {
            it.copy(
                isAiBusy = false,
                lastParmarReply = finalText,
                lastActionBadge = actionLabel,
                pendingAdminSetupIntent = setupIntent,
                phase = if (speakOut) AssistantPhase.SPEAKING else AssistantPhase.STANDBY
            )
        }

        if (speakOut) {
            ttsManager.speak(finalText)
        }
    }

    private fun extractPhoneResultText(
        res: PhoneActionResult,
        language: String,
        onNeedsSetup: (Intent) -> Unit
    ): String {
        return when (res) {
            is PhoneActionResult.Success -> if (language == "gu") res.messageGu else res.messageEn
            is PhoneActionResult.Failure -> if (language == "gu") res.messageGu else res.messageEn
            is PhoneActionResult.NeedsSetup -> {
                onNeedsSetup(res.setupIntent)
                if (language == "gu") res.messageGu else res.messageEn
            }
        }
    }

    private fun parseTimeHintToMillis(hint: String): Long {
        val now = System.currentTimeMillis()
        val lower = hint.lowercase()
        val minutesMatch = Regex("(\\d+)\\s*(min|minute|મિનિટ)").find(lower)
        if (minutesMatch != null) {
            val mins = minutesMatch.groupValues[1].toLongOrNull() ?: 15L
            return now + mins * 60_000L
        }
        val hoursMatch = Regex("(\\d+)\\s*(hr|hour|કલાક)").find(lower)
        if (hoursMatch != null) {
            val hrs = hoursMatch.groupValues[1].toLongOrNull() ?: 1L
            return now + hrs * 3_600_000L
        }
        return now + 15 * 60_000L
    }

    // Quick Phone Control shortcuts
    fun quickToggleFlashlight() {
        val res = phoneController.toggleFlashlight()
        val msg = extractPhoneResultText(res, settings.value.language) {}
        _uiState.update { it.copy(isFlashlightOn = phoneController.isTorchOn, infoToast = msg) }
    }

    fun quickCheckAgenda() = submitUserMessage(
        if (settings.value.language == "gu") "આજે શું કામ કરવાનું છે?" else "What do I have today?",
        viaVoice = true
    )

    fun quickReadTime() = submitUserMessage(
        if (settings.value.language == "gu") "સમય અને તારીખ જણાવો" else "What time is it?",
        viaVoice = true
    )

    fun quickOpenBluetooth() = phoneController.openBluetoothSettings()
    fun quickOpenWifi() = phoneController.openNetworkSettings()
    fun quickOpenBatterySettings() = phoneController.openBatteryOptimizationSettings()
    fun quickOpenAppSettings() = phoneController.openAppDetailsSettings()
    fun createDeviceAdminIntent(): Intent = phoneController.createDeviceAdminSetupIntent()
    fun createExactAlarmIntent(): Intent? = phoneController.createExactAlarmPermissionIntent()

    fun quickLockPhone(): Intent? {
        val res = phoneController.lockPhone()
        val msg = extractPhoneResultText(res, settings.value.language) { setup ->
            _uiState.update { it.copy(pendingAdminSetupIntent = setup) }
        }
        _uiState.update { it.copy(infoToast = msg) }
        return (res as? PhoneActionResult.NeedsSetup)?.setupIntent
    }

    fun adjustVolumePercent(percent: Int) {
        phoneController.adjustAssistantVolume("set", percent)
        _uiState.update { it.copy(currentVolumePercent = phoneController.getCurrentVolumePercent()) }
    }

    // Memory Proposal actions
    fun approvePendingMemory() {
        val proposal = _uiState.value.pendingMemory ?: return
        viewModelScope.launch {
            repository.addMemory(
                key = proposal.key,
                value = proposal.value,
                category = proposal.category
            )
            _uiState.update {
                it.copy(
                    pendingMemory = null,
                    infoToast = if (settings.value.language == "gu") "મેમરી સેવ થઈ ગઈ Sir!" else "Memory saved, Sir!"
                )
            }
        }
    }

    fun dismissPendingMemory() {
        _uiState.update { it.copy(pendingMemory = null) }
    }

    // Notes CRUD
    fun saveNote(id: Long? = null, title: String, content: String, category: String, isPinned: Boolean = false) {
        viewModelScope.launch {
            if (id == null || id == 0L) {
                repository.addNote(title, content, category)
            } else {
                repository.updateNote(NoteEntity(id = id, title = title, content = content, category = category, isPinned = isPinned))
            }
        }
    }

    fun toggleNotePin(note: NoteEntity) {
        viewModelScope.launch { repository.updateNote(note.copy(isPinned = !note.isPinned)) }
    }

    fun deleteNote(id: Long) {
        viewModelScope.launch { repository.deleteNote(id) }
    }

    // Reminders CRUD
    fun saveReminder(title: String, triggerAtMillis: Long, repeatType: String = "ONCE") {
        viewModelScope.launch {
            val id = repository.addReminder(title, triggerAtMillis, repeatType)
            phoneController.scheduleLocalReminderAlarm(
                ReminderEntity(id = id, title = title, triggerAtMillis = triggerAtMillis, repeatType = repeatType)
            )
        }
    }

    fun toggleReminderComplete(reminder: ReminderEntity) {
        viewModelScope.launch {
            val updated = reminder.copy(isCompleted = !reminder.isCompleted)
            repository.updateReminder(updated)
            if (updated.isCompleted) {
                phoneController.cancelLocalReminderAlarm(updated.id)
            } else {
                phoneController.scheduleLocalReminderAlarm(updated)
            }
        }
    }

    fun deleteReminder(id: Long) {
        viewModelScope.launch {
            phoneController.cancelLocalReminderAlarm(id)
            repository.deleteReminder(id)
        }
    }

    // Memory CRUD + Device Auth Protection
    fun addMemoryDirect(key: String, value: String, category: String, isPrivate: Boolean) {
        viewModelScope.launch {
            repository.addMemory(key, value, category, isPrivate)
        }
    }

    fun deleteMemory(id: Long) {
        viewModelScope.launch { repository.deleteMemory(id) }
    }

    fun clearAllMemoriesGuarded() {
        if (settings.value.requireAuthForPrivateActions && deviceAuthHelper.isDeviceSecured()) {
            deviceAuthHelper.authenticate(
                title = "PARMAR AI Security",
                subtitle = "Verify device identity to erase saved memories",
                onSuccess = { viewModelScope.launch { repository.clearAllMemories() } },
                onError = { err -> _uiState.update { it.copy(errorMessage = err) } }
            )
        } else {
            viewModelScope.launch { repository.clearAllMemories() }
        }
    }

    fun unlockPrivateMemories() {
        if (!settings.value.requireAuthForPrivateActions || !deviceAuthHelper.isDeviceSecured()) {
            _uiState.update { it.copy(privateMemoriesUnlocked = true) }
            return
        }
        deviceAuthHelper.authenticate(
            title = "Unlock Private Memories",
            subtitle = "Confirm device PIN, pattern, or biometric",
            onSuccess = { _uiState.update { it.copy(privateMemoriesUnlocked = true) } },
            onError = { err -> _uiState.update { it.copy(errorMessage = err) } }
        )
    }

    fun lockPrivateMemories() {
        _uiState.update { it.copy(privateMemoriesUnlocked = false) }
    }

    // Backup & Restore
    fun exportBackup(uri: Uri) {
        viewModelScope.launch {
            val res = repository.exportBackupToUri(uri)
            res.onSuccess { count ->
                _uiState.update {
                    it.copy(infoToast = "Backup exported ($count items)")
                }
            }.onFailure { err ->
                _uiState.update { it.copy(errorMessage = err.localizedMessage ?: "Export failed") }
            }
        }
    }

    fun importBackup(uri: Uri) {
        viewModelScope.launch {
            val res = repository.importBackupFromUri(uri)
            res.onSuccess { count ->
                val allReminders = repository.reminders.first()
                allReminders.forEach { phoneController.scheduleLocalReminderAlarm(it) }
                _uiState.update { it.copy(infoToast = "Restored $count items successfully") }
            }.onFailure { err ->
                _uiState.update { it.copy(errorMessage = err.localizedMessage ?: "Restore failed") }
            }
        }
    }

    // Settings & Diagnostics
    fun updateSettings(transform: (AssistantSettings) -> AssistantSettings) {
        viewModelScope.launch { repository.updateSettings(transform) }
    }

    fun testVoiceOutput() {
        val lang = settings.value.language
        val sample = if (lang == "gu") {
            "Hello Sir! હું PARMAR છું. મારો અવાજ અને ઝડપ બરાબર છે?"
        } else {
            "Hello Sir! I am PARMAR. How does my voice sound?"
        }
        ttsManager.speak(sample)
    }

    fun calibrateVoiceProfile() {
        wakeWordDetector.sampleVoiceEnergyOnce { baseline ->
            updateSettings {
                it.copy(
                    voiceProfileEnabled = true,
                    voiceProfileEnergyBaseline = baseline
                )
            }
            _uiState.update {
                it.copy(infoToast = "Voice profile calibrated (baseline: ${String.format("%.2f", baseline)})")
            }
        }
    }

    fun testAiConnection() {
        viewModelScope.launch {
            _uiState.update { it.copy(aiConnectionState = AiConnectionState.TESTING, aiConnectionMessage = "Connecting to Gemini 3.8 Flash...") }
            val result = aiEngine.testConnection()
            result.onSuccess { reply ->
                _uiState.update {
                    it.copy(
                        aiConnectionState = AiConnectionState.CONNECTED,
                        aiConnectionMessage = "Connected: $reply"
                    )
                }
            }.onFailure { err ->
                _uiState.update {
                    it.copy(
                        aiConnectionState = AiConnectionState.ERROR,
                        aiConnectionMessage = err.localizedMessage ?: "Connection failed"
                    )
                }
            }
        }
    }

    fun clearChatHistory() {
        viewModelScope.launch { repository.clearChatHistory() }
    }

    fun clearPendingAdminIntent() {
        _uiState.update { it.copy(pendingAdminSetupIntent = null) }
    }

    fun dismissError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun dismissToast() {
        _uiState.update { it.copy(infoToast = null) }
    }

    override fun onCleared() {
        super.onCleared()
        wakeWordDetector.stopAll()
        ttsManager.shutdown()
        connectivityMonitor.release()
    }
}
