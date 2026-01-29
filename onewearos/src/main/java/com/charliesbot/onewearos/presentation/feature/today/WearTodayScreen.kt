package com.charliesbot.onewearos.presentation.feature.today

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material3.Card
import androidx.wear.compose.material3.CardDefaults
import androidx.wear.compose.material3.CircularProgressIndicator
import androidx.wear.compose.material3.ProgressIndicatorDefaults
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.IconToggleButton
import androidx.wear.compose.material3.IconToggleButtonDefaults
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.Text
import androidx.wear.tooling.preview.devices.WearDevices
import com.charliesbot.shared.R
import com.charliesbot.shared.core.constants.FastGoal
import com.charliesbot.shared.core.constants.PredefinedFastingGoals
import com.charliesbot.shared.core.utils.TimeFormat
import com.charliesbot.shared.core.utils.convertMillisToLocalDateTime
import com.charliesbot.shared.core.utils.formatDate
import com.charliesbot.shared.core.utils.formatTimestamp
import com.charliesbot.shared.core.utils.getHours
import com.google.android.horologist.compose.layout.ColumnItemType
import com.google.android.horologist.compose.layout.rememberResponsiveColumnPadding
import kotlinx.coroutines.delay
import org.koin.androidx.compose.koinViewModel
import java.time.LocalDateTime
import com.charliesbot.onewearos.R as WearR

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
        initializeTemporalTime = viewModel::initializeTemporalTime,
        onStartFasting = viewModel::onStartFasting,
        onStopFasting = viewModel::onStopFasting,
        onNavigateToGoalSelection = onNavigateToGoalSelection,
        onNavigateToStartDateSelection = onNavigateToStartDateSelection
    )
}

@Composable
private fun StatusIndicator(
    label: String,
    isActive: Boolean
) {
    Box(
        modifier = Modifier
            .background(
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                shape = MaterialTheme.shapes.extraLarge
            )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(
                        color = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                        shape = CircleShape
                    )
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodyExtraSmall
            )
        }
    }
}


@Composable
private fun LabeledTimeInfo(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
//        horizontalAlignment = Alignment.CenterHorizontally,
//        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyExtraSmall,
            fontSize = 7.sp,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.width(2.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.bodyExtraSmall,
            fontSize = 7.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun FastingCard(
    onNavigateToGoalSelection: () -> Unit,
    content: @Composable () -> Unit
) {
    Card(
        onClick = { onNavigateToGoalSelection() },
        modifier = Modifier,
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        )
    ) {
        content()
    }
}

@Composable
private fun NotFastingCardContent(
    timeLabel: String,
    isLargeScreen: Boolean
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp)
    ) {
        Text(
            text = stringResource(R.string.fasting_goal),
            style = MaterialTheme.typography.labelSmall
        )
        Text(
            text = timeLabel,
            style = if (isLargeScreen) MaterialTheme.typography.titleLarge else MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun ActiveFastingCardContent(
    timeLabel: String,
    progress: Float,
    goal: FastGoal?,
    startTime: LocalDateTime,
    isLargeScreen: Boolean
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = timeLabel,
                style = if (isLargeScreen) MaterialTheme.typography.titleLarge else MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            LabeledTimeInfo(
                label = stringResource(WearR.string.label_started),
                value = formatDate(startTime, TimeFormat.TIME),
            )
            LabeledTimeInfo(
                label = stringResource(WearR.string.label_goal),
                value = formatDate(
                    startTime.plusHours(getHours(goal?.durationMillis)),
                    TimeFormat.TIME
                ),
            )
        }
        Column(
            verticalArrangement = Arrangement.Center,
        ) {
            CircularProgressIndicator(
                progress = { progress },
                modifier = Modifier.size(40.dp),
                startAngle = 120f,
                endAngle = 60f,
                strokeWidth = 6.dp,
                colors = ProgressIndicatorDefaults.colors(
                    indicatorColor = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceContainerLow
                )
            )
//            Icon(
//                painter = painterResource(R.drawable.today_24px),
//                contentDescription = null,
//                modifier = Modifier.size(8.dp),
//                tint = MaterialTheme.colorScheme.primary
//            )
        }
    }
//    Spacer(modifier = Modifier.height(4.dp))
//    Row(
//        modifier = Modifier.fillMaxWidth(),
//    ) {
//        LabeledTimeInfo(
//            label = stringResource(WearR.string.label_started),
//            value = formatDate(startTime, TimeFormat.TIME),
//            modifier = Modifier.weight(1f)
//        )
//        LabeledTimeInfo(
//            label = stringResource(WearR.string.label_goal),
//            value = formatDate(endTime, TimeFormat.TIME),
//            modifier = Modifier.weight(1f)
//        )
//    }
}

@Composable
fun WearTodayContent(
    startTimeInMillis: Long,
    isFasting: Boolean,
    fastingGoalId: String,
    initializeTemporalTime: () -> Unit,
    onStartFasting: () -> Unit,
    onStopFasting: () -> Unit,
    onNavigateToStartDateSelection: () -> Unit,
    onNavigateToGoalSelection: () -> Unit
) {
    var elapsedTime by remember { mutableLongStateOf(0L) }
    val isLargeScreen = rememberIsLargeScreen()
    val listState = rememberScalingLazyListState()
    val contentPadding = rememberResponsiveColumnPadding(
        first = ColumnItemType.Button,
        last = ColumnItemType.Button,
    )
    val startTimeInLocalDateTime = convertMillisToLocalDateTime(startTimeInMillis)
    val fastButtonLabel =
        if (isFasting) stringResource(R.string.end_fast) else stringResource(R.string.start_fasting)
    val currentGoal = PredefinedFastingGoals.goalsById[fastingGoalId]

    val timeLabel = if (isFasting) {
        formatTimestamp(elapsedTime)
    } else {
        stringResource(
            com.charliesbot.onewearos.R.string.target_duration_hours,
            currentGoal?.durationDisplay.toString()
        )
    }

    val progress = if (isFasting && currentGoal != null) {
        (elapsedTime.toFloat() / currentGoal.durationMillis).coerceIn(0f, 1f)
    } else {
        0f
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
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(horizontal = 20.dp)
            ) {
                StatusIndicator(
                    label = stringResource(WearR.string.status_ready),
                    isActive = !isFasting
                )
                FastingCard(onNavigateToGoalSelection = onNavigateToGoalSelection) {
                    if (isFasting) {
                        ActiveFastingCardContent(
                            goal = currentGoal,
                            startTime = startTimeInLocalDateTime,
                            timeLabel = timeLabel,
                            progress = progress,
                            isLargeScreen = isLargeScreen
                        )
                    } else {
                        NotFastingCardContent(
                            timeLabel = timeLabel,
                            isLargeScreen = isLargeScreen
                        )
                    }
                }
                IconToggleButton(
                    shapes = IconToggleButtonDefaults.animatedShapes(),
                    checked = isFasting,
                    onCheckedChange = {
                        if (isFasting) onStopFasting() else onStartFasting()
                    }
                ) {
                    Icon(
                        painter = painterResource(
                            if (isFasting) R.drawable.stop_24px else R.drawable.play_arrow_24px
                        ),
                        contentDescription = fastButtonLabel
                    )
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
        isFasting = false,
        fastingGoalId = "16:8", // 16:8 fasting goal
        initializeTemporalTime = {},
        onStartFasting = { },
        onStopFasting = { },
        onNavigateToGoalSelection = { },
        onNavigateToStartDateSelection = { }
    )
}
