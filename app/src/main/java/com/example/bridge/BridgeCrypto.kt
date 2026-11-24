package com.example.bridge

import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

@OptIn(ExperimentalEncodingApi::class)
object BridgeCrypto {
    private const val AES_ALGORITHM = "AES"
    private const val AES_GCM_TRANSFORMATION = "AES/GCM/NoPadding"
    const val GCM_IV_LENGTH = 12
    const val GCM_TAG_LENGTH_BITS = 128

    private val secureRandom = SecureRandom()

    fun generatePairCode(): String {
        val code = secureRandom.nextInt(900_000) + 100_000
        return code.toString()
    }

    fun deriveKeyFromPairCode(pairCode: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
            .digest("iTell-Android-Bridge-v1:${pairCode.trim()}".toByteArray(Charsets.UTF_8))
        return Base64.encode(digest)
    }

    fun generateSessionKey(): String {
        val bytes = ByteArray(32)
        secureRandom.nextBytes(bytes)
        return Base64.encode(bytes)
    }

    fun computeFingerprint(sessionKeyBase64: String): String {
        val raw = runCatching { Base64.decode(sessionKeyBase64) }
            .getOrElse { sessionKeyBase64.toByteArray(Charsets.UTF_8) }
        val hash = MessageDigest.getInstance("SHA-256").digest(raw)
        val hex = hash.take(4).joinToString("") { "%02X".format(it) }
        return "${hex.substring(0, 4)}-${hex.substring(4, 8)}"
    }

    fun encrypt(plaintext: ByteArray, keyBase64: String): Pair<ByteArray, ByteArray> {
        val keyBytes = Base64.decode(keyBase64)
        val iv = ByteArray(GCM_IV_LENGTH).also { secureRandom.nextBytes(it) }
        val cipher = Cipher.getInstance(AES_GCM_TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(keyBytes, AES_ALGORITHM), GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv))
        val ciphertext = cipher.doFinal(plaintext)
        return iv to ciphertext
    }

    fun decrypt(iv: ByteArray, ciphertext: ByteArray, keyBase64: String): ByteArray {
        val keyBytes = Base64.decode(keyBase64)
        val cipher = Cipher.getInstance(AES_GCM_TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(keyBytes, AES_ALGORITHM), GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv))
        return cipher.doFinal(ciphertext)
    }
}
