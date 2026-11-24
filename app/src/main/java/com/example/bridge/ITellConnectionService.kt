package com.example.bridge

import android.net.Uri
import android.os.Build
import android.telecom.Connection
import android.telecom.ConnectionRequest
import android.telecom.ConnectionService
import android.telecom.DisconnectCause
import android.telecom.PhoneAccountHandle
import android.telecom.TelecomManager

/**
 * Native Android Telecom ConnectionService enabling calls placed from the phone's default
 * Phone app (via the iTell Calling Account) and incoming Host call alerts to use the native
 * Android call workflow while streaming audio through the encrypted iTell bridge.
 */
class ITellConnectionService : ConnectionService() {

    override fun onCreateOutgoingConnection(
        connectionManagerPhoneAccount: PhoneAccountHandle?,
        request: ConnectionRequest?
    ): Connection {
        val number = request?.address?.schemeSpecificPart.orEmpty()
        val simSlot = request?.extras?.getInt("sim_slot", BridgeSessionHub.preferredSimSlot)
            ?: BridgeSessionHub.preferredSimSlot

        val sent = BridgeSessionHub.sendRemoteDial(applicationContext, number, simSlot)
        if (!sent) {
            return Connection.createFailedConnection(
                DisconnectCause(DisconnectCause.ERROR, "SimLink Host not connected")
            )
        }

        val voice = BridgeSessionHub.voiceBridge(applicationContext)
        if (voice.hasRecordAudioPermission()) {
            if (voice.start()) {
                BridgeSessionHub.transport(applicationContext).sendJson(BridgeProtocol.CMD_VOICE_START)
            }
        }

        return ITellBridgeConnection(number, isIncoming = false).apply {
            setDialing()
            setActive()
        }
    }

    override fun onCreateIncomingConnection(
        connectionManagerPhoneAccount: PhoneAccountHandle?,
        request: ConnectionRequest?
    ): Connection {
        val hint = request?.extras?.getString("caller_hint").orEmpty().ifBlank { "SimLink Host SIM" }
        return ITellBridgeConnection(hint, isIncoming = true).apply {
            setRinging()
        }
    }

    private inner class ITellBridgeConnection(
        displayNumber: String,
        isIncoming: Boolean
    ) : Connection() {

        init {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                connectionProperties = PROPERTY_SELF_MANAGED
            }
            connectionCapabilities = CAPABILITY_MUTE or CAPABILITY_SUPPORT_HOLD
            audioModeIsVoip = true
            setAddress(Uri.parse("tel:$displayNumber"), TelecomManager.PRESENTATION_ALLOWED)
            setCallerDisplayName(
                if (isIncoming) "SimLink • $displayNumber" else displayNumber,
                TelecomManager.PRESENTATION_ALLOWED
            )
        }

        override fun onAnswer() {
            val voice = BridgeSessionHub.voiceBridge(applicationContext)
            if (voice.hasRecordAudioPermission() && voice.start()) {
                BridgeSessionHub.transport(applicationContext).sendJson(BridgeProtocol.CMD_VOICE_START)
            }
            setActive()
        }

        override fun onReject() {
            cleanupAndDestroy(DisconnectCause.REJECTED)
        }

        override fun onDisconnect() {
            cleanupAndDestroy(DisconnectCause.LOCAL)
        }

        override fun onAbort() {
            cleanupAndDestroy(DisconnectCause.CANCELED)
        }

        private fun cleanupAndDestroy(causeCode: Int) {
            val voice = BridgeSessionHub.voiceBridge(applicationContext)
            if (voice.state.value.isActive) {
                voice.stop()
                BridgeSessionHub.transport(applicationContext).sendJson(BridgeProtocol.CMD_VOICE_STOP)
            }
            setDisconnected(DisconnectCause(causeCode))
            destroy()
        }
    }
}
