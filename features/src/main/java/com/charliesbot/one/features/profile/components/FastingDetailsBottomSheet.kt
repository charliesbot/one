package com.charliesbot.one.features.profile.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
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
    modifier: Modifier = Modifier,
    sheetState: SheetState = rememberModalBottomSheetState()
) {
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
            // Date Header
            Text(
                text = formatDate(fastingData.date),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Duration
            Text(
                text = formatDuration(
                    fastingData.startTimeEpochMillis ?: 0L,
                    fastingData.endTimeEpochMillis ?: 0L
                ),
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
                label = "Started",
                time = formatTime(fastingData.startTimeEpochMillis ?: 0L)
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
                label = "Ended",
                time = formatTime(fastingData.endTimeEpochMillis ?: 0L)
            )
        }
    }
}

@Composable
private fun TimeRow(
    icon: @Composable () -> Unit,
    label: String,
    time: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
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
    }
}

// Formatting Functions

private fun formatDate(date: LocalDate): String {
    val formatter = DateTimeFormatter.ofPattern("MMMM d, yyyy")
    return date.format(formatter)
}

private fun formatTime(epochMillis: Long): String {
    val instant = Instant.ofEpochMilli(epochMillis)
    val zonedDateTime = instant.atZone(ZoneId.systemDefault())
    val formatter = DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT)
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
            onDismiss = {}
        )
    }
}

