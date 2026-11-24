package com.example.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.i18n.LocalAppStrings
import com.example.ui.i18n.localizeDigits
import com.example.ui.theme.LocalIsDarkTheme
import com.example.ui.theme.MonoTechnicalStyle
import kotlin.math.PI
import kotlin.math.sin
import kotlinx.coroutines.launch

private data class OnboardingPageData(
    val badge: String,
    val title: String,
    val description: String,
    val accent: Color
)

@Composable
fun AnimatedOnboardingGuide(
    isDarkTheme: Boolean,
    onToggleTheme: () -> Unit,
    onToggleLanguage: () -> Unit,
    onComplete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = LocalAppStrings.current
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val colors = MaterialTheme.colorScheme

    val pages = listOf(
        OnboardingPageData(
            badge = strings.onboardingBadge1,
            title = strings.onboardingTitle1,
            description = strings.onboardingDesc1,
            accent = colors.primary
        ),
        OnboardingPageData(
            badge = strings.onboardingBadge2,
            title = strings.onboardingTitle2,
            description = strings.onboardingDesc2,
            accent = colors.tertiary
        ),
        OnboardingPageData(
            badge = strings.onboardingBadge3,
            title = strings.onboardingTitle3,
            description = strings.onboardingDesc3,
            accent = colors.secondary
        ),
        OnboardingPageData(
            badge = strings.onboardingBadge4,
            title = strings.onboardingTitle4,
            description = strings.onboardingDesc4,
            accent = colors.tertiary
        )
    )

    val pagerState = rememberPagerState(initialPage = 0, pageCount = { pages.size })
    val isLastPage = pagerState.currentPage == pages.lastIndex
    val activeAccent by animateColorAsState(
        targetValue = pages[pagerState.currentPage].accent,
        animationSpec = tween(360),
        label = "onboardingAccent"
    )

    BackHandler(enabled = true) {
        if (pagerState.currentPage > 0) {
            scope.launch { pagerState.animateScrollToPage(pagerState.currentPage - 1) }
        } else {
            onComplete()
        }
    }

    AtmosphericCanvasBox(
        modifier = modifier
            .fillMaxSize()
            .testTag("animated_onboarding_guide")
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top Utility Row: Quick Theme Toggle, Language Toggle, Skip
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 600.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .clickable {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onToggleTheme()
                            }
                            .testTag("onboarding_theme_toggle"),
                        shape = RoundedCornerShape(14.dp),
                        color = colors.surfaceContainerHigh,
                        border = BorderStroke(1.dp, activeAccent.copy(alpha = 0.35f))
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            AnimatedContent(
                                targetState = isDarkTheme,
                                transitionSpec = {
                                    (fadeIn(tween(220)) + scaleIn(initialScale = 0.8f)) togetherWith
                                        (fadeOut(tween(180)) + scaleOut(targetScale = 0.8f))
                                },
                                label = "onboardingThemeIcon"
                            ) { dark ->
                                Icon(
                                    imageVector = if (dark) Icons.Filled.LightMode else Icons.Filled.DarkMode,
                                    contentDescription = strings.themeModeTitle,
                                    tint = if (dark) colors.secondary else colors.primary,
                                    modifier = Modifier.size(21.dp)
                                )
                            }
                        }
                    }

                    Surface(
                        modifier = Modifier
                            .height(48.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .clickable {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onToggleLanguage()
                            }
                            .testTag("onboarding_lang_toggle"),
                        shape = RoundedCornerShape(14.dp),
                        color = colors.surfaceContainerHigh,
                        border = BorderStroke(1.dp, colors.primary.copy(alpha = 0.35f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Language,
                                contentDescription = strings.languageLabel,
                                tint = colors.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = if (strings.isPersian) "EN" else "فا",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = colors.onSurface
                            )
                        }
                    }
                }

                TextButton(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onComplete()
                    },
                    modifier = Modifier
                        .height(48.dp)
                        .testTag("onboarding_skip_button")
                ) {
                    Text(
                        text = strings.onboardingSkip,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.onSurfaceVariant
                    )
                }
            }

            // Center Interactive Animated HorizontalPager
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .widthIn(max = 600.dp)
            ) { pageIndex ->
                val page = pages[pageIndex]
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(vertical = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    // Bespoke Animated Hero Card
                    GlassBridgeCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(260.dp),
                        accentColor = page.accent,
                        accentStrength = 0.52f,
                        cornerRadius = 28.dp
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.radialGradient(
                                        colors = listOf(
                                            page.accent.copy(alpha = if (LocalIsDarkTheme.current) 0.20f else 0.12f),
                                            Color.Transparent
                                        )
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            when (pageIndex) {
                                0 -> DualPhoneBridgeStage(accent = page.accent)
                                1 -> QrPairingStage(accent = page.accent)
                                2 -> LiveSignalAndDefaultAppsStage(accent = page.accent)
                                else -> BatteryTrendAndPrivacyStage(accent = page.accent)
                            }
                        }
                    }

                    Spacer(Modifier.height(26.dp))

                    // Step Pill Badge
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = page.accent.copy(alpha = if (LocalIsDarkTheme.current) 0.16f else 0.12f),
                        border = BorderStroke(1.dp, page.accent.copy(alpha = 0.42f))
                    ) {
                        Text(
                            text = page.badge,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = page.accent,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                        )
                    }

                    Spacer(Modifier.height(14.dp))

                    Text(
                        text = page.title,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = colors.onBackground,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )

                    Spacer(Modifier.height(10.dp))

                    Text(
                        text = page.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 10.dp)
                    )
                }
            }

            // Bottom Controls: Expanding Pill Indicators + Back / Next / Get Started Buttons
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 600.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                // Expanding Pill Indicators
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    repeat(pages.size) { index ->
                        val selected = pagerState.currentPage == index
                        val pillWidth by animateDpAsState(
                            targetValue = if (selected) 30.dp else 9.dp,
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                stiffness = Spring.StiffnessMediumLow
                            ),
                            label = "indicatorWidth"
                        )
                        val pillColor by animateColorAsState(
                            targetValue = if (selected) activeAccent else colors.outline.copy(alpha = 0.45f),
                            animationSpec = tween(250),
                            label = "indicatorColor"
                        )
                        Box(
                            modifier = Modifier
                                .height(9.dp)
                                .width(pillWidth)
                                .clip(CircleShape)
                                .background(pillColor)
                                .clickable {
                                    scope.launch { pagerState.animateScrollToPage(index) }
                                }
                        )
                    }
                }

                // Navigation Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AnimatedVisibility(visible = pagerState.currentPage > 0) {
                        OutlinedButton(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                scope.launch {
                                    pagerState.animateScrollToPage((pagerState.currentPage - 1).coerceAtLeast(0))
                                }
                            },
                            modifier = Modifier
                                .height(54.dp)
                                .testTag("onboarding_back_button"),
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(1.dp, colors.outline)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = strings.onboardingBack,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(strings.onboardingBack, fontWeight = FontWeight.SemiBold)
                        }
                    }

                    Button(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            if (isLastPage) {
                                onComplete()
                            } else {
                                scope.launch {
                                    pagerState.animateScrollToPage(pagerState.currentPage + 1)
                                }
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(54.dp)
                            .testTag(if (isLastPage) "onboarding_start_button" else "onboarding_next_button"),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = activeAccent,
                            contentColor = if (activeAccent == colors.tertiary) colors.onTertiary
                            else if (activeAccent == colors.secondary) colors.onSecondary
                            else colors.onPrimary
                        )
                    ) {
                        Text(
                            text = if (isLastPage) strings.onboardingGetStarted else strings.onboardingNext,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.width(8.dp))
                        Icon(
                            imageVector = if (isLastPage) Icons.Filled.CheckCircle else Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Slide 1 Animation: Two Android phones connected via an encrypted glowing radio arc
 * with orbiting AES-256 packets and concentric radar ripples.
 */
@Composable
private fun DualPhoneBridgeStage(accent: Color) {
    val tertiary = MaterialTheme.colorScheme.tertiary
    val surfaceHigh = MaterialTheme.colorScheme.surfaceContainerHigh
    val onSurface = MaterialTheme.colorScheme.onSurface
    val infinite = rememberInfiniteTransition(label = "bridgeStage")

    val packetT by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1800, easing = LinearEasing), RepeatMode.Restart),
        label = "packetT"
    )
    val ripple by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(2200, easing = FastOutSlowInEasing), RepeatMode.Restart),
        label = "ripple"
    )
    val floatPhase by infinite.animateFloat(
        initialValue = 0f,
        targetValue = (2f * PI).toFloat(),
        animationSpec = infiniteRepeatable(tween(2800, easing = LinearEasing), RepeatMode.Restart),
        label = "floatPhase"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val leftCenter = Offset(size.width * 0.22f, size.height * 0.54f)
            val rightCenter = Offset(size.width * 0.78f, size.height * 0.54f)
            val ctrl = Offset(size.width * 0.5f, size.height * 0.14f)

            // Concentric ripples around both phones
            val maxRadius = 62.dp.toPx()
            val curRadius = 26.dp.toPx() + (maxRadius - 26.dp.toPx()) * ripple
            val alpha = (1f - ripple) * 0.42f
            drawCircle(accent.copy(alpha = alpha), curRadius, leftCenter, style = Stroke(2.5.dp.toPx()))
            drawCircle(tertiary.copy(alpha = alpha), curRadius, rightCenter, style = Stroke(2.5.dp.toPx()))

            // Curved bridge arc
            val arcPath = Path().apply {
                moveTo(leftCenter.x, leftCenter.y)
                quadraticTo(ctrl.x, ctrl.y, rightCenter.x, rightCenter.y)
            }
            drawPath(
                path = arcPath,
                brush = Brush.horizontalGradient(listOf(accent, tertiary)),
                style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round)
            )

            // Two luminous packets traveling smoothly along the arc
            listOf(packetT, (packetT + 0.5f) % 1f).forEach { t ->
                val inv = 1f - t
                val px = inv * inv * leftCenter.x + 2 * inv * t * ctrl.x + t * t * rightCenter.x
                val py = inv * inv * leftCenter.y + 2 * inv * t * ctrl.y + t * t * rightCenter.y
                drawCircle(accent.copy(alpha = 0.28f), 14.dp.toPx(), Offset(px, py))
                drawCircle(tertiary, 5.5.dp.toPx(), Offset(px, py))
                drawCircle(Color.White, 2.5.dp.toPx(), Offset(px, py))
            }

            // Stylized phone bodies
            val phoneW = 52.dp.toPx()
            val phoneH = 88.dp.toPx()
            listOf(leftCenter to accent, rightCenter to tertiary).forEach { (center, borderTint) ->
                val topLeft = Offset(center.x - phoneW / 2f, center.y - phoneH / 2f)
                drawRoundRect(
                    color = surfaceHigh,
                    topLeft = topLeft,
                    size = Size(phoneW, phoneH),
                    cornerRadius = CornerRadius(14.dp.toPx())
                )
                drawRoundRect(
                    color = borderTint,
                    topLeft = topLeft,
                    size = Size(phoneW, phoneH),
                    cornerRadius = CornerRadius(14.dp.toPx()),
                    style = Stroke(2.5.dp.toPx())
                )
                // Inner screen bars
                drawRoundRect(
                    color = borderTint.copy(alpha = 0.22f),
                    topLeft = Offset(topLeft.x + 8.dp.toPx(), topLeft.y + 14.dp.toPx()),
                    size = Size(phoneW - 16.dp.toPx(), 10.dp.toPx()),
                    cornerRadius = CornerRadius(4.dp.toPx())
                )
                drawRoundRect(
                    color = onSurface.copy(alpha = 0.14f),
                    topLeft = Offset(topLeft.x + 8.dp.toPx(), topLeft.y + 30.dp.toPx()),
                    size = Size(phoneW - 22.dp.toPx(), 8.dp.toPx()),
                    cornerRadius = CornerRadius(4.dp.toPx())
                )
            }
        }

        // Floating AES-256 Lock Badge above the arc
        val floatY = sin(floatPhase) * 6f
        Surface(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 6.dp)
                .graphicsLayer { translationY = floatY },
            shape = RoundedCornerShape(50),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.2.dp, accent.copy(alpha = 0.55f)),
            shadowElevation = 4.dp
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(Icons.Filled.Lock, contentDescription = null, tint = accent, modifier = Modifier.size(15.dp))
                Text(
                    text = "AES-256-GCM",
                    style = MonoTechnicalStyle,
                    color = accent,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

/**
 * Slide 2 Animation: Breathing QR viewfinder brackets with a sweeping glowing laser scanner
 * and floating local Wi-Fi / Bluetooth badges.
 */
@Composable
private fun QrPairingStage(accent: Color) {
    val primary = MaterialTheme.colorScheme.primary
    val onSurface = MaterialTheme.colorScheme.onSurface
    val infinite = rememberInfiniteTransition(label = "qrStage")

    val laserY by infinite.animateFloat(
        initialValue = 0.15f,
        targetValue = 0.85f,
        animationSpec = infiniteRepeatable(tween(1500, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "laserY"
    )
    val breathe by infinite.animateFloat(
        initialValue = 0.96f,
        targetValue = 1.04f,
        animationSpec = infiniteRepeatable(tween(1400, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "breathe"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        contentAlignment = Alignment.Center
    ) {
        // Floating transport badges
        Surface(
            modifier = Modifier
                .align(Alignment.TopStart)
                .graphicsLayer { translationY = (breathe - 1f) * 80f },
            shape = RoundedCornerShape(14.dp),
            color = primary.copy(alpha = 0.15f),
            border = BorderStroke(1.dp, primary.copy(alpha = 0.45f))
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                Icon(Icons.Filled.Wifi, contentDescription = null, tint = primary, modifier = Modifier.size(16.dp))
                Text("Wi-Fi", style = MonoTechnicalStyle, color = primary, fontWeight = FontWeight.Bold)
            }
        }

        Surface(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .graphicsLayer { translationY = (1f - breathe) * 80f },
            shape = RoundedCornerShape(14.dp),
            color = accent.copy(alpha = 0.15f),
            border = BorderStroke(1.dp, accent.copy(alpha = 0.45f))
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                Icon(Icons.Filled.Bluetooth, contentDescription = null, tint = accent, modifier = Modifier.size(16.dp))
                Text("BT", style = MonoTechnicalStyle, color = accent, fontWeight = FontWeight.Bold)
            }
        }

        // Animated QR Matrix + Sweeping Laser
        Box(
            modifier = Modifier
                .size(164.dp)
                .graphicsLayer {
                    scaleX = breathe
                    scaleY = breathe
                },
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val pad = 16.dp.toPx()
                val qrSize = size.minDimension - pad * 2f
                val cell = qrSize / 7f

                // Stylized 7x7 QR pattern
                val pattern = listOf(
                    "1110111",
                    "1010101",
                    "1110111",
                    "0001010",
                    "1101101",
                    "1010011",
                    "1111010"
                )
                pattern.forEachIndexed { row, line ->
                    line.forEachIndexed { col, ch ->
                        if (ch == '1') {
                            drawRoundRect(
                                color = onSurface.copy(alpha = 0.82f),
                                topLeft = Offset(pad + col * cell + 2f, pad + row * cell + 2f),
                                size = Size(cell - 4f, cell - 4f),
                                cornerRadius = CornerRadius(3.dp.toPx())
                            )
                        }
                    }
                }

                // 4 Corner Brackets
                val corner = 26.dp.toPx()
                val stroke = 3.8.dp.toPx()
                val w = size.width
                val h = size.height
                drawLine(accent, Offset(0f, 0f), Offset(corner, 0f), stroke, StrokeCap.Round)
                drawLine(accent, Offset(0f, 0f), Offset(0f, corner), stroke, StrokeCap.Round)

                drawLine(accent, Offset(w, 0f), Offset(w - corner, 0f), stroke, StrokeCap.Round)
                drawLine(accent, Offset(w, 0f), Offset(w, corner), stroke, StrokeCap.Round)

                drawLine(accent, Offset(0f, h), Offset(corner, h), stroke, StrokeCap.Round)
                drawLine(accent, Offset(0f, h), Offset(0f, h - corner), stroke, StrokeCap.Round)

                drawLine(accent, Offset(w, h), Offset(w - corner, h), stroke, StrokeCap.Round)
                drawLine(accent, Offset(w, h), Offset(w, h - corner), stroke, StrokeCap.Round)

                // Sweeping glowing laser beam
                val beamY = size.height * laserY
                drawLine(
                    brush = Brush.horizontalGradient(
                        listOf(Color.Transparent, primary, accent, primary, Color.Transparent)
                    ),
                    start = Offset(6.dp.toPx(), beamY),
                    end = Offset(w - 6.dp.toPx(), beamY),
                    strokeWidth = 3.5.dp.toPx(),
                    cap = StrokeCap.Round
                )
            }
        }
    }
}

/**
 * Slide 3 Animation: Live status-bar signal wave (always LTR ascending), dynamic battery fill,
 * default Phone app pill, and animated 1-tap OTP code chip.
 */
@Composable
private fun LiveSignalAndDefaultAppsStage(accent: Color) {
    val strings = LocalAppStrings.current
    val primary = MaterialTheme.colorScheme.primary
    val tertiary = MaterialTheme.colorScheme.tertiary
    val infinite = rememberInfiniteTransition(label = "signalStage")

    val signalPhase by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 4f,
        animationSpec = infiniteRepeatable(tween(2000, easing = LinearEasing), RepeatMode.Restart),
        label = "signalPhase"
    )
    val batterySweep by infinite.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.94f,
        animationSpec = infiniteRepeatable(tween(2400, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "batterySweep"
    )

    val litBars = (signalPhase.toInt() % 4) + 1
    val batteryPct = (batterySweep * 100).toInt()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Live Status Notification Preview Card
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.2.dp, primary.copy(alpha = 0.42f)),
            shadowElevation = 4.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(primary.copy(alpha = 0.16f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Filled.PhoneAndroid, contentDescription = null, tint = primary, modifier = Modifier.size(20.dp))
                    }
                    Column {
                        Text(
                            text = "SIM 1 • 5G / LTE",
                            style = MonoTechnicalStyle,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "SIM 2 • ${batteryPct}%".localizeDigits(strings.isPersian),
                            style = MaterialTheme.typography.labelSmall,
                            color = tertiary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                // Ascending 4-bar animated signal meter (always LTR ascending)
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.Bottom
                    ) {
                        listOf(8.dp, 13.dp, 18.dp, 24.dp).forEachIndexed { idx, h ->
                            val active = idx < litBars
                            Box(
                                modifier = Modifier
                                    .width(5.dp)
                                    .height(h)
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(if (active) primary else primary.copy(alpha = 0.22f))
                            )
                        }
                    }
                }
            }
        }

        // Dynamic Charging Gauge + Default Phone Badge
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Surface(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(16.dp),
                color = tertiary.copy(alpha = 0.14f),
                border = BorderStroke(1.dp, tertiary.copy(alpha = 0.42f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Filled.BatteryChargingFull, contentDescription = null, tint = tertiary, modifier = Modifier.size(20.dp))
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(8.dp)
                            .clip(CircleShape)
                            .background(tertiary.copy(alpha = 0.22f))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(batterySweep)
                                .height(8.dp)
                                .clip(CircleShape)
                                .background(tertiary)
                        )
                    }
                }
            }

            Surface(
                shape = RoundedCornerShape(16.dp),
                color = accent.copy(alpha = 0.15f),
                border = BorderStroke(1.dp, accent.copy(alpha = 0.45f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(Icons.Filled.Call, contentDescription = null, tint = accent, modifier = Modifier.size(18.dp))
                    Text(
                        text = "Telecom",
                        style = MonoTechnicalStyle,
                        color = accent,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Animated OTP Verification Pill
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            color = primary.copy(alpha = 0.12f),
            border = BorderStroke(1.dp, primary.copy(alpha = 0.38f))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Filled.Key, contentDescription = null, tint = primary, modifier = Modifier.size(18.dp))
                    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                        Text(
                            text = "482  910",
                            style = MonoTechnicalStyle.copy(fontSize = 16.sp, letterSpacing = 1.8.sp),
                            color = primary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                Icon(
                    imageVector = Icons.Filled.CheckCircle,
                    contentDescription = null,
                    tint = tertiary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

/**
 * Slide 4 Animation: Live animated cubic battery curve with area gradient fill,
 * pulsing telemetry beacon, and Play Creator privacy shield badge.
 */
@Composable
private fun BatteryTrendAndPrivacyStage(accent: Color) {
    val primary = MaterialTheme.colorScheme.primary
    val infinite = rememberInfiniteTransition(label = "chartStage")

    val drawProgress by infinite.animateFloat(
        initialValue = 0.18f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(2400, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "chartProgress"
    )
    val beaconPulse by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1400, easing = FastOutSlowInEasing), RepeatMode.Restart),
        label = "beaconPulse"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp)
    ) {
        // Privacy Shield Badge at Top
        Surface(
            modifier = Modifier.align(Alignment.TopCenter),
            shape = RoundedCornerShape(50),
            color = accent.copy(alpha = 0.16f),
            border = BorderStroke(1.dp, accent.copy(alpha = 0.48f))
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(Icons.Filled.Security, contentDescription = null, tint = accent, modifier = Modifier.size(16.dp))
                Text(
                    text = "Zero Cloud • 100% P2P",
                    style = MonoTechnicalStyle,
                    color = accent,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Animated Battery Trend Curve
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 42.dp, bottom = 10.dp, start = 8.dp, end = 8.dp)
        ) {
            val w = size.width
            val h = size.height

            // Subtle horizontal grid lines
            listOf(0.2f, 0.5f, 0.8f).forEach { frac ->
                drawLine(
                    color = accent.copy(alpha = 0.16f),
                    start = Offset(0f, h * frac),
                    end = Offset(w, h * frac),
                    strokeWidth = 1.dp.toPx()
                )
            }

            // Sample points along normalized trend curve
            val rawPoints = listOf(
                Offset(0.00f, 0.72f),
                Offset(0.18f, 0.64f),
                Offset(0.36f, 0.70f),
                Offset(0.54f, 0.48f),
                Offset(0.74f, 0.34f),
                Offset(1.00f, 0.22f)
            )

            val maxIdx = (drawProgress * (rawPoints.size - 1)).coerceIn(1f, (rawPoints.size - 1).toFloat())
            val fullCount = maxIdx.toInt()
            val frac = maxIdx - fullCount

            val activePoints = mutableListOf<Offset>()
            for (i in 0..fullCount) {
                activePoints.add(Offset(rawPoints[i].x * w, rawPoints[i].y * h))
            }
            if (fullCount < rawPoints.lastIndex && frac > 0.01f) {
                val p0 = rawPoints[fullCount]
                val p1 = rawPoints[fullCount + 1]
                activePoints.add(
                    Offset(
                        (p0.x + (p1.x - p0.x) * frac) * w,
                        (p0.y + (p1.y - p0.y) * frac) * h
                    )
                )
            }

            if (activePoints.size >= 2) {
                val linePath = Path().apply {
                    moveTo(activePoints.first().x, activePoints.first().y)
                    for (i in 1 until activePoints.size) {
                        val prev = activePoints[i - 1]
                        val curr = activePoints[i]
                        val midX = (prev.x + curr.x) / 2f
                        cubicTo(midX, prev.y, midX, curr.y, curr.x, curr.y)
                    }
                }
                val areaPath = Path().apply {
                    addPath(linePath)
                    lineTo(activePoints.last().x, h)
                    lineTo(activePoints.first().x, h)
                    close()
                }
                drawPath(
                    path = areaPath,
                    brush = Brush.verticalGradient(
                        colors = listOf(accent.copy(alpha = 0.32f), Color.Transparent)
                    )
                )
                drawPath(
                    path = linePath,
                    brush = Brush.horizontalGradient(listOf(primary, accent)),
                    style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round)
                )

                // Pulsing live beacon at tip
                val tip = activePoints.last()
                drawCircle(
                    color = accent.copy(alpha = (1f - beaconPulse) * 0.45f),
                    radius = 8.dp.toPx() + 14.dp.toPx() * beaconPulse,
                    center = tip
                )
                drawCircle(color = accent, radius = 6.dp.toPx(), center = tip)
                drawCircle(color = Color.White, radius = 2.8.dp.toPx(), center = tip)
            }
        }
    }
}
