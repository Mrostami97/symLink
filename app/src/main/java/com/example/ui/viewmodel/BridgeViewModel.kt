package com.example.ui.viewmodel

import android.app.Application
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.telephony.TelephonyManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.bridge.BatteryTimeWindow
import com.example.bridge.BatteryTrendCalculator
import com.example.bridge.BridgeCrypto
import com.example.bridge.BridgeForegroundService
import com.example.bridge.BridgeProtocol
import com.example.bridge.BridgeSessionHub
import com.example.bridge.ConnectionState
import com.example.bridge.DeviceRole
import com.example.bridge.DeviceTelemetry
import com.example.bridge.DeviceTelemetryReader
import com.example.bridge.DiscoveredPeer
import com.example.bridge.HandoffType
import com.example.bridge.ITellTelecomManager
import com.example.bridge.IncomingCallBanner
import com.example.bridge.PairingPayload
import com.example.bridge.PendingHandoff
import com.example.bridge.TransportMode
import com.example.bridge.VoiceTunnelState
import com.example.ui.i18n.localizeDigits
import com.example.data.local.BatteryHistoryEntity
import com.example.data.local.BridgeCallEventEntity
import com.example.data.local.BridgeDatabase
import com.example.data.local.BridgeMessageEntity
import com.example.data.local.PairedDeviceEntity
import com.example.data.local.SyncedContactEntity
import com.example.data.preferences.UserPreferences
import com.example.data.preferences.UserPreferencesRepository
import com.example.data.repository.BridgeRepository
import com.example.ui.i18n.stringsForLanguage
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

class BridgeViewModel(application: Application) : AndroidViewModel(application) {

    private val appContext = application.applicationContext
    private val db = BridgeDatabase.getInstance(appContext)
    private val repository = BridgeRepository(db.bridgeDao())
    private val prefsRepo = UserPreferencesRepository(appContext)
    val telemetryReader = DeviceTelemetryReader(appContext)
    private val transport = BridgeSessionHub.transport(appContext)
    private val voiceBridge = BridgeSessionHub.voiceBridge(appContext)

    val preferences: StateFlow<UserPreferences> = prefsRepo.preferencesFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = UserPreferences()
    )

    val connectionState: StateFlow<ConnectionState> = transport.connectionState
    val discoveredPeers: StateFlow<List<DiscoveredPeer>> = transport.discoveredPeers
    val hostPairingPayload: StateFlow<PairingPayload?> = transport.hostPairingPayload
    val latencyMs: StateFlow<Long> = transport.latencyMs
    val voiceTunnelState: StateFlow<VoiceTunnelState> = voiceBridge.state

    val pairedDevices: StateFlow<List<PairedDeviceEntity>> = repository.pairedDevices.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList()
    )

    val messages: StateFlow<List<BridgeMessageEntity>> = repository.messages.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList()
    )

    val latestOtpMessage: StateFlow<BridgeMessageEntity?> = repository.messages
        .map { list -> list.firstOrNull { !it.extractedOtp.isNullOrBlank() } }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = null
        )

    val callEvents: StateFlow<List<BridgeCallEventEntity>> = repository.callEvents.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList()
    )

    val contacts: StateFlow<List<SyncedContactEntity>> = repository.contacts.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList()
    )

    private val _localTelemetry = MutableStateFlow(telemetryReader.captureTelemetry(Build.MODEL ?: "Android"))
    val localTelemetry: StateFlow<DeviceTelemetry> = _localTelemetry.asStateFlow()

    private val _peerTelemetry = MutableStateFlow<DeviceTelemetry?>(null)
    val peerTelemetry: StateFlow<DeviceTelemetry?> = _peerTelemetry.asStateFlow()

    private val _incomingCallBanner = MutableStateFlow(IncomingCallBanner())
    val incomingCallBanner: StateFlow<IncomingCallBanner> = _incomingCallBanner.asStateFlow()

    private val _pendingHandoffs = MutableStateFlow<List<PendingHandoff>>(emptyList())
    val pendingHandoffs: StateFlow<List<PendingHandoff>> = _pendingHandoffs.asStateFlow()

    private val _isCallingAccountEnabled = MutableStateFlow(ITellTelecomManager.isCallingAccountEnabled(appContext))
    val isCallingAccountEnabled: StateFlow<Boolean> = _isCallingAccountEnabled.asStateFlow()

    private val _selectedBatteryWindow = MutableStateFlow(BatteryTimeWindow.ONE_HOUR)
    val selectedBatteryWindow: StateFlow<BatteryTimeWindow> = _selectedBatteryWindow.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val batteryHistory: StateFlow<List<BatteryHistoryEntity>> = combine(
        preferences,
        connectionState,
        _selectedBatteryWindow
    ) { prefs, conn, window ->
        Triple(prefs.deviceRole, conn, window)
    }.flatMapLatest { (roleSetting, conn, window) ->
        val since = System.currentTimeMillis() - window.durationMs
        when {
            roleSetting == DeviceRole.COMPANION.name && conn is ConnectionState.Connected -> {
                repository.observeBatteryHistoryForDevice(conn.peerDeviceId, since)
            }
            roleSetting == DeviceRole.COMPANION.name -> {
                repository.observePeerBatteryHistory(BatteryTrendCalculator.LOCAL_HOST_DEVICE_ID, since)
            }
            else -> {
                repository.observeBatteryHistoryForDevice(BatteryTrendCalculator.LOCAL_HOST_DEVICE_ID, since)
            }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList()
    )

    private val _snackbarEvents = MutableSharedFlow<String>(extraBufferCapacity = 16)
    val snackbarEvents: SharedFlow<String> = _snackbarEvents.asSharedFlow()

    private val lastRecordedBatteryByDevice = ConcurrentHashMap<String, BatteryHistoryEntity>()
    private var lastPruneTimestamp: Long = 0L
    private var telemetryLoopJob: Job? = null
    private var autoReconnectAttempted = false
    private var lastSavedPairingKey: String? = null
    private var lastSavedHostIp: String = ""
    private var lastSavedPort: Int = BridgeProtocol.DEFAULT_PORT
    private var lastSavedBtAddr: String = ""

    init {
        ITellTelecomManager.registerCallingAccount(appContext)
        refreshCallingAccountStatus()
        observePreferencesAndAutoReconnect()
        observeIncomingCommands()
        observeIncomingAudio()
        observeConnectionLifecycle()
        startLocalTelemetryTicker()
    }

    private fun observePreferencesAndAutoReconnect() {
        viewModelScope.launch {
            prefsRepo.preferencesFlow.collect { pref ->
                BridgeSessionHub.languageCode = pref.languageCode
                BridgeSessionHub.darkTheme = pref.darkTheme
                BridgeSessionHub.preferredSimSlot = pref.preferredSimSlot

                if (!autoReconnectAttempted && pref.deviceRole.isNotBlank()) {
                    autoReconnectAttempted = true
                    if (pref.autoReconnect && connectionState.value is ConnectionState.Idle) {
                        if (pref.deviceRole == DeviceRole.COMPANION.name) {
                            val latestHost = repository.pairedDevices.firstOrNull()?.firstOrNull()
                            if (latestHost != null) {
                                reconnectSavedDevice(latestHost)
                            }
                        } else if (pref.deviceRole == DeviceRole.HOST.name) {
                            startHostBridge()
                        }
                    }
                }
            }
        }
    }

    fun refreshCallingAccountStatus() {
        _isCallingAccountEnabled.value = ITellTelecomManager.isCallingAccountEnabled(appContext)
    }

    fun openCallingAccountSettings() {
        ITellTelecomManager.openCallingAccountSettings(appContext)
    }

    fun openDefaultPhoneApp() {
        ITellTelecomManager.openDefaultPhoneApp(appContext)
    }

    fun openDefaultMessagesApp() {
        ITellTelecomManager.openDefaultMessagesApp(appContext)
    }

    private fun currentStrings() = stringsForLanguage(preferences.value.languageCode)

    fun currentRole(): DeviceRole =
        if (preferences.value.deviceRole == DeviceRole.COMPANION.name) DeviceRole.COMPANION else DeviceRole.HOST

    fun selectRole(role: DeviceRole) {
        viewModelScope.launch {
            transport.stopAll()
            voiceBridge.stop()
            BridgeForegroundService.stop(appContext)
            _peerTelemetry.value = null
            _incomingCallBanner.value = IncomingCallBanner()
            prefsRepo.setDeviceRole(role.name)
            ITellTelecomManager.registerCallingAccount(appContext)
            refreshCallingAccountStatus()
            if (role == DeviceRole.HOST) {
                telemetryReader.startTelephonyMonitoring(::onHostCallStateChanged) {
                    sendLocalTelemetryNow()
                }
            } else {
                telemetryReader.stopTelephonyMonitoring()
            }
        }
    }

    fun setLanguage(code: String) {
        viewModelScope.launch { prefsRepo.setLanguage(code) }
    }

    fun setDarkTheme(enabled: Boolean) {
        viewModelScope.launch { prefsRepo.setDarkTheme(enabled) }
    }

    fun setDeviceAlias(alias: String) {
        viewModelScope.launch { prefsRepo.setDeviceAlias(alias) }
    }

    fun setPreferredSimSlot(slot: Int) {
        BridgeSessionHub.preferredSimSlot = slot
        viewModelScope.launch { prefsRepo.setPreferredSimSlot(slot) }
    }

    fun setAutoReconnect(enabled: Boolean) {
        viewModelScope.launch { prefsRepo.setAutoReconnect(enabled) }
    }

    fun setVibrateOnIncoming(enabled: Boolean) {
        viewModelScope.launch { prefsRepo.setVibrateOnIncoming(enabled) }
    }

    fun selectBatteryWindow(window: BatteryTimeWindow) {
        _selectedBatteryWindow.value = window
    }

    private suspend fun maybeRecordBatterySample(
        deviceId: String,
        deviceName: String,
        batteryPercent: Int,
        isCharging: Boolean
    ) {
        val now = System.currentTimeMillis()
        val last = lastRecordedBatteryByDevice[deviceId]
        if (!BatteryTrendCalculator.shouldRecordSample(last, batteryPercent, isCharging, now)) return
        val sample = BatteryHistoryEntity(
            deviceId = deviceId,
            deviceName = deviceName.ifBlank { deviceId },
            batteryPercent = batteryPercent,
            isCharging = isCharging,
            timestamp = now
        )
        lastRecordedBatteryByDevice[deviceId] = sample
        repository.recordBatterySample(sample)
        if (now - lastPruneTimestamp > 30L * 60_000L) {
            lastPruneTimestamp = now
            repository.pruneOldBatterySamples(now - BatteryTrendCalculator.RETENTION_MS)
        }
    }

    fun refreshLocalTelemetry() {
        _localTelemetry.value = telemetryReader.captureTelemetry(
            deviceAlias = preferences.value.deviceAlias,
            pingMs = latencyMs.value
        )
        if (currentRole() == DeviceRole.HOST && telemetryReader.hasPhoneStatePermission()) {
            telemetryReader.startTelephonyMonitoring(::onHostCallStateChanged) {
                sendLocalTelemetryNow()
            }
        }
    }

    fun startHostBridge() {
        refreshLocalTelemetry()
        val alias = preferences.value.deviceAlias
        val ip = telemetryReader.getLocalIpv4Address()
        transport.startHosting(alias, ip)
        updateLiveSignalNotification(
            telemetry = _localTelemetry.value,
            isConnected = false,
            peerName = null,
            hostIp = ip
        )
        if (telemetryReader.hasPhoneStatePermission()) {
            telemetryReader.startTelephonyMonitoring(::onHostCallStateChanged) {
                sendLocalTelemetryNow()
            }
        }
    }

    fun stopBridge() {
        voiceBridge.stop()
        transport.stopAll()
        telemetryReader.stopTelephonyMonitoring()
        BridgeForegroundService.stop(appContext)
        BridgeSessionHub.latestPeerTelemetry = null
        _peerTelemetry.value = null
        _incomingCallBanner.value = IncomingCallBanner()
    }

    fun startNearbyDiscovery() {
        transport.startDiscovery()
    }

    fun refreshBluetoothDevices() {
        transport.refreshPairedBluetoothPeers()
    }

    fun connectFromQr(qrRaw: String): Boolean {
        val payload = PairingPayload.parseQrString(qrRaw) ?: return false
        lastSavedPairingKey = payload.sessionKeyBase64
        lastSavedHostIp = payload.hostIp
        lastSavedPort = payload.port
        lastSavedBtAddr = payload.btAddress
        val alias = preferences.value.deviceAlias
        if (payload.hostIp.isNotBlank() && payload.hostIp != "127.0.0.1") {
            transport.connectViaWifi(payload.hostIp, payload.port, payload.sessionKeyBase64, alias)
        } else if (payload.btAddress.isNotBlank()) {
            transport.connectViaBluetooth(payload.btAddress, payload.sessionKeyBase64, alias)
        } else {
            return false
        }
        return true
    }

    fun connectManually(hostIp: String, port: Int, pairCode: String) {
        val cleanIp = DeviceTelemetryReader.toWesternDigits(hostIp).trim()
        val cleanCode = DeviceTelemetryReader.toWesternDigits(pairCode).trim()
        if (cleanIp.isBlank() || cleanCode.length < 4) return
        val derivedKey = BridgeCrypto.deriveKeyFromPairCode(cleanCode)
        lastSavedPairingKey = derivedKey
        lastSavedHostIp = cleanIp
        lastSavedPort = port
        lastSavedBtAddr = ""
        transport.connectViaWifi(cleanIp, port, derivedKey, preferences.value.deviceAlias)
    }

    fun connectToDiscoveredPeer(peer: DiscoveredPeer, pairCode: String) {
        val cleanCode = DeviceTelemetryReader.toWesternDigits(pairCode).trim()
        val key = BridgeCrypto.deriveKeyFromPairCode(cleanCode)
        lastSavedPairingKey = key
        lastSavedHostIp = peer.hostIp
        lastSavedPort = peer.port
        lastSavedBtAddr = peer.btAddress
        when (peer.transport) {
            TransportMode.WIFI_TCP -> {
                transport.connectViaWifi(peer.hostIp, peer.port, key, preferences.value.deviceAlias)
            }
            TransportMode.BLUETOOTH_RFCOMM -> {
                transport.connectViaBluetooth(peer.btAddress, key, preferences.value.deviceAlias)
            }
        }
    }

    fun reconnectSavedDevice(device: PairedDeviceEntity) {
        lastSavedPairingKey = device.sessionKeyBase64
        lastSavedHostIp = device.ipAddress
        lastSavedPort = device.port
        lastSavedBtAddr = device.bluetoothAddress
        val alias = preferences.value.deviceAlias
        if (device.ipAddress.isNotBlank() && device.ipAddress != "127.0.0.1") {
            transport.connectViaWifi(device.ipAddress, device.port, device.sessionKeyBase64, alias)
        } else if (device.bluetoothAddress.isNotBlank()) {
            transport.connectViaBluetooth(device.bluetoothAddress, device.sessionKeyBase64, alias)
        }
    }

    fun forgetPairedDevice(deviceId: String) {
        viewModelScope.launch { repository.forgetPairedDevice(deviceId) }
    }

    // --- Call Bridge & Dialer ---
    fun requestDial(phoneNumber: String, simSlot: Int) {
        val cleanNumber = DeviceTelemetryReader.normalizePhoneNumber(phoneNumber)
        if (cleanNumber.isBlank()) return
        val contactName = contacts.value.firstOrNull { it.normalizedNumber == cleanNumber }?.displayName
            ?: cleanNumber

        if (currentRole() == DeviceRole.HOST) {
            launchSystemDialer(cleanNumber)
            viewModelScope.launch {
                repository.recordCallEvent(
                    BridgeCallEventEntity(
                        phoneNumber = cleanNumber,
                        contactName = contactName,
                        simSlot = simSlot,
                        eventType = "REMOTE_DIAL_REQUEST"
                    )
                )
            }
            return
        }

        val sent = transport.sendJson(
            BridgeProtocol.CMD_DIAL_REQUEST,
            JSONObject().apply {
                put("number", cleanNumber)
                put("simSlot", simSlot)
                put("contactName", contactName)
            }
        )
        viewModelScope.launch {
            if (sent) {
                repository.recordCallEvent(
                    BridgeCallEventEntity(
                        phoneNumber = cleanNumber,
                        contactName = contactName,
                        simSlot = simSlot,
                        eventType = "REMOTE_DIAL_REQUEST"
                    )
                )
                _snackbarEvents.emit("${currentStrings().sendDialToHost} • $cleanNumber")
            } else {
                _snackbarEvents.emit(currentStrings().connectFirstHint)
            }
        }
    }

    fun launchSystemDialer(phoneNumber: String) {
        val clean = DeviceTelemetryReader.normalizePhoneNumber(phoneNumber)
        val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$clean")).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        runCatching { appContext.startActivity(intent) }
    }

    fun launchSystemSmsApp(phoneNumber: String, body: String) {
        val clean = DeviceTelemetryReader.normalizePhoneNumber(phoneNumber)
        val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:$clean")).apply {
            putExtra("sms_body", body)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        runCatching { appContext.startActivity(intent) }
    }

    fun executePendingHandoff(handoff: PendingHandoff) {
        when (handoff.type) {
            HandoffType.DIAL_REQUEST -> launchSystemDialer(handoff.phoneNumber)
            HandoffType.SMS_DRAFT -> {
                launchSystemSmsApp(handoff.phoneNumber, handoff.body)
                transport.sendJson(
                    BridgeProtocol.CMD_SMS_HANDOFF_ACK,
                    JSONObject().apply { put("handoffId", handoff.id) }
                )
            }
        }
        dismissPendingHandoff(handoff.id)
    }

    fun dismissPendingHandoff(id: String) {
        _pendingHandoffs.update { list -> list.filterNot { it.id == id } }
    }

    fun dismissIncomingCallBanner() {
        _incomingCallBanner.value = IncomingCallBanner()
    }

    // --- Messages, OTP Relay & SMS Handoff ---
    fun sendDirectRelayMessage(body: String, simSlot: Int) {
        val trimmed = body.trim()
        if (trimmed.isBlank()) return
        val otp = DeviceTelemetryReader.extractOtpCode(trimmed)
        val msgType = if (otp != null) "OTP_CODE" else "RELAY_CHAT"
        val cmd = if (otp != null) BridgeProtocol.CMD_OTP_RELAY else BridgeProtocol.CMD_MESSAGE_RELAY
        val peerName = (connectionState.value as? ConnectionState.Connected)?.peerName
            ?: preferences.value.deviceAlias

        viewModelScope.launch {
            val rowId = repository.saveMessage(
                BridgeMessageEntity(
                    peerAddress = "",
                    peerName = peerName,
                    body = trimmed,
                    simSlot = simSlot,
                    messageType = msgType,
                    extractedOtp = otp,
                    isOutgoing = true,
                    deliveryStatus = "SENT"
                )
            )
            val sent = transport.sendJson(
                cmd,
                JSONObject().apply {
                    put("body", trimmed)
                    put("simSlot", simSlot)
                    put("otp", otp ?: "")
                    put("sender", preferences.value.deviceAlias)
                }
            )
            if (sent) {
                repository.markMessageStatus(rowId, "DELIVERED")
            } else {
                _snackbarEvents.emit(currentStrings().connectFirstHint)
            }
        }
    }

    fun sendSmsHandoffDraft(recipient: String, body: String, simSlot: Int) {
        val cleanRecipient = DeviceTelemetryReader.normalizePhoneNumber(recipient)
        val trimmedBody = body.trim()
        if (cleanRecipient.isBlank() || trimmedBody.isBlank()) return
        val contactName = contacts.value.firstOrNull { it.normalizedNumber == cleanRecipient }?.displayName
            ?: cleanRecipient

        if (currentRole() == DeviceRole.HOST) {
            launchSystemSmsApp(cleanRecipient, trimmedBody)
            viewModelScope.launch {
                repository.saveMessage(
                    BridgeMessageEntity(
                        peerAddress = cleanRecipient,
                        peerName = contactName,
                        body = trimmedBody,
                        simSlot = simSlot,
                        messageType = "SMS_HANDOFF",
                        isOutgoing = true,
                        deliveryStatus = "HANDED_OFF"
                    )
                )
            }
            return
        }

        val handoffId = UUID.randomUUID().toString()
        viewModelScope.launch {
            val rowId = repository.saveMessage(
                BridgeMessageEntity(
                    peerAddress = cleanRecipient,
                    peerName = contactName,
                    body = trimmedBody,
                    simSlot = simSlot,
                    messageType = "SMS_HANDOFF",
                    isOutgoing = true,
                    deliveryStatus = "SENT"
                )
            )
            val sent = transport.sendJson(
                BridgeProtocol.CMD_SMS_HANDOFF,
                JSONObject().apply {
                    put("handoffId", handoffId)
                    put("recipient", cleanRecipient)
                    put("body", trimmedBody)
                    put("simSlot", simSlot)
                    put("rowId", rowId)
                }
            )
            if (sent) {
                repository.markMessageStatus(rowId, "DELIVERED")
            } else {
                _snackbarEvents.emit(currentStrings().connectFirstHint)
            }
        }
    }

    fun relayClipboardOtp() {
        val clipboard = appContext.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        val clipText = clipboard?.primaryClip?.getItemAt(0)?.coerceToText(appContext)?.toString()?.trim()
        if (clipText.isNullOrBlank()) {
            viewModelScope.launch { _snackbarEvents.emit(currentStrings().noClipboardOtpFound) }
            return
        }
        sendDirectRelayMessage(clipText, preferences.value.preferredSimSlot)
    }

    fun handleIncomingSharedText(sharedText: String) {
        if (sharedText.isBlank()) return
        sendDirectRelayMessage(sharedText, preferences.value.preferredSimSlot)
        viewModelScope.launch { _snackbarEvents.emit(currentStrings().sharedTextRelayed) }
    }

    // --- Voice Audio Tunnel ---
    fun toggleVoiceTunnel(): Boolean {
        if (voiceBridge.state.value.isActive) {
            val duration = voiceBridge.state.value.durationSeconds
            voiceBridge.stop()
            transport.sendJson(BridgeProtocol.CMD_VOICE_STOP)
            val peerName = (connectionState.value as? ConnectionState.Connected)?.peerName ?: "Peer"
            viewModelScope.launch {
                repository.recordCallEvent(
                    BridgeCallEventEntity(
                        phoneNumber = peerName,
                        contactName = peerName,
                        simSlot = preferences.value.preferredSimSlot,
                        eventType = "VOICE_TUNNEL",
                        durationSeconds = duration
                    )
                )
            }
            return true
        } else {
            if (connectionState.value !is ConnectionState.Connected) {
                viewModelScope.launch { _snackbarEvents.emit(currentStrings().connectFirstHint) }
                return false
            }
            val started = voiceBridge.start()
            if (started) {
                transport.sendJson(BridgeProtocol.CMD_VOICE_START)
            }
            return started
        }
    }

    fun toggleVoiceMute() = voiceBridge.toggleMute()
    fun toggleVoiceSpeaker() = voiceBridge.toggleSpeaker()

    // --- Contacts Sync ---
    fun importPickedContact(uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            val contact = telemetryReader.readContactFromPickerUri(uri) ?: return@launch
            repository.saveContact(contact)
            pushContactsToPeer(listOf(contact))
            _snackbarEvents.emit("1 ${currentStrings().contactsSyncedCount}")
        }
    }

    fun syncFullAddressBook() {
        viewModelScope.launch(Dispatchers.IO) {
            val list = telemetryReader.readAddressBookContacts()
            if (list.isNotEmpty()) {
                repository.saveContacts(list)
                pushContactsToPeer(list)
                _snackbarEvents.emit("${list.size} ${currentStrings().contactsSyncedCount}")
            }
        }
    }

    fun pushAllSavedContactsToPeer() {
        viewModelScope.launch(Dispatchers.IO) {
            val list = repository.getContactsSnapshot()
            if (list.isNotEmpty()) {
                val sent = pushContactsToPeer(list)
                if (sent) {
                    _snackbarEvents.emit("${list.size} ${currentStrings().contactsSyncedCount}")
                } else {
                    _snackbarEvents.emit(currentStrings().connectFirstHint)
                }
            }
        }
    }

    private fun pushContactsToPeer(list: List<SyncedContactEntity>): Boolean {
        val arr = JSONArray()
        list.take(200).forEach { c ->
            arr.put(
                JSONObject().apply {
                    put("name", c.displayName)
                    put("num", c.phoneNumber)
                    put("norm", c.normalizedNumber)
                    put("color", c.avatarColorIndex)
                }
            )
        }
        return transport.sendJson(
            BridgeProtocol.CMD_CONTACTS_SYNC,
            JSONObject().apply { put("contacts", arr) }
        )
    }

    // --- Internal Protocol Handlers ---
    private fun onHostCallStateChanged(state: Int) {
        if (currentRole() != DeviceRole.HOST) return
        val stateName = when (state) {
            TelephonyManager.CALL_STATE_RINGING -> "RINGING"
            TelephonyManager.CALL_STATE_OFFHOOK -> "OFFHOOK"
            else -> "IDLE"
        }
        transport.sendJson(
            BridgeProtocol.CMD_CALL_STATE,
            JSONObject().apply {
                put("state", stateName)
                put("simSlot", preferences.value.preferredSimSlot)
            }
        )
        if (state == TelephonyManager.CALL_STATE_RINGING) {
            viewModelScope.launch {
                repository.recordCallEvent(
                    BridgeCallEventEntity(
                        phoneNumber = currentStrings().callerUnknown,
                        contactName = currentStrings().callerUnknown,
                        simSlot = preferences.value.preferredSimSlot,
                        eventType = "INCOMING_ALERT"
                    )
                )
            }
        }
    }

    private fun observeIncomingCommands() {
        viewModelScope.launch {
            transport.incomingJsonCommands.collect { json ->
                when (json.optString("cmd")) {
                    BridgeProtocol.CMD_TELEMETRY -> {
                        val snapshot = DeviceTelemetry.fromJson(json, latencyMs.value)
                        _peerTelemetry.value = snapshot
                        BridgeSessionHub.latestPeerTelemetry = snapshot
                        val connected = connectionState.value as? ConnectionState.Connected
                        if (connected != null) {
                            maybeRecordBatterySample(
                                deviceId = connected.peerDeviceId,
                                deviceName = connected.peerName,
                                batteryPercent = snapshot.batteryPercent,
                                isCharging = snapshot.isCharging
                            )
                            if (currentRole() == DeviceRole.COMPANION) {
                                updateLiveSignalNotification(
                                    telemetry = snapshot,
                                    isConnected = true,
                                    peerName = connected.peerName
                                )
                            }
                        }
                    }
                    BridgeProtocol.CMD_DIAL_REQUEST -> {
                        val number = json.optString("number")
                        val simSlot = json.optInt("simSlot", 1)
                        val contactName = json.optString("contactName", number)
                        if (number.isNotBlank()) {
                            val handoff = PendingHandoff(
                                id = UUID.randomUUID().toString(),
                                type = HandoffType.DIAL_REQUEST,
                                phoneNumber = number,
                                simSlot = simSlot
                            )
                            _pendingHandoffs.update { listOf(handoff) + it }
                            repository.recordCallEvent(
                                BridgeCallEventEntity(
                                    phoneNumber = number,
                                    contactName = contactName,
                                    simSlot = simSlot,
                                    eventType = "REMOTE_DIAL_REQUEST"
                                )
                            )
                            BridgeForegroundService.postAlertNotification(
                                appContext,
                                currentStrings().incomingDialRequest,
                                "$contactName • SIM $simSlot"
                            )
                            // Launch system dialer directly on Host (ACTION_DIAL is zero-permission compliant)
                            launchSystemDialer(number)
                        }
                    }
                    BridgeProtocol.CMD_CALL_STATE -> {
                        val callState = json.optString("state", "IDLE")
                        val simSlot = json.optInt("simSlot", 1)
                        when (callState) {
                            "RINGING" -> {
                                _incomingCallBanner.value = IncomingCallBanner(
                                    isActive = true,
                                    isRinging = true,
                                    phoneNumberHint = currentStrings().callerUnknown,
                                    simSlot = simSlot
                                )
                                if (preferences.value.vibrateOnIncoming) triggerIncomingVibration()
                                ITellTelecomManager.notifyIncomingHostCall(
                                    appContext,
                                    currentStrings().callerUnknown,
                                    simSlot
                                )
                                BridgeForegroundService.postAlertNotification(
                                    appContext,
                                    currentStrings().incomingCallOnHost,
                                    "SIM $simSlot"
                                )
                                repository.recordCallEvent(
                                    BridgeCallEventEntity(
                                        phoneNumber = currentStrings().callerUnknown,
                                        contactName = currentStrings().callerUnknown,
                                        simSlot = simSlot,
                                        eventType = "INCOMING_ALERT"
                                    )
                                )
                            }
                            "OFFHOOK" -> {
                                _incomingCallBanner.update {
                                    it.copy(isActive = true, isRinging = false, simSlot = simSlot)
                                }
                            }
                            else -> {
                                _incomingCallBanner.value = IncomingCallBanner()
                            }
                        }
                    }
                    BridgeProtocol.CMD_MESSAGE_RELAY, BridgeProtocol.CMD_OTP_RELAY -> {
                        val body = json.optString("body")
                        val simSlot = json.optInt("simSlot", 1)
                        val sender = json.optString("sender", "Peer")
                        val otp = json.optString("otp").ifBlank { DeviceTelemetryReader.extractOtpCode(body) }
                        if (body.isNotBlank()) {
                            repository.saveMessage(
                                BridgeMessageEntity(
                                    peerAddress = "",
                                    peerName = sender,
                                    body = body,
                                    simSlot = simSlot,
                                    messageType = if (otp != null) "OTP_CODE" else "RELAY_CHAT",
                                    extractedOtp = otp,
                                    isOutgoing = false,
                                    deliveryStatus = "DELIVERED"
                                )
                            )
                            if (otp != null) {
                                BridgeForegroundService.postOtpNotification(
                                    context = appContext,
                                    title = "${currentStrings().otpDetected}: $otp",
                                    otpCode = otp,
                                    fullBody = body,
                                    copyLabel = currentStrings().copyOtp
                                )
                            }
                        }
                    }
                    BridgeProtocol.CMD_SMS_HANDOFF -> {
                        val handoffId = json.optString("handoffId", UUID.randomUUID().toString())
                        val recipient = json.optString("recipient")
                        val body = json.optString("body")
                        val simSlot = json.optInt("simSlot", 1)
                        if (recipient.isNotBlank() && body.isNotBlank()) {
                            val handoff = PendingHandoff(
                                id = handoffId,
                                type = HandoffType.SMS_DRAFT,
                                phoneNumber = recipient,
                                body = body,
                                simSlot = simSlot
                            )
                            _pendingHandoffs.update { listOf(handoff) + it }
                            repository.saveMessage(
                                BridgeMessageEntity(
                                    peerAddress = recipient,
                                    peerName = recipient,
                                    body = body,
                                    simSlot = simSlot,
                                    messageType = "SMS_HANDOFF",
                                    isOutgoing = false,
                                    deliveryStatus = "DELIVERED"
                                )
                            )
                            BridgeForegroundService.postAlertNotification(
                                appContext,
                                currentStrings().incomingSmsRequest,
                                "$recipient: $body"
                            )
                        }
                    }
                    BridgeProtocol.CMD_CONTACTS_SYNC -> {
                        val arr = json.optJSONArray("contacts") ?: return@collect
                        val imported = buildList {
                            for (i in 0 until arr.length()) {
                                val obj = arr.optJSONObject(i) ?: continue
                                val norm = obj.optString("norm")
                                val num = obj.optString("num")
                                if (norm.isNotBlank() && num.isNotBlank()) {
                                    add(
                                        SyncedContactEntity(
                                            normalizedNumber = norm,
                                            displayName = obj.optString("name", num),
                                            phoneNumber = num,
                                            avatarColorIndex = obj.optInt("color", 0)
                                        )
                                    )
                                }
                            }
                        }
                        if (imported.isNotEmpty()) {
                            repository.saveContacts(imported)
                            _snackbarEvents.emit("${imported.size} ${currentStrings().contactsSyncedCount}")
                        }
                    }
                    BridgeProtocol.CMD_VOICE_START -> {
                        if (!voiceBridge.state.value.isActive && voiceBridge.hasRecordAudioPermission()) {
                            voiceBridge.start()
                        }
                    }
                    BridgeProtocol.CMD_VOICE_STOP -> {
                        voiceBridge.stop()
                    }
                }
            }
        }
    }

    private fun observeIncomingAudio() {
        viewModelScope.launch(Dispatchers.IO) {
            transport.incomingAudioFrames.collect { pcm ->
                voiceBridge.playIncomingPcm(pcm)
            }
        }
    }

    private fun observeConnectionLifecycle() {
        viewModelScope.launch {
            connectionState.collect { state ->
                val strings = currentStrings()
                when (state) {
                    is ConnectionState.Connected -> {
                        val activeTelemetry = if (currentRole() == DeviceRole.COMPANION) {
                            _peerTelemetry.value ?: _localTelemetry.value
                        } else {
                            _localTelemetry.value
                        }
                        updateLiveSignalNotification(
                            telemetry = activeTelemetry,
                            isConnected = true,
                            peerName = state.peerName
                        )
                        val keyToSave = lastSavedPairingKey ?: hostPairingPayload.value?.sessionKeyBase64
                        if (!keyToSave.isNullOrBlank()) {
                            repository.savePairedDevice(
                                PairedDeviceEntity(
                                    deviceId = state.peerDeviceId,
                                    deviceName = state.peerName,
                                    ipAddress = lastSavedHostIp.ifBlank {
                                        state.endpointLabel.substringBefore(":")
                                    },
                                    port = lastSavedPort,
                                    bluetoothAddress = lastSavedBtAddr,
                                    sessionKeyBase64 = keyToSave,
                                    fingerprint = state.fingerprint,
                                    peerRole = if (currentRole() == DeviceRole.HOST) "COMPANION" else "HOST"
                                )
                            )
                        }
                        // Push immediate telemetry snapshot upon connect
                        sendLocalTelemetryNow()
                    }
                    is ConnectionState.Idle -> {
                        voiceBridge.stop()
                        BridgeSessionHub.latestPeerTelemetry = null
                        _peerTelemetry.value = null
                    }
                    else -> Unit
                }
            }
        }
    }

    private fun updateLiveSignalNotification(
        telemetry: DeviceTelemetry?,
        isConnected: Boolean,
        peerName: String?,
        hostIp: String? = null
    ) {
        val strings = currentStrings()
        val sims = telemetry?.simCards.orEmpty()
        val maxSignal = sims.maxOfOrNull { it.signalLevel } ?: 0
        val title = when {
            isConnected && !peerName.isNullOrBlank() -> "${strings.appTitle} • $peerName"
            hostIp != null -> "${strings.appTitle} • ${strings.roleHost}"
            else -> strings.appTitle
        }

        val detailLines = buildList {
            sims.take(2).forEach { sim ->
                val bars = BridgeForegroundService.signalBarText(sim.signalLevel)
                add(
                    "${strings.simSlotLabel} ${sim.slotIndex}: ${sim.carrierName} • ${sim.networkType}  $bars (${sim.signalLevel}/4)"
                        .localizeDigits(strings.isPersian)
                )
            }
            if (telemetry != null && telemetry.batteryPercent >= 0) {
                val chargeIcon = if (telemetry.isCharging) " ⚡ ${strings.charging}" else ""
                val pingTag = if (isConnected && latencyMs.value > 0) " • ${latencyMs.value}ms" else ""
                val gaugeBar = BridgeForegroundService.batteryBarText(telemetry.batteryPercent)
                add(
                    "${strings.hostBattery}: ${telemetry.batteryPercent}%$chargeIcon  $gaugeBar$pingTag"
                        .localizeDigits(strings.isPersian)
                )
            }
        }

        val primarySim = sims.firstOrNull { it.slotIndex == preferences.value.preferredSimSlot } ?: sims.firstOrNull()
        val summary = when {
            !isConnected && hostIp != null -> "${strings.statusHosting} ($hostIp)"
            primarySim != null -> {
                val batPart = if ((telemetry?.batteryPercent ?: -1) >= 0) {
                    val bolt = if (telemetry?.isCharging == true) "⚡" else ""
                    " • ${telemetry?.batteryPercent}%$bolt"
                } else ""
                "${primarySim.carrierName} ${primarySim.networkType} ${BridgeForegroundService.signalBarText(primarySim.signalLevel)}$batPart"
                    .localizeDigits(strings.isPersian)
            }
            isConnected -> strings.statusConnected
            else -> strings.statusIdle
        }

        BridgeForegroundService.startOrUpdate(
            context = appContext,
            title = title,
            body = summary,
            signalLevel = maxSignal,
            detailLines = detailLines,
            isConnected = isConnected,
            batteryPercent = telemetry?.batteryPercent ?: -1,
            isCharging = telemetry?.isCharging ?: false
        )
    }

    private fun startLocalTelemetryTicker() {
        telemetryLoopJob?.cancel()
        telemetryLoopJob = viewModelScope.launch(Dispatchers.IO) {
            while (isActive) {
                val snapshot = telemetryReader.captureTelemetry(
                    deviceAlias = preferences.value.deviceAlias,
                    pingMs = latencyMs.value
                )
                _localTelemetry.value = snapshot
                maybeRecordBatterySample(
                    deviceId = BatteryTrendCalculator.LOCAL_HOST_DEVICE_ID,
                    deviceName = preferences.value.deviceAlias,
                    batteryPercent = snapshot.batteryPercent,
                    isCharging = snapshot.isCharging
                )
                val conn = connectionState.value
                if (conn is ConnectionState.Connected) {
                    transport.sendJson(BridgeProtocol.CMD_TELEMETRY, snapshot.toJson())
                    if (currentRole() == DeviceRole.HOST) {
                        updateLiveSignalNotification(
                            telemetry = snapshot,
                            isConnected = true,
                            peerName = conn.peerName
                        )
                    }
                } else if (conn is ConnectionState.Hosting && currentRole() == DeviceRole.HOST) {
                    updateLiveSignalNotification(
                        telemetry = snapshot,
                        isConnected = false,
                        peerName = null,
                        hostIp = conn.hostIp
                    )
                }
                delay(4_000L)
            }
        }
    }

    private fun sendLocalTelemetryNow() {
        viewModelScope.launch(Dispatchers.IO) {
            val snapshot = telemetryReader.captureTelemetry(
                deviceAlias = preferences.value.deviceAlias,
                pingMs = latencyMs.value
            )
            _localTelemetry.value = snapshot
            val conn = connectionState.value
            if (conn is ConnectionState.Connected) {
                transport.sendJson(BridgeProtocol.CMD_TELEMETRY, snapshot.toJson())
                if (currentRole() == DeviceRole.HOST) {
                    updateLiveSignalNotification(
                        telemetry = snapshot,
                        isConnected = true,
                        peerName = conn.peerName
                    )
                }
            }
        }
    }

    private fun triggerIncomingVibration() {
        runCatching {
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                (appContext.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager)?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                appContext.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 350, 200, 350), -1))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(400L)
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        // Keep BridgeSessionHub and BridgeForegroundService alive when the user switches
        // to the default Phone or Messages app; only explicit stopBridge() tears down the session.
        telemetryLoopJob?.cancel()
    }
}
