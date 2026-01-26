package com.charliesbot.one.features.dashboard.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TimePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.charliesbot.shared.R
import com.charliesbot.shared.core.components.DateTimeWheelPickerDialog
import com.charliesbot.shared.core.utils.convertMillisToLocalDateTime
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

@OptIn(ExperimentalMaterial3Api::class)
private fun convertTimePickerStateToMillis(
    timePickerState: TimePickerState
): Long {
    val zoneId = ZoneId.systemDefault()
    val today = LocalDate.now(zoneId)
    val newDate =
        LocalDateTime.of(today, LocalTime.of(timePickerState.hour, timePickerState.minute))
    return newDate.atZone(zoneId).toInstant().toEpochMilli()
}

/**
 * Base TimePickerDialog that works with hour and minute values.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimePickerDialog(
    title: String = stringResource(R.string.time_picker_select_time),
    initialHour: Int,
    initialMinute: Int,
    onConfirm: (hour: Int, minute: Int) -> Unit,
    onDismiss: () -> Unit,
) {
    val timePickerState = rememberTimePickerState(
        initialHour = initialHour,
        initialMinute = initialMinute,
        is24Hour = false,
    )

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            shape = MaterialTheme.shapes.extraLarge,
            tonalElevation = 6.dp,
            modifier = Modifier
                .width(IntrinsicSize.Min)
                .height(IntrinsicSize.Min)
                .background(
                    shape = MaterialTheme.shapes.extraLarge,
                    color = MaterialTheme.colorScheme.surface
                ),
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 20.dp),
                    text = title,
                    style = MaterialTheme.typography.labelMedium
                )
                TimePicker(state = timePickerState)
                Row(
                    modifier = Modifier
                        .height(40.dp)
                        .fillMaxWidth()
                ) {
                    Spacer(modifier = Modifier.weight(1f))
                    TextButton(onClick = onDismiss) {
                        Text(stringResource(R.string.time_picker_cancel))
                    }
                    TextButton(onClick = {
                        onConfirm(timePickerState.hour, timePickerState.minute)
                    }) {
                        Text(stringResource(R.string.time_picker_save))
                    }
                }
            }
        }
    }
}

/**
 * TimePickerDialog overload that works with milliseconds (for selecting datetime).
 * Uses DateTimeWheelPicker to allow both date and time selection.
 */
@Composable
fun TimePickerDialog(
    startTimeMillis: Long,
    onConfirm: (Long) -> Unit,
    onDismiss: () -> Unit,
) {
    DateTimeWheelPickerDialog(
        initialDateTimeMillis = startTimeMillis,
        onConfirm = onConfirm,
        onDismiss = onDismiss
    )
}

/**
 * TimePickerDialog overload that works with minutes from midnight (for time-of-day selection).
 */
@Composable
fun TimePickerDialog(
    title: String,
    currentMinutes: Int,
    onConfirm: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    val initialHour = currentMinutes / 60
    val initialMinute = currentMinutes % 60

    TimePickerDialog(
        title = title,
        initialHour = initialHour,
        initialMinute = initialMinute,
        onConfirm = { hour, minute ->
            onConfirm(hour * 60 + minute)
        },
        onDismiss = onDismiss,
    )
}

@Preview
@Composable
private fun TimePickerDialogPreview() {
    TimePickerDialog(
        initialHour = 10,
        initialMinute = 30,
        onConfirm = { _, _ -> },
        onDismiss = {}
    )
}
