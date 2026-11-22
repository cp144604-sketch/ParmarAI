package com.example.control

import android.app.AlarmManager
import android.app.PendingIntent
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.media.AudioManager
import android.net.Uri
import android.os.Build
import android.provider.AlarmClock
import android.provider.Settings
import android.util.Log
import com.example.data.ReminderEntity
import com.example.service.ReminderReceiver
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private const val TAG = "PhoneController"

data class InstalledAppInfo(
    val label: String,
    val packageName: String
)

sealed interface PhoneActionResult {
    data class Success(val messageGu: String, val messageEn: String) : PhoneActionResult
    data class NeedsSetup(val messageGu: String, val messageEn: String, val setupIntent: Intent) : PhoneActionResult
    data class Failure(val messageGu: String, val messageEn: String) : PhoneActionResult
}

class PhoneController(private val context: Context) {

    private val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    private val devicePolicyManager =
        context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
    private val adminComponent = ComponentName(context, ParmarDeviceAdminReceiver::class.java)

    var isTorchOn: Boolean = false
        private set

    fun toggleFlashlight(turnOn: Boolean? = null): PhoneActionResult {
        return try {
            val targetCameraId = cameraManager.cameraIdList.firstOrNull { id ->
                val chars = cameraManager.getCameraCharacteristics(id)
                chars.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
            } ?: return PhoneActionResult.Failure(
                "Sir, આ ડિવાઇસમાં ફ્લેશલાઇટ હાર્ડવેર ઉપલબ્ધ નથી.",
                "Sir, flashlight hardware is not available on this device."
            )

            val nextState = turnOn ?: !isTorchOn
            cameraManager.setTorchMode(targetCameraId, nextState)
            isTorchOn = nextState
            if (nextState) {
                PhoneActionResult.Success(
                    "Sir, ફ્લેશલાઇટ ચાલુ કરી દીધી છે.",
                    "Sir, flashlight has been turned ON."
                )
            } else {
                PhoneActionResult.Success(
                    "Sir, ફ્લેશલાઇટ બંધ કરી દીધી છે.",
                    "Sir, flashlight has been turned OFF."
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Flashlight error", e)
            PhoneActionResult.Failure(
                "Sir, ફ્લેશલાઇટ નિયંત્રિત કરવામાં સમસ્યા આવી: ${e.localizedMessage}",
                "Sir, unable to control flashlight: ${e.localizedMessage}"
            )
        }
    }

    fun getInstalledLauncherApps(): List<InstalledAppInfo> {
        return try {
            val pm = context.packageManager
            val mainIntent = Intent(Intent.ACTION_MAIN, null).apply {
                addCategory(Intent.CATEGORY_LAUNCHER)
            }
            val resolveInfos = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                pm.queryIntentActivities(mainIntent, PackageManager.ResolveInfoFlags.of(0L))
            } else {
                @Suppress("DEPRECATION")
                pm.queryIntentActivities(mainIntent, 0)
            }
            resolveInfos
                .map { info ->
                    InstalledAppInfo(
                        label = info.loadLabel(pm).toString(),
                        packageName = info.activityInfo.packageName
                    )
                }
                .distinctBy { it.packageName }
                .sortedBy { it.label.lowercase() }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to query launcher apps", e)
            emptyList()
        }
    }

    fun openAppByName(rawQuery: String): PhoneActionResult {
        val normalized = normalizeAppQuery(rawQuery)
        if (normalized.isBlank()) {
            return PhoneActionResult.Failure(
                "Sir, કઈ એપ્લિકેશન ખોલવી છે તે સ્પષ્ટ જણાવો.",
                "Sir, please specify which application you would like to open."
            )
        }

        val apps = getInstalledLauncherApps()
        val matched = apps.firstOrNull {
            it.label.equals(normalized, ignoreCase = true)
        } ?: apps.firstOrNull {
            it.label.lowercase().contains(normalized) || it.packageName.lowercase().contains(normalized)
        }

        if (matched == null) {
            return PhoneActionResult.Failure(
                "Sir, '$rawQuery' એપ્લિકેશન ફોનમાં ઇન્સ્ટોલ થયેલી મળી નથી.",
                "Sir, could not find installed app '$rawQuery'."
            )
        }

        return try {
            val launchIntent = context.packageManager.getLaunchIntentForPackage(matched.packageName)
            if (launchIntent != null) {
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(launchIntent)
                PhoneActionResult.Success(
                    "Sir, ${matched.label} ખોલી રહી છું.",
                    "Sir, opening ${matched.label}."
                )
            } else {
                PhoneActionResult.Failure(
                    "Sir, ${matched.label} લોન્ચ કરી શકાતી નથી.",
                    "Sir, unable to launch ${matched.label}."
                )
            }
        } catch (e: Exception) {
            PhoneActionResult.Failure(
                "Sir, એપ ખોલવામાં ભૂલ આવી: ${e.localizedMessage}",
                "Sir, error launching app: ${e.localizedMessage}"
            )
        }
    }

    fun readDateAndTime(language: String): String {
        val now = Date()
        return if (language == "gu") {
            val timeFmt = SimpleDateFormat("hh:mm a", Locale("gu", "IN"))
            val dateFmt = SimpleDateFormat("EEEE, dd MMMM yyyy", Locale("gu", "IN"))
            "Sir, હાલ સમય ${timeFmt.format(now)} થયો છે અને તારીખ ${dateFmt.format(now)} છે."
        } else {
            val timeFmt = SimpleDateFormat("hh:mm a", Locale.ENGLISH)
            val dateFmt = SimpleDateFormat("EEEE, dd MMMM yyyy", Locale.ENGLISH)
            "Sir, the current time is ${timeFmt.format(now)} on ${dateFmt.format(now)}."
        }
    }

    fun adjustAssistantVolume(mode: String, targetPercent: Int? = null): PhoneActionResult {
        return try {
            val stream = AudioManager.STREAM_MUSIC
            val maxVol = audioManager.getStreamMaxVolume(stream)
            val currentVol = audioManager.getStreamVolume(stream)

            val newVol = when {
                targetPercent != null -> ((targetPercent.coerceIn(0, 100) / 100f) * maxVol).toInt()
                mode.equals("up", true) -> (currentVol + (maxVol / 5).coerceAtLeast(1)).coerceAtMost(maxVol)
                mode.equals("down", true) -> (currentVol - (maxVol / 5).coerceAtLeast(1)).coerceAtLeast(0)
                mode.equals("mute", true) -> 0
                mode.equals("max", true) -> maxVol
                else -> currentVol
            }

            audioManager.setStreamVolume(stream, newVol, AudioManager.FLAG_SHOW_UI)
            val pct = if (maxVol > 0) (newVol * 100) / maxVol else 0
            PhoneActionResult.Success(
                "Sir, આસિસ્ટન્ટ વોલ્યુમ $pct% પર સેટ કર્યું છે.",
                "Sir, assistant volume set to $pct%."
            )
        } catch (e: Exception) {
            PhoneActionResult.Failure(
                "Sir, વોલ્યુમ બદલવામાં સમસ્યા આવી.",
                "Sir, unable to adjust volume."
            )
        }
    }

    fun getCurrentVolumePercent(): Int {
        val maxVol = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        val curVol = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
        return if (maxVol > 0) (curVol * 100) / maxVol else 50
    }

    fun openBluetoothSettings(): PhoneActionResult {
        return launchSettingsIntent(
            Intent(Settings.ACTION_BLUETOOTH_SETTINGS),
            "Sir, બ્લુટુથ સેટિંગ્સ ખોલી રહી છું.",
            "Sir, opening Bluetooth settings."
        )
    }

    fun openNetworkSettings(): PhoneActionResult {
        val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            Intent(Settings.Panel.ACTION_INTERNET_CONNECTIVITY)
        } else {
            Intent(Settings.ACTION_WIFI_SETTINGS)
        }
        return launchSettingsIntent(
            intent,
            "Sir, નેટવર્ક અને Wi-Fi સેટિંગ્સ ખોલી રહી છું.",
            "Sir, opening Network and Wi-Fi settings."
        )
    }

    fun openBatteryOptimizationSettings(): PhoneActionResult {
        return launchSettingsIntent(
            Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS),
            "Sir, બેટરી ઓપ્ટિમાઇઝેશન સેટિંગ્સ ખોલી રહી છું.",
            "Sir, opening battery optimization settings."
        )
    }

    fun openAppDetailsSettings(): PhoneActionResult {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", context.packageName, null)
        }
        return launchSettingsIntent(
            intent,
            "Sir, એપ્લિકેશન પરમિશન સેટિંગ્સ ખોલી રહી છું.",
            "Sir, opening app permission settings."
        )
    }

    fun setSystemAlarm(hour: Int, minute: Int, message: String): PhoneActionResult {
        val intent = Intent(AlarmClock.ACTION_SET_ALARM).apply {
            putExtra(AlarmClock.EXTRA_HOUR, hour.coerceIn(0, 23))
            putExtra(AlarmClock.EXTRA_MINUTES, minute.coerceIn(0, 59))
            putExtra(AlarmClock.EXTRA_MESSAGE, message.ifBlank { "PARMAR AI Alarm" })
            putExtra(AlarmClock.EXTRA_SKIP_UI, false)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return try {
            context.startActivity(intent)
            PhoneActionResult.Success(
                "Sir, ${String.format(Locale.getDefault(), "%02d:%02d", hour, minute)} નો એલાર્મ સેટ કરવા ઘડિયાળ ખોલી છે.",
                "Sir, opened Clock to set alarm for ${String.format(Locale.getDefault(), "%02d:%02d", hour, minute)}."
            )
        } catch (e: Exception) {
            PhoneActionResult.Failure(
                "Sir, સિસ્ટમ એલાર્મ એપ મળી નથી.",
                "Sir, no Clock alarm app available to handle this action."
            )
        }
    }

    fun isDeviceAdminActive(): Boolean {
        return devicePolicyManager.isAdminActive(adminComponent)
    }

    fun createDeviceAdminSetupIntent(): Intent {
        return Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN).apply {
            putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, adminComponent)
            putExtra(
                DevicePolicyManager.EXTRA_ADD_EXPLANATION,
                "Allows PARMAR AI to lock the screen strictly when you command it."
            )
        }
    }

    fun lockPhone(): PhoneActionResult {
        return if (isDeviceAdminActive()) {
            try {
                devicePolicyManager.lockNow()
                PhoneActionResult.Success(
                    "Sir, ફોન લોક કરી દીધો છે.",
                    "Sir, phone locked."
                )
            } catch (e: Exception) {
                PhoneActionResult.Failure(
                    "Sir, ફોન લોક કરવામાં અડચણ આવી: ${e.localizedMessage}",
                    "Sir, unable to lock screen: ${e.localizedMessage}"
                )
            }
        } else {
            PhoneActionResult.NeedsSetup(
                "Sir, ફોન લોક કરવા માટે પહેલાં Permissions સ્ક્રીનમાં 'Screen Lock Admin' ની મંજૂરી આપવી જરૂરી છે.",
                "Sir, please enable Device Admin permission first to allow screen locking.",
                createDeviceAdminSetupIntent()
            )
        }
    }

    fun canScheduleExactAlarms(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            alarmManager.canScheduleExactAlarms()
        } else {
            true
        }
    }

    fun createExactAlarmPermissionIntent(): Intent? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                data = Uri.fromParts("package", context.packageName, null)
            }
        } else null
    }

    fun scheduleLocalReminderAlarm(reminder: ReminderEntity) {
        if (reminder.isCompleted || reminder.triggerAtMillis <= System.currentTimeMillis()) return
        try {
            val intent = Intent(context, ReminderReceiver::class.java).apply {
                putExtra(ReminderReceiver.EXTRA_REMINDER_ID, reminder.id)
                putExtra(ReminderReceiver.EXTRA_REMINDER_TITLE, reminder.title)
            }
            val pending = PendingIntent.getBroadcast(
                context,
                reminder.id.toInt(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
                alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    reminder.triggerAtMillis,
                    pending
                )
            } else {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    reminder.triggerAtMillis,
                    pending
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to schedule reminder alarm", e)
        }
    }

    fun cancelLocalReminderAlarm(reminderId: Long) {
        try {
            val intent = Intent(context, ReminderReceiver::class.java)
            val pending = PendingIntent.getBroadcast(
                context,
                reminderId.toInt(),
                intent,
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
            )
            pending?.let { alarmManager.cancel(it) }
        } catch (_: Exception) {
        }
    }

    private fun launchSettingsIntent(intent: Intent, msgGu: String, msgEn: String): PhoneActionResult {
        return try {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
            PhoneActionResult.Success(msgGu, msgEn)
        } catch (e: Exception) {
            PhoneActionResult.Failure(
                "Sir, સેટિંગ્સ ખોલી શકાયા નથી.",
                "Sir, could not open settings."
            )
        }
    }

    private fun normalizeAppQuery(raw: String): String {
        val cleaned = raw.trim().lowercase()
        val aliases = mapOf(
            "વોટ્સએપ" to "whatsapp",
            "વ્હોટ્સએપ" to "whatsapp",
            "whatsapp" to "whatsapp",
            "યુટ્યુબ" to "youtube",
            "youtube" to "youtube",
            "કેમેરા" to "camera",
            "camera" to "camera",
            "સેટિંગ્સ" to "settings",
            "સેટિંગ" to "settings",
            "settings" to "settings",
            "ક્રોમ" to "chrome",
            "chrome" to "chrome",
            "કેલ્ક્યુલેટર" to "calculator",
            "calculator" to "calculator",
            "ઘડિયાળ" to "clock",
            "clock" to "clock",
            "ફોન" to "phone",
            "ડાયલર" to "phone",
            "મેસેજ" to "messages",
            "જીમેઇલ" to "gmail",
            "gmail" to "gmail",
            "મેપ્સ" to "maps",
            "maps" to "maps"
        )
        for ((key, value) in aliases) {
            if (cleaned.contains(key)) return value
        }
        return cleaned
            .removePrefix("open ")
            .removePrefix("launch ")
            .removeSuffix(" ખોલો")
            .removeSuffix(" ખોલ")
            .removeSuffix(" ચાલુ કર")
            .trim()
    }
}
