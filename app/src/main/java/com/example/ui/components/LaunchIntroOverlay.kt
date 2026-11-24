package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.ui.i18n.LocalAppStrings
import com.example.ui.theme.LocalIsDarkTheme
import kotlinx.coroutines.delay

@Composable
fun LaunchIntroOverlay(
    visible: Boolean,
    onFinished: () -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = LocalAppStrings.current
    var started by remember { mutableStateOf(false) }
    var progressTarget by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(visible) {
        if (visible) {
            started = true
            progressTarget = 1f
            delay(2600L)
            onFinished()
        }
    }

    val logoScale by animateFloatAsState(
        targetValue = if (started) 1f else 0.78f,
        animationSpec = spring(dampingRatio = 0.65f, stiffness = 220f),
        label = "splashLogoScale"
    )
    val contentAlpha by animateFloatAsState(
        targetValue = if (started) 1f else 0f,
        animationSpec = tween(650, easing = FastOutSlowInEasing),
        label = "splashContentAlpha"
    )
    val animatedProgress by animateFloatAsState(
        targetValue = progressTarget,
        animationSpec = tween(2400, easing = LinearEasing),
        label = "splashProgress"
    )

    val infinite = rememberInfiniteTransition(label = "splashHalo")
    val pulse by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "splashHaloPulse"
    )

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(250)),
        exit = fadeOut(tween(420)) + scaleOut(targetScale = 1.04f, animationSpec = tween(420)),
        modifier = modifier
    ) {
        val isDark = LocalIsDarkTheme.current
        val bgColor = MaterialTheme.colorScheme.background
        val primaryAccent = MaterialTheme.colorScheme.primary
        val emeraldAccent = MaterialTheme.colorScheme.tertiary
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(bgColor)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onFinished
                )
                .testTag("launch_intro_overlay")
        ) {
            // Subtle atmospheric studio art background
            Image(
                painter = painterResource(id = R.drawable.img_bridge_hero_1795556747511),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .alpha(if (isDark) 0.28f else 0.12f)
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                bgColor.copy(alpha = if (isDark) 0.65f else 0.78f),
                                bgColor.copy(alpha = if (isDark) 0.85f else 0.90f),
                                bgColor.copy(alpha = 0.97f)
                            )
                        )
                    )
            )

            // Centerpiece brand emblem & creator credit
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .padding(horizontal = 28.dp, vertical = 32.dp)
                    .alpha(contentAlpha),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Top security pill
                Surface(
                    shape = RoundedCornerShape(50),
                    color = primaryAccent.copy(alpha = 0.12f),
                    border = BorderStroke(1.dp, primaryAccent.copy(alpha = 0.35f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.VerifiedUser,
                            contentDescription = null,
                            tint = primaryAccent,
                            modifier = Modifier.size(15.dp)
                        )
                        Text(
                            text = "AES-256-GCM P2P",
                            style = MaterialTheme.typography.labelSmall,
                            color = primaryAccent,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                // Emblem + Title + Prominent Creator Credit
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(18.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(144.dp)
                            .scale(logoScale),
                        contentAlignment = Alignment.Center
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val center = Offset(size.width / 2f, size.height / 2f)
                            val maxR = size.minDimension / 2f
                            drawCircle(
                                color = primaryAccent.copy(alpha = (1f - pulse) * 0.45f),
                                radius = maxR * (0.62f + 0.38f * pulse),
                                center = center,
                                style = Stroke(width = 3f)
                            )
                            drawCircle(
                                brush = Brush.radialGradient(
                                    colors = listOf(
                                        primaryAccent.copy(alpha = 0.25f),
                                        emeraldAccent.copy(alpha = 0.10f),
                                        Color.Transparent
                                    ),
                                    center = center,
                                    radius = maxR
                                ),
                                radius = maxR,
                                center = center
                            )
                        }
                        Surface(
                            modifier = Modifier.size(92.dp),
                            shape = RoundedCornerShape(28.dp),
                            color = MaterialTheme.colorScheme.surfaceContainerLow,
                            border = BorderStroke(
                                1.5.dp,
                                Brush.linearGradient(listOf(primaryAccent, emeraldAccent))
                            ),
                            shadowElevation = 14.dp
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.itell_icon_fg_1795555028207),
                                contentDescription = strings.appTitle,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(RoundedCornerShape(28.dp))
                            )
                        }
                    }

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = strings.appTitle,
                            style = MaterialTheme.typography.displaySmall,
                            color = MaterialTheme.colorScheme.onBackground,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = strings.appTagline,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }

                    // Prominent Creator Credit Glass Card
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
                        border = BorderStroke(
                            1.2.dp,
                            Brush.horizontalGradient(
                                colors = listOf(
                                    primaryAccent.copy(alpha = 0.60f),
                                    emeraldAccent.copy(alpha = 0.60f)
                                )
                            )
                        ),
                        shadowElevation = if (isDark) 0.dp else 4.dp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("launch_creator_credit")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 18.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(
                                        Brush.linearGradient(
                                            listOf(
                                                primaryAccent.copy(alpha = 0.20f),
                                                emeraldAccent.copy(alpha = 0.20f)
                                            )
                                        )
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.AutoAwesome,
                                    contentDescription = null,
                                    tint = primaryAccent,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(Modifier.width(10.dp))
                            Text(
                                text = strings.creatorCredit,
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }

                // Bottom sleek progress bar
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth(0.55f)
                ) {
                    LinearProgressIndicator(
                        progress = { animatedProgress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                            .clip(RoundedCornerShape(50)),
                        color = primaryAccent,
                        trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.14f),
                        strokeCap = StrokeCap.Round
                    )
                }
            }
        }
    }
}
