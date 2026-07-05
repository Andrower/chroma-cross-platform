package com.chroma.projector

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

class ProjectionViewModel(application: Application) : AndroidViewModel(application) {
    var launchMode by mutableStateOf(LaunchMode.Choose)
        private set

    var status by mutableStateOf("请选择模式")
        private set

    var discoveredControl by mutableStateOf<DiscoveredControl?>(null)
        private set

    var connectedDevices by mutableStateOf<List<ManagedDevice>>(emptyList())
        private set

    var selectedDeviceId by mutableStateOf<String?>(null)
        private set

    var editedState by mutableStateOf(defaultProjectionState)
        private set

    var receiverState by mutableStateOf(defaultProjectionState)
        private set

    private var controlHub: ControlHub? = null
    private var receiverAgent: ReceiverAgent? = null

    fun chooseControlMode() {
        shutdown()
        launchMode = LaunchMode.Control
        controlHub = ControlHub(
            getApplication(),
            onDevicesChanged = { devices ->
                viewModelScope.launch {
                    connectedDevices = devices
                    if (selectedDeviceId == null || devices.none { it.id == selectedDeviceId }) {
                        selectedDeviceId = devices.firstOrNull()?.id
                    }
                    selectedDeviceId?.let { id ->
                        editedState = devices.firstOrNull { it.id == id }?.state ?: editedState
                    }
                }
            },
            onStatus = { text ->
                viewModelScope.launch { status = text }
            }
        ).also { it.start() }
        status = "控制端启动中"
    }

    fun chooseReceiverMode() {
        shutdown()
        launchMode = LaunchMode.Receiver
        receiverAgent = ReceiverAgent(
            getApplication(),
            onStateChanged = { state ->
                viewModelScope.launch {
                    receiverState = state
                    editedState = state
                }
            },
            onStatus = { text ->
                viewModelScope.launch { status = text }
            },
            onControlDiscovered = { control ->
                viewModelScope.launch { discoveredControl = control }
            }
        ).also { it.start() }
        status = "等待自动发现控制端"
    }

    fun selectDevice(deviceId: String) {
        selectedDeviceId = deviceId
        editedState = connectedDevices.firstOrNull { it.id == deviceId }?.state ?: editedState
    }

    fun applyPreset(preset: ProjectionPreset) {
        applyState(
            editedState.copy(
                bgColor = preset.bgColor,
                bgBrightness = preset.bgBrightness,
                crossColor = preset.crossColor,
                crossBrightness = preset.crossBrightness
            )
        )
    }

    fun updateEditedState(transform: (ProjectionState) -> ProjectionState) {
        applyState(transform(editedState))
    }

    private fun applyState(newState: ProjectionState) {
        editedState = newState
        if (launchMode == LaunchMode.Control) {
            selectedDeviceId?.let { id ->
                controlHub?.updateDeviceState(id, newState)
            }
        }
    }

    fun receiverStateText(): String = status

    fun clearSelection() {
        selectedDeviceId = null
    }

    fun shutdown() {
        controlHub?.stop()
        receiverAgent?.stop()
        controlHub = null
        receiverAgent = null
        connectedDevices = emptyList()
        discoveredControl = null
        selectedDeviceId = null
    }

    override fun onCleared() {
        shutdown()
        super.onCleared()
    }
}
