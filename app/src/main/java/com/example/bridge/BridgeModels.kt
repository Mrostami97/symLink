package com.example.bridge

import org.json.JSONArray
import org.json.JSONObject

enum class DeviceRole {
    HOST,
    COMPANION
}

enum class TransportMode {
    WIFI_TCP,
    BLUETOOTH_RFCOMM
}

sealed interface ConnectionState {
    data object Idle : ConnectionState
    data class Hosting(val hostIp: String, val port: Int, val bluetoothReady: Boolean) : ConnectionState
    data object Discovering : ConnectionState
    data class Connecting(val targetLabel: String) : ConnectionState
    data class Connected(
        val peerDeviceId: String,
        val peerName: String,
        val transport: TransportMode,
        val fingerprint: String,
        val endpointLabel: String
    ) : ConnectionState
    data class Reconnecting(val peerName: String, val attempt: Int) : ConnectionState
    data class Error(val message: String) : ConnectionState
}

data class SimCardInfo(
    val slotIndex: Int, // 1 or 2
    val subscriptionId: Int,
    val carrierName: String,
    val displayName: String,
    val networkType: String,
    val signalLevel: Int = 0, // 0..4 cellular signal bars
    val isActive: Boolean = true
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("slot", slotIndex)
        put("subId", subscriptionId)
        put("carrier", carrierName)
        put("display", displayName)
        put("net", networkType)
        put("sig", signalLevel.coerceIn(0, 4))
        put("active", isActive)
    }

    companion object {
        fun fromJson(obj: JSONObject): SimCardInfo = SimCardInfo(
            slotIndex = obj.optInt("slot", 1),
            subscriptionId = obj.optInt("subId", -1),
            carrierName = obj.optString("carrier", "SIM"),
            displayName = obj.optString("display", "SIM"),
            networkType = obj.optString("net", "LTE"),
            signalLevel = obj.optInt("sig", 0).coerceIn(0, 4),
            isActive = obj.optBoolean("active", true)
        )
    }
}

data class DeviceTelemetry(
    val deviceName: String = "",
    val batteryPercent: Int = -1,
    val isCharging: Boolean = false,
    val localIp: String = "",
    val simCards: List<SimCardInfo> = emptyList(),
    val pingMs: Long = -1L,
    val lastUpdatedAt: Long = System.currentTimeMillis()
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("deviceName", deviceName)
        put("battery", batteryPercent)
        put("charging", isCharging)
        put("localIp", localIp)
        put("sims", JSONArray().apply { simCards.forEach { put(it.toJson()) } })
        put("ts", lastUpdatedAt)
    }

    companion object {
        fun fromJson(obj: JSONObject, pingMs: Long = -1L): DeviceTelemetry {
            val simsJson = obj.optJSONArray("sims") ?: JSONArray()
            val sims = buildList {
                for (i in 0 until simsJson.length()) {
                    simsJson.optJSONObject(i)?.let { add(SimCardInfo.fromJson(it)) }
                }
            }
            return DeviceTelemetry(
                deviceName = obj.optString("deviceName", "Android"),
                batteryPercent = obj.optInt("battery", -1),
                isCharging = obj.optBoolean("charging", false),
                localIp = obj.optString("localIp", ""),
                simCards = sims,
                pingMs = pingMs,
                lastUpdatedAt = System.currentTimeMillis()
            )
        }
    }
}

data class PairingPayload(
    val deviceId: String,
    val deviceName: String,
    val hostIp: String,
    val port: Int,
    val btAddress: String = "",
    val sessionKeyBase64: String,
    val pairCode: String,
    val fingerprint: String
) {
    fun toQrString(): String = JSONObject().apply {
        put("proto", "itell-v1")
        put("id", deviceId)
        put("name", deviceName)
        put("ip", hostIp)
        put("port", port)
        put("bt", btAddress)
        put("key", sessionKeyBase64)
        put("code", pairCode)
        put("fp", fingerprint)
    }.toString()

    companion object {
        fun parseQrString(raw: String): PairingPayload? = runCatching {
            val obj = JSONObject(raw.trim())
            if (obj.optString("proto") != "itell-v1") return null
            val ip = obj.optString("ip", "")
            val port = obj.optInt("port", 47470)
            val key = obj.optString("key", "")
            if (ip.isBlank() && obj.optString("bt", "").isBlank()) return null
            if (key.isBlank()) return null
            PairingPayload(
                deviceId = obj.optString("id", "host_${ip.hashCode()}"),
                deviceName = obj.optString("name", "iTell Host"),
                hostIp = ip,
                port = port,
                btAddress = obj.optString("bt", ""),
                sessionKeyBase64 = key,
                pairCode = obj.optString("code", ""),
                fingerprint = obj.optString("fp", BridgeCrypto.computeFingerprint(key))
            )
        }.getOrNull()
    }
}

data class DiscoveredPeer(
    val id: String,
    val name: String,
    val hostIp: String = "",
    val port: Int = 47470,
    val btAddress: String = "",
    val transport: TransportMode
)

data class IncomingCallBanner(
    val isActive: Boolean = false,
    val isRinging: Boolean = false,
    val phoneNumberHint: String = "",
    val simSlot: Int = 1,
    val startedAt: Long = System.currentTimeMillis()
)

data class PendingHandoff(
    val id: String,
    val type: HandoffType,
    val phoneNumber: String,
    val body: String = "",
    val simSlot: Int = 1,
    val timestamp: Long = System.currentTimeMillis()
)

enum class HandoffType {
    DIAL_REQUEST,
    SMS_DRAFT
}

data class VoiceTunnelState(
    val isActive: Boolean = false,
    val isMuted: Boolean = false,
    val isSpeakerOn: Boolean = true,
    val localAmplitude: Float = 0f,
    val remoteAmplitude: Float = 0f,
    val durationSeconds: Int = 0
)
