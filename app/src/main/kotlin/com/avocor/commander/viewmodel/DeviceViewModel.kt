package com.avocor.commander.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.avocor.commander.api.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class DeviceViewModel(application: Application) : AndroidViewModel(application) {

    private var api: CommanderApi? = null
    private val wsManager = WebSocketManager(viewModelScope)

    private val _devices = MutableStateFlow<List<DeviceDto>>(emptyList())
    val devices: StateFlow<List<DeviceDto>> = _devices

    private val _groups = MutableStateFlow<List<GroupDto>>(emptyList())
    val groups: StateFlow<List<GroupDto>> = _groups

    private val _statuses = MutableStateFlow<List<DeviceStatusDto>>(emptyList())
    val statuses: StateFlow<List<DeviceStatusDto>> = _statuses

    private val _macros = MutableStateFlow<List<MacroDto>>(emptyList())
    val macros: StateFlow<List<MacroDto>> = _macros

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing

    private val _commandResult = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val commandResult: SharedFlow<String> = _commandResult

    val wsConnectionState = wsManager.connectionState

    fun initialize(serverUrl: String, token: String) {
        api = CommanderApi.create(serverUrl) { token }
        wsManager.connect(serverUrl, token)

        // Listen for WebSocket events
        viewModelScope.launch {
            wsManager.events.collect { event ->
                handleWsEvent(event)
            }
        }

        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _isRefreshing.value = true
            try {
                val currentApi = api ?: return@launch
                _devices.value = currentApi.getDevices()
                _groups.value = currentApi.getGroups()
                _statuses.value = currentApi.getStatus()
                _macros.value = currentApi.getMacros()
            } catch (e: Exception) {
                _commandResult.emit("Refresh failed: ${e.message}")
            } finally {
                _isRefreshing.value = false
            }
        }
    }

    fun sendCommand(deviceId: Int, command: String, category: String? = null) {
        viewModelScope.launch {
            try {
                val currentApi = api ?: return@launch
                val response = currentApi.sendCommand(
                    deviceId,
                    CommandRequest(category = category, command = command)
                )
                if (response.success) {
                    _commandResult.emit("OK: ${response.response ?: "Command sent"}")
                } else {
                    _commandResult.emit("Error: ${response.error ?: "Command failed"}")
                }
            } catch (e: Exception) {
                _commandResult.emit("Error: ${e.message}")
            }
        }
    }

    fun wakeDevice(deviceId: Int) {
        viewModelScope.launch {
            try {
                val currentApi = api ?: return@launch
                val response = currentApi.wakeDevice(deviceId)
                if (response.success) {
                    _commandResult.emit("Wake: ${response.message ?: "Sent"}")
                } else {
                    _commandResult.emit("Wake failed: ${response.message}")
                }
            } catch (e: Exception) {
                _commandResult.emit("Wake error: ${e.message}")
            }
        }
    }

    fun connectDevice(deviceId: Int) {
        viewModelScope.launch {
            try {
                val currentApi = api ?: return@launch
                val response = currentApi.connectDevice(deviceId)
                _commandResult.emit(if (response.success) "Connected" else "Connect failed")
                refresh()
            } catch (e: Exception) {
                _commandResult.emit("Connect error: ${e.message}")
            }
        }
    }

    fun disconnectDevice(deviceId: Int) {
        viewModelScope.launch {
            try {
                val currentApi = api ?: return@launch
                val response = currentApi.disconnectDevice(deviceId)
                _commandResult.emit(if (response.success) "Disconnected" else "Disconnect failed")
                refresh()
            } catch (e: Exception) {
                _commandResult.emit("Disconnect error: ${e.message}")
            }
        }
    }

    fun runMacro(macroId: Int) {
        viewModelScope.launch {
            try {
                val currentApi = api ?: return@launch
                val response = currentApi.runMacro(macroId, MacroRunRequest())
                _commandResult.emit(
                    if (response.success) "Macro started" else "Macro failed: ${response.message}"
                )
            } catch (e: Exception) {
                _commandResult.emit("Macro error: ${e.message}")
            }
        }
    }

    fun sendGroupCommand(groupId: Int, command: String, category: String? = null) {
        viewModelScope.launch {
            try {
                val currentApi = api ?: return@launch
                val responses = currentApi.sendGroupCommand(
                    groupId,
                    GroupCommandRequest(category = category, command = command)
                )
                val successCount = responses.count { it.success }
                _commandResult.emit("Group command: $successCount/${responses.size} succeeded")
            } catch (e: Exception) {
                _commandResult.emit("Group command error: ${e.message}")
            }
        }
    }

    private fun handleWsEvent(event: WsEvent) {
        when (event.type) {
            "connection.changed", "device.health" -> {
                // Refresh status on connection/health changes
                viewModelScope.launch {
                    try {
                        api?.let { _statuses.value = it.getStatus() }
                    } catch (_: Exception) {
                        // Ignore refresh failures from WS events
                    }
                }
            }
        }
    }

    fun getStatusForDevice(deviceId: Int): DeviceStatusDto? {
        return _statuses.value.find { it.deviceId == deviceId }
    }

    fun cleanup() {
        wsManager.disconnect()
    }

    override fun onCleared() {
        super.onCleared()
        cleanup()
    }
}
