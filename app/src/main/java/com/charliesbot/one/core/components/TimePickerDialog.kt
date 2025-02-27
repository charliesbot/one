package com.charliesbot.one.core.components

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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.charliesbot.shared.core.utils.convertMillisToLocalDateTime
import com.charliesbot.one.ui.theme.OneTheme
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

@OptIn(ExperimentalMaterial3Api::class)
fun convertTimePickerStateToMillis(
    timePickerState: TimePickerState
): Long {
    val zoneId = ZoneId.systemDefault()
    val today = LocalDate.now(zoneId)
    val newDate =
        LocalDateTime.of(today, LocalTime.of(timePickerState.hour, timePickerState.minute))
    return newDate.atZone(zoneId).toInstant().toEpochMilli()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimePickerDialog(
    startTimeMillis: Long,
    onConfirm: (Long) -> Unit,
    onDismiss: () -> Unit,
) {
    val startTime = convertMillisToLocalDateTime(startTimeMillis)
    val timePickerState = rememberTimePickerState(
        initialHour = startTime.hour,
        initialMinute = startTime.minute,
        is24Hour = false,
    )

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            shape = MaterialTheme.shapes.extraLarge,
            tonalElevation = 6.dp,
            modifier =
            Modifier
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
                    text = "Select Time",
                    style = MaterialTheme.typography.labelMedium
                )
                TimePicker(
                    state = timePickerState,
                )
                Row(
                    modifier = Modifier
                        .height(40.dp)
                        .fillMaxWidth()
                ) {
                    Spacer(modifier = Modifier.weight(1f))
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                    TextButton(onClick = {
                        onConfirm(
                            convertTimePickerStateToMillis(
                                timePickerState
                            )
                        )
                    }) { Text("Save") }
                }
            }
        }
    }
}

@Preview
@Composable
fun InnerTimePickerPreview() {
    OneTheme {
        TimePickerDialog(onConfirm = {}, onDismiss = {}, startTimeMillis = 639716400000)
    }
}