package com.charliesbot.shared.core.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.charliesbot.shared.R
import com.charliesbot.shared.core.utils.convertMillisToLocalDateTime
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale

/**
 * AM/PM enum for 12-hour time format.
 */
enum class AmPm {
    AM, PM
}

/**
 * Data class representing a date item in the picker.
 */
data class DateItem(
    val date: LocalDate,
    val daysAgo: Int
)

/**
 * State holder for DateTimeWheelPicker.
 * Hoists the selected date/time state for easy access and testing.
 */
@Stable
class DateTimeWheelPickerState(
    initialDateTime: LocalDateTime
) {
    // Generate date list once at init time (not lazily recomputed)
    private val referenceDate: LocalDate = LocalDate.now()

    // Dates ordered oldest first (top) to newest (bottom)
    // So scrolling UP goes back in time
    val dateItems: List<DateItem> = (29 downTo 0).map { daysAgo ->
        DateItem(
            date = referenceDate.minusDays(daysAgo.toLong()),
            daysAgo = daysAgo
        )
    }

    val hourItems: List<Int> = (1..12).toList()
    val minuteItems: List<Int> = (0..59).toList()
    val amPmItems: List<AmPm> = listOf(AmPm.AM, AmPm.PM)

    var selectedDateIndex by mutableIntStateOf(findInitialDateIndex(initialDateTime.toLocalDate()))
    var selectedHourIndex by mutableIntStateOf(to12HourIndex(initialDateTime.hour))
    var selectedMinuteIndex by mutableIntStateOf(initialDateTime.minute)
    var selectedAmPmIndex by mutableIntStateOf(if (initialDateTime.hour >= 12) 1 else 0)

    val selectedDate: LocalDate
        get() = dateItems[selectedDateIndex].date

    val selectedHour: Int
        get() = hourItems[selectedHourIndex]

    val selectedMinute: Int
        get() = minuteItems[selectedMinuteIndex]

    val selectedAmPm: AmPm
        get() = amPmItems[selectedAmPmIndex]

    val selectedDateTime: LocalDateTime
        get() {
            val hour24 = to24Hour(selectedHour, selectedAmPm)
            return LocalDateTime.of(selectedDate, LocalTime.of(hour24, selectedMinute))
        }

    val selectedMillis: Long
        get() = selectedDateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

    private fun findInitialDateIndex(date: LocalDate): Int {
        val daysAgo = ChronoUnit.DAYS.between(date, referenceDate).toInt().coerceIn(0, 29)
        // Index 0 = 29 days ago, Index 29 = Today
        return 29 - daysAgo
    }

    private fun to12HourIndex(hour24: Int): Int {
        val hour12 = when {
            hour24 == 0 -> 12
            hour24 > 12 -> hour24 - 12
            else -> hour24
        }
        return hour12 - 1 // Convert to 0-indexed
    }

    /**
     * Returns true if the selected time is in the future.
     */
    val isFutureTime: Boolean
        get() = selectedMillis > System.currentTimeMillis()

    companion object {
        fun to24Hour(hour12: Int, amPm: AmPm): Int = when {
            amPm == AmPm.AM && hour12 == 12 -> 0
            amPm == AmPm.PM && hour12 == 12 -> 12
            amPm == AmPm.PM -> hour12 + 12
            else -> hour12
        }
    }
}

/**
 * Remember a DateTimeWheelPickerState initialized with the given milliseconds.
 */
@Composable
fun rememberDateTimeWheelPickerState(initialMillis: Long): DateTimeWheelPickerState {
    val initialDateTime = convertMillisToLocalDateTime(initialMillis)
    return remember(initialMillis) { DateTimeWheelPickerState(initialDateTime) }
}

/**
 * A combined date and time wheel picker with 4 columns:
 * Date | Hour | Minute | AM/PM
 */
private val ITEM_HEIGHT = 48.dp

@Composable
fun DateTimeWheelPicker(
    state: DateTimeWheelPickerState,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        // Unified selection indicator (behind)
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp)
                .height(ITEM_HEIGHT),
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f),
            shape = RoundedCornerShape(12.dp)
        ) {}

        // Wheel pickers row (on top)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Date column (wider)
            WheelPicker(
                items = state.dateItems,
                initialIndex = state.selectedDateIndex,
                onSelectedIndexChange = { state.selectedDateIndex = it },
                modifier = Modifier.weight(2f),
                infiniteScroll = false
            ) { dateItem ->
                Text(
                    text = formatDateForDisplay(dateItem, context),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center
                )
            }

            // Hour column
            WheelPicker(
                items = state.hourItems,
                initialIndex = state.selectedHourIndex,
                onSelectedIndexChange = { state.selectedHourIndex = it },
                modifier = Modifier.weight(1f),
                infiniteScroll = true
            ) { hour ->
                Text(
                    text = hour.toString().padStart(2, '0'),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center
                )
            }

            // Minute column
            WheelPicker(
                items = state.minuteItems,
                initialIndex = state.selectedMinuteIndex,
                onSelectedIndexChange = { state.selectedMinuteIndex = it },
                modifier = Modifier.weight(1f),
                infiniteScroll = true
            ) { minute ->
                Text(
                    text = minute.toString().padStart(2, '0'),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center
                )
            }

            // AM/PM column
            WheelPicker(
                items = state.amPmItems,
                initialIndex = state.selectedAmPmIndex,
                onSelectedIndexChange = { state.selectedAmPmIndex = it },
                modifier = Modifier.weight(0.8f),
                infiniteScroll = false
            ) { amPm ->
                Text(
                    text = amPm.name,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

/**
 * Format date for display in the wheel picker.
 * "Today", "Yesterday", or "EEE d MMM" format.
 */
private fun formatDateForDisplay(dateItem: DateItem, context: android.content.Context): String {
    return when (dateItem.daysAgo) {
        0 -> context.getString(R.string.wheel_picker_today)
        1 -> context.getString(R.string.wheel_picker_yesterday)
        else -> {
            val formatter = DateTimeFormatter.ofPattern("EEE d MMM", Locale.getDefault())
            dateItem.date.format(formatter)
        }
    }
}

/**
 * Dialog wrapper for DateTimeWheelPicker.
 *
 * @param initialDateTimeMillis Initial time to show in the picker
 * @param onConfirm Called when user confirms the selection
 * @param onDismiss Called when user dismisses the dialog
 * @param buttonText Custom text for the confirm button (defaults to "Update starting time")
 * @param isValidSelection Additional validation for the selected time. Combined with future time check.
 */
@Composable
fun DateTimeWheelPickerDialog(
    initialDateTimeMillis: Long,
    onConfirm: (Long) -> Unit,
    onDismiss: () -> Unit,
    buttonText: String = stringResource(R.string.wheel_picker_update_start_time),
    isValidSelection: (selectedMillis: Long) -> Boolean = { true }
) {
    val state = rememberDateTimeWheelPickerState(initialDateTimeMillis)
    val isEnabled = !state.isFutureTime && isValidSelection(state.selectedMillis)

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = MaterialTheme.shapes.extraLarge,
            tonalElevation = 6.dp,
            modifier = Modifier
                .width(IntrinsicSize.Min)
                .background(
                    shape = MaterialTheme.shapes.extraLarge,
                    color = MaterialTheme.colorScheme.surface
                )
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header with close button
                Box(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.align(Alignment.TopEnd)
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.close_24px),
                            contentDescription = stringResource(R.string.time_picker_cancel)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Date Time Wheel Picker
                DateTimeWheelPicker(
                    state = state,
                    modifier = Modifier.width(320.dp)
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Confirm button (disabled for future times or invalid selection)
                FilledTonalButton(
                    onClick = { onConfirm(state.selectedMillis) },
                    enabled = isEnabled,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 32.dp),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Text(
                        text = buttonText,
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun DateTimeWheelPickerPreview() {
    val state = remember {
        DateTimeWheelPickerState(LocalDateTime.now())
    }
    DateTimeWheelPicker(state = state)
}

@Preview(showBackground = true)
@Composable
private fun DateTimeWheelPickerDialogPreview() {
    DateTimeWheelPickerDialog(
        initialDateTimeMillis = System.currentTimeMillis(),
        onConfirm = {},
        onDismiss = {}
    )
}
