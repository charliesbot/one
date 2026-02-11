package com.charliesbot.one.features.profile.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import com.charliesbot.one.features.dashboard.components.TimePickerDialog
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import kotlinx.coroutines.launch
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.charliesbot.shared.R
import com.charliesbot.shared.core.components.FastingDayData
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FastingDetailsBottomSheet(
    fastingData: FastingDayData,
    onDismiss: () -> Unit,
    onDelete: () -> Unit,
    onUpdateStartTime: (newStartTime: Long) -> Unit,
    onUpdateEndTime: (newEndTime: Long) -> Unit,
    modifier: Modifier = Modifier,
    sheetState: SheetState = rememberModalBottomSheetState()
) {
    var showDeleteConfirmation by remember { mutableStateOf(false) }
    var showStartTimePicker by remember { mutableStateOf(false) }
    var showEndTimePicker by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    // Local state for times - allows immediate UI updates
    var currentStartTime by remember { mutableLongStateOf(fastingData.startTimeEpochMillis ?: 0L) }
    var currentEndTime by remember { mutableLongStateOf(fastingData.endTimeEpochMillis ?: 0L) }

    // Derive date from end time (fasts are grouped by end date in the calendar)
    val currentDate = remember(currentEndTime) {
        Instant.ofEpochMilli(currentEndTime)
            .atZone(ZoneId.systemDefault())
            .toLocalDate()
    }

    if (showDeleteConfirmation) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmation = false },
            title = { Text(stringResource(R.string.delete_confirmation_title)) },
            text = { Text(stringResource(R.string.delete_confirmation_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirmation = false
                        scope.launch {
                            sheetState.hide()
                            onDelete()
                        }
                    }
                ) {
                    Text(
                        text = stringResource(R.string.delete),
                        color = MaterialTheme.colorScheme.error
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmation = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    // Start time picker dialog
    if (showStartTimePicker) {
        TimePickerDialog(
            startTimeMillis = currentStartTime,
            onConfirm = { newStartTime ->
                currentStartTime = newStartTime
                onUpdateStartTime(newStartTime)
                showStartTimePicker = false
            },
            onDismiss = { showStartTimePicker = false },
            buttonText = stringResource(R.string.wheel_picker_update_start_time),
            isValidSelection = { selectedTime -> selectedTime < currentEndTime }
        )
    }

    // End time picker dialog
    if (showEndTimePicker) {
        TimePickerDialog(
            startTimeMillis = currentEndTime,
            onConfirm = { newEndTime ->
                currentEndTime = newEndTime
                onUpdateEndTime(newEndTime)
                showEndTimePicker = false
            },
            onDismiss = { showEndTimePicker = false },
            buttonText = stringResource(R.string.wheel_picker_update_end_time),
            isValidSelection = { selectedTime -> selectedTime > currentStartTime }
        )
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp)
        ) {
            // Date Header (derived from end time)
            Text(
                text = formatDate(currentDate),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Duration (uses local state for immediate updates)
            Text(
                text = formatDuration(currentStartTime, currentEndTime),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(24.dp))

            HorizontalDivider()

            Spacer(modifier = Modifier.height(16.dp))

            // Start Time
            TimeRow(
                icon = {
                    Icon(
                        painter = painterResource(R.drawable.play_arrow_24px),
                        contentDescription = "Start",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                },
                label = stringResource(R.string.started),
                time = formatDateTime(currentStartTime),
                onClick = { showStartTimePicker = true }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // End Time
            TimeRow(
                icon = {
                    Icon(
                        painter = painterResource(R.drawable.stop_24px),
                        contentDescription = "End",
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(24.dp)
                    )
                },
                label = stringResource(R.string.ended),
                time = formatDateTime(currentEndTime),
                onClick = { showEndTimePicker = true }
            )
            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider()

            Spacer(modifier = Modifier.height(16.dp))

            TextButton(
                onClick = { showDeleteConfirmation = true }
            ) {
                Icon(
                    painter = painterResource(R.drawable.delete_24px),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.size(8.dp))
                Text(
                    text = stringResource(R.string.delete_entry),
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
private fun TimeRow(
    icon: @Composable () -> Unit,
    label: String,
    time: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        icon()

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = time,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        Icon(
            painter = painterResource(R.drawable.chevron_right_24px),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp)
        )
    }
}

// Formatting Functions

private fun formatDate(date: LocalDate): String {
    val formatter = DateTimeFormatter.ofPattern("MMMM d, yyyy")
    return date.format(formatter)
}

private fun formatDateTime(epochMillis: Long): String {
    val instant = Instant.ofEpochMilli(epochMillis)
    val zonedDateTime = instant.atZone(ZoneId.systemDefault())
    // Format: "Feb 10, 7:15 PM" (month, day, time - no year)
    val formatter = DateTimeFormatter.ofPattern("MMM d, h:mm a")
    return zonedDateTime.format(formatter)
}

private fun formatDuration(startMillis: Long, endMillis: Long): String {
    val durationMillis = endMillis - startMillis
    val hours = durationMillis / 3_600_000
    val minutes = (durationMillis % 3_600_000) / 60_000

    return when {
        hours > 0 && minutes > 0 -> "$hours hours $minutes minutes"
        hours > 0 -> "$hours hours"
        else -> "$minutes minutes"
    }
}

// Preview

@OptIn(ExperimentalMaterial3Api::class)
@Preview(showBackground = true)
@Composable
private fun FastingDetailsBottomSheetPreview() {
    MaterialTheme {
        val now = System.currentTimeMillis()
        val testData = FastingDayData(
            date = LocalDate.now().minusDays(1),
            durationHours = 16,
            isGoalMet = true,
            startTimeEpochMillis = now - (16 * 60 * 60 * 1000) - (15 * 60 * 1000), // 16h 15m ago
            endTimeEpochMillis = now - (15 * 60 * 1000) // 15 minutes ago
        )

        FastingDetailsBottomSheet(
            fastingData = testData,
            onDismiss = {},
            onDelete = {},
            onUpdateStartTime = {},
            onUpdateEndTime = {}
        )
    }
}

