package com.charliesbot.shared.core.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter

data class FastingDayData(
    val date: LocalDate,
    val durationHours: Int? = null, // null means no fasting that day
    val isGoalMet: Boolean = false
)

@Composable
fun FastingMonthCalendar(
    modifier: Modifier = Modifier,
    yearMonth: YearMonth,
    fastingData: Map<LocalDate, FastingDayData> = emptyMap(),
    firstDayOfWeek: DayOfWeek = DayOfWeek.SUNDAY,
    onDayClick: (LocalDate) -> Unit = {},
) {
    val monthName = yearMonth.format(DateTimeFormatter.ofPattern("MMM ''yy"))
    val monthTotalHours = fastingData.values.sumOf { it.durationHours ?: 0 }

    Column(
        modifier = modifier.padding(16.dp)
    ) {
        // Month Header
        MonthHeader(
            monthName = monthName,
            totalHours = monthTotalHours
        )

        Spacer(modifier = Modifier.height(16.dp))

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

@Composable
private fun MonthHeader(
    monthName: String,
    totalHours: Int
) {
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
        Text(
            text = "${totalHours}h",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Medium
        )
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
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

private fun getWeekDaysLabels(firstDayOfWeek: DayOfWeek): List<String> {
    val allDays = listOf("S", "M", "T", "W", "T", "F", "S")

    return when (firstDayOfWeek) {
        DayOfWeek.SUNDAY -> allDays // Sun, Mon, Tue, Wed, Thu, Fri, Sat
        DayOfWeek.MONDAY -> allDays.drop(1) + allDays.take(1) // Mon, Tue, Wed, Thu, Fri, Sat, Sun
        else -> allDays // Default to Sunday start for other days
    }
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
    // Convert DayOfWeek to 0-based index based on calendar start day
    val monthStartIndex = when (calendarFirstDay) {
        DayOfWeek.SUNDAY -> {
            // Sunday = 0, Monday = 1, ..., Saturday = 6
            when (firstDayOfMonth) {
                DayOfWeek.SUNDAY -> 0
                DayOfWeek.MONDAY -> 1
                DayOfWeek.TUESDAY -> 2
                DayOfWeek.WEDNESDAY -> 3
                DayOfWeek.THURSDAY -> 4
                DayOfWeek.FRIDAY -> 5
                DayOfWeek.SATURDAY -> 6
            }
        }

        DayOfWeek.MONDAY -> {
            // Monday = 0, Tuesday = 1, ..., Sunday = 6
            when (firstDayOfMonth) {
                DayOfWeek.MONDAY -> 0
                DayOfWeek.TUESDAY -> 1
                DayOfWeek.WEDNESDAY -> 2
                DayOfWeek.THURSDAY -> 3
                DayOfWeek.FRIDAY -> 4
                DayOfWeek.SATURDAY -> 5
                DayOfWeek.SUNDAY -> 6
            }
        }

        else -> 0 // Default case
    }

    return monthStartIndex
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
            .clip(MaterialShapes.Cookie12Sided.toShape())
            .background(backgroundColor)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = dayNumber.toString(),
                style = MaterialTheme.typography.bodySmall,
                color = contentColor,
            )

            fastingData?.durationHours?.let { hours ->
                Text(
                    text = "${hours}h",
                    style = MaterialTheme.typography.labelSmall,
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