package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "paired_devices")
data class PairedDeviceEntity(
    @PrimaryKey val deviceId: String,
    val deviceName: String,
    val ipAddress: String,
    val port: Int,
    val bluetoothAddress: String = "",
    val sessionKeyBase64: String,
    val fingerprint: String,
    val peerRole: String,
    val lastConnectedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "bridge_messages")
data class BridgeMessageEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val peerAddress: String,
    val peerName: String,
    val body: String,
    val simSlot: Int = 1,
    val messageType: String, // RELAY_CHAT, OTP_CODE, SMS_HANDOFF
    val extractedOtp: String? = null,
    val isOutgoing: Boolean,
    val deliveryStatus: String = "SENT", // SENT, DELIVERED, HANDED_OFF
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "bridge_call_events")
data class BridgeCallEventEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val phoneNumber: String,
    val contactName: String,
    val simSlot: Int = 1,
    val eventType: String, // INCOMING_ALERT, REMOTE_DIAL_REQUEST, VOICE_TUNNEL
    val durationSeconds: Int = 0,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "synced_contacts")
data class SyncedContactEntity(
    @PrimaryKey val normalizedNumber: String,
    val displayName: String,
    val phoneNumber: String,
    val avatarColorIndex: Int = 0,
    val syncedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "battery_history")
data class BatteryHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val deviceId: String,
    val deviceName: String,
    val batteryPercent: Int,
    val isCharging: Boolean,
    val timestamp: Long = System.currentTimeMillis()
)
