package com.example.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.QrCode2
import androidx.compose.material.icons.outlined.Radar
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.bridge.DeviceRole
import com.example.ui.components.AnimatedOnboardingGuide
import com.example.ui.components.AtmosphericCanvasBox
import com.example.ui.components.FloatingGlassNavBar
import com.example.ui.components.FloatingNavItem
import com.example.ui.components.LaunchIntroOverlay
import com.example.ui.i18n.LocalAppStrings
import com.example.ui.i18n.stringsForLanguage
import com.example.ui.screens.ContactsAndSettingsScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.PairingScreen
import com.example.ui.theme.ITellTheme
import com.example.ui.viewmodel.BridgeViewModel
import kotlinx.coroutines.launch

object ITellRoutes {
    const val DASHBOARD = "dashboard"
    const val PAIRING = "pairing"
    const val MORE = "contacts_settings"
}

@Composable
fun ITellApp(
    viewModel: BridgeViewModel,
    incomingSharedText: String? = null,
    onSharedTextConsumed: () -> Unit = {}
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val prefs by viewModel.preferences.collectAsStateWithLifecycle()
    val preferencesLoaded by viewModel.preferencesLoaded.collectAsStateWithLifecycle()
    val connectionState by viewModel.connectionState.collectAsStateWithLifecycle()
    val discoveredPeers by viewModel.discoveredPeers.collectAsStateWithLifecycle()
    val hostPayload by viewModel.hostPairingPayload.collectAsStateWithLifecycle()
    val localTelemetry by viewModel.localTelemetry.collectAsStateWithLifecycle()
    val peerTelemetry by viewModel.peerTelemetry.collectAsStateWithLifecycle()
    val latencyMs by viewModel.latencyMs.collectAsStateWithLifecycle()
    val voiceTunnelState by viewModel.voiceTunnelState.collectAsStateWithLifecycle()
    val incomingCallBanner by viewModel.incomingCallBanner.collectAsStateWithLifecycle()
    val pendingHandoffs by viewModel.pendingHandoffs.collectAsStateWithLifecycle()
    val pairedDevices by viewModel.pairedDevices.collectAsStateWithLifecycle()
    val callEvents by viewModel.callEvents.collectAsStateWithLifecycle()
    val latestOtpMessage by viewModel.latestOtpMessage.collectAsStateWithLifecycle()
    val batteryHistory by viewModel.batteryHistory.collectAsStateWithLifecycle()
    val selectedBatteryWindow by viewModel.selectedBatteryWindow.collectAsStateWithLifecycle()
    val contacts by viewModel.contacts.collectAsStateWithLifecycle()
    val isCallingAccountEnabled by viewModel.isCallingAccountEnabled.collectAsStateWithLifecycle()

    val strings = remember(prefs.languageCode) { stringsForLanguage(prefs.languageCode) }
    val layoutDirection = if (strings.isPersian) LayoutDirection.Rtl else LayoutDirection.Ltr
    val activeRole = if (prefs.deviceRole == DeviceRole.COMPANION.name) DeviceRole.COMPANION else DeviceRole.HOST
    var showLaunchIntro by remember { mutableStateOf(true) }
    var replayOnboarding by remember { mutableStateOf(false) }

    var hasPhonePerm by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    var hasCameraPerm by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    var hasBluetoothPerm by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) ==
                    PackageManager.PERMISSION_GRANTED
            } else true
        )
    }

    // Refresh Telecom Calling Account state when returning from Android system settings
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.refreshCallingAccountStatus()
                viewModel.refreshLocalTelemetry()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val phonePermLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasPhonePerm = granted
        if (granted) viewModel.refreshLocalTelemetry()
    }

    val cameraPermLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPerm = granted
    }

    val audioPermLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            viewModel.toggleVoiceTunnel()
        } else {
            coroutineScope.launch { snackbarHostState.showSnackbar(strings.audioPermRequired) }
        }
    }

    val contactsPermLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) viewModel.syncFullAddressBook()
    }

    val notificationPermLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { _ ->
        viewModel.startHostBridge()
    }

    val bluetoothPermLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { grants ->
        val granted = grants.values.all { it }
        hasBluetoothPerm = granted
        if (granted) viewModel.refreshBluetoothDevices()
    }

    val pickContactLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickContact()
    ) { uri ->
        if (uri != null) viewModel.importPickedContact(uri)
    }

    LaunchedEffect(Unit) {
        viewModel.snackbarEvents.collect { msg ->
            snackbarHostState.showSnackbar(msg)
        }
    }

    LaunchedEffect(incomingSharedText) {
        if (!incomingSharedText.isNullOrBlank()) {
            viewModel.handleIncomingSharedText(incomingSharedText)
            onSharedTextConsumed()
        }
    }

    val startHostWithNotificationCheck: () -> Unit = {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            viewModel.startHostBridge()
        }
    }

    val toggleVoiceWithAudioCheck: () -> Unit = {
        if (!voiceTunnelState.isActive &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            audioPermLauncher.launch(Manifest.permission.RECORD_AUDIO)
        } else {
            viewModel.toggleVoiceTunnel()
        }
    }

    val requestBluetoothAccess: () -> Unit = {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            bluetoothPermLauncher.launch(
                arrayOf(
                    Manifest.permission.BLUETOOTH_CONNECT,
                    Manifest.permission.BLUETOOTH_SCAN,
                    Manifest.permission.BLUETOOTH_ADVERTISE
                )
            )
        } else {
            hasBluetoothPerm = true
            viewModel.refreshBluetoothDevices()
        }
    }

    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route ?: ITellRoutes.DASHBOARD

    val destinations = remember(strings) {
        listOf(
            FloatingNavItem(ITellRoutes.DASHBOARD, strings.navDashboard, Icons.Filled.Radar, Icons.Outlined.Radar),
            FloatingNavItem(ITellRoutes.PAIRING, strings.navPairing, Icons.Filled.QrCode2, Icons.Outlined.QrCode2),
            FloatingNavItem(ITellRoutes.MORE, strings.navMore, Icons.Filled.Settings, Icons.Outlined.Settings)
        )
    }

    val navigateTo: (String) -> Unit = { route ->
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    ITellTheme(darkTheme = prefs.darkTheme, isPersian = strings.isPersian) {
        CompositionLocalProvider(
            LocalAppStrings provides strings,
            LocalLayoutDirection provides layoutDirection
        ) {
            AtmosphericCanvasBox {
                BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                    val useNavigationRail = maxWidth >= 600.dp

                    Scaffold(
                        modifier = Modifier.fillMaxSize(),
                        containerColor = Color.Transparent,
                        contentWindowInsets = WindowInsets.safeDrawing,
                        snackbarHost = { SnackbarHost(snackbarHostState) },
                        bottomBar = {
                            if (!useNavigationRail && prefs.deviceRole.isNotBlank()) {
                                FloatingGlassNavBar(
                                    items = destinations,
                                    currentRoute = currentRoute,
                                    onSelectRoute = navigateTo
                                )
                            }
                        }
                    ) { innerPadding ->
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(innerPadding)
                        ) {
                            if (useNavigationRail && prefs.deviceRole.isNotBlank()) {
                                NavigationRail(
                                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                                    modifier = Modifier.fillMaxHeight()
                                ) {
                                    destinations.forEach { dest ->
                                        val selected = currentRoute == dest.route
                                        NavigationRailItem(
                                            selected = selected,
                                            onClick = { navigateTo(dest.route) },
                                            icon = {
                                                Icon(
                                                    imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                                                    contentDescription = dest.label
                                                )
                                            },
                                            label = { Text(dest.label, maxLines = 1) },
                                            modifier = Modifier.testTag("nav_rail_${dest.route}")
                                        )
                                    }
                                }
                            }

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight(),
                                contentAlignment = Alignment.TopCenter
                            ) {
                                NavHost(
                                    navController = navController,
                                    startDestination = ITellRoutes.DASHBOARD,
                                    enterTransition = { fadeIn(animationSpec = tween(220)) },
                                    exitTransition = { fadeOut(animationSpec = tween(180)) },
                                    modifier = Modifier
                                        .widthIn(max = 760.dp)
                                        .fillMaxSize()
                                ) {
                                    composable(ITellRoutes.DASHBOARD) {
                                        DashboardScreen(
                                            deviceRoleSetting = prefs.deviceRole,
                                            activeRole = activeRole,
                                            connectionState = connectionState,
                                            localTelemetry = localTelemetry,
                                            peerTelemetry = peerTelemetry,
                                            latencyMs = latencyMs,
                                            preferredSimSlot = prefs.preferredSimSlot,
                                            hasPhonePermission = hasPhonePerm,
                                            isCallingAccountEnabled = isCallingAccountEnabled,
                                            incomingCallBanner = incomingCallBanner,
                                            voiceTunnelState = voiceTunnelState,
                                            pendingHandoffs = pendingHandoffs,
                                            recentCallEvents = callEvents,
                                            latestOtpMessage = latestOtpMessage,
                                            batteryHistory = batteryHistory,
                                            selectedBatteryWindow = selectedBatteryWindow,
                                            onSelectBatteryWindow = { viewModel.selectBatteryWindow(it) },
                                            onSelectRole = { role -> viewModel.selectRole(role) },
                                            onStartHost = startHostWithNotificationCheck,
                                            onStopBridge = { viewModel.stopBridge() },
                                            onNavigatePairing = { navigateTo(ITellRoutes.PAIRING) },
                                            onSelectPreferredSim = { viewModel.setPreferredSimSlot(it) },
                                            onRequestPhonePermission = {
                                                phonePermLauncher.launch(Manifest.permission.READ_PHONE_STATE)
                                            },
                                            onOpenCallingAccountSettings = { viewModel.openCallingAccountSettings() },
                                            onOpenDefaultPhoneApp = { viewModel.openDefaultPhoneApp() },
                                            onOpenDefaultMessagesApp = { viewModel.openDefaultMessagesApp() },
                                            onRelayClipboardOtp = { viewModel.relayClipboardOtp() },
                                            onToggleVoiceTunnel = toggleVoiceWithAudioCheck,
                                            onToggleVoiceMute = { viewModel.toggleVoiceMute() },
                                            onToggleVoiceSpeaker = { viewModel.toggleVoiceSpeaker() },
                                            onDismissIncomingCall = { viewModel.dismissIncomingCallBanner() },
                                            onExecuteHandoff = { viewModel.executePendingHandoff(it) },
                                            onDismissHandoff = { viewModel.dismissPendingHandoff(it) },
                                            onCopyFeedback = { text ->
                                                coroutineScope.launch { snackbarHostState.showSnackbar(text) }
                                            },
                                            onToggleLanguage = {
                                                viewModel.setLanguage(if (prefs.languageCode == "fa") "en" else "fa")
                                            },
                                            onReplayOnboarding = { replayOnboarding = true }
                                        )
                                    }

                                    composable(ITellRoutes.PAIRING) {
                                        PairingScreen(
                                            role = activeRole,
                                            connectionState = connectionState,
                                            hostPayload = hostPayload,
                                            discoveredPeers = discoveredPeers,
                                            savedDevices = pairedDevices,
                                            hasCameraPermission = hasCameraPerm,
                                            hasBluetoothPermission = hasBluetoothPerm,
                                            onStartHost = startHostWithNotificationCheck,
                                            onStopBridge = { viewModel.stopBridge() },
                                            onRequestCameraPermission = {
                                                cameraPermLauncher.launch(Manifest.permission.CAMERA)
                                            },
                                            onRequestBluetoothPermission = requestBluetoothAccess,
                                            onStartDiscovery = { viewModel.startNearbyDiscovery() },
                                            onQrScanned = { raw ->
                                                val ok = viewModel.connectFromQr(raw)
                                                if (ok) {
                                                    navigateTo(ITellRoutes.DASHBOARD)
                                                } else {
                                                    coroutineScope.launch {
                                                        snackbarHostState.showSnackbar(strings.statusError)
                                                    }
                                                }
                                            },
                                            onConnectDiscoveredPeer = { peer, code ->
                                                viewModel.connectToDiscoveredPeer(peer, code)
                                                navigateTo(ITellRoutes.DASHBOARD)
                                            },
                                            onConnectManual = { ip, port, code ->
                                                viewModel.connectManually(ip, port, code)
                                                navigateTo(ITellRoutes.DASHBOARD)
                                            },
                                            onReconnectSaved = { dev ->
                                                viewModel.reconnectSavedDevice(dev)
                                                navigateTo(ITellRoutes.DASHBOARD)
                                            },
                                            onForgetSaved = { id -> viewModel.forgetPairedDevice(id) },
                                            onCopyFeedback = { text ->
                                                coroutineScope.launch { snackbarHostState.showSnackbar(text) }
                                            }
                                        )
                                    }

                                    composable(ITellRoutes.MORE) {
                                        ContactsAndSettingsScreen(
                                            contacts = contacts,
                                            preferences = prefs,
                                            onPickContact = { pickContactLauncher.launch(null) },
                                            onSyncAddressBook = {
                                                if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) ==
                                                    PackageManager.PERMISSION_GRANTED
                                                ) {
                                                    viewModel.syncFullAddressBook()
                                                } else {
                                                    contactsPermLauncher.launch(Manifest.permission.READ_CONTACTS)
                                                }
                                            },
                                            onPushContactsToPeer = { viewModel.pushAllSavedContactsToPeer() },
                                            onDialContact = { num -> viewModel.launchSystemDialer(num) },
                                            onSmsContact = { num -> viewModel.launchSystemSmsApp(num, "") },
                                            onSetLanguage = { viewModel.setLanguage(it) },
                                            onSetDarkTheme = { viewModel.setDarkTheme(it) },
                                            onSetDeviceAlias = { viewModel.setDeviceAlias(it) },
                                            onSetDefaultSim = { viewModel.setPreferredSimSlot(it) },
                                            onSetAutoReconnect = { viewModel.setAutoReconnect(it) },
                                            onSetVibrateIncoming = { viewModel.setVibrateOnIncoming(it) },
                                            onReplayOnboarding = { replayOnboarding = true }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    val showOnboarding = !showLaunchIntro &&
                        ((preferencesLoaded && !prefs.onboardingCompleted) || replayOnboarding)
                    AnimatedVisibility(
                        visible = showOnboarding,
                        enter = fadeIn(tween(320)),
                        exit = fadeOut(tween(260))
                    ) {
                        AnimatedOnboardingGuide(
                            isDarkTheme = prefs.darkTheme,
                            onToggleTheme = { viewModel.setDarkTheme(!prefs.darkTheme) },
                            onToggleLanguage = {
                                viewModel.setLanguage(if (prefs.languageCode == "fa") "en" else "fa")
                            },
                            onComplete = {
                                replayOnboarding = false
                                viewModel.setOnboardingCompleted(true)
                            }
                        )
                    }

                    LaunchIntroOverlay(
                        visible = showLaunchIntro,
                        onFinished = { showLaunchIntro = false }
                    )
                }
            }
        }
    }
}
