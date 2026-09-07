package com.charliesbot.one.widget.common

import com.charliesbot.shared.core.domain.repository.FastingDataRepository
import com.charliesbot.shared.core.models.FastingDataItem
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.Test

class PeriodicRefreshRegressionTest {
  private val repository = mockk<FastingDataRepository>()
  private val goals = mockk<GoalDurationResolver>()
  private val platform = mockk<WidgetPlatformAdapter>(relaxed = true)
  private val scheduler = WidgetRefreshScheduler(repository, goals, platform)

  @Test
  fun `worker leaves recurrence to WorkManager`() = runTest {
    coEvery { platform.hasActiveWidgets() } returns true
    coEvery { repository.getCurrentFasting() } returns
      FastingDataItem(
        isFasting = true,
        startTimeInMillis = System.currentTimeMillis(),
        fastingGoalId = "16:8",
      )
    coEvery { goals.durationMillis("16:8") } returns 16 * 3600000L

    scheduler.refresh()

    coVerify(exactly = 1) { platform.requestWidgetUpdate() }
    verify(exactly = 0) { platform.ensurePeriodicWork() }
  }

  @Test
  fun `new fast started during redraw is not cancelled by old inactive state`() = runTest {
    var fast = FastingDataItem(isFasting = false)
    coEvery { platform.hasActiveWidgets() } returns true
    coEvery { repository.getCurrentFasting() } coAnswers { fast }
    coEvery { goals.durationMillis(any()) } returns 16 * 3600000L
    coEvery { platform.requestWidgetUpdate() } coAnswers
      {
        fast =
          FastingDataItem(
            isFasting = true,
            startTimeInMillis = System.currentTimeMillis(),
            fastingGoalId = "16:8",
          )
      }

    scheduler.refresh()

    verify(exactly = 0) { platform.cancelScheduledWork() }
  }
}
