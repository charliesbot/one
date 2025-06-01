package com.charliesbot.one.today

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ButtonGroup
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.charliesbot.one.BuildConfig
import com.charliesbot.one.R
import com.charliesbot.one.core.components.GoalBottomSheet
import com.charliesbot.one.core.components.TimeDisplay
import com.charliesbot.one.core.components.TimePickerDialog
import com.charliesbot.one.core.components.WeeklyProgress
import com.charliesbot.one.today.components.CurrentFastingProgress
import com.charliesbot.one.ui.theme.OneTheme
import com.charliesbot.shared.core.constants.PredefinedFastingGoals
import com.charliesbot.shared.core.utils.convertMillisToLocalDateTime
import kotlinx.coroutines.delay
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun TodayScreen(viewModel: TodayViewModel = koinViewModel()) {
    val screenPadding = 32.dp
    val isTimePickerDialogOpen by viewModel.isTimePickerDialogOpen.collectAsStateWithLifecycle()
    val isGoalBottomSheetOpen by viewModel.isGoalBottomSheetOpen.collectAsStateWithLifecycle()
    val isFasting by viewModel.isFasting.collectAsStateWithLifecycle()
    val starTimeInMillis by viewModel.startTimeInMillis.collectAsStateWithLifecycle()
    val fastingGoalId by viewModel.fastingGoalId.collectAsStateWithLifecycle()
    val startTimeInLocalDateTime =
        convertMillisToLocalDateTime(starTimeInMillis)
    var elapsedTime by remember { mutableLongStateOf(0L) }
    val fastButtonLabel =
        stringResource(if (isFasting) R.string.end_fast else R.string.start_fasting)
    val scrollState = rememberScrollState()

    LaunchedEffect(isFasting) {
        if (isFasting) {
            while (true) {
                elapsedTime = System.currentTimeMillis() - starTimeInMillis
                delay(1000L) // refresh timer every second
            }
        } else {
            elapsedTime = 0L
        }
    }

    Scaffold { innerPadding ->
        if (isTimePickerDialogOpen) {
            TimePickerDialog(
                starTimeInMillis,
                onConfirm = { updatedStartTime ->
                    viewModel.updateStartTime(updatedStartTime)
                    viewModel.closeTimePickerDialog()
                },
                onDismiss = {
                    viewModel.closeTimePickerDialog()
                },
            )
        }
        if (isGoalBottomSheetOpen) {
            GoalBottomSheet(
                onDismiss = viewModel::closeGoalBottomSheet,
                onSave = { id ->
                    viewModel.updateFastingGoal(id)
                    viewModel.closeGoalBottomSheet()
                },
                initialSelectedGoalId = fastingGoalId
            )
        }
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .verticalScroll(scrollState),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (BuildConfig.DEBUG) {
                WeeklyProgress(
                    modifier = Modifier
                        .widthIn(max = 500.dp)
                        .fillMaxWidth()
                        .padding(horizontal = screenPadding + 24.dp)
                )
            } else {
                Spacer(modifier = Modifier.height(40.dp))
            }
            ElevatedCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(600.dp)
                    .padding(screenPadding),
                elevation = CardDefaults.cardElevation(
                    defaultElevation = 4.dp,
                )
            ) {
                Column(
                    modifier = Modifier
                        .padding(all = 32.dp)
                        .fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    CurrentFastingProgress(
                        isFasting = isFasting,
                        elapsedTime = elapsedTime,
                        fastingGoalId = fastingGoalId
                    )
                    Spacer(modifier = Modifier.height(40.dp))
                    AnimatedVisibility(
                        visible = isFasting,
                        enter = fadeIn(animationSpec = tween(durationMillis = 600)) +
                                expandVertically(animationSpec = tween(durationMillis = 350)),
                        exit =
                            fadeOut(animationSpec = tween(durationMillis = 150)) +
                                    shrinkVertically(animationSpec = tween(durationMillis = 350))
                    ) {
                        ButtonGroup(
                            overflowIndicator = {},
                            expandedRatio = 0f,
                            modifier = Modifier
                                .fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween),
                        ) {
                            customItem(
                                buttonGroupContent = {
                                    TimeDisplay(
                                        title = stringResource(R.string.started),
                                        date = startTimeInLocalDateTime,
                                        shapes = ButtonGroupDefaults.connectedLeadingButtonShapes(),
                                        onClick = {
                                            viewModel.openTimePickerDialog()
                                        },
                                        modifier = Modifier.weight(1f)
                                    )
                                },
                                menuContent = {}
                            )
                            customItem(
                                buttonGroupContent = {
                                    TimeDisplay(
                                        title = stringResource(
                                            R.string.goal_with_duration,
                                            "${PredefinedFastingGoals.getGoalById(fastingGoalId).durationDisplay}H"
                                        ),
                                        date = startTimeInLocalDateTime.plusHours(16),
                                        shapes = ButtonGroupDefaults.connectedTrailingButtonShapes(),
                                        onClick = {
                                            viewModel.openGoalBottomSheet()
                                        },
                                        modifier = Modifier.weight(1f)
                                    )
                                },
                                menuContent = {}
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    FilledTonalButton(
                        onClick = if (isFasting) viewModel::onStopFasting else viewModel::onStartFasting,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                    ) {
                        Text(text = fastButtonLabel)
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewTodayScreen() {
    OneTheme {
        TodayScreen()
    }
}
