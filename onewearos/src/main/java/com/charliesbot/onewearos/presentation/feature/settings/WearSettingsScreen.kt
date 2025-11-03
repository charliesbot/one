package com.charliesbot.onewearos.presentation.feature.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.TextButton
import androidx.wear.compose.material3.TextToggleButton
import androidx.wear.tooling.preview.devices.WearDevices
import com.google.android.horologist.composables.TimePicker
import com.google.android.horologist.composables.TimePickerResult
import org.koin.androidx.compose.koinViewModel
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@Composable
fun WearSettingsScreen(
    viewModel: WearSettingsViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showTimePicker by remember { mutableStateOf(false) }

    if (showTimePicker) {
        TimePicker(
            onTimeConfirm = { hour, minute ->
                viewModel.setBedtime(LocalTime.of(hour, minute))
                showTimePicker = false
            },
            time = uiState.bedtime ?: LocalTime.of(22, 0) // Default 10 PM
        ) {
            showTimePicker = false
        }
    } else {
        WearSettingsContent(
            smartNotificationsEnabled = uiState.smartNotificationsEnabled,
            vibrationEnabled = uiState.vibrationEnabled,
            bedtime = uiState.bedtime,
            calculatedNotificationTime = uiState.calculatedNotificationTime,
            onToggleSmartNotifications = viewModel::toggleSmartNotifications,
            onToggleVibration = viewModel::toggleVibration,
            onSetBedtime = { showTimePicker = true },
            onClearBedtime = { viewModel.setBedtime(null) }
        )
    }
}

@Composable
private fun WearSettingsContent(
    smartNotificationsEnabled: Boolean,
    vibrationEnabled: Boolean,
    bedtime: LocalTime?,
    calculatedNotificationTime: String,
    onToggleSmartNotifications: () -> Unit,
    onToggleVibration: () -> Unit,
    onSetBedtime: () -> Unit,
    onClearBedtime: () -> Unit,
    modifier: Modifier = Modifier
) {
    ScreenScaffold {
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Title
            Text(
                text = "Settings",
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Smart Notifications Toggle
            TextToggleButton(
                checked = smartNotificationsEnabled,
                onCheckedChange = { onToggleSmartNotifications() },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Smart Notifications",
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center
                )
            }

            // Vibration Toggle
            TextToggleButton(
                checked = vibrationEnabled,
                onCheckedChange = { onToggleVibration() },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Vibration",
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center
                )
            }

            // Bedtime Button
            TextButton(
                onClick = onSetBedtime,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Bedtime",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = bedtime?.format(DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT))
                            ?: "Not set",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Clear bedtime if set
            if (bedtime != null) {
                TextButton(
                    onClick = onClearBedtime,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Clear Bedtime",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Next reminder info (read-only)
            if (smartNotificationsEnabled) {
                Text(
                    text = "Next reminder:",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = calculatedNotificationTime,
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Preview(device = WearDevices.LARGE_ROUND, showSystemUi = true)
@Composable
private fun WearSettingsScreenPreview() {
    MaterialTheme {
        WearSettingsContent(
            smartNotificationsEnabled = true,
            vibrationEnabled = true,
            bedtime = LocalTime.of(22, 30),
            calculatedNotificationTime = "8:00 PM",
            onToggleSmartNotifications = {},
            onToggleVibration = {},
            onSetBedtime = {},
            onClearBedtime = {}
        )
    }
}

