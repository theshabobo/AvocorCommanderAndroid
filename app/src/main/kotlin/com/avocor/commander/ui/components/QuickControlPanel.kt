package com.avocor.commander.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.avocor.commander.ui.theme.AvocorBlue
import com.avocor.commander.ui.theme.StatusError

@Composable
fun QuickControlPanel(
    onPowerOn: () -> Unit,
    onPowerOff: () -> Unit,
    onSource: (String) -> Unit,
    onVolume: (Int) -> Unit,
    onMuteToggle: () -> Unit,
    currentVolume: Int?,
    isMuted: Boolean?,
    modifier: Modifier = Modifier
) {
    var volumeSlider by remember(currentVolume) {
        mutableFloatStateOf((currentVolume ?: 50).toFloat())
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Power controls
            Text(
                text = "Power",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = onPowerOn,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AvocorBlue
                    )
                ) {
                    Icon(Icons.Filled.PowerSettingsNew, contentDescription = null,
                        modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("ON")
                }
                Button(
                    onClick = onPowerOff,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = StatusError
                    )
                ) {
                    Icon(Icons.Filled.PowerOff, contentDescription = null,
                        modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("OFF")
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))

            // Source selection
            Text(
                text = "Input Source",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )

            // Row 1: HDMI sources
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SourceButton("HDMI 1", Modifier.weight(1f)) { onSource("HDMI 1") }
                SourceButton("HDMI 2", Modifier.weight(1f)) { onSource("HDMI 2") }
            }
            // Row 2: Other sources
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SourceButton("DP", Modifier.weight(1f)) { onSource("DisplayPort 1") }
                SourceButton("USB-C", Modifier.weight(1f)) { onSource("USB-C") }
                SourceButton("Home", Modifier.weight(1f)) { onSource("HOME") }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))

            // Volume
            Text(
                text = "Volume",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = if (isMuted == true) Icons.Filled.VolumeOff
                        else Icons.Filled.VolumeUp,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(24.dp)
                )

                Slider(
                    value = volumeSlider,
                    onValueChange = { volumeSlider = it },
                    onValueChangeFinished = { onVolume(volumeSlider.toInt()) },
                    valueRange = 0f..100f,
                    steps = 3, // 0, 25, 50, 75, 100
                    modifier = Modifier.weight(1f),
                    colors = SliderDefaults.colors(
                        thumbColor = AvocorBlue,
                        activeTrackColor = AvocorBlue
                    )
                )

                Text(
                    text = "${volumeSlider.toInt()}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.width(32.dp)
                )
            }

            // Volume presets + Mute
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(0, 25, 50, 75, 100).forEach { level ->
                    FilledTonalButton(
                        onClick = {
                            volumeSlider = level.toFloat()
                            onVolume(level)
                        },
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 4.dp)
                    ) {
                        Text("$level", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }

            // Mute toggle
            Button(
                onClick = onMuteToggle,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isMuted == true) StatusError
                        else MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = if (isMuted == true) MaterialTheme.colorScheme.onError
                        else MaterialTheme.colorScheme.onSurfaceVariant
                )
            ) {
                Icon(
                    imageVector = if (isMuted == true) Icons.Filled.VolumeOff
                        else Icons.Filled.VolumeMute,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(if (isMuted == true) "UNMUTE" else "MUTE")
            }
        }
    }
}

@Composable
private fun SourceButton(
    label: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier
    ) {
        Text(label, style = MaterialTheme.typography.labelLarge)
    }
}
