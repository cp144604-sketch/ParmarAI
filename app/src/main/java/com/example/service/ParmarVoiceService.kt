package com.example.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import com.example.MainActivity
import com.example.R
import com.example.data.SettingsDataStore
import com.example.voice.WakeWordDetector
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class ParmarVoiceService : Service() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var wakeWordDetector: WakeWordDetector? = null
    private var wakeLock: PowerManager.WakeLock? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP_LISTENING) {
            ServiceStateBus.emitEvent(VoiceServiceEvent.ServiceStoppedByUser)
            stopSelfCleanly()
            return START_NOT_STICKY
        }

        val notification = buildOngoingNotification()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            ServiceCompat.startForeground(
                this,
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }

        serviceScope.launch {
            val settings = SettingsDataStore(applicationContext).settingsFlow.first()
            if (settings.screenOffWakeEnabled) {
                acquirePartialWakeLock()
            }
            if (wakeWordDetector == null) {
                wakeWordDetector = WakeWordDetector(
                    context = applicationContext,
                    onWakeWordDetected = {
                        ServiceStateBus.emitEvent(VoiceServiceEvent.WakeWordTriggered)
                    },
                    onStopWordDetected = {
                        ServiceStateBus.emitEvent(VoiceServiceEvent.StopWordTriggered)
                    },
                    onCommandTranscript = {},
                    onErrorMessage = {}
                )
            }
            wakeWordDetector?.updateConfig(
                wakeWord = settings.wakeWord,
                stopWord = settings.stopWord,
                languageCode = settings.language,
                preferOffline = true
            )
            wakeWordDetector?.startStandbyWakeMonitoring()
            ServiceStateBus.setForegroundListening(true)

            launch {
                wakeWordDetector?.audioLevel?.collect { lvl ->
                    ServiceStateBus.updateAudioLevel(lvl)
                }
            }
        }

        return START_STICKY
    }

    private fun acquirePartialWakeLock() {
        if (wakeLock?.isHeld == true) return
        val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "ParmarAI::WakeWordLock").apply {
            setReferenceCounted(false)
            acquire(30 * 60 * 1000L) // 30-minute safety cap for battery protection
        }
    }

    private fun stopSelfCleanly() {
        wakeWordDetector?.stopAll()
        wakeWordDetector = null
        runCatching { if (wakeLock?.isHeld == true) wakeLock?.release() }
        wakeLock = null
        ServiceStateBus.setForegroundListening(false)
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        wakeWordDetector?.stopAll()
        runCatching { if (wakeLock?.isHeld == true) wakeLock?.release() }
        ServiceStateBus.setForegroundListening(false)
        serviceScope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.voice_service_channel_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = getString(R.string.voice_service_channel_desc)
                setShowBadge(false)
            }
            manager.createNotificationChannel(channel)
        }
    }

    private fun buildOngoingNotification(): Notification {
        val openIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val openPending = PendingIntent.getActivity(
            this,
            101,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = Intent(this, ParmarVoiceService::class.java).apply {
            action = ACTION_STOP_LISTENING
        }
        val stopPending = PendingIntent.getService(
            this,
            102,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("PARMAR AI • Mic Standby Active")
            .setContentText("Say \"DIAMOND\" to wake PARMAR. Tap Stop to turn off mic.")
            .setOngoing(true)
            .setContentIntent(openPending)
            .addAction(0, "Stop / બંધ કરો", stopPending)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    companion object {
        const val CHANNEL_ID = "parmar_voice_channel"
        const val NOTIFICATION_ID = 1001
        const val ACTION_STOP_LISTENING = "com.example.action.STOP_LISTENING"

        fun start(context: Context) {
            val intent = Intent(context, ParmarVoiceService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, ParmarVoiceService::class.java)
            context.stopService(intent)
        }
    }
}
