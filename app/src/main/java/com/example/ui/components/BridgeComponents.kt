package com.example.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.PhoneInTalk
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.SimCard
import androidx.compose.material.icons.filled.WifiTethering
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.bridge.ConnectionState
import com.example.bridge.DeviceRole
import com.example.bridge.IncomingCallBanner
import com.example.bridge.SimCardInfo
import com.example.bridge.TransportMode
import com.example.bridge.VoiceTunnelState
import com.example.ui.i18n.LocalAppStrings
import com.example.ui.i18n.localizeDigits
import com.example.ui.theme.LocalIsDarkTheme
import com.example.ui.theme.MonoTechnicalStyle
import kotlinx.coroutines.delay

@Composable
fun SignalCoreHeroCard(
    role: DeviceRole,
    connectionState: ConnectionState,
    latencyMs: Long,
    onPrimaryAction: () -> Unit,
    onSecondaryAction: () -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = LocalAppStrings.current
    val isDark = LocalIsDarkTheme.current
    val primaryAccent = MaterialTheme.colorScheme.primary
    val emeraldAccent = MaterialTheme.colorScheme.tertiary
    val errorAccent = MaterialTheme.colorScheme.error
    val isConnected = connectionState is ConnectionState.Connected
    val isActivePulse = connectionState is ConnectionState.Hosting ||
        connectionState is ConnectionState.Discovering ||
        connectionState is ConnectionState.Connecting ||
        isConnected

    val targetAccent = when (connectionState) {
        is ConnectionState.Connected -> emeraldAccent
        is ConnectionState.Hosting,
        is ConnectionState.Discovering,
        is ConnectionState.Connecting -> primaryAccent
        is ConnectionState.Error -> errorAccent
        else -> primaryAccent
    }
    val accentColor by animateColorAsState(
        targetValue = targetAccent,
        animationSpec = tween(350),
        label = "heroAccentColor"
    )

    val statusTitle = when (connectionState) {
        is ConnectionState.Idle -> strings.statusIdle
        is ConnectionState.Hosting -> strings.statusHosting
        is ConnectionState.Discovering -> strings.statusDiscovering
        is ConnectionState.Connecting -> strings.statusConnecting
        is ConnectionState.Connected -> strings.statusConnected
        is ConnectionState.Reconnecting -> strings.statusReconnecting
        is ConnectionState.Error -> "${strings.statusError}: ${connectionState.message}"
    }

    val infinite = rememberInfiniteTransition(label = "signalCoreMotion")
    val pulseProgress by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulseRadius"
    )
    val streamProgress by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1600, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "streamPacket"
    )

    GlassBridgeCard(
        modifier = modifier
            .fillMaxWidth()
            .testTag("signal_core_hero_card"),
        accentColor = accentColor,
        accentStrength = if (isConnected) 0.62f else 0.34f,
        cornerRadius = 26.dp
    ) {
        // Subtle pairing mesh artwork backdrop
        Image(
            painter = painterResource(id = R.drawable.img_pairing_mesh_1795556747776),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .matchParentSize()
                .alpha(if (isDark) 0.24f else 0.08f)
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            accentColor.copy(alpha = if (isDark) 0.20f else 0.11f),
                            Color.Transparent
                        ),
                        center = Offset(380f, 120f),
                        radius = 620f
                    )
                )
                .padding(20.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                // Header Row: Role badge + Live Ping / Transport pill
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.85f),
                        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.35f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(accentColor)
                            )
                            Icon(
                                imageVector = if (role == DeviceRole.HOST) Icons.Filled.WifiTethering else Icons.Filled.Link,
                                contentDescription = null,
                                tint = accentColor,
                                modifier = Modifier.size(15.dp)
                            )
                            Text(
                                text = if (role == DeviceRole.HOST) strings.roleHost else strings.roleCompanion,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    if (connectionState is ConnectionState.Connected) {
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = emeraldAccent.copy(alpha = 0.16f),
                            border = BorderStroke(1.dp, emeraldAccent.copy(alpha = 0.45f))
                        ) {
                            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                                Text(
                                    text = buildString {
                                        append(if (connectionState.transport == TransportMode.WIFI_TCP) "Wi-Fi" else "BT")
                                        if (latencyMs > 0) append(" • ${latencyMs}ms")
                                    }.localizeDigits(strings.isPersian),
                                    style = MonoTechnicalStyle,
                                    color = emeraldAccent,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                )
                            }
                        }
                    }
                }

                // Dual-Node Bridge Signal Visualization
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(86.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(
                            if (isDark) MaterialTheme.colorScheme.background.copy(alpha = 0.55f)
                            else MaterialTheme.colorScheme.surfaceContainerLow
                        )
                        .padding(horizontal = 18.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val leftX = 32.dp.toPx()
                        val rightX = size.width - 32.dp.toPx()
                        val midY = size.height / 2f
                        val ctrlY = midY - 22.dp.toPx()

                        // Base bridge arc
                        val arcPath = Path().apply {
                            moveTo(leftX, midY)
                            quadraticTo(size.width / 2f, ctrlY, rightX, midY)
                        }
                        drawPath(
                            path = arcPath,
                            brush = Brush.horizontalGradient(
                                colors = listOf(
                                    primaryAccent.copy(alpha = if (isConnected) 0.85f else 0.35f),
                                    emeraldAccent.copy(alpha = if (isConnected) 0.9f else 0.30f)
                                )
                            ),
                            style = Stroke(width = if (isConnected) 5f else 3f, cap = StrokeCap.Round)
                        )

                        // Animated traveling data pulses along quadratic curve
                        if (isActivePulse) {
                            listOf(streamProgress, (streamProgress + 0.5f) % 1f).forEach { t ->
                                val inv = 1f - t
                                val px = inv * inv * leftX + 2 * inv * t * (size.width / 2f) + t * t * rightX
                                val py = inv * inv * midY + 2 * inv * t * ctrlY + t * t * midY
                                drawCircle(
                                    color = accentColor.copy(alpha = 0.35f),
                                    radius = 14f,
                                    center = Offset(px, py)
                                )
                                drawCircle(
                                    color = if (isDark) Color.White else accentColor,
                                    radius = 5.5f,
                                    center = Offset(px, py)
                                )
                            }
                        }

                        // Pulsing halos on active node(s)
                        val hostCenter = Offset(leftX, midY)
                        val peerCenter = Offset(rightX, midY)
                        if (isActivePulse) {
                            val r = 18.dp.toPx() + 18.dp.toPx() * pulseProgress
                            val a = (1f - pulseProgress) * 0.45f
                            drawCircle(
                                color = accentColor.copy(alpha = a),
                                radius = r,
                                center = hostCenter,
                                style = Stroke(width = 2.5f)
                            )
                            if (isConnected) {
                                drawCircle(
                                    color = emeraldAccent.copy(alpha = a),
                                    radius = r,
                                    center = peerCenter,
                                    style = Stroke(width = 2.5f)
                                )
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        NodeDeviceBadge(
                            accent = primaryAccent,
                            active = true
                        )
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = accentColor.copy(alpha = 0.16f),
                            border = BorderStroke(1.dp, accentColor.copy(alpha = 0.45f))
                        ) {
                            Text(
                                text = if (isConnected) "AES-256" else "P2P",
                                style = MonoTechnicalStyle.copy(fontSize = 11.sp),
                                color = accentColor,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
                            )
                        }
                        NodeDeviceBadge(
                            accent = if (isConnected) emeraldAccent else MaterialTheme.colorScheme.onSurfaceVariant,
                            active = isConnected
                        )
                    }
                }

                // Status & Telemetry Copy
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = statusTitle,
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    when (connectionState) {
                        is ConnectionState.Connected -> {
                            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                                Text(
                                    text = "${connectionState.peerName} • ${connectionState.endpointLabel}",
                                    style = MonoTechnicalStyle,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Security,
                                    contentDescription = null,
                                    tint = emeraldAccent,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = "${strings.securityFingerprint}:",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = emeraldAccent
                                )
                                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                                    Text(
                                        text = connectionState.fingerprint,
                                        style = MonoTechnicalStyle,
                                        color = emeraldAccent
                                    )
                                }
                            }
                        }
                        is ConnectionState.Hosting -> {
                            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                                Text(
                                    text = "${connectionState.hostIp}:${connectionState.port}",
                                    style = MonoTechnicalStyle,
                                    color = primaryAccent
                                )
                            }
                            Text(
                                text = strings.encryptedTunnel,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        else -> {
                            Text(
                                text = strings.appTagline,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    val primaryText = when {
                        isConnected -> strings.disconnect
                        role == DeviceRole.HOST && connectionState is ConnectionState.Hosting -> strings.stopBridge
                        role == DeviceRole.HOST -> strings.startBridge
                        else -> strings.scanQr
                    }
                    Button(
                        onClick = onPrimaryAction,
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp)
                            .testTag("hero_primary_action_button"),
                        shape = RoundedCornerShape(16.dp),
                        colors = if (isConnected || connectionState is ConnectionState.Hosting) {
                            ButtonDefaults.buttonColors(
                                containerColor = errorAccent.copy(alpha = 0.16f),
                                contentColor = errorAccent
                            )
                        } else {
                            ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                    ) {
                        Text(text = primaryText, fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = onSecondaryAction,
                        modifier = Modifier
                            .height(50.dp)
                            .testTag("hero_secondary_action_button"),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.45f))
                    ) {
                        Text(
                            text = if (role == DeviceRole.HOST) strings.showQr else strings.switchRole,
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun NodeDeviceBadge(
    accent: Color,
    active: Boolean
) {
    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(accent.copy(alpha = if (active) 0.18f else 0.08f)),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Filled.PhoneAndroid,
            contentDescription = null,
            tint = accent,
            modifier = Modifier.size(22.dp)
        )
    }
}

@Composable
fun SimCardTile(
    sim: SimCardInfo,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = LocalAppStrings.current
    val simAccent = if (sim.slotIndex == 1) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary

    GlassBridgeCard(
        modifier = modifier.testTag("sim_card_slot_${sim.slotIndex}"),
        accentColor = simAccent,
        accentStrength = if (selected) 0.65f else 0.18f,
        cornerRadius = 18.dp,
        onClick = onClick
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    if (selected) {
                        Brush.verticalGradient(
                            colors = listOf(simAccent.copy(alpha = 0.14f), Color.Transparent)
                        )
                    } else {
                        Brush.verticalGradient(listOf(Color.Transparent, Color.Transparent))
                    }
                )
                .padding(14.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(simAccent.copy(alpha = 0.16f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.SimCard,
                                contentDescription = null,
                                tint = simAccent,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Text(
                            text = "${strings.simSlotLabel} ${sim.slotIndex}".localizeDigits(strings.isPersian),
                            style = MaterialTheme.typography.labelMedium,
                            color = simAccent,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = simAccent.copy(alpha = 0.16f)
                    ) {
                        Text(
                            text = sim.networkType,
                            style = MonoTechnicalStyle.copy(fontSize = 11.sp),
                            color = simAccent,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = sim.carrierName,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    // Real 4-bar hardware signal indicator + numeric level (always LTR ascending)
                    val level = sim.signalLevel.coerceIn(0, 4)
                    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(5.dp),
                            verticalAlignment = Alignment.Bottom
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(2.dp),
                                verticalAlignment = Alignment.Bottom
                            ) {
                                listOf(5.dp, 8.dp, 11.dp, 14.dp).forEachIndexed { idx, h ->
                                    val barLit = idx < level
                                    Box(
                                        modifier = Modifier
                                            .width(3.dp)
                                            .height(h)
                                            .clip(RoundedCornerShape(2.dp))
                                            .background(
                                                if (barLit) simAccent else simAccent.copy(alpha = 0.22f)
                                            )
                                    )
                                }
                            }
                            Text(
                                text = "$level/4".localizeDigits(strings.isPersian),
                                style = MonoTechnicalStyle.copy(fontSize = 11.sp),
                                color = if (level > 0) simAccent else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun IncomingCallOverlay(
    banner: IncomingCallBanner,
    onOpenVoiceTunnel: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = LocalAppStrings.current
    val emeraldAccent = MaterialTheme.colorScheme.tertiary
    AnimatedVisibility(
        visible = banner.isActive,
        enter = slideInVertically { -it } + fadeIn(),
        exit = slideOutVertically { -it } + fadeOut(),
        modifier = modifier
    ) {
        GlassBridgeCard(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("incoming_call_overlay"),
            accentColor = emeraldAccent,
            accentStrength = 0.8f,
            cornerRadius = 22.dp
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                emeraldAccent.copy(alpha = 0.22f),
                                Color.Transparent
                            )
                        )
                    )
                    .padding(16.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(emeraldAccent.copy(alpha = 0.22f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.PhoneInTalk,
                                contentDescription = null,
                                tint = emeraldAccent,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (banner.isRinging) strings.incomingCallOnHost else strings.hostCallActive,
                                style = MaterialTheme.typography.titleMedium,
                                color = emeraldAccent,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${banner.phoneNumberHint} • ${strings.simSlotLabel} ${banner.simSlot}".localizeDigits(strings.isPersian),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = onOpenVoiceTunnel,
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("incoming_open_voice_button"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = emeraldAccent,
                                contentColor = MaterialTheme.colorScheme.onTertiary
                            ),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Text(strings.openVoiceBridge, fontWeight = FontWeight.Bold)
                        }
                        OutlinedButton(
                            onClick = onDismiss,
                            modifier = Modifier
                                .height(48.dp)
                                .testTag("incoming_dismiss_button"),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Text(strings.dismiss)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun VoiceTunnelActiveBar(
    voiceState: VoiceTunnelState,
    onToggleMute: () -> Unit,
    onToggleSpeaker: () -> Unit,
    onHangUp: () -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = LocalAppStrings.current
    val primaryAccent = MaterialTheme.colorScheme.primary
    val emeraldAccent = MaterialTheme.colorScheme.tertiary
    val amberAccent = MaterialTheme.colorScheme.secondary
    val errorAccent = MaterialTheme.colorScheme.error
    AnimatedVisibility(
        visible = voiceState.isActive,
        enter = fadeIn() + slideInVertically { -it / 2 },
        exit = fadeOut() + slideOutVertically { -it / 2 },
        modifier = modifier
    ) {
        GlassBridgeCard(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("voice_tunnel_active_bar"),
            accentColor = primaryAccent,
            accentStrength = 0.72f,
            cornerRadius = 22.dp
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(primaryAccent.copy(alpha = 0.16f), Color.Transparent)
                        )
                    )
                    .padding(16.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Filled.GraphicEq, contentDescription = null, tint = primaryAccent)
                            Text(
                                text = strings.voiceTunnelActive,
                                style = MaterialTheme.typography.titleSmall,
                                color = primaryAccent
                            )
                        }
                        val mins = voiceState.durationSeconds / 60
                        val secs = voiceState.durationSeconds % 60
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = primaryAccent.copy(alpha = 0.14f)
                        ) {
                            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                                Text(
                                    text = "%02d:%02d".format(mins, secs).localizeDigits(strings.isPersian),
                                    style = MonoTechnicalStyle,
                                    color = primaryAccent,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }
                    }

                    // 13-bar live symmetric audio waveform visualizer
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(38.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val level = maxOf(voiceState.localAmplitude, voiceState.remoteAmplitude, 0.12f)
                        val weights = listOf(0.28f, 0.52f, 0.75f, 0.92f, 0.68f, 1f, 0.82f, 1f, 0.68f, 0.92f, 0.75f, 0.52f, 0.28f)
                        weights.forEach { w ->
                            val barHeight = (6.dp + (30.dp * (level * w).coerceIn(0.10f, 1f)))
                            Box(
                                modifier = Modifier
                                    .width(5.dp)
                                    .height(barHeight)
                                    .clip(RoundedCornerShape(50))
                                    .background(
                                        Brush.verticalGradient(listOf(primaryAccent, emeraldAccent))
                                    )
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        FilledTonalIconButton(
                            onClick = onToggleMute,
                            modifier = Modifier
                                .size(50.dp)
                                .testTag("voice_mute_button"),
                            colors = IconButtonDefaults.filledTonalIconButtonColors(
                                containerColor = if (voiceState.isMuted) amberAccent.copy(alpha = 0.25f)
                                else MaterialTheme.colorScheme.surfaceContainerHigh
                            )
                        ) {
                            Icon(
                                imageVector = if (voiceState.isMuted) Icons.Filled.MicOff else Icons.Filled.Mic,
                                contentDescription = if (voiceState.isMuted) strings.unmute else strings.mute,
                                tint = if (voiceState.isMuted) amberAccent else MaterialTheme.colorScheme.onSurface
                            )
                        }

                        FilledTonalIconButton(
                            onClick = onToggleSpeaker,
                            modifier = Modifier
                                .size(50.dp)
                                .testTag("voice_speaker_button"),
                            colors = IconButtonDefaults.filledTonalIconButtonColors(
                                containerColor = if (voiceState.isSpeakerOn) primaryAccent.copy(alpha = 0.24f)
                                else MaterialTheme.colorScheme.surfaceContainerHigh
                            )
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                                contentDescription = strings.speaker,
                                tint = if (voiceState.isSpeakerOn) primaryAccent else MaterialTheme.colorScheme.onSurface
                            )
                        }

                        FilledTonalIconButton(
                            onClick = onHangUp,
                            modifier = Modifier
                                .size(50.dp)
                                .testTag("voice_hangup_button"),
                            colors = IconButtonDefaults.filledTonalIconButtonColors(
                                containerColor = errorAccent
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Filled.CallEnd,
                                contentDescription = strings.stopVoiceTunnel,
                                tint = MaterialTheme.colorScheme.onError
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun OtpCodeChip(
    otpCode: String,
    onCopy: () -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = LocalAppStrings.current
    val haptic = LocalHapticFeedback.current
    var copiedRecently by remember { mutableStateOf(false) }

    LaunchedEffect(copiedRecently) {
        if (copiedRecently) {
            delay(2000L)
            copiedRecently = false
        }
    }

    val chipAccent by animateColorAsState(
        targetValue = if (copiedRecently) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary,
        animationSpec = tween(220),
        label = "otpChipAccent"
    )

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                copiedRecently = true
                onCopy()
            }
            .testTag("otp_copy_chip_$otpCode"),
        shape = RoundedCornerShape(16.dp),
        color = chipAccent.copy(alpha = 0.14f),
        border = BorderStroke(1.2.dp, chipAccent.copy(alpha = 0.65f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = strings.otpDetected,
                    style = MaterialTheme.typography.labelSmall,
                    color = chipAccent.copy(alpha = 0.85f)
                )
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                    Text(
                        text = otpCode.chunked(3).joinToString("  "),
                        style = MonoTechnicalStyle.copy(
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 2.5.sp
                        ),
                        color = chipAccent
                    )
                }
            }
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = chipAccent.copy(alpha = 0.22f)
            ) {
                AnimatedContent(
                    targetState = copiedRecently,
                    transitionSpec = { fadeIn(tween(180)) togetherWith fadeOut(tween(150)) },
                    label = "otpCopyState"
                ) { isCopied ->
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = if (isCopied) Icons.Filled.Check else Icons.Filled.ContentCopy,
                            contentDescription = strings.copyOtp,
                            tint = chipAccent,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = if (isCopied) strings.copied else strings.copyOtp,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = chipAccent
                        )
                    }
                }
            }
        }
    }
}
