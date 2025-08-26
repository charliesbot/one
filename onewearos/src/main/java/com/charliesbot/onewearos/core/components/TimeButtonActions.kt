package com.charliesbot.onewearos.core.components

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material.Text
import androidx.wear.compose.material3.ButtonGroup
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.TextButton
import com.charliesbot.onewearos.R
import com.charliesbot.shared.core.constants.FastGoal
import com.charliesbot.shared.core.constants.PredefinedFastingGoals
import com.charliesbot.shared.core.utils.TimeFormat
import com.charliesbot.shared.core.utils.formatDate
import com.charliesbot.shared.core.utils.getHours
import java.time.LocalDateTime

@Composable
private fun TimeInfoDisplay(
    title: String, date: LocalDateTime, onClick: (() -> Unit)
) {
    val interactionSource = remember { MutableInteractionSource() }
    val verticalSpace = 2.dp
    val textColor =
        MaterialTheme.colorScheme.onSurface
    val dateFormat = TimeFormat.TIME

    TextButton(
        onClick = onClick,
        interactionSource = interactionSource,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(verticalSpace)
        ) {
            Text(
                text = title.uppercase(),
                fontSize = 8.sp,
                fontWeight = FontWeight.W500,
                color = textColor
            )
            Text(
                text = formatDate(date, dateFormat),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = textColor
            )
        }
    }
}

@Composable
fun TimeButtonActions(
    startTime: LocalDateTime,
    goal: FastGoal?,
    onStartTimeClick: () -> Unit,
    onGoalTimeClick: () -> Unit
) {
    ButtonGroup(modifier = Modifier.fillMaxWidth()) {
        TimeInfoDisplay(
            title = stringResource(R.string.label_started),
            date = startTime,
            onClick = {
                onStartTimeClick()
            },
        )
        TimeInfoDisplay(
            title = stringResource(R.string.label_goal),
            date = startTime.plusHours(getHours(goal?.durationMillis)),
            onClick = {
                onGoalTimeClick()
            },
        )
    }
}

@Preview(showBackground = true, device = Devices.PIXEL)
@Composable
private fun TimeButtonActionsPreview() {
    TimeButtonActions(
        startTime = LocalDateTime.now(),
        goal = PredefinedFastingGoals.goalsById["16:8"],
        onStartTimeClick = {},
        onGoalTimeClick = {}
    )
}