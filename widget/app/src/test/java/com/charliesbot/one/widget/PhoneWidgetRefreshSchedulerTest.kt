package com.charliesbot.one.widget

import android.content.Context
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager
import io.mockk.mockk
import io.mockk.verify
import org.junit.Before
import org.junit.Test

class PhoneWidgetRefreshSchedulerTest {
  private lateinit var context: Context
  private lateinit var workManager: WorkManager
  private val oneHourMillis = 60L * 60L * 1000L

  @Before
  fun setup() {
    context = mockk(relaxed = true)
    workManager = mockk(relaxed = true)
  }

  @Test
  fun `does not schedule and cancels work when no active widgets exist`() {
    val scheduler = PhoneWidgetRefreshScheduler(
      context = context,
      workManager = workManager,
      activeWidgetChecker = { false },
    )

    scheduler.scheduleNext(startTimeMillis = 0L, goalDurationMillis = 16 * oneHourMillis)

    verify { workManager.cancelUniqueWork(PhoneWidgetRefreshScheduler.WORK_NAME) }
    verify(exactly = 0) {
      workManager.enqueueUniqueWork(any(), any(), any<OneTimeWorkRequest>())
    }
  }

  @Test
  fun `schedules unique work with delay when widget is active and fast is ongoing`() {
    val scheduler = PhoneWidgetRefreshScheduler(
      context = context,
      workManager = workManager,
      activeWidgetChecker = { true },
    )

    val startTime = 1000L
    val currentTime = startTime + (11 * oneHourMillis) + (15 * 60 * 1000L) // 11h 15m elapsed
    val goalDuration = 16 * oneHourMillis

    scheduler.scheduleNext(
      startTimeMillis = startTime,
      goalDurationMillis = goalDuration,
      currentTimeMillis = currentTime,
    )

    verify(exactly = 1) {
      workManager.enqueueUniqueWork(
        PhoneWidgetRefreshScheduler.WORK_NAME,
        ExistingWorkPolicy.REPLACE,
        any<OneTimeWorkRequest>(),
      )
    }
  }

  @Test
  fun `cancels work when fast is already completed`() {
    val scheduler = PhoneWidgetRefreshScheduler(
      context = context,
      workManager = workManager,
      activeWidgetChecker = { true },
    )

    val startTime = 1000L
    val currentTime = startTime + (16 * oneHourMillis) // exactly completed
    val goalDuration = 16 * oneHourMillis

    scheduler.scheduleNext(
      startTimeMillis = startTime,
      goalDurationMillis = goalDuration,
      currentTimeMillis = currentTime,
    )

    verify { workManager.cancelUniqueWork(PhoneWidgetRefreshScheduler.WORK_NAME) }
    verify(exactly = 0) {
      workManager.enqueueUniqueWork(any(), any(), any<OneTimeWorkRequest>())
    }
  }

  @Test
  fun `cancel delegates to workManager cancelUniqueWork`() {
    val scheduler = PhoneWidgetRefreshScheduler(
      context = context,
      workManager = workManager,
      activeWidgetChecker = { true },
    )

    scheduler.cancel()

    verify { workManager.cancelUniqueWork(PhoneWidgetRefreshScheduler.WORK_NAME) }
  }
}
