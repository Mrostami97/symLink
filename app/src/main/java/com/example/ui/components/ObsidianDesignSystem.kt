package com.example.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.example.ui.theme.LocalIsDarkTheme

/**
 * Full-bleed atmospheric canvas drawn smoothly behind all screens, adapting to Light & Dark themes
 * and mirroring radial accents for RTL layouts.
 */
@Composable
fun AtmosphericCanvasBox(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    val isDark = LocalIsDarkTheme.current
    val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    val baseBg = MaterialTheme.colorScheme.background
    val primaryGlow = MaterialTheme.colorScheme.primary
    val tertiaryGlow = MaterialTheme.colorScheme.tertiary
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(baseBg)
            .drawBehind {
                val primaryAlpha = if (isDark) 0.11f else 0.09f
                val tertiaryAlpha = if (isDark) 0.08f else 0.07f
                val startGlowX = if (isRtl) size.width * 0.82f else size.width * 0.18f
                val endGlowX = if (isRtl) size.width * 0.12f else size.width * 0.88f
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            primaryGlow.copy(alpha = primaryAlpha),
                            Color.Transparent
                        ),
                        center = Offset(startGlowX, size.height * 0.06f),
                        radius = size.maxDimension * 0.58f
                    )
                )
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            tertiaryGlow.copy(alpha = tertiaryAlpha),
                            Color.Transparent
                        ),
                        center = Offset(endGlowX, size.height * 0.24f),
                        radius = size.maxDimension * 0.50f
                    )
                )
            },
        content = content
    )
}

/**
 * Layered Material 3 card with specular gradient hairline border, crisp light-mode elevation,
 * and optional tactile press bounce.
 */
@Composable
fun GlassBridgeCard(
    modifier: Modifier = Modifier,
    accentColor: Color = MaterialTheme.colorScheme.primary,
    accentStrength: Float = 0.28f,
    cornerRadius: Dp = 22.dp,
    onClick: (() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit
) {
    val isDark = LocalIsDarkTheme.current
    val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    val haptic = LocalHapticFeedback.current
    val shape: Shape = RoundedCornerShape(cornerRadius)
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val pressScale by animateFloatAsState(
        targetValue = if (onClick != null && isPressed) 0.97f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow, dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "glassCardScale"
    )

    val borderColors = if (isDark) {
        listOf(
            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f),
            accentColor.copy(alpha = accentStrength),
            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)
        )
    } else {
        listOf(
            accentColor.copy(alpha = (accentStrength * 0.95f).coerceIn(0.22f, 0.65f)),
            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.85f),
            accentColor.copy(alpha = (accentStrength * 0.55f).coerceIn(0.16f, 0.45f))
        )
    }
    val borderBrush = Brush.linearGradient(
        colors = if (isRtl) borderColors.reversed() else borderColors
    )

    Surface(
        modifier = modifier
            .scale(pressScale)
            .clip(shape)
            .then(
                if (onClick != null) {
                    Modifier.clickable(
                        interactionSource = interactionSource,
                        indication = androidx.compose.material3.ripple(color = accentColor),
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onClick()
                        }
                    )
                } else {
                    Modifier
                }
            ),
        shape = shape,
        color = if (isDark) MaterialTheme.colorScheme.surface.copy(alpha = 0.94f) else MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, borderBrush),
        tonalElevation = if (isDark) 2.dp else 0.dp,
        shadowElevation = if (isDark) 0.dp else 3.dp
    ) {
        Box(content = content)
    }
}

/**
 * Generated studio artwork banner with adaptive Light/Dark surface fade for WCAG-compliant text legibility.
 */
@Composable
fun HeroArtworkBanner(
    @DrawableRes drawableRes: Int,
    contentDescription: String,
    height: Dp = 172.dp,
    modifier: Modifier = Modifier,
    overlayContent: @Composable BoxScope.() -> Unit = {}
) {
    val isDark = LocalIsDarkTheme.current
    val scrimBase = MaterialTheme.colorScheme.scrim
    val bgFade = MaterialTheme.colorScheme.surface
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clip(RoundedCornerShape(24.dp))
    ) {
        Image(
            painter = painterResource(id = drawableRes),
            contentDescription = contentDescription,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    if (isDark) {
                        Brush.verticalGradient(
                            colors = listOf(
                                scrimBase.copy(alpha = 0.16f),
                                scrimBase.copy(alpha = 0.50f),
                                bgFade.copy(alpha = 0.96f)
                            )
                        )
                    } else {
                        Brush.verticalGradient(
                            colorStops = arrayOf(
                                0.0f to scrimBase.copy(alpha = 0.10f),
                                0.36f to bgFade.copy(alpha = 0.62f),
                                0.72f to bgFade.copy(alpha = 0.94f),
                                1.0f to bgFade.copy(alpha = 0.99f)
                            )
                        )
                    }
                )
        )
        overlayContent()
    }
}

data class FloatingNavItem(
    val route: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
)

/**
 * Floating glass capsule bottom navigation bar that strictly respects WindowInsets.navigationBars.
 */
@Composable
fun FloatingGlassNavBar(
    items: List<FloatingNavItem>,
    currentRoute: String,
    onSelectRoute: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = LocalIsDarkTheme.current
    val haptic = LocalHapticFeedback.current

    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 14.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(
                    elevation = if (isDark) 18.dp else 10.dp,
                    shape = RoundedCornerShape(28.dp),
                    ambientColor = MaterialTheme.colorScheme.primary.copy(alpha = if (isDark) 0.18f else 0.12f),
                    spotColor = MaterialTheme.colorScheme.scrim.copy(alpha = if (isDark) 0.65f else 0.22f)
                ),
            shape = RoundedCornerShape(28.dp),
            color = if (isDark) {
                MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = 0.95f)
            } else {
                MaterialTheme.colorScheme.surface.copy(alpha = 0.97f)
            },
            border = BorderStroke(
                width = 1.dp,
                brush = Brush.linearGradient(
                    colors = if (isDark) {
                        listOf(
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.16f),
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.30f),
                            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                        )
                    } else {
                        listOf(
                            MaterialTheme.colorScheme.outlineVariant,
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.32f),
                            MaterialTheme.colorScheme.outlineVariant
                        )
                    }
                )
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 7.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                items.forEach { item ->
                    val selected = currentRoute == item.route
                    val pillBg by animateColorAsState(
                        targetValue = if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.17f)
                        else Color.Transparent,
                        animationSpec = tween(200),
                        label = "navPillBg"
                    )
                    val contentTint by animateColorAsState(
                        targetValue = if (selected) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                        animationSpec = tween(200),
                        label = "navTint"
                    )
                    val itemScale by animateFloatAsState(
                        targetValue = if (selected) 1.03f else 1f,
                        animationSpec = spring(stiffness = Spring.StiffnessMedium),
                        label = "navScale"
                    )

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .scale(itemScale)
                            .clip(RoundedCornerShape(20.dp))
                            .background(pillBg)
                            .clickable {
                                if (!selected) {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    onSelectRoute(item.route)
                                }
                            }
                            .padding(vertical = 8.dp, horizontal = 4.dp)
                            .testTag("nav_bar_${item.route}"),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Icon(
                            imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                            contentDescription = item.label,
                            tint = contentTint,
                            modifier = Modifier.size(22.dp)
                        )
                        Text(
                            text = item.label,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                            color = contentTint,
                            textAlign = TextAlign.Center,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}
