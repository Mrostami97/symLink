package com.example.ui.screens

import android.util.Size
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.R
import com.example.bridge.BridgeProtocol
import com.example.bridge.ConnectionState
import com.example.bridge.DeviceRole
import com.example.bridge.DiscoveredPeer
import com.example.bridge.PairingPayload
import com.example.bridge.QrCodeAnalyzer
import com.example.bridge.QrCodeUtils
import com.example.bridge.TransportMode
import com.example.data.local.PairedDeviceEntity
import com.example.ui.components.GlassBridgeCard
import com.example.ui.components.HeroArtworkBanner
import com.example.ui.i18n.LocalAppStrings
import com.example.ui.theme.ConnectedEmerald
import com.example.ui.theme.MonoDialpadStyle
import com.example.ui.theme.MonoTechnicalStyle
import com.example.ui.theme.SignalCyan
import java.util.concurrent.Executors

@Composable
fun PairingScreen(
    role: DeviceRole,
    connectionState: ConnectionState,
    hostPayload: PairingPayload?,
    discoveredPeers: List<DiscoveredPeer>,
    savedDevices: List<PairedDeviceEntity>,
    hasCameraPermission: Boolean,
    hasBluetoothPermission: Boolean,
    onStartHost: () -> Unit,
    onStopBridge: () -> Unit,
    onRequestCameraPermission: () -> Unit,
    onRequestBluetoothPermission: () -> Unit,
    onStartDiscovery: () -> Unit,
    onQrScanned: (String) -> Unit,
    onConnectDiscoveredPeer: (DiscoveredPeer, String) -> Unit,
    onConnectManual: (String, Int, String) -> Unit,
    onReconnectSaved: (PairedDeviceEntity) -> Unit,
    onForgetSaved: (String) -> Unit,
    onCopyFeedback: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = LocalAppStrings.current
    val clipboard = LocalClipboardManager.current
    val haptic = LocalHapticFeedback.current

    var isScanningQr by remember { mutableStateOf(false) }
    var showManualConnect by remember { mutableStateOf(false) }
    var manualIp by remember { mutableStateOf("") }
    var manualPort by remember { mutableStateOf(BridgeProtocol.DEFAULT_PORT.toString()) }
    var manualCode by remember { mutableStateOf("") }
    var selectedPeerForCode by remember { mutableStateOf<DiscoveredPeer?>(null) }
    var peerDialogCode by remember { mutableStateOf("") }

    selectedPeerForCode?.let { peer ->
        AlertDialog(
            onDismissRequest = { selectedPeerForCode = null },
            title = { Text(peer.name) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(strings.pairingCodeLabel, style = MaterialTheme.typography.bodySmall)
                    OutlinedTextField(
                        value = peerDialogCode,
                        onValueChange = { if (it.length <= 6) peerDialogCode = it },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        label = { Text(strings.pairingCodeLabel) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("peer_pair_code_input")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onConnectDiscoveredPeer(peer, peerDialogCode)
                        selectedPeerForCode = null
                        peerDialogCode = ""
                    },
                    enabled = peerDialogCode.length >= 4
                ) {
                    Text(strings.connect)
                }
            },
            dismissButton = {
                TextButton(onClick = { selectedPeerForCode = null }) {
                    Text(strings.dismiss)
                }
            }
        )
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("pairing_screen"),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Pairing Mesh Hero Header
        item {
            HeroArtworkBanner(
                drawableRes = R.drawable.img_pairing_mesh_1795556747776,
                contentDescription = if (role == DeviceRole.HOST) strings.hostPairingTitle else strings.companionPairingTitle,
                height = 122.dp
            ) {
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = if (role == DeviceRole.HOST) strings.hostPairingTitle else strings.companionPairingTitle,
                        style = MaterialTheme.typography.headlineSmall,
                        color = Color.White
                    )
                    Text(
                        text = if (role == DeviceRole.HOST) strings.hostPairingSubtitle else strings.companionPairingSubtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFCBD5E1)
                    )
                }
            }
        }

        if (role == DeviceRole.HOST) {
            item {
                if (hostPayload == null) {
                    GlassBridgeCard(
                        modifier = Modifier.fillMaxWidth(),
                        accentColor = SignalCyan,
                        accentStrength = 0.35f
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(22.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(56.dp)
                                    .clip(RoundedCornerShape(18.dp))
                                    .background(SignalCyan.copy(alpha = 0.16f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Wifi,
                                    contentDescription = null,
                                    tint = SignalCyan,
                                    modifier = Modifier.size(30.dp)
                                )
                            }
                            Text(
                                text = strings.statusIdle,
                                style = MaterialTheme.typography.titleMedium
                            )
                            Button(
                                onClick = onStartHost,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp)
                                    .testTag("pairing_start_host_button"),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Text(strings.startBridge, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                } else {
                    val qrBitmap = remember(hostPayload.toQrString()) {
                        QrCodeUtils.generateQrBitmap(hostPayload.toQrString(), 520)
                    }
                    GlassBridgeCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("host_qr_card"),
                        accentColor = SignalCyan,
                        accentStrength = 0.58f,
                        cornerRadius = 26.dp
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            // QR Code framed with luminous precision brackets
                            if (qrBitmap != null) {
                                Box(
                                    modifier = Modifier.size(236.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(20.dp),
                                        color = Color(0xFFF8FAFC),
                                        modifier = Modifier.size(214.dp)
                                    ) {
                                        Image(
                                            bitmap = qrBitmap.asImageBitmap(),
                                            contentDescription = strings.showQr,
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .padding(12.dp)
                                        )
                                    }
                                    Canvas(modifier = Modifier.fillMaxSize()) {
                                        drawCornerBrackets(SignalCyan, cornerLen = 28.dp.toPx(), stroke = 3.5.dp.toPx())
                                    }
                                }
                            }

                            // 6-Digit Pairing Code Boxes
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = strings.pairingCodeLabel,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        hostPayload.pairCode.forEach { digit ->
                                            Surface(
                                                shape = RoundedCornerShape(12.dp),
                                                color = SignalCyan.copy(alpha = 0.14f),
                                                border = BorderStroke(1.dp, SignalCyan.copy(alpha = 0.45f)),
                                                modifier = Modifier.size(width = 40.dp, height = 48.dp)
                                            ) {
                                                Box(contentAlignment = Alignment.Center) {
                                                    Text(
                                                        text = digit.toString(),
                                                        style = MonoDialpadStyle.copy(fontSize = 22.sp),
                                                        color = SignalCyan
                                                    )
                                                }
                                            }
                                        }
                                        IconButton(
                                            onClick = {
                                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                clipboard.setText(AnnotatedString(hostPayload.pairCode))
                                                onCopyFeedback(strings.copied)
                                            },
                                            modifier = Modifier.testTag("copy_pair_code_button")
                                        ) {
                                            Icon(Icons.Filled.ContentCopy, contentDescription = strings.copyCode, tint = SignalCyan)
                                        }
                                    }
                                }
                            }

                            // Local Endpoint Box
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = MaterialTheme.colorScheme.surfaceContainer,
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = strings.localEndpointLabel,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                                            Text(
                                                text = "${hostPayload.hostIp}:${hostPayload.port}",
                                                style = MonoTechnicalStyle,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                    }
                                    IconButton(
                                        onClick = {
                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            clipboard.setText(AnnotatedString("${hostPayload.hostIp}:${hostPayload.port}"))
                                            onCopyFeedback(strings.copied)
                                        }
                                    ) {
                                        Icon(Icons.Filled.ContentCopy, contentDescription = strings.copyCode)
                                    }
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                TransportPill(
                                    icon = Icons.Filled.Wifi,
                                    label = strings.wifiTransport,
                                    active = true,
                                    modifier = Modifier.weight(1f)
                                )
                                TransportPill(
                                    icon = Icons.Filled.Bluetooth,
                                    label = strings.bluetoothTransport,
                                    active = (connectionState as? ConnectionState.Hosting)?.bluetoothReady == true,
                                    modifier = Modifier.weight(1f)
                                )
                            }

                            if (!hasBluetoothPermission) {
                                OutlinedButton(
                                    onClick = onRequestBluetoothPermission,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(48.dp)
                                        .testTag("enable_bt_host_button"),
                                    shape = RoundedCornerShape(14.dp)
                                ) {
                                    Text(strings.enableBluetoothPerm)
                                }
                            }

                            OutlinedButton(
                                onClick = onStopBridge,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .testTag("pairing_stop_host_button"),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Text(strings.stopBridge)
                            }
                        }
                    }
                }
            }
        } else {
            // COMPANION MODE — CameraX Live QR Scanner with Sweeping Laser
            item {
                GlassBridgeCard(
                    modifier = Modifier.fillMaxWidth(),
                    accentColor = SignalCyan,
                    accentStrength = 0.45f
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            onClick = {
                                if (!hasCameraPermission) {
                                    onRequestCameraPermission()
                                } else {
                                    isScanningQr = !isScanningQr
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("toggle_qr_scanner_button"),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Icon(Icons.Filled.QrCodeScanner, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text(if (isScanningQr) strings.stopScan else strings.scanQr, fontWeight = FontWeight.Bold)
                        }

                        if (!hasCameraPermission) {
                            Text(
                                text = strings.cameraPermNeeded,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        if (isScanningQr && hasCameraPermission) {
                            QrCameraPreviewBox(
                                onCodeDetected = { raw ->
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    isScanningQr = false
                                    onQrScanned(raw)
                                }
                            )
                        }
                    }
                }
            }

            // Nearby Discovered Wi-Fi & Bluetooth Peers
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = strings.nearbyHostsWifi,
                        style = MaterialTheme.typography.titleMedium
                    )
                    OutlinedButton(
                        onClick = onStartDiscovery,
                        modifier = Modifier.testTag("refresh_discovery_button"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Filled.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(strings.refreshScan)
                    }
                }
            }

            if (!hasBluetoothPermission) {
                item {
                    OutlinedButton(
                        onClick = onRequestBluetoothPermission,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("enable_bt_companion_button"),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(Icons.Filled.Bluetooth, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(strings.enableBluetoothPerm)
                    }
                }
            }

            if (discoveredPeers.isEmpty()) {
                item {
                    GlassBridgeCard(
                        modifier = Modifier.fillMaxWidth(),
                        accentColor = MaterialTheme.colorScheme.outline,
                        accentStrength = 0.2f,
                        cornerRadius = 18.dp
                    ) {
                        Text(
                            text = strings.noNearbyHosts,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
            } else {
                items(discoveredPeers, key = { it.id }) { peer ->
                    GlassBridgeCard(
                        modifier = Modifier.fillMaxWidth(),
                        accentColor = SignalCyan,
                        accentStrength = 0.35f,
                        cornerRadius = 18.dp
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(SignalCyan.copy(alpha = 0.14f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (peer.transport == TransportMode.WIFI_TCP) Icons.Filled.Wifi else Icons.Filled.Bluetooth,
                                    contentDescription = null,
                                    tint = SignalCyan
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(peer.name, style = MaterialTheme.typography.titleSmall)
                                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                                    Text(
                                        text = if (peer.transport == TransportMode.WIFI_TCP)
                                            "${peer.hostIp}:${peer.port}" else peer.btAddress,
                                        style = MonoTechnicalStyle,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Button(
                                onClick = { selectedPeerForCode = peer },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.height(48.dp)
                            ) {
                                Text(strings.connect)
                            }
                        }
                    }
                }
            }

            // Collapsible Manual IP + Port + 6-Digit Code Card (keeps Pairing clean & easy)
            item {
                GlassBridgeCard(
                    modifier = Modifier.fillMaxWidth(),
                    accentColor = ConnectedEmerald,
                    accentStrength = 0.28f,
                    onClick = { showManualConnect = !showManualConnect }
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = strings.advancedManualConnect,
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Icon(
                                imageVector = if (showManualConnect) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                                contentDescription = strings.advancedManualConnect,
                                tint = ConnectedEmerald
                            )
                        }
                        AnimatedVisibility(visible = showManualConnect) {
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedTextField(
                                        value = manualIp,
                                        onValueChange = { manualIp = it },
                                        label = { Text(strings.hostIpLabel) },
                                        placeholder = { Text("192.168.43.1") },
                                        singleLine = true,
                                        shape = RoundedCornerShape(14.dp),
                                        modifier = Modifier
                                            .weight(2f)
                                            .testTag("manual_ip_input")
                                    )
                                    OutlinedTextField(
                                        value = manualPort,
                                        onValueChange = { manualPort = it.filter(Char::isDigit).take(5) },
                                        label = { Text(strings.portLabel) },
                                        singleLine = true,
                                        shape = RoundedCornerShape(14.dp),
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("manual_port_input")
                                    )
                                }
                                OutlinedTextField(
                                    value = manualCode,
                                    onValueChange = { if (it.length <= 6) manualCode = it },
                                    label = { Text(strings.pairingCodeLabel) },
                                    singleLine = true,
                                    shape = RoundedCornerShape(14.dp),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("manual_pair_code_input")
                                )
                                Button(
                                    onClick = {
                                        val parsedPort = manualPort.toIntOrNull() ?: BridgeProtocol.DEFAULT_PORT
                                        onConnectManual(manualIp, parsedPort, manualCode)
                                    },
                                    enabled = manualIp.isNotBlank() && manualCode.length >= 4,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(50.dp)
                                        .testTag("manual_connect_button"),
                                    shape = RoundedCornerShape(16.dp)
                                ) {
                                    Text(strings.connect, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }

        // Saved Paired Devices
        item {
            Text(
                text = strings.savedDevices,
                style = MaterialTheme.typography.titleMedium
            )
        }
        if (savedDevices.isEmpty()) {
            item {
                Text(
                    text = strings.noSavedDevices,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            items(savedDevices, key = { it.deviceId }) { device ->
                GlassBridgeCard(
                    modifier = Modifier.fillMaxWidth(),
                    accentColor = ConnectedEmerald,
                    accentStrength = 0.25f,
                    cornerRadius = 18.dp
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(ConnectedEmerald.copy(alpha = 0.14f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Filled.Devices, contentDescription = null, tint = ConnectedEmerald)
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(device.deviceName, style = MaterialTheme.typography.titleSmall)
                            Text(
                                text = "${device.ipAddress}:${device.port} • ${device.fingerprint}",
                                style = MonoTechnicalStyle,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (role == DeviceRole.COMPANION) {
                            TextButton(onClick = { onReconnectSaved(device) }) {
                                Text(strings.reconnect)
                            }
                        }
                        IconButton(onClick = { onForgetSaved(device.deviceId) }) {
                            Icon(
                                imageVector = Icons.Filled.DeleteOutline,
                                contentDescription = strings.forgetDevice,
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TransportPill(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    active: Boolean,
    modifier: Modifier = Modifier
) {
    val tint = if (active) ConnectedEmerald else MaterialTheme.colorScheme.onSurfaceVariant
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = if (active) ConnectedEmerald.copy(alpha = 0.14f) else MaterialTheme.colorScheme.surfaceContainer,
        border = BorderStroke(1.dp, tint.copy(alpha = 0.4f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = tint, modifier = Modifier.size(16.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = tint,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun QrCameraPreviewBox(
    onCodeDetected: (String) -> Unit
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val executor = remember { Executors.newSingleThreadExecutor() }

    DisposableEffect(Unit) {
        onDispose { executor.shutdown() }
    }

    val infinite = rememberInfiniteTransition(label = "qrLaser")
    val laserYFraction by infinite.animateFloat(
        initialValue = 0.18f,
        targetValue = 0.82f,
        animationSpec = infiniteRepeatable(
            animation = tween(1700, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "laserSweep"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1.1f)
            .clip(RoundedCornerShape(20.dp))
            .background(Color.Black)
            .testTag("qr_camera_preview")
    ) {
        AndroidView(
            factory = { ctx ->
                val previewView = PreviewView(ctx)
                val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                cameraProviderFuture.addListener({
                    runCatching {
                        val cameraProvider = cameraProviderFuture.get()
                        val preview = Preview.Builder().build().also {
                            it.surfaceProvider = previewView.surfaceProvider
                        }
                        @Suppress("DEPRECATION")
                        val analysis = ImageAnalysis.Builder()
                            .setTargetResolution(Size(960, 720))
                            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                            .build()
                            .also {
                                it.setAnalyzer(executor, QrCodeAnalyzer { qr ->
                                    ContextCompat.getMainExecutor(ctx).execute {
                                        onCodeDetected(qr)
                                    }
                                })
                            }
                        cameraProvider.unbindAll()
                        cameraProvider.bindToLifecycle(
                            lifecycleOwner,
                            CameraSelector.DEFAULT_BACK_CAMERA,
                            preview,
                            analysis
                        )
                    }
                }, ContextCompat.getMainExecutor(ctx))
                previewView
            },
            modifier = Modifier.fillMaxSize()
        )

        // Precision corner brackets + animated glowing cyan laser line
        Canvas(modifier = Modifier.fillMaxSize()) {
            val inset = size.minDimension * 0.16f
            val boxSize = size.minDimension - inset * 2f
            val left = (size.width - boxSize) / 2f
            val top = (size.height - boxSize) / 2f
            val right = left + boxSize
            val bottom = top + boxSize
            val corner = 28.dp.toPx()
            val stroke = 4.dp.toPx()

            // Four precision corner brackets
            drawLine(SignalCyan, Offset(left, top), Offset(left + corner, top), stroke, StrokeCap.Round)
            drawLine(SignalCyan, Offset(left, top), Offset(left, top + corner), stroke, StrokeCap.Round)

            drawLine(SignalCyan, Offset(right, top), Offset(right - corner, top), stroke, StrokeCap.Round)
            drawLine(SignalCyan, Offset(right, top), Offset(right, top + corner), stroke, StrokeCap.Round)

            drawLine(SignalCyan, Offset(left, bottom), Offset(left + corner, bottom), stroke, StrokeCap.Round)
            drawLine(SignalCyan, Offset(left, bottom), Offset(left, bottom - corner), stroke, StrokeCap.Round)

            drawLine(SignalCyan, Offset(right, bottom), Offset(right - corner, bottom), stroke, StrokeCap.Round)
            drawLine(SignalCyan, Offset(right, bottom), Offset(right, bottom - corner), stroke, StrokeCap.Round)

            // Animated cyan laser scan beam
            val beamY = size.height * laserYFraction
            drawLine(
                brush = Brush.horizontalGradient(
                    colors = listOf(Color.Transparent, SignalCyan, ConnectedEmerald, SignalCyan, Color.Transparent),
                    startX = left,
                    endX = right
                ),
                start = Offset(left + 8f, beamY),
                end = Offset(right - 8f, beamY),
                strokeWidth = 3.5.dp.toPx(),
                cap = StrokeCap.Round
            )
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawCornerBrackets(
    color: Color,
    cornerLen: Float,
    stroke: Float
) {
    val w = size.width
    val h = size.height
    drawLine(color, Offset(0f, 0f), Offset(cornerLen, 0f), stroke, StrokeCap.Round)
    drawLine(color, Offset(0f, 0f), Offset(0f, cornerLen), stroke, StrokeCap.Round)

    drawLine(color, Offset(w, 0f), Offset(w - cornerLen, 0f), stroke, StrokeCap.Round)
    drawLine(color, Offset(w, 0f), Offset(w, cornerLen), stroke, StrokeCap.Round)

    drawLine(color, Offset(0f, h), Offset(cornerLen, h), stroke, StrokeCap.Round)
    drawLine(color, Offset(0f, h), Offset(0f, h - cornerLen), stroke, StrokeCap.Round)

    drawLine(color, Offset(w, h), Offset(w - cornerLen, h), stroke, StrokeCap.Round)
    drawLine(color, Offset(w, h), Offset(w, h - cornerLen), stroke, StrokeCap.Round)
}
