package com.chroma.projector

import android.content.Context
import android.net.wifi.WifiManager
import android.os.Build
import android.provider.Settings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.MulticastSocket
import java.net.ServerSocket
import java.net.Socket
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

private const val MULTICAST_GROUP = "239.255.42.99"
private const val MULTICAST_PORT = 54545
private const val CONTROL_PORT = 54546
private const val HEARTBEAT_TIMEOUT_MS = 5_000L
private const val DEVICE_STALE_MS = 30_000L

private fun jsonLine(obj: JSONObject): String = obj.toString() + "\n"

private fun contextDeviceId(context: Context): String {
    val androidId = Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
    return when {
        !androidId.isNullOrBlank() -> androidId
        else -> UUID.randomUUID().toString()
    }
}

private fun contextDeviceName(): String = Build.MODEL ?: "Android"

private class MulticastLocker(context: Context, tag: String) {
    private val lock = (context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager)
        .createMulticastLock(tag)
        .apply { setReferenceCounted(false) }

    fun acquire() {
        if (!lock.isHeld) lock.acquire()
    }

    fun release() {
        if (lock.isHeld) lock.release()
    }
}

class ControlHub(
    private val context: Context,
    private val onDevicesChanged: (List<ManagedDevice>) -> Unit,
    private val onStatus: (String) -> Unit
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val multicastLocker = MulticastLocker(context, "chroma-control")
    private val devices = ConcurrentHashMap<String, DeviceRecord>()
    private val sessions = ConcurrentHashMap<String, ControlSession>()
    private val serverState = ConcurrentHashMap<String, ProjectionState>()
    private var serverSocket: ServerSocket? = null
    private var beaconSocket: DatagramSocket? = null
    private var beaconJob: Job? = null
    private var acceptJob: Job? = null
    private val controlId = contextDeviceId(context)

    fun start() {
        multicastLocker.acquire()
        if (acceptJob == null) {
            acceptJob = scope.launch { acceptLoop() }
        }
        if (beaconJob == null) {
            beaconJob = scope.launch { beaconLoop() }
        }
        publishDevices()
    }

    fun stop() {
        beaconJob?.cancel()
        acceptJob?.cancel()
        beaconJob = null
        acceptJob = null
        sessions.values.forEach { it.close() }
        sessions.clear()
        devices.clear()
        serverState.clear()
        serverSocket?.close()
        beaconSocket?.close()
        serverSocket = null
        beaconSocket = null
        multicastLocker.release()
        scope.cancel()
    }

    fun updateDeviceState(deviceId: String, state: ProjectionState) {
        serverState[deviceId] = state
        devices[deviceId]?.state = state
        sessions[deviceId]?.sendState(state)
        publishDevices()
    }

    fun currentState(deviceId: String): ProjectionState = serverState[deviceId] ?: defaultProjectionState

    private suspend fun acceptLoop() {
        try {
            serverSocket = ServerSocket(CONTROL_PORT)
            onStatus("控制端已启动")
            while (scope.isActive) {
                val socket = serverSocket?.accept() ?: break
                scope.launch { handleConnection(socket) }
            }
        } catch (t: Throwable) {
            onStatus("控制端启动失败: ${t.message ?: t.javaClass.simpleName}")
        }
    }

    private suspend fun handleConnection(socket: Socket) {
        socket.tcpNoDelay = true
        val reader = BufferedReader(InputStreamReader(socket.getInputStream()))
        val writer = BufferedWriter(OutputStreamWriter(socket.getOutputStream()))
        var deviceId = ""
        try {
            val hello = JSONObject(reader.readLine() ?: return)
            deviceId = hello.optString("deviceId").ifBlank { UUID.randomUUID().toString() }
            val deviceName = hello.optString("name", "设备")
            val address = socket.inetAddress.hostAddress ?: "unknown"
            val now = System.currentTimeMillis()
            val state = serverState[deviceId] ?: defaultProjectionState
            serverState[deviceId] = state

            devices[deviceId] = DeviceRecord(
                id = deviceId,
                name = deviceName,
                address = address,
                lastSeen = now,
                connected = true,
                state = state
            )
            sessions[deviceId] = ControlSession(deviceId, socket, writer)
            publishDevices()
            sessions[deviceId]?.sendState(state)
            onStatus("已连接: $deviceName")

            while (scope.isActive && !socket.isClosed) {
                val line = reader.readLine() ?: break
                val msg = JSONObject(line)
                when (msg.optString("type")) {
                    "heartbeat" -> {
                        val record = devices[deviceId] ?: continue
                        record.lastSeen = System.currentTimeMillis()
                        publishDevices()
                    }
                }
            }
        } catch (_: Throwable) {
            // drop through and clean up
        } finally {
            if (deviceId.isNotBlank()) {
                devices.remove(deviceId)
                sessions.remove(deviceId)
                serverState.remove(deviceId)
            }
            socket.close()
            publishDevices()
        }
    }

    private suspend fun beaconLoop() {
        try {
            beaconSocket = DatagramSocket()
            val group = InetAddress.getByName(MULTICAST_GROUP)
            while (scope.isActive) {
                val payload = JSONObject().apply {
                    put("type", "beacon")
                    put("controlId", controlId)
                    put("name", contextDeviceName())
                    put("port", CONTROL_PORT)
                }.toString()
                val bytes = payload.toByteArray(Charsets.UTF_8)
                val packet = DatagramPacket(bytes, bytes.size, group, MULTICAST_PORT)
                beaconSocket?.send(packet)
                delay(1500)
            }
        } catch (_: Throwable) {
        }
    }

    private fun publishDevices() {
        pruneDevices()
        val now = System.currentTimeMillis()
        val list = devices.values
            .map {
                ManagedDevice(
                    id = it.id,
                    name = it.name,
                    address = it.address,
                    lastSeen = it.lastSeen,
                    connected = now - it.lastSeen < HEARTBEAT_TIMEOUT_MS,
                    state = serverState[it.id] ?: it.state
                )
            }
            .sortedByDescending { it.lastSeen }
        onDevicesChanged(list)
    }

    private fun pruneDevices() {
        val now = System.currentTimeMillis()
        val stale = devices.values.filter { now - it.lastSeen > DEVICE_STALE_MS }
        stale.forEach {
            devices.remove(it.id)
            sessions.remove(it.id)
            serverState.remove(it.id)
        }
    }

    private data class DeviceRecord(
        val id: String,
        val name: String,
        val address: String,
        var lastSeen: Long,
        var connected: Boolean,
        var state: ProjectionState
    )

    private class ControlSession(
        private val deviceId: String,
        private val socket: Socket,
        private val writer: BufferedWriter
    ) {
        @Synchronized
        fun sendState(state: ProjectionState) {
            try {
                writer.write(jsonLine(JSONObject().apply {
                    put("type", "state")
                    put("deviceId", deviceId)
                    put("state", state.toJson())
                }))
                writer.flush()
            } catch (_: Throwable) {
            }
        }

        fun close() {
            try {
                socket.close()
            } catch (_: Throwable) {
            }
        }
    }
}

class ReceiverAgent(
    private val context: Context,
    private val onStateChanged: (ProjectionState) -> Unit,
    private val onStatus: (String) -> Unit,
    private val onControlDiscovered: (DiscoveredControl?) -> Unit
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val multicastLocker = MulticastLocker(context, "chroma-receiver")
    private var discoveryJob: Job? = null
    private var heartbeatJob: Job? = null
    private var socket: Socket? = null
    private var writer: BufferedWriter? = null
    private var reader: BufferedReader? = null
    private var discoveredControl: DiscoveredControl? = null
    private val deviceId = contextDeviceId(context)
    private val deviceName = contextDeviceName()

    fun start() {
        multicastLocker.acquire()
        if (discoveryJob == null) {
            discoveryJob = scope.launch { discoveryLoop() }
        }
    }

    fun stop() {
        discoveryJob?.cancel()
        heartbeatJob?.cancel()
        discoveryJob = null
        heartbeatJob = null
        closeSocket()
        multicastLocker.release()
        scope.cancel()
    }

    private suspend fun discoveryLoop() {
        val group = InetAddress.getByName(MULTICAST_GROUP)
        while (scope.isActive) {
            try {
                MulticastSocket(MULTICAST_PORT).use { multicastSocket ->
                    multicastSocket.reuseAddress = true
                    multicastSocket.joinGroup(group)
                    val buffer = ByteArray(4096)
                    while (scope.isActive) {
                        val packet = DatagramPacket(buffer, buffer.size)
                        multicastSocket.receive(packet)
                        val json = JSONObject(String(packet.data, 0, packet.length, Charsets.UTF_8))
                        if (json.optString("type") != "beacon") continue
                        val control = DiscoveredControl(
                            host = packet.address.hostAddress ?: continue,
                            port = json.optInt("port", CONTROL_PORT),
                            name = json.optString("name", "控制端"),
                            lastSeen = System.currentTimeMillis()
                        )
                        discoveredControl = control
                        onControlDiscovered(control)
                        if (!isConnectedTo(control.host, control.port)) {
                            connectTo(control)
                        }
                    }
                }
            } catch (_: Throwable) {
                onStatus("等待控制端广播")
                delay(1000)
            }
        }
    }

    private fun isConnectedTo(host: String, port: Int): Boolean {
        val control = discoveredControl ?: return false
        return socket?.isConnected == true && !socket!!.isClosed && control.host == host && control.port == port
    }

    private fun connectTo(control: DiscoveredControl) {
        closeSocket()
        scope.launch {
            try {
                val target = Socket()
                target.connect(InetSocketAddress(control.host, control.port), 2500)
                socket = target
                writer = BufferedWriter(OutputStreamWriter(target.getOutputStream()))
                reader = BufferedReader(InputStreamReader(target.getInputStream()))
                writer?.write(jsonLine(JSONObject().apply {
                    put("type", "hello")
                    put("deviceId", deviceId)
                    put("name", deviceName)
                }))
                writer?.flush()
                onStatus("已连接控制端: ${control.name}")
                heartbeatJob?.cancel()
                heartbeatJob = scope.launch { heartbeatLoop() }
                readLoop()
            } catch (_: Throwable) {
                onStatus("控制端连接失败")
                closeSocket()
            }
        }
    }

    private suspend fun readLoop() {
        val currentReader = reader ?: return
        try {
            while (scope.isActive) {
                val line = currentReader.readLine() ?: break
                val json = JSONObject(line)
                when (json.optString("type")) {
                    "state" -> onStateChanged(stateFromJson(json.getJSONObject("state")))
                }
            }
        } catch (_: Throwable) {
        } finally {
            onStatus("控制端断开，重新搜索")
            closeSocket()
        }
    }

    private suspend fun heartbeatLoop() {
        while (scope.isActive && socket?.isConnected == true && !socket!!.isClosed) {
            try {
                writer?.write(jsonLine(JSONObject().apply {
                    put("type", "heartbeat")
                    put("deviceId", deviceId)
                }))
                writer?.flush()
            } catch (_: Throwable) {
                break
            }
            delay(1500)
        }
    }

    private fun closeSocket() {
        try {
            socket?.close()
        } catch (_: Throwable) {
        }
        socket = null
        writer = null
        reader = null
    }
}
