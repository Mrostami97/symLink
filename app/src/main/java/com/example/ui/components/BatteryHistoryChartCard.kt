package com.example.ui.components

import android.text.Layout
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.BatteryStd
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.bridge.BatteryTimeWindow
import com.example.bridge.BatteryTrendCalculator
import com.example.bridge.DeviceRole
import com.example.data.local.BatteryHistoryEntity
import com.example.ui.i18n.LocalAppStrings
import com.example.ui.i18n.localizeDigits
import com.example.ui.theme.AlertCrimson
import com.example.ui.theme.ConnectedEmerald
import com.example.ui.theme.MonoTechnicalStyle
import com.example.ui.theme.SignalCyan
import com.example.ui.theme.Sim2Amber
import com.patrykandpatrick.vico.compose.cartesian.CartesianChartHost
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberBottom
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberStart
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLine
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLineCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.marker.rememberDefaultCartesianMarker
import com.patrykandpatrick.vico.compose.cartesian.rememberCartesianChart
import com.patrykandpatrick.vico.compose.common.ProvideVicoTheme
import com.patrykandpatrick.vico.compose.common.component.rememberLineComponent
import com.patrykandpatrick.vico.compose.common.component.rememberShapeComponent
import com.patrykandpatrick.vico.compose.common.component.rememberTextComponent
import com.patrykandpatrick.vico.compose.common.fill
import com.patrykandpatrick.vico.compose.m3.common.rememberM3VicoTheme
import com.patrykandpatrick.vico.core.cartesian.axis.HorizontalAxis
import com.patrykandpatrick.vico.core.cartesian.axis.VerticalAxis
import com.patrykandpatrick.vico.core.cartesian.data.CartesianChartModelProducer
import com.patrykandpatrick.vico.core.cartesian.data.CartesianLayerRangeProvider
import com.patrykandpatrick.vico.core.cartesian.data.CartesianValueFormatter
import com.patrykandpatrick.vico.core.cartesian.data.lineSeries
import com.patrykandpatrick.vico.core.cartesian.layer.LineCartesianLayer
import com.patrykandpatrick.vico.core.cartesian.marker.DefaultCartesianMarker
import com.patrykandpatrick.vico.core.common.Insets
import com.patrykandpatrick.vico.core.common.shader.ShaderProvider
import com.patrykandpatrick.vico.core.common.shape.CorneredShape
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun BatteryHistoryChartCard(
    role: DeviceRole,
    deviceName: String?,
    samples: List<BatteryHistoryEntity>,
    selectedWindow: BatteryTimeWindow,
    onSelectWindow: (BatteryTimeWindow) -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = LocalAppStrings.current
    val summary = remember(samples) { BatteryTrendCalculator.summarize(samples) }

    val lineAccent = when {
        summary == null -> SignalCyan
        summary.isCharging -> SignalCyan
        summary.currentPercent <= 20 -> AlertCrimson
        summary.currentPercent <= 40 -> Sim2Amber
        else -> ConnectedEmerald
    }

    val chartPoints = remember(samples) {
        when {
            samples.isEmpty() -> emptyList()
            samples.size == 1 -> {
                val single = samples.first()
                listOf(
                    single.copy(timestamp = single.timestamp - 30_000L),
                    single
                )
            }
            else -> samples
        }
    }

    val modelProducer = remember { CartesianChartModelProducer() }
    LaunchedEffect(chartPoints) {
        if (chartPoints.isNotEmpty()) {
            modelProducer.runTransaction {
                lineSeries {
                    series(
                        x = chartPoints.indices.map { it.toDouble() },
                        y = chartPoints.map { it.batteryPercent.toDouble() }
                    )
                }
            }
        }
    }

    val timeFormatter = remember(selectedWindow) {
        val pattern = if (selectedWindow == BatteryTimeWindow.FIFTEEN_MIN) "HH:mm:ss" else "HH:mm"
        SimpleDateFormat(pattern, Locale.getDefault())
    }

    val xAxisFormatter = remember(chartPoints, strings.isPersian, timeFormatter) {
        CartesianValueFormatter { _, value, _ ->
            val idx = value.toInt().coerceIn(0, (chartPoints.size - 1).coerceAtLeast(0))
            val ts = chartPoints.getOrNull(idx)?.timestamp ?: return@CartesianValueFormatter ""
            timeFormatter.format(Date(ts)).localizeDigits(strings.isPersian)
        }
    }

    val yAxisFormatter = remember(strings.isPersian) {
        CartesianValueFormatter { _, value, _ ->
            "${value.toInt()}%".localizeDigits(strings.isPersian)
        }
    }

    GlassBridgeCard(
        modifier = modifier
            .fillMaxWidth()
            .testTag("battery_history_chart_card"),
        accentColor = lineAccent,
        accentStrength = 0.34f,
        cornerRadius = 22.dp
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header row: icon + title + live battery badge
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
                            .size(36.dp)
                            .clip(RoundedCornerShape(11.dp))
                            .background(lineAccent.copy(alpha = 0.16f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ShowChart,
                            contentDescription = strings.batteryTrendTitle,
                            tint = lineAccent,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                        Text(
                            text = strings.batteryTrendTitle,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = deviceName?.takeIf { it.isNotBlank() }
                                ?: if (role == DeviceRole.COMPANION) {
                                    strings.batteryTrendSubtitleConnected
                                } else {
                                    strings.batteryTrendSubtitleLocal
                                },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                if (summary != null) {
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = lineAccent.copy(alpha = 0.16f),
                        border = BorderStroke(1.dp, lineAccent.copy(alpha = 0.42f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = if (summary.isCharging) {
                                    Icons.Filled.BatteryChargingFull
                                } else {
                                    Icons.Filled.BatteryStd
                                },
                                contentDescription = strings.hostBattery,
                                tint = lineAccent,
                                modifier = Modifier.size(15.dp)
                            )
                            Text(
                                text = "${summary.currentPercent}%".localizeDigits(strings.isPersian),
                                style = MonoTechnicalStyle,
                                color = lineAccent,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // Time-window selector chips
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                BatteryTimeWindow.entries.forEach { window ->
                    val selected = window == selectedWindow
                    val label = when (window) {
                        BatteryTimeWindow.FIFTEEN_MIN -> strings.batteryWindow15m
                        BatteryTimeWindow.ONE_HOUR -> strings.batteryWindow1h
                        BatteryTimeWindow.SIX_HOURS -> strings.batteryWindow6h
                        BatteryTimeWindow.TWENTY_FOUR_HOURS -> strings.batteryWindow24h
                    }
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .height(40.dp)
                            .widthIn(min = 48.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { onSelectWindow(window) }
                            .testTag("battery_window_${window.tag}"),
                        shape = RoundedCornerShape(12.dp),
                        color = if (selected) {
                            lineAccent.copy(alpha = 0.20f)
                        } else {
                            MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.65f)
                        },
                        border = BorderStroke(
                            1.dp,
                            if (selected) lineAccent.copy(alpha = 0.6f)
                            else MaterialTheme.colorScheme.outline.copy(alpha = 0.24f)
                        )
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                color = if (selected) lineAccent else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            if (chartPoints.isEmpty()) {
                // Friendly empty state when waiting for connected peer telemetry
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.55f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 22.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(SignalCyan.copy(alpha = 0.14f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ShowChart,
                                contentDescription = null,
                                tint = SignalCyan,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Text(
                            text = if (role == DeviceRole.COMPANION) {
                                strings.batteryEmptyCompanion
                            } else {
                                strings.batteryCollectingHint
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                // Native Vico Material 3 Interactive Line & Area Chart (LTR time axis)
                val axisLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
                val guidelineColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.22f)
                val markerSurfaceColor = MaterialTheme.colorScheme.surfaceContainerHighest
                val markerTextColor = MaterialTheme.colorScheme.onSurface

                val vicoTheme = rememberM3VicoTheme()
                val markerLabel = rememberTextComponent(
                    color = markerTextColor,
                    textSize = 11.sp,
                    padding = Insets(horizontalDp = 8f, verticalDp = 4f),
                    background = rememberShapeComponent(
                        fill = fill(markerSurfaceColor),
                        shape = CorneredShape.Pill
                    ),
                    textAlignment = Layout.Alignment.ALIGN_CENTER
                )
                val marker = rememberDefaultCartesianMarker(
                    label = markerLabel,
                    valueFormatter = remember {
                        DefaultCartesianMarker.ValueFormatter.default(DecimalFormat("#'%'"))
                    },
                    guideline = rememberLineComponent(
                        fill = fill(lineAccent.copy(alpha = 0.45f)),
                        thickness = 1.5.dp
                    )
                )

                val areaGradientTop = Color(
                    red = lineAccent.red,
                    green = lineAccent.green,
                    blue = lineAccent.blue,
                    alpha = 0.32f
                )
                val areaGradientBottom = Color(
                    red = lineAccent.red,
                    green = lineAccent.green,
                    blue = lineAccent.blue,
                    alpha = 0.02f
                )

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.48f),
                    border = BorderStroke(1.dp, lineAccent.copy(alpha = 0.18f))
                ) {
                    Column(modifier = Modifier.padding(top = 12.dp, bottom = 8.dp, start = 8.dp, end = 12.dp)) {
                        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                            ProvideVicoTheme(vicoTheme) {
                                CartesianChartHost(
                                    chart = rememberCartesianChart(
                                        rememberLineCartesianLayer(
                                            lineProvider = LineCartesianLayer.LineProvider.series(
                                                LineCartesianLayer.rememberLine(
                                                    fill = LineCartesianLayer.LineFill.single(fill(lineAccent)),
                                                    areaFill = LineCartesianLayer.AreaFill.single(
                                                        fill(
                                                            ShaderProvider.verticalGradient(
                                                                colors = intArrayOf(
                                                                    areaGradientTop.toArgb(),
                                                                    areaGradientBottom.toArgb()
                                                                )
                                                            )
                                                        )
                                                    ),
                                                    pointConnector = LineCartesianLayer.PointConnector.cubic()
                                                )
                                            ),
                                            rangeProvider = remember {
                                                CartesianLayerRangeProvider.fixed(minY = 0.0, maxY = 100.0)
                                            }
                                        ),
                                        startAxis = VerticalAxis.rememberStart(
                                            label = rememberTextComponent(
                                                color = axisLabelColor,
                                                textSize = 10.sp
                                            ),
                                            guideline = rememberLineComponent(
                                                fill = fill(guidelineColor),
                                                thickness = 1.dp
                                            ),
                                            valueFormatter = yAxisFormatter,
                                            itemPlacer = remember { VerticalAxis.ItemPlacer.count(count = { 5 }) }
                                        ),
                                        bottomAxis = HorizontalAxis.rememberBottom(
                                            label = rememberTextComponent(
                                                color = axisLabelColor,
                                                textSize = 10.sp
                                            ),
                                            guideline = null,
                                            valueFormatter = xAxisFormatter
                                        ),
                                        marker = marker
                                    ),
                                    modelProducer = modelProducer,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(185.dp)
                                        .testTag("battery_vico_chart")
                                )
                            }
                        }

                        if (samples.size < 2) {
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = strings.batteryCollectingHint,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 8.dp)
                            )
                        }
                    }
                }

                // Bottom Analytics Strip: Min, Max, Delta/Rate, Points
                if (summary != null) {
                    val trendValue = if (summary.ratePerHour != null) {
                        BatteryTrendCalculator.formatRatePerHour(summary.ratePerHour)
                    } else {
                        BatteryTrendCalculator.formatSignedDelta(summary.deltaPercent)
                    }
                    val trendColor = when {
                        summary.deltaPercent > 0 -> ConnectedEmerald
                        summary.deltaPercent < 0 -> Sim2Amber
                        else -> SignalCyan
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        BatteryStatMiniTile(
                            label = strings.batteryMinLabel,
                            value = "${summary.minPercent}%".localizeDigits(strings.isPersian),
                            accent = Sim2Amber,
                            modifier = Modifier.weight(1f)
                        )
                        BatteryStatMiniTile(
                            label = strings.batteryMaxLabel,
                            value = "${summary.maxPercent}%".localizeDigits(strings.isPersian),
                            accent = ConnectedEmerald,
                            modifier = Modifier.weight(1f)
                        )
                        BatteryStatMiniTile(
                            label = strings.batteryChangeLabel,
                            value = trendValue.localizeDigits(strings.isPersian),
                            accent = trendColor,
                            modifier = Modifier.weight(1f)
                        )
                        BatteryStatMiniTile(
                            label = strings.batterySamplesLabel,
                            value = summary.sampleCount.toString().localizeDigits(strings.isPersian),
                            accent = SignalCyan,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BatteryStatMiniTile(
    label: String,
    value: String,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = accent.copy(alpha = 0.09f),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.24f))
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 7.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = value,
                style = MonoTechnicalStyle,
                color = accent,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
