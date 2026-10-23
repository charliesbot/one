package com.charliesbot.one.features.dashboard

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.charliesbot.shared.core.constants.PredefinedFastingGoals
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TodayScreenContentTest {

  @get:Rule val composeRule = createComposeRule()

  private var isFasting by mutableStateOf<Boolean?>(null)

  private fun setWideContent() {
    composeRule.setContent {
      Box(Modifier.size(width = 1200.dp, height = 800.dp)) {
        TodayScreenContent(
          isTimePickerDialogOpen = false,
          isGoalBottomSheetOpen = false,
          isFasting = isFasting,
          startTimeInMillis = 0L,
          fastingGoalId = PredefinedFastingGoals.SIXTEEN_EIGHT.id,
          weeklyProgress = emptyList(),
          suggestion = null,
          bedtimeMinutes = null,
          allGoals = emptyList(),
          nowMillis = System.currentTimeMillis(),
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
}
