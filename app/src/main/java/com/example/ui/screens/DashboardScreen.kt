package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.Message
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.BatteryStd
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentPasteGo
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.HistoryToggleOff
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.SettingsPhone
import androidx.compose.material.icons.filled.SignalCellularAlt
import androidx.compose.material.icons.filled.SimCard
import androidx.compose.material.icons.filled.SimCardAlert
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.WifiTethering
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.bridge.BatteryTimeWindow
import com.example.bridge.ConnectionState
import com.example.bridge.DeviceRole
import com.example.bridge.DeviceTelemetry
import com.example.bridge.HandoffType
import com.example.bridge.IncomingCallBanner
import com.example.bridge.PendingHandoff
import com.example.bridge.VoiceTunnelState
import com.example.data.local.BatteryHistoryEntity
import com.example.data.local.BridgeCallEventEntity
import com.example.data.local.BridgeMessageEntity
import com.example.ui.components.BatteryHistoryChartCard
import com.example.ui.components.GlassBridgeCard
import com.example.ui.components.HeroArtworkBanner
import com.example.ui.components.IncomingCallOverlay
import com.example.ui.components.OtpCodeChip
import com.example.ui.components.QuickHelpDialog
import com.example.ui.components.QuickStartChecklistCard
import com.example.ui.components.SignalCoreHeroCard
import com.example.ui.components.SimCardTile
import com.example.ui.components.VoiceTunnelActiveBar
import com.example.ui.i18n.LocalAppStrings
import com.example.ui.i18n.localizeDigits
import com.example.ui.theme.MonoTechnicalStyle
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun DashboardScreen(
    deviceRoleSetting: String,
    activeRole: DeviceRole,
    connectionState: ConnectionState,
    localTelemetry: DeviceTelemetry,
    peerTelemetry: DeviceTelemetry?,
    latencyMs: Long,
    preferredSimSlot: Int,
    hasPhonePermission: Boolean,
    isCallingAccountEnabled: Boolean,
    incomingCallBanner: IncomingCallBanner,
    voiceTunnelState: VoiceTunnelState,
    pendingHandoffs: List<PendingHandoff>,
    recentCallEvents: List<BridgeCallEventEntity>,
    latestOtpMessage: BridgeMessageEntity?,
    batteryHistory: List<BatteryHistoryEntity>,
    selectedBatteryWindow: BatteryTimeWindow,
    onSelectBatteryWindow: (BatteryTimeWindow) -> Unit,
    onSelectRole: (DeviceRole) -> Unit,
    onStartHost: () -> Unit,
    onStopBridge: () -> Unit,
    onNavigatePairing: () -> Unit,
    onSelectPreferredSim: (Int) -> Unit,
    onRequestPhonePermission: () -> Unit,
    onOpenCallingAccountSettings: () -> Unit,
    onOpenDefaultPhoneApp: () -> Unit,
    onOpenDefaultMessagesApp: () -> Unit,
    onRelayClipboardOtp: () -> Unit,
    onToggleVoiceTunnel: () -> Unit,
    onToggleVoiceMute: () -> Unit,
    onToggleVoiceSpeaker: () -> Unit,
    onDismissIncomingCall: () -> Unit,
    onExecuteHandoff: (PendingHandoff) -> Unit,
    onDismissHandoff: (String) -> Unit,
    onCopyFeedback: (String) -> Unit,
    onToggleLanguage: () -> Unit,
    onReplayOnboarding: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val strings = LocalAppStrings.current
    val clipboard = LocalClipboardManager.current
    val primaryAccent = MaterialTheme.colorScheme.primary
    val emeraldAccent = MaterialTheme.colorScheme.tertiary
    val amberAccent = MaterialTheme.colorScheme.secondary
    var showQuickHelp by remember { mutableStateOf(false) }
    var guideDismissed by rememberSaveable { mutableStateOf(false) }

    if (showQuickHelp) {
        QuickHelpDialog(
            onDismiss = { showQuickHelp = false },
            onReplayAnimatedGuide = onReplayOnboarding
        )
    }

    if (deviceRoleSetting.isBlank()) {
        FirstRunRolePicker(
            onSelectRole = onSelectRole,
            onToggleLanguage = onToggleLanguage,
            modifier = modifier
        )
        return
    }

    val displayedTelemetry = if (activeRole == DeviceRole.COMPANION && peerTelemetry != null) {
        peerTelemetry
    } else {
        localTelemetry
    }
    val maxSignalLevel = displayedTelemetry.simCards.maxOfOrNull { it.signalLevel } ?: 0
    val isConnected = connectionState is ConnectionState.Connected

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("dashboard_screen"),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Brand Header with App Icon Emblem + Quick QR + Language Toggle
        item {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        // Framed iTell App Icon Emblem
                        Surface(
                            modifier = Modifier.size(48.dp),
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surfaceContainerLowest,
                            border = BorderStroke(
                                1.2.dp,
                                Brush.linearGradient(
                                    listOf(
                                        primaryAccent.copy(alpha = 0.8f),
                                        emeraldAccent.copy(alpha = 0.65f)
                                    )
                                )
                            ),
                            shadowElevation = 6.dp
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.itell_icon_fg_1795555028207),
                                contentDescription = strings.appTitle,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(RoundedCornerShape(16.dp))
                            )
                        }
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(7.dp)
                            ) {
                                Text(
                                    text = strings.appTitle,
                                    style = MaterialTheme.typography.headlineSmall,
                                    color = MaterialTheme.colorScheme.onBackground,
                                    fontWeight = FontWeight.Bold
                                )
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(if (isConnected) emeraldAccent else primaryAccent)
                                )
                            }
                            Text(
                                text = strings.appTagline,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .clickable { showQuickHelp = true }
                                .testTag("dashboard_help_button"),
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.85f),
                            border = BorderStroke(1.dp, amberAccent.copy(alpha = 0.35f))
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.HelpOutline,
                                    contentDescription = strings.helpTitle,
                                    tint = amberAccent,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        Surface(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .clickable(onClick = onNavigatePairing)
                                .testTag("dashboard_header_qr_button"),
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.85f),
                            border = BorderStroke(1.dp, emeraldAccent.copy(alpha = 0.35f))
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Filled.QrCode2,
                                    contentDescription = strings.showQr,
                                    tint = emeraldAccent,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        Surface(
                            modifier = Modifier
                                .height(44.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .clickable(onClick = onToggleLanguage)
                                .testTag("dashboard_language_toggle"),
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.85f),
                            border = BorderStroke(1.dp, primaryAccent.copy(alpha = 0.35f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Language,
                                    contentDescription = strings.languageLabel,
                                    tint = primaryAccent,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = if (strings.isPersian) "EN" else "فا",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }

                // Sleek Icon-Rich Glass Role Switcher
                GlassBridgeCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("role_segmented_row"),
                    accentColor = primaryAccent,
                    accentStrength = 0.25f,
                    cornerRadius = 18.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(5.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        RoleSwitchPill(
                            label = strings.roleHost,
                            icon = Icons.Filled.WifiTethering,
                            selected = activeRole == DeviceRole.HOST,
                            accent = primaryAccent,
                            onClick = { onSelectRole(DeviceRole.HOST) },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("role_host_segment")
                        )
                        RoleSwitchPill(
                            label = strings.roleCompanion,
                            icon = Icons.Filled.PhoneAndroid,
                            selected = activeRole == DeviceRole.COMPANION,
                            accent = emeraldAccent,
                            onClick = { onSelectRole(DeviceRole.COMPANION) },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("role_companion_segment")
                        )
                    }
                }
            }
        }

        // Interactive 3-Step Quick Setup Checklist (auto-hides when all steps are done)
        val isLinkedOrHosting = isConnected || connectionState is ConnectionState.Hosting
        val isSystemReady = if (activeRole == DeviceRole.HOST) hasPhonePermission else isCallingAccountEnabled
        if (!guideDismissed && (!isLinkedOrHosting || !isSystemReady)) {
            item {
                QuickStartChecklistCard(
                    role = activeRole,
                    isLinkedOrHosting = isLinkedOrHosting,
                    isSystemReady = isSystemReady,
                    onStep2Action = {
                        if (activeRole == DeviceRole.HOST) onStartHost() else onNavigatePairing()
                    },
                    onStep3Action = {
                        if (activeRole == DeviceRole.HOST && !hasPhonePermission) {
                            onRequestPhonePermission()
                        } else {
                            onOpenCallingAccountSettings()
                        }
                    },
                    onDismiss = { guideDismissed = true }
                )
            }
        }

        // Active Incoming Call Overlay
        item {
            IncomingCallOverlay(
                banner = incomingCallBanner,
                onOpenVoiceTunnel = onToggleVoiceTunnel,
                onDismiss = onDismissIncomingCall
            )
        }

        // Active Voice Tunnel Waveform Bar
        item {
            VoiceTunnelActiveBar(
                voiceState = voiceTunnelState,
                onToggleMute = onToggleVoiceMute,
                onToggleSpeaker = onToggleVoiceSpeaker,
                onHangUp = onToggleVoiceTunnel
            )
        }

        // Dual-Node Signal Core Hero
        item {
            SignalCoreHeroCard(
                role = activeRole,
                connectionState = connectionState,
                latencyMs = latencyMs,
                onPrimaryAction = {
                    when {
                        connectionState is ConnectionState.Connected ||
                            connectionState is ConnectionState.Hosting -> onStopBridge()
                        activeRole == DeviceRole.HOST -> onStartHost()
                        else -> onNavigatePairing()
                    }
                },
                onSecondaryAction = {
                    if (activeRole == DeviceRole.HOST) {
                        onNavigatePairing()
                    } else {
                        onSelectRole(DeviceRole.HOST)
                    }
                }
            )
        }

        // Live Telemetry Trio Strip (Signal Bars, Battery, Encryption/Ping)
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                TelemetryMetricPill(
                    icon = Icons.Filled.SignalCellularAlt,
                    label = strings.signalStrengthLabel,
                    value = "$maxSignalLevel/4".localizeDigits(strings.isPersian),
                    accent = primaryAccent,
                    modifier = Modifier.weight(1f)
                )
                val batValue = if (displayedTelemetry.batteryPercent >= 0) {
                    "${displayedTelemetry.batteryPercent}%".localizeDigits(strings.isPersian)
                } else {
                    "—"
                }
                TelemetryMetricPill(
                    icon = if (displayedTelemetry.isCharging) Icons.Filled.BatteryChargingFull else Icons.Filled.BatteryStd,
                    label = strings.hostBattery,
                    value = batValue,
                    accent = if (displayedTelemetry.batteryPercent in 0..25) amberAccent else emeraldAccent,
                    modifier = Modifier.weight(1f)
                )
                TelemetryMetricPill(
                    icon = Icons.Filled.VerifiedUser,
                    label = strings.latencyMs,
                    value = if (isConnected && latencyMs > 0) {
                        "${latencyMs}ms".localizeDigits(strings.isPersian)
                    } else {
                        "AES"
                    },
                    accent = amberAccent,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Pending Handoff Requests on Host
        if (pendingHandoffs.isNotEmpty()) {
            item {
                DashboardSectionHeader(
                    icon = Icons.Filled.NotificationsActive,
                    title = strings.pendingHandoffRequests,
                    accent = amberAccent
                )
            }
            items(pendingHandoffs, key = { it.id }) { handoff ->
                PendingHandoffCard(
                    handoff = handoff,
                    onExecute = { onExecuteHandoff(handoff) },
                    onDismiss = { onDismissHandoff(handoff.id) }
                )
            }
        }

        // Dual-SIM & Battery Telemetry Section
        item {
            GlassBridgeCard(
                modifier = Modifier.fillMaxWidth(),
                accentColor = primaryAccent,
                accentStrength = 0.28f
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        DashboardSectionHeader(
                            icon = Icons.Filled.SimCard,
                            title = strings.dualSimStatus,
                            accent = primaryAccent
                        )
                        if (displayedTelemetry.batteryPercent >= 0) {
                            val batColor = if (displayedTelemetry.batteryPercent > 25) emeraldAccent else amberAccent
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = batColor.copy(alpha = 0.14f),
                                border = BorderStroke(1.dp, batColor.copy(alpha = 0.35f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = if (displayedTelemetry.isCharging)
                                            Icons.Filled.BatteryChargingFull else Icons.Filled.BatteryStd,
                                        contentDescription = strings.hostBattery,
                                        tint = batColor,
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Text(
                                        text = "${displayedTelemetry.batteryPercent}%".localizeDigits(strings.isPersian),
                                        style = MonoTechnicalStyle,
                                        color = batColor
                                    )
                                }
                            }
                        }
                    }

                    if (activeRole == DeviceRole.HOST && !hasPhonePermission) {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = amberAccent.copy(alpha = 0.08f),
                            border = BorderStroke(1.dp, amberAccent.copy(alpha = 0.4f))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.SimCardAlert,
                                    contentDescription = null,
                                    tint = amberAccent
                                )
                                Text(
                                    text = strings.grantPhonePermForSim,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.weight(1f)
                                )
                                Button(
                                    onClick = onRequestPhonePermission,
                                    modifier = Modifier.testTag("grant_phone_perm_button"),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text(strings.grantPermission)
                                }
                            }
                        }
                    } else if (displayedTelemetry.simCards.isEmpty()) {
                        Text(
                            text = strings.noSimDetected,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            displayedTelemetry.simCards.take(2).forEach { sim ->
                                SimCardTile(
                                    sim = sim,
                                    selected = preferredSimSlot == sim.slotIndex,
                                    onClick = { onSelectPreferredSim(sim.slotIndex) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }

                    // Live Status-Bar Signal & Notification callout
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = primaryAccent.copy(alpha = 0.08f),
                        border = BorderStroke(1.dp, primaryAccent.copy(alpha = 0.25f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.NotificationsActive,
                                contentDescription = null,
                                tint = primaryAccent,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = strings.signalBarsNotificationHint,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        // Historical Battery Usage Trends Chart (Vico Material 3)
        item {
            val chartDeviceName = when {
                connectionState is ConnectionState.Connected && activeRole == DeviceRole.COMPANION ->
                    connectionState.peerName
                batteryHistory.isNotEmpty() -> batteryHistory.last().deviceName
                else -> displayedTelemetry.deviceName.takeIf { it.isNotBlank() }
            }
            BatteryHistoryChartCard(
                role = activeRole,
                deviceName = chartDeviceName,
                samples = batteryHistory,
                selectedWindow = selectedBatteryWindow,
                onSelectWindow = onSelectBatteryWindow
            )
        }

        // System Default Phone & Messages Integration Card
        item {
            val statusColor = if (isCallingAccountEnabled) emeraldAccent else amberAccent
            GlassBridgeCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("system_integration_card"),
                accentColor = statusColor,
                accentStrength = 0.36f,
                cornerRadius = 22.dp
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(statusColor.copy(alpha = 0.16f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (isCallingAccountEnabled) Icons.Filled.CheckCircle else Icons.Filled.SettingsPhone,
                                    contentDescription = null,
                                    tint = statusColor,
                                    modifier = Modifier.size(21.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = strings.systemIntegrationTitle,
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = strings.callingAccountStatus,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = statusColor.copy(alpha = 0.15f),
                            border = BorderStroke(1.dp, statusColor.copy(alpha = 0.4f))
                        ) {
                            Text(
                                text = if (isCallingAccountEnabled) strings.callingAccountEnabled else strings.callingAccountDisabled,
                                style = MaterialTheme.typography.labelSmall,
                                color = statusColor,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Text(
                        text = strings.systemRouterHint,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    if (!isCallingAccountEnabled) {
                        Button(
                            onClick = onOpenCallingAccountSettings,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("enable_calling_account_button"),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = amberAccent,
                                contentColor = MaterialTheme.colorScheme.onSecondary
                            )
                        ) {
                            Icon(Icons.Filled.SettingsPhone, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(strings.enableCallingAccount, fontWeight = FontWeight.Bold)
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = onOpenDefaultPhoneApp,
                            modifier = Modifier
                                .weight(1f)
                                .height(50.dp)
                                .testTag("open_default_phone_button"),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = emeraldAccent.copy(alpha = 0.16f),
                                contentColor = emeraldAccent
                            )
                        ) {
                            Icon(Icons.Filled.Call, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = strings.openSystemPhone,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Button(
                            onClick = onOpenDefaultMessagesApp,
                            modifier = Modifier
                                .weight(1f)
                                .height(50.dp)
                                .testTag("open_default_messages_button"),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = primaryAccent.copy(alpha = 0.16f),
                                contentColor = primaryAccent
                            )
                        ) {
                            Icon(Icons.AutoMirrored.Filled.Message, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = strings.openSystemMessages,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }

        // Latest Received Verification Code (OTP) — 1-Tap Copy
        latestOtpMessage?.extractedOtp?.let { otpCode ->
            item {
                GlassBridgeCard(
                    modifier = Modifier.fillMaxWidth(),
                    accentColor = primaryAccent,
                    accentStrength = 0.42f,
                    cornerRadius = 22.dp
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        DashboardSectionHeader(
                            icon = Icons.Filled.Key,
                            title = strings.latestOtpTitle,
                            accent = primaryAccent
                        )
                        OtpCodeChip(
                            otpCode = otpCode,
                            onCopy = {
                                clipboard.setText(AnnotatedString(otpCode))
                                onCopyFeedback("${strings.copied}: $otpCode")
                            }
                        )
                    }
                }
            }
        }

        // Quick Actions Grid with Rich Icons & Subtitles
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                DashboardSectionHeader(
                    icon = Icons.Filled.Bolt,
                    title = strings.quickActions,
                    accent = primaryAccent
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    QuickActionCard(
                        icon = Icons.Filled.ContentPasteGo,
                        title = strings.shareClipboardOtp,
                        subtitle = strings.quickSubtitleOtp,
                        accent = primaryAccent,
                        onClick = onRelayClipboardOtp,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("quick_action_clipboard_otp")
                    )
                    QuickActionCard(
                        icon = Icons.Filled.GraphicEq,
                        title = if (voiceTunnelState.isActive) strings.stopVoiceTunnel else strings.startVoiceTunnel,
                        subtitle = strings.quickSubtitleVoice,
                        accent = emeraldAccent,
                        onClick = onToggleVoiceTunnel,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("quick_action_voice_tunnel")
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    QuickActionCard(
                        icon = Icons.Filled.QrCode2,
                        title = if (activeRole == DeviceRole.HOST) strings.showQr else strings.scanQr,
                        subtitle = strings.quickSubtitleQr,
                        accent = amberAccent,
                        onClick = onNavigatePairing,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("quick_action_qr_pair")
                    )
                    QuickActionCard(
                        icon = Icons.Filled.Security,
                        title = strings.encryptedTunnel,
                        subtitle = strings.quickSubtitleSecurity,
                        accent = primaryAccent,
                        onClick = onNavigatePairing,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("quick_action_security")
                    )
                }
            }
        }

        // Recent Bridge Events
        item {
            DashboardSectionHeader(
                icon = Icons.Filled.History,
                title = strings.recentBridgeEvents,
                accent = emeraldAccent
            )
        }

        if (recentCallEvents.isEmpty()) {
            item {
                GlassBridgeCard(
                    modifier = Modifier.fillMaxWidth(),
                    accentColor = MaterialTheme.colorScheme.outline,
                    accentStrength = 0.22f,
                    cornerRadius = 18.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(primaryAccent.copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.HistoryToggleOff,
                                contentDescription = null,
                                tint = primaryAccent,
                                modifier = Modifier.size(21.dp)
                            )
                        }
                        Text(
                            text = strings.noRecentEvents,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            items(recentCallEvents.take(5), key = { it.id }) { event ->
                RecentEventRow(event = event)
            }
        }
    }
}

@Composable
private fun DashboardSectionHeader(
    icon: ImageVector,
    title: String,
    accent: Color
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(accent.copy(alpha = 0.16f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = accent,
                modifier = Modifier.size(16.dp)
            )
        }
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun RoleSwitchPill(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    accent: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .height(46.dp)
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        color = if (selected) accent.copy(alpha = 0.18f) else Color.Transparent,
        border = if (selected) BorderStroke(1.dp, accent.copy(alpha = 0.5f)) else null
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (selected) accent else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(17.dp)
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                color = if (selected) accent else MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun TelemetryMetricPill(
    icon: ImageVector,
    label: String,
    value: String,
    accent: Color,
    modifier: Modifier = Modifier
) {
    GlassBridgeCard(
        modifier = modifier,
        accentColor = accent,
        accentStrength = 0.28f,
        cornerRadius = 18.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        listOf(accent.copy(alpha = 0.10f), Color.Transparent)
                    )
                )
                .padding(horizontal = 10.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(30.dp)
                    .clip(CircleShape)
                    .background(accent.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = accent,
                    modifier = Modifier.size(16.dp)
                )
            }
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                Text(
                    text = value,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1
                )
            }
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun FirstRunRolePicker(
    onSelectRole: (DeviceRole) -> Unit,
    onToggleLanguage: () -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = LocalAppStrings.current
    val primaryAccent = MaterialTheme.colorScheme.primary
    val emeraldAccent = MaterialTheme.colorScheme.tertiary
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("first_run_role_picker"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            HeroArtworkBanner(
                drawableRes = R.drawable.img_bridge_hero_1795556747511,
                contentDescription = strings.appTitle,
                height = 225.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp)
                        .align(Alignment.TopCenter),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            modifier = Modifier.size(38.dp),
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceContainerLowest,
                            border = BorderStroke(1.dp, primaryAccent.copy(alpha = 0.6f))
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.itell_icon_fg_1795555028207),
                                contentDescription = strings.appTitle,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.90f),
                            border = BorderStroke(1.dp, primaryAccent.copy(alpha = 0.45f))
                        ) {
                            Text(
                                text = strings.appTitle,
                                style = MaterialTheme.typography.titleMedium,
                                color = primaryAccent,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                            )
                        }
                    }
                    Surface(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .clickable(onClick = onToggleLanguage)
                            .testTag("onboarding_language_button"),
                        shape = RoundedCornerShape(50),
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.90f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(Icons.Filled.Language, contentDescription = null, tint = primaryAccent, modifier = Modifier.size(16.dp))
                            Text(
                                text = if (strings.isPersian) "English" else "فارسی",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }

                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Text(
                        text = strings.chooseDeviceRoleTitle,
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = strings.chooseDeviceRoleSubtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        item {
            RoleChoiceCard(
                icon = Icons.Filled.WifiTethering,
                title = strings.roleHost,
                description = strings.roleHostDesc,
                badgeText = "SIM 1/2 • QR",
                accent = primaryAccent,
                onClick = { onSelectRole(DeviceRole.HOST) },
                modifier = Modifier.testTag("choose_host_role_card")
            )
        }
        item {
            RoleChoiceCard(
                icon = Icons.Filled.Link,
                title = strings.roleCompanion,
                description = strings.roleCompanionDesc,
                badgeText = "Phone • SMS • Live Signal",
                accent = emeraldAccent,
                onClick = { onSelectRole(DeviceRole.COMPANION) },
                modifier = Modifier.testTag("choose_companion_role_card")
            )
        }
        item {
            GlassBridgeCard(
                modifier = Modifier.fillMaxWidth(),
                accentColor = emeraldAccent,
                accentStrength = 0.32f,
                cornerRadius = 18.dp
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        imageVector = Icons.Filled.Security,
                        contentDescription = null,
                        tint = emeraldAccent
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = strings.playComplianceTitle,
                            style = MaterialTheme.typography.titleSmall,
                            color = emeraldAccent,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = strings.playComplianceBody,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
        item {
            GlassBridgeCard(
                modifier = Modifier.fillMaxWidth(),
                accentColor = primaryAccent,
                accentStrength = 0.42f,
                cornerRadius = 16.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.AutoAwesome,
                        contentDescription = null,
                        tint = primaryAccent,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = strings.creatorCredit,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun RoleChoiceCard(
    icon: ImageVector,
    title: String,
    description: String,
    badgeText: String,
    accent: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    GlassBridgeCard(
        modifier = modifier.fillMaxWidth(),
        accentColor = accent,
        accentStrength = 0.55f,
        cornerRadius = 24.dp,
        onClick = onClick
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(accent.copy(alpha = 0.14f), Color.Transparent)
                    )
                )
                .padding(18.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(accent.copy(alpha = 0.18f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(imageVector = icon, contentDescription = null, tint = accent, modifier = Modifier.size(28.dp))
                }
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Text(text = title, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface)
                    Text(text = description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = accent.copy(alpha = 0.14f)
                    ) {
                        Text(
                            text = badgeText,
                            style = MonoTechnicalStyle,
                            color = accent,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = accent.copy(alpha = 0.8f),
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

@Composable
private fun QuickActionCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    accent: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    GlassBridgeCard(
        modifier = modifier,
        accentColor = accent,
        accentStrength = 0.36f,
        cornerRadius = 20.dp,
        onClick = onClick
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(accent.copy(alpha = 0.12f), Color.Transparent)
                    )
                )
                .padding(14.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(RoundedCornerShape(13.dp))
                            .background(accent.copy(alpha = 0.18f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(imageVector = icon, contentDescription = title, tint = accent, modifier = Modifier.size(22.dp))
                    }
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = null,
                        tint = accent.copy(alpha = 0.65f),
                        modifier = Modifier.size(18.dp)
                    )
                }
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
private fun PendingHandoffCard(
    handoff: PendingHandoff,
    onExecute: () -> Unit,
    onDismiss: () -> Unit
) {
    val strings = LocalAppStrings.current
    val amberAccent = MaterialTheme.colorScheme.secondary
    val isDial = handoff.type == HandoffType.DIAL_REQUEST
    GlassBridgeCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("pending_handoff_${handoff.id}"),
        accentColor = amberAccent,
        accentStrength = 0.7f,
        cornerRadius = 20.dp
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(amberAccent.copy(alpha = 0.16f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isDial) Icons.Filled.Call else Icons.AutoMirrored.Filled.Message,
                        contentDescription = null,
                        tint = amberAccent
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (isDial) strings.incomingDialRequest else strings.incomingSmsRequest,
                        style = MaterialTheme.typography.titleSmall,
                        color = amberAccent
                    )
                    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                        Text(
                            text = "${handoff.phoneNumber} • ${strings.simSlotLabel} ${handoff.simSlot}".localizeDigits(strings.isPersian),
                            style = MonoTechnicalStyle,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    if (handoff.body.isNotBlank()) {
                        Text(
                            text = handoff.body,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = onExecute,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = amberAccent, contentColor = MaterialTheme.colorScheme.onSecondary),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text(if (isDial) strings.openDialer else strings.openSmsApp, fontWeight = FontWeight.Bold)
                }
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.height(48.dp),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text(strings.dismiss)
                }
            }
        }
    }
}

@Composable
private fun RecentEventRow(event: BridgeCallEventEntity) {
    val strings = LocalAppStrings.current
    val primaryAccent = MaterialTheme.colorScheme.primary
    val label = when (event.eventType) {
        "INCOMING_ALERT" -> strings.callTypeIncoming
        "VOICE_TUNNEL" -> strings.callTypeVoiceTunnel
        else -> strings.callTypeRemoteDial
    }
    val timeText = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(event.timestamp))
    GlassBridgeCard(
        modifier = Modifier.fillMaxWidth(),
        accentColor = primaryAccent,
        accentStrength = 0.22f,
        cornerRadius = 16.dp
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(primaryAccent.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (event.eventType == "VOICE_TUNNEL") Icons.Filled.GraphicEq else Icons.Filled.PhoneAndroid,
                    contentDescription = null,
                    tint = primaryAccent,
                    modifier = Modifier.size(19.dp)
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = event.contactName,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "$label • ${strings.simSlotLabel} ${event.simSlot}".localizeDigits(strings.isPersian),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = timeText.localizeDigits(strings.isPersian),
                style = MonoTechnicalStyle,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
