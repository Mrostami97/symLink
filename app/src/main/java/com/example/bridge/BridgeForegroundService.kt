package com.example.bridge

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.os.Build
import android.os.IBinder
import androidx.annotation.DrawableRes
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R

class BridgeForegroundService : Service() {

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
            return START_NOT_STICKY
        }

        ensureChannels(this)
        val title = intent?.getStringExtra(EXTRA_TITLE) ?: getString(R.string.app_name)
        val body = intent?.getStringExtra(EXTRA_BODY) ?: getString(R.string.notification_channel_bridge_desc)
        val signalLevel = intent?.getIntExtra(EXTRA_SIGNAL_LEVEL, -1) ?: -1
        val detailLines = intent?.getStringArrayListExtra(EXTRA_DETAIL_LINES).orEmpty()
        val isConnected = intent?.getBooleanExtra(EXTRA_IS_CONNECTED, false) ?: false
        val batteryPercent = intent?.getIntExtra(EXTRA_BATTERY_PERCENT, -1) ?: -1
        val isCharging = intent?.getBooleanExtra(EXTRA_IS_CHARGING, false) ?: false

        val notification = buildLiveTelemetryNotification(
            context = this,
            title = title,
            summary = body,
            signalLevel = signalLevel,
            detailLines = detailLines,
            isConnected = isConnected,
            batteryPercent = batteryPercent,
            isCharging = isCharging
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(
                NOTIFICATION_ID_BRIDGE,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            )
        } else {
            startForeground(NOTIFICATION_ID_BRIDGE, notification)
        }
        return START_STICKY
    }

    companion object {
        const val CHANNEL_BRIDGE = "itell_bridge_channel"
        const val CHANNEL_ALERTS = "itell_alerts_channel"
        const val NOTIFICATION_ID_BRIDGE = 1001
        const val NOTIFICATION_ID_ALERT = 1002
        const val NOTIFICATION_ID_OTP = 1003
        const val ACTION_START = "com.example.itell.ACTION_START"
        const val ACTION_STOP = "com.example.itell.ACTION_STOP"
        const val EXTRA_TITLE = "extra_title"
        const val EXTRA_BODY = "extra_body"
        const val EXTRA_SIGNAL_LEVEL = "extra_signal_level"
        const val EXTRA_DETAIL_LINES = "extra_detail_lines"
        const val EXTRA_IS_CONNECTED = "extra_is_connected"
        const val EXTRA_BATTERY_PERCENT = "extra_battery_percent"
        const val EXTRA_IS_CHARGING = "extra_is_charging"

        fun ensureChannels(context: Context) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val mgr = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                val bridgeChannel = NotificationChannel(
                    CHANNEL_BRIDGE,
                    context.getString(R.string.notification_channel_bridge_name),
                    NotificationManager.IMPORTANCE_LOW
                ).apply {
                    description = context.getString(R.string.notification_channel_bridge_desc)
                    setShowBadge(false)
                }
                val alertChannel = NotificationChannel(
                    CHANNEL_ALERTS,
                    context.getString(R.string.notification_channel_alerts_name),
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = context.getString(R.string.notification_channel_alerts_desc)
                    enableVibration(true)
                }
                mgr.createNotificationChannel(bridgeChannel)
                mgr.createNotificationChannel(alertChannel)
            }
        }

        @DrawableRes
        fun signalIconRes(signalLevel: Int, isConnected: Boolean): Int {
            if (!isConnected) return R.drawable.ic_signal_disconnected
            return when (signalLevel.coerceIn(0, 4)) {
                0 -> R.drawable.ic_signal_bars_0
                1 -> R.drawable.ic_signal_bars_1
                2 -> R.drawable.ic_signal_bars_2
                3 -> R.drawable.ic_signal_bars_3
                else -> R.drawable.ic_signal_bars_4
            }
        }

        fun signalBarText(level: Int): String = when (level.coerceIn(0, 4)) {
            0 -> "○○○○"
            1 -> "▂○○○"
            2 -> "▂▄○○"
            3 -> "▂▄▆○"
            else -> "▂▄▆█"
        }

        fun batteryBarText(percent: Int): String {
            val clamped = percent.coerceIn(0, 100)
            val filled = ((clamped + 5) / 10).coerceIn(0, 10)
            return "[" + "█".repeat(filled) + "░".repeat(10 - filled) + "]"
        }

        private fun batteryAccentColor(percent: Int, isCharging: Boolean): Int {
            return when {
                isCharging || percent >= 50 -> Color.parseColor("#10B981") // Emerald
                percent >= 21 -> Color.parseColor("#38BDF8") // Signal Cyan
                else -> Color.parseColor("#EF4444") // Alert Crimson
            }
        }

        /**
         * Renders a dynamic circular telemetry battery gauge bitmap for the persistent notification's
         * large icon, color-coded by battery level & charging state.
         */
        private fun buildBatteryBadgeBitmap(batteryPercent: Int, isCharging: Boolean): Bitmap {
            val size = 128
            val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            val center = size / 2f
            val clamped = batteryPercent.coerceIn(0, 100)
            val accent = batteryAccentColor(clamped, isCharging)

            // Dark obsidian medallion backdrop
            val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#0B1220")
                style = Paint.Style.FILL
            }
            canvas.drawCircle(center, center, center - 4f, bgPaint)

            // Outer track ring
            val strokeW = 10f
            val arcBounds = RectF(strokeW + 4f, strokeW + 4f, size - strokeW - 4f, size - strokeW - 4f)
            val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.argb(55, 148, 163, 184)
                style = Paint.Style.STROKE
                strokeWidth = strokeW
            }
            canvas.drawOval(arcBounds, trackPaint)

            // Dynamic sweep arc proportional to batteryPercent
            val sweepPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = accent
                style = Paint.Style.STROKE
                strokeWidth = strokeW
                strokeCap = Paint.Cap.ROUND
            }
            val sweepAngle = (clamped / 100f) * 360f
            canvas.drawArc(arcBounds, -90f, sweepAngle, false, sweepPaint)

            // Percentage readout
            val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.WHITE
                textAlign = Paint.Align.CENTER
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textSize = if (clamped == 100) 29f else 33f
            }
            val textY = if (isCharging) center + 2f else center + 11f
            canvas.drawText("$clamped%", center, textY, textPaint)

            // Charging indicator subtitle when plugged in
            if (isCharging) {
                val boltPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.parseColor("#FBBF24")
                    textAlign = Paint.Align.CENTER
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                    textSize = 22f
                }
                canvas.drawText("⚡", center, center + 29f, boltPaint)
            }

            return bitmap
        }

        fun startOrUpdate(
            context: Context,
            title: String,
            body: String,
            signalLevel: Int = -1,
            detailLines: List<String> = emptyList(),
            isConnected: Boolean = false,
            batteryPercent: Int = -1,
            isCharging: Boolean = false
        ) {
            val intent = Intent(context, BridgeForegroundService::class.java).apply {
                action = ACTION_START
                putExtra(EXTRA_TITLE, title)
                putExtra(EXTRA_BODY, body)
                putExtra(EXTRA_SIGNAL_LEVEL, signalLevel)
                putStringArrayListExtra(EXTRA_DETAIL_LINES, ArrayList(detailLines))
                putExtra(EXTRA_IS_CONNECTED, isConnected)
                putExtra(EXTRA_BATTERY_PERCENT, batteryPercent)
                putExtra(EXTRA_IS_CHARGING, isCharging)
            }
            runCatching {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, BridgeForegroundService::class.java).apply {
                action = ACTION_STOP
            }
            runCatching { context.startService(intent) }
        }

        fun postAlertNotification(context: Context, title: String, body: String) {
            ensureChannels(context)
            val openIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val pendingIntent = PendingIntent.getActivity(
                context,
                1,
                openIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            val notification = NotificationCompat.Builder(context, CHANNEL_ALERTS)
                .setSmallIcon(R.drawable.ic_signal_bars_4)
                .setContentTitle(title)
                .setContentText(body)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .build()

            val mgr = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            runCatching { mgr.notify(NOTIFICATION_ID_ALERT, notification) }
        }

        fun postOtpNotification(
            context: Context,
            title: String,
            otpCode: String,
            fullBody: String,
            copyLabel: String
        ) {
            ensureChannels(context)
            val copyIntent = Intent(context, BridgeActionReceiver::class.java).apply {
                action = BridgeActionReceiver.ACTION_COPY_OTP
                putExtra(BridgeActionReceiver.EXTRA_OTP, otpCode)
            }
            val copyPending = PendingIntent.getBroadcast(
                context,
                otpCode.hashCode(),
                copyIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            val openIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val openPending = PendingIntent.getActivity(
                context,
                2,
                openIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            val notification = NotificationCompat.Builder(context, CHANNEL_ALERTS)
                .setSmallIcon(R.drawable.ic_signal_bars_4)
                .setContentTitle(title)
                .setContentText(fullBody)
                .setStyle(NotificationCompat.BigTextStyle().bigText(fullBody))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(openPending)
                .addAction(0, "$copyLabel ($otpCode)", copyPending)
                .build()

            val mgr = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            runCatching { mgr.notify(NOTIFICATION_ID_OTP, notification) }
        }

        private fun buildLiveTelemetryNotification(
            context: Context,
            title: String,
            summary: String,
            signalLevel: Int,
            detailLines: List<String>,
            isConnected: Boolean,
            batteryPercent: Int,
            isCharging: Boolean
        ): Notification {
            val openIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val pendingIntent = PendingIntent.getActivity(
                context,
                0,
                openIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val phonePending = PendingIntent.getBroadcast(
                context,
                10,
                Intent(context, BridgeActionReceiver::class.java).setAction(BridgeActionReceiver.ACTION_OPEN_PHONE),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            val smsPending = PendingIntent.getBroadcast(
                context,
                11,
                Intent(context, BridgeActionReceiver::class.java).setAction(BridgeActionReceiver.ACTION_OPEN_MESSAGES),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            val voicePending = PendingIntent.getBroadcast(
                context,
                12,
                Intent(context, BridgeActionReceiver::class.java).setAction(BridgeActionReceiver.ACTION_TOGGLE_VOICE),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val builder = NotificationCompat.Builder(context, CHANNEL_BRIDGE)
                .setSmallIcon(signalIconRes(signalLevel, isConnected))
                .setContentTitle(title)
                .setContentText(summary)
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setContentIntent(pendingIntent)

            if (batteryPercent in 0..100) {
                val clamped = batteryPercent.coerceIn(0, 100)
                val badgeText = if (isCharging) "$clamped% ⚡" else "$clamped%"
                builder
                    .setSubText(badgeText)
                    .setLargeIcon(buildBatteryBadgeBitmap(clamped, isCharging))
                    .setColor(batteryAccentColor(clamped, isCharging))
                    .setProgress(100, clamped, false)
            } else {
                builder.setColor(Color.parseColor("#38BDF8"))
            }

            if (detailLines.isNotEmpty()) {
                val inbox = NotificationCompat.InboxStyle().setBigContentTitle(title)
                detailLines.forEach { inbox.addLine(it) }
                if (batteryPercent in 0..100) {
                    val chargeTag = if (isCharging) " ⚡" else ""
                    inbox.setSummaryText("$batteryPercent%$chargeTag  ${batteryBarText(batteryPercent)}")
                }
                builder.setStyle(inbox)
            }

            if (isConnected) {
                builder
                    .addAction(0, "Phone", phonePending)
                    .addAction(0, "SMS", smsPending)
                    .addAction(0, "Voice", voicePending)
            }

            return builder.build()
        }
    }
}
