package com.avocor.commander.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.avocor.commander.api.GroupDto
import com.avocor.commander.ui.theme.AvocorBlue
import com.avocor.commander.viewmodel.AppMode
import com.avocor.commander.viewmodel.DeviceViewModel
import com.avocor.commander.viewmodel.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    settingsViewModel: SettingsViewModel,
    deviceViewModel: DeviceViewModel,
    onBack: () -> Unit
) {
    val appMode by settingsViewModel.appMode.collectAsState()
    val assignedRoomId by settingsViewModel.assignedRoomId.collectAsState()
    val assignedRoomName by settingsViewModel.assignedRoomName.collectAsState()
    val kioskEnabled by settingsViewModel.kioskEnabled.collectAsState()
    val hasPinConfigured by settingsViewModel.hasPinConfigured.collectAsState()
    val groups by deviceViewModel.groups.collectAsState()

    var showPinDialog by remember { mutableStateOf(false) }
    var pinAction by remember { mutableStateOf<PinAction>(PinAction.None) }
    var showRoomPicker by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
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
            // Mode Toggle
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "App Mode",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Phone mode for mobile use. Tablet mode for wall-mounted room panels.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                        SegmentedButton(
                            selected = appMode == AppMode.PHONE,
                            onClick = { settingsViewModel.setMode(AppMode.PHONE) },
                            shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
                        ) {
                            Text("Phone")
                        }
                        SegmentedButton(
                            selected = appMode == AppMode.TABLET,
                            onClick = {
                                if (!hasPinConfigured) {
                                    // Force PIN setup before switching to tablet mode
                                    pinAction = PinAction.SetupThenSwitchMode
                                    showPinDialog = true
                                } else {
                                    settingsViewModel.setMode(AppMode.TABLET)
                                }
                            },
                            shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
                        ) {
                            Text("Tablet")
                        }
                    }
                }
            }

            // Tablet Settings (only visible in tablet mode)
            if (appMode == AppMode.TABLET) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Tablet Configuration",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        // Room Assignment
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Assigned Room",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = if (assignedRoomId != null) assignedRoomName
                                    else "Not assigned",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Button(
                                onClick = {
                                    pinAction = PinAction.AssignRoom
                                    showPinDialog = true
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = AvocorBlue)
                            ) {
                                Text(if (assignedRoomId != null) "Change" else "Assign")
                            }
                        }

                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 12.dp),
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                        )

                        // Kiosk Toggle
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Kiosk Lockdown",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Prevent app switching, hide navigation bars",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = kioskEnabled,
                                onCheckedChange = { enabled ->
                                    if (enabled) {
                                        pinAction = PinAction.EnableKiosk
                                        showPinDialog = true
                                    } else {
                                        pinAction = PinAction.DisableKiosk
                                        showPinDialog = true
                                    }
                                }
                            )
                        }

                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 12.dp),
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                        )

                        // Change PIN
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Change Admin PIN",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            OutlinedButton(onClick = {
                                pinAction = PinAction.ChangePin
                                showPinDialog = true
                            }) {
                                Text("Change")
                            }
                        }
                    }
                }
            }

            // PIN Setup (visible when no PIN configured and in phone mode)
            if (!hasPinConfigured && appMode == AppMode.PHONE) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Admin PIN",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Set a PIN to enable tablet mode. Required for room assignment and kiosk settings.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = {
                                pinAction = PinAction.SetupPin
                                showPinDialog = true
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = AvocorBlue)
                        ) {
                            Text("Set PIN")
                        }
                    }
                }
            }
        }
    }

    // PIN Dialog
    if (showPinDialog) {
        when (pinAction) {
            PinAction.SetupPin, PinAction.SetupThenSwitchMode -> {
                PinEntryDialog(
                    isSetup = true,
                    onVerified = {
                        showPinDialog = false
                        if (pinAction == PinAction.SetupThenSwitchMode) {
                            settingsViewModel.setMode(AppMode.TABLET)
                        }
                    },
                    onDismiss = { showPinDialog = false },
                    onPinCreated = { settingsViewModel.setPin(it) }
                )
            }

            PinAction.ChangePin -> {
                // First verify current PIN, then setup new one
                var verified by remember { mutableStateOf(false) }
                if (!verified) {
                    PinEntryDialog(
                        isSetup = false,
                        onVerified = { verified = true },
                        onDismiss = { showPinDialog = false },
                        verifyPin = { settingsViewModel.verifyPin(it) }
                    )
                } else {
                    PinEntryDialog(
                        isSetup = true,
                        onVerified = { showPinDialog = false },
                        onDismiss = { showPinDialog = false },
                        onPinCreated = { settingsViewModel.setPin(it) }
                    )
                }
            }

            PinAction.AssignRoom -> {
                PinEntryDialog(
                    isSetup = false,
                    onVerified = {
                        showPinDialog = false
                        showRoomPicker = true
                    },
                    onDismiss = { showPinDialog = false },
                    verifyPin = { settingsViewModel.verifyPin(it) }
                )
            }

            PinAction.EnableKiosk -> {
                PinEntryDialog(
                    isSetup = false,
                    onVerified = {
                        showPinDialog = false
                        settingsViewModel.setKioskEnabled(true)
                    },
                    onDismiss = { showPinDialog = false },
                    verifyPin = { settingsViewModel.verifyPin(it) }
                )
            }

            PinAction.DisableKiosk -> {
                PinEntryDialog(
                    isSetup = false,
                    onVerified = {
                        showPinDialog = false
                        settingsViewModel.setKioskEnabled(false)
                    },
                    onDismiss = { showPinDialog = false },
                    verifyPin = { settingsViewModel.verifyPin(it) }
                )
            }

            PinAction.None -> { showPinDialog = false }
        }
    }

    // Room Picker Dialog
    if (showRoomPicker) {
        RoomPickerDialog(
            groups = groups,
            currentRoomId = assignedRoomId,
            onRoomSelected = { group ->
                settingsViewModel.assignRoom(group.id, group.groupName)
                showRoomPicker = false
            },
            onDismiss = { showRoomPicker = false }
        )
    }
}

private enum class PinAction {
    None, SetupPin, SetupThenSwitchMode, ChangePin, AssignRoom, EnableKiosk, DisableKiosk
}

@Composable
private fun RoomPickerDialog(
    groups: List<GroupDto>,
    currentRoomId: Int?,
    onRoomSelected: (GroupDto) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select Room") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                if (groups.isEmpty()) {
                    Text(
                        text = "No rooms/groups found. Create groups on the server first.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    groups.forEach { group ->
                        val isSelected = group.id == currentRoomId
                        Surface(
                            onClick = { onRoomSelected(group) },
                            color = if (isSelected) MaterialTheme.colorScheme.primaryContainer
                            else MaterialTheme.colorScheme.surface,
                            shape = MaterialTheme.shapes.small
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = group.groupName,
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "${group.memberDeviceIds.size} device(s)",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                if (isSelected) {
                                    Text(
                                        text = "Current",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
