package com.charliesbot.shared.core.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import com.charliesbot.shared.R
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.ButtonGroup
import androidx.compose.material3.Icon
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

data class FastingDayData(
    val date: LocalDate,
    val durationHours: Int? = null, // null means no fasting that day
    val isGoalMet: Boolean = false,
    val startTimeEpochMillis: Long? = null,
    val endTimeEpochMillis: Long? = null,
    val goalId: String? = null
)

@Composable
fun FastingMonthCalendar(
    modifier: Modifier = Modifier,
    yearMonth: YearMonth,
    fastingData: Map<LocalDate, FastingDayData> = emptyMap(),
    firstDayOfWeek: DayOfWeek = DayOfWeek.SUNDAY,
    onDayClick: (LocalDate) -> Unit = {},
    onPreviousMonth: () -> Unit = {},
    onNextMonth: () -> Unit = {}
) {
    val monthName = yearMonth.format(DateTimeFormatter.ofPattern("MMM ''yy"))

    Column(
        modifier = modifier.padding(16.dp)
    ) {
        MonthHeader(
            monthName = monthName,
            onPreviousMonth = onPreviousMonth,
            onNextMonth = onNextMonth
        )

        Spacer(modifier = Modifier.height(28.dp))

        // Week Days Header
        WeekDaysHeader(firstDayOfWeek = firstDayOfWeek)

        Spacer(modifier = Modifier.height(8.dp))

        // Days Grid
        DayGrid(
            yearMonth = yearMonth,
            fastingData = fastingData,
            firstDayOfWeek = firstDayOfWeek,
            onDayClick = onDayClick
        )
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun MonthHeader(
    monthName: String,
    onPreviousMonth: () -> Unit = {},
    onNextMonth: () -> Unit = {},
) {
    val interactionSources = List(size = 2) { MutableInteractionSource() }
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = monthName,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        ButtonGroup(
            overflowIndicator = {},
            expandedRatio = 0f,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            customItem(
                buttonGroupContent = {
                    FilledIconButton(
                        onClick = {onPreviousMonth()},
                        modifier = Modifier.animateWidth(interactionSource = interactionSources[0]),
                        shapes = IconButtonDefaults.shapes(),
                        colors =
                            IconButtonDefaults.filledIconButtonColors(
                                containerColor =
                                    MaterialTheme.colorScheme.tertiary,
                            ),
                        interactionSource = interactionSources[0],
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.chevron_left_24px),
                            contentDescription = null,
                        )
                    }
                },
                menuContent = {}
            )
            customItem(
                buttonGroupContent = {
                    FilledIconButton(
                        onClick = {onNextMonth()},
                        modifier = Modifier.animateWidth(interactionSource = interactionSources[1]),
                        shapes = IconButtonDefaults.shapes(),
                        interactionSource = interactionSources[1],
                        colors =
                            IconButtonDefaults.filledIconButtonColors(
                                containerColor =
                                    MaterialTheme.colorScheme.tertiary,
                            ),
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.chevron_right_24px),
                            contentDescription = null,
                        )
                    }
                },
                menuContent = {}
            )
        }
    }
}

@Composable
private fun WeekDaysHeader(firstDayOfWeek: DayOfWeek) {
    val weekDays = getWeekDaysLabels(firstDayOfWeek)

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        weekDays.forEach { day ->
            Text(
                text = day,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

private fun getWeekDaysLabels(firstDayOfWeek: DayOfWeek): List<String> {
    // 1. Get all 7 day-of-week enums in their declared order
    // This list is [MONDAY, TUESDAY, ..., SUNDAY]
    val days = DayOfWeek.entries.toTypedArray()

    // 2. Get the narrow display name (e.g., "M", "T", "S")
    val labels = days.map { it.getDisplayName(TextStyle.NARROW, Locale.getDefault()) }

    // 3. We need to find the *index* of the starting day.
    // MONDAY (value 1) is at index 0.
    // SUNDAY (value 7) is at index 6.
    val startIndex = firstDayOfWeek.value - 1

    // 4. Rotate the list
    // If startIndex is 0 (Monday): .drop(0) + .take(0) -> [M, T, W, T, F, S, S]
    // If startIndex is 6 (Sunday): .drop(6) + .take(6) -> [S, M, T, W, T, F, S]
    return labels.drop(startIndex) + labels.take(startIndex)
}

@Composable
private fun DayGrid(
    yearMonth: YearMonth,
    fastingData: Map<LocalDate, FastingDayData>,
    firstDayOfWeek: DayOfWeek,
    onDayClick: (LocalDate) -> Unit
) {
    val firstDayOfMonth = yearMonth.atDay(1)
    val startOffset = calculateStartOffset(firstDayOfMonth.dayOfWeek, firstDayOfWeek)
    val daysInMonth = yearMonth.lengthOfMonth()

    // Create 6 weeks grid
    repeat(6) { week ->
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 2.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            repeat(7) { dayOfWeek ->
                val dayNumber = week * 7 + dayOfWeek - startOffset + 1

                if (dayNumber in 1..daysInMonth) {
                    val date = yearMonth.atDay(dayNumber)
                    val fastingDayData = fastingData[date]

                    FastingDayCell(
                        dayNumber = dayNumber,
                        fastingData = fastingDayData,
                        onClick = { onDayClick(date) },
                        modifier = Modifier.weight(1f)
                    )
                } else {
                    // Empty cell for padding
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

private fun calculateStartOffset(firstDayOfMonth: DayOfWeek, calendarFirstDay: DayOfWeek): Int {
    return (firstDayOfMonth.value - calendarFirstDay.value + 7) % 7
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun FastingDayCell(
    dayNumber: Int,
    fastingData: FastingDayData?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val backgroundColor = when {
        fastingData?.durationHours != null && fastingData.isGoalMet -> MaterialTheme.colorScheme.primary
        fastingData?.durationHours != null -> MaterialTheme.colorScheme.surfaceVariant
        else -> Color.Transparent
    }

    val contentColor = when {
        fastingData?.durationHours != null && fastingData.isGoalMet -> MaterialTheme.colorScheme.onPrimary
        fastingData?.durationHours != null -> MaterialTheme.colorScheme.onSurfaceVariant
        else -> MaterialTheme.colorScheme.onSurface
    }

    Box(
        modifier = modifier
            .padding(4.dp)
            .size(44.dp)
            .clip(MaterialShapes.Square.toShape())
            .background(backgroundColor)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = dayNumber.toString(),
                style = MaterialTheme.typography.bodyMedium,
                color = contentColor,
            )

            fastingData?.durationHours?.let { hours ->
                Text(
                    text = "${hours}h",
                    style = MaterialTheme.typography.labelMediumEmphasized,
                    color = contentColor,
                    fontWeight = if (fastingData.isGoalMet) FontWeight.Bold else FontWeight.Normal
                )
            }
        }
    }
}

// Hardcoded data for preview
fun createMockFastingData(yearMonth: YearMonth): Map<LocalDate, FastingDayData> {
    val data = mutableMapOf<LocalDate, FastingDayData>()
    val daysInMonth = yearMonth.lengthOfMonth()

    repeat(daysInMonth) { dayIndex ->
        val date = yearMonth.atDay(dayIndex + 1)
        val randomHours = (10..24).random()
        val isGoalMet = randomHours >= 16 // Assume 16h is the goal

        // Not every day has fasting data
        if ((dayIndex + 1) % 3 != 0) { // Skip every 3rd day
            data[date] = FastingDayData(
                date = date,
                durationHours = randomHours,
                isGoalMet = isGoalMet
            )
        }
    }

    return data
}

@Preview(showBackground = true, name = "Sunday Start")
@Composable
private fun FastingMonthCalendarSundayPreview() {
    MaterialTheme {
        Surface {
            FastingMonthCalendar(
                yearMonth = YearMonth.of(2025, 9),
                fastingData = createMockFastingData(YearMonth.of(2025, 9)),
                firstDayOfWeek = DayOfWeek.SUNDAY
            )
        }
    }
}

@Preview(showBackground = true, name = "Monday Start")
@Composable
private fun FastingMonthCalendarMondayPreview() {
    MaterialTheme {
        Surface {
            FastingMonthCalendar(
                yearMonth = YearMonth.of(2025, 9),
                fastingData = createMockFastingData(YearMonth.of(2025, 9)),
                firstDayOfWeek = DayOfWeek.MONDAY
            )
        }
    }
}