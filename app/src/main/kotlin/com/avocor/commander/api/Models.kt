package com.avocor.commander.api

import com.google.gson.annotations.SerializedName

// ── Auth ──

data class LoginRequest(
    val username: String,
    val password: String
)

data class LoginResponse(
    val token: String,
    val username: String,
    val role: String
)

// ── Devices ──

data class DeviceDto(
    val id: Int,
    val deviceName: String,
    val modelNumber: String,
    val serialNumber: String? = null,
    val ipAddress: String? = null,
    val port: Int = 23,
    val protocol: String = "telnet",
    val series: String? = null,
    val roomId: Int? = null,
    val roomName: String? = null,
    val groupId: Int? = null,
    val groupName: String? = null,
    val notes: String? = null,
    val isConnected: Boolean = false,
    val lastSeen: String? = null
)

data class DeviceStatusDto(
    val deviceId: Int,
    val deviceName: String,
    val isConnected: Boolean,
    val powerState: String? = null,
    val inputSource: String? = null,
    val volume: Int? = null,
    val isMuted: Boolean? = null,
    val lastSeen: String? = null,
    val health: String? = null
)

// ── Commands ──

data class CommandRequest(
    val category: String? = null,
    val command: String
)

data class CommandResponse(
    val deviceId: Int,
    val deviceName: String,
    val success: Boolean,
    val response: String? = null,
    val error: String? = null
)

data class CommandDto(
    val name: String,
    val category: String,
    val description: String? = null,
    val rawCommand: String? = null,
    val series: String? = null,
    val parameters: List<CommandParameterDto>? = null
)

data class CommandParameterDto(
    val name: String,
    val type: String,
    val description: String? = null,
    val options: List<String>? = null
)

// ── Groups ──

data class GroupDto(
    val id: Int,
    val groupName: String,
    val notes: String? = null,
    val memberDeviceIds: List<Int> = emptyList()
)

data class GroupCommandRequest(
    val category: String? = null,
    val command: String
)

// ── Macros ──

data class MacroDto(
    val id: Int,
    val name: String,
    val description: String? = null,
    val steps: List<MacroStepDto>? = null
)

data class MacroStepDto(
    val order: Int,
    val type: String,
    val description: String? = null
)

data class MacroRunRequest(
    val confirmPrompts: Boolean = true
)

// ── Wake / Connect ──

data class WakeResponse(
    val deviceId: Int,
    val deviceName: String,
    val success: Boolean,
    val message: String? = null
)

data class SuccessResponse(
    val success: Boolean,
    val message: String? = null
)

// ── WebSocket Events ──

data class WsEvent(
    val type: String,
    val data: Map<String, Any>? = null
)
