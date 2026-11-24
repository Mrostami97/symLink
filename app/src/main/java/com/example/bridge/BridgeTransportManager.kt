package com.example.bridge

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothServerSocket
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.content.pm.PackageManager
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.os.Build
import android.provider.Settings
import androidx.core.content.ContextCompat
import java.io.DataInputStream
import java.io.DataOutputStream
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket
import java.util.UUID
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.json.JSONObject

class BridgeTransportManager(private val context: Context) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    val localDeviceId: String = runCatching {
        Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
    }.getOrNull()?.takeIf { it.isNotBlank() } ?: "itell_${Build.MODEL.hashCode()}"

    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Idle)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    private val _discoveredPeers = MutableStateFlow<List<DiscoveredPeer>>(emptyList())
    val discoveredPeers: StateFlow<List<DiscoveredPeer>> = _discoveredPeers.asStateFlow()

    private val _hostPairingPayload = MutableStateFlow<PairingPayload?>(null)
    val hostPairingPayload: StateFlow<PairingPayload?> = _hostPairingPayload.asStateFlow()

    private val _incomingJsonCommands = MutableSharedFlow<JSONObject>(extraBufferCapacity = 64)
    val incomingJsonCommands: SharedFlow<JSONObject> = _incomingJsonCommands.asSharedFlow()

    private val _incomingAudioFrames = MutableSharedFlow<ByteArray>(extraBufferCapacity = 64)
    val incomingAudioFrames: SharedFlow<ByteArray> = _incomingAudioFrames.asSharedFlow()

    private val _latencyMs = MutableStateFlow<Long>(-1L)
    val latencyMs: StateFlow<Long> = _latencyMs.asStateFlow()

    private var activeTcpServer: ServerSocket? = null
    private var activeBtServer: BluetoothServerSocket? = null
    private var activeTcpSocket: Socket? = null
    private var activeBtSocket: BluetoothSocket? = null
    private var activeOut: DataOutputStream? = null
    private var activeSessionKey: String? = null
    private var fallbackCodeKey: String? = null

    private var serverJob: Job? = null
    private var btAcceptJob: Job? = null
    private var readerJob: Job? = null
    private var heartbeatJob: Job? = null

    private var nsdManager: NsdManager? = null
    private var nsdRegListener: NsdManager.RegistrationListener? = null
    private var nsdDiscListener: NsdManager.DiscoveryListener? = null

    private var lastPingSentAt = 0L

    fun hasBluetoothConnectPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) ==
                PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    @SuppressLint("MissingPermission")
    fun startHosting(deviceAlias: String, localIp: String) {
        stopAll()

        val pairCode = BridgeCrypto.generatePairCode()
        val sessionKey = BridgeCrypto.generateSessionKey()
        val codeDerivedKey = BridgeCrypto.deriveKeyFromPairCode(pairCode)
        val fingerprint = BridgeCrypto.computeFingerprint(sessionKey)
        activeSessionKey = sessionKey
        fallbackCodeKey = codeDerivedKey

        serverJob = scope.launch {
            try {
                val server = runCatching { ServerSocket(BridgeProtocol.DEFAULT_PORT) }
                    .getOrElse { ServerSocket(0) }
                activeTcpServer = server
                val boundPort = server.localPort

                val btReady = startBluetoothHostListener(deviceAlias, sessionKey, codeDerivedKey)
                val btAddr = if (hasBluetoothConnectPermission()) {
                    runCatching {
                        val mgr = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
                        mgr?.adapter?.address.orEmpty()
                    }.getOrDefault("")
                } else ""

                val payload = PairingPayload(
                    deviceId = localDeviceId,
                    deviceName = deviceAlias,
                    hostIp = localIp,
                    port = boundPort,
                    btAddress = btAddr,
                    sessionKeyBase64 = sessionKey,
                    pairCode = pairCode,
                    fingerprint = fingerprint
                )
                _hostPairingPayload.value = payload
                _connectionState.value = ConnectionState.Hosting(localIp, boundPort, btReady)
                registerNsdService(deviceAlias, boundPort)

                while (isActive && !server.isClosed) {
                    val clientSocket = server.accept()
                    attachTcpSession(
                        socket = clientSocket,
                        primaryKey = sessionKey,
                        fallbackKey = codeDerivedKey,
                        localAlias = deviceAlias,
                        localRole = DeviceRole.HOST
                    )
                }
            } catch (e: Exception) {
                if (_connectionState.value !is ConnectionState.Idle) {
                    _connectionState.value = ConnectionState.Error(e.message ?: "Host socket closed")
                }
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun startBluetoothHostListener(
        deviceAlias: String,
        sessionKey: String,
        fallbackKey: String
    ): Boolean {
        if (!hasBluetoothConnectPermission()) return false
        val btManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
        val adapter: BluetoothAdapter = btManager?.adapter ?: return false
        if (!adapter.isEnabled) return false

        return runCatching {
            val serverSocket = adapter.listenUsingRfcommWithServiceRecord(
                "SimLinkBridge",
                UUID.fromString(BridgeProtocol.BLUETOOTH_SPP_UUID)
            )
            activeBtServer = serverSocket
            btAcceptJob = scope.launch {
                while (isActive) {
                    val btClient = runCatching { serverSocket.accept() }.getOrNull() ?: break
                    attachBluetoothSession(
                        socket = btClient,
                        primaryKey = sessionKey,
                        fallbackKey = fallbackKey,
                        localAlias = deviceAlias,
                        localRole = DeviceRole.HOST
                    )
                }
            }
            true
        }.getOrDefault(false)
    }

    fun connectViaWifi(
        hostIp: String,
        port: Int,
        sessionKeyBase64: String,
        localAlias: String
    ) {
        stopActiveSessionOnly()
        _connectionState.value = ConnectionState.Connecting("$hostIp:$port")
        scope.launch {
            try {
                val socket = Socket()
                socket.connect(InetSocketAddress(hostIp.trim(), port), 8_000)
                socket.tcpNoDelay = true
                attachTcpSession(
                    socket = socket,
                    primaryKey = sessionKeyBase64,
                    fallbackKey = null,
                    localAlias = localAlias,
                    localRole = DeviceRole.COMPANION
                )
            } catch (e: Exception) {
                _connectionState.value = ConnectionState.Error(e.message ?: "TCP connection failed")
            }
        }
    }

    @SuppressLint("MissingPermission")
    fun connectViaBluetooth(
        btAddress: String,
        sessionKeyBase64: String,
        localAlias: String
    ) {
        if (!hasBluetoothConnectPermission()) {
            _connectionState.value = ConnectionState.Error("Bluetooth permission required")
            return
        }
        stopActiveSessionOnly()
        _connectionState.value = ConnectionState.Connecting("BT $btAddress")
        scope.launch {
            try {
                val btManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
                val adapter = btManager?.adapter ?: throw IllegalStateException("Bluetooth unavailable")
                val device = adapter.getRemoteDevice(btAddress)
                val socket = device.createRfcommSocketToServiceRecord(
                    UUID.fromString(BridgeProtocol.BLUETOOTH_SPP_UUID)
                )
                runCatching { adapter.cancelDiscovery() }
                socket.connect()
                attachBluetoothSession(
                    socket = socket,
                    primaryKey = sessionKeyBase64,
                    fallbackKey = null,
                    localAlias = localAlias,
                    localRole = DeviceRole.COMPANION
                )
            } catch (e: Exception) {
                _connectionState.value = ConnectionState.Error(e.message ?: "Bluetooth connection failed")
            }
        }
    }

    private fun attachTcpSession(
        socket: Socket,
        primaryKey: String,
        fallbackKey: String?,
        localAlias: String,
        localRole: DeviceRole
    ) {
        stopActiveSessionOnly()
        activeTcpSocket = socket
        activeSessionKey = primaryKey
        fallbackCodeKey = fallbackKey
        val input = DataInputStream(socket.getInputStream())
        val output = DataOutputStream(socket.getOutputStream())
        activeOut = output
        val endpoint = "${socket.inetAddress?.hostAddress ?: "LAN"}:${socket.port}"
        startSessionLoops(input, output, primaryKey, fallbackKey, TransportMode.WIFI_TCP, endpoint, localAlias, localRole)
    }

    private fun attachBluetoothSession(
        socket: BluetoothSocket,
        primaryKey: String,
        fallbackKey: String?,
        localAlias: String,
        localRole: DeviceRole
    ) {
        stopActiveSessionOnly()
        activeBtSocket = socket
        activeSessionKey = primaryKey
        fallbackCodeKey = fallbackKey
        val input = DataInputStream(socket.inputStream)
        val output = DataOutputStream(socket.outputStream)
        activeOut = output
        val endpoint = socket.remoteDevice?.address ?: "Bluetooth"
        startSessionLoops(input, output, primaryKey, fallbackKey, TransportMode.BLUETOOTH_RFCOMM, endpoint, localAlias, localRole)
    }

    private fun startSessionLoops(
        input: DataInputStream,
        output: DataOutputStream,
        primaryKey: String,
        fallbackKey: String?,
        transport: TransportMode,
        endpoint: String,
        localAlias: String,
        localRole: DeviceRole
    ) {
        // Send handshake immediately
        scope.launch {
            sendJson(
                BridgeProtocol.CMD_HANDSHAKE,
                JSONObject().apply {
                    put("deviceId", localDeviceId)
                    put("deviceName", localAlias)
                    put("role", localRole.name)
                }
            )
        }

        readerJob = scope.launch {
            var negotiatedKey = primaryKey
            try {
                while (isActive) {
                    val (frame, usedKey) = BridgeProtocol.readEncryptedFrame(input, negotiatedKey, fallbackKey)
                    if (usedKey != negotiatedKey) {
                        negotiatedKey = usedKey
                        activeSessionKey = usedKey
                    }
                    when (frame.type) {
                        BridgeProtocol.FRAME_JSON -> {
                            val obj = JSONObject(String(frame.payload, Charsets.UTF_8))
                            val cmd = obj.optString("cmd")
                            if (cmd == BridgeProtocol.CMD_HANDSHAKE || cmd == BridgeProtocol.CMD_HANDSHAKE_ACK) {
                                val peerId = obj.optString("deviceId", "peer_$endpoint")
                                val peerName = obj.optString("deviceName", "Android Peer")
                                _connectionState.value = ConnectionState.Connected(
                                    peerDeviceId = peerId,
                                    peerName = peerName,
                                    transport = transport,
                                    fingerprint = BridgeCrypto.computeFingerprint(negotiatedKey),
                                    endpointLabel = endpoint
                                )
                                if (cmd == BridgeProtocol.CMD_HANDSHAKE) {
                                    sendJson(
                                        BridgeProtocol.CMD_HANDSHAKE_ACK,
                                        JSONObject().apply {
                                            put("deviceId", localDeviceId)
                                            put("deviceName", localAlias)
                                            put("role", localRole.name)
                                        }
                                    )
                                }
                            }
                            _incomingJsonCommands.tryEmit(obj)
                        }
                        BridgeProtocol.FRAME_AUDIO_PCM -> {
                            _incomingAudioFrames.tryEmit(frame.payload)
                        }
                        BridgeProtocol.FRAME_PING -> {
                            runCatching {
                                BridgeProtocol.writeEncryptedFrame(
                                    output,
                                    BridgeProtocol.FRAME_PONG,
                                    frame.payload,
                                    negotiatedKey
                                )
                            }
                        }
                        BridgeProtocol.FRAME_PONG -> {
                            if (lastPingSentAt > 0L) {
                                _latencyMs.value = (System.currentTimeMillis() - lastPingSentAt).coerceAtLeast(1L)
                            }
                        }
                    }
                }
            } catch (_: Exception) {
                val payload = _hostPairingPayload.value
                if (localRole == DeviceRole.HOST && activeTcpServer?.isClosed == false && payload != null) {
                    _connectionState.value = ConnectionState.Hosting(
                        payload.hostIp,
                        payload.port,
                        activeBtServer != null
                    )
                } else {
                    _connectionState.value = ConnectionState.Idle
                }
                _latencyMs.value = -1L
            }
        }

        heartbeatJob = scope.launch {
            while (isActive) {
                delay(5_000L)
                val key = activeSessionKey ?: continue
                val out = activeOut ?: continue
                lastPingSentAt = System.currentTimeMillis()
                runCatching {
                    BridgeProtocol.writeEncryptedFrame(
                        out,
                        BridgeProtocol.FRAME_PING,
                        lastPingSentAt.toString().toByteArray(),
                        key
                    )
                }
            }
        }
    }

    fun sendJson(cmd: String, extra: JSONObject = JSONObject()): Boolean {
        val out = activeOut ?: return false
        val key = activeSessionKey ?: return false
        extra.put("cmd", cmd)
        extra.put("ts", System.currentTimeMillis())
        return runCatching {
            BridgeProtocol.writeEncryptedFrame(
                out,
                BridgeProtocol.FRAME_JSON,
                extra.toString().toByteArray(Charsets.UTF_8),
                key
            )
            true
        }.getOrDefault(false)
    }

    fun sendAudioPcm(pcmBytes: ByteArray) {
        val out = activeOut ?: return
        val key = activeSessionKey ?: return
        runCatching {
            BridgeProtocol.writeEncryptedFrame(out, BridgeProtocol.FRAME_AUDIO_PCM, pcmBytes, key)
        }
    }

    fun startDiscovery() {
        stopDiscovery()
        _discoveredPeers.value = emptyList()
        if (_connectionState.value is ConnectionState.Idle || _connectionState.value is ConnectionState.Error) {
            _connectionState.value = ConnectionState.Discovering
        }

        // 1. Discover Bluetooth paired Android peers immediately
        refreshPairedBluetoothPeers()

        // 2. Discover local Wi-Fi / Hotspot NSD services
        runCatching {
            val mgr = context.getSystemService(Context.NSD_SERVICE) as? NsdManager ?: return
            nsdManager = mgr
            val listener = object : NsdManager.DiscoveryListener {
                override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) {}
                override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) {}
                override fun onDiscoveryStarted(regType: String) {}
                override fun onDiscoveryStopped(serviceType: String) {}
                override fun onServiceLost(service: NsdServiceInfo) {
                    _discoveredPeers.update { list -> list.filterNot { it.id == "nsd_${service.serviceName}" } }
                }

                @Suppress("DEPRECATION")
                override fun onServiceFound(service: NsdServiceInfo) {
                    val sName = service.serviceName
                    if (!sName.startsWith("SimLink-") && !sName.startsWith("iTell-")) return
                    mgr.resolveService(service, object : NsdManager.ResolveListener {
                        override fun onResolveFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {}
                        override fun onServiceResolved(serviceInfo: NsdServiceInfo) {
                            val ip = serviceInfo.host?.hostAddress ?: return
                            val cleanName = serviceInfo.serviceName
                                .removePrefix("SimLink-")
                                .removePrefix("iTell-")
                            val peer = DiscoveredPeer(
                                id = "nsd_${serviceInfo.serviceName}",
                                name = cleanName,
                                hostIp = ip,
                                port = serviceInfo.port,
                                transport = TransportMode.WIFI_TCP
                            )
                            _discoveredPeers.update { list ->
                                (list.filterNot { it.id == peer.id } + peer)
                            }
                        }
                    })
                }
            }
            nsdDiscListener = listener
            mgr.discoverServices(BridgeProtocol.NSD_SERVICE_TYPE, NsdManager.PROTOCOL_DNS_SD, listener)
        }
    }

    @SuppressLint("MissingPermission")
    fun refreshPairedBluetoothPeers() {
        if (!hasBluetoothConnectPermission()) return
        runCatching {
            val btManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
            val bonded = btManager?.adapter?.bondedDevices.orEmpty()
            val btPeers = bonded.map { dev ->
                DiscoveredPeer(
                    id = "bt_${dev.address}",
                    name = dev.name ?: dev.address,
                    btAddress = dev.address,
                    transport = TransportMode.BLUETOOTH_RFCOMM
                )
            }
            _discoveredPeers.update { existing ->
                val nonBt = existing.filter { it.transport != TransportMode.BLUETOOTH_RFCOMM }
                nonBt + btPeers
            }
        }
    }

    private fun registerNsdService(deviceAlias: String, port: Int) {
        runCatching {
            val mgr = context.getSystemService(Context.NSD_SERVICE) as? NsdManager ?: return
            nsdManager = mgr
            val info = NsdServiceInfo().apply {
                serviceName = "SimLink-${deviceAlias.take(20)}"
                serviceType = BridgeProtocol.NSD_SERVICE_TYPE
                setPort(port)
            }
            val listener = object : NsdManager.RegistrationListener {
                override fun onServiceRegistered(nsdServiceInfo: NsdServiceInfo) {}
                override fun onRegistrationFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {}
                override fun onServiceUnregistered(arg0: NsdServiceInfo) {}
                override fun onUnregistrationFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {}
            }
            nsdRegListener = listener
            mgr.registerService(info, NsdManager.PROTOCOL_DNS_SD, listener)
        }
    }

    fun stopDiscovery() {
        runCatching {
            nsdDiscListener?.let { nsdManager?.stopServiceDiscovery(it) }
        }
        nsdDiscListener = null
    }

    private fun stopActiveSessionOnly() {
        readerJob?.cancel()
        readerJob = null
        heartbeatJob?.cancel()
        heartbeatJob = null
        runCatching { activeTcpSocket?.close() }
        activeTcpSocket = null
        runCatching { activeBtSocket?.close() }
        activeBtSocket = null
        activeOut = null
        _latencyMs.value = -1L
    }

    fun stopAll() {
        stopDiscovery()
        stopActiveSessionOnly()
        serverJob?.cancel()
        serverJob = null
        btAcceptJob?.cancel()
        btAcceptJob = null
        runCatching { nsdRegListener?.let { nsdManager?.unregisterService(it) } }
        nsdRegListener = null
        runCatching { activeTcpServer?.close() }
        activeTcpServer = null
        runCatching { activeBtServer?.close() }
        activeBtServer = null
        _hostPairingPayload.value = null
        _connectionState.value = ConnectionState.Idle
    }
}
