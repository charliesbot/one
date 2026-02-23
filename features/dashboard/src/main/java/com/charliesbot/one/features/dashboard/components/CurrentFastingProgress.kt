package com.charliesbot.one.features.dashboard.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.charliesbot.shared.R
import com.charliesbot.shared.core.constants.PredefinedFastingGoals

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun FastingStatusIndicator(
    isFasting: Boolean,
    elapsedTime: Long,
    fastingGoalId: String,
    onClick: () -> Unit
) {
    val fastingGoal = PredefinedFastingGoals.getGoalById(fastingGoalId)
    val headerLabel = if (isFasting) {
        val progress = calculateProgressPercentage(elapsedTime, fastingGoal.durationMillis)
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
        TextButton(
            onClick = onClick
        ) {
            Text(
                text = timeLabel,
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 38.sp,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
fun CurrentFastingProgress(
    elapsedTime: Long,
    fastingGoalId: String,
    onFastingStatusClick: () -> Unit,
    isFasting: Boolean = false,
) {
    val fastingGoal = PredefinedFastingGoals.getGoalById(fastingGoalId)
    val progress = calculateProgressFraction(elapsedTime, fastingGoal.durationMillis)
    Box(contentAlignment = Alignment.Center) {
        FastingProgressBar(
            progress = progress,
            strokeWidth = 35.dp,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f, matchHeightConstraintsFirst = true)
                .heightIn(max = 300.dp)
        )
        FastingStatusIndicator(isFasting, elapsedTime, fastingGoalId, onFastingStatusClick)
    }
}

@Preview(showBackground = true)
@Composable
fun CurrentFastingProgressPreview() {
    Column(verticalArrangement = Arrangement.spacedBy(50.dp)) {
        CurrentFastingProgress(
            isFasting = false,
            elapsedTime = 0L,
            fastingGoalId = "circadian",
            onFastingStatusClick = {})
        CurrentFastingProgress(
            isFasting = true,
            elapsedTime = 7 * 1000 * 60 * 60,
            fastingGoalId = "circadian",
            onFastingStatusClick = {},
        )
    }
}