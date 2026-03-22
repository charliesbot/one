package com.charliesbot.one.features.profile

import com.charliesbot.shared.core.components.FastingDayData
import com.charliesbot.shared.core.domain.repository.FastingHistoryRepository
import com.charliesbot.shared.core.domain.usecase.GetMonthlyFastingMapUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import java.time.YearMonth
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
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class YouViewModelTest {

  private val testDispatcher = UnconfinedTestDispatcher()

  private val getMonthlyFastingMapUseCase: GetMonthlyFastingMapUseCase = mockk()
  private val fastingHistoryRepository: FastingHistoryRepository = mockk()

  @Before
  fun setup() {
    Dispatchers.setMain(testDispatcher)
  }

  @After
  fun tearDown() {
    Dispatchers.resetMain()
  }

  private fun createViewModel(): YouViewModel {
    every { getMonthlyFastingMapUseCase(any()) } returns flowOf(emptyMap())

    return YouViewModel(
      getMonthlyFastingMapUseCase = getMonthlyFastingMapUseCase,
      fastingHistoryRepository = fastingHistoryRepository,
    )
  }

  @Test
  fun `initial state has current month`() = runTest {
    val viewModel = createViewModel()
    advanceUntilIdle()

    assertEquals(YearMonth.now(), viewModel.uiState.value.selectedMonth)
  }

  @Test
  fun `onNextMonth advances month`() = runTest {
    val viewModel = createViewModel()
    backgroundScope.launch { viewModel.uiState.collect {} }
    advanceUntilIdle()

    val initialMonth = viewModel.uiState.value.selectedMonth
    viewModel.onNextMonth()
    advanceUntilIdle()

    assertEquals(initialMonth.plusMonths(1), viewModel.uiState.value.selectedMonth)
  }

  @Test
  fun `onPreviousMonth goes back one month`() = runTest {
    val viewModel = createViewModel()
    backgroundScope.launch { viewModel.uiState.collect {} }
    advanceUntilIdle()

    val initialMonth = viewModel.uiState.value.selectedMonth
    viewModel.onPreviousMonth()
    advanceUntilIdle()

    assertEquals(initialMonth.minusMonths(1), viewModel.uiState.value.selectedMonth)
  }

  @Test
  fun `onDaySelected sets selected day`() = runTest {
    val viewModel = createViewModel()
    backgroundScope.launch { viewModel.uiState.collect {} }
    advanceUntilIdle()

    val day = FastingDayData(date = java.time.LocalDate.now(), durationHours = 16, goalId = "16:8")
    viewModel.onDaySelected(day)
    advanceUntilIdle()

    assertNotNull(viewModel.uiState.value.selectedDay)
    assertEquals(16, viewModel.uiState.value.selectedDay?.durationHours)
  }

  @Test
  fun `onDaySelected with null clears selection`() = runTest {
    val viewModel = createViewModel()
    advanceUntilIdle()

    viewModel.onDaySelected(FastingDayData(date = java.time.LocalDate.now(), durationHours = 16))
    advanceUntilIdle()
    viewModel.onDaySelected(null)
    advanceUntilIdle()

    assertNull(viewModel.uiState.value.selectedDay)
  }

  @Test
  fun `onDeleteFastingEntry deletes and clears selection`() = runTest {
    coEvery { fastingHistoryRepository.deleteFastingRecord(any()) } returns Unit

    val viewModel = createViewModel()
    advanceUntilIdle()

    viewModel.onDaySelected(
      FastingDayData(
        date = java.time.LocalDate.now(),
        durationHours = 16,
        startTimeEpochMillis = 1000L,
      )
    )
    advanceUntilIdle()
    viewModel.onDeleteFastingEntry(1000L)
    advanceUntilIdle()

    coVerify { fastingHistoryRepository.deleteFastingRecord(1000L) }
    assertNull(viewModel.uiState.value.selectedDay)
  }
}
