package com.example.service

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow

sealed interface VoiceServiceEvent {
    data object WakeWordTriggered : VoiceServiceEvent
    data object StopWordTriggered : VoiceServiceEvent
    data object ServiceStoppedByUser : VoiceServiceEvent
}

object ServiceStateBus {
    private val _isForegroundListening = MutableStateFlow(false)
    val isForegroundListening: StateFlow<Boolean> = _isForegroundListening.asStateFlow()

    private val _serviceAudioLevel = MutableStateFlow(0f)
    val serviceAudioLevel: StateFlow<Float> = _serviceAudioLevel.asStateFlow()

    private val _events = MutableSharedFlow<VoiceServiceEvent>(extraBufferCapacity = 8)
    val events: SharedFlow<VoiceServiceEvent> = _events.asSharedFlow()

    fun setForegroundListening(active: Boolean) {
        _isForegroundListening.value = active
        if (!active) _serviceAudioLevel.value = 0f
    }

    fun updateAudioLevel(level: Float) {
        _serviceAudioLevel.value = level
    }

    fun emitEvent(event: VoiceServiceEvent) {
        _events.tryEmit(event)
    }
}
