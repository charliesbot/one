package com.charliesbot.onewearos

import com.charliesbot.shared.core.constants.PredefinedFastingGoals
import com.charliesbot.shared.core.domain.usecase.ObserveFastingStateUseCase
import com.charliesbot.shared.core.domain.usecase.StartFastingUseCase
import com.charliesbot.shared.core.domain.usecase.StopFastingUseCase
import com.charliesbot.shared.core.domain.usecase.UpdateFastingConfigUseCase
import com.charliesbot.shared.core.models.FastingDataItem
import com.charliesbot.shared.core.models.FastingGoal
import com.charliesbot.shared.core.models.FastingGoalCatalog
import com.charliesbot.shared.core.utils.GoalResolver
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import java.time.LocalDate
import java.time.LocalTime
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class WearTodayViewModelTest {

  private val testDispatcher = UnconfinedTestDispatcher()

  private val observeFastingStateUseCase: ObserveFastingStateUseCase = mockk()
  private val startFastingUseCase: StartFastingUseCase = mockk()
  private val stopFastingUseCase: StopFastingUseCase = mockk()
  private val updateFastingConfigUseCase: UpdateFastingConfigUseCase = mockk()
  private val goalResolver: GoalResolver = mockk()

  @Before
  fun setup() {
    Dispatchers.setMain(testDispatcher)
    mockkStatic(android.util.Log::class)
    every { android.util.Log.d(any(), any()) } returns 0
    every { android.util.Log.e(any(), any()) } returns 0
    every { android.util.Log.e(any(), any(), any()) } returns 0
  }

  @After
  fun tearDown() {
    PredefinedFastingGoals.registerCustomGoals(emptyList())
    Dispatchers.resetMain()
  }

  private fun createViewModel(
    fastingItem: FastingDataItem? = null,
    goals: List<FastingGoal> = FastingGoalCatalog.allGoals,
  ): WearTodayViewModel {
    every { observeFastingStateUseCase() } returns flowOf(fastingItem)
    every { goalResolver.allGoals } returns flowOf(goals)

    return WearTodayViewModel(
      observeFastingStateUseCase = observeFastingStateUseCase,
      startFastingUseCase = startFastingUseCase,
      stopFastingUseCase = stopFastingUseCase,
      updateFastingConfigUseCase = updateFastingConfigUseCase,
      goalResolver = goalResolver,
    )
  }

  @Test
  fun `initial state is not fasting`() = runTest {
    val viewModel = createViewModel()
    advanceUntilIdle()

    assertFalse(viewModel.isFasting.value)
  }

  @Test
  fun `reflects fasting state from use case`() = runTest {
    val fastingItem =
      FastingDataItem(isFasting = true, startTimeInMillis = 1000L, fastingGoalId = "16:8")
    val viewModel = createViewModel(fastingItem)

    backgroundScope.launch { viewModel.isFasting.collect {} }
    backgroundScope.launch { viewModel.startTimeInMillis.collect {} }
    backgroundScope.launch { viewModel.fastingGoalId.collect {} }
    advanceUntilIdle()

    assertTrue(viewModel.isFasting.value)
    assertEquals(1000L, viewModel.startTimeInMillis.value)
    assertEquals("16:8", viewModel.fastingGoalId.value)
  }

  @Test
  fun `onStartFasting calls start use case`() = runTest {
    coEvery { startFastingUseCase(any()) } returns Result.success(Unit)
    val viewModel = createViewModel()
    advanceUntilIdle()

    viewModel.onStartFasting()
    advanceUntilIdle()

    coVerify { startFastingUseCase(PredefinedFastingGoals.SIXTEEN_EIGHT.id) }
  }

  @Test
  fun `onStopFasting calls stop use case`() = runTest {
    coEvery { stopFastingUseCase() } returns Result.success(Unit)
    val viewModel = createViewModel()
    advanceUntilIdle()

    viewModel.onStopFasting()
    advanceUntilIdle()

    coVerify { stopFastingUseCase() }
  }

  @Test
  fun `updateTemporalDate preserves time`() = runTest {
    val viewModel = createViewModel()
    advanceUntilIdle()

    viewModel.updateTemporalTime(LocalTime.of(14, 30))
    viewModel.updateTemporalDate(LocalDate.of(2026, 3, 15))

    val result = viewModel.temporalStartTime.value
    assertEquals(LocalDate.of(2026, 3, 15), result?.toLocalDate())
    assertEquals(LocalTime.of(14, 30), result?.toLocalTime())
  }

  @Test
  fun `updateTemporalTime preserves date`() = runTest {
    val viewModel = createViewModel()
    advanceUntilIdle()

    viewModel.updateTemporalDate(LocalDate.of(2026, 3, 15))
    viewModel.updateTemporalTime(LocalTime.of(18, 0))

    val result = viewModel.temporalStartTime.value
    assertEquals(LocalDate.of(2026, 3, 15), result?.toLocalDate())
    assertEquals(LocalTime.of(18, 0), result?.toLocalTime())
  }

  @Test
  fun `allGoals exposes FastGoal values and registers custom goals for lookup`() = runTest {
    val customGoal =
      FastingGoal(
        id = "custom_wear",
        name = "Wear Goal",
        durationMillis = 15 * 60 * 60 * 1000L,
        colorHex = 0xFF0000FF,
      )
    val viewModel = createViewModel(goals = FastingGoalCatalog.allGoals + customGoal)

    backgroundScope.launch { viewModel.allGoals.collect {} }
    advanceUntilIdle()

    val fastGoals = viewModel.allGoals.value
    assertEquals(6, fastGoals.size)
    assertEquals("Wear Goal", fastGoals.last().titleText)
    assertEquals("15", fastGoals.last().durationDisplay)
    assertEquals(fastGoals.last(), PredefinedFastingGoals.getGoalById("custom_wear"))
  }
}
