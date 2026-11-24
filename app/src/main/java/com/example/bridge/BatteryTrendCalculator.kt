package com.example.bridge

import com.example.data.local.BatteryHistoryEntity
import kotlin.math.abs

enum class BatteryTimeWindow(val durationMs: Long, val tag: String) {
    FIFTEEN_MIN(15L * 60_000L, "15m"),
    ONE_HOUR(60L * 60_000L, "1h"),
    SIX_HOURS(6L * 60L * 60_000L, "6h"),
    TWENTY_FOUR_HOURS(24L * 60L * 60_000L, "24h")
}

data class BatteryTrendSummary(
    val currentPercent: Int,
    val isCharging: Boolean,
    val minPercent: Int,
    val maxPercent: Int,
    val deltaPercent: Int,
    val ratePerHour: Double?,
    val sampleCount: Int
)

object BatteryTrendCalculator {
    const val LOCAL_HOST_DEVICE_ID = "LOCAL_HOST"
    const val HEARTBEAT_INTERVAL_MS = 60_000L
    const val RETENTION_MS = 24L * 60L * 60_000L

    fun shouldRecordSample(
        lastSample: BatteryHistoryEntity?,
        newPercent: Int,
        newCharging: Boolean,
        nowMs: Long
    ): Boolean {
        if (newPercent !in 0..100) return false
        if (lastSample == null) return true
        if (lastSample.batteryPercent != newPercent) return true
        if (lastSample.isCharging != newCharging) return true
        return (nowMs - lastSample.timestamp) >= HEARTBEAT_INTERVAL_MS
    }

    fun summarize(samples: List<BatteryHistoryEntity>): BatteryTrendSummary? {
        if (samples.isEmpty()) return null
        val sorted = samples.sortedBy { it.timestamp }
        val first = sorted.first()
        val last = sorted.last()
        val minVal = sorted.minOf { it.batteryPercent }
        val maxVal = sorted.maxOf { it.batteryPercent }
        val delta = last.batteryPercent - first.batteryPercent
        val elapsedMs = last.timestamp - first.timestamp
        val ratePerHour = if (elapsedMs >= 60_000L) {
            delta.toDouble() / (elapsedMs.toDouble() / 3_600_000.0)
        } else {
            null
        }
        return BatteryTrendSummary(
            currentPercent = last.batteryPercent,
            isCharging = last.isCharging,
            minPercent = minVal,
            maxPercent = maxVal,
            deltaPercent = delta,
            ratePerHour = ratePerHour,
            sampleCount = sorted.size
        )
    }

    fun formatSignedDelta(delta: Int): String = when {
        delta > 0 -> "+$delta%"
        delta < 0 -> "$delta%"
        else -> "0%"
    }

    fun formatRatePerHour(rate: Double): String {
        val rounded = (abs(rate) * 10).toInt() / 10.0
        val sign = when {
            rate > 0.05 -> "+"
            rate < -0.05 -> "-"
            else -> ""
        }
        return "$sign$rounded%/h"
    }
}
