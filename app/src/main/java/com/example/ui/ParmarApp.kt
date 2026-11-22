package com.example.ui

import android.Manifest
import android.content.Intent
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.GraphicEq
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.NoteAlt
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.rounded.Alarm
import androidx.compose.material.icons.rounded.ChatBubble
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.NoteAlt
import androidx.compose.material.icons.rounded.Psychology
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.ui.components.ParmarStatusHeader
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.MemoryScreen
import com.example.ui.screens.NotesScreen
import com.example.ui.screens.PermissionsPrivacyScreen
import com.example.ui.screens.RemindersScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.SplashScreen
import com.example.ui.screens.TextChatScreen
import com.example.ui.screens.VoiceAssistantScreen
import com.example.ui.theme.CoolSlate
import com.example.ui.theme.DeepSpace
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.ElevatedCard
import com.example.ui.theme.ObsidianBlack

object Routes {
    const val SPLASH = "splash"
    const val HOME = "home"
    const val VOICE = "voice"
    const val CHAT = "chat"
    const val NOTES = "notes"
    const val REMINDERS = "reminders"
    const val MEMORY = "memory"
    const val SETTINGS = "settings"
    const val PERMISSIONS = "permissions"
}

private data class TopNavItem(
    val route: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val tag: String
)

@Composable
fun ParmarApp(viewModel: ParmarViewModel = viewModel()) {
    val context = LocalContext.current
    val navController = rememberNavController()
    val backStack by navController.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route ?: Routes.SPLASH

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val notes by viewModel.notes.collectAsStateWithLifecycle()
    val reminders by viewModel.reminders.collectAsStateWithLifecycle()
    val memories by viewModel.memories.collectAsStateWithLifecycle()
    val chatMessages by viewModel.chatMessages.collectAsStateWithLifecycle()
    val availableVoices by viewModel.availableVoices.collectAsStateWithLifecycle()

    val strings = AppStrings.of(settings.language)
    val snackbarHostState = remember { SnackbarHostState() }
    var showMoreMenu by remember { mutableStateOf(false) }

    val micLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        viewModel.refreshSystemSnapshot()
        if (granted && settings.backgroundListeningEnabled) {
            viewModel.setBackgroundListening(true)
        }
    }

    val notifLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) {
        viewModel.refreshSystemSnapshot()
    }

    val exportBackupLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        uri?.let { viewModel.exportBackup(it) }
    }

    val importBackupLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let { viewModel.importBackup(it) }
    }

    val activityResultLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        viewModel.refreshSystemSnapshot()
    }

    LaunchedEffect(uiState.infoToast) {
        uiState.infoToast?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.dismissToast()
        }
    }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.dismissError()
        }
    }

    val topNavItems = listOf(
        TopNavItem(Routes.HOME, strings.navHome, Icons.Rounded.Home, Icons.Outlined.Home, "nav_home"),
        TopNavItem(Routes.VOICE, strings.navVoice, Icons.Rounded.GraphicEq, Icons.Outlined.GraphicEq, "nav_voice"),
        TopNavItem(Routes.CHAT, strings.navChat, Icons.Rounded.ChatBubble, Icons.Outlined.ChatBubbleOutline, "nav_chat"),
        TopNavItem(Routes.NOTES, strings.navNotes, Icons.Rounded.NoteAlt, Icons.Outlined.NoteAlt, "nav_notes")
    )

    fun navigateSingleTop(route: String) {
        navController.navigate(route) {
            popUpTo(Routes.HOME) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    val showChrome = currentRoute != Routes.SPLASH

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useRail = maxWidth >= 640.dp

        Scaffold(
            containerColor = ObsidianBlack,
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            snackbarHost = { SnackbarHost(snackbarHostState) },
            bottomBar = {
                if (showChrome && !useRail) {
                    NavigationBar(
                        containerColor = DeepSpace,
                        modifier = Modifier.navigationBarsPadding()
                    ) {
                        topNavItems.forEach { item ->
                            val selected = currentRoute == item.route
                            NavigationBarItem(
                                selected = selected,
                                onClick = { navigateSingleTop(item.route) },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                                        contentDescription = item.label
                                    )
                                },
                                label = { Text(item.label, maxLines = 1) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = ObsidianBlack,
                                    selectedTextColor = ElectricBlue,
                                    indicatorColor = ElectricBlue,
                                    unselectedIconColor = CoolSlate,
                                    unselectedTextColor = CoolSlate
                                ),
                                modifier = Modifier.testTag(item.tag)
                            )
                        }

                        val moreSelected = currentRoute in listOf(
                            Routes.REMINDERS, Routes.MEMORY, Routes.SETTINGS, Routes.PERMISSIONS
                        )
                        NavigationBarItem(
                            selected = moreSelected,
                            onClick = { showMoreMenu = true },
                            icon = {
                                Icon(
                                    imageVector = if (moreSelected) Icons.Rounded.Settings else Icons.Outlined.Settings,
                                    contentDescription = strings.navMore
                                )
                                DropdownMenu(
                                    expanded = showMoreMenu,
                                    onDismissRequest = { showMoreMenu = false }
                                ) {
                                    DropdownMenuItem(
                                        text = { Text(strings.remindersTitle) },
                                        leadingIcon = { Icon(Icons.Rounded.Alarm, contentDescription = null) },
                                        onClick = {
                                            showMoreMenu = false
                                            navigateSingleTop(Routes.REMINDERS)
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text(strings.memoryTitle) },
                                        leadingIcon = { Icon(Icons.Rounded.Psychology, contentDescription = null) },
                                        onClick = {
                                            showMoreMenu = false
                                            navigateSingleTop(Routes.MEMORY)
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text(strings.settingsTitle) },
                                        leadingIcon = { Icon(Icons.Rounded.Settings, contentDescription = null) },
                                        onClick = {
                                            showMoreMenu = false
                                            navigateSingleTop(Routes.SETTINGS)
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text(strings.permissionsTitle) },
                                        leadingIcon = { Icon(Icons.Rounded.Security, contentDescription = null) },
                                        onClick = {
                                            showMoreMenu = false
                                            navigateSingleTop(Routes.PERMISSIONS)
                                        }
                                    )
                                }
                            },
                            label = { Text(strings.navMore, maxLines = 1) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = ObsidianBlack,
                                selectedTextColor = ElectricBlue,
                                indicatorColor = ElectricBlue,
                                unselectedIconColor = CoolSlate,
                                unselectedTextColor = CoolSlate
                            ),
                            modifier = Modifier.testTag("nav_more")
                        )
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (showChrome && useRail) {
                    NavigationRail(
                        containerColor = DeepSpace,
                        modifier = Modifier.windowInsetsPadding(WindowInsets.statusBars)
                    ) {
                        val allRail = topNavItems + listOf(
                            TopNavItem(Routes.REMINDERS, strings.remindersTitle, Icons.Rounded.Alarm, Icons.Rounded.Alarm, "rail_reminders"),
                            TopNavItem(Routes.MEMORY, strings.memoryTitle, Icons.Rounded.Psychology, Icons.Rounded.Psychology, "rail_memory"),
                            TopNavItem(Routes.SETTINGS, strings.settingsTitle, Icons.Rounded.Settings, Icons.Outlined.Settings, "rail_settings"),
                            TopNavItem(Routes.PERMISSIONS, strings.permissionsTitle, Icons.Rounded.Security, Icons.Rounded.Security, "rail_permissions")
                        )
                        allRail.forEach { item ->
                            val selected = currentRoute == item.route
                            NavigationRailItem(
                                selected = selected,
                                onClick = { navigateSingleTop(item.route) },
                                icon = { Icon(if (selected) item.selectedIcon else item.unselectedIcon, contentDescription = item.label) },
                                label = { Text(item.label, maxLines = 1) },
                                colors = NavigationRailItemDefaults.colors(
                                    selectedIconColor = ObsidianBlack,
                                    selectedTextColor = ElectricBlue,
                                    indicatorColor = ElectricBlue,
                                    unselectedIconColor = CoolSlate,
                                    unselectedTextColor = CoolSlate
                                )
                            )
                        }
                    }
                }

                Column(modifier = Modifier.weight(1f).fillMaxSize()) {
                    if (showChrome) {
                        ParmarStatusHeader(
                            strings = strings,
                            language = settings.language,
                            isOffline = uiState.isEffectiveOffline,
                            isMicListening = uiState.phase == AssistantPhase.LISTENING ||
                                uiState.isWakeStandbyActive ||
                                uiState.isServiceRunning,
                            onToggleLanguage = { viewModel.toggleLanguage() },
                            onStopListening = { viewModel.stopListeningAll() },
                            modifier = Modifier.windowInsetsPadding(WindowInsets.statusBars)
                        )
                    }

                    NavHost(
                        navController = navController,
                        startDestination = Routes.SPLASH,
                        modifier = Modifier.weight(1f)
                    ) {
                        composable(Routes.SPLASH) {
                            SplashScreen(
                                onFinished = {
                                    navController.navigate(Routes.HOME) {
                                        popUpTo(Routes.SPLASH) { inclusive = true }
                                    }
                                    if (!uiState.permissions.micGranted) {
                                        micLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                    }
                                }
                            )
                        }

                        composable(Routes.HOME) {
                            HomeScreen(
                                uiState = uiState,
                                settings = settings,
                                strings = strings,
                                pendingRemindersCount = reminders.count { !it.isCompleted },
                                notesCount = notes.size,
                                memoriesCount = memories.size,
                                onOrbClick = {
                                    if (!uiState.permissions.micGranted) {
                                        micLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                    } else {
                                        viewModel.onOrbTapped()
                                    }
                                },
                                onSimulateDiamond = { viewModel.onWakeWordTriggered() },
                                onToggleStandbyMic = {
                                    if (!uiState.permissions.micGranted) {
                                        micLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                    } else if (uiState.isWakeStandbyActive || uiState.isServiceRunning) {
                                        viewModel.stopListeningAll()
                                    } else {
                                        viewModel.startStandbyWakeListening()
                                    }
                                },
                                onQuickAgenda = { viewModel.quickCheckAgenda() },
                                onQuickTorch = { viewModel.quickToggleFlashlight() },
                                onQuickTime = { viewModel.quickReadTime() },
                                onQuickBluetooth = { viewModel.quickOpenBluetooth() },
                                onQuickWifi = { viewModel.quickOpenWifi() },
                                onQuickLock = {
                                    viewModel.quickLockPhone()?.let { setupIntent ->
                                        activityResultLauncher.launch(setupIntent)
                                    }
                                },
                                onNavigateToReminders = { navigateSingleTop(Routes.REMINDERS) },
                                onNavigateToMemory = { navigateSingleTop(Routes.MEMORY) },
                                onNavigateToSettings = { navigateSingleTop(Routes.SETTINGS) },
                                onNavigateToPermissions = { navigateSingleTop(Routes.PERMISSIONS) },
                                onApproveMemory = { viewModel.approvePendingMemory() },
                                onDismissMemory = { viewModel.dismissPendingMemory() }
                            )
                        }

                        composable(Routes.VOICE) {
                            VoiceAssistantScreen(
                                uiState = uiState,
                                settings = settings,
                                strings = strings,
                                onOrbClick = {
                                    if (!uiState.permissions.micGranted) {
                                        micLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                    } else {
                                        viewModel.onOrbTapped()
                                    }
                                },
                                onWakeDiamond = { viewModel.onWakeWordTriggered() },
                                onStopWordDiamond = { viewModel.onStopWordTriggered() },
                                onStartListening = {
                                    if (!uiState.permissions.micGranted) {
                                        micLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                    } else {
                                        viewModel.startActiveVoiceCapture()
                                    }
                                },
                                onApproveMemory = { viewModel.approvePendingMemory() },
                                onDismissMemory = { viewModel.dismissPendingMemory() }
                            )
                        }

                        composable(Routes.CHAT) {
                            TextChatScreen(
                                messages = chatMessages,
                                uiState = uiState,
                                settings = settings,
                                strings = strings,
                                onSendMessage = { viewModel.sendStreamingChatQuestion(it) },
                                onVoiceInput = {
                                    navigateSingleTop(Routes.VOICE)
                                    if (uiState.permissions.micGranted) {
                                        viewModel.startActiveVoiceCapture()
                                    } else {
                                        micLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                    }
                                },
                                onClearChat = { viewModel.clearChatHistory() },
                                onApproveMemory = { viewModel.approvePendingMemory() },
                                onDismissMemory = { viewModel.dismissPendingMemory() }
                            )
                        }

                        composable(Routes.NOTES) {
                            NotesScreen(
                                notes = notes,
                                language = settings.language,
                                strings = strings,
                                onSaveNote = { id, title, content, cat, pinned ->
                                    viewModel.saveNote(id, title, content, cat, pinned)
                                },
                                onTogglePin = { viewModel.toggleNotePin(it) },
                                onDeleteNote = { viewModel.deleteNote(it) }
                            )
                        }

                        composable(Routes.REMINDERS) {
                            RemindersScreen(
                                reminders = reminders,
                                language = settings.language,
                                strings = strings,
                                onCreateReminder = { title, millis, repeat ->
                                    viewModel.saveReminder(title, millis, repeat)
                                },
                                onToggleComplete = { viewModel.toggleReminderComplete(it) },
                                onDeleteReminder = { viewModel.deleteReminder(it) }
                            )
                        }

                        composable(Routes.MEMORY) {
                            MemoryScreen(
                                memories = memories,
                                privateUnlocked = uiState.privateMemoriesUnlocked,
                                language = settings.language,
                                strings = strings,
                                onAddMemory = { k, v, c, p -> viewModel.addMemoryDirect(k, v, c, p) },
                                onDeleteMemory = { viewModel.deleteMemory(it) },
                                onUnlockPrivate = { viewModel.unlockPrivateMemories() },
                                onLockPrivate = { viewModel.lockPrivateMemories() },
                                onExportBackup = {
                                    exportBackupLauncher.launch("parmar_ai_backup.json")
                                },
                                onImportBackup = {
                                    importBackupLauncher.launch(arrayOf("application/json", "*/*"))
                                }
                            )
                        }

                        composable(Routes.SETTINGS) {
                            SettingsScreen(
                                settings = settings,
                                uiState = uiState,
                                availableVoices = availableVoices,
                                strings = strings,
                                onUpdateSettings = { viewModel.updateSettings(it) },
                                onToggleBackgroundListening = { enabled ->
                                    if (enabled && !uiState.permissions.micGranted) {
                                        micLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                    } else {
                                        viewModel.setBackgroundListening(enabled)
                                    }
                                },
                                onTestVoice = { viewModel.testVoiceOutput() },
                                onCalibrateVoiceProfile = {
                                    if (!uiState.permissions.micGranted) {
                                        micLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                    } else {
                                        viewModel.calibrateVoiceProfile()
                                    }
                                },
                                onTestAiConnection = { viewModel.testAiConnection() },
                                onClearAllMemories = { viewModel.clearAllMemoriesGuarded() },
                                onAdjustVolume = { viewModel.adjustVolumePercent(it) }
                            )
                        }

                        composable(Routes.PERMISSIONS) {
                            PermissionsPrivacyScreen(
                                uiState = uiState,
                                settings = settings,
                                strings = strings,
                                onRequestMicPermission = {
                                    micLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                },
                                onRequestNotificationPermission = {
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                        notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                    }
                                },
                                onRequestExactAlarm = {
                                    viewModel.createExactAlarmIntent()?.let {
                                        activityResultLauncher.launch(it)
                                    }
                                },
                                onRequestDeviceAdmin = {
                                    activityResultLauncher.launch(viewModel.createDeviceAdminIntent())
                                },
                                onOpenBatterySettings = { viewModel.quickOpenBatterySettings() },
                                onOpenAppSettings = { viewModel.quickOpenAppSettings() },
                                onStopListeningKillSwitch = { viewModel.stopListeningAll() }
                            )
                        }
                    }
                }
            }
        }
    }

    // Optional Device Admin Setup Dialog triggered by Lock Phone voice command
    uiState.pendingAdminSetupIntent?.let { adminIntent ->
        AlertDialog(
            onDismissRequest = { viewModel.clearPendingAdminIntent() },
            title = {
                Text(if (settings.language == "gu") "સ્ક્રીન લોક પરમિશન" else "Enable Screen Lock Admin")
            },
            text = {
                Text(
                    if (settings.language == "gu") {
                        "Android સુરક્ષા નીતિ મુજબ, ફોન લોક કરવા માટે Device Admin ની મંજૂરી આપવી જરૂરી છે. શું તમે સેટઅપ ખોલવા માંગો છો?"
                    } else {
                        "Android requires explicit Device Admin activation before PARMAR can lock the screen. Open setup now?"
                    }
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.clearPendingAdminIntent()
                        activityResultLauncher.launch(adminIntent)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue, contentColor = ObsidianBlack)
                ) {
                    Text(if (settings.language == "gu") "મંજૂરી આપો" else "Open Setup")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.clearPendingAdminIntent() }) {
                    Text(if (settings.language == "gu") "રદ કરો" else "Cancel")
                }
            }
        )
    }
}
