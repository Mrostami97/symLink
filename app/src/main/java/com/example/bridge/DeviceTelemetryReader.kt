package com.example.bridge

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.Uri
import android.os.BatteryManager
import android.os.Build
import android.provider.ContactsContract
import android.telephony.PhoneStateListener
import android.telephony.SignalStrength
import android.telephony.SubscriptionManager
import android.telephony.TelephonyCallback
import android.telephony.TelephonyManager
import androidx.core.content.ContextCompat
import com.example.data.local.SyncedContactEntity
import java.net.Inet4Address
import java.net.NetworkInterface
import kotlin.math.abs

class DeviceTelemetryReader(private val context: Context) {

    private var telephonyCallback: Any? = null
    @Suppress("DEPRECATION")
    private var legacyListener: PhoneStateListener? = null

    @Volatile
    private var lastObservedSignalLevel: Int = 0

    fun hasPhoneStatePermission(): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE) ==
            PackageManager.PERMISSION_GRANTED

    fun hasContactsPermission(): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) ==
            PackageManager.PERMISSION_GRANTED

    fun readBatteryStatus(): Pair<Int, Boolean> {
        val intent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val level = intent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = intent?.getIntExtra(BatteryManager.EXTRA_SCALE, 100) ?: 100
        val status = intent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        val percent = if (level >= 0 && scale > 0) ((level * 100f) / scale).toInt() else -1
        val charging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
            status == BatteryManager.BATTERY_STATUS_FULL
        return percent to charging
    }

    fun getLocalIpv4Address(): String {
        return runCatching {
            val interfaces = NetworkInterface.getNetworkInterfaces()?.toList().orEmpty()
            val candidates = interfaces
                .filter { it.isUp && !it.isLoopback }
                .flatMap { iface ->
                    iface.inetAddresses.toList().map { iface.name to it }
                }
                .filter { (_, addr) -> addr is Inet4Address && !addr.isLoopbackAddress }

            val preferred = candidates.firstOrNull { (name, _) ->
                name.startsWith("wlan") || name.startsWith("ap") || name.startsWith("swlan")
            } ?: candidates.firstOrNull()

            preferred?.second?.hostAddress ?: "127.0.0.1"
        }.getOrDefault("127.0.0.1")
    }

    @SuppressLint("MissingPermission")
    fun readSimCards(): List<SimCardInfo> {
        if (!hasPhoneStatePermission()) return emptyList()
        return runCatching {
            val subManager = context.getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE) as? SubscriptionManager
            val baseTm = context.getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager
            val activeSubs = subManager?.activeSubscriptionInfoList.orEmpty()
            if (activeSubs.isNotEmpty()) {
                activeSubs.mapIndexed { idx, info ->
                    val subTm = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        baseTm?.createForSubscriptionId(info.subscriptionId) ?: baseTm
                    } else {
                        baseTm
                    }
                    val sigLevel = readSignalLevel(subTm)
                    val networkLabel = readNetworkLabel(subTm)
                    SimCardInfo(
                        slotIndex = (info.simSlotIndex + 1).coerceIn(1, 2),
                        subscriptionId = info.subscriptionId,
                        carrierName = info.carrierName?.toString()?.takeIf { it.isNotBlank() }
                            ?: "SIM ${idx + 1}",
                        displayName = info.displayName?.toString()?.takeIf { it.isNotBlank() }
                            ?: "SIM ${idx + 1}",
                        networkType = networkLabel,
                        signalLevel = sigLevel,
                        isActive = true
                    )
                }.sortedBy { it.slotIndex }
            } else {
                val operator = baseTm?.networkOperatorName?.takeIf { it.isNotBlank() }
                if (operator != null) {
                    listOf(
                        SimCardInfo(
                            slotIndex = 1,
                            subscriptionId = 1,
                            carrierName = operator,
                            displayName = operator,
                            networkType = readNetworkLabel(baseTm),
                            signalLevel = readSignalLevel(baseTm),
                            isActive = true
                        )
                    )
                } else {
                    emptyList()
                }
            }
        }.getOrDefault(emptyList())
    }

    @SuppressLint("MissingPermission")
    private fun readSignalLevel(tm: TelephonyManager?): Int {
        if (tm == null) return lastObservedSignalLevel
        return runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                tm.signalStrength?.level?.coerceIn(0, 4) ?: lastObservedSignalLevel
            } else {
                lastObservedSignalLevel
            }
        }.getOrDefault(lastObservedSignalLevel)
    }

    @SuppressLint("MissingPermission")
    private fun readNetworkLabel(tm: TelephonyManager?): String {
        if (tm == null) return "SIM"
        return runCatching {
            when (tm.dataNetworkType) {
                TelephonyManager.NETWORK_TYPE_NR -> "5G"
                TelephonyManager.NETWORK_TYPE_LTE -> "4G LTE"
                TelephonyManager.NETWORK_TYPE_HSPAP,
                TelephonyManager.NETWORK_TYPE_HSPA,
                TelephonyManager.NETWORK_TYPE_UMTS -> "3G"
                TelephonyManager.NETWORK_TYPE_EDGE,
                TelephonyManager.NETWORK_TYPE_GPRS -> "2G"
                else -> "SIM"
            }
        }.getOrDefault("SIM")
    }

    fun captureTelemetry(deviceAlias: String, pingMs: Long = -1L): DeviceTelemetry {
        val (battery, charging) = readBatteryStatus()
        return DeviceTelemetry(
            deviceName = deviceAlias,
            batteryPercent = battery,
            isCharging = charging,
            localIp = getLocalIpv4Address(),
            simCards = readSimCards(),
            pingMs = pingMs,
            lastUpdatedAt = System.currentTimeMillis()
        )
    }

    @SuppressLint("MissingPermission")
    fun startTelephonyMonitoring(
        onCallStateChanged: (Int) -> Unit,
        onSignalChanged: (Int) -> Unit
    ) {
        if (!hasPhoneStatePermission()) return
        stopTelephonyMonitoring()
        val tm = context.getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager ?: return
        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val cb = object : TelephonyCallback(),
                    TelephonyCallback.CallStateListener,
                    TelephonyCallback.SignalStrengthsListener {
                    override fun onCallStateChanged(state: Int) {
                        onCallStateChanged(state)
                    }

                    override fun onSignalStrengthsChanged(signalStrength: SignalStrength) {
                        val level = signalStrength.level.coerceIn(0, 4)
                        lastObservedSignalLevel = level
                        onSignalChanged(level)
                    }
                }
                tm.registerTelephonyCallback(context.mainExecutor, cb)
                telephonyCallback = cb
            } else {
                @Suppress("DEPRECATION")
                val listener = object : PhoneStateListener() {
                    @Deprecated("Deprecated in Java")
                    override fun onCallStateChanged(state: Int, phoneNumber: String?) {
                        onCallStateChanged(state)
                    }

                    @Deprecated("Deprecated in Java")
                    override fun onSignalStrengthsChanged(signalStrength: SignalStrength?) {
                        val level = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                            signalStrength?.level?.coerceIn(0, 4) ?: 0
                        } else 2
                        lastObservedSignalLevel = level
                        onSignalChanged(level)
                    }
                }
                @Suppress("DEPRECATION")
                tm.listen(
                    listener,
                    PhoneStateListener.LISTEN_CALL_STATE or PhoneStateListener.LISTEN_SIGNAL_STRENGTHS
                )
                legacyListener = listener
            }
        }
    }

    fun stopTelephonyMonitoring() {
        val tm = context.getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager ?: return
        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                (telephonyCallback as? TelephonyCallback)?.let { tm.unregisterTelephonyCallback(it) }
                telephonyCallback = null
            } else {
                @Suppress("DEPRECATION")
                legacyListener?.let { tm.listen(it, PhoneStateListener.LISTEN_NONE) }
                legacyListener = null
            }
        }
    }

    fun readContactFromPickerUri(contactUri: Uri): SyncedContactEntity? {
        return runCatching {
            val resolver = context.contentResolver
            var displayName = ""
            var phone = ""
            var contactId: String? = null

            resolver.query(contactUri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val nameIdx = cursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME)
                    if (nameIdx >= 0) displayName = cursor.getString(nameIdx).orEmpty()
                    val numIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                    if (numIdx >= 0) phone = cursor.getString(numIdx).orEmpty()
                    val idIdx = cursor.getColumnIndex(ContactsContract.Contacts._ID)
                    if (idIdx >= 0) contactId = cursor.getString(idIdx)
                }
            }

            if (phone.isBlank() && contactId != null && hasContactsPermission()) {
                resolver.query(
                    ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                    arrayOf(ContactsContract.CommonDataKinds.Phone.NUMBER),
                    "${ContactsContract.CommonDataKinds.Phone.CONTACT_ID} = ?",
                    arrayOf(contactId),
                    null
                )?.use { pCursor ->
                    if (pCursor.moveToFirst()) {
                        phone = pCursor.getString(0).orEmpty()
                    }
                }
            }

            if (phone.isBlank()) return null
            val normalized = normalizePhoneNumber(phone)
            SyncedContactEntity(
                normalizedNumber = normalized,
                displayName = displayName.ifBlank { phone },
                phoneNumber = phone.trim(),
                avatarColorIndex = abs(normalized.hashCode()) % 6
            )
        }.getOrNull()
    }

    @SuppressLint("MissingPermission")
    fun readAddressBookContacts(limit: Int = 250): List<SyncedContactEntity> {
        if (!hasContactsPermission()) return emptyList()
        val result = LinkedHashMap<String, SyncedContactEntity>()
        runCatching {
            context.contentResolver.query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                arrayOf(
                    ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                    ContactsContract.CommonDataKinds.Phone.NUMBER
                ),
                null,
                null,
                "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} ASC"
            )?.use { cursor ->
                val nameCol = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
                val numCol = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                while (cursor.moveToNext() && result.size < limit) {
                    val name = if (nameCol >= 0) cursor.getString(nameCol).orEmpty().trim() else ""
                    val number = if (numCol >= 0) cursor.getString(numCol).orEmpty().trim() else ""
                    val normalized = normalizePhoneNumber(number)
                    if (normalized.length >= 3 && !result.containsKey(normalized)) {
                        result[normalized] = SyncedContactEntity(
                            normalizedNumber = normalized,
                            displayName = name.ifBlank { number },
                            phoneNumber = number,
                            avatarColorIndex = abs(normalized.hashCode()) % 6
                        )
                    }
                }
            }
        }
        return result.values.toList()
    }

    companion object {
        fun normalizePhoneNumber(raw: String): String {
            val western = toWesternDigits(raw)
            return western.filter { it.isDigit() || it == '+' }
        }

        fun toWesternDigits(input: String): String = buildString(input.length) {
            for (ch in input) {
                when (ch) {
                    in '۰'..'۹' -> append(('0'.code + (ch.code - '۰'.code)).toChar())
                    in '٠'..'٩' -> append(('0'.code + (ch.code - '٠'.code)).toChar())
                    else -> append(ch)
                }
            }
        }

        fun extractOtpCode(text: String): String? {
            val normalized = toWesternDigits(text)
            val labeledRegex = Regex(
                """(?:code|otp|pin|passcode|verification|رمز|کد|تایید|تأیید)\D{0,15}(\d{4,8})\b""",
                RegexOption.IGNORE_CASE
            )
            labeledRegex.find(normalized)?.groupValues?.getOrNull(1)?.let { return it }

            val standaloneRegex = Regex("""(?<!\d)(\d{4,8})(?!\d)""")
            return standaloneRegex.find(normalized)?.groupValues?.getOrNull(1)
        }
    }
}
