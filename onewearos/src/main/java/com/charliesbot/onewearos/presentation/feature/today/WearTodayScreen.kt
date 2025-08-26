package com.charliesbot.onewearos.presentation.feature.today

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.TextButton
import androidx.wear.compose.material3.TextToggleButton
import androidx.wear.tooling.preview.devices.WearDevices
import com.charliesbot.onewearos.R
import com.charliesbot.onewearos.core.components.TimeButtonActions
import com.charliesbot.shared.core.components.FastingProgressBar
import com.charliesbot.shared.core.constants.PredefinedFastingGoals
import com.charliesbot.shared.core.utils.calculateProgressFraction
import com.charliesbot.shared.core.utils.convertMillisToLocalDateTime
import com.charliesbot.shared.core.utils.formatTimestamp
import kotlinx.coroutines.delay
import org.koin.androidx.compose.koinViewModel

@Composable
fun rememberIsLargeScreen(): Boolean {
    val screenWidthDp = LocalConfiguration.current.screenWidthDp
    return remember(screenWidthDp) {
        screenWidthDp >= 225
    }
}


@Composable
fun WearTodayScreen(
    viewModel: WearTodayViewModel = koinViewModel(),
    onNavigateToStartDateSelection: () -> Unit,
    onNavigateToGoalSelection: () -> Unit
) {
    val startTimeInMillis by viewModel.startTimeInMillis.collectAsStateWithLifecycle()
    val isFasting by viewModel.isFasting.collectAsStateWithLifecycle()
    val fastingGoalId by viewModel.fastingGoalId.collectAsStateWithLifecycle()

    WearTodayContent(
        startTimeInMillis = startTimeInMillis,
        isFasting = isFasting,
        fastingGoalId = fastingGoalId,
        onStartFasting = viewModel::onStartFasting,
        onStopFasting = viewModel::onStopFasting,
        onNavigateToGoalSelection = onNavigateToGoalSelection,
        onNavigateToStartDateSelection = onNavigateToStartDateSelection
    )
}

@Composable
fun WearTodayContent(
    startTimeInMillis: Long,
    isFasting: Boolean,
    fastingGoalId: String,
    onStartFasting: () -> Unit,
    onStopFasting: () -> Unit,
    onNavigateToStartDateSelection: () -> Unit,
    onNavigateToGoalSelection: () -> Unit
) {
    var elapsedTime by remember { mutableLongStateOf(0L) }
    val isLargeScreen = rememberIsLargeScreen()
    val startTimeInLocalDateTime = convertMillisToLocalDateTime(startTimeInMillis)
    val fastButtonLabel =
        if (isFasting) stringResource(R.string.end_fast) else stringResource(R.string.start_fasting)
    val currentGoal = PredefinedFastingGoals.goalsById[fastingGoalId]

    val timeLabel = if (isFasting) {
        formatTimestamp(elapsedTime)
    } else {
        stringResource(R.string.target_duration_hours, currentGoal?.durationDisplay.toString())
    }

    LaunchedEffect(isFasting, startTimeInMillis) {
        if (isFasting) {
            while (true) {
                elapsedTime = System.currentTimeMillis() - startTimeInMillis
                delay(1000L) // refresh timer every second
            }
        } else {
            elapsedTime = 0L
        }
    }
    ScreenScaffold {
        FastingProgressBar(
            progress = calculateProgressFraction(elapsedTime, currentGoal?.durationMillis),
            strokeWidth = 5.dp,
            indicatorColor = MaterialTheme.colorScheme.primaryDim,
            trackColor = MaterialTheme.colorScheme.onBackground,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                TextButton(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 40.dp),
                    onClick = {
                        onNavigateToGoalSelection()
                    }
                ) {
                    Text(
                        text = timeLabel,
                        color = MaterialTheme.colorScheme.onBackground,
                        fontSize = if (isLargeScreen) 30.sp else 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                if (!isFasting) {
                    Spacer(modifier = Modifier.height(8.dp))
                }
                AnimatedVisibility(
                    visible = isFasting
                ) {
                    TimeButtonActions(
                        startTime = startTimeInLocalDateTime,
                        goal = currentGoal,
                        onStartTimeClick = {
                            onNavigateToStartDateSelection()
                        },
                        onGoalTimeClick = {
                            onNavigateToGoalSelection()
                        }
                    )
                }
                TextToggleButton(
                    checked = !isFasting,
                    onCheckedChange = {
                        if (isFasting) onStopFasting() else onStartFasting()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 40.dp),
                ) {
                    Text(fastButtonLabel, fontSize = if (isLargeScreen) 16.sp else 12.sp)
                }
            }
        }
    }
}

@Preview(device = WearDevices.LARGE_ROUND, showSystemUi = true)
@Composable
private fun DefaultPreview() {
    WearTodayContent(
        startTimeInMillis = System.currentTimeMillis() - (2 * 60 * 60 * 1000), // 2 hours ago
        isFasting = true,
        fastingGoalId = "16:8", // 16:8 fasting goal
        onStartFasting = { },
        onStopFasting = { },
        onNavigateToGoalSelection = { },
        onNavigateToStartDateSelection = { }
    )
}
