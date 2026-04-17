package com.avocor.commander.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Circle
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.avocor.commander.api.WebSocketManager
import com.avocor.commander.model.CommandTarget
import com.avocor.commander.ui.components.QuickControlPanel
import com.avocor.commander.ui.theme.AvocorBlue
import com.avocor.commander.ui.theme.StatusOffline
import com.avocor.commander.ui.theme.StatusOnline
import com.avocor.commander.ui.theme.StatusWarning
import com.avocor.commander.viewmodel.DeviceViewModel
import com.avocor.commander.viewmodel.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TabletRoomScreen(
    settingsViewModel: SettingsViewModel,
    deviceViewModel: DeviceViewModel,
    onSettingsRequested: () -> Unit
) {
    val assignedRoomId by settingsViewModel.assignedRoomId.collectAsState()
    val assignedRoomName by settingsViewModel.assignedRoomName.collectAsState()

    val groups by deviceViewModel.groups.collectAsState()
    val allDevices by deviceViewModel.devices.collectAsState()
    val statuses by deviceViewModel.statuses.collectAsState()
    val wsState by deviceViewModel.wsConnectionState.collectAsState()
    val commandResult by deviceViewModel.commandResult.collectAsState(initial = null)

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(commandResult) {
        commandResult?.let {
            snackbarHostState.showSnackbar(it, duration = SnackbarDuration.Short)
        }
    }

    val roomId = assignedRoomId ?: return
    val group = groups.find { it.id == roomId }
    val roomDevices = allDevices.filter { it.id in (group?.memberDeviceIds ?: emptyList()) }

    // Command target: All Displays or a specific device
    var selectedDeviceId by remember { mutableStateOf<Int?>(null) }
    var pickerExpanded by remember { mutableStateOf(false) }

    val currentTarget = if (selectedDeviceId != null) {
        CommandTarget.SingleDevice(selectedDeviceId!!)
    } else {
        CommandTarget.AllInRoom(roomId)
    }

    val selectedDeviceName = if (selectedDeviceId != null) {
        roomDevices.find { it.id == selectedDeviceId }?.deviceName ?: "Unknown"
    } else {
        "All Displays"
    }

    // For volume/mute display in All mode, use first connected device
    val representativeStatus = if (selectedDeviceId != null) {
        statuses.find { it.deviceId == selectedDeviceId }
    } else {
        roomDevices.firstNotNullOfOrNull { device ->
            statuses.find { it.deviceId == device.id && it.isConnected }
        }
    }

    // PIN dialog for settings access
    var showPinDialog by remember { mutableStateOf(false) }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(assignedRoomName.ifEmpty { "Room" })
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            imageVector = Icons.Filled.Circle,
                            contentDescription = "Connection",
                            modifier = Modifier.size(8.dp),
                            tint = when (wsState) {
                                WebSocketManager.ConnectionState.CONNECTED -> StatusOnline
                                WebSocketManager.ConnectionState.CONNECTING -> StatusWarning
                                WebSocketManager.ConnectionState.DISCONNECTED -> StatusOffline
                            }
                        )
                    }
                },
                actions = {
                    // Device picker dropdown in the top bar
                    ExposedDropdownMenuBox(
                        expanded = pickerExpanded,
                        onExpandedChange = { pickerExpanded = it }
                    ) {
                        TextButton(
                            onClick = { pickerExpanded = true },
                            modifier = @Suppress("DEPRECATION") Modifier.menuAnchor()
                        ) {
                            Text(
                                text = selectedDeviceName,
                                color = if (selectedDeviceId == null) AvocorBlue
                                else MaterialTheme.colorScheme.onSurface,
                                style = MaterialTheme.typography.labelLarge
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = pickerExpanded)
                        }

                        ExposedDropdownMenu(
                            expanded = pickerExpanded,
                            onDismissRequest = { pickerExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        "All Displays",
                                        color = if (selectedDeviceId == null) AvocorBlue
                                        else MaterialTheme.colorScheme.onSurface
                                    )
                                },
                                onClick = {
                                    selectedDeviceId = null
                                    pickerExpanded = false
                                }
                            )

                            HorizontalDivider()

                            roomDevices.forEach { device ->
                                val deviceStatus = statuses.find { it.deviceId == device.id }
                                val isConnected = deviceStatus?.isConnected ?: device.isConnected

                                DropdownMenuItem(
                                    text = {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Filled.Circle,
                                                contentDescription = null,
                                                modifier = Modifier.size(8.dp),
                                                tint = if (isConnected) StatusOnline else StatusOffline
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(device.deviceName)
                                        }
                                    },
                                    onClick = {
                                        selectedDeviceId = device.id
                                        pickerExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    IconButton(onClick = { showPinDialog = true }) {
                        Icon(Icons.Filled.Settings, contentDescription = "Settings")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Quick Control Panel — wired to CommandTarget
            QuickControlPanel(
                onPowerOn = {
                    deviceViewModel.powerOnWithWakeFallback(currentTarget)
                },
                onPowerOff = {
                    deviceViewModel.sendTargetedCommand(currentTarget, "Power Off")
                },
                onSource = { source ->
                    deviceViewModel.sendTargetedCommand(currentTarget, source)
                },
                onVolume = { level ->
                    deviceViewModel.sendTargetedCommand(currentTarget, "Set Volume $level")
                },
                onMuteToggle = {
                    val muted = representativeStatus?.isMuted ?: false
                    deviceViewModel.sendTargetedCommand(
                        currentTarget,
                        if (muted) "Mute Off" else "Mute On"
                    )
                },
                currentVolume = representativeStatus?.volume,
                isMuted = representativeStatus?.isMuted
            )
        }
    }

    // PIN dialog for settings access
    if (showPinDialog) {
        PinEntryDialog(
            isSetup = false,
            onVerified = {
                showPinDialog = false
                onSettingsRequested()
            },
            onDismiss = { showPinDialog = false },
            verifyPin = { settingsViewModel.verifyPin(it) }
        )
    }
}
