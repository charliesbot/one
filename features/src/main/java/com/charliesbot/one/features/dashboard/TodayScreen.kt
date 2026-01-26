package com.charliesbot.one.features.dashboard

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ButtonGroup
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
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
import com.charliesbot.one.features.dashboard.components.CurrentFastingProgress
import com.charliesbot.one.features.dashboard.components.GoalBottomSheet
import com.charliesbot.one.features.dashboard.components.TimeDisplay
import com.charliesbot.one.features.dashboard.components.TimePickerDialog
import com.charliesbot.one.features.dashboard.components.WeeklyProgress
import com.charliesbot.shared.R
import com.charliesbot.shared.core.constants.PredefinedFastingGoals
import com.charliesbot.shared.core.models.TimePeriodProgress
import com.charliesbot.shared.core.testing.MockDataUtils
import com.charliesbot.shared.core.utils.convertMillisToLocalDateTime
import com.charliesbot.shared.core.utils.formatMinutesAsTime
import com.charliesbot.shared.core.utils.getHours
import com.charliesbot.shared.core.utils.isWidthAtLeastMedium
import kotlinx.coroutines.delay
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun TodayScreen(viewModel: TodayViewModel = koinViewModel()) {
    val isTimePickerDialogOpen by viewModel.isTimePickerDialogOpen.collectAsStateWithLifecycle()
    val isGoalBottomSheetOpen by viewModel.isGoalBottomSheetOpen.collectAsStateWithLifecycle()
    val isFasting by viewModel.isFasting.collectAsStateWithLifecycle()
    val starTimeInMillis by viewModel.startTimeInMillis.collectAsStateWithLifecycle()
    val fastingGoalId by viewModel.fastingGoalId.collectAsStateWithLifecycle()
    val weeklyProgress by viewModel.weeklyProgress.collectAsStateWithLifecycle()
    val smartRemindersEnabled by viewModel.smartRemindersEnabled.collectAsStateWithLifecycle()
    val suggestedFastingTime by viewModel.suggestedFastingTime.collectAsStateWithLifecycle()
    val isWidthAtLeastMedium = isWidthAtLeastMedium()

    TodayScreenContent(
        isTimePickerDialogOpen = isTimePickerDialogOpen,
        isGoalBottomSheetOpen = isGoalBottomSheetOpen,
        isFasting = isFasting,
        startTimeInMillis = starTimeInMillis,
        fastingGoalId = fastingGoalId,
        weeklyProgress = weeklyProgress,
        smartRemindersEnabled = smartRemindersEnabled,
        suggestedTimeMinutes = suggestedFastingTime?.suggestedTimeMinutes,
        suggestedTimeReasoning = suggestedFastingTime?.reasoning,
        suggestedTimeSource = suggestedFastingTime?.source,
        isWidthAtLeastMedium = isWidthAtLeastMedium,
        onStartFasting = viewModel::onStartFasting,
        onStopFasting = viewModel::onStopFasting,
        onOpenTimePickerDialog = viewModel::openTimePickerDialog,
        onCloseTimePickerDialog = viewModel::closeTimePickerDialog,
        onUpdateStartTime = viewModel::updateStartTime,
        onOpenGoalBottomSheet = viewModel::openGoalBottomSheet,
        onCloseGoalBottomSheet = viewModel::closeGoalBottomSheet,
        onUpdateFastingGoal = viewModel::updateFastingGoal
    )
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun TodayScreenContent(
    isTimePickerDialogOpen: Boolean,
    isGoalBottomSheetOpen: Boolean,
    isFasting: Boolean,
    startTimeInMillis: Long,
    fastingGoalId: String,
    weeklyProgress: List<TimePeriodProgress>,
    smartRemindersEnabled: Boolean,
    suggestedTimeMinutes: Int?,
    suggestedTimeReasoning: String?,
    suggestedTimeSource: com.charliesbot.shared.core.models.SuggestionSource?,
    isWidthAtLeastMedium: Boolean,
    onStartFasting: () -> Unit,
    onStopFasting: () -> Unit,
    onOpenTimePickerDialog: () -> Unit,
    onCloseTimePickerDialog: () -> Unit,
    onUpdateStartTime: (Long) -> Unit,
    onOpenGoalBottomSheet: () -> Unit,
    onCloseGoalBottomSheet: () -> Unit,
    onUpdateFastingGoal: (String) -> Unit
) {
    val screenPadding = 32.dp
    val startTimeInLocalDateTime =
        convertMillisToLocalDateTime(startTimeInMillis)
    var elapsedTime by remember { mutableLongStateOf(0L) }
    val fastButtonLabel =
        stringResource(if (isFasting) R.string.end_fast else R.string.start_fasting)
    val scrollState = rememberScrollState()
    val currentGoal = PredefinedFastingGoals.goalsById[fastingGoalId]

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

    Scaffold(
        topBar = {
            TopAppBar(
                windowInsets = WindowInsets(top = 0.dp),
                title = { Text(stringResource(R.string.nav_today)) },
            )
        },
    ) { innerPadding ->
        val maxWidth = if (isWidthAtLeastMedium) 800.dp else 600.dp
        if (isTimePickerDialogOpen) {
            TimePickerDialog(
                startTimeInMillis,
                onConfirm = { updatedStartTime ->
                    onUpdateStartTime(updatedStartTime)
                    onCloseTimePickerDialog()
                },
                onDismiss = {
                    onCloseTimePickerDialog()
                },
            )
        }
        if (isGoalBottomSheetOpen) {
            GoalBottomSheet(
                onDismiss = onCloseGoalBottomSheet,
                onSave = { id ->
                    onUpdateFastingGoal(id)
                    onCloseGoalBottomSheet()
                },
                initialSelectedGoalId = fastingGoalId
            )
        }
        Box(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize(),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = maxWidth)
                    .verticalScroll(scrollState),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                WeeklyProgress(
                    weeklyProgress = weeklyProgress,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = screenPadding + 24.dp)
                )

                // Smart Suggestion Card - shown when not fasting and reminders are enabled
                AnimatedVisibility(
                    visible = smartRemindersEnabled && !isFasting && suggestedTimeMinutes != null,
                    enter = fadeIn(animationSpec = tween(durationMillis = 300)) +
                            expandVertically(animationSpec = tween(durationMillis = 300)),
                    exit = fadeOut(animationSpec = tween(durationMillis = 150)) +
                            shrinkVertically(animationSpec = tween(durationMillis = 200))
                ) {
                    suggestedTimeMinutes?.let { minutes ->
                        SmartSuggestionCard(
                            suggestedTimeMinutes = minutes,
                            reasoning = suggestedTimeReasoning ?: "",
                            source = suggestedTimeSource,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = screenPadding, vertical = 8.dp)
                        )
                    }
                }

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(screenPadding),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
                ) {
                    val progressContent: @Composable () -> Unit = {
                        CurrentFastingProgress(
                            isFasting = isFasting,
                            elapsedTime = elapsedTime,
                            fastingGoalId = fastingGoalId,
                            onFastingStatusClick = onOpenGoalBottomSheet,
                        )
                    }

                    val buttonsContent: @Composable () -> Unit = {
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
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween),
                            ) {
                                customItem(
                                    buttonGroupContent = {
                                        TimeDisplay(
                                            title = stringResource(R.string.started),
                                            date = startTimeInLocalDateTime,
                                            shapes = ButtonGroupDefaults.connectedLeadingButtonShapes(
                                                pressedShape = RoundedCornerShape(60.dp),
                                            ),
                                            onClick = { onOpenTimePickerDialog() },
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
                                                "${currentGoal?.durationDisplay}H"
                                            ),
                                            date = startTimeInLocalDateTime.plusHours(
                                                getHours(currentGoal?.durationMillis)
                                            ),
                                            shapes = ButtonGroupDefaults.connectedTrailingButtonShapes(
                                                pressedShape = RoundedCornerShape(60.dp),
                                            ),
                                            onClick = { onOpenGoalBottomSheet() },
                                            modifier = Modifier.weight(1f)
                                        )
                                    },
                                    menuContent = {}
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        FilledTonalButton(
                            onClick = if (isFasting) onStopFasting else onStartFasting,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                        ) {
                            Text(text = fastButtonLabel)
                        }
                    }

                    if (isWidthAtLeastMedium) {
                        Row(
                            modifier = Modifier
                                .padding(all = screenPadding)
                                .fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(32.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Box(modifier = Modifier.weight(1f)) { progressContent() }
                            Column(
                                modifier = Modifier.weight(1f),
                                horizontalAlignment = Alignment.CenterHorizontally,
                            ) { buttonsContent() }
                        }
                    } else {
                        Column(
                            modifier = Modifier.padding(all = screenPadding),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            progressContent()
                            Spacer(modifier = Modifier.height(40.dp))
                            buttonsContent()
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SmartSuggestionCard(
    suggestedTimeMinutes: Int,
    reasoning: String,
    source: com.charliesbot.shared.core.models.SuggestionSource?,
    modifier: Modifier = Modifier
) {
    val formattedTime = formatMinutesAsTime(suggestedTimeMinutes)
    val sourceText = when (source) {
        com.charliesbot.shared.core.models.SuggestionSource.MOVING_AVERAGE -> stringResource(R.string.smart_suggestion_based_on_average)
        com.charliesbot.shared.core.models.SuggestionSource.BEDTIME_BASED -> stringResource(R.string.smart_suggestion_based_on_bedtime)
        else -> null
    }

    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = stringResource(R.string.upcoming_fast),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = formattedTime,
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = reasoning,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
            )
            if (sourceText != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = sourceText,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun PreviewTodayScreen() {
        TodayScreenContent(
            isTimePickerDialogOpen = false,
            isGoalBottomSheetOpen = false,
            isFasting = true,
            startTimeInMillis = System.currentTimeMillis() - 3600000, // 1 hour ago
            fastingGoalId = "16:8",
            weeklyProgress = MockDataUtils.createMockWeeklyProgress(),
            smartRemindersEnabled = true,
            suggestedTimeMinutes = 1200, // 8:00 PM
            suggestedTimeReasoning = "Based on your recent 7-day average",
            suggestedTimeSource = com.charliesbot.shared.core.models.SuggestionSource.MOVING_AVERAGE,
            isWidthAtLeastMedium = false,
            onStartFasting = {},
            onStopFasting = {},
            onOpenTimePickerDialog = {},
            onCloseTimePickerDialog = {},
            onUpdateStartTime = {},
            onOpenGoalBottomSheet = {},
            onCloseGoalBottomSheet = {},
            onUpdateFastingGoal = {},
        )
}

@Preview(heightDp = 360, widthDp = 800)
@Composable
private fun PreviewTodayScreenLandscape() {
        TodayScreenContent(
            isTimePickerDialogOpen = false,
            isGoalBottomSheetOpen = false,
            isFasting = true,
            startTimeInMillis = System.currentTimeMillis() - 3600000, // 1 hour ago
            fastingGoalId = "16:8",
            weeklyProgress = MockDataUtils.createMockWeeklyProgress(),
            smartRemindersEnabled = true,
            suggestedTimeMinutes = 1200,
            suggestedTimeReasoning = "Based on your recent 7-day average",
            suggestedTimeSource = com.charliesbot.shared.core.models.SuggestionSource.MOVING_AVERAGE,
            isWidthAtLeastMedium = true,
            onStartFasting = {},
            onStopFasting = {},
            onOpenTimePickerDialog = {},
            onCloseTimePickerDialog = {},
            onUpdateStartTime = {},
            onOpenGoalBottomSheet = {},
            onCloseGoalBottomSheet = {},
            onUpdateFastingGoal = {},
        )
}

