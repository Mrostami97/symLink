package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.bridge.BridgeCrypto
import com.example.bridge.DeviceTelemetryReader
import com.example.bridge.PairingPayload
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `read app name from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("iTell", appName)
    }

    @Test
    fun `aes gcm encrypt decrypt roundtrip succeeds`() {
        val key = BridgeCrypto.generateSessionKey()
        val message = "iTell Bridge Encrypted Payload • سلام".toByteArray(Charsets.UTF_8)
        val (iv, cipher) = BridgeCrypto.encrypt(message, key)
        val decrypted = BridgeCrypto.decrypt(iv, cipher, key)
        assertEquals(String(message, Charsets.UTF_8), String(decrypted, Charsets.UTF_8))
    }

    @Test
    fun `qr pairing payload serializes and parses cleanly`() {
        val key = BridgeCrypto.generateSessionKey()
        val fp = BridgeCrypto.computeFingerprint(key)
        val payload = PairingPayload(
            deviceId = "dev_1",
            deviceName = "Pixel Host",
            hostIp = "192.168.43.1",
            port = 47470,
            btAddress = "AA:BB:CC:DD:EE:FF",
            sessionKeyBase64 = key,
            pairCode = "482915",
            fingerprint = fp
        )
        val parsed = PairingPayload.parseQrString(payload.toQrString())
        assertNotNull(parsed)
        assertEquals("192.168.43.1", parsed?.hostIp)
        assertEquals("482915", parsed?.pairCode)
        assertEquals(fp, parsed?.fingerprint)
    }

    @Test
    fun `extracts persian and english otp codes`() {
        assertEquals("482915", DeviceTelemetryReader.extractOtpCode("کد تایید شما: ۴۸۲۹۱۵"))
        assertEquals("903412", DeviceTelemetryReader.extractOtpCode("Your verification code is 903412"))
    }
}
