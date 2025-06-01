package com.charliesbot.one.today.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.charliesbot.shared.core.components.FastingProgressBar
import com.charliesbot.shared.core.utils.calculateProgressFraction
import com.charliesbot.shared.core.utils.calculateProgressPercentage
import com.charliesbot.shared.core.utils.formatTimestamp
import com.charliesbot.one.R
import com.charliesbot.one.ui.theme.OneTheme
import com.charliesbot.shared.core.constants.PredefinedFastingGoals

@Composable
fun FastingStatusIndicator(
    isFasting: Boolean,
    elapsedTime: Long,
    fastingGoalId: String,
) {
    val headerLabel = if (isFasting) {
        val progress = calculateProgressPercentage(elapsedTime)
        stringResource(R.string.elapsed_percentage, progress)
    } else {
        stringResource(R.string.upcoming_fast).uppercase()
    }
    val timeLabel = if (isFasting) {
        formatTimestamp(elapsedTime)
    } else {
        stringResource(
            R.string.fasting_duration_hours,
            PredefinedFastingGoals.getGoalById(fastingGoalId).durationDisplay
        )
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = headerLabel, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
        Text(
            text = timeLabel,
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 38.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun CurrentFastingProgress(
    elapsedTime: Long,
    fastingGoalId: String,
    isFasting: Boolean = false
) {
    val progress = calculateProgressFraction(elapsedTime)
    FastingProgressBar(
        progress = progress,
        strokeWidth = 35.dp,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 400.dp),
        innerContent = {
            FastingStatusIndicator(isFasting, elapsedTime, fastingGoalId)
        }
    )
}

@Preview(showBackground = true)
@Composable
fun CurrentFastingProgressPreview() {
    OneTheme {
        Column(verticalArrangement = Arrangement.spacedBy(50.dp)) {
            CurrentFastingProgress(isFasting = false, elapsedTime = 0L, fastingGoalId = "circadian")
            CurrentFastingProgress(
                isFasting = true,
                elapsedTime = 7 * 1000 * 60 * 60,
                fastingGoalId = "circadian"
            )
        }
    }
}
