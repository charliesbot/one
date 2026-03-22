package com.charliesbot.one.features.settings

import com.charliesbot.shared.core.abstraction.AppVersionProvider
import com.charliesbot.shared.core.abstraction.ClipboardHelper
import com.charliesbot.shared.core.abstraction.HistoryExporter
import com.charliesbot.shared.core.abstraction.StringProvider
import com.charliesbot.shared.core.data.db.FastingRecord
import com.charliesbot.shared.core.domain.repository.FastingHistoryRepository
import com.charliesbot.shared.core.domain.repository.SettingsRepository
import com.charliesbot.shared.core.domain.repository.SmartReminderMode
import com.charliesbot.shared.core.domain.usecase.GetSuggestedFastingStartTimeUseCase
import com.charliesbot.shared.core.domain.usecase.SyncFastingStateUseCase
import com.charliesbot.shared.core.models.SuggestedFastingTime
import com.charliesbot.shared.core.models.SuggestionSource
import com.charliesbot.shared.core.services.SmartReminderCallback
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.verify
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
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {

  private val testDispatcher = UnconfinedTestDispatcher()

  private val settingsRepository: SettingsRepository = mockk(relaxed = true)
  private val fastingHistoryRepository: FastingHistoryRepository = mockk()
  private val syncFastingStateUseCase: SyncFastingStateUseCase = mockk()
  private val smartReminderCallback: SmartReminderCallback = mockk(relaxed = true)
  private val getSuggestedFastingStartTimeUseCase: GetSuggestedFastingStartTimeUseCase = mockk()
  private val stringProvider: StringProvider = mockk()
  private val appVersionProvider: AppVersionProvider = mockk()
  private val historyExporter: HistoryExporter = mockk()
  private val clipboardHelper: ClipboardHelper = mockk(relaxed = true)

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

  private fun createViewModel(): SettingsViewModel {
    every { settingsRepository.notificationsEnabled } returns flowOf(true)
    every { settingsRepository.notifyOnCompletion } returns flowOf(true)
    every { settingsRepository.notifyOneHourBefore } returns flowOf(true)
    every { settingsRepository.smartRemindersEnabled } returns flowOf(false)
    every { settingsRepository.bedtimeMinutes } returns flowOf(1320)
    every { settingsRepository.smartReminderMode } returns flowOf(SmartReminderMode.AUTO)
    every { settingsRepository.fixedFastingStartMinutes } returns flowOf(1140)
    every { appVersionProvider.versionName } returns "1.0.0"
    coEvery { getSuggestedFastingStartTimeUseCase() } returns
      SuggestedFastingTime(
        suggestedTimeMillis = 0L,
        suggestedTimeMinutes = 1080,
        reasoning = "test",
        source = SuggestionSource.BEDTIME_BASED,
      )

    return SettingsViewModel(
      settingsRepository = settingsRepository,
      fastingHistoryRepository = fastingHistoryRepository,
      syncFastingStateUseCase = syncFastingStateUseCase,
      smartReminderCallback = smartReminderCallback,
      getSuggestedFastingStartTimeUseCase = getSuggestedFastingStartTimeUseCase,
      stringProvider = stringProvider,
      appVersionProvider = appVersionProvider,
      historyExporter = historyExporter,
      clipboardHelper = clipboardHelper,
    )
  }

  @Test
  fun `uiState includes version name from provider`() = runTest {
    val viewModel = createViewModel()
    backgroundScope.launch { viewModel.uiState.collect {} }
    advanceUntilIdle()

    assertEquals("1.0.0", viewModel.uiState.value.versionName)
  }

  @Test
  fun `uiState reflects settings repository values`() = runTest {
    val viewModel = createViewModel()
    backgroundScope.launch { viewModel.uiState.collect {} }
    advanceUntilIdle()

    val state = viewModel.uiState.value
    assertFalse(state.smartRemindersEnabled)
    assertEquals(1320, state.bedtimeMinutes)
    assertFalse(state.isSyncing)
    assertFalse(state.isExporting)
  }

  @Test
  fun `exportHistory with empty records sends error`() = runTest {
    every { fastingHistoryRepository.getAllHistory() } returns flowOf(emptyList())
    every { stringProvider.getString(any()) } returns "error"

    val viewModel = createViewModel()
    advanceUntilIdle()

    viewModel.exportHistory()
    advanceUntilIdle()

    assertFalse(viewModel.uiState.value.isExporting)
  }

  @Test
  fun `exportHistory delegates to historyExporter`() = runTest {
    val records =
      listOf(
        FastingRecord(
          startTimeEpochMillis = 1000L,
          endTimeEpochMillis = 2000L,
          fastingGoalId = "16:8",
        )
      )
    every { fastingHistoryRepository.getAllHistory() } returns flowOf(records)
    coEvery { historyExporter.export(records) } returns Result.success("test.csv")
    every { stringProvider.getString(any()) } returns "success"

    val viewModel = createViewModel()
    advanceUntilIdle()

    viewModel.exportHistory()
    advanceUntilIdle()

    coVerify { historyExporter.export(records) }
    assertFalse(viewModel.uiState.value.isExporting)
  }

  @Test
  fun `copyVersionToClipboard delegates to clipboardHelper`() = runTest {
    every { stringProvider.getString(any()) } returns "copied"

    val viewModel = createViewModel()
    advanceUntilIdle()

    viewModel.copyVersionToClipboard()
    advanceUntilIdle()

    verify { clipboardHelper.copy("App Version", "1.0.0") }
  }

  @Test
  fun `forceSyncToWatch calls sync use case`() = runTest {
    coEvery { syncFastingStateUseCase() } returns Result.success(Unit)
    every { stringProvider.getString(any()) } returns "synced"

    val viewModel = createViewModel()
    advanceUntilIdle()

    viewModel.forceSyncToWatch()
    advanceUntilIdle()

    coVerify { syncFastingStateUseCase() }
    assertFalse(viewModel.uiState.value.isSyncing)
  }
}
