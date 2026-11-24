package com.example

import com.example.bridge.BatteryTrendCalculator
import com.example.bridge.BridgeCrypto
import com.example.bridge.BridgeForegroundService
import com.example.bridge.BridgeProtocol
import com.example.bridge.DeviceTelemetryReader
import com.example.data.local.BatteryHistoryEntity
import com.example.ui.i18n.localizeDigits
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import javax.crypto.AEADBadTagException
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun aes256GcmEncryptDecryptRoundtripUsesUniqueIvs() {
        val key = BridgeCrypto.generateSessionKey()
        val plaintext = "SimLink سلام دنیا 123456".toByteArray(Charsets.UTF_8)

        val (iv1, cipher1) = BridgeCrypto.encrypt(plaintext, key)
        val (iv2, cipher2) = BridgeCrypto.encrypt(plaintext, key)

        assertEquals(BridgeCrypto.GCM_IV_LENGTH, iv1.size)
        assertFalse("Each encryption must use a fresh random IV", iv1.contentEquals(iv2))
        assertArrayEquals(plaintext, BridgeCrypto.decrypt(iv1, cipher1, key))
        assertArrayEquals(plaintext, BridgeCrypto.decrypt(iv2, cipher2, key))
    }

    @Test
    fun aes256GcmRejectsWrongKeyAndTamperedCiphertext() {
        val keyA = BridgeCrypto.generateSessionKey()
        val keyB = BridgeCrypto.generateSessionKey()
        val (iv, cipher) = BridgeCrypto.encrypt("secret-otp-8899".toByteArray(), keyA)

        try {
            BridgeCrypto.decrypt(iv, cipher, keyB)
            fail("Expected AEADBadTagException for wrong key")
        } catch (_: AEADBadTagException) {
            // Expected
        }

        val tampered = cipher.copyOf().apply { this[0] = (this[0].toInt() xor 0xFF).toByte() }
        try {
            BridgeCrypto.decrypt(iv, tampered, keyA)
            fail("Expected AEADBadTagException for tampered ciphertext")
        } catch (_: AEADBadTagException) {
            // Expected
        }
    }

    @Test
    fun pairCodeDerivationAndFingerprintAreDeterministic() {
        val code = BridgeCrypto.generatePairCode()
        assertEquals(6, code.length)
        assertTrue(code.all(Char::isDigit))

        val k1 = BridgeCrypto.deriveKeyFromPairCode("482915")
        val k2 = BridgeCrypto.deriveKeyFromPairCode(" 482915 ")
        val k3 = BridgeCrypto.deriveKeyFromPairCode("999999")
        assertEquals(k1, k2)
        assertNotEquals(k1, k3)

        val fp = BridgeCrypto.computeFingerprint(k1)
        assertEquals(9, fp.length)
        assertEquals('-', fp[4])
    }

    @Test
    fun encryptedBinaryFrameProtocolRoundtripAndFallbackKey() {
        val primaryKey = BridgeCrypto.generateSessionKey()
        val fallbackKey = BridgeCrypto.deriveKeyFromPairCode("654321")
        val jsonBytes = """{"cmd":"TELEMETRY","bat":82}""".toByteArray(Charsets.UTF_8)
        val pcmBytes = ByteArray(320) { (it % 127).toByte() }

        val buffer = ByteArrayOutputStream()
        val out = DataOutputStream(buffer)
        BridgeProtocol.writeEncryptedFrame(out, BridgeProtocol.FRAME_JSON, jsonBytes, primaryKey)
        BridgeProtocol.writeEncryptedFrame(out, BridgeProtocol.FRAME_AUDIO_PCM, pcmBytes, fallbackKey)

        val input = DataInputStream(ByteArrayInputStream(buffer.toByteArray()))
        val (frame1, usedKey1) = BridgeProtocol.readEncryptedFrame(input, primaryKey, fallbackKey)
        assertEquals(BridgeProtocol.FRAME_JSON, frame1.type)
        assertArrayEquals(jsonBytes, frame1.payload)
        assertEquals(primaryKey, usedKey1)

        val (frame2, usedKey2) = BridgeProtocol.readEncryptedFrame(input, primaryKey, fallbackKey)
        assertEquals(BridgeProtocol.FRAME_AUDIO_PCM, frame2.type)
        assertArrayEquals(pcmBytes, frame2.payload)
        assertEquals(fallbackKey, usedKey2)
    }

    @Test
    fun extractsPersianAndEnglishOtpCodes() {
        assertEquals("482915", DeviceTelemetryReader.extractOtpCode("کد تایید شما: ۴۸۲۹۱۵"))
        assertEquals("903412", DeviceTelemetryReader.extractOtpCode("Your verification code is 903412"))
        assertEquals("7712", DeviceTelemetryReader.extractOtpCode("رمز پویا 7712 می باشد"))
    }

    @Test
    fun normalizesPersianAndFormattedPhoneNumbers() {
        assertEquals("+989123456789", DeviceTelemetryReader.normalizePhoneNumber("+۹۸ ۹۱۲-۳۴۵-۶۷۸۹"))
        assertEquals("09121112233", DeviceTelemetryReader.normalizePhoneNumber("۰۹۱۲ ۱۱۱ ۲۲۳۳"))
    }

    @Test
    fun signalAndBatteryNotificationIndicatorsFormatAccurately() {
        assertEquals("○○○○", BridgeForegroundService.signalBarText(0))
        assertEquals("▂▄○○", BridgeForegroundService.signalBarText(2))
        assertEquals("▂▄▆█", BridgeForegroundService.signalBarText(4))
        assertEquals("▂▄▆█", BridgeForegroundService.signalBarText(9))

        assertEquals("[░░░░░░░░░░]", BridgeForegroundService.batteryBarText(0))
        assertEquals("[█████░░░░░]", BridgeForegroundService.batteryBarText(50))
        assertEquals("[██████████]", BridgeForegroundService.batteryBarText(100))
        assertEquals("[████████░░]", BridgeForegroundService.batteryBarText(82))
    }

    @Test
    fun localizesDigitsForPersianMode() {
        assertEquals("۱۲:۳۴", "12:34".localizeDigits(isPersian = true))
        assertEquals("12:34", "12:34".localizeDigits(isPersian = false))
    }

    @Test
    fun batteryTrendCalculatorComputesSummaryAndDrainRate() {
        assertNull(BatteryTrendCalculator.summarize(emptyList()))

        val baseTime = 1_700_000_000_000L
        val samples = listOf(
            BatteryHistoryEntity(1, "peer1", "Pixel", 85, false, baseTime),
            BatteryHistoryEntity(2, "peer1", "Pixel", 80, false, baseTime + 30 * 60_000L),
            BatteryHistoryEntity(3, "peer1", "Pixel", 75, false, baseTime + 60 * 60_000L)
        )
        val summary = BatteryTrendCalculator.summarize(samples)
        assertNotNull(summary)
        assertEquals(75, summary!!.currentPercent)
        assertEquals(75, summary.minPercent)
        assertEquals(85, summary.maxPercent)
        assertEquals(-10, summary.deltaPercent)
        assertEquals(3, summary.sampleCount)
        assertEquals("-10.0%/h", BatteryTrendCalculator.formatRatePerHour(summary.ratePerHour!!))
    }

    @Test
    fun batterySampleThrottlingRecordsChangesAndHeartbeatsOnly() {
        val now = 1_700_000_000_000L
        val prev = BatteryHistoryEntity(1, "peer1", "Host", 70, false, now)

        assertFalse(BatteryTrendCalculator.shouldRecordSample(prev, -1, false, now + 5_000L))
        assertTrue(BatteryTrendCalculator.shouldRecordSample(null, 70, false, now))
        assertFalse(BatteryTrendCalculator.shouldRecordSample(prev, 70, false, now + 10_000L))
        assertTrue(BatteryTrendCalculator.shouldRecordSample(prev, 69, false, now + 10_000L))
        assertTrue(BatteryTrendCalculator.shouldRecordSample(prev, 70, true, now + 10_000L))
        assertTrue(
            BatteryTrendCalculator.shouldRecordSample(
                prev,
                70,
                false,
                now + BatteryTrendCalculator.HEARTBEAT_INTERVAL_MS
            )
        )
    }
}
