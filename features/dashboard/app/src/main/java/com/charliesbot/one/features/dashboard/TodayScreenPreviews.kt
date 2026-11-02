package com.charliesbot.one.features.dashboard

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import com.charliesbot.shared.core.constants.PredefinedFastingGoals
import com.charliesbot.shared.core.models.SuggestedFastingTime
import com.charliesbot.shared.core.models.SuggestionSource
import com.charliesbot.shared.core.testing.MockDataUtils
import java.time.Instant
import java.time.ZoneId
import java.util.concurrent.TimeUnit

private const val PREVIEW_NOW = 1_761_255_900_000L // Thu, Oct 23 2025, 3:45 PM PDT

// Two hours and a quarter after the preview's "now", in whatever zone renders the preview.
private val previewSuggestion = suggestionAfterNow(minutes = 135)

private fun suggestionAfterNow(minutes: Long): SuggestedFastingTime {
  val startMillis = PREVIEW_NOW + TimeUnit.MINUTES.toMillis(minutes)
  val time = Instant.ofEpochMilli(startMillis).atZone(ZoneId.systemDefault()).toLocalTime()
  return SuggestedFastingTime(
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

// A late start on a short goal puts bedtime close to the start time, where their labels could meet.
@Preview(showBackground = true, widthDp = 411, heightDp = 923)
@Composable
private fun TodayIdleLateStartPreview() {
  MaterialTheme {
    PreviewContent(
      isFasting = false,
      suggestion = suggestionAfterNow(minutes = 221),
      isWide = false,
      goalId = PredefinedFastingGoals.CIRCADIAN.id,
    )
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
