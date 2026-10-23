package com.charliesbot.one.features.dashboard

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.charliesbot.one.features.dashboard.components.ActiveHero
import com.charliesbot.one.features.dashboard.components.FastDetails
import com.charliesbot.one.features.dashboard.components.FastDetailsState
import com.charliesbot.one.features.dashboard.components.FastWindowCard
import com.charliesbot.one.features.dashboard.components.FastWindowCardState
import com.charliesbot.one.features.dashboard.components.GoalBottomSheet
import com.charliesbot.one.features.dashboard.components.GoalOption
import com.charliesbot.one.features.dashboard.components.GoalPickerCard
import com.charliesbot.one.features.dashboard.components.TodayHeroCard
import com.charliesbot.one.features.dashboard.components.TodayTextStyles
import com.charliesbot.one.features.dashboard.components.WeekCard
import com.charliesbot.one.features.dashboard.components.WeekStrip
import com.charliesbot.one.features.dashboard.model.FastWindow
import com.charliesbot.one.features.dashboard.model.WeekDay
import com.charliesbot.one.features.dashboard.model.buildWeekDays
import com.charliesbot.shared.core.constants.FastGoal
import com.charliesbot.shared.core.constants.PredefinedFastingGoals
import com.charliesbot.shared.core.designsystem.app.components.TimePickerDialog
import com.charliesbot.shared.core.models.SuggestedFastingTime
import com.charliesbot.shared.core.models.TimePeriodProgress
import com.charliesbot.shared.core.strings.R
import com.charliesbot.shared.core.utils.isWidthAtLeastExpanded
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel

@Composable
fun TodayScreen(viewModel: TodayViewModel = koinViewModel()) {
  val isTimePickerDialogOpen by viewModel.isTimePickerDialogOpen.collectAsStateWithLifecycle()
  val isGoalBottomSheetOpen by viewModel.isGoalBottomSheetOpen.collectAsStateWithLifecycle()
  val isFasting by viewModel.isFasting.collectAsStateWithLifecycle()
  val startTimeInMillis by viewModel.startTimeInMillis.collectAsStateWithLifecycle()
  val fastingGoalId by viewModel.fastingGoalId.collectAsStateWithLifecycle()
  val weeklyProgress by viewModel.weeklyProgress.collectAsStateWithLifecycle()
  val smartRemindersEnabled by viewModel.smartRemindersEnabled.collectAsStateWithLifecycle()
  val suggestedFastingTime by viewModel.suggestedFastingTime.collectAsStateWithLifecycle()
  val bedtimeMinutes by viewModel.bedtimeMinutes.collectAsStateWithLifecycle()
  val allGoals by viewModel.allGoals.collectAsStateWithLifecycle()

  // Seconds matter while fasting (the timer); otherwise only the idle countdown moves.
  var nowMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }
  LaunchedEffect(isFasting) {
    while (true) {
      nowMillis = System.currentTimeMillis()
      delay(if (isFasting == true) 1_000L else 30_000L)
    }
  }

  TodayScreenContent(
    isTimePickerDialogOpen = isTimePickerDialogOpen,
    isGoalBottomSheetOpen = isGoalBottomSheetOpen,
    isFasting = isFasting,
    startTimeInMillis = startTimeInMillis,
    fastingGoalId = fastingGoalId,
    weeklyProgress = weeklyProgress,
    suggestion = suggestedFastingTime.takeIf { smartRemindersEnabled },
    bedtimeMinutes = bedtimeMinutes,
    allGoals = allGoals,
    nowMillis = nowMillis,
    isWide = isWidthAtLeastExpanded(),
    onStartFasting = viewModel::onStartFasting,
    onStopFasting = viewModel::onStopFasting,
    onOpenTimePickerDialog = viewModel::openTimePickerDialog,
    onCloseTimePickerDialog = viewModel::closeTimePickerDialog,
    onUpdateStartTime = viewModel::updateStartTime,
    onOpenGoalBottomSheet = viewModel::openGoalBottomSheet,
    onCloseGoalBottomSheet = viewModel::closeGoalBottomSheet,
    onUpdateFastingGoal = viewModel::updateFastingGoal,
    onSaveCustomGoal = viewModel::saveCustomGoal,
    onDeleteCustomGoal = viewModel::deleteCustomGoal,
  )
}

/**
 * The Today screen for one moment in time. Phones stack the week strip, the hero card and, while
 * fasting, the Started/Goal tiles. Expanded windows turn the hero sideways and put the week and a
 * second card (goal picker when idle, fast window when fasting) side by side below it. Cards size
 * to their content; the page scrolls only when the content outgrows the window.
 *
 * [isFasting] is null until the stored state loads; until then only the header (and the week strip
 * on phones) shows, so opening the app mid-fast doesn't flash the idle card and animate into the
 * active one.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun TodayScreenContent(
  isTimePickerDialogOpen: Boolean,
  isGoalBottomSheetOpen: Boolean,
  isFasting: Boolean?,
  startTimeInMillis: Long,
  fastingGoalId: String,
  weeklyProgress: List<TimePeriodProgress>,
  suggestion: SuggestedFastingTime?,
  bedtimeMinutes: Int?,
  allGoals: List<FastGoal>,
  nowMillis: Long,
  isWide: Boolean,
  onStartFasting: () -> Unit,
  onStopFasting: () -> Unit,
  onOpenTimePickerDialog: () -> Unit,
  onCloseTimePickerDialog: () -> Unit,
  onUpdateStartTime: (Long) -> Unit,
  onOpenGoalBottomSheet: () -> Unit,
  onCloseGoalBottomSheet: () -> Unit,
  onUpdateFastingGoal: (String) -> Unit,
  onSaveCustomGoal: (FastGoal) -> Unit,
  onDeleteCustomGoal: (String) -> Unit,
) {
  val goal = PredefinedFastingGoals.getGoalById(fastingGoalId)
  val format = currentTimeFormatting()
  val activeWindow =
    FastWindow.create(startTimeInMillis, goal.durationMillis, bedtimeMinutes, format.zone)
  val weekDays =
    buildWeekDays(
      weekProgress = weeklyProgress,
      today = format.dateTime(nowMillis).dayOfWeek,
      activeFastProgress = if (isFasting == true) activeWindow.progressAt(nowMillis) else null,
    )

  val snackbarHostState = remember { SnackbarHostState() }
  TodayDialogs(
    isTimePickerDialogOpen = isTimePickerDialogOpen,
    isGoalBottomSheetOpen = isGoalBottomSheetOpen,
    startTimeInMillis = startTimeInMillis,
    fastingGoalId = fastingGoalId,
    allGoals = allGoals,
    snackbarHostState = snackbarHostState,
    onCloseTimePickerDialog = onCloseTimePickerDialog,
    onUpdateStartTime = onUpdateStartTime,
    onCloseGoalBottomSheet = onCloseGoalBottomSheet,
    onUpdateFastingGoal = onUpdateFastingGoal,
    onSaveCustomGoal = onSaveCustomGoal,
    onDeleteCustomGoal = onDeleteCustomGoal,
  )

  // The app shell's scaffold already applies the system bar insets.
  Scaffold(
    contentWindowInsets = WindowInsets(0),
    snackbarHost = { SnackbarHost(snackbarHostState) },
  ) { innerPadding ->
    BoxWithConstraints(
      modifier = Modifier.fillMaxSize().padding(innerPadding),
      contentAlignment = Alignment.TopCenter,
    ) {
      val dialSize: Dp = if (isWide) min(maxHeight * 0.38f, 400.dp) else 256.dp
      Column(
        modifier =
          // Full width from the first frame, so the header doesn't shift when content loads.
          Modifier.widthIn(max = if (isWide) 1040.dp else 560.dp)
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = if (maxWidth >= 600.dp) 24.dp else 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
      ) {
        TodayHeader(date = headerDate(nowMillis, format), inline = isWide)
        if (!isWide) WeekStrip(weekDays, showHours = false)

        // Everything that depends on the fasting state waits for it to load, so the first frame
        // after loading composes as the initial state instead of animating into it.
        if (isFasting != null) {
          val active =
            activeWindow
              .takeIf { isFasting }
              ?.let {
                ActiveHero(
                  activeHeroState(it, nowMillis),
                  fastDetailsState(it, goal, nowMillis, format),
                )
              }
          Column {
            TodayHeroCard(
              idle = idleHeroState(goal, suggestion, bedtimeMinutes, nowMillis, format),
              active = active,
              wide = isWide,
              dialSize = dialSize,
              onStartFasting = onStartFasting,
              onEndFast = onStopFasting,
              onChangeGoal = onOpenGoalBottomSheet,
              onEditStart = onOpenTimePickerDialog,
            )
            if (!isWide) {
              PhoneFastDetails(
                details = active?.details,
                onEditStart = onOpenTimePickerDialog,
                onEditGoal = onOpenGoalBottomSheet,
              )
            }
          }
          if (isWide) {
            WideAside(
              weekDays = weekDays,
              fastWindow =
                activeWindow
                  .takeIf { isFasting }
                  ?.let { fastWindowCardState(it, nowMillis, format) },
              goals = goalOptions(allGoals),
              selectedGoalId = fastingGoalId,
              onSelectGoal = onUpdateFastingGoal,
              onNewGoal = onOpenGoalBottomSheet,
            )
          }
        }
      }
    }
  }
}

/** The start-time picker and the goal sheet, with a snackbar after saving or deleting a goal. */
@Composable
private fun TodayDialogs(
  isTimePickerDialogOpen: Boolean,
  isGoalBottomSheetOpen: Boolean,
  startTimeInMillis: Long,
  fastingGoalId: String,
  allGoals: List<FastGoal>,
  snackbarHostState: SnackbarHostState,
  onCloseTimePickerDialog: () -> Unit,
  onUpdateStartTime: (Long) -> Unit,
  onCloseGoalBottomSheet: () -> Unit,
  onUpdateFastingGoal: (String) -> Unit,
  onSaveCustomGoal: (FastGoal) -> Unit,
  onDeleteCustomGoal: (String) -> Unit,
) {
  val scope = rememberCoroutineScope()
  val customGoalSavedMessage = stringResource(R.string.custom_goal_saved)
  val customGoalUpdatedMessage = stringResource(R.string.custom_goal_updated)
  val customGoalDeletedMessage = stringResource(R.string.custom_goal_deleted)

  if (isTimePickerDialogOpen) {
    TimePickerDialog(
      startTimeInMillis,
      onConfirm = { updatedStartTime ->
        onUpdateStartTime(updatedStartTime)
        onCloseTimePickerDialog()
      },
      onDismiss = onCloseTimePickerDialog,
    )
  }
  if (isGoalBottomSheetOpen) {
    GoalBottomSheet(
      onDismiss = onCloseGoalBottomSheet,
      onSave = { id ->
        onUpdateFastingGoal(id)
        onCloseGoalBottomSheet()
      },
      onSaveCustomGoal = { customGoal ->
        val isUpdate = allGoals.any { it.id == customGoal.id }
        onSaveCustomGoal(customGoal)
        onCloseGoalBottomSheet()
        scope.launch {
          snackbarHostState.showSnackbar(
            if (isUpdate) customGoalUpdatedMessage else customGoalSavedMessage
          )
        }
      },
      onDeleteCustomGoal = { goalId ->
        onDeleteCustomGoal(goalId)
        onCloseGoalBottomSheet()
        scope.launch { snackbarHostState.showSnackbar(customGoalDeletedMessage) }
      },
      initialSelectedGoalId = fastingGoalId,
      allGoals = allGoals,
    )
  }
}

/** Phones: Started and Goal slide down from under the hero card while a fast runs. */
@Composable
private fun PhoneFastDetails(
  details: FastDetailsState?,
  onEditStart: () -> Unit,
  onEditGoal: () -> Unit,
) {
  val motion = MaterialTheme.motionScheme
  AnimatedContent(
    targetState = details,
    contentKey = { it != null },
    transitionSpec = {
      (fadeIn(motion.defaultEffectsSpec()) togetherWith fadeOut(motion.fastEffectsSpec())).using(
        SizeTransform { _, _ -> motion.defaultSpatialSpec() }
      )
    },
    // Full width with the content at the bottom: only the height grows, so the tiles slide down
    // from under the card instead of wiping in from a corner.
    modifier = Modifier.fillMaxWidth(),
    contentAlignment = Alignment.BottomStart,
    label = "today-details",
  ) { shown ->
    if (shown != null) {
      FastDetails(
        state = shown,
        onEditStart = onEditStart,
        onEditGoal = onEditGoal,
        stacked = false,
        modifier = Modifier.padding(top = 12.dp),
      )
    }
  }
}

/** Large screens: the week beside the goal picker when idle, or the fast window while fasting. */
@Composable
private fun WideAside(
  weekDays: List<WeekDay>,
  fastWindow: FastWindowCardState?,
  goals: List<GoalOption>,
  selectedGoalId: String,
  onSelectGoal: (String) -> Unit,
  onNewGoal: () -> Unit,
) {
  val motion = MaterialTheme.motionScheme
  Row(
    modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min),
    horizontalArrangement = Arrangement.spacedBy(16.dp),
  ) {
    WeekCard(weekDays, Modifier.weight(1f).fillMaxHeight())
    AnimatedContent(
      targetState = fastWindow,
      contentKey = { it != null },
      transitionSpec = {
        fadeIn(motion.defaultEffectsSpec()) togetherWith fadeOut(motion.fastEffectsSpec())
      },
      modifier = Modifier.weight(1f).fillMaxHeight(),
      label = "today-aside",
    ) { shown ->
      if (shown != null) {
        FastWindowCard(shown, Modifier.fillMaxSize())
      } else {
        GoalPickerCard(
          goals = goals,
          selectedGoalId = selectedGoalId,
          onSelectGoal = onSelectGoal,
          onNewGoal = onNewGoal,
          modifier = Modifier.fillMaxSize(),
        )
      }
    }
  }
}

@Composable
private fun TodayHeader(date: String, inline: Boolean) {
  val title = stringResource(R.string.nav_today)
  val dateStyle = MaterialTheme.typography.titleMedium
  val dateColor = MaterialTheme.colorScheme.onSurfaceVariant
  if (inline) {
    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
      Text(title, style = TodayTextStyles.ScreenTitle, modifier = Modifier.alignByBaseline())
      Text(date, style = dateStyle, color = dateColor, modifier = Modifier.alignByBaseline())
    }
  } else {
    Column(Modifier.padding(horizontal = 8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
      Text(title, style = TodayTextStyles.ScreenTitle)
      Text(date, style = dateStyle, color = dateColor)
    }
  }
}
