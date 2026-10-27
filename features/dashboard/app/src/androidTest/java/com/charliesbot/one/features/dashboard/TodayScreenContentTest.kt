package com.charliesbot.one.features.dashboard

import android.text.format.DateFormat
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.charliesbot.one.features.dashboard.model.ClockText
import com.charliesbot.shared.core.constants.PredefinedFastingGoals
import java.time.Instant
import java.time.ZoneId
import java.util.concurrent.TimeUnit
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TodayScreenContentTest {

  @get:Rule val composeRule = createComposeRule()

  private var isFasting by mutableStateOf<Boolean?>(null)
  private val nowMillis = System.currentTimeMillis()
  private val startMillis = nowMillis - TimeUnit.HOURS.toMillis(2)

  private fun setWideContent() {
    composeRule.setContent {
      Box(Modifier.size(width = 1200.dp, height = 800.dp)) {
        TodayScreenContent(
          isTimePickerDialogOpen = false,
          isGoalBottomSheetOpen = false,
          isFasting = isFasting,
          startTimeInMillis = startMillis,
          fastingGoalId = PredefinedFastingGoals.SIXTEEN_EIGHT.id,
          weeklyProgress = emptyList(),
          suggestion = null,
          bedtimeMinutes = null,
          allGoals = emptyList(),
          nowMillis = nowMillis,
          isWide = true,
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
    }
  }

  @Test
  fun wideHeaderStaysInPlaceWhenTheFastingStateLoads() {
    setWideContent()
    val title = composeRule.onNodeWithText("Today")
    val loadingLeft = title.getUnclippedBoundsInRoot().left

    isFasting = false
    composeRule.waitForIdle()

    assertEquals(loadingLeft, title.getUnclippedBoundsInRoot().left)
  }

  @Test
  fun wideFastWindowReadsItsTimesToScreenReaders() {
    isFasting = true
    setWideContent()
    val context = InstrumentationRegistry.getInstrumentation().targetContext
    val startClock =
      ClockText.compact(
        Instant.ofEpochMilli(startMillis).atZone(ZoneId.systemDefault()).toLocalTime(),
        DateFormat.is24HourFormat(context),
        context.resources.configuration.locales[0],
      )

    composeRule.onNodeWithContentDescription(startClock, substring = true).assertExists()
  }
}
