package com.example.bridge

import java.io.DataInputStream
import java.io.DataOutputStream
import org.json.JSONObject

object BridgeProtocol {
    const val DEFAULT_PORT = 47470
    const val NSD_SERVICE_TYPE = "_itell._tcp."
    const val BLUETOOTH_SPP_UUID = "8d2e1b40-7a3c-4f9e-a512-6c8b9e1d2f40"

    // Frame types
    const val FRAME_JSON: Byte = 1
    const val FRAME_AUDIO_PCM: Byte = 2
    const val FRAME_PING: Byte = 3
    const val FRAME_PONG: Byte = 4

    // JSON command types
    const val CMD_HANDSHAKE = "HANDSHAKE"
    const val CMD_HANDSHAKE_ACK = "HANDSHAKE_ACK"
    const val CMD_TELEMETRY = "TELEMETRY"
    const val CMD_DIAL_REQUEST = "DIAL_REQUEST"
    const val CMD_DIAL_ACK = "DIAL_ACK"
    const val CMD_CALL_STATE = "CALL_STATE"
    const val CMD_MESSAGE_RELAY = "MESSAGE_RELAY"
    const val CMD_OTP_RELAY = "OTP_RELAY"
    const val CMD_SMS_HANDOFF = "SMS_HANDOFF"
    const val CMD_SMS_HANDOFF_ACK = "SMS_HANDOFF_ACK"
    const val CMD_CONTACTS_SYNC = "CONTACTS_SYNC"
    const val CMD_VOICE_START = "VOICE_START"
    const val CMD_VOICE_STOP = "VOICE_STOP"

    private const val MAX_FRAME_BYTES = 1024 * 1024 // 1 MB safety limit

    data class Frame(
        val type: Byte,
        val payload: ByteArray
    )

    fun writeEncryptedFrame(
        out: DataOutputStream,
        type: Byte,
        plaintext: ByteArray,
        keyBase64: String
    ) {
        val (iv, cipher) = BridgeCrypto.encrypt(plaintext, keyBase64)
        synchronized(out) {
            out.writeByte(type.toInt())
            out.writeInt(iv.size)
            out.write(iv)
            out.writeInt(cipher.size)
            out.write(cipher)
            out.flush()
        }
    }

    fun readEncryptedFrame(
        input: DataInputStream,
        primaryKeyBase64: String,
        fallbackKeyBase64: String? = null
    ): Pair<Frame, String> {
        val type = input.readByte()
        val ivLen = input.readInt()
        if (ivLen <= 0 || ivLen > 64) throw IllegalStateException("Invalid IV length: $ivLen")
        val iv = ByteArray(ivLen)
        input.readFully(iv)

        val cipherLen = input.readInt()
        if (cipherLen <= 0 || cipherLen > MAX_FRAME_BYTES) {
            throw IllegalStateException("Invalid frame length: $cipherLen")
        }
        val cipher = ByteArray(cipherLen)
        input.readFully(cipher)

        return try {
            val plain = BridgeCrypto.decrypt(iv, cipher, primaryKeyBase64)
            Frame(type, plain) to primaryKeyBase64
        } catch (primaryError: Exception) {
            if (!fallbackKeyBase64.isNullOrBlank() && fallbackKeyBase64 != primaryKeyBase64) {
                val plain = BridgeCrypto.decrypt(iv, cipher, fallbackKeyBase64)
                Frame(type, plain) to fallbackKeyBase64
            } else {
                throw primaryError
            }
        }
    }

    fun jsonCommand(cmd: String, block: JSONObject.() -> Unit = {}): ByteArray {
        val obj = JSONObject().apply {
            put("cmd", cmd)
            put("ts", System.currentTimeMillis())
            block()
        }
        return obj.toString().toByteArray(Charsets.UTF_8)
    }
}
