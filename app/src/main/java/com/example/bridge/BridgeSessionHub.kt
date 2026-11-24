package com.example.bridge

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.json.JSONObject

/**
 * Process-wide bridge runtime holder shared by BridgeViewModel, ITellConnectionService,
 * BridgeSystemRouterActivity, and BridgeActionReceiver so calls/messages routed from
 * system default apps use the exact same live encrypted P2P session.
 */
object BridgeSessionHub {

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    @Volatile
    private var transportInstance: BridgeTransportManager? = null

    @Volatile
    private var voiceBridgeInstance: VoiceAudioBridge? = null

    @Volatile
    var preferredSimSlot: Int = 1

    @Volatile
    var languageCode: String = "fa"

    @Volatile
    var darkTheme: Boolean = true

    @Volatile
    var latestPeerTelemetry: DeviceTelemetry? = null

    fun transport(context: Context): BridgeTransportManager {
        return transportInstance ?: synchronized(this) {
            transportInstance ?: BridgeTransportManager(context.applicationContext).also {
                transportInstance = it
            }
        }
    }

    fun voiceBridge(context: Context): VoiceAudioBridge {
        return voiceBridgeInstance ?: synchronized(this) {
            val t = transport(context)
            voiceBridgeInstance ?: VoiceAudioBridge(context.applicationContext, appScope) { pcm ->
                t.sendAudioPcm(pcm)
            }.also {
                voiceBridgeInstance = it
            }
        }
    }

    fun sendRemoteDial(context: Context, phoneNumber: String, simSlot: Int = preferredSimSlot): Boolean {
        val clean = DeviceTelemetryReader.normalizePhoneNumber(phoneNumber)
        if (clean.isBlank()) return false
        return transport(context).sendJson(
            BridgeProtocol.CMD_DIAL_REQUEST,
            JSONObject().apply {
                put("number", clean)
                put("simSlot", simSlot)
                put("contactName", clean)
            }
        )
    }

    fun sendRemoteSmsDraft(
        context: Context,
        recipient: String,
        body: String,
        simSlot: Int = preferredSimSlot
    ): Boolean {
        val cleanRecipient = DeviceTelemetryReader.normalizePhoneNumber(recipient)
        val trimmed = body.trim()
        if (cleanRecipient.isBlank() || trimmed.isBlank()) return false
        return transport(context).sendJson(
            BridgeProtocol.CMD_SMS_HANDOFF,
            JSONObject().apply {
                put("handoffId", java.util.UUID.randomUUID().toString())
                put("recipient", cleanRecipient)
                put("body", trimmed)
                put("simSlot", simSlot)
            }
        )
    }

    fun toggleVoiceFromSystem(context: Context): Boolean {
        val t = transport(context)
        val vb = voiceBridge(context)
        return if (vb.state.value.isActive) {
            vb.stop()
            t.sendJson(BridgeProtocol.CMD_VOICE_STOP)
            false
        } else {
            if (t.connectionState.value !is ConnectionState.Connected) return false
            val started = vb.start()
            if (started) t.sendJson(BridgeProtocol.CMD_VOICE_START)
            started
        }
    }
}
