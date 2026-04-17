package com.avocor.commander.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Circle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.avocor.commander.api.DeviceDto
import com.avocor.commander.ui.components.QuickControlPanel
import com.avocor.commander.ui.theme.StatusOffline
import com.avocor.commander.ui.theme.StatusOnline
import com.avocor.commander.viewmodel.DeviceViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ControlScreen(
    deviceViewModel: DeviceViewModel,
    initialDevice: DeviceDto?,
    onBack: () -> Unit
) {
    val devices by deviceViewModel.devices.collectAsState()
    val statuses by deviceViewModel.statuses.collectAsState()
    val commandResult by deviceViewModel.commandResult.collectAsState(initial = null)

    var selectedDeviceId by remember {
        mutableIntStateOf(initialDevice?.id ?: devices.firstOrNull()?.id ?: -1)
    }
    var dropdownExpanded by remember { mutableStateOf(false) }

    val selectedDevice = devices.find { it.id == selectedDeviceId }
    val selectedStatus = statuses.find { it.deviceId == selectedDeviceId }

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(commandResult) {
        commandResult?.let {
            snackbarHostState.showSnackbar(it, duration = SnackbarDuration.Short)
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Device Control") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
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
            // Device picker
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Select Device",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    ExposedDropdownMenuBox(
                        expanded = dropdownExpanded,
                        onExpandedChange = { dropdownExpanded = it }
                    ) {
                        OutlinedTextField(
                            value = selectedDevice?.deviceName ?: "Select a device",
                            onValueChange = {},
                            readOnly = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(),
                            trailingIcon = {
                                ExposedDropdownMenuDefaults.TrailingIcon(expanded = dropdownExpanded)
                            },
                            leadingIcon = {
                                val isConnected = selectedStatus?.isConnected
                                    ?: selectedDevice?.isConnected ?: false
                                Icon(
                                    imageVector = Icons.Filled.Circle,
                                    contentDescription = null,
                                    modifier = Modifier.size(10.dp),
                                    tint = if (isConnected) StatusOnline else StatusOffline
                                )
                            }
                        )

                        ExposedDropdownMenu(
                            expanded = dropdownExpanded,
                            onDismissRequest = { dropdownExpanded = false }
                        ) {
                            devices.forEach { device ->
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
                                        dropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    // Device details
                    if (selectedDevice != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Model: ${selectedDevice.modelNumber}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (selectedDevice.ipAddress != null) {
                            Text(
                                text = "IP: ${selectedDevice.ipAddress}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        // Connect/Disconnect button
                        Spacer(modifier = Modifier.height(8.dp))
                        val isConnected = selectedStatus?.isConnected
                            ?: selectedDevice.isConnected
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (!isConnected) {
                                Button(
                                    onClick = {
                                        deviceViewModel.connectDevice(selectedDeviceId)
                                    }
                                ) {
                                    Text("Connect")
                                }
                                OutlinedButton(
                                    onClick = {
                                        deviceViewModel.wakeDevice(selectedDeviceId)
                                    }
                                ) {
                                    Text("Wake")
                                }
                            } else {
                                OutlinedButton(
                                    onClick = {
                                        deviceViewModel.disconnectDevice(selectedDeviceId)
                                    }
                                ) {
                                    Text("Disconnect")
                                }
                            }
                        }
                    }
                }
            }

            // Quick Controls
            if (selectedDevice != null) {
                QuickControlPanel(
                    onPowerOn = {
                        deviceViewModel.sendCommand(
                            selectedDeviceId, "power on", "power"
                        )
                    },
                    onPowerOff = {
                        deviceViewModel.sendCommand(
                            selectedDeviceId, "power off", "power"
                        )
                    },
                    onSource = { source ->
                        deviceViewModel.sendCommand(
                            selectedDeviceId, "input $source", "input"
                        )
                    },
                    onVolume = { level ->
                        deviceViewModel.sendCommand(
                            selectedDeviceId, "volume $level", "audio"
                        )
                    },
                    onMuteToggle = {
                        val muted = selectedStatus?.isMuted ?: false
                        deviceViewModel.sendCommand(
                            selectedDeviceId,
                            if (muted) "mute off" else "mute on",
                            "audio"
                        )
                    },
                    currentVolume = selectedStatus?.volume,
                    isMuted = selectedStatus?.isMuted
                )
            }
        }
    }
}
