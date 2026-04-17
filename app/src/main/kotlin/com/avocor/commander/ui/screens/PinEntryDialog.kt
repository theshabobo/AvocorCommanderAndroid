package com.avocor.commander.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

/**
 * PIN entry dialog. Supports verify mode (check existing PIN) and setup mode (create new PIN).
 */
@Composable
fun PinEntryDialog(
    isSetup: Boolean = false,
    onVerified: () -> Unit,
    onDismiss: () -> Unit,
    verifyPin: ((String) -> Boolean)? = null,
    onPinCreated: ((String) -> Unit)? = null
) {
    var pin by remember { mutableStateOf("") }
    var confirmPin by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var failCount by remember { mutableIntStateOf(0) }
    var cooldownSeconds by remember { mutableIntStateOf(0) }

    // Cooldown timer
    LaunchedEffect(cooldownSeconds) {
        if (cooldownSeconds > 0) {
            delay(1000)
            cooldownSeconds--
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (isSetup) "Create Admin PIN" else "Enter Admin PIN",
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = pin,
                    onValueChange = { if (it.length <= 6 && it.all { c -> c.isDigit() }) pin = it },
                    label = { Text(if (isSetup) "New PIN" else "PIN") },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = cooldownSeconds == 0
                )

                if (isSetup) {
                    OutlinedTextField(
                        value = confirmPin,
                        onValueChange = {
                            if (it.length <= 6 && it.all { c -> c.isDigit() }) confirmPin = it
                        },
                        label = { Text("Confirm PIN") },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                if (error != null) {
                    Text(
                        text = error!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.Center
                    )
                }

                if (cooldownSeconds > 0) {
                    Text(
                        text = "Too many attempts. Try again in ${cooldownSeconds}s",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.Center
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (isSetup) {
                        when {
                            pin.length < 4 -> error = "PIN must be 4-6 digits"
                            pin != confirmPin -> error = "PINs don't match"
                            else -> {
                                onPinCreated?.invoke(pin)
                                onVerified()
                            }
                        }
                    } else {
                        if (verifyPin?.invoke(pin) == true) {
                            onVerified()
                        } else {
                            failCount++
                            error = "Incorrect PIN"
                            pin = ""
                            if (failCount >= 3) {
                                cooldownSeconds = 30
                                failCount = 0
                            }
                        }
                    }
                },
                enabled = cooldownSeconds == 0 && pin.length >= 4
            ) {
                Text(if (isSetup) "Set PIN" else "Unlock")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
