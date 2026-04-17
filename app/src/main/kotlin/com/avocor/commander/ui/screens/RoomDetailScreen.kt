package com.avocor.commander.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.avocor.commander.api.DeviceDto
import com.avocor.commander.api.GroupDto
import com.avocor.commander.ui.components.DeviceTile
import com.avocor.commander.ui.theme.StatusOffline
import com.avocor.commander.ui.theme.StatusOnline
import com.avocor.commander.viewmodel.DeviceViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoomDetailScreen(
    groupId: Int,
    deviceViewModel: DeviceViewModel,
    onDeviceSelected: (DeviceDto) -> Unit,
    onBack: () -> Unit
) {
    val groups by deviceViewModel.groups.collectAsState()
    val allDevices by deviceViewModel.devices.collectAsState()
    val statuses by deviceViewModel.statuses.collectAsState()
    val commandResult by deviceViewModel.commandResult.collectAsState(initial = null)

    val group = groups.find { it.id == groupId }
    val roomDevices = allDevices.filter { it.id in (group?.memberDeviceIds ?: emptyList()) }

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
                title = {
                    Column {
                        Text(group?.groupName ?: "Room")
                        if (!group?.notes.isNullOrBlank()) {
                            Text(
                                text = group!!.notes!!,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    // Online count badge
                    val onlineCount = roomDevices.count { device ->
                        statuses.any { it.deviceId == device.id && it.isConnected }
                                || device.isConnected
                    }
                    Text(
                        text = "$onlineCount/${roomDevices.size} online",
                        style = MaterialTheme.typography.labelMedium,
                        color = if (onlineCount > 0) StatusOnline else StatusOffline,
                        modifier = Modifier.padding(end = 16.dp)
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        if (roomDevices.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No devices in this room",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(roomDevices, key = { it.id }) { device ->
                    val deviceStatus = statuses.find { it.deviceId == device.id }
                    DeviceTile(
                        device = device,
                        status = deviceStatus,
                        onTap = { onDeviceSelected(device) },
                        onWake = { deviceViewModel.wakeDevice(device.id) }
                    )
                }
            }
        }
    }
}
