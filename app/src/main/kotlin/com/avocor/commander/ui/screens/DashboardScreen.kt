package com.avocor.commander.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Circle
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.avocor.commander.api.DeviceDto
import com.avocor.commander.api.WebSocketManager
import com.avocor.commander.ui.components.DeviceTile
import com.avocor.commander.ui.theme.StatusOffline
import com.avocor.commander.ui.theme.StatusOnline
import com.avocor.commander.ui.theme.StatusWarning
import com.avocor.commander.viewmodel.DeviceViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    deviceViewModel: DeviceViewModel,
    username: String,
    onDeviceSelected: (DeviceDto) -> Unit,
    onLogout: () -> Unit
) {
    val groups by deviceViewModel.groups.collectAsState()
    val statuses by deviceViewModel.statuses.collectAsState()
    val isRefreshing by deviceViewModel.isRefreshing.collectAsState()
    val wsState by deviceViewModel.wsConnectionState.collectAsState()
    val commandResult by deviceViewModel.commandResult.collectAsState(initial = null)

    val snackbarHostState = remember { SnackbarHostState() }

    // Show command results as snackbar
    LaunchedEffect(commandResult) {
        commandResult?.let {
            snackbarHostState.showSnackbar(it, duration = SnackbarDuration.Short)
        }
    }

    // Track expanded groups
    var expandedGroups by remember { mutableStateOf(setOf<Int>()) }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Avocor Commander")
                        Spacer(modifier = Modifier.width(8.dp))
                        // WebSocket status indicator
                        Icon(
                            imageVector = Icons.Filled.Circle,
                            contentDescription = "Connection status",
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
                    Text(
                        text = username,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    IconButton(onClick = { deviceViewModel.refresh() }) {
                        Icon(Icons.Filled.Refresh, contentDescription = "Refresh")
                    }
                    IconButton(onClick = onLogout) {
                        Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = "Logout")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = { deviceViewModel.refresh() },
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (groups.isEmpty() && !isRefreshing) {
                // Empty state
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "No devices found",
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Pull to refresh or check your server connection",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(groups, key = { it.id }) { group ->
                        val isExpanded = group.id in expandedGroups

                        // Group card
                        Card(
                            shape = MaterialTheme.shapes.medium,
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            )
                        ) {
                            Column {
                                // Group header
                                Surface(
                                    onClick = {
                                        expandedGroups = if (isExpanded) {
                                            expandedGroups - group.id
                                        } else {
                                            expandedGroups + group.id
                                        }
                                    },
                                    color = MaterialTheme.colorScheme.surface
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(16.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = group.name,
                                                style = MaterialTheme.typography.titleLarge,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            if (group.description != null) {
                                                Text(
                                                    text = group.description,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }

                                        // Device count badge
                                        val onlineCount = group.devices.count { device ->
                                            statuses.any { it.deviceId == device.id && it.isConnected }
                                                    || device.isConnected
                                        }
                                        Text(
                                            text = "$onlineCount/${group.devices.size}",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = if (onlineCount > 0) StatusOnline else StatusOffline
                                        )
                                    }
                                }

                                // Expanded device list
                                AnimatedVisibility(visible = isExpanded) {
                                    Column(
                                        modifier = Modifier.padding(
                                            start = 16.dp,
                                            end = 16.dp,
                                            bottom = 16.dp
                                        ),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        group.devices.forEach { device ->
                                            val deviceStatus = statuses.find {
                                                it.deviceId == device.id
                                            }
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
                    }
                }
            }
        }
    }
}
