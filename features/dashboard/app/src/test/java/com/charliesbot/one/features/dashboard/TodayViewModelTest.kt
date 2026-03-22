package com.charliesbot.one.features.dashboard

import com.charliesbot.shared.core.constants.PredefinedFastingGoals
import com.charliesbot.shared.core.domain.repository.CustomGoalRepository
import com.charliesbot.shared.core.domain.repository.FastingHistoryRepository
import com.charliesbot.shared.core.domain.repository.SettingsRepository
import com.charliesbot.shared.core.domain.usecase.GetSuggestedFastingStartTimeUseCase
import com.charliesbot.shared.core.domain.usecase.ObserveFastingStateUseCase
import com.charliesbot.shared.core.domain.usecase.StartFastingUseCase
import com.charliesbot.shared.core.domain.usecase.StopFastingUseCase
import com.charliesbot.shared.core.domain.usecase.UpdateFastingConfigUseCase
import com.charliesbot.shared.core.models.FastingDataItem
import com.charliesbot.shared.core.models.SuggestedFastingTime
import com.charliesbot.shared.core.models.SuggestionSource
import com.charliesbot.shared.core.utils.GoalResolver
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
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
class TodayViewModelTest {

  private val testDispatcher = UnconfinedTestDispatcher()

  private val fastingHistoryRepository: FastingHistoryRepository = mockk()
  private val observeFastingStateUseCase: ObserveFastingStateUseCase = mockk()
  private val startFastingUseCase: StartFastingUseCase = mockk()
  private val stopFastingUseCase: StopFastingUseCase = mockk()
  private val updateFastingConfigUseCase: UpdateFastingConfigUseCase = mockk()
  private val settingsRepository: SettingsRepository = mockk()
  private val getSuggestedFastingStartTimeUseCase: GetSuggestedFastingStartTimeUseCase = mockk()
  private val customGoalRepository: CustomGoalRepository = mockk()
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
    Dispatchers.resetMain()
  }

  private fun createViewModel(): TodayViewModel {
    every { observeFastingStateUseCase() } returns flowOf(null)
    every { fastingHistoryRepository.getCurrentWeekHistory() } returns flowOf(emptyList())
    every { settingsRepository.smartRemindersEnabled } returns flowOf(false)
    every { settingsRepository.bedtimeMinutes } returns flowOf(1320)
    every { goalResolver.allGoals } returns flowOf(PredefinedFastingGoals.allGoals)
    coEvery { getSuggestedFastingStartTimeUseCase() } returns
      SuggestedFastingTime(
        suggestedTimeMillis = 0L,
        suggestedTimeMinutes = 1080,
        reasoning = "test",
        source = SuggestionSource.BEDTIME_BASED,
      )

    return TodayViewModel(
      fastingHistoryRepository = fastingHistoryRepository,
      observeFastingStateUseCase = observeFastingStateUseCase,
      startFastingUseCase = startFastingUseCase,
      stopFastingUseCase = stopFastingUseCase,
      updateFastingConfigUseCase = updateFastingConfigUseCase,
      settingsRepository = settingsRepository,
      getSuggestedFastingStartTimeUseCase = getSuggestedFastingStartTimeUseCase,
      customGoalRepository = customGoalRepository,
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
    every { observeFastingStateUseCase() } returns flowOf(fastingItem)
    every { fastingHistoryRepository.getCurrentWeekHistory() } returns flowOf(emptyList())
    every { settingsRepository.smartRemindersEnabled } returns flowOf(false)
    every { settingsRepository.bedtimeMinutes } returns flowOf(1320)
    every { goalResolver.allGoals } returns flowOf(PredefinedFastingGoals.allGoals)
    coEvery { getSuggestedFastingStartTimeUseCase() } returns
      SuggestedFastingTime(
        suggestedTimeMillis = 0L,
        suggestedTimeMinutes = 1080,
        reasoning = "test",
        source = SuggestionSource.BEDTIME_BASED,
      )

    val viewModel =
      TodayViewModel(
        fastingHistoryRepository = fastingHistoryRepository,
        observeFastingStateUseCase = observeFastingStateUseCase,
        startFastingUseCase = startFastingUseCase,
        stopFastingUseCase = stopFastingUseCase,
        updateFastingConfigUseCase = updateFastingConfigUseCase,
        settingsRepository = settingsRepository,
        getSuggestedFastingStartTimeUseCase = getSuggestedFastingStartTimeUseCase,
        customGoalRepository = customGoalRepository,
        goalResolver = goalResolver,
      )

    // Activate WhileSubscribed flows
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
  fun `dialog state toggles correctly`() = runTest {
    val viewModel = createViewModel()
    advanceUntilIdle()

    assertFalse(viewModel.isTimePickerDialogOpen.value)
    viewModel.openTimePickerDialog()
    assertTrue(viewModel.isTimePickerDialogOpen.value)
    viewModel.closeTimePickerDialog()
    assertFalse(viewModel.isTimePickerDialogOpen.value)
  }

  @Test
  fun `goal bottom sheet toggles correctly`() = runTest {
    val viewModel = createViewModel()
    advanceUntilIdle()

    assertFalse(viewModel.isGoalBottomSheetOpen.value)
    viewModel.openGoalBottomSheet()
    assertTrue(viewModel.isGoalBottomSheetOpen.value)
    viewModel.closeGoalBottomSheet()
    assertFalse(viewModel.isGoalBottomSheetOpen.value)
  }
}
