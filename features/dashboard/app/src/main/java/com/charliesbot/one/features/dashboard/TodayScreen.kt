package com.charliesbot.one.features.dashboard

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.charliesbot.one.features.dashboard.components.FastDetails
import com.charliesbot.one.features.dashboard.components.FastWindowCard
import com.charliesbot.one.features.dashboard.components.GoalBottomSheet
import com.charliesbot.one.features.dashboard.components.GoalPickerCard
import com.charliesbot.one.features.dashboard.components.TodayHeroCard
import com.charliesbot.one.features.dashboard.components.TodayTextStyles
import com.charliesbot.one.features.dashboard.components.WeekCard
import com.charliesbot.one.features.dashboard.components.WeekStrip
import com.charliesbot.one.features.dashboard.model.FastWindow
import com.charliesbot.one.features.dashboard.model.buildWeekDays
import com.charliesbot.shared.core.constants.FastGoal
import com.charliesbot.shared.core.constants.PredefinedFastingGoals
import com.charliesbot.shared.core.designsystem.app.components.TimePickerDialog
import com.charliesbot.shared.core.models.SuggestedFastingTime
import com.charliesbot.shared.core.models.SuggestionSource
import com.charliesbot.shared.core.models.TimePeriodProgress
import com.charliesbot.shared.core.strings.R
import com.charliesbot.shared.core.testing.MockDataUtils
import com.charliesbot.shared.core.utils.isWidthAtLeastExpanded
import java.time.Instant
import java.time.ZoneId
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel

/**
 * [onContentReady] is called once the stored fasting state has loaded and the full screen is
 * composed, so the app can hold its splash screen until then instead of showing a bare header.
 */
@Composable
fun TodayScreen(onContentReady: () -> Unit = {}, viewModel: TodayViewModel = koinViewModel()) {
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
    onContentReady = onContentReady,
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
 * fasting, the Started/Goal rows. Expanded windows turn the hero sideways and put the week and a
 * second card (goal picker when idle, fast window when fasting) side by side below it. Cards size
 * to their content; the page scrolls only when the content outgrows the window.
 *
 * [isFasting] is null until the stored state loads; until then only the header (and the week strip
 * on phones) shows, so opening the app mid-fast doesn't flash the idle card and animate into the
 * active one. [onContentReady] fires once the rest is composed.
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
  onContentReady: () -> Unit,
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

        // The active content outlives the fast by one transition so it can animate out.
        val lastActiveWindow = rememberLatest(activeWindow.takeIf { isFasting == true })
        val activeUi = lastActiveWindow?.let { activeHeroState(it, nowMillis) }
        val detailsUi = lastActiveWindow?.let { fastDetailsState(it, goal, nowMillis, format) }
        val motion = MaterialTheme.motionScheme

        val hero: @Composable (Boolean) -> Unit = { fasting ->
          TodayHeroCard(
            isFasting = fasting,
            idle = idleHeroState(goal, suggestion, bedtimeMinutes, nowMillis, format),
            active = activeUi,
            details = detailsUi,
            wide = isWide,
            dialSize = dialSize,
            onStartFasting = onStartFasting,
            onEndFast = onStopFasting,
            onChangeGoal = onOpenGoalBottomSheet,
            onEditStart = onOpenTimePickerDialog,
          )
        }

        if (!isWide) WeekStrip(weekDays, showHours = false)
        // Everything that depends on the fasting state waits for it to load, so the first frame
        // after loading composes as the initial state instead of animating into it.
        val fasting = isFasting
        if (fasting != null) {
          SideEffect(onContentReady)
          if (isWide) {
            hero(fasting)
            Row(
              modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min),
              horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
              WeekCard(weekDays, Modifier.weight(1f).fillMaxHeight())
              AnimatedContent(
                targetState = fasting && lastActiveWindow != null,
                transitionSpec = {
                  fadeIn(motion.defaultEffectsSpec()) togetherWith fadeOut(motion.fastEffectsSpec())
                },
                modifier = Modifier.weight(1f).fillMaxHeight(),
                label = "today-aside",
              ) { showWindow ->
                val window = lastActiveWindow
                if (showWindow && window != null) {
                  FastWindowCard(
                    progress = window.progressAt(nowMillis),
                    bedtimeFraction = window.bedtimeFraction,
                    start =
                      "${format.shortDay(window.startMillis)}, ${format.compactClock(window.startMillis)}",
                    end =
                      "${format.shortDay(window.endMillis)}, ${format.compactClock(window.endMillis)}",
                    bedtimeNote = bedtimeNote(window, nowMillis, format),
                    description = stringResource(R.string.fast_window),
                    modifier = Modifier.fillMaxSize(),
                  )
                } else {
                  GoalPickerCard(
                    goals = goalOptions(allGoals),
                    selectedGoalId = fastingGoalId,
                    onSelectGoal = onUpdateFastingGoal,
                    onNewGoal = onOpenGoalBottomSheet,
                    modifier = Modifier.fillMaxSize(),
                  )
                }
              }
            }
          } else {
            Column {
              hero(fasting)
              // Started and Goal slide down from under the card when a fast starts.
              AnimatedVisibility(
                visible = fasting && detailsUi != null,
                enter =
                  expandVertically(motion.defaultSpatialSpec()) +
                    fadeIn(motion.defaultEffectsSpec()),
                exit =
                  shrinkVertically(motion.fastSpatialSpec()) + fadeOut(motion.fastEffectsSpec()),
              ) {
                detailsUi?.let { details ->
                  FastDetails(
                    state = details,
                    onEditStart = onOpenTimePickerDialog,
                    onEditGoal = onOpenGoalBottomSheet,
                    stacked = false,
                    modifier = Modifier.padding(top = 12.dp),
                  )
                }
              }
            }
          }
        }
      }
    }
  }
}

/** [value] while it is non-null, otherwise the last non-null value seen. */
@Composable
private fun <T : Any> rememberLatest(value: T?): T? {
  val latest = remember { LatestHolder<T>() }
  if (value != null) latest.value = value
  return latest.value
}

private class LatestHolder<T : Any> {
  var value: T? = null
}

@Composable
private fun TodayHeader(date: String, inline: Boolean) {
  val title: @Composable () -> Unit = {
    Text(stringResource(R.string.nav_today), style = TodayTextStyles.ScreenTitle)
  }
  val subtitle: @Composable (Modifier) -> Unit = { modifier ->
    Text(
      date,
      style = MaterialTheme.typography.titleMedium,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
      modifier = modifier,
    )
  }
  if (inline) {
    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
      Box(Modifier.alignByBaseline()) { title() }
      subtitle(Modifier.alignByBaseline())
    }
  } else {
    Column(Modifier.padding(horizontal = 8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
      title()
      subtitle(Modifier)
    }
  }
}

@Composable
private fun bedtimeNote(window: FastWindow, nowMillis: Long, format: TimeFormatting): String? {
  val bedtime = window.bedtimeMillis ?: return null
  val time = format.compactClock(bedtime)
  return if (bedtime <= nowMillis) stringResource(R.string.asleep_since, time)
  else stringResource(R.string.bedtime_at, time)
}

private const val PREVIEW_NOW = 1_761_255_900_000L // Thu, Oct 23 2025, 3:45 PM PDT

// Two hours and a quarter after the preview's "now", in whatever zone renders the preview.
private val previewSuggestion =
  (PREVIEW_NOW + TimeUnit.MINUTES.toMillis(135)).let { startMillis ->
    val time = Instant.ofEpochMilli(startMillis).atZone(ZoneId.systemDefault()).toLocalTime()
    SuggestedFastingTime(
      suggestedTimeMillis = startMillis,
      suggestedTimeMinutes = time.hour * 60 + time.minute,
      reasoning = "",
      source = SuggestionSource.BEDTIME_BASED,
    )
  }

@Composable
private fun PreviewContent(
  isFasting: Boolean,
  suggestion: SuggestedFastingTime?,
  isWide: Boolean,
  goalId: String = PredefinedFastingGoals.SIXTEEN_EIGHT.id,
  elapsedMinutes: Long = 9 * 60 + 42,
) {
  TodayScreenContent(
    isTimePickerDialogOpen = false,
    isGoalBottomSheetOpen = false,
    isFasting = isFasting,
    startTimeInMillis = PREVIEW_NOW - TimeUnit.MINUTES.toMillis(elapsedMinutes),
    fastingGoalId = goalId,
    weeklyProgress = MockDataUtils.createMockWeeklyProgress(),
    suggestion = suggestion,
    bedtimeMinutes = 22 * 60,
    allGoals = PredefinedFastingGoals.allGoals,
    nowMillis = PREVIEW_NOW,
    isWide = isWide,
    onContentReady = {},
    onStartFasting = {},
    onStopFasting = {},
    onOpenTimePickerDialog = {},
    onCloseTimePickerDialog = {},
    onUpdateStartTime = {},
    onOpenGoalBottomSheet = {},
    onCloseGoalBottomSheet = {},
    onUpdateFastingGoal = {},
    onSaveCustomGoal = {},
    onDeleteCustomGoal = {},
  )
}

@Preview(showBackground = true, widthDp = 411, heightDp = 923)
@Composable
private fun TodayIdlePreview() {
  MaterialTheme {
    PreviewContent(isFasting = false, suggestion = previewSuggestion, isWide = false)
  }
}

@Preview(showBackground = true, widthDp = 411, heightDp = 923)
@Composable
private fun TodayIdleNoSuggestionPreview() {
  MaterialTheme { PreviewContent(isFasting = false, suggestion = null, isWide = false) }
}

@Preview(showBackground = true, widthDp = 411, heightDp = 923)
@Composable
private fun TodayActivePreview() {
  MaterialTheme { PreviewContent(isFasting = true, suggestion = null, isWide = false) }
}

@Preview(showBackground = true, widthDp = 411, heightDp = 923)
@Composable
private fun TodayActiveLongGoalPreview() {
  MaterialTheme {
    PreviewContent(
      isFasting = true,
      suggestion = null,
      isWide = false,
      goalId = PredefinedFastingGoals.THIRTY_SIX_HOUR.id,
    )
  }
}

// A fast past its goal: the timer has more digits and must shrink to fit inside the dial.
@Preview(showBackground = true, widthDp = 411, heightDp = 923)
@Composable
private fun TodayActiveOvertimePreview() {
  MaterialTheme {
    PreviewContent(isFasting = true, suggestion = null, isWide = false, elapsedMinutes = 20 * 60L)
  }
}

@Preview(showBackground = true, widthDp = 411, heightDp = 923, fontScale = 2f)
@Composable
private fun TodayActiveLargeFontPreview() {
  MaterialTheme { PreviewContent(isFasting = true, suggestion = null, isWide = false) }
}

// A high-chroma wallpaper scheme sampled from a device, to check the hero card graphics stay
// visible.
private val warmWallpaperScheme =
  lightColorScheme(
    primary = Color(0xFF744A2E),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFEDAC84),
    onPrimaryContainer = Color(0xFF4A2A17),
    inversePrimary = Color(0xFFE7B394),
    background = Color(0xFFFFF8F5),
    surface = Color(0xFFFFF8F5),
  )

@Preview(showBackground = true, widthDp = 853, heightDp = 704)
@Composable
private fun TodayIdleWarmWallpaperPreview() {
  MaterialTheme(colorScheme = warmWallpaperScheme) {
    PreviewContent(isFasting = false, suggestion = previewSuggestion, isWide = true)
  }
}

@Preview(showBackground = true, widthDp = 411, heightDp = 923)
@Composable
private fun TodayActiveWarmWallpaperPreview() {
  MaterialTheme(colorScheme = warmWallpaperScheme) {
    PreviewContent(isFasting = true, suggestion = previewSuggestion, isWide = false)
  }
}

@Preview(showBackground = true, backgroundColor = 0xFF141218, widthDp = 411, heightDp = 923)
@Composable
private fun TodayIdleDarkPreview() {
  MaterialTheme(colorScheme = darkColorScheme()) {
    PreviewContent(isFasting = false, suggestion = previewSuggestion, isWide = false)
  }
}

@Preview(showBackground = true, backgroundColor = 0xFF141218, widthDp = 411, heightDp = 923)
@Composable
private fun TodayActiveDarkPreview() {
  MaterialTheme(colorScheme = darkColorScheme()) {
    PreviewContent(isFasting = true, suggestion = null, isWide = false)
  }
}

@Preview(showBackground = true, widthDp = 853, heightDp = 704)
@Composable
private fun TodayIdleWidePreview() {
  MaterialTheme { PreviewContent(isFasting = false, suggestion = previewSuggestion, isWide = true) }
}

@Preview(showBackground = true, widthDp = 853, heightDp = 704)
@Composable
private fun TodayActiveWidePreview() {
  MaterialTheme { PreviewContent(isFasting = true, suggestion = null, isWide = true) }
}
