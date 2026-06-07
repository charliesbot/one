package com.charliesbot.shared.core.data.notifications

import android.content.Context
import android.util.Log
import androidx.work.ExistingWorkPolicy
import androidx.work.ListenableWorker
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager
import com.charliesbot.shared.core.domain.repository.CustomGoalRepository
import com.charliesbot.shared.core.domain.repository.SettingsRepository
import com.charliesbot.shared.core.models.CustomGoalData
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import io.mockk.verify
import io.mockk.slot
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.After
import org.junit.Before
import org.junit.Test

class NotificationSchedulerTest {

  private lateinit var context: Context
  private lateinit var settingsRepository: SettingsRepository
  private lateinit var customGoalRepository: CustomGoalRepository
  private lateinit var workManager: WorkManager
  private lateinit var scheduler: NotificationScheduler

  @Before
  fun setup() {
    mockkStatic(Log::class)
    every { Log.d(any(), any()) } returns 0
    every { Log.e(any(), any()) } returns 0
    every { Log.w(any(), any<String>()) } returns 0

    context = mockk(relaxed = true)
    settingsRepository = mockk(relaxed = true)
    customGoalRepository = mockk(relaxed = true)
    workManager = mockk(relaxed = true)

    scheduler =
      NotificationScheduler(
        context = context,
        workerClass = ListenableWorker::class.java,
        settingsRepository = settingsRepository,
        customGoalRepository = customGoalRepository,
        workManager = workManager,
      )
  }

  @After
  fun tearDown() {
    unmockkAll()
  }

  @Test
  fun `scheduleSmartReminderNotifications skips when settings not yet persisted`() = runTest {
    // Simulate the race condition: DataStore hasn't been updated yet
    every { settingsRepository.smartRemindersEnabled } returns flowOf(false)

    val futureTime = System.currentTimeMillis() + 3_600_000L

    scheduler.scheduleSmartReminderNotifications(futureTime)

    // No work should be enqueued because smart reminders appear disabled
    verify(exactly = 0) { workManager.enqueueUniqueWork(any(), any(), any<OneTimeWorkRequest>()) }
  }

  @Test
  fun `scheduleSmartReminderNotificationsForced schedules even without checking settings`() =
    runTest {
      // Even though DataStore says disabled, forced variant should schedule
      every { settingsRepository.smartRemindersEnabled } returns flowOf(false)

      val futureTime = System.currentTimeMillis() + 3_600_000L

      scheduler.scheduleSmartReminderNotificationsForced(futureTime)

      // Work SHOULD be enqueued regardless of settings
      verify(atLeast = 1) {
        workManager.enqueueUniqueWork(any(), any<ExistingWorkPolicy>(), any<OneTimeWorkRequest>())
      }
    }

  @Test
  fun `scheduleSmartReminderNotifications schedules when enabled`() = runTest {
    every { settingsRepository.smartRemindersEnabled } returns flowOf(true)

    val futureTime = System.currentTimeMillis() + 3_600_000L

    scheduler.scheduleSmartReminderNotifications(futureTime)

    // Work should be enqueued because smart reminders are enabled
    verify(atLeast = 1) {
      workManager.enqueueUniqueWork(any(), any<ExistingWorkPolicy>(), any<OneTimeWorkRequest>())
    }
  }

  @Test
  fun `scheduleNotifications uses custom goal duration when goal id is custom`() = runTest {
    every { settingsRepository.notificationsEnabled } returns flowOf(true)
    every { settingsRepository.notifyOneHourBefore } returns flowOf(false)
    every { settingsRepository.notifyOnCompletion } returns flowOf(true)
    every { customGoalRepository.customGoals } returns
      flowOf(
        listOf(
          CustomGoalData(
            id = "custom_14",
            name = "Fourteen",
            durationMillis = 14L * 60L * 60L * 1000L,
            colorHex = 0xFF000000,
          )
        )
      )
    val workRequest = slot<OneTimeWorkRequest>()

    scheduler.scheduleNotifications(System.currentTimeMillis(), "custom_14")

    verify {
      workManager.enqueueUniqueWork(
        "notification-COMPLETION",
        ExistingWorkPolicy.REPLACE,
        capture(workRequest),
      )
    }
    val initialDelay = workRequest.captured.workSpec.initialDelay
    assertTrue(
      "Expected custom 14h delay, got $initialDelay",
      initialDelay in 13L * 60L * 60L * 1000L..14L * 60L * 60L * 1000L,
    )
  }
}
